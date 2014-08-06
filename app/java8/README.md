# Chapter 1: Logs Analzyer Reference App in Java 8.

To compile this code, use maven:
```
% mvn package
```

To run the program, you can use spark-submit program:
```
%  ${YOUR_SPARK_HOME}/bin/spark-submit
   --class "com.databricks.apps.logs.LogsAnalyzerReferenceAppMain"
   --master local[4]
   YOUR_LOCAL_LOGS_DIRECTORY
   SOME_FILE_ON_YOUR_SYSTEM.html
   target/log-analyzer-1.0.jar
```

Drop new apache access log files into the specified logs directory.
Open the local html page in a browser and refresh it to see an 
update set of statistics from your log files.
