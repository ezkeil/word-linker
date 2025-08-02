package com.ezkeil.wordlinker;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat; // Typically used for text files
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat; // Typically used for text output

import java.io.IOException;

public class DictionaryLinker {

    public static void main(String[] args) throws Exception {
        // Ensure that exactly two arguments are provided: input path and output path
        if (args.length != 2) {
            System.err.println("Usage: DictionaryLinker <input path> <output path>");
            System.exit(-1);
        }

        // Create a new Hadoop configuration
        Configuration conf = new Configuration();

        // Get an instance of the job. Give it a meaningful name.
        Job job = Job.getInstance(conf, "WordNet Linker");

        // Set the JAR file that contains your job classes.
        // This is crucial for Hadoop to find your Mapper, Reducer, and Driver classes.
        job.setJarByClass(DictionaryLinker.class);

        // Set the Mapper class for this job
        job.setMapperClass(LinkExtractorMapper.class);

        // Set the Reducer class for this job
        job.setReducerClass(LinkReducer.class);

        // Define the output key and value types for the Mapper
        // The Mapper outputs (Text, Text) -> (headword, linkedWord|RELATION_TYPE)
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        // Define the output key and value types for the Reducer
        // The Reducer outputs (Text, Text) -> (headword, comma_separated_links)
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Specify the InputFormat and OutputFormat classes
        // TextInputFormat treats each line of the input file as a record.
        // TextOutputFormat writes key-value pairs as lines of text.
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        // Set the input path for the job (where your wordnet_lemmas_input.txt will be in HDFS)
        FileInputFormat.addInputPath(job, new Path(args[0]));

        // Set the output path for the job (where the results will be written in HDFS)
        // Hadoop will create this directory, but it must not exist prior to job submission.
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Set the number of reducer tasks.
        // If you expect a large number of unique headwords and want to parallelize
        // the reduction, you can increase this. For initial testing, 1 is fine.
        // More than 1 reducer will produce multiple part-r-xxxxx files.
        // For WordNet, given its size, more than 1 reducer is often beneficial.
        // You can also let Hadoop decide based on input size.
        // job.setNumReduceTasks(1); // Uncomment if you want only one reducer for consolidated output

        // Submit the job and wait for it to complete
        // 'true' means verbose output to console
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}