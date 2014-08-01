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
The first step is to bring up a Spark context, load loglines from a text file 
into an RDD.  We use the parser to transform that to an RDD of ApacheAccessLog
objects.  Finally, before exiting the function, we stop the Spark context.

```java

// Create an RDD.

```
Note how in the above example, the access logs RDD is cached, since it will be
queried multiple times to compute all four statistics.  

Let's calculate the average, minimum, and maximum content size of the
response returned.  A map transformation to extract the content sizes, and 
then different actions(reduce, count, min, and max) are called to compute
the different stats.
```java

// Content Size Code.
  
```

Now, let's compute the total count for each response code.  For that, we'll
have to work with key value pairs.  Note that we use take(100) instead of 
collect() to gather the final output.  That's just to be safe since it 
could take a really long time to call collect() on a very large dataset.

```
// Top Stats code.
```

To return a list of IPAddresses that has accessed the server more than 10 times,
we'll use the filter transformation.
```
// IP Address code.
```

Last, let's calculate the top endpoints requested in this log file.
```
// Top Endpoint code.
```

These code snippets are from [LogAnalyzer.java](java8/src/main/com/databricks/apps/log/LogAnalyzer.java).
Now that we understand the code, try running that example.


## Spark SQL

For those of you who are familiar with SQL, the same statistics we calculated
in the previous example can be done using Spark SQL rather than calling
Spark transformations and actions directly.  We walk through how to do that
here.

First, we need to create a sql Spark context.
```
Insert code to create Spark context
```

Next, we need a way to register our logs data into a table.  In Java, Spark SQL
can infer the table schema on a standard Java POJO - with getters and setters
as we've done with [ApacheAccessLog.java](src/sajfdjdfjfd).  (Note: if you are
using a different language besides Java, there is a different way to infer the
table schema.  The examples in this directory work out of the box.  Or you can
also refer to the
[Spark Streaming Programming Guide](https://spark.apache.org/docs/latest/sql-programming-guide.html#rdds)
for more details.)
```
  // Add code to register table.
```

Now, we are 


Try running [LogAnalyzerSQL.java](java8/src/main/com/databricks/apps/log/LogAnalyzer.java) now.


## Spark Streaming

The earlier examples show us how to compute stats an old log file - but 
in a production system, we want live monitoring of the server logs.
To do that, we must use Spark Streaming.

To run the streaming examples, we'll tail a log file into netcat to send to Spark.
That is not the ideal way to get data into Spark in a production system,
but this is an easy workaround for now to demonstrate how to program in
Spark Streaming.  We will cover best practices for how to import data for Spark
Streaming in [Chapter 2](../chapter2/README.md).

[[INSERT DETAILS HERE ON HOW TO SET UP TAIL FOR STREAMING.]]


### Windows

A typical use case for logs analysis is live monitoring of the web server,
in which case we may only be interested in the logs for the last one hour of time,
and we want those statistics to refresh every minute.  One hour would be
the *window length*, while one minute would be the *slide interval*.  In this 
example, we use a window length of 30 seconds and a slide interval of
10 seconds since that is a comfortable choice for development.
  
Using the windows feature of Spark Streaming, makes it very easy to accomplish
this.

[[Insert code here.]]

Now, we can reuse the code from either two examples (regular or SQL) verbatim.
We've simply copied and pasted the code in this example, but you could refactor
this code nicely for reuse in your system.

We've included examples of Streaming calling the plain or SQL style Spark.


### UpdateStateByKey

If you really want to keep track of the log statistics for all of time, you'll
need to maintain the state of 





## More computations
[[TBW]]

### Joins

### Broadcast variables

### Accumulators



