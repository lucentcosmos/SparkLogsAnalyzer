# Chapter 2: Importing and Exporting Data

In the last chapter we covered how to get started with Spark for logs analysis,
but in those examples, we just pulled data in from a local file and printed out
the statistics to standard out.  In this chapter, we go over techniques for
loading data into Apache Spark for a production system, as well as how to export
the computed data into your production systems.  In particular, the system 
must to scale to handle the large production volumes of logs data.

Note - some of the code from Chapter 1 has been nicely refactored into classes
for reuse in this example.  All that code should be familiar now.

## Importing Data into Apache Spark

Apache Spark is meant to be deployed on a cluster of machines.  While you
could continue running the examples locally on data sets that are small
enough to fit in memory on one machine, it's more fun to run our jobs on
more than one machine that can support more data.  It is recommended
that you set up a simple one node cluster on your local machine and run
the remaining examples on that local cluster. See this page on the
[Spark Standalone Mode](https://spark.apache.org/docs/latest/spark-standalone.html)
for instructions.

Once you get your local Spark cluster up:
* Use spark-submit to run your jobs rather than using the JVM parameter.  Run one of the
examples from the previous chapter to check your set up.
* Poke around and familiarize with the web interfaces for Spark.

Even if you choose to continue running your examples locally, it's important
to understand the concept that Spark runs on a cluster of machines with a driver
and a set of workers.
Read the [Spark Cluster Overview Guide](https://spark.apache.org/docs/latest/cluster-overview.html).
The explanations below touch upon those concepts.

### Batch Import

This section covers batch importing data into Apache Spark, such as
seen in the non-streaming examples from Chapter 1.  Those examples load data
from a file all at once into one RDD, processes that RDD, the job completes,
and the program exits.  In a production system, you could set up a cron job to
kick off a batch job each night to process the last day's worth of log files and 
then publish statistics for the last day.

To support batch import of data on a Spark cluster, the data needs to be on a
file system that is accessible by all machines on the cluster.  Files that are
only accessible to one worker machine and cannot be read by the others will
cause failures.

NFS or some other network file system would suffice, but NFS isn't fault tolerant
to machine failures and if your dataset is too
big to fit on one NFS volume, it could be cumbersome to partition your data and
store them on multiple volumes and track them.  Instead, HDFS and S3 are better
file systems for massive datasets, and this section will cover how to
read data from those sources.

* S3 is an Amazon AWS solution for storing files in the cloud, easily
accessible to all.
* HDFS is a distributed file system on that can be installed on a cluster of
machines.  It can not only accommodate a lot of data, but is also fault tolerant
should one of the machines in the cluster go down.

Spark is also compatible with many databases for import and more may
be added soon - we won't go over them all here - refer to the
[Apache Spark Examples Directory](https://github.com/apache/spark/tree/master/examples)
which contains more examples that may be useful to you.  Most likely, you
aren't going to be reading your logs data in from a databases, but there
may be other data you want to input to Spark.

#### S3

S3 is Amazon Web Services's solution for storing large files in the cloud.
This section covers how to sign up for an S3 account, put some files on it,
get your S3 credentials, and then run the Spark example with that input source.
On a production system, you want your EC2 compute nodes on the same zone 
as your S3 file for speed as well as cost reasons.  While S3 files can be read
from other machines, it would take a long time and be expensive (Amazon S3 data
transfer prices differ if you read data within AWS vs. to somewhere else on the
internet).

[[TODO: Set up an S3 account, save a file, run the example]]

#### HDFS

HDFS is a file system that is meant for storing large data sets and being fault
tolerant.  This section covers how to set up HDFS locally on your machine,
and run a streaming example on HDFS.  In a production system, your Spark cluster
should ideally be on the same machines as your Hadoop cluster to make it easy to
read files.  Note: Spark must be compiled with the same HDFS version as the one
on your clusters.

[[TODO: Install a local HDFS, and run the example.]]

Run [LogAnalyzerBatchImport.java](src/main/java/com/databricks/apps/logs/chapter2/LogAnalyzerBatchImport.java)
while copying files into HDFS.

Note that there are other Hadoop Compatible File Systems, and this same code
would work on those systems as well.
 
### Streaming Import

This section covers importing data for streaming.  The streaming example in the
previous chapter received data through a single socket - which is not
a scalable solution.  In a real production system, there are many servers 
continuously writing logs, and we want to process all of those files.  This
section contains scalable solutions for data import.  Since streaming is now
used, there is no longer the need for a nightly batch job to process logs,
but instead - this logs processing program can be long-lived - continuously
receiving new logs data, processing the data, and computing refreshed stats on the
fly in your production system.

#### Built in methods of Streaming Context

The StreamingContext has many built in methods for importing data for streaming.
We used socketTextStream in the previous chapter, and we'll use textFileStream
here.  The textFileStream method monitors any Hadoop-compatible filesystem for new
files and when it detects a new one - reads it in for streaming as a text file.
Just replace the old call to socketTextStream with textFileStream,
and pass in the directory to monitor for log files. 

```java
// This methods monitors a directory for new files to read in for streaming.
JavaDStream<String> logData = jssc.textFileStream(directory);
```

Try running [LogAnalyzerStreamingImportDirectory.java](src/main/java/com/databricks/apps/logs/chapter2/LogAnalyzerStreamingImportDirectory.java)
by specifying a directory.   You'll also need to drop/copy some new log files
manually into that directory while the program is running.

There are more built-in input methods for streaming - check them out in the
reference API documents for the StreamingContext.

#### KafkaInputDStream

While the previous example picks up new log files right away - the log
files aren't copied over until a long time after the HTTP requests in the logs
actually occurred, so we don't get up to date log statistics.  To get real time
logs processing, we need a way to send over log lines immediately.  Kafka is a
high-throughput distributed message system that is perfect for that use case.

Here is some useful documentation to set up with Kafka in streaming:

* [Kafka Documentation](http://kafka.apache.org/documentation.html)
* [KafkaUtils class in the external module of the Spark project](https://github.com/apache/spark/blob/master/external/kafka/src/main/scala/org/apache/spark/streaming/kafka/KafkaUtils.scala)
* [Spark Streaming Example of using Kafka](https://github.com/apache/spark/blob/master/examples/src/main/java/org/apache/spark/examples/streaming/JavaKafkaWordCount.java)

## Exporting Data out of Spark

This section contains talks about exporting data out of Spark into your
other production systems.  First, you'll have to figure out if your output data
is small (meaning can fit on memory on one machine) or large
(too big to fit into memory on one machine).

* Small Datasets - If you have a small dataset, you can call an action on 
this dataset to retrieve objects in memory on the driver program, and then write
those objects out any way you want.
* Large Datasets - For a large dataset, it's important to remember that this
dataset is too large to fit in memory on the driver program.  In that case, you
can either call Spark to write the data to files directly or you can have a
custom writer that you can call in a map function to output the data.

### Exporting a small output dataset

If the data you are exporting out of Spark is small, you can just use an
action to convert the RDD into objects in memory on the driver program, and then
write that output directly to any data storage solution of your choosing.
You may remember that we called the take(N) action where N
is some finite number instead of the collect() action to ensure the output
fits in memory - no matter how big the input data set may be - this is good
practice.  This ection walks through example code where you'll write the
log statistics to a file.

```java
LogStatistics logStatistics = logAnalyzerRDD.processRdd(accessLogs);

String outputFile = args[1];
Writer out = new BufferedWriter(
    new OutputStreamWriter(new FileOutputStream(outputFile)));

Tuple4<Long, Long, Long, Long> contentSizeStats =
    logStatistics.getContentSizeStats();
out.write(String.format("Content Size Avg: %s, Min: %s, Max: %s\n",
    contentSizeStats._1() / contentSizeStats._2(),
    contentSizeStats._3(),
    contentSizeStats._4()));

List<Tuple2<Integer, Long>> responseCodeToCount =
    logStatistics.getResponseCodeToCount();
out.write(String.format("Response code counts: %s\n", responseCodeToCount));

List<String> ipAddresses = logStatistics.getIpAddresses();
out.write(String.format("IPAddresses > 10 times: %s\n", ipAddresses));

List<Tuple2<String, Long>> topEndpoints = logStatistics.getTopEndpoints();
out.write(String.format("Top Endpoints: %s\n", topEndpoints));

out.close();
```

Now, run [LogAnalyzerExportSmallData.java](src/main/java/com/databricks/apps/logs/chapter2/LogAnalyzerExportSmallData.java).

It may not be that useful to have these stats output to a file - in an actual
production system, you would probably write these stats to a database instead.

### Exporting a large output dataset

If you are exporting a very large dataset, you can't call collect() or 
a similar action to read all the data from the RDD onto the single driver
program.  Instead, you can either call one of the save methods on the RDD
to write the data to files.  Or if you want to output the data to a
different storage solution, then call map on the RDD and implement a
custom writer.

#### Save the RDD to files by calling save methods

RDD's have a number of built in methods for saving them to disk.  We'll
demonstrate the simple .saveAsTextFile() method.  This will write the data
to simple text files where the .toString method is called on the object.

[[TODO: Insert example for saveAsTextFile]]

Run the example now - notice that number of files that are output depends on
the number of partitions of the RDD being saved.

Another common use case is to output to various Hadoop file formats.
Many of the Hadoop databases can load in data directly from files in a 
specific format.

Refer to the API documentation for other built in methods for saving to file.  
There are different built in methods for the RDD's, so skim the whole
RDD package to see if there is something to suit your needs.

#### Map the RDD to call your own custom writer

Alternately, you can create your own classes to write your data out to 
a custom data storage system.  You can call map on your RDD, and then call your
own method for saving the data.  There are some big considerations though to
watch out for though when writing a large data set:

* Use partitioning to control the parallelism for writing to your data storage.
Multiple Spark workers will be calling map at the same time.
If you are writing out to files, it may be fine to have many file
handles open, but if you are writing a database, the number of concurrent 
connections your database can handle may be small.
* Considering making use of batching for writing out multiple objects at a time
rather than write them out one by one.

For example, a common way to store a large dataset is in sharded SQL.
If writing out to a set of sharded SQL databases, you might do
the following:

* Split your large dataset into multiple RDD's - one RDD per shard.
* Repartition each RDD to be the number of concurrent connections
  each database shard can handle for writing.
* Use batching when doing the actual writes to the database -
  choose a good batch size for your writes.

[[TODO: Example code writing to SQL in a batch.]]
