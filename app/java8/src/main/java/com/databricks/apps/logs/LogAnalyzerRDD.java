package com.databricks.apps.logs;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;
import scala.Tuple4;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

public class LogAnalyzerRDD implements Serializable {
  public static LogStatistics processRdd(JavaRDD<ApacheAccessLog> accessLogs) {
    JavaRDD<Long> contentSizes =
        accessLogs.map(ApacheAccessLog::getContentSize).cache();
    Tuple4<Long, Long, Long, Long> contentSizeStats = new Tuple4<>(
        contentSizes.reduce(LogFunctions.SUM_REDUCER),
        contentSizes.count(),
        contentSizes.min(Comparator.naturalOrder()),
        contentSizes.max(Comparator.naturalOrder()));

    List<Tuple2<Integer, Long>> responseCodeToCount = accessLogs.mapToPair(
        log -> new Tuple2<>(log.getResponseCode(), 1L))
        .reduceByKey(LogFunctions.SUM_REDUCER)
        .take(100);

    List<String> ipAddresses = accessLogs
        .mapToPair(log -> new Tuple2<>(log.getIpAddress(), 1L))
        .reduceByKey(LogFunctions.SUM_REDUCER)
        .filter(tuple -> tuple._2() > 10)
        .map(Tuple2::_1)
        .take(100);

    List<Tuple2<String, Long>> topEndpoints = accessLogs
        .mapToPair(log -> new Tuple2<>(log.getEndpoint(), 1L))
        .reduceByKey(LogFunctions.SUM_REDUCER)
        .top(10, new LogFunctions.ValueComparator<>(Comparator.<Long>naturalOrder()));

    return new LogStatistics(contentSizeStats, responseCodeToCount,
        ipAddresses, topEndpoints);
  }
}
