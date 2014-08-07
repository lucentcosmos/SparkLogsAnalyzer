# Spark Streaming

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

In a terminal window, just run this command on a logfile which you will append to:
```
% tail -f [[YOUR_LOG_FILE]] | nc -lk 9999
```

If you don't have a log file that is actually been appended to live, you
can add lines manually with included data file or another file:
```
% cat ../../data/apache.access.log >> [[YOUR_LOG_FILE]]
```

When data is streamed into Spark, there are two common use cases, which will be covered:

1. [Windowed Calculations](windows.md) means that you only care about data received in the last N amount of time.  Perhaps for monitoring your web servers, you only care about what has happened in the last hour.  Spark Streaming conveniently splits the input data into windows for easy processing and drops any data that is beyond that window of time.
1. [Cumulative Calculations](total.md) means that you want to keep cumulative statistics and you want data to be streamed in to refresh those statistics.  In that case, you need to maintain the state for those statistics.  The Spark Streaming library also has some convenient functions for maintaining state to support that use case.
