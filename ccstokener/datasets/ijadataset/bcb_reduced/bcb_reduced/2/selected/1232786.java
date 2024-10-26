package org.rockaa.search.yahoo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.rockaa.observer.Observer;
import org.rockaa.search.AbstractSearchEngine;
import org.rockaa.search.SearchException;
import org.rockaa.translation.Translator;

@SuppressWarnings("unchecked")
public class YahooSearchEngine extends AbstractSearchEngine {

    private static final String SEARCH_URL_COMMON_PART_2 = "&query=";

    private static final String SEARCH_URL_COMMON_PART_1 = "?type=all&output=xml&results=50&appid=";

    private static final String RESULT_ELEMENT_NAME = "Result";

    private static final String URL_BASE = "http://search.yahooapis.com/ImageSearchService/V1/imageSearch";

    private static final String APPLICATION_ID = "ZKq98TXV34HHUFHRV7IN1Warjr7LpbBJgD9X0f4dhlQeokZ9LTBuEGYECMzbElDT4w--";

    private String buildSearchURL(final String keywords) {
        final StringBuilder sb = new StringBuilder();
        sb.append(YahooSearchEngine.URL_BASE);
        sb.append(YahooSearchEngine.SEARCH_URL_COMMON_PART_1);
        sb.append(YahooSearchEngine.APPLICATION_ID);
        sb.append(YahooSearchEngine.SEARCH_URL_COMMON_PART_2);
        sb.append(keywords);
        return sb.toString().replace(" ", "%20");
    }

    private void continueAsynchron(final Document document, final Observer observer) {
        final Thread thread = new Thread() {

            @Override
            public void run() {
                YahooSearchEngine.this.parse(document, observer);
            }
        };
        thread.start();
    }

    @Override
    public String getDescriptionForGUI() {
        return "Yahoo";
    }

    private Document getDocument(final String request) throws JDOMException, IOException {
        final URL url = new URL(request);
        return new SAXBuilder().build(new InputStreamReader(url.openStream()));
    }

    private int getResultCount(final Document doc) {
        final Element root = doc.getRootElement();
        final Attribute a = root.getAttribute("totalResultsReturned");
        try {
            return a.getIntValue();
        } catch (final DataConversionException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void parse(final Document doc, final Observer observer) {
        final Element root = doc.getRootElement();
        final List<?> list = root.getContent();
        final Iterator<?> it = list.iterator();
        while (it.hasNext()) {
            final Object o = it.next();
            if (o instanceof Element) this.parseResult((Element) o, observer);
        }
    }

    private void parseResult(final Element e, final Observer observer) {
        if (YahooSearchEngine.RESULT_ELEMENT_NAME.equals(e.getName())) new YahooSearchResult(e, observer);
    }

    @Override
    public int search(final String keywords, final Observer observer) throws SearchException {
        final String request = this.buildSearchURL(keywords);
        Document document;
        try {
            document = this.getDocument(request);
        } catch (final IOException e) {
            throw new SearchException(Translator.translate("error_downloading_from_yahoo"), e.getCause(), e.getStackTrace());
        } catch (final JDOMException e) {
            throw new SearchException(Translator.translate("error_downloading_from_yahoo"), e.getCause(), e.getStackTrace());
        }
        this.continueAsynchron(document, observer);
        return this.getResultCount(document);
    }
}
