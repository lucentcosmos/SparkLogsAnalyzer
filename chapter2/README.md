# Chapter 2: Importing and Exporting Data

In the last chapter we covered how to get started with Spark for logs analysis,
but in those examples, we just pulled data in from a local file and printed out
the statistics to standard out.  In this chapter, we go over techniques for
loading data into Apache Spark for a production system, as well as how to save
the computed data into a live system.

Note - some of the code from Chapter 1 has been nicely refactored into classes
for reuse in this example.  All that code should be familiar now.

## Importing Data into Apache Spark

Apache Spark is meant to be deployed on a cluster of machines.  While we
could continue running our examples locally on datasets that are small
enough to fit in memory on one machine, it's more fun to run our jobs on
more than one machine that can support more data.

[[Go over how to setup or deploy a Spark Cluster locally or on a cluster of machines.]]

### Batch Import

This section covers batch importing data into Apache Spark, such as
seen in the non-streaming examples from Chapter 1.  Those examples load data
from a file all at once into one RDD, processes that RDD, the job completes,
and the program exits.  In a production system, you could set up a cron job to
kick off a batch job each night to process the last day's worth of log files and 
then publish the last days worth of statistics.

To support batch import of data, the data needs to be a file system that
is accessible by all machines on the Spark cluster.  Files that are on
only one machine cannot be input to Spark.  These are some recommended file
systems for storing data that Spark can read:

* NFS or some other network file system that all machines on the cluster can read from.
* HDFS - Ideally, the Spark cluster would be deployed on the same machines.
* S3 is a cheap place to store files and easy to set up.

We'll cover how to set up and text HDFS and S3 as input sources for Spark.

Spark is also compatible with many databases for import and more may
be added soon - check the
[Apache Spark Examples Directory](https://github.com/apache/spark/tree/master/examples)
which contains more examples that may be useful to you.  Most likely, you
aren't going to be reading your logs data in from databases, but there
may be other data you want to input to Spark from a database.

#### HDFS

This section covers how to set up HDFS locally on your machine, and run
a streaming example on HDFS.

In a production system, your Spark cluster should ideally be on the same
machines as your Hadoop cluster to make it easy to read files.

Spark must be compiled with the same HDFS version as the one on your clusters.

[[Install a local HDFS, and run the example.]]

Run [LogAnalyzerBatchImport.java]() while copying files into HDFS.
 
#### S3

This section covers how to sign up for an S3 account, put files on it,
get your S3 credentials, and then run the Spark example with that input source.

On a production system, you want your EC2 compute nodes on the same zone 
as your S3 files.  While this Spark code can still run, it would take a long
time and be expensive to read all that data onto other machines.

[[Set up an S3 account, save a file, run the example]]

#### Database?

[[Load data about an IPAddress or User account or something like that
from a database?]]

### Streaming Import

This section covers importing data for streaming.  The streaming example in the
previous chapter just received data through a single socket - which is not
a scalable solution.  In a real production system, there are many of servers 
continuously writing logs, and we want to process all of those files.  This
section contains scalable solutions for data import.  Since streaming is now
used, there is no longer the need for a night cron to launch a new batch 
job, but instead - this logs processing program can be long-lived - continuously
receiving new logs data, processing the data, and computing refreshed stats on the
fly.

#### Built in methods of Streaming Context

The StreamingContext has many built in methods for importing data for streaming.
We used socketTextStream in the previous chapter, and we'll use textFileStream
here.  The textFileStream method monitors a Hadoop-compatible filesystem for new
files and reads them as text files.  Just replace the old call to socketTextStream
with textFileStream, passing in the directory to monitor for log files.

```java
// This methods monitors a directory for new files to read in for streaming.
JavaDStream<String> logData = jssc.textFileStream(directory);
```

Try running [LogAnalyzerStreamingImportDirectory.java](src/main/java/com/databricks/apps/logs/chapter2/LogAnalyzerStreamingImportDirectory.java)
by specifying a directory, and copying some log files into
that directory so that the logs analysis application can pick those files up
for reading.

There are more built in methods - check them out in the reference API documents
for the StreamingContext.

#### KafkaInputDStream

While the previous example picks up new log files right away - the log
files aren't copied over until a long time after the HTTP requests in the logs
happend.  To get up-to-date logs processing, we need a way to send over 
log lines immediately.  Kafka is a high-throughput distributed
message system that is perfect for that use case.

[[Pointers to Kafka documentation to set up a local kafka.]]

[[Sample Code to write a logline to Kafka.  (Is there a way to configure
the Apache Logger to do that automatically?)]]

[[Explain how to use the KafkaInputDStream.]]

## Exporting Data out of Apache Spark

