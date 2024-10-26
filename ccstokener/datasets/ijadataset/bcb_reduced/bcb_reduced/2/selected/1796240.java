package net.sf.nic.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.*;

/**
 * Implements an ResourceBundle that support XML files as sources.
 *
 * @author Juergen_Kellerer, 2009-12-07
 * @version 1.0
 */
public class XMLResourceBundle extends ResourceBundle {

    private static class Control extends ResourceBundle.Control {

        public List<String> getFormats(String baseName) {
            return XML;
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            if ((baseName == null) || (locale == null) || (format == null) || (loader == null)) throw new NullPointerException();
            if (!XML.contains(format)) return null;
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, format);
            URL url = loader.getResource(resourceName);
            if (url == null) return null;
            URLConnection c = url.openConnection();
            if (c == null) return null;
            if (reload) c.setUseCaches(false);
            InputStream bis = new BufferedInputStream(c.getInputStream());
            try {
                return new XMLResourceBundle(bis);
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                bis.close();
            }
        }
    }

    private static final String MESSAGE_FORMAT_PREFIX = "mf.";

    private static final String LOCALE_NS = "http://net.sf.nic/messages";

    private static final String LOCALE_TAG = "m";

    private static final String LOCALE_PREFIX = "";

    private static final List<String> XML = Collections.singletonList("xml");

    public static final ResourceBundle.Control CONTROL = new Control();

    private final Map<String, Object> resources = new HashMap<String, Object>();

    private XMLResourceBundle(InputStream in) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);
        in.close();
        populateElements(doc, LOCALE_NS, LOCALE_PREFIX);
    }

    private void populateElements(Document doc, String ns, String prefix) {
        NodeList nl = doc.getElementsByTagNameNS(ns, LOCALE_TAG);
        for (int i = nl.getLength() - 1; i >= 0; i--) {
            Element el = (Element) nl.item(i);
            if (Boolean.TRUE.toString().equalsIgnoreCase(el.getAttribute("xmlcontent"))) resources.put(prefix.concat(el.getAttribute("id")), toXmlString(el)); else resources.put(prefix.concat(el.getAttribute("id")), toPlainString(el));
        }
    }

    private String toPlainString(Element el) {
        StringBuilder content = new StringBuilder();
        NodeList nl = el.getChildNodes();
        for (int i = 0, len = nl.getLength(); i < len; i++) {
            if (nl.item(i).getNodeType() != Node.TEXT_NODE) continue;
            content.append(nl.item(i).getTextContent());
        }
        return content.toString();
    }

    private String toXmlString(Element el) {
        try {
            StringWriter buffer = new StringWriter(256);
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.newTransformer().transform(new DOMSource(el), new StreamResult(buffer));
            return buffer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public Object handleGetObject(String key) {
        Object o = resources.get(key);
        if (o == null && key.startsWith(MESSAGE_FORMAT_PREFIX)) {
            o = resources.get(key.substring(3));
            if (o != null) {
                o = new MessageFormat(o.toString());
                resources.put(key, o);
            }
        }
        return o;
    }

    /**
	 * {@inheritDoc}
	 */
    public Enumeration<String> getKeys() {
        return Collections.enumeration(resources.keySet());
    }
}
