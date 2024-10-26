package com.agilejava.javaone;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang.StringUtils;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import com.sun.org.apache.xalan.internal.xsltc.trax.SAX2DOM;
import com.sun.org.apache.xpath.internal.XPathAPI;

public abstract class AbstractSessionCatalog {

    private static final Map<String, SessionType> TYPES = new HashMap<String, SessionType>();

    private ExecutorService service;

    static {
        TYPES.put("Technical Session", SessionType.TechnicalSession);
        TYPES.put("Hands-On Lab", SessionType.HandsOnLab);
        TYPES.put("Birds-of-a-Feather Session (BOF)", SessionType.BirdsOfAFeather);
        TYPES.put("Panel Session", SessionType.PanelSession);
    }

    private static final int DETAILS_SNIP_START = "javascript:newWnd('".length();

    private static final int DETAILS_SNIP_END = -"');".length();

    public AbstractSessionCatalog(int nrThreads) {
        this.service = Executors.newFixedThreadPool(nrThreads);
    }

    protected Logger logger = new NullLogger();

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    protected String getBaseURL() {
        return "http://www28.cplan.com/cc191/";
    }

    protected String extractURL(String detailsHref) {
        return getBaseURL() + StringUtils.substring(detailsHref, DETAILS_SNIP_START, DETAILS_SNIP_END);
    }

    protected Node loadDocument(URL url) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException, IOException, SAXException {
        return loadDocument(url.openStream());
    }

    protected Node loadDocument(InputStream in) throws IOException, SAXException, ParserConfigurationException {
        Parser parser = new Parser();
        parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        SAX2DOM sax2dom = new SAX2DOM();
        parser.setContentHandler(sax2dom);
        parser.parse(new InputSource(in));
        Node doc = sax2dom.getDOM();
        return doc;
    }

    protected SessionType getType(String name) {
        SessionType result = TYPES.get(name);
        if (result == null) {
            return SessionType.Unknown;
        } else {
            return result;
        }
    }

    protected void addDetails(Session session) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        try {
            Node detailNode = loadDocument(new URL(session.getDetailsURL()));
            Node valueNode = XPathAPI.selectSingleNode(detailNode, "//html:body/html:table[position()=2]");
            session.setSummary(XPathAPI.eval(valueNode, "html:tr[position()=4]/html:td[position()=2]").toString().trim());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected void addDetails(Presenter presenter) {
    }

    protected final ExecutorService getExecutorService() {
        return service;
    }
}
