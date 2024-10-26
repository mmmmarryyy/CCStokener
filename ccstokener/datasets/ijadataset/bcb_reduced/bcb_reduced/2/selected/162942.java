package com.io_software.catools.search;

import java.net.URL;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** Wraps a Google Usenet result {@link URL} as a searchable text source,
    knowing that the first few lines up to the end of the search form do not
    contribute to the search result, hence skipping them in the reader that is
    returned by {@link #getText}.

    @version $Id: GoogleUsenetResultTextSearchable.java,v 1.1 2001/05/14 08:28:31 aul Exp $
    @author Axel Uhl
*/
public class GoogleUsenetResultTextSearchable extends TextSearchable {

    /** Constructs an instance of this class based on the specified
	file.
      */
    public GoogleUsenetResultTextSearchable(URL url) {
        this.url = url;
    }

    /** retrieves the text belonging to this searchable object by opening the
	URL connecting and loading the contents as a {@link Reader}. All lines
	including the first line containing the string "&lt;/table&gt;&lt;br
	clear=all&gt;" are skipped. Can be called multiple times, always
	yielding a valid reader.
	
	@return a reader around the string held by this instance
    */
    protected Reader getText() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String readLine;
        do {
            readLine = br.readLine();
        } while (readLine != null && readLine.indexOf("</table><br clear=all>") < 0);
        return br;
    }

    /** creates a {@link WebSearchResult} for the URL wrapped by this
	searchable

	@return a {@link WebSearchResult} constructed on the {@link #url}
	wrapped by this object
    */
    public TextSearchResult asSearchResult() throws IOException {
        if (searchResult == null) searchResult = new WebSearchResult(url);
        return searchResult;
    }

    /** The string representation contains the type and the URL */
    public String toString() {
        return "com.io_software.catools.search.TextURLSearchable: " + url;
    }

    /** the content to search in */
    private URL url;
}
