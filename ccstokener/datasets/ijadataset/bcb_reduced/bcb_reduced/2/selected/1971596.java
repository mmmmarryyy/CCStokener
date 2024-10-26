package org.swemof.input.webfeed.opml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.SAXParserFactory;
import org.swemof.corpus.DocumentSet;
import org.swemof.input.ImportException;
import org.swemof.input.Source;
import org.swemof.input.webfeed.WebFeedSource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * {@link Source} implementation for OPML. This implementation (re)uses
 * {@link WebFeedSource} to parse the feed URL's encountered in the OPML.
 * <p>
 * For more details, see <a href="http://www.opml.org/">http://www.opml.org/</a>.
 */
public class OPMLSource implements Source {

    private final WebFeedSource feedSource;

    private final SAXParserFactory factory;

    /**
     * Initializes the source.
     * 
     * @param feedSource the delegate source to parse the feed URL's
     */
    public OPMLSource(WebFeedSource feedSource) {
        this.feedSource = feedSource;
        this.factory = SAXParserFactory.newInstance();
    }

    public Collection<DocumentSet> getDocumentSets(String uri) throws ImportException {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            throw new ImportException("Failed to parse URI", e);
        }
        XMLReader xmlReader;
        try {
            xmlReader = factory.newSAXParser().getXMLReader();
        } catch (Exception e) {
            throw new ImportException("Failed to create parser OPML", e);
        }
        FetchingHandler handler = new FetchingHandler();
        xmlReader.setContentHandler(handler);
        InputStream stream;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            throw new ImportException("Failed to read OPML", e);
        }
        try {
            xmlReader.parse(new InputSource(stream));
        } catch (Exception e) {
            throw new ImportException("Failed to parse OPML", e);
        }
        Collection<DocumentSet> sets = new ArrayList<DocumentSet>();
        for (String feedUrl : handler.getUrls()) {
            sets.addAll(feedSource.getDocumentSets(feedUrl));
        }
        return sets;
    }

    /**
     * {@link ContentHandler} that harvests any feed URL's encountered.
     */
    private static final class FetchingHandler extends DefaultHandler {

        private static final String OUTLINE_ELEM_NAME = "outline";

        private static final String URL_ATTR_NAME = "xmlUrl";

        private final List<String> urls;

        public FetchingHandler() {
            urls = new ArrayList<String>();
        }

        /**
         * @return the harvested URL's in the order they appear in the OPML.
         */
        public List<String> getUrls() {
            return urls;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
            if (OUTLINE_ELEM_NAME.equals(name)) {
                String url = atts.getValue(URL_ATTR_NAME);
                if (url != null) {
                    urls.add(url);
                }
            }
        }
    }
}
