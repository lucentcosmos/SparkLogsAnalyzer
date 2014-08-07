package com.databricks.apps.logs;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The LogAnalyzerAppMain is an sample logs analysis application.  For now,
 * it is a simple minimal viable product:
 *   - Read in new log files from a directory and input those new files into streaming.
 *   - Computes stats for all of time as well as the last time interval based on those logs.
 *   - Write the calculated stats to an txt file on the local file system
 *     that gets refreshed every time interval.
 *
 * Once you get this program up and running, feed apache access log files
 * into the local directory of your choosing.
 *
 * Then open your output text file, perhaps in a web browser, and refresh
 * that page to see more stats come in.
 *
 * Example command to run:
 * %  ${YOUR_SPARK_HOME}/bin/spark-submit
 *     --class "com.databricks.apps.logs.LogAnalyzerAppMain"
 *     --master local[4]
 *     YOUR_LOCAL_LOGS_DIRECTORY
 *     AN_OUTPUT_FILE
 *     target/log-analyzer-1.0.jar
 */
public class LogAnalyzerAppMain {
  private static final Duration WINDOW_LENGTH = new Duration(30 * 1000);
  private static final Duration SLIDE_INTERVAL = new Duration(10 * 1000);

  // These static variables stores the running content size values.
  private static final AtomicLong runningCount = new AtomicLong(0);
  private static final AtomicLong runningSum = new AtomicLong(0);
  private static final AtomicLong runningMin = new AtomicLong(Long.MAX_VALUE);
  private static final AtomicLong runningMax = new AtomicLong(Long.MIN_VALUE);
  private static List<Tuple2<Integer, Long>> currentResponseCodeCounts = null;
  private static List<String> currentIPAddresses = null;
  private static List<Tuple2<String, Long>> currentTopEndpoints = null;

  private static LogStatistics lastWindowStatistics = null;

  public static void main(String[] args) throws IOException {
    SparkConf conf = new SparkConf().setAppName("Log Analyzer Reference App Main");
    JavaSparkContext sc = new JavaSparkContext(conf);
    JavaStreamingContext jssc = new JavaStreamingContext(sc, SLIDE_INTERVAL);

    // Specify a directory to monitor for log files.
    if (args.length < 2) {
      System.out.println("Must specify an access logs directory and an output file.");
      System.exit(-1);
    }
    String directory = args[0];
    String outputFile = args[1];

    // Checkpointing must be enabled to use the updateStateByKey function.
    jssc.checkpoint("/tmp/log-analyzer-streaming");

    // This methods monitors a directory for new files to read in for streaming.
    JavaDStream<String> logData = jssc.textFileStream(directory);

    JavaDStream<ApacheAccessLog> accessLogsDStream
        = logData.map(ApacheAccessLog::parseFromLogLine).cache();


    // Calculate statistics based on the content size, and update the static variables to track this.
    JavaDStream<Long> contentSizeDStream =
        accessLogDStream.map(ApacheAccessLog::getContentSize).cache();
    contentSizeDStream.foreachRDD(rdd -> {
      if (rdd.count() > 0) {
        runningSum.getAndAdd(rdd.reduce(LogFunctions.SUM_REDUCER));
        runningCount.getAndAdd(rdd.count());
        runningMin.set(Math.min(runningMin.get(),
            rdd.min(Comparator.naturalOrder())));
        runningMax.set(Math.max(runningMax.get(),
            rdd.max(Comparator.naturalOrder())));
      }
      return null;
    });


    // A DStream of Resonse Code Counts;
    JavaPairDStream<Integer, Long> responseCodeCountDStream = accessLogDStream
        .mapToPair(s -> new Tuple2<>(s.getResponseCode(), 1L))
        .reduceByKey(SUM_REDUCER)
        .updateStateByKey(COMPUTE_RUNNING_SUM);
    responseCodeCountDStream.foreachRDD(rdd -> {
      currentResponseCodeCounts = rdd.take(100);
      return null;
    });

    // A DStream of ipAddressCounts.
    JavaDStream<String> ipAddressesDStream = accessLogsDStream
        .mapToPair(s -> new Tuple2<>(s.getIpAddress(), 1L))
        .reduceByKey(LogFunctions.SUM_REDUCER)
        .updateStateByKey(LogFunctions.COMPUTE_RUNNING_SUM)
        .filter(tuple -> tuple._2() > 10)
        .map(Tuple2::_1);
    ipAddressesDStream.foreachRDD(rdd -> {
      currentIPAddresses = rdd.take(100);
      return null;
    });

    // A DStream of endpoint to count.
    JavaPairDStream<String, Long> endpointCountsDStream = accessLogsDStream
        .mapToPair(s -> new Tuple2<>(s.getEndpoint(), 1L))
        .reduceByKey(LogFunctions.SUM_REDUCER)
        .updateStateByKey(LogFunctions.COMPUTE_RUNNING_SUM);
    endpointCountsDStream.foreachRDD(rdd -> {
     currentTopEndpoints = rdd.takeOrdered(10,
         new LogFunctions.ValueComparator<>(Comparator.<Long>naturalOrder()));
      return null;
    });

    // Calculate statistics for the last time interval.
    JavaDStream<ApacheAccessLog> windowDStream = accessLogsDStream.window(
        WINDOW_LENGTH, SLIDE_INTERVAL);
    windowDStream.foreachRDD(accessLogs -> {
      if (accessLogs.count() == 0) {
        lastWindowStatistics = null;
      } else {
        lastWindowStatistics = LogAnalyzerRDD.processRdd(accessLogs);
      }
      // Call this to output the stats.
      outputLogStatsToFile(outputFile);

      return null;
    });

    // Start the streaming server.
    jssc.start();              // Start the computation
    jssc.awaitTermination();   // Wait for the computation to terminate
  }

  private static void outputLogStatsToFile(String filename)
      throws IOException {
    Writer out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filename)));

    out.write("----Log statistics for all of time:\n");

    if (runningCount.get() == 0) {
      out.write("Content Size Avg: -, Min: -, Max: -\n");
    } else {
      out.write(String.format("Content Size Avg: %s, Min: %s, Max: %s\n",
          runningSum.get() / runningCount.get(),
          runningMin.get(),
          runningMax.get()));
    }
    out.write(String.format("Response code counts: %s\n", currentResponseCodeCounts));
    out.write(String.format("IPAddresses > 10 times: %s\n", currentIPAddresses));
    out.write(String.format("Top Endpoints: %s\n", currentTopEndpoints));

    out.write("\n");

    if (lastWindowStatistics != null) {
      out.write("---Log Statistics for the last time interval:\n");
      lastWindowStatistics.writeToFile(out);
    }

    out.close();
  }
}
