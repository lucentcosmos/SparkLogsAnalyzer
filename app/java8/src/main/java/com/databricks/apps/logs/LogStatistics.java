package com.databricks.apps.logs;

import scala.Tuple2;
import scala.Tuple4;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class LogStatistics {
  private Tuple4<Long, Long, Long, Long> contentSizeStats;
  private List<Tuple2<Integer, Long>> responseCodeToCount;
  private List<String> ipAddresses;
  private List<Tuple2<String, Long>> topEndpoints;

  public LogStatistics(Tuple4<Long, Long, Long, Long> contentSizeStats,
                       List<Tuple2<Integer, Long>> responseCodeToCount,
                       List<String> ipAddresses,
                       List<Tuple2<String, Long>> topEndpoints) {
    this.contentSizeStats = contentSizeStats;
    this.responseCodeToCount = responseCodeToCount;
    this.ipAddresses = ipAddresses;
    this.topEndpoints = topEndpoints;
  }

  public Tuple4<Long, Long, Long, Long> getContentSizeStats() {
    return contentSizeStats;
  }

  public List<Tuple2<Integer, Long>> getResponseCodeToCount() {
    return responseCodeToCount;
  }

  public List<String> getIpAddresses() {
    return ipAddresses;
  }

  public List<Tuple2<String, Long>> getTopEndpoints() {
    return topEndpoints;
  }

  public void writeToFile(Writer out) throws IOException {
    out.write(String.format("Content Size Avg: %s, Min: %s, Max: %s\n",
        contentSizeStats._1() / contentSizeStats._2(),
        contentSizeStats._3(),
        contentSizeStats._4()));

    out.write(String.format("Response code counts: %s\n", responseCodeToCount));

    out.write(String.format("IPAddresses > 10 times: %s\n", ipAddresses));

    out.write(String.format("Top Endpoints: %s\n", topEndpoints));
  }
}
