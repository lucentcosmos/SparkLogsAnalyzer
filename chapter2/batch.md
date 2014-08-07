# Batch Data Import

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
store them on multiple volumes and track them.

HDFS and S3 are better
file systems for massive datasets, and this section will have examples shows
how to read data from those sources.

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

## S3

S3 is Amazon Web Services's solution for storing large files in the cloud.
This section covers how to sign up for an S3 account, put some files on it,
get your S3 credentials, and then run the Spark example with that input source.
On a production system, you want your EC2 compute nodes on the same zone
as your S3 file for speed as well as cost reasons.  While S3 files can be read
from other machines, it would take a long time and be expensive (Amazon S3 data
transfer prices differ if you read data within AWS vs. to somewhere else on the
internet).

[[TODO: Set up an S3 account, save a file, run the example]]

## HDFS

HDFS is a file system that is meant for storing large data sets and being fault
tolerant.  This section covers how to set up HDFS locally on your machine,
and run a batch import example on HDFS.  In a production system, your Spark cluster
should ideally be on the same machines as your Hadoop cluster to make it easy to
read files.  Note: Spark must be compiled with the same HDFS version as the one
on your clusters.

[[TODO: Install a local HDFS, and run the example.]]

Run [LogAnalyzerBatchImport.java](src/main/java/com/databricks/apps/logs/chapter2/LogAnalyzerBatchImport.java)
while copying files into HDFS.

Note that there are other Hadoop Compatible File Systems, and the same code
would work on any of them.
