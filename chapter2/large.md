# Exporting Large Datasets

If you are exporting a very large dataset, you can't call ```collect()``` or
a similar action to read all the data from the RDD onto the single driver
program.  Instead, call one of the save methods on the RDD
to write the data to files directly from the Spark workers.

## Save the RDD to files

RDD's have a number of built in methods for saving them to disk.  We'll
demonstrate the simple ```.saveAsTextFile()``` method.  This will write the data
to simple text files where the ```.toString()``` method is called on the object.

[[TODO: Insert example for saveAsTextFile]]

Run the example now - notice that number of files that are output depends on
the number of partitions of the RDD being saved.

Another common use case is to output to various Hadoop file formats.
Many of the Hadoop databases can load in data directly from files in a
specific format.  [Sqoop](http://http://sqoop.apache.org/) is a very useful tool that can bulk import large sets of Hadoop files into various databases.

Refer to the API documentation for other built in methods for saving to file.
There are different built in methods for the RDD's, so skim the whole
RDD package to see if there is something to suit your needs.

## Saving directly to a data storage

Of course, you could also write your own custom writer on all the elements in your RDD, but there's a lot of ways to write something that looks like it would work, but does not.  Here are some things to watch out for:

* Use partitioning to control the parallelism for writing to your data storage.  Your data storage may not support too many concurrent connections.
* Use batching for writing out multiple objects at a time if batching is optimal for your data storage.
* Make sure your write mechanism is resilient to failures.  Writing out a very large dataset can take a long time, which increases the chance something can go wrong - a network failure, etc.
* A common naive mistake is to open a connection on the Spark driver program, and then try to use that connection on the Spark workers.  The connection should be opened on the Spark worker, such as by calling ```forEachPartition``` and opening the connection inside that function.
* Consider utilizing a static pool of database connections on your Spark workers.
* If you are writing to a sharded data storage, partition your RDD to match your sharding strategy.  That way each of your Spark workers only connects to one database shard, rather than each Spark worker connecting to every database shard.

Be cautious when writing out so much data!
