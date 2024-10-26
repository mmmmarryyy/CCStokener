package pl.kernelpanic.dbmonster;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Dictionary Manager.
 *
 * @author Piotr Maj &lt;pm@jcake.com&gt;
 *
 * @version $Id: DictionaryManager.java,v 1.2 2006/01/05 16:29:37 majek Exp $
 */
public class DictionaryManager {

    /**
     * Holds dictionaries. As  keys canonical pathnamea are used.
     */
    private Map dictionaries = new HashMap();

    /**
     * Random number generator.
     */
    private Random random = null;

    /**
     * Sets the random number generator.
     *
     * @param rnd random number generator
     */
    public void setRandom(Random rnd) {
        random = rnd;
    }

    /**
     * Returns a dictionary.
     *
     * @param schemaPath the schema file home
     * @param path pathname
     *
     * @return a dictionary
     *
     * @throws Exception if dictionary cannot be obtained
     */
    public Dictionary getDictionary(String schemaPath, String path) throws Exception {
        File dictFile = new File(path);
        if (!dictFile.isAbsolute()) {
            dictFile = new File(schemaPath, path);
        }
        String key = dictFile.getCanonicalPath();
        if (dictionaries.containsKey(key)) {
            return (Dictionary) dictionaries.get(key);
        } else {
            loadDictionary(key);
            return (Dictionary) dictionaries.get(key);
        }
    }

    /**
     * Loads a dictionary using specified url.
     *
     * @param url url
     *
     * @return dictionary
     *
     * @throws Exception if dictionary cannot be loded.
     */
    public Dictionary getDictionary(URL url) throws Exception {
        if (dictionaries.containsKey(url.toString())) {
            return (Dictionary) dictionaries.get(url.toString());
        } else {
            loadDictionary(url);
            return (Dictionary) dictionaries.get(url.toString());
        }
    }

    /**
     * Loads a dictionary.
     *
     * @param path a canonical path of the file
     *
     * @throws Exception if dictionary could not be loaded.
     */
    private void loadDictionary(String path) throws Exception {
        File f = new File(path);
        if (!f.exists() || !f.canRead()) {
            throw new Exception("Cannot access dictionary file + " + path);
        }
        InputStream is = null;
        if (path.endsWith(".zip")) {
            ZipFile zf = new ZipFile(f);
            ZipEntry ze = (ZipEntry) zf.entries().nextElement();
            is = zf.getInputStream(ze);
        } else if (path.endsWith(".gz")) {
            is = new GZIPInputStream(new FileInputStream(f));
        } else {
            is = new FileInputStream(f);
        }
        readDictionary(path, is);
    }

    /**
     * Loads a dictionary.
     *
     * @param url url
     *
     * @throws Exception if dictionary could not be loaded.
     */
    private void loadDictionary(URL url) throws Exception {
        InputStream is = new GZIPInputStream(url.openStream());
        readDictionary(url.toString(), is);
    }

    /**
     * Reads a dictionary and adds it to the list.
     *
     * @param key a key
     * @param is input stream
     *
     * @throws Exception exception
     */
    private void readDictionary(String key, InputStream is) throws Exception {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        String line = null;
        Dictionary dictionary = new Dictionary();
        dictionary.setName(key);
        dictionary.setRandom(random);
        while ((line = reader.readLine()) != null) {
            dictionary.addItem(line);
        }
        dictionaries.put(key, dictionary);
    }
}
