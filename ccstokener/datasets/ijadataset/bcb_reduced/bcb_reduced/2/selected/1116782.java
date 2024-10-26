package spider.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;

/**
 * The class <code>Stopper</code> is used to remove stop words
 *
 * @author Gautam Pant
 */
public class Stopper {

    Hashtable stopWords = new Hashtable();

    public Stopper(String stopWordsFile) {
        try {
            BufferedReader br = null;
            FileReader fr = null;
            if (stopWordsFile.startsWith("http")) {
                URL url = new URL(stopWordsFile);
                br = new BufferedReader(new InputStreamReader(url.openStream()));
            } else {
                fr = new FileReader(new File(stopWordsFile));
                br = new BufferedReader(fr);
            }
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                stopWords.put(line, "");
            }
            fr.close();
        } catch (Exception e) {
            System.out.println("Stopwords not Found");
            return;
        }
    }

    public boolean isStopWord(String word) {
        String lword = word.toLowerCase();
        return stopWords.containsKey(lword);
    }

    public String stopString(String text) {
        String[] terms = text.split("[_\\W+]");
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < terms.length; i++) {
            if (isStopWord(terms[i])) {
                continue;
            }
            sb.append(terms[i]).append(" ");
        }
        return sb.toString().trim();
    }
}
