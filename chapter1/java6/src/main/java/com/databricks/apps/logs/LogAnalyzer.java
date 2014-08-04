package com.databricks.apps.logs;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * The LogAnalyzer takes in an apache access log file and
 * computes some statistics on them.
 *
 * Example command to run:
 * %  ${YOUR_SPARK_HOME}/bin/spark-submit
 *     --class "com.databricks.apps.logs.LogsAnalyzer"
 *     --master local[4]
 *     target/log-analyzer-1.0.jar
 *     ../../data/apache.access.log
 */
public class LogAnalyzer {
  private static Function2<Long, Long, Long> SUM_REDUCER = new Function2<Long, Long, Long>() {
    @Override
    public Long call(Long a, Long b) throws Exception {
      return a+b;
    }
  };

  private static class ValueComparator<K, V>
     implements Comparator<Tuple2<K, V>>, Serializable {
    private Comparator<V> comparator;

    public ValueComparator(Comparator<V> comparator) {
      this.comparator = comparator;
    }

    @Override
    public int compare(Tuple2<K, V> o1, Tuple2<K, V> o2) {
      return comparator.compare(o1._2(), o2._2());
    }
  }

  private static Comparator<Long> LONG_NATURAL_ORDER_COMPARATOR = new Comparator<Long>() {
    @Override
    public int compare(Long a, Long b) {
      if (a > b) return 1;
      if (a.equals(b)) return 0;
      return -1;
    }
  };

  public static void main(String[] args) {
    // Create a Spark Context.
    SparkConf conf = new SparkConf().setAppName("Log Analyzer");
    JavaSparkContext sc = new JavaSparkContext(conf);

    // Load the text file into Spark.
    if (args.length == 0) {
      System.out.println("Must specify an access logs file.");
      System.exit(-1);
    }
    String logFile = args[0];
    JavaRDD<String> logLines = sc.textFile(logFile);

    // Convert the text log lines to ApacheAccessLog objects and cache them
    //   since multiple transformations and actions will be called on that data.
    JavaRDD<ApacheAccessLog> accessLogs =
       logLines.map(new Function<String, ApacheAccessLog>() {
         @Override
         public ApacheAccessLog call(String logline) throws Exception {
           return ApacheAccessLog.parseFromLogLine(logline);
         }
       }).cache();

    // Calculate statistics based on the content size.
    // Note how the contentSizes are cached as well since multiple actions
    //   are called on that RDD.
    JavaRDD<Long> contentSizes =
       accessLogs.map(new Function<ApacheAccessLog, Long>() {
         @Override
         public Long call(ApacheAccessLog apacheAccessLog) throws Exception {
           return apacheAccessLog.getContentSize();
         }
       }).cache();
    System.out.print("Content Size Avg: " + contentSizes.reduce(SUM_REDUCER) / contentSizes.count());
    System.out.print(", Min: " + contentSizes.min(LONG_NATURAL_ORDER_COMPARATOR));
    System.out.println(", Max: " + contentSizes.max(LONG_NATURAL_ORDER_COMPARATOR));

    // Compute Response Code to Count.
    List<Tuple2<Integer, Long>> responseCodeToCount =
        accessLogs.mapToPair(
            new PairFunction<ApacheAccessLog, Integer, Long>() {
              @Override
              public Tuple2<Integer, Long> call(ApacheAccessLog log) throws Exception {
                return new Tuple2<Integer, Long>(log.getResponseCode(), 1L);
              }
            })
            .reduceByKey(SUM_REDUCER)
            .take(100);
    System.out.println("Response code counts: " + responseCodeToCount);

    // Any IPAddress that has accessed the server more than 10 times.
    List<String> ipAddresses =
        accessLogs.mapToPair(
            new PairFunction<ApacheAccessLog, String, Long>() {
              @Override
              public Tuple2<String, Long> call(ApacheAccessLog log) throws Exception {
                return new Tuple2<String, Long>(log.getIpAddress(), 1L);
              }
            })
            .reduceByKey(SUM_REDUCER)
            .filter(new Function<Tuple2<String, Long>, Boolean>() {
                      @Override
                      public Boolean call(Tuple2<String, Long> tuple) throws Exception {
                        return tuple._2() > 10;
                      }
                    }
            )
            .map(new Function<Tuple2<String, Long>, String>() {
              @Override
              public String call(Tuple2<String, Long> tuple) throws Exception {
                return tuple._1();
              }
            })
            .take(100);
    System.out.println("IPAddresses > 10 times: " + ipAddresses);

    // Top Endpoints.
    List<Tuple2<String, Long>> topEndpoints = accessLogs
        .mapToPair(new PairFunction<ApacheAccessLog, String, Long>() {
                     @Override
                     public Tuple2<String, Long> call(ApacheAccessLog log) throws Exception {
                       return new Tuple2<String, Long>(log.getEndpoint(), 1L);
                     }
                   })
        .reduceByKey(SUM_REDUCER)
        .top(10, new ValueComparator<String, Long>(LONG_NATURAL_ORDER_COMPARATOR));
    System.out.println("Top Endpoints: " + topEndpoints);

    sc.stop();
  }
}

