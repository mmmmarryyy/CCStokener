package com.io_software.catools.search;

import java.net.URL;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** Wraps a {@link URL} as a searchable text source.

    @version $Id: TextURLSearchable.java,v 1.6 2001/04/09 11:44:22 aul Exp $
    @author Axel Uhl
  */
public class TextURLSearchable extends TextSearchable {

    /** Constructs an instance of this class based on the specified
	file.
      */
    public TextURLSearchable(URL url) {
        this.url = url;
        this.title = " -- No Title -- ";
    }

    /** initializes the new object also with a title

	@param url the URL that is represented by the new searchable
	@param title the title of the corresponding document. Used to build a
	representation as search result efficiently
    */
    public TextURLSearchable(URL url, String title) {
        this(url);
        if (title != null) this.title = title;
    }

    /** retrieves the text belonging to this searchable object by opening the
	URL connecting and loading the contents as a {@link Reader}. Can be
	called multiple times, always yielding a valid reader.
	
	@return a reader around the string held by this instance
    */
    protected Reader getText() throws IOException {
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    /** sets the origin for this searchable. Will be used in {@link
	#asSearchResult} to initialize the {@link WebSearchResult} instance
	properly.
    */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /** sets a URL for rendering the origin. Will be used in {@link
	#asSearchResult} to initialize the {@link WebSearchResult} instance
	properly. May be <tt>null</tt>.
    */
    public void setOriginURL(URL originURL) {
        this.originURL = originURL;
    }

    /** creates a {@link WebSearchResult} for the URL wrapped by this
	searchable

	@return a {@link WebSearchResult} constructed on the {@link #url}
	wrapped by this object
    */
    public TextSearchResult asSearchResult() throws IOException {
        String originString = origin;
        if (originString == null) originString = "";
        if (searchResult == null) if (title == null) searchResult = new WebSearchResult(url); else searchResult = new WebSearchResult(this, url, title, title, null, originString, originURL);
        return searchResult;
    }

    /** The string representation contains the type and the URL */
    public String toString() {
        return "com.io_software.catools.search.TextURLSearchable: " + url;
    }

    /** the content to search in */
    private URL url;

    /** title of the represented document. Can be <tt>null</tt> */
    private String title;

    /** a string optionally denoting the origin of this searchable. May be set
	using {@link #setOrigin} and will, if not <tt>null</tt>, be rendered in
	the {@link #asSearchResult} method.
    */
    private String origin;

    /** denotes a URL to be used to turn the {@link #origin} string into an
	HTML link, if not <tt>null</tt>. See also {@link #asSearchResult} and
	{@link #setOriginURL}.
    */
    private URL originURL;

    static final long serialVersionUID = -1126961764099076498l;
}
