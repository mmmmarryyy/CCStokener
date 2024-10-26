package com.azureus.plugins.aztsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;

/**
 * Implements generic support for all torrent search engines. A specific
 * torrent search engine must extend this class.
 * 
 * @version 0.4
 * @author Dalmazio Brisinda
 * 
 * <p>
 * This software is licensed under the 
 * <a href="http://creativecommons.org/licenses/GPL/2.0/">CC-GNU GPL.</a>
 */
public abstract class TSEngine {

    private static final String HTML_NBSP_FILTER_RE = "&nbsp;";

    private static final String HTML_BREAK_FILTER_RE = "\\<(?:/)?br\\>";

    private static final String HTML_FILTER_RE = HTML_NBSP_FILTER_RE + "|" + HTML_BREAK_FILTER_RE;

    /**
	 * Debugging member to keep track of the match count for the current matcher.
	 */
    protected int debugMatchCount;

    /**
	 * The TSController instance that provides access to user interface options.
	 */
    protected TSController controller;

    /**
	 * The TSSearchResultsManager instance that manages the search results.
	 */
    protected TSSearchResultsManager resultsManager;

    /**
	 * The number of results so far for the current search engine. It is the 
	 * responsibility of subclasses to update this instance variable so that it
	 * is meaningful.
	 */
    protected int resultsCount;

    /**
	 * Indicates if the search should be terminated. It is the responsibility of
	 * subclasses to update this instance variable so that it is meaningful
	 * under the desired conditions.
	 */
    protected boolean terminateSearch;

    /**
	 * Constructs a new search engine and initializes search engine instance
	 * variables.
	 * 
	 * @param controller the main controller for the plugin arbitrating between
	 * the model (search results) and the GUI view.
	 * 
	 * @param resultsManager the search results manager for all searches.
	 */
    TSEngine(TSController controller, TSSearchResultsManager resultsManager) {
        this.controller = controller;
        this.resultsManager = resultsManager;
        resultsCount = 0;
        terminateSearch = false;
    }

    /**
	 * All classes that extend this class are required to override this method
	 * by first calling the parent's version of this method as it ensures
	 * various instance variables are properly initialized. The method should
	 * perform the search by sending the query string out to the appropriate
	 * torrent site, collecting, parsing, and organizing the results via the
	 * TSSearchResultsManager.
	 * 
	 * Note: if the performSearch() method will be called more than once for the
	 * same instance then it is important to reinitialize the terminateSearch
	 * instance variable to false in case the previous search set it to true.
	 * 
	 * @param query the search string trimmed and encoded for URL submission.
	 */
    public void performSearch(String query) {
        resultsCount = 0;
        terminateSearch = false;
    }

    /**
	 * Get the specified HTML page. This method employs a standard
	 * URL.openConnection() call and reading from the resulting InputStream. If 
	 * special behavior is required (like setting cookies or headers etc.) then
	 * this method should be overridden. Note: All "&amp;nbsp;" escape codes and
	 * &lt;br&gt; and &lt;/br&gt; HTML tags are replaced with a space character
	 * in order to simplify parsing.
	 * 
	 * @param url the URL of the page to fetch.
	 * @return the HTML page as a string or null if an error occurred.
	 */
    public String getHtmlPage(URL url) {
        String html = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            html = sb.toString().replaceAll(HTML_FILTER_RE, " ");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }

    /**
	 * Show the contents of a regular expression match. The contents of the most
	 * recent match is printed to the console.
	 * 
	 * @param m the regular expression Matcher object.
	 */
    protected void showRegularExpressionMatch(Matcher m) {
        System.out.println();
        System.out.println("Match: " + ++debugMatchCount);
        System.out.println("GroupCount: " + m.groupCount());
        for (int i = 1; i <= m.groupCount(); i++) {
            System.out.println("Group " + i + ": " + m.group(i));
        }
        System.out.flush();
    }
}
