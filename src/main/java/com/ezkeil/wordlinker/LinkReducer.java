package com.ezkeil.wordlinker;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

// The Reducer takes (Text, Iterable<Text>) as input,
// where the key is the headword and the values are all the
// linked words with their relation types (e.g., "fruit|GLOSS_MENTION", "entity|HYPERNYM").
// It outputs (Text, Text), where the key is the headword
// and the value is a comma-separated string of all its linked words and relation types.
public class LinkReducer extends Reducer<Text, Text, Text, Text> {

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        StringBuilder linksBuilder = new StringBuilder();
        boolean firstLink = true; // Flag to handle comma separation

        // Iterate through all values (linked words with relation types) associated with the current headword (key)
        for (Text val : values) {
            if (!firstLink) {
                linksBuilder.append(","); // Add a comma to separate links
            }
            linksBuilder.append(val.toString()); // Append the linked word and its relation type
            firstLink = false;
        }

        // Only write output if there were any links found for the headword
        if (linksBuilder.length() > 0) {
            // The output format will be: "headword\tlinkedWord1|RELATION_TYPE1,linkedWord2|RELATION_TYPE2,..."
            context.write(key, new Text(linksBuilder.toString()));
            // Increment a counter to track how many headwords were processed by the reducer
            context.getCounter("WordNet", "HeadwordsProcessedByReducer").increment(1);
        }
    }
}