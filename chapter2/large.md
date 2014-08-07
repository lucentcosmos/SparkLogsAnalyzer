# Exporting Large Datasets

If you are exporting a very large dataset, you can't call collect() or
a similar action to read all the data from the RDD onto the single driver
program.  Instead, you can either call one of the save methods on the RDD
to write the data to files.  Or if you want to output the data to a
different storage solution, then call map on the RDD and implement a
custom writer.

## Save the RDD to files by calling save methods

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

## Map the RDD to call your own custom writer

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
