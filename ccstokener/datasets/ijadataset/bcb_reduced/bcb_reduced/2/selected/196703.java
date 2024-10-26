package shieh.pnn.wm;

import shieh.pnn.Tools;
import shieh.pnn.core.Item;
import shieh.pnn.core.ItemSet;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

public class WordSet extends ItemSet {

    /**
	 * serialVersionUID
	 */
    private static final long serialVersionUID = 1230123600944902135L;

    private String resourceLocation = "resources/";

    private String[] vocNames = { "digits.in", "cdigits.in", "letters.in", "nonwords.in", "long.in" };

    public enum StandardVocabulary {

        DIGITS, CDIGITS, LETTERS, NONWORDS, LONGWORDS
    }

    ;

    private String confusable = "bpdtcv";

    private String nonconfusable = "rmuyso";

    public WordSet confusableItems = null;

    public WordSet nonconfusableItems = null;

    public class Word extends Item {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1824288837692049348L;

        public int[] phonemes;

        public Vector<double[]> inputSequence;

        public int getDuration() {
            return inputSequence != null ? inputSequence.size() : 0;
        }
    }

    protected PhonemeSet phonemeSet = new PhonemeSet();

    public WordSet() {
        this(null);
    }

    public WordSet(StandardVocabulary vocabulary) {
        String packageName = this.getClass().getPackage().getName();
        resourceLocation = packageName.replace(".", "/") + "/" + resourceLocation;
        if (vocabulary != null) loadVocabulary(vocabulary);
        if (vocabulary == StandardVocabulary.LETTERS) collectConfusables();
        if (vocabulary == StandardVocabulary.NONWORDS) {
            confusableItems = new WordSet();
            nonconfusableItems = new WordSet();
            for (int i = 0; i < size(); i++) if (i < size() / 2) confusableItems.add(get(i)); else nonconfusableItems.add(get(i));
        }
    }

    protected void collectConfusables() {
        confusableItems = new WordSet();
        nonconfusableItems = new WordSet();
        int i;
        Item item;
        for (i = 0; i < confusable.length(); i++) {
            String letter = confusable.substring(i, i + 1);
            item = getItemByLabel(letter);
            if (item != null) {
                confusableItems.add(item);
            }
        }
        for (i = 0; i < nonconfusable.length(); i++) {
            String letter = nonconfusable.substring(i, i + 1);
            item = getItemByLabel(letter);
            if (item != null) {
                nonconfusableItems.add(item);
            }
        }
    }

    public void setConfusables(String confusables, String nonconfusables) {
        this.confusable = confusables;
        this.nonconfusable = nonconfusables;
        collectConfusables();
    }

    public PhonemeSet getPhonemeSet() {
        return phonemeSet;
    }

    public boolean loadVocabulary(StandardVocabulary vocabulary) {
        String vocName = vocNames[vocabulary.ordinal()];
        URL url = ClassLoader.getSystemResource(resourceLocation + vocName);
        if (url == null) return false;
        return loadVocabulary(url);
    }

    public int getInputSize() {
        return phonemeSet.getInputSize();
    }

    /**
	 * Loads the vocabulary file by URL
	 * @param url -- the URL of the resource
	 */
    public boolean loadVocabulary(URL url) {
        clear();
        int ID = 0;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                String[] field = line.split("\t");
                if (field.length == 2) {
                    Word word = new Word();
                    word.label = field[0];
                    word.ID = ID++;
                    word.phonemes = parsePhonemes(field[1]);
                    word.inputSequence = phonemeSet.getWordInputCode(word.phonemes);
                    add(word);
                }
            }
            in.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
	 * Parses a string into a sequence of phonemes. The returned array could have a zero length 
	 * if no phonemes are recognized.
	 * 
	 * @param string the phonemic string
	 * @return an array of the sequence of parsed phonemes
	 */
    protected int[] parsePhonemes(String phoneticString) {
        String[] symbols = phoneticString.split("\\.");
        int[] phonemeArray = new int[symbols.length];
        int len = 0;
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i];
            if (symbol.length() > 0) {
                int phonemeID = phonemeSet.findPhoneme(symbol);
                if (phonemeID != -1) phonemeArray[len++] = phonemeID; else Tools.warning(String.format("unknown phoneme %s encountered in string %s.", symbol, phoneticString));
            }
        }
        return Arrays.copyOfRange(phonemeArray, 0, len);
    }
}
