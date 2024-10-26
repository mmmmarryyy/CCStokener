package org.apache.myfaces.shared_impl.webapp.webxml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.myfaces.shared_impl.util.ClassUtils;
import org.apache.myfaces.shared_impl.util.xml.MyFacesErrorHandler;
import org.apache.myfaces.shared_impl.util.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 933817 $ $Date: 2010-04-13 18:45:52 -0500 (Tue, 13 Apr 2010) $
 */
public class WebXmlParser {

    private static final Logger log = Logger.getLogger(WebXmlParser.class.getName());

    private static final String WEB_XML_PATH = "/WEB-INF/web.xml";

    private static final String WEB_APP_2_2_J2EE_SYSTEM_ID = "http://java.sun.com/j2ee/dtds/web-app_2_2.dtd";

    private static final String WEB_APP_2_2_SYSTEM_ID = "http://java.sun.com/dtd/web-app_2_2.dtd";

    private static final String WEB_APP_2_2_RESOURCE = "javax/servlet/resources/web-app_2_2.dtd";

    private static final String WEB_APP_2_3_SYSTEM_ID = "http://java.sun.com/dtd/web-app_2_3.dtd";

    private static final String WEB_APP_2_3_RESOURCE = "javax/servlet/resources/web-app_2_3.dtd";

    private ExternalContext _context;

    private org.apache.myfaces.shared_impl.webapp.webxml.WebXml _webXml;

    public WebXmlParser(ExternalContext context) {
        _context = context;
    }

    public WebXml parse() {
        _webXml = new WebXml();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver(new _EntityResolver());
            db.setErrorHandler(new MyFacesErrorHandler(log));
            InputSource is = createContextInputSource(null, WEB_XML_PATH);
            if (is == null) {
                URL url = _context.getResource(WEB_XML_PATH);
                log.fine("No web-xml found at : " + (url == null ? " null " : url.toString()));
                return _webXml;
            }
            Document document = db.parse(is);
            Element webAppElem = document.getDocumentElement();
            if (webAppElem == null || !webAppElem.getNodeName().equals("web-app")) {
                throw new FacesException("No valid web-app root element found!");
            }
            readWebApp(webAppElem);
            return _webXml;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to parse web.xml", e);
            throw new FacesException(e);
        }
    }

    public static long getWebXmlLastModified(ExternalContext context) {
        try {
            URL url = context.getResource(WEB_XML_PATH);
            if (url != null) return url.openConnection().getLastModified();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not find web.xml in path " + WEB_XML_PATH);
        }
        return 0L;
    }

    private InputSource createContextInputSource(String publicId, String systemId) {
        InputStream inStream = _context.getResourceAsStream(systemId);
        if (inStream == null) {
            return null;
        }
        InputSource is = new InputSource(inStream);
        is.setPublicId(publicId);
        is.setSystemId(systemId);
        return is;
    }

    private InputSource createClassloaderInputSource(String publicId, String systemId) {
        InputStream inStream = ClassUtils.getContextClassLoader().getResourceAsStream(systemId);
        if (inStream == null) {
            inStream = this.getClass().getClassLoader().getResourceAsStream(systemId);
        }
        if (inStream == null) {
            return null;
        }
        InputSource is = new InputSource(inStream);
        is.setPublicId(publicId);
        is.setSystemId(systemId);
        return is;
    }

    private class _EntityResolver implements EntityResolver {

        public InputSource resolveEntity(String publicId, String systemId) throws IOException {
            if (systemId == null) {
                throw new UnsupportedOperationException("systemId must not be null");
            }
            if (systemId.equals(WebXmlParser.WEB_APP_2_2_SYSTEM_ID) || systemId.equals(WebXmlParser.WEB_APP_2_2_J2EE_SYSTEM_ID)) {
                return createClassloaderInputSource(publicId, WebXmlParser.WEB_APP_2_2_RESOURCE);
            } else if (systemId.equals(WebXmlParser.WEB_APP_2_3_SYSTEM_ID)) {
                return createClassloaderInputSource(publicId, WebXmlParser.WEB_APP_2_3_RESOURCE);
            } else {
                return createContextInputSource(publicId, systemId);
            }
        }
    }

    private void readWebApp(Element webAppElem) {
        NodeList nodeList = webAppElem.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals("servlet")) {
                    readServlet((Element) n);
                }
                if (n.getNodeName().equals("servlet-mapping")) {
                    readServletMapping((Element) n);
                }
                if (n.getNodeName().equals("filter")) {
                    readFilter((Element) n);
                }
                if (n.getNodeName().equals("filter-mapping")) {
                    readFilterMapping((Element) n);
                }
                if (n.getNodeName().equals("error-page")) {
                    _webXml.setErrorPagePresent(true);
                }
            } else {
                if (log.isLoggable(Level.FINE)) log.fine("Ignored node '" + n.getNodeName() + "' of type " + n.getNodeType());
            }
        }
    }

    private void readServlet(Element servletElem) {
        String servletName = null;
        String servletClass = null;
        NodeList nodeList = servletElem.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals("servlet-name")) {
                    servletName = XmlUtils.getElementText((Element) n);
                } else if (n.getNodeName().equals("servlet-class")) {
                    servletClass = org.apache.myfaces.shared_impl.util.xml.XmlUtils.getElementText((Element) n).trim();
                } else if (n.getNodeName().equals("description") || n.getNodeName().equals("load-on-startup") || n.getNodeName().equals("init-param")) {
                } else {
                    if (log.isLoggable(Level.FINE)) log.fine("Ignored element '" + n.getNodeName() + "' as child of '" + servletElem.getNodeName() + "'.");
                }
            } else {
                if (log.isLoggable(Level.FINE)) log.fine("Ignored node '" + n.getNodeName() + "' of type " + n.getNodeType());
            }
        }
        _webXml.addServlet(servletName, servletClass);
    }

    private void readServletMapping(Element servletMappingElem) {
        String servletName = null;
        String urlPattern = null;
        NodeList nodeList = servletMappingElem.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals("servlet-name")) {
                    servletName = org.apache.myfaces.shared_impl.util.xml.XmlUtils.getElementText((Element) n);
                } else if (n.getNodeName().equals("url-pattern")) {
                    urlPattern = org.apache.myfaces.shared_impl.util.xml.XmlUtils.getElementText((Element) n).trim();
                } else {
                    if (log.isLoggable(Level.FINE)) log.fine("Ignored element '" + n.getNodeName() + "' as child of '" + servletMappingElem.getNodeName() + "'.");
                }
            } else {
                if (log.isLoggable(Level.FINE)) log.fine("Ignored node '" + n.getNodeName() + "' of type " + n.getNodeType());
            }
        }
        urlPattern = urlPattern.trim();
        _webXml.addServletMapping(servletName, urlPattern);
    }

    private void readFilter(Element filterElem) {
        String filterName = null;
        String filterClass = null;
        NodeList nodeList = filterElem.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals("filter-name")) {
                    filterName = XmlUtils.getElementText((Element) n).trim();
                } else if (n.getNodeName().equals("filter-class")) {
                    filterClass = org.apache.myfaces.shared_impl.util.xml.XmlUtils.getElementText((Element) n).trim();
                } else if (n.getNodeName().equals("description") || n.getNodeName().equals("init-param")) {
                } else {
                    if (log.isLoggable(Level.FINE)) log.fine("Ignored element '" + n.getNodeName() + "' as child of '" + filterElem.getNodeName() + "'.");
                }
            } else {
                if (log.isLoggable(Level.FINE)) log.fine("Ignored node '" + n.getNodeName() + "' of type " + n.getNodeType());
            }
        }
        _webXml.addFilter(filterName, filterClass);
    }

    private void readFilterMapping(Element filterMappingElem) {
        String filterName = null;
        String urlPattern = null;
        NodeList nodeList = filterMappingElem.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals("filter-name")) {
                    filterName = org.apache.myfaces.shared_impl.util.xml.XmlUtils.getElementText((Element) n).trim();
                } else if (n.getNodeName().equals("url-pattern")) {
                    urlPattern = org.apache.myfaces.shared_impl.util.xml.XmlUtils.getElementText((Element) n).trim();
                } else if (n.getNodeName().equals("servlet-name")) {
                } else {
                    if (log.isLoggable(Level.FINE)) log.fine("Ignored element '" + n.getNodeName() + "' as child of '" + filterMappingElem.getNodeName() + "'.");
                }
            } else {
                if (log.isLoggable(Level.FINE)) log.fine("Ignored node '" + n.getNodeName() + "' of type " + n.getNodeType());
            }
        }
        _webXml.addFilterMapping(filterName, urlPattern);
    }
}
