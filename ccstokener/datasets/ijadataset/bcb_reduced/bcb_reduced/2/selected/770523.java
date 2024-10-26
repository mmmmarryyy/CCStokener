package atheneum.shared;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Paul Smith
 */
public class AthenServletResponse {

    public enum Type {

        ERROR, MESSAGE, RESULTS, USERS, UNKNOWN
    }

    public enum Method {

        GET, POST
    }

    private Type m_type;

    private String m_xml;

    private Document m_dom;

    private String m_query;

    private String m_message;

    private NodeList m_itemNodes;

    private AthenItem[] m_items;

    public static AthenServletResponse callServlet(Method method, String page, String args) throws MalformedURLException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        if (method == null) {
            throw new NullPointerException("method was null");
        }
        URL url = null;
        URLConnection urlConn = null;
        InputStreamReader input = null;
        OutputStreamWriter output = null;
        StringBuffer xml = new StringBuffer(0);
        char[] buffer = new char[1024];
        try {
            switch(method) {
                case GET:
                    if (args != null && args.length() > 0) {
                        url = new URL(page + '?' + args);
                    } else {
                        url = new URL(page);
                    }
                    break;
                case POST:
                    url = new URL(page);
                    break;
                default:
                    url = new URL(page);
                    break;
            }
            urlConn = url.openConnection();
            if (method == Method.POST) {
                urlConn.setDoOutput(true);
            }
            urlConn.connect();
            if ((method == Method.POST) && (args != null && args.length() > 0)) {
                output = new OutputStreamWriter(urlConn.getOutputStream());
                output.write(args);
                output.flush();
                output.close();
                output = null;
            }
            input = new InputStreamReader(urlConn.getInputStream());
            while (true) {
                int read = input.read(buffer);
                if (read == -1) {
                    break;
                }
                xml.append(buffer, 0, read);
            }
            input.close();
            input = null;
            return new AthenServletResponse(xml.toString());
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
    }

    public AthenServletResponse(String xml) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        if (xml == null) {
            throw new NullPointerException("xml was null");
        }
        m_xml = xml;
        parseXML();
    }

    private void parseXML() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        StringReader reader = new StringReader(m_xml);
        InputSource input = new InputSource(reader);
        m_dom = docBuilder.parse(input);
        reader.close();
        Element root = m_dom.getDocumentElement();
        String rootName = root.getTagName();
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        if ("error".equals(rootName)) {
            m_type = Type.ERROR;
            m_query = xpath.evaluate("query", root);
            m_message = xpath.evaluate("msg", root);
            m_itemNodes = null;
        } else if ("message".equals(rootName)) {
            m_type = Type.MESSAGE;
            m_query = xpath.evaluate("query", root);
            m_message = xpath.evaluate("msg", root);
            m_itemNodes = null;
        } else if ("results".equals(rootName)) {
            m_type = Type.RESULTS;
            m_query = xpath.evaluate("query", root);
            m_message = xpath.evaluate("msg", root);
            m_itemNodes = (NodeList) xpath.evaluate("item", root, XPathConstants.NODESET);
            m_items = new AthenItem[m_itemNodes.getLength()];
            for (int i = 0; i < m_items.length; ++i) {
                Node node = m_itemNodes.item(i);
                int id = Integer.parseInt(xpath.evaluate("id", node));
                String upc = xpath.evaluate("upc", node);
                String isbn13 = xpath.evaluate("isbn13", node);
                String isbn10 = xpath.evaluate("isbn10", node);
                String title = xpath.evaluate("title", node);
                String description = xpath.evaluate("description", node);
                String category = xpath.evaluate("category", node);
                String shelfNumber = xpath.evaluate("shelfNumber", node);
                int quantity = Integer.parseInt(xpath.evaluate("quantity", node));
                m_items[i] = new AthenSearchResponseItem(id, upc, isbn13, isbn10, title, description, category, shelfNumber, quantity);
            }
        } else {
            m_type = Type.UNKNOWN;
            m_query = null;
            m_message = null;
            m_itemNodes = null;
        }
    }

    public String getXML() {
        return m_xml;
    }

    public Document getDOM() {
        return m_dom;
    }

    public Type getType() {
        return m_type;
    }

    public String getQuery() {
        return m_query;
    }

    public String getMessage() {
        return m_message;
    }

    public AthenItem[] getItems() {
        return m_items;
    }

    private class AthenSearchResponseItem implements AthenItem {

        private final int m_id;

        private final String m_upc, m_isbn13, m_isbn10;

        private final String m_title, m_description, m_category, m_shelfNumber;

        private final int m_quantity;

        public AthenSearchResponseItem(int id, String upc, String isbn13, String isbn10, String title, String description, String category, String shelfNumber, int quantity) {
            m_id = id;
            m_upc = upc;
            m_isbn13 = isbn13;
            m_isbn10 = isbn10;
            m_title = title;
            m_description = description;
            m_category = category;
            m_shelfNumber = shelfNumber;
            m_quantity = quantity;
        }

        public int getID() {
            return m_id;
        }

        public String getUPC() {
            return m_upc;
        }

        public String getISBN13() {
            return m_isbn13;
        }

        public String getISBN10() {
            return m_isbn10;
        }

        public String getTitle() {
            return m_title;
        }

        public String getDescription() {
            return m_description;
        }

        public String getCategory() {
            return m_category;
        }

        public String getShelfNumber() {
            return m_shelfNumber;
        }

        public int getQuantity() {
            return m_quantity;
        }
    }
}
