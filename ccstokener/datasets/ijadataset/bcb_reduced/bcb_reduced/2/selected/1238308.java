package org.bing.engine.utility.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.bing.engine.common.logging.Log;
import org.bing.engine.common.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JdkDomHelper {

    private static final Log logger = LogFactory.getLog(JdkDomHelper.class);

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private static DocumentBuilder builder = null;

    static {
        try {
            builder = factory.newDocumentBuilder();
        } catch (Exception e) {
            logger.error("Fail to init dom builder! ", e);
            throw new RuntimeException(e);
        }
    }

    public static Document build(URL url) {
        try {
            return build(url.openStream());
        } catch (IOException e) {
            logger.error("Fail to build dom from " + url, e);
            return null;
        }
    }

    public static Document build(InputStream ins) {
        try {
            return builder.parse(ins);
        } catch (Exception e) {
            logger.error("Fail to build dom from " + ins, e);
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    logger.error("Fail to close stream! ", e);
                }
            }
        }
    }

    public static String getSingleSubTextByName(Element e, String name, String def) {
        NodeList ns = e.getElementsByTagName(name);
        if (ns.getLength() > 0) {
            Node n = ns.item(0);
            return n.getTextContent();
        } else {
            return def;
        }
    }

    public static String getSingleSubAttributeByName(Element e, String name, String attribute, String def) {
        NodeList ns = e.getElementsByTagName(name);
        if (ns.getLength() > 0) {
            Element ne = (Element) ns.item(0);
            String res = ne.getAttribute(attribute);
            if (res == null || res.length() == 0) {
                return def;
            } else {
                return res;
            }
        } else {
            return def;
        }
    }
}
