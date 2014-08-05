# Chapter 1: Introduction to Apache Spark

In this chapter, we demonstrate how simple it is to analyze web logs using
Apache Spark.  We'll show how to load a Resilient Distributed Dataset
(**RDD**) of access log lines and use Spark tranformations and actions to compute
some statistics for web server monitoring.  In the process, we'll introduce
the Spark SQL and the Spark Streaming libraries.

In this explanation, the code snippets are in Java 8.  However,
there is also sample code in [Java 6](java6), [Scala](scala), and [Python](python)
included in this directory.  Refer to the README's in those folders for
instructions on how to build and run those examples.
(Note that at the time this was written, streaming has not yet been supported
 in python, so streaming is omitted in that example.)

## The Basic Log Analyzer

Going through the [Quick Start](https://spark.apache.org/docs/latest/quick-start.html)
and skimming the [Programming Guide](https://spark.apache.org/docs/latest/programming-guide.html)
are helpful before going through this section.

Before we begin using Spark, we'll need two things:

* *An Apache access log file*: If you have one, it's more interesting to use real
data.  If not, you can use the sample one provided at
 [data/apache.access.log](../data/apache.access.log).
* *A parser and model for the access logs* - see
 [ApacheAccessLog.java](src/main/java/com/databricks/apps/log/ApacheAccessLog.java).

We'll compute the following statistics based on logs data:

* The average, min, and max content size of responses returned from the server.
* A count of response code's returned.
* Return IPAddresses that have accessed this server more than N times.
* The top endpoints requested.

Now we are let's begin with a close look at the code that we will run.

Let's look at the main body of a simple Spark example.
The first step is to bring up a Spark context, load log lines from a text file 
into an RDD.  We use the parser to transform that to an RDD of ApacheAccessLog
objects and cache those objects since later on, we'll call multiple transformations
and actions on that RDD.  Caching means we won't have to recompute the RDD.
Finally, before exiting the function, we stop the Spark context.

```java
public class LogAnalyzer {
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
       logLines.map(ApacheAccessLog::parseFromLogLine).cache();
         
    ...[Insert more code here] 

    sc.stop();
  }
}
```

Now let's calculate the average, minimum, and maximum content size of the
response returned.  A map transformation extracts the content sizes, and 
then different actions(reduce, count, min, and max) are called to output
the different stats.
```java
// Calculate statistics based on the content size.
// Note how the contentSizes are cached as well since multiple actions
//   are called on that RDD.
JavaRDD<Long> contentSizes =
   accessLogs.map(ApacheAccessLog::getContentSize).cache();
System.out.print("Content Size Avg: " + contentSizes.reduce(SUM_REDUCER) / contentSizes.count());
System.out.print(", Min: " + contentSizes.min(Comparator.naturalOrder()));
System.out.println(", Max: " + contentSizes.max(Comparator.naturalOrder()));  
```

Now, let's compute the total count for each response code.
First, we'll define a sum reducer since that's used so commonly.
```java
private static Function2<Long, Long, Long> SUM_REDUCER = (a, b) -> a + b;
```

To compute the response code counts, we have to work with key value pairs.
Note that call take(100) instead of collect() to gather the final output.
That's just to be safe since it could take a really long time to call
collect() on a very large dataset.

```java
// Compute Response Code to Count.
List<Tuple2<Integer, Long>> responseCodeToCount =
    accessLogs.mapToPair(log -> new Tuple2<>(log.getResponseCode(), 1L))
        .reduceByKey(SUM_REDUCER)
        .take(100);
System.out.println("Response code counts: " + responseCodeToCount);
```

Last we calculate the top endpoints requested in this log file. We can define
an inner class, Value Comparator to help with that.

```java
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
```

Then, we can use the ValueComparator in the top function to compute the top
endpoints accessed on this server.
```java
List<Tuple2<String, Long>> topEndpoints = accessLogs
    .mapToPair(log -> new Tuple2<>(log.getEndpoint(), 1L))
    .reduceByKey(SUM_REDUCER)
    .top(10, new ValueComparator<>(Comparator.<Long>naturalOrder()));
System.out.println("Top Endpoints: " + topEndpoints);
```

These code snippets are from [LogAnalyzer.java](java8/src/main/com/databricks/apps/log/LogAnalyzer.java).
Now that we've walked through the code, try running that example.


## Spark SQL

The [Spark SQL Guide](https://spark.apache.org/docs/latest/sql-programming-guide.html)
is a useful reference for this section.

For those of you who are familiar with SQL, the same statistics we calculated
in the previous example can be done using Spark SQL rather than calling
Spark transformations and actions directly.  We walk through how to do that
here.

First, we need to create a SQL Spark context. Note how we create one Spark
Context, and then use that to instantiate different versions of Spark contexts.
```java
public class LogAnalyzerSQL {
  public static void main(String[] args) {
    // Create the spark context.
    SparkConf conf = new SparkConf().setAppName("Log Analyzer SQL");
    JavaSparkContext sc = new JavaSparkContext(conf);
    JavaSQLContext sqlContext = new JavaSQLContext(sc);

    if (args.length == 0) {
      System.out.println("Must specify an access logs file.");
      System.exit(-1);
    }
    String logFile = args[0];
    JavaRDD<ApacheAccessLog> accessLogs = sc.textFile(logFile)
        .map(ApacheAccessLog::parseFromLogLine);

    ...
    
    sc.stop();
  }
}
```

Next, we need a way to register our logs data into a table.  In Java, Spark SQL
can infer the table schema on a standard Java POJO - with getters and setters
as we've done with [ApacheAccessLog.java](src/main/java/com/databricks/apps/logs/ApacheAccessLog.java).
(Note: if you are using a different language besides Java, there is a different
way to infer the table schema.  The examples in this directory work out of the
box.  Or you can also refer to the
[Spark Streaming Programming Guide](https://spark.apache.org/docs/latest/sql-programming-guide.html#rdds)
for more details.)
```java
JavaSchemaRDD schemaRDD = sqlContext.applySchema(accessLogs, ApacheAccessLog.class).cache();
schemaRDD.registerAsTable("logs");
```

Now, we are ready to start running some SQL queries on our table.  Here's
the code to compute the identical statistics in the previous section:
```java
// Calculate statistics based on the content size.
Tuple4<Long, Long, Long, Long> contentSizeStats =
    sqlContext.sql("SELECT SUM(contentSize), COUNT(*), MIN(contentSize), MAX(contentSize) FROM logs")
        .map(row -> new Tuple4<>(row.getLong(0), row.getLong(1), row.getLong(2), row.getLong(3)))
        .first();
System.out.println(String.format("Content Size Avg: %s, Min: %s, Max: %s",
    contentSizeStats._1() / contentSizeStats._2(),
    contentSizeStats._3(),
    contentSizeStats._4()));

// Compute Response Code to Count.
List<Tuple2<Integer, Long>> responseCodeToCount = sqlContext
    .sql("SELECT responseCode, COUNT(*) FROM logs GROUP BY responseCode")
    .mapToPair(row -> new Tuple2<>(row.getInt(0), row.getLong(1)))
    .take(1000);
System.out.println(String.format("Response code counts: %s", responseCodeToCount));

// Any IPAddress that has accessed the server more than 10 times.
List<String> ipAddresses = sqlContext
    .sql("SELECT ipAddress, COUNT(*) AS total FROM logs GROUP BY ipAddress HAVING total > 10")
    .map(row -> row.getString(0))
    .take(100);  // Take only 100 in case this is a super large data set.
System.out.println(String.format("IPAddresses > 10 times: %s", ipAddresses));

// Top Endpoints.
List<Tuple2<String, Long>> topEndpoints = sqlContext
    .sql("SELECT endpoint, COUNT(*) AS total FROM logs GROUP BY endpoint ORDER BY total DESC LIMIT 10")
    .map(row -> new Tuple2<>(row.getString(0), row.getLong(1)))
    .collect();
System.out.println(String.format("Top Endpoints: %s", topEndpoints));
```

Try running [LogAnalyzerSQL.java](java8/src/main/com/databricks/apps/logs/LogAnalyzer.java) now.


## Spark Streaming

Please familiarize yourself with the concepts introducted in the
[Spark Streaming Programming Guide](https://spark.apache.org/docs/latest/streaming-programming-guide.html)
before beginning this section.  In particular, it defines DStreams which are an
essential concept in understanding Spark Streaming.

The earlier examples show us how to compute stats an old log file - but 
in a production system, we want live monitoring of the server logs.
To do that, we must use Spark Streaming.

To run the streaming examples, we'll tail a log file into netcat to send to Spark.
That is not the ideal way to get data into Spark in a production system,
but this is an easy workaround for now to demonstrate how to program in
Spark Streaming.  We will cover best practices for how to import data for Spark
Streaming in [Chapter 2](../chapter2/README.md).

In a terminal window, just run on a log file:
```
% tail -f [[YOUR_LOG_FILE]] | nc -lk 9999
```

If you don't have a log file that is actually been appended to live, you
can add lines manually either using the included test file or another file.
```
% cat ../../data/apache.access.log >> [[YOUR_LOG_FILE]]
```

### Windows

A typical use case for logs analysis is live monitoring of the web server,
in which case we may only be interested in the logs for the last one hour of time,
and we want those statistics to refresh every minute.  One hour would be
the *window length*, while one minute would be the *slide interval*.  In our 
examples, we use a window length of 30 seconds and a slide interval of
10 seconds since that is a comfortable choice for development.
  
Using the windows feature of Spark Streaming, makes it very easy to compute
stats for a window of time.

The first step is to create a streaming context and optionally a sql context
as well if you want to use Spark SQL.  Then, you have the main body of the code
where you set up the computations to do.  Finally, you call start() to startup 
the streaming context, and awaitTermination() to keep the streaming context up.
```java
public class LogAnalyzerStreamingSQL {
  public static void main(String[] args) {
    SparkConf conf = new SparkConf().setAppName("Log Analyzer Streaming SQL");
    JavaSparkContext sc = new JavaSparkContext(conf);

    JavaStreamingContext jssc = new JavaStreamingContext(sc,
        SLIDE_INTERVAL);  // This sets the update window to be every 10 seconds.

    // Create a SQL Context so SQL can be used.
    // Note that this SQL context is initialized from the same JavaSparkContext
    //   used to create the Java Streaming Context.
    JavaSQLContext sqlContext = new JavaSQLContext(sc);
    
    ...Insert code here to process logs.
    
    // Start the streaming server.
    jssc.start();              // Start the computation
    jssc.awaitTermination();   // Wait for the computation to terminate
  }
}
```

Now, a DStream can be read in from the socket.
```java 
JavaReceiverInputDStream<String> logData = jssc.socketTextStream("localhost", 9999);
```

Next, we can add familiar code to convert the logline into a ApacheAccessLog
DStream.
```java 
JavaDStream<ApacheAccessLog> accessLogDStream = logData.map(ApacheAccessLog::parseFromLogLine).cache();
```

Now, we create can call window on the accessLogDStream to create a windowDStream.
The window DStream has an RDD every slide interval of time, containing all the data
for the last window length of time.
```java 
JavaDStream<ApacheAccessLog> windowDStream = accessLogDStream.window(WINDOW_LENGTH, SLIDE_INTERVAL);
```

Then we call foreachRDD to the windowDStream, we get an RDD which we can then
process.  We can reuse the code from either two examples (regular or SQL) verbatim
on this RDD.  We've simply copied and pasted the code in this example,
but you could refactor this code nicely for reuse in your production system.
```java
windowDStream.foreachRDD(accessLogs -> {
  if (accessLogs.count() == 0) {
    System.out.println("No access logs in this time interval");
    return null;
  }
  // Insert code verbatim from LogAnalyzer.java or LogAnalyzerSQL.java here.
}
```

Now that we've walked through the code, run [LogAnalyzerStreaming.java](java8/src/main/com/databricks/apps/logs/LogAnalyzerStreaming.java) now.
and/or [LogAnalyzerStreamingSQL.java](java8/src/main/com/databricks/apps/logs/LogAnalyzerStreamingSQL.java).
Note that if you need to feed data into your log file manually, you'll need to
keep the streaming program open in one terminal while you feed data into the
log file from another terminal.

### UpdateStateByKey

If you really want to keep track of the log statistics for all of time, you'll
need to maintain the state of the statistics between processing RDD's.  If you
wish to compute keyed statistics and the number of keys is very large
(i.e. too big to fit in memory on one machine), you can use the the
UpdateStateByKey function of the Spark streaming library which we'll introduce
in this section.

First, to use UpdateStateByKey, we need to set up checkpointing on the streaming
context because we are maintaining state.  To do that, just call checkpoint
on the streaming context with a directory to write the checkpoint data:
```java
public class LogAnalyzerStreamingTotal {
  public static void main(String[] args) {
    SparkConf conf = new SparkConf().setAppName("Log Analyzer Streaming Total");
    JavaSparkContext sc = new JavaSparkContext(conf);

    JavaStreamingContext jssc = new JavaStreamingContext(sc,
        new Duration(10000));  // This sets the update window to be every 10 seconds.

    // Checkpointing must be enabled to use the updateStateByKey function.
    jssc.checkpoint("/tmp/log-analyzer-streaming");
    
    JavaReceiverInputDStream<String> logData = jssc.socketTextStream("localhost", 9999);
    
    JavaDStream<ApacheAccessLog> accessLogs = logData.map(ApacheAccessLog::parseFromLogLine).cache();
    
    ...
    
    // Start the streaming server.
    jssc.start();              // Start the computation
    jssc.awaitTermination();   // Wait for the computation to terminate
```

To compute the content size statistics, we can simply use static variables
to save the current running sum, count, min and max of the content sizes.

```java
// These static variables stores the running content size values.
private static final AtomicLong runningCount = new AtomicLong(0);
private static final AtomicLong runningSum = new AtomicLong(0);
private static final AtomicLong runningMin = new AtomicLong(Long.MAX_VALUE);
private static final AtomicLong runningMax = new AtomicLong(Long.MIN_VALUE);
```

Now, we can just update the values for these static variables by calling
foreachRDD on the content size Dstream:
```java
JavaDStream<Long> contentSizes = accessLogs.map(ApacheAccessLog::getContentSize).cache();
contentSizes.foreachRDD(rdd -> {
  if (rdd.count() > 0) {
    runningSum.getAndAdd(rdd.reduce(SUM_REDUCER));
    runningCount.getAndAdd(rdd.count());
    runningMin.set(Math.min(runningMin.get(), rdd.min(Comparator.naturalOrder())));
    runningMax.set(Math.max(runningMax.get(), rdd.max(Comparator.naturalOrder())));
    System.out.print("Content Size Avg: " +  runningSum.get() / runningCount.get());
    System.out.print(", Min: " + runningMin.get());
    System.out.println(", Max: " + runningMax.get());
  }
  return null;
});
```

For the other statistics, since they make use of key value pairs, we can't 
use static variables anymore.  The amount of state what we need to maintain
is potentially too big to fit on one machine.  So
for those stats, we'll make use of UpdateStateByKey which allows us to maintain
a value for every key in our dataset.
 
But before we can call updateStateByKey, we need to create a function to pass
in as it's argument.  UpdateStateByKey takes in a different reduce function.
While our previous sum reducer just took in two values and output their sum, this
reduce function takes in a current value and an iterator of values,
and outputs one new value.
```java
private static Function2<List<Long>, Optional<Long>, Optional<Long>>
   COMPUTE_RUNNING_SUM = (nums, current) -> {
     long sum = current.or(0L);
     for (long i : nums) {
       sum += i;
     }
     return Optional.of(sum);
   };
```

Now, we can compute the actual keyed statistics for all of time:
```java
// Compute Response Code to Count.
// Note the use of updateStateByKey.
JavaPairDStream<Integer, Long> responseCodeCount =
    accessLogs.mapToPair(s -> new Tuple2<>(s.getResponseCode(), 1L))
        .reduceByKey(SUM_REDUCER);
JavaPairDStream<Integer, Long> allResponseCodeCount =
    responseCodeCount.updateStateByKey(COMPUTE_RUNNING_SUM);
allResponseCodeCount.foreachRDD(rdd -> {
  System.out.println("Response code counts: " + rdd.take(100));
  return null;
});

// A DStream of endpoint to count.
JavaDStream<String> ipAddressesDStream = accessLogs
    .mapToPair(s -> new Tuple2<>(s.getIpAddress(), 1L))
    .reduceByKey(SUM_REDUCER)
    .updateStateByKey(COMPUTE_RUNNING_SUM)
    .filter(tuple -> tuple._2() > 10)
    .map(Tuple2::_1);
ipAddressesDStream.foreachRDD(rdd -> {
  List<String> ipAddresses = rdd.take(100);
  System.out.println("All IPAddresses > 10 times: " + ipAddresses);
  return null;
});

// A DStream of endpoint to count.
JavaPairDStream<String, Long> endpointCountsDStream = accessLogs
    .mapToPair(s -> new Tuple2<>(s.getEndpoint(), 1L))
    .reduceByKey(SUM_REDUCER)
    .updateStateByKey(COMPUTE_RUNNING_SUM);
endpointCountsDStream.foreachRDD(rdd -> {
  List<Tuple2<String, Long>> topEndpoints =
      rdd.takeOrdered(10, new ValueComparator<>(Comparator.<Long>naturalOrder()));
  System.out.println("Top Endpoints: " + topEndpoints);
  return null;
});
```

Run [LogAnalyzerStreamingTotal.java](java8/src/main/com/databricks/apps/logs/LogAnalyzerStreamingTotal.java)
now for yourself.

### Unittesting

Refactor this code for use in the sample app, and unittest it.






