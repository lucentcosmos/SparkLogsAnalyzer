# Sample Logs Analyzer Application

This directory contains code snippets in the chapters, assembled together to form
a sample logs analyzer application.  These are the features of our MVP
(minimal viable product) logs analyzer application.

* Read in new log files from a directory and input those new files into streaming.
* Compute stats on the logs using Spark SQL.
* Write the calculated stats to an txt file on the local file system that gets
  refreshed on a set interval.

You can use this simple application as a skeleton and combine features from 
the chapters to produce your own custom logs analysis application.
