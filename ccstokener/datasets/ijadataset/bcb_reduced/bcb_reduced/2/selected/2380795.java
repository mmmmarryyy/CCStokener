package org.torweg.pulse.component.core.qc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.sf.classifier4J.IStopWordProvider;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.torweg.pulse.bundle.Controller;
import org.torweg.pulse.util.xml.XMLConverter;

/**
 * checks the contents of a {@code SitemapNode} or a given URL for search
 * engine optimization criteria.
 * 
 * @author Thomas Weber
 * @version $Revision: 1454 $
 */
public class SearchEngineQualityControl extends Controller {

    /**
	 * performs the analysis on the given URL.
	 * 
	 * @param url
	 *            the URL to be checked
	 * @param stopWordProvider
	 *            the stop word provider or {@code null}
	 * @param path
	 *            the XPath to the content part or {@code null}
	 * @return the result of the check
	 * @throws IOException
	 *             on errors
	 * @throws JDOMException
	 *             on errors
	 */
    public final SearchEngineQualityControlResult analyze(final URL url, final IStopWordProvider stopWordProvider, final XPath path) throws IOException, JDOMException {
        Document xhtmlDocument = getXHTMLFromURL(url);
        SearchEngineQualityControlResult result = analyze(xhtmlDocument, stopWordProvider, path);
        result.setURL(url.toString());
        return result;
    }

    /**
	 * performs the analysis on the given document.
	 * 
	 * @param xhtmlDocument
	 *            the XHTML document
	 * @param stopWordProvider
	 *            the stop word provider or {@code null}
	 * @param path
	 *            the XPath to the content part or {@code null}
	 * @return the result of the check
	 * @throws JDOMException
	 *             on errors
	 */
    public final SearchEngineQualityControlResult analyze(final Document xhtmlDocument, final IStopWordProvider stopWordProvider, final XPath path) throws JDOMException {
        if ((stopWordProvider == null) && (path == null)) {
            return new SearchEngineQualityControlResult(xhtmlDocument);
        } else if (stopWordProvider == null) {
            return new SearchEngineQualityControlResult(xhtmlDocument, path);
        }
        return new SearchEngineQualityControlResult(xhtmlDocument, stopWordProvider, path);
    }

    /**
	 * connects to the given {@code URL} via GET and tries to retrieve the
	 * contents as XHTML following all redirects.
	 * 
	 * @param url
	 *            the URL to connect to
	 * @return the XHTML document
	 * @throws IOException
	 *             on i/o errors
	 * @throws JDOMException
	 *             on errors parsing the document
	 */
    public static Document getXHTMLFromURL(final URL url) throws IOException, JDOMException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);
        String encoding = con.getContentEncoding();
        if (encoding == null) {
            encoding = "utf-8";
        }
        BufferedReader response = new BufferedReader(new InputStreamReader(con.getInputStream(), encoding));
        Document xhtmlDocument = XMLConverter.cleanHTML(response).getDocument();
        con.disconnect();
        return xhtmlDocument;
    }
}
