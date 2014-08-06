# SparkLogsAnalyzer

This project demonstrates how easy it is to do logs analysis with Apache Spark.
Logs analysis is an ideal use case for Spark - it's a very large, common data source
and contains a rich set of information.  Spark allows you to store your logs
in files to disk cheaply, while still providing a
quick and simple way to process logs.  We hope this project will show you to use
Apache Spark on your organization's production logs and fully harness the power
of that data.  You can use logs data for better business and customer
intelligence, for building recommendation systems, for preventing fraud,
and much more.  

This project will also appeal to those who want to learn Spark and 
learn better by example.  Those readers can browse the chapters, see
what features of the logs analyzer is similar to their use case, and 
refashion the code samples in this project for their needs.

Additionally, this is meant to be a practical guide for using Spark in your
systems, so we'll even touch upon practical topics of which other technologies
complement Spark for log processing - such as using HDFS or S3 over 
NFS for storing your log files.

## How to use this project 

This project is broken up into chapters with bite-sized examples for 
demonstrating new Spark functionality for logs processing.  This makes
the examples easy to run and learn as they cover just one new topic at a time.
At the end, we put a few of these examples together to form a logs
analyzer application.

###[Chapter 1: Introduction to Apache Spark](chapter1/README.md)

The Apache Spark library is introduced, including RDD's, transformation, 
and actions.  We'll also introduce Spark SQL and Spark Streaming.  By the
end of this chapter, a reader will know how to do queries with Apache Spark.

###[Chapter 2: Importing and Exporting Data](chapter2/README.md)

This chapter includes examples to illustrate how to get your data into 
Spark - both batch import of data, as well as streaming data import
for Spark Streaming.  It also covers how to export the data processed with
Spark so that it can be served in your production systems.

### Chapter 3: More Spark

Chapter 3 is not yet written, but ideas for content include:

* Unittesting.
* Illustrate calling an API on the data, such as for geo locating an ipaddress.
* Joins.
* Broadcast variables.
* Accumulators.

### Chapter 4: Intelligence with Spark

Chapter 4 is not yet written, but the plan is to discuss using Spark on logs
data for more sophisticated applications.

* Auto detect if an endpoint is suddenly throwing an usually high number of
error response codes.
* Fraud detection with logs.
* With IPAddress as the user, and the endpoint being the movielens song, predict
what movies someone would like to see.

### Final Chapter: [Logs Analyis Application](app/README.md)

This section puts together some of the code in the other chapters to form
a log analysis application.  

## Final Word

This project is a work in progress, so please excuse if some of the examples
or documentation aren't refined.  Or better yet, submit a pull request to
contribute!