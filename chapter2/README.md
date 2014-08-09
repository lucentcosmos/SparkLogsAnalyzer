# Chapter 2: Importing and Exporting Data

In the last chapter we covered how to get started with Spark for logs analysis,
but in those examples, we just pulled data in from a local file and printed
the statistics to standard out.  In this chapter, we go over techniques for
loading and exporting data that is suitable for a production system.  In particular, the techniques must scale to handle large production volumes of logs.

To scale, Apache Spark is meant to be deployed on a cluster of machines.  While you
could continue running the examples in local mode, it is recommended
that you set up a simple local cluster on your local machine and run
the remaining examples on that cluster - just to get practice
working with a Spark cluster.  See this page on the
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

Proceed on to the next sections:

* [Importing Data](import.md)
* [Exporting Data](export.md)
