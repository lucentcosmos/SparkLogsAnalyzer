package com.databricks.apps.logs.chapter2;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

public class LogAnalyzerStreamingImportKafka {
  private static final Duration WINDOW_LENGTH = new Duration(30 * 1000);
  private static final Duration SLIDE_INTERVAL = new Duration(10 * 1000);

  public static void main(String[] args) {
    SparkConf conf = new SparkConf().setAppName("Log Analyzer Import Streaming HDFS");
    JavaSparkContext sc = new JavaSparkContext(conf);
    JavaStreamingContext jssc = new JavaStreamingContext(sc, SLIDE_INTERVAL);

    val dStream = KafkaUtils.createStream(ssc, zkQuorum, group, topicpMap)
    dStream.saveAsNewAPIHadoopFiles(hdfsDataUrl, "csv", classOf[String],
        classOf[String], classOf[TextOutputFormat[String,String]],
    ssc.sparkContext.hadoopConfiguration)

    sc.hadoopConfiguration();
//    KafkaUtils.createStream
//    // This methods monitors a directory for new files to read in for streaming.
//    JavaDStream<String> logData = jssc.textFileStream(directory);
//
//    JavaDStream<ApacheAccessLog> accessLogsDStream
//        = logData.map(ApacheAccessLog::parseFromLogLine).cache();
//
//    JavaDStream<ApacheAccessLog> windowDStream = accessLogsDStream.window(
//        WINDOW_LENGTH, SLIDE_INTERVAL);
//
//    final LogAnalyzerRDD logAnalyzerRDD = new LogAnalyzerRDD(sqlContext);
//    windowDStream.foreachRDD(accessLogs -> {
//      if (accessLogs.count() == 0) {
//        System.out.println("No access logs in this time interval");
//        return null;
//      }
//
//      LogStatistics logStatistics = logAnalyzerRDD.processRdd(accessLogs);
//      logStatistics.printToStandardOut();
//
//      return null;
//    });

    // Start the streaming server.
    jssc.start();              // Start the computation
    jssc.awaitTermination();   // Wait for the computation to terminate
  }
}
