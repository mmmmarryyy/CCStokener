package com.io_software.catools.search;

import java.io.Reader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URL;
import java.util.Set;
import java.util.Locale;
import java.util.HashSet;
import java.util.Date;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.REMatchEnumeration;
import gnu.regexp.CharIndexedReader;
import com.abb.util.TeeReader;

/** Takes a reader returned from the Yahoo search engine and
    parses its output into {@link WebSearchResult} objects. The filter will, if
    the link exists, also instantiate the <em>Web Sites</em> results reported
    by Yahoo, but it won't go to the <em>Web Pages</em> section, only if Yahoo
    doesn't return any other results. This is because the <em>Web Pages</em>
    section is basically the standard Google engine which is already covered by
    {@link GoogleWebForm}.<p>
    
    <b>Note:</b> This implementation of the {@link Searchable} interface is for
    demonstration purposes only. Be sure not to use this implementation
    commercially but only for your personal use. See also <a
    href=http://docs.yahoo.com/info/terms/>Yahoo's Terms of
    Use</a>.<p>

    There is one specialty about this reader filter: It can be used both for
    the "Web Sites" page and the category results page. Unfortunately, Yahoo
    displays a "Web Sites" link also on the "Web Sites" page itself. If the
    reader didn't know in which context it operates it would run into an
    endless recursion following the "Web Sites" links. Therefore, an additional
    constructor has been provided telling the filter to operate in "Web Sites"
    mode.
    
   @author Axel Uhl
   @version $Id: YahooReaderFilter.java,v 1.16 2001/05/03 10:03:47 aul Exp $
 */
public class YahooReaderFilter extends ReaderFilter {

    /** empty constructor that may catch the exception thrown by the
	implicit initialization of the Perl pattern attributes
      */
    public YahooReaderFilter() throws REException {
        countPattern = new RE("(Found <b>([0-9]*)</b>\r?\ncategories\r?\nand <b>([0-9]*)</b> *\r?\n)|(Found[\r\n ]*(about[\r\n ]*)?<b>([0-9,]*)</b>[\r\n ]*web pages?[\r\n ]*for)", RE.REG_MULTILINE);
        separationTablePattern = new RE("<table[^\r\n]*</table>", RE.REG_MULTILINE);
        webSitesLinkPattern = new RE("(<a href=\"([^\"]*)\">Web Sites</a>)|" + "(<b>Web Sites</b>)|(<b>Web Pages</b>)");
        webPagesLinkPattern = new RE("(<a href=\"([^\"]*)\">Web Pages</a>)|" + "(<b>Web Pages</b>)");
        matchPattern = new RE("<li><a href=\"([^\"]*)\">(((</?b>)?[^<](</?b>)?)*)</a>" + "(\r?\n)?( - (((</?b>)?[^<](</?b>)?)*))?((</li>)|(<dd>)|(<p>))", RE.REG_MULTILINE);
        decideYahooOrGooglePattern = new RE("(ALT=\"Yahoo! and Google\")|" + "(alt=\"Yahoo!\")", RE.REG_MULTILINE);
        String everythingExceptBR = "(((<[^bB])|(<[bB][^r])|(<[bB][rR][^>]))?[^\r\n<]+((<[^bB])|(<[bB][^r])|(<[bB][rR][^>]))?)*";
        googleMatchPattern = new RE("<li>\r?\n<a href=\"([^\"]*)\">(((</?b>)?[^<](</?b>)?)*)\r?\n</a>" + "<br><small>(" + everythingExceptBR + "(<br>\r?\n" + everythingExceptBR + ")*)\r?\n", RE.REG_MULTILINE);
        webSitesMode = false;
    }

    public YahooReaderFilter(boolean webSitesMode) throws REException {
        this();
        this.webSitesMode = webSitesMode;
    }

    /** extracts all search results from an AltaVista results
	page including the excerpts, last modification dates and title
	strings.<p>

	This implementation will scan only the first result page,
	usually containing only up to 10 document references. Future
	extensions might be that optionally the reader filter can be
	parameterized at construction time to read the referenced
	further result pages and analyze them as well.

	@param r the reader granting access to AltaVista's output
	@return the set containing {@link WebSearchResult} objects,
	each with initialized url, title, excerpt and last modified
	date fields
    */
    public Set extractResults(Reader r) throws IOException {
        try {
            if (YAHOO_URL == null) YAHOO_URL = new URL("http://www.yahoo.com/");
        } catch (Exception e) {
            System.err.println("Error initializing Yahoo URL. " + "Leaving null");
            e.printStackTrace();
        }
        CharIndexedReader is = new CharIndexedReader(r, 0);
        Set results = new HashSet();
        REMatch match = getAndSkipMatch(is, decideYahooOrGooglePattern);
        boolean isGoogleFormat = (match.toString(1) != null && match.toString(1).length() > 0);
        match = getAndSkipMatch(is, countPattern);
        if (match != null) {
            int count;
            if (isGoogleFormat) count = new Integer(match.toString(6)).intValue(); else {
                String countString1 = match.toString(2);
                String countString2 = match.toString(3);
                count = new Integer(countString1).intValue() + new Integer(countString2).intValue();
            }
            System.out.println("Count is " + count);
            match = getAndSkipMatch(is, webSitesLinkPattern);
            if (!webSitesMode && match != null && match.toString(2) != null && match.toString(1).length() > 0) kickOffWebSitesGrabbing(match.toString(2), this); else {
                webSitesGrabbingThread = null;
                webSitesResults = new HashSet();
            }
            System.out.println("webSitesLinkPattern: " + match);
            match = getAndSkipMatch(is, isGoogleFormat ? googleMatchPattern : matchPattern);
            while (match != null) {
                String urlString = match.toString(1);
                String title = removeMarkup(match.toString(2));
                String description = removeMarkup(match.toString(isGoogleFormat ? 5 : 7));
                WebSearchResult result = new WebSearchResult(new URL(urlString), title, description, null, YAHOO_NAME, YAHOO_URL);
                System.out.print("Y");
                System.out.flush();
                results.add(result);
                match = getAndSkipMatch(is, isGoogleFormat ? googleMatchPattern : matchPattern);
            }
            waitForWebSitesGrabbing();
            results.addAll(webSitesResults);
        }
        r.close();
        return results;
    }

    /** starts a thread which will start a new thread that downloads the URL
	whose string is passed and forwards the contents to a new {@link
	YahooReaderFilter}. The results will be stored in <tt>caller</tt>'s {@link
	#webSitesResults} attribute.<p>

	To enable {@link #waitForWebSitesGrabbing} to join on the created
	thread, the thread object will be saved in the <tt>caller</tt>'s {@link
	#webSitesGrabbingThread} attribute.<p>

	The set stored in {@link #webSitesResults} will always be valid but may
	be empty.

	@param urlString the URL where the web sites results can be loaded from
	@param caller
    */
    private void kickOffWebSitesGrabbing(final String urlString, final YahooReaderFilter caller) {
        caller.webSitesGrabbingThread = new Thread() {

            public void run() {
                try {
                    caller.webSitesResults = new HashSet();
                    YahooReaderFilter filter = new YahooReaderFilter(true);
                    URL url = new URL(urlString);
                    Reader r = new InputStreamReader(url.openStream());
                    caller.webSitesResults = filter.extractResults(r);
                } catch (Exception e) {
                    System.err.println("Couldn't retrieve Yahoo's " + "web sites results:");
                    e.printStackTrace();
                }
            }
        };
        caller.webSitesGrabbingThread.start();
    }

    /** waits until the web sites grabbing thread (see {@link
	#webSitesGrabbingThread}) has terminated. If {@link
	#kickOffWebSitesGrabbing} has been called before, then afterwards
	{@link #webSitesResults} can be assumed to contain a valid (but
	possibly empty) set.
    */
    private void waitForWebSitesGrabbing() {
        if (webSitesGrabbingThread != null) try {
            webSitesGrabbingThread.join();
        } catch (InterruptedException ie) {
        }
    }

    /** given a string that may contain HTML tags removes the tags
	from the string, leaving only the content between matching
	tags. This implies that a string not containing any HTML tags
	will remain unchanged.

	@param s the string to convert. If <tt>null</tt>, then
	<tt>null</tt> will be returned.
    */
    public String removeMarkup(String s) {
        if (s == null) return null;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '<') {
                int newI = s.indexOf(">", i);
                if (newI != -1) i = newI; else result.append(s.charAt(i));
            } else result.append(s.charAt(i));
        }
        return result.toString();
    }

    /** matches the count of pages. Contains the number of
	categories in group #2 and the number of sites in group #3 in case of
	Yahoo-Syntax or the number of found web pages in #5 for Google Format
      */
    protected RE countPattern;

    /** Matches the table separating categories of matches; currently not used
	because depending on the way of submitting (manual through browser or
	programmatically) Yahoo seems to return different formats for the
	header. The list items, though, seem to remain the same.
      */
    protected RE separationTablePattern;

    /** matches the link to the web sites found by Yahoo for the given
	query. The link URL is contained in group #2
    */
    protected RE webSitesLinkPattern;

    /** matches whe web pages link in the Yahoo results page, linking to Yahoo
	output containing references to individual web pages Yahoo consideres
	relating to the query. The link URL is contained in group #2
    */
    protected RE webPagesLinkPattern;

    /** matches a Yahoo match. Group #1 is the URL, group #2 the title
	and group #7 the optional description which may be null.
    */
    protected RE matchPattern;

    /** matches a Google match. Group #1 is the URL, group #2 the title
	and group #5 the optional description which may be null.
    */
    protected RE googleMatchPattern;

    /** can be used to decide whether the returned page contains results in
	Google format or in Yahoo format. If group #1 has been matched, the
	results are in Google format, if group #2 has been matched, the results
	are in Yahoo format.
    */
    protected RE decideYahooOrGooglePattern;

    /** may contain the thread used to grab the contents of Yahoo's web sites
	results for the processed query
    */
    private Thread webSitesGrabbingThread;

    /** contains the results of analyzing the web sites results. See also
	{@link kickOffWebSitesGrabbing}
    */
    private Set webSitesResults;

    /** if <tt>true</tt>, the reader filter operates in "Web Sites" mode. In
	this case it will ignore any further links titled "Web Sites" in order
	to avoid an endless recursion.
    */
    private boolean webSitesMode;

    /** a date parser, adapted to US locale and to the date format
	dd-MMM-yyyy so that it can understand the last modified date
	from the AltaVista output
      */
    private final SimpleDateFormat df = new SimpleDateFormat("mm/dd/yyyy", Locale.US);

    /** the name to be displayed as the origin: "Yahoo" */
    private static final String YAHOO_NAME = "Yahoo";

    /** the home URL of the corresponding search engine */
    private static URL YAHOO_URL = null;
}
