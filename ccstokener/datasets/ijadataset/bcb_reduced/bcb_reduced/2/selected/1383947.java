package com.io_software.utils.web;

import java.net.URL;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.Collections;
import com.abb.util.RequestManager;
import com.abb.util.Request;
import ec.search.SearchEngine;
import ec.search.SearchEngineQuery;
import ec.search.MetaCrawlerSearchEngine;
import ec.search.WebSearchResult;

/** Given a finite {@link Form}, this class helps submitting elements
    from the form's parameter space, extracting phrases from the
    returned results and then searching for the resulting page using a
    web search engine.<p>

    The intention behind this is to gather statistical knowledge about
    search engines' coverage of content reachable via
    forms. Unfortunately, this does not yet cover those forms with
    "infinite" parameter space (containing open text fields). While
    such a form can be passed to {@link #submitAndSearch}, this method
    won't guess any contents for the open text fields but simply submit
    any value passed in the parameter list for it.
    
    @author Axel Uhl
    @version $Id: FormContentSearcher.java,v 1.3 2001/03/03 17:09:42 aul Exp $
  */
public class FormContentSearcher {

    /** Constructs an instance of this class. The main purpose of
	the constructor is to create the search engine since this
	may require some time (web lookup, download, etc.). After
	that the search engine should be readily available for
	subsequent calls to other methods of this class.
      */
    public FormContentSearcher() throws IOException {
        searchEngine = new MetaCrawlerSearchEngine();
    }

    /** Given a form and a set of corresponding actual parameters for
	this form, submits the form with these parameters and extracts
	a possibly unique phrase form the returned results (given that
	there was no error like "404 Not Found" or similar). This
	phrase is then submitted to a web search engine. If there were
	matches, they are verified (at most the first
	{@link #howManySearchResultsToVerify}) by
	loading the documents referenced by the search results
	and searching them for the phrases. The first URL
	returned by the search engine with a successful
	verification is	returned. Otherwise, <tt>null</tt>
	is the result of this method.<p>
	
	<b>Postcondition:</b> <tt>verify(submitAndSearch(form, params),
				  getLongPhrases(form.submitForm(params))
				  == true</tt>

	@param form the form to submit
	@param params the set of actual parameters to submit with the
	    form
	@return a URL returned by a search engine whose referred
	    document is supposed to contain a possibly unique phrase of
	    the document returned by submitting the form, or <tt>null</tt>
	    if no such document was found by the search page
    */
    public URL submitAndSearch(Form form, ActualFormParameters params) throws Exception {
        Reader r = form.submitForm(params);
        Vector phrases = getLongPhrases(r);
        SearchEngineQuery query = getStrongQuery(phrases);
        System.err.println("\nLooking for: " + query);
        Set lookupResults = searchEngine.search(query, null);
        URL result = null;
        if (lookupResults.size() > 0) {
            int count = 0;
            for (Iterator i = lookupResults.iterator(); result == null && count < howManySearchResultsToVerify && i.hasNext(); count++) {
                URL u = ((WebSearchResult) i.next()).getURL();
                if (verify(u, phrases)) result = u;
            }
        }
        return result;
    }

    /** verifies whether a given URL contains all of the specified
	phrases. The retrieved document has its non-blank whitespaces
	replaced by blanks (see {@link #normalizeWhitespaces}) in order
	to correspond with the whitespace normalization taking place
	in {@link #getLongPhrases}.
	
	@param url the URL to retrieve and to check
	@param phrases a vector of {@link String}s to search in the
		content stream of the passed url
	@return <tt>true</tt> if all phrases passed in <tt>phrases</tt>
		were found in the url's contents, <tt>false</tt>
		otherwise
	@exception java.io.IOException in case retrieving the URL failed
      */
    public boolean verify(URL url, Vector phrases) throws IOException {
        String contents = removeHTMLComments(normalizeWhitespaces(getURLContents(url)));
        for (Enumeration e = phrases.elements(); e.hasMoreElements(); ) {
            String phrase = (String) e.nextElement();
            if (contents.indexOf(phrase) < 0) return false;
        }
        return true;
    }

    /** For a given form scans its parameter space and submits a random
	sample of the elements of the parameter space. Then, for the retrieved
	results checks whether these results are indexed by a web
	search engine (see {@link #submitAndSearch}). The ratio of
	parameter combinations submitted and results found on the
	web is returned.
	
	@return the computed ratio as the number of results found
		by a search engine over the number of total
		parameter combinations submitted
      */
    public double computePublicResultsRatio(Form form) throws Exception {
        int total = 0;
        int found = 0;
        for (Enumeration pse = getRandomParameterSpaceSample(form); pse.hasMoreElements(); total++) {
            ActualFormParameters afps = (ActualFormParameters) pse.nextElement();
            try {
                URL url = submitAndSearch(form, afps);
                if (url != null) found++;
            } catch (IOException ioe) {
                System.err.println("Exception while searching form results");
                ioe.printStackTrace();
                System.err.println("Ignoring and continuing");
            }
        }
        return (double) (((double) found) / ((double) total));
    }

    /** For a form assembles a possibly random sample of the form's parameter
	space. Precondition: <tt>form.hasFiniteParameterSpace()</tt>.<p>
	Postcondition: The returned enumeration has less than 100 element.
	
	@param form the form for which to assemble a random sample of
		the parameter space
	@return an enumeration containing {@link ActualFormParameters}
		objects taken randomly from the form's
		{@link Form#enumerateParameterSpace} enumeration
      */
    private Enumeration getRandomParameterSpaceSample(Form form) {
        Vector v = new Vector();
        int count = 0;
        for (Enumeration e = form.enumerateParameterSpace(); e.hasMoreElements() && count < 100; count++) v.addElement(e.nextElement());
        return v.elements();
    }

    /** constructs a "strong query" based on the contents of the
	passed reader. The query is supposed to match the content
	as sharply as possible, matching a possibly small number of
	other documents but matching at least the content itself.<p>

	The current implementation collects long ASCII phrases from
	the page (see {@link #getLongPhrases}) and combines the 10
	longest into a search query.
	
	@param phrases a vector of {@link String}s ordered so that
		longer phrases appear at lower indexes.
	@return a query that is "strong" in the above sense
      */
    private SearchEngineQuery getStrongQuery(Vector phrases) throws IOException {
        StringBuffer queryBuffer = new StringBuffer();
        int i = 0;
        for (Enumeration e = phrases.elements(); e.hasMoreElements() && i < 10; i++) {
            String phrase = (String) e.nextElement();
            queryBuffer.append("\"");
            queryBuffer.append(phrase);
            queryBuffer.append("\"");
            if (e.hasMoreElements()) queryBuffer.append(" ");
        }
        SearchEngineQuery query = new SearchEngineQuery(queryBuffer.toString(), SearchEngineQuery.ALL_MODE);
        return query;
    }

    /** reads the contents of a document referred to by the given URL
	and returns it as a String.
	
	@param url the url to retrieve
	@return the string representing the returned contents
	@exception java.io.IOException in case retrieving the URL's
		    contents failed
      */
    protected static String getURLContents(URL url) throws IOException {
        Reader r = new InputStreamReader(url.openStream());
        StringBuffer sb = new StringBuffer();
        int read = r.read();
        while (read != -1) {
            sb.append((char) read);
            read = r.read();
        }
        return sb.toString();
    }

    /** replaces all whitespace sequences (\r, \t, \n, ' ') by
	a single blank and trims the string, removing blanks from both
	ends.
	
	@param s the string in which to replace non-blank whitespaces
	@return a string with all whitespace sequences replaced by
		a single blank and trimmed ends
      */
    private String normalizeWhitespaces(String s) {
        String blanked = s.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        StringBuffer result = new StringBuffer();
        boolean lastWasBlank = false;
        for (int i = 0; i < blanked.length(); i++) {
            if (blanked.charAt(i) != ' ' || !lastWasBlank) result.append(blanked.charAt(i));
            lastWasBlank = (blanked.charAt(i) == ' ');
        }
        return result.toString().trim();
    }

    /** From a reader that reads a HTML page extracts phrases as
	long and unique as possible (heuristically) and returns that
	string list.<p>
	
	Non-blank whitespaces that occurred in the document are replaced
	by blanks (see also {@link #normalizeWhitespaces}), so that the
	only whitespace character occuring in the returned phrases
	are blanks.
	
	@param r the reader to read from. Supposed to read from an HTML
		stream
	@return a list of phrases found in the contents of the reader
		ordered for decreasing length (longest first) with a
		minimum length of 3 (see {@link #getASCIIParagraphs}).
		The returned vector is always valid but may be empty.
      */
    public Vector getLongPhrases(Reader r) throws IOException {
        StringBuffer sb = new StringBuffer();
        int read = r.read();
        while (read != -1) {
            sb.append((char) read);
            read = r.read();
        }
        String pageString = normalizeWhitespaces(sb.toString());
        Vector strings = new Vector(new HashSet(getASCIIParagraphs(pageString)));
        Collections.sort(strings, new Comparator() {

            /** sorts longer strings to the beginning */
            public int compare(Object o1, Object o2) {
                String s1 = (String) o1;
                String s2 = (String) o2;
                return s2.length() - s1.length();
            }
        });
        return strings;
    }

    /** tries to extract the title from a string containing a complete
	HTML page. If no &lt;title&gt; tag is found (case-insensitively),
	the empty string ("") is returned.
	
	@param page the string denoting the page's contents
	@return the string between the &lt;title&gt; tags or "" if no
		matching tags were found
      */
    private String getTitle(String page) {
        String result = "";
        int start = page.indexOf("<title>");
        if (start == -1) start = page.indexOf("<TITLE>");
        if (start == -1) start = page.indexOf("<Title>");
        if (start != -1) {
            int end = page.indexOf("</title>");
            if (end == -1) end = page.indexOf("</TITLE>");
            if (end == -1) end = page.indexOf("</Title>");
            if (end != -1) result = page.substring(start + "<title>".length(), end);
        }
        return result.trim();
    }

    /** From an HTML content string tries to extract ASCII
	text passages as long as possible, thus heuristically speaking
	as unique as possible. The constraints for the phrases are as
	follows:

	<ul>

	    <li> each phrase starts at either a word separator or a
	    &lt;&gt; tag boundary (starts after &gt; and ends before
	    &lt;). Note, that the &amp;; tags don't count here. They
	    usually represent single characters and are regarded
	    phrase separators but no legal phrase borders here.

	    <li> word separator are all whitespace characters (blank,
	    tab, newline, carriage return)

	    <li> a phrase contains only characters from the set
	    [0-9A-Za-z\- ]

	    <li> The phrase is outside of a tag definition (not
	    between &lt; and &gt; and not between &amp; and ;)

	    <li> The phrase is outside of an HTML comment (delimited
	    by &lt;!-- and &gt;)

	</ul>

	This has one important implication: If while parsing a phrase
	a character outside of the allowed set of phrase characters is
	discovered the parser has to track back to the last word
	boundary and cut off the phrase there in order to fulfill the
	phrase boundary constraint.<p>
	
	The method proceeds by looking for the longest string of
	plain ASCII characters (as regular expression: [a-zA-Z0-9-_:;.,@/\]*)
	that can be found in the string. If the document contains
	no ASCII characters at all, an empty string is returned.
	
	@param rawPage the HTML page contents as a string
	@return a list of {@link String}s that were extracted from the
		passed string that were found outside of any tag
		specification and had a length >= 3.
		Text occurring between an opening
		tag and a closing tag is considered, whereas e.g. the
		names of tag attributes or their contents are not.
		A text string is considered a string of ASCII characters
		delimited by one of [^&lt;&gt;;]. Whitespace
		characters [\r\t\n] are replaced by a blank. If no
		strings fulfilling the minimum length requirement
		are found, a valid but empty vector is returned.
      */
    private Vector getASCIIParagraphs(String rawPage) {
        Vector result = new Vector();
        int i = 0;
        int phraseStart = 0;
        int lastValidPhraseEnd = -1;
        int inTag = 0;
        String page = removeHTMLComments(rawPage);
        while (i < page.length()) {
            switch(inTag) {
                case 0:
                    if (page.charAt(i) == '<') {
                        if (phraseStart != -1) {
                            addPhrase(page, phraseStart, i, result);
                            phraseStart = -1;
                            lastValidPhraseEnd = -1;
                        }
                        inTag = 1;
                    } else if (page.charAt(i) == '&' || isPhraseSeparator(page.charAt(i))) {
                        if (phraseStart != -1 && lastValidPhraseEnd != -1) addPhrase(page, phraseStart, lastValidPhraseEnd, result);
                        phraseStart = -1;
                        lastValidPhraseEnd = -1;
                        if (page.charAt(i) == '&') inTag = 2;
                    } else if (whitespaceChars.indexOf((int) page.charAt(i)) != -1) {
                        if (phraseStart == -1) phraseStart = i + 1; else lastValidPhraseEnd = i;
                    }
                    break;
                case 1:
                    if (page.charAt(i) == '>') {
                        inTag = 0;
                        phraseStart = i + 1;
                        lastValidPhraseEnd = -1;
                    }
                    break;
                case 2:
                    if (page.charAt(i) == ';') inTag = 0;
                    break;
            }
            i++;
        }
        return result;
    }

    /** from a string removes all HTML comments of format "&lt;!--
	--&gt;"

	@param s the string from which to remove the comments
	@return <tt>s</tt> with all comments removed
    */
    private String removeHTMLComments(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '<' && s.length() > i + "!--".length() && s.charAt(i + 1) == '!' && s.charAt(i + 2) == '-' && s.charAt(i + 3) == '-') {
                i = s.indexOf("-->", i) + "-->".length();
                if (i == -1) i = s.length();
                i--;
            } else result.append(s.charAt(i));
        }
        return result.toString();
    }

    /** Adds a phrase to the result vector of phrases if it is longer
	than three characters. The start and end index into the page
	string are passed. The extracted and trimmed phrase with
	whitespaces replaced by blanks (see {@link #whitespaceChars})
	is added to the results vecto if longer than three characters.

	@param page the string from which to extract the phrase
	@param startIndex index into <tt>page</tt> denoting the first
	character to be taken over into the phrase
	@param endIndex index into <tt>page</tt> pointing
	<em>after</em> the last character to be taken over into the
	phrase
	@param result out-parameter. Gets added a {@link String}
	object containing the extracted phrase if the stripped and
	trimmed phrase was longer than three characters.  */
    private void addPhrase(String page, int startIndex, int endIndex, Vector result) {
        final int minLength = 3;
        StringBuffer phrase = new StringBuffer(page.substring(startIndex, endIndex).trim());
        for (int i = 0; i < phrase.length(); i++) {
            if (whitespaceChars.indexOf((int) phrase.charAt(i)) != -1) phrase.setCharAt(i, ' ');
        }
        if (phrase.length() >= minLength) result.addElement(phrase.toString());
    }

    /** tells whether a character is to be considered a phrase
	separator. Those characters won't be taken as part of a phrase
	and will furthermore demarcate the end of a phrase.<p>

	Currently, all characters that are <em>not</em> in the set
	[A-Za-z0-9_ ] are considered separators.

	@param c the character to check
	@return <tt>true</tt> if thte passed character is to be
	considered a separator
    */
    private boolean isPhraseSeparator(char c) {
        int a = (int) c;
        return !((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == ' ');
    }

    /** For the specified crawler manager (if no name is given, the
	default root name is tried) retrieves all {@link Form} objects
	and computes their public results ratio if not yet defined.
    
	@param args In <tt>args[0]</tt> the name of the root object on
		which to operate is expected. If not specified, the
		default root object name is used.
	@see #computePublicResultsRatio(ec.metrics.CrawlerManager)
      */
    public static void main(String[] args) {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        String rootName = null;
        if (args.length > 0) rootName = args[0];
        try {
            CrawlerManager cm = CrawlerManagerImpl.getCrawlerManager(rootName, false, false);
            computePublicResultsRatio(cm);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Given a crawler manager retrieves all {@link form} objects
	that are reachable from it via crawlers, results, {@link HTMLPage}
	instances and finally the {@link Form} objects contained by them.
	For all the finite forms that don't have a public results ratio
	computed yet (see {@link Form#hasFiniteParamterSpace} and
	{@link Form#getPublicResultsRatio}) a request is
	created and put into a request manager that will compute this
	ratio for the form.
    
	@param cm the crawler manager to use as root object for the
		forms retrieval
	@see RequestManager
	@see RatioComputingRequest
      */
    private static void computePublicResultsRatio(CrawlerManager cm) throws RemoteException, IOException {
        FormContentSearcher searcher = new FormContentSearcher();
        RequestManager rm = new RequestManager(50);
        for (Enumeration e = cm.getCrawlers().elements(); e.hasMoreElements(); ) {
            Crawler c = (Crawler) e.nextElement();
            Map results = c.getResults();
            for (Iterator r = results.values().iterator(); r.hasNext(); ) {
                Object o = r.next();
                if (o instanceof HTMLPage) {
                    HTMLPage page = (HTMLPage) o;
                    for (Enumeration f = page.getForms(); f.hasMoreElements(); ) {
                        Form form = (Form) f.nextElement();
                        if (form.hasFiniteParameterSpace() && form.getPublicResultsRatio() < 0) addRatioComputingRequest(searcher, rm, form);
                    }
                }
            }
        }
        rm.waitForLastRequestToFinish();
    }

    /** Creates a request of class {@link RatioComputingRequest} for
	the specified parameters and adds it to the passed request
	manager
	
	@param searcher the instance of this class used as outer
		object to the request objects to be created by this
		method.
	@param rm the request manager to which to add the created request
	@param form create the ratio compute request for this form
      */
    private static void addRatioComputingRequest(FormContentSearcher searcher, RequestManager rm, Form form) {
        RatioComputingRequest request = searcher.new RatioComputingRequest(form, rm);
        rm.addRequest(request);
    }

    /** This inner class implements the {@link Request} interface and
	thus can be used by the {@link RequestManager} for thread
	pooled execution. For the form passed to the constructor it
	will compute its public results ratio and update this value
	in the form's database representation (see {@link
	Form#setPublicResultsRatio}).<p>
	
	In order to avoid concurrent database accesses, the session
	to be used by all threads in the thread pool is passed to the
	constructor. Updates (everything between and including
	<tt>session.begin()</tt> and <tt>session.commit()</tt>) has
	to be synchronized on the request manager which is therefore
	also passed to the constructor.
      */
    public class RatioComputingRequest implements Request {

        /** Remembers the form for which to compute the public results
	    ratio, the session to use for database updates and the
	    request manager that is responsible for scheduling this
	    request and that must be used for synchronizing session
	    access.
	    
	    @param form the form which to compute the public results ratio
	    @param rm the request manager to be used for session sychronization
	  */
        public RatioComputingRequest(Form form, RequestManager rm) {
            this.form = form;
            this.rm = rm;
        }

        /** executes this request by applying the method
	    {@link computePublicResultsRation(ec.metrics.Form)} to
	    the form stored in this request object (see {@link #form}).
	    When the results are computed, the database session is
	    begun, synchronized on the request manager passed to
	    the constructor and stored in attribute
	    {@link #rm}. This will then use
	    {@link Form#setPublicResultsRatio} to update the
	    form in the database.
	  */
        public void execute() {
            try {
                System.out.println("computing public results ratio for form " + "with action URL " + form.getActionURL());
                double ratio = computePublicResultsRatio(form);
                synchronized (rm) {
                    form.setPublicResultsRatio(ratio);
                }
                System.out.println("done with " + form.getActionURL());
            } catch (Exception e) {
                System.err.println("Exception while trying to compute " + "public results ratio for form " + form + ".\nLeaving ratio unset. " + "Exception was:");
                e.printStackTrace();
            }
        }

        /** the form which to compute the public results ratio */
        private Form form;

        /** the request manager to be used for session sychronization */
        private RequestManager rm;
    }

    /** The search engine to use for looking up form results */
    private SearchEngine searchEngine;

    /** tells how many of the returned search results to verify before
	assuming that the form output really was not found by a search
	engine. Currently defaults to 10.
      */
    private static final int howManySearchResultsToVerify = 10;

    /** lists the characters to be considered whitespaces that are
	replaced by blanks when extracting phrases from an HTML page
    */
    private static final String whitespaceChars = " \n\r\t ";
}
