package com.aivik.wordspell.engine;

import com.aivik.wordspell.dict.EmptyClass;
import edu.uw.tcss558.team1.server.HibernateUtil;
import edu.uw.tcss558.team1.server.pojo.Dictionary;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * A custom spell checker.
 * 
 * @author Kevin Ewig
 */
public final class KSpellCheckEngine {

    private static Logger logger = Logger.getLogger(KSpellCheckEngine.class);

    /**
     * This lists the order which the word lists are to be added to the BKTree.
     * The more common words are added to the tree first, followed by longer and
     * more complicated words.
     */
    private static final int[] ORDER = { 3, 5, 7, 2, 4, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 };

    /**
     * Instance of the spell check engine.
     */
    private static KSpellCheckEngine instance = null;

    /**
     * The current instance of the BK Tree.
     */
    private BKTree tree = null;

    /**
     * Check if Spell Check engine is initialized.
     */
    private boolean initialized = false;

    /**
     * Make into singleton.
     */
    private KSpellCheckEngine() {
    }

    /**
     * Get this instance.
     */
    public static synchronized KSpellCheckEngine getInstance() {
        if (instance == null) {
            instance = new KSpellCheckEngine();
            Thread thread = new Thread() {

                @Override
                public void run() {
                    instance.initializeTreeWithHibernate();
                }
            };
            thread.start();
        }
        return instance;
    }

    /**
     * Check if the word exists in the dictionary.
     */
    public boolean isWordMisspelled(String aWord) {
        boolean result = false;
        if (aWord != null) {
            if (aWord.length() > 1) {
                if (aWord.matches("[a-zA-Z]+.+")) {
                    result = tree.isWordMisspelled(aWord);
                }
            }
        }
        return result;
    }

    /**
     * This method returns an array of corrections. Will only return 5
     * suggestions.
     *
     * @param aWord A misspelled word to get the spelling.
     */
    public List<BKSuggestion> getSpellingSuggestions(String aWord) throws Exception {
        List<BKSuggestion> listOfSuggestions = null;
        if (tree != null) {
            listOfSuggestions = tree.findSuggestion(aWord);
        }
        return listOfSuggestions;
    }

    /**
     * This method returns an array of corrections. Will return a specified max
     * number of suggestions.
     *
     * @param aWord A misspelled word to get the spelling.
     */
    public List<BKSuggestion> getSpellingSuggestions(String aWord, int aMaxNumberOfSuggestions) throws Exception {
        List<BKSuggestion> listOfSuggestions = null;
        if (tree != null) {
            listOfSuggestions = tree.findSuggestion(aWord, aMaxNumberOfSuggestions);
        }
        return listOfSuggestions;
    }

    /**
     * Insert a word into the dictionary. Used for the "add word to dictionary"
     * functionality.
     */
    void addWordToDictionary(String aWord) {
        tree.insertDictionaryWord(aWord, false, false);
    }

    /**
     * This will initialize the BK Tree with Hibernate.
     */
    private void initializeTreeWithHibernate() {
        Session session = null;
        try {
            try {
                int numberOfWordsLoaded = 0;
                SessionFactory factory = HibernateUtil.getSessionFactory();
                session = factory.openSession();
                session.beginTransaction();
                Query query = session.createQuery("from Dictionary");
                for (Iterator it = query.iterate(); it.hasNext(); ) {
                    Dictionary dictionary = (Dictionary) it.next();
                    String line = dictionary.getDicWord();
                    if (line != null) {
                        String word = null;
                        boolean plural = line.endsWith("/S");
                        boolean forbidden = line.endsWith("/X");
                        if (plural) {
                            int stringIndex = line.indexOf("/S");
                            word = new String(line.substring(0, stringIndex));
                        } else if (forbidden) {
                            int stringIndex = line.indexOf("/X");
                            word = new String(line.substring(0, stringIndex));
                        } else {
                            word = line.toString();
                        }
                        if (tree == null) {
                            tree = new BKTree();
                        }
                        tree.insertDictionaryWord(word, plural, forbidden);
                        numberOfWordsLoaded++;
                        if (numberOfWordsLoaded % 10000 == 0) {
                            logger.info("Loaded " + numberOfWordsLoaded + " words ");
                        }
                    }
                }
                initialized = true;
            } catch (Exception exception) {
                logger.error("Error", exception);
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        } catch (Exception exception) {
            logger.error("Error", exception);
        }
    }

    /**
     * This will initialize the BK Tree. 
     */
    private void initializeTree() {
        InputStreamReader reader = null;
        BufferedReader buffReader = null;
        try {
            for (int i = 0; i < ORDER.length; i++) {
                int index = ORDER[i];
                String indexName = index < 10 ? "0" + index : (index > 20 ? "big" : "" + index);
                URL url = EmptyClass.class.getResource("engchar" + indexName + ".dic");
                logger.info("... Loading: " + "engchar" + indexName + ".dic = {" + url + "}");
                reader = new InputStreamReader(url.openStream());
                buffReader = new BufferedReader(reader);
                String line = null;
                String word = null;
                do {
                    line = buffReader.readLine();
                    if (line != null) {
                        boolean plural = line.endsWith("/S");
                        boolean forbidden = line.endsWith("/X");
                        if (plural) {
                            int stringIndex = line.indexOf("/S");
                            word = new String(line.substring(0, stringIndex));
                        } else if (forbidden) {
                            int stringIndex = line.indexOf("/X");
                            word = new String(line.substring(0, stringIndex));
                        } else {
                            word = line.toString();
                        }
                        if (tree == null) {
                            tree = new BKTree();
                        }
                        tree.insertDictionaryWord(word, plural, forbidden);
                    }
                } while (line != null);
            }
            logger.debug("Loading supplemental dictionary...");
            List<String> listOfWords = KSupplementalDictionaryUtil.getWords();
            for (String word : listOfWords) {
                tree.insertDictionaryWord(word, false, false);
            }
            initialized = true;
        } catch (Exception exception) {
            logger.error("Error", exception);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                }
            }
            if (buffReader != null) {
                try {
                    buffReader.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * Check if spell check engine is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}
