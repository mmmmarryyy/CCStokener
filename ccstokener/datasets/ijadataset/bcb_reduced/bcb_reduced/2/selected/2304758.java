package sunw.demo.searchdemo;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.EventListener;
import java.util.Locale;
import javax.help.search.*;
import javax.help.search.SearchEvent;
import javax.help.search.SearchListener;

/**
 * ClientSearchEngine is the default search engine. 
 *
 * Search results are returned through SearchEvents to
 * listeners that
 * register with a ClientSearchEngine instance.
 *
 * @author Roger D. Brinkley
 * @version	1.3	10/29/97
 *
 * @see java.help.SearchEngine
 * @see java.help.SearchEvent
 * @see java.help.SearchListener
 */
public class ClientSearchEngine extends SearchEngine {

    private WordVector wordVec;

    private DocVector docVec;

    /**
     * The "and" modifier specifies that the search return pages
     * that contain both strings in the search entry.
     */
    public static int AND = 1;

    /**
     * The "or" modifier specifies that the search return pages
     * that contain either of the entry strings.
     */
    public static int OR = 2;

    /**
     * The "not" modifier specifies that the search return pages
     * in which the first string occurs and the second string
     * does not occur.
     */
    public static int NOT = 3;

    /**
     * The "near" modifier specifies that the search return pages
     * in which the first string occurs withing 20 words of
     * the second string.  This is the default behavior when
     * no search modifiers are specified.
     */
    public static int NEAR = 4;

    /**
     * ( LEFT PAREN
     */
    public static int LEFT_PAREN = 5;

    /**
     * ) RIGHT PAREN
     */
    public static int RIGHT_PAREN = 6;

    /**
     * The "adj" modifier specifies that the search return pages
     * that contain search entries directly adjacent to
     * each other. Equivalent to enclosing
     * the entries in quotation marks ("").
     */
    public static int ADJ = 7;

    /**
     * Create a ClientSearchEngine 
     */
    public ClientSearchEngine(URL base, Hashtable params) {
        super(base, params);
        URL url;
        URLConnection uc;
        DataInputStream from;
        debug("Loading Search Database");
        try {
            String urldata = (String) params.get("data");
            debug("base=" + base.toExternalForm());
            debug("urldata=" + urldata);
            url = new URL(base, urldata + ".inv");
            debug("url: " + url);
            uc = url.openConnection();
            uc.setAllowUserInteraction(true);
            from = new DataInputStream(new BufferedInputStream(uc.getInputStream()));
            wordVec = new WordVector(from);
            url = new URL(base, urldata + ".dat");
            debug("url: " + url);
            uc = url.openConnection();
            uc.setAllowUserInteraction(true);
            from = new DataInputStream(new BufferedInputStream(uc.getInputStream()));
            docVec = new DocVector(from);
            debug("Search Database loaded");
        } catch (Exception e) {
            wordVec = null;
            docVec = null;
            debug("Failed to load Search DataBase");
            e.printStackTrace();
        }
    }

    public SearchQuery createQuery() {
        return new ClientSearchQuery(this);
    }

    public WordVector getWordVector() {
        return wordVec;
    }

    public DocVector getDocVector() {
        return docVec;
    }

    public URL getBase() {
        return base;
    }

    /**
     * For printf debugging.
     */
    private static boolean debugFlag = false;

    private static void debug(String str) {
        if (debugFlag) {
            System.out.println("ClientSearchEngine: " + str);
        }
    }
}
