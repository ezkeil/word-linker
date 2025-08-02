import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import edu.mit.jwi.Dictionary; // JWI Library
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS; // Part of Speech
import edu.mit.jwi.item.Pointer; // WordNet relations
import edu.mit.jwi.data.ILoadPolicy;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LinkExtractorMapper extends Mapper<LongWritable, Text, Text, Text> {

    private IDictionary dict;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b"); // Simple word extraction

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        // The path must match where WordNet data is copied in the Dockerfile
        String path = "/app/wordnet_data/"; // This is correct now
        URL url = new URL("file", null, path);

        dict = new Dictionary(url);
        dict.open();
        // dict.load(true); // Consider loading into memory if performance is key and RAM allows
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        if (dict != null) {
            dict.close();
        }
        super.cleanup(context);
    }

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // In WordNet, you won't get lines like "word: definition".
        // Instead, you'll iterate through synsets or words directly via the JWI API.
        // The 'value' in this case might just be an arbitrary input to trigger map()
        // OR you might list all POS types and process them one by one.

        // A better approach for WordNet:
        // Instead of reading 'value' as a line, the Mapper could receive
        // the POS as a key and then iterate through all words/synsets of that POS.
        // Or, you could pre-process WordNet to flatten it into one "word: definition" file,
        // but that defeats the purpose of using its structured relations.

        // Let's assume for a moment the 'value' is a word, and we want to find its links.
        // A more robust way would be to get ALL synsets and process them.

        // Example: Iterate through all Noun synsets and extract relationships
        // This is a simplified example. For a real job, you might want to process
        // each Synset as a separate record in the Mapper, or flatten the WordNet structure
        // into a format Hadoop can consume easily (e.g., one line per synset, with gloss and pointers).

        // If you want to process ALL words/synsets:
        // You could have a dummy input file for your Hadoop job (e.g., just one line "start")
        // and the first Mapper instance would iterate through WordNet.
        // Or, better, create a custom InputFormat for WordNet.

        // For simplicity, let's assume the Mapper receives a 'Synset ID' or 'Word String'
        // as its input 'value' from a pre-processed list.
        // For demonstration, let's process one word (value) and find its links

        String inputWordStr = value.toString().toLowerCase().trim();
        if (inputWordStr.isEmpty()) return;

        // Get all senses/synsets for the input word across all POS
        for (POS pos : POS.values()) { // Iterate through Noun, Verb, Adj, Adv
            IIndexWord idxWord = dict.getIndexWord(inputWordStr, pos);
            if (idxWord != null) {
                for (IWordID wordID : idxWord.getWordIDs()) {
                    IWord word = dict.getWord(wordID);
                    ISynset synset = word.getSynset();

                    // 1. Extract links from the gloss (definition)
                    String gloss = synset.getGloss();
                    Matcher matcher = WORD_PATTERN.matcher(gloss.toLowerCase());
                    while (matcher.find()) {
                        String linkedWord = matcher.group();
                        // Check if the extracted word is a known lemma in WordNet
                        if (dict.getIndexWord(linkedWord, POS.NOUN) != null ||
                            dict.getIndexWord(linkedWord, POS.VERB) != null ||
                            dict.getIndexWord(linkedWord, POS.ADJECTIVE) != null ||
                            dict.getIndexWord(linkedWord, POS.ADVERB) != null) {
                            context.write(new Text(inputWordStr), new Text(linkedWord + "|GLOSS_MENTION"));
                        }
                    }

                    // 2. Extract explicit relationships (pointers)
                    for (Pointer ptr : Pointer.values()) {
                        for (ISynsetID relatedSynsetId : synset.getRelatedSynsets(ptr)) {
                            ISynset relatedSynset = dict.getSynset(relatedSynsetId);
                            for (IWord relatedWord : relatedSynset.getWords()) {
                                // Filter out self-loops if desired, and only emit meaningful relations
                                if (!relatedWord.getLemma().equalsIgnoreCase(inputWordStr)) {
                                    context.write(new Text(inputWordStr), new Text(relatedWord.getLemma() + "|" + ptr.getName()));
                                }
                            }
                        }
                    }
                    // You can also get relations for individual words within a synset:
                    // for (IWord relatedWord : word.getRelatedWords()) { ... }
                }
            }
        }
    }
}