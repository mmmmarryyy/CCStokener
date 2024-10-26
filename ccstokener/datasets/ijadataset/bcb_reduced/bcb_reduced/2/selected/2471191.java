package wpspider.client.dao.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import wpspider.client.common.WPSpiderProperties;
import wpspider.client.dao.PageDAO;
import wpspider.client.model.Page;

/**
 * PageDAO class.
 */
public class HTTPPageDAO implements PageDAO {

    /** Logger. */
    private static Logger _logger = Logger.getLogger(HTTPPageDAO.class);

    /** Property key for service URL. */
    private static final String PROPERTY_KEY_BASEURL = "wpspider.client.dao.http.HTTPPageDAO.BASEURL";

    /** Service URL. */
    private String _baseUrl;

    /**
     * Constructor.
     * @throws IOException
     */
    public HTTPPageDAO() throws IOException {
        _baseUrl = WPSpiderProperties.getInstance().getProperty(PROPERTY_KEY_BASEURL);
        _logger.debug("_baseUrl=" + _baseUrl);
        if (_baseUrl == null) {
            throw new NullPointerException("Property `" + PROPERTY_KEY_BASEURL + "' is null.");
        }
    }

    /**
     * Closes transaction.
     */
    public void close() {
    }

    /**
     * Gets pages.
     * @param names
     * @return List of Page.
     * @throws IOException
     * @throws MalformedURLException
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    public List<Page> findByNames(List<String> names) throws MalformedURLException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
        List<Page> pages = new ArrayList<Page>();
        StringBuffer query = new StringBuffer();
        int lastIndex = names.size() - 1;
        for (int i = 0; i < names.size(); ++i) {
            String name = (String) URLEncoder.encode(names.get(i), "UTF-8");
            query.append(name + "+");
            if (i == lastIndex || 200 < query.length()) {
                String url = _baseUrl + "?names=" + query;
                List<Page> xpages = requestHTTPGet(url);
                pages.addAll(xpages);
                query = new StringBuffer();
            }
        }
        return pages;
    }

    /**
     * Gets Page.
     * @param names
     * @throws IOException
     * @throws MalformedURLException
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    public Page findByName(String name) throws MalformedURLException, IOException, ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {
        List<String> names = new ArrayList<String>();
        names.add(name);
        List<Page> pages = findByNames(names);
        if (pages == null || pages.size() == 0) {
            return null;
        } else {
            return pages.get(0);
        }
    }

    /**
     * Requests HTTP GET Method.
     * @param url Request URL.
     * @return List of Page.
     * @throws IOException
     * @throws MalformedURLException
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    private List<Page> requestHTTPGet(String url) throws MalformedURLException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, TransformerException {
        List<Page> pages = new ArrayList<Page>();
        _logger.debug("url=" + url);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conn.getInputStream();
                List<Page> xpages = XMLParser.parsePageXML(inputStream);
                pages.addAll(xpages);
            } else {
                _logger.warn("HTTP Result: " + conn.getResponseMessage());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return pages;
    }
}
