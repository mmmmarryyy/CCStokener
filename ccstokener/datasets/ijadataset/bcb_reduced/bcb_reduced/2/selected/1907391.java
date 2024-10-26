package org.lindenb.lib.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.lindenb.lib.debug.Debug;
import org.lindenb.lib.xml.XMLUtilities;
import org.lindenb.lib.xml.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author pierre
 * first created by me, and then freely inspired from http://java.sun.com/developer/EJTechTips/2004/tt0730.html
 *
 */
public class EUtilities {

    public static final String BASEURL_EUTILS = "http://www.ncbi.nlm.nih.gov/entrez/eutils/";

    public static final String BASEURL_ESEARCH = BASEURL_EUTILS + "esearch.fcgi";

    public static final String BASEURL_EFETCH = BASEURL_EUTILS + "efetch.fcgi";

    public static final String BASEURL_ELINK = BASEURL_EUTILS + "elink.fcgi";

    public static final String BASEURL_POST = BASEURL_EUTILS + "epost.fcgi";

    public static final String PUBMED = "pubmed";

    private String database;

    private Document document = null;

    private int retstart = 0;

    private int retcount = 20;

    private String queryKey = null;

    private String WebEnv = null;

    public EUtilities(String database) {
        this.database = database;
    }

    /**
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public int getReturnCount() {
        return retcount;
    }

    public void setReturnCount(int retcount) {
        this.retcount = retcount;
    }

    public int getReturnStart() {
        return retstart;
    }

    public void setReturnStart(int retstart) {
        this.retstart = retstart;
    }

    public Document searchAndFetch(String terms) throws SAXException, IOException {
        search(terms);
        return fetch();
    }

    public Document relatedAndFetch(int uid) throws SAXException, IOException {
        HashMap params = new HashMap();
        params.put("db", "" + getDatabase());
        params.put("id", "" + uid);
        params.put("cmd", "neighbor");
        try {
            this.WebEnv = null;
            URL url = buildUrl(BASEURL_ELINK, params);
            Document doc = fetchURL(url);
            Element Id[] = XPathAPI.selectElements(doc, "/eLinkResult/LinkSet/LinkSetDb/Link/Id");
            String URLStr = BASEURL_POST + "?db=" + getDatabase() + "&id=";
            for (int i = 0; i < Id.length; ++i) {
                if (i > 0) URLStr += ",";
                URLStr += XMLUtilities.textContent(Id[i]);
            }
            doc = fetchURL(new URL(URLStr));
            if (doc == null) throw new SAXException("No Document");
            Element root = doc.getDocumentElement();
            if (root == null) throw new SAXException("No Document Element");
            if (!root.getNodeName().equals("ePostResult")) {
                throw new SAXException("Not a <ePostResult> Element");
            }
            for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE) continue;
                String name = c.getNodeName();
                if (name.equals("QueryKey")) {
                    this.queryKey = XMLUtilities.textContent(c);
                } else if (name.equals("WebEnv")) {
                    this.WebEnv = XMLUtilities.textContent(c);
                } else if (name.equals("IdList")) {
                }
            }
            if (this.WebEnv == null) throw new SAXException("No <WebEnv> Element");
            return fetch();
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    public void search(String terms) throws SAXException, IOException {
        HashMap params = new HashMap();
        params.put("db", "" + getDatabase());
        params.put("usehistory", "y");
        params.put("term", "" + terms);
        params.put("retstart", "" + getReturnStart());
        params.put("retmax", "" + getReturnCount());
        try {
            URL url = buildUrl(BASEURL_ESEARCH, params);
            this.document = fetchURL(url);
            Element root = this.document.getDocumentElement();
            if (root == null) throw new SAXException("No Document Eement");
            if (!root.getNodeName().equals("eSearchResult")) {
                throw new SAXException("Not a <eSearchResult> Element");
            }
            for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE) continue;
                String name = c.getNodeName();
                if (name.equals("QueryKey")) {
                    this.queryKey = XMLUtilities.textContent(c);
                } else if (name.equals("WebEnv")) {
                    this.WebEnv = XMLUtilities.textContent(c);
                } else if (name.equals("IdList")) {
                }
            }
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    public Document fetch() throws SAXException, IOException {
        HashMap params = new HashMap();
        params.put("WebEnv", this.WebEnv);
        params.put("query_key", this.queryKey);
        params.put("db", getDatabase());
        params.put("retstart", "" + getReturnStart());
        params.put("retmax", "" + getReturnCount());
        params.put("retmode", "xml");
        params.put("rettype", "abstract");
        try {
            URL url = buildUrl(BASEURL_EFETCH, params);
            this.document = fetchURL(url);
            return this.document;
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    public Document fetch(int uid) throws SAXException, IOException {
        HashMap params = new HashMap();
        params.put("id", String.valueOf(uid));
        params.put("db", getDatabase());
        params.put("retmode", "xml");
        params.put("rettype", "abstract");
        try {
            URL url = buildUrl(BASEURL_EFETCH, params);
            this.document = fetchURL(url);
            return this.document;
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    protected Document fetchURL(URL url) throws IOException, SAXException {
        InputStream inputstream = url.openStream();
        InputSource xmlInp = new InputSource(inputstream);
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(false);
        docBuilderFactory.setExpandEntityReferences(false);
        DocumentBuilder parser = null;
        try {
            parser = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException err) {
            throw new SAXException(err.toString());
        }
        parser.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String SystemID) throws SAXException, IOException {
                if (SystemID.startsWith("http:/dtd")) {
                    SystemID = "http://www.ncbi.nlm.nih.gov" + SystemID.substring(5);
                }
                return new InputSource(SystemID);
            }
        });
        Document doc = parser.parse(xmlInp.getByteStream(), "http://www.ncbi.nlm.nih.gov/dtd/");
        doc.getDocumentElement().normalize();
        inputstream.close();
        return doc;
    }

    protected static URL buildUrl(String base, Map params) throws MalformedURLException {
        StringBuffer sb = new StringBuffer(base);
        sb.append("?");
        Iterator ip = params.keySet().iterator();
        while (ip.hasNext()) {
            String param = (String) ip.next();
            if (param == null) continue;
            sb.append(param);
            sb.append("=");
            sb.append(params.get(param));
            if (ip.hasNext()) {
                sb.append("&");
            }
        }
        return new URL(sb.toString());
    }
}
