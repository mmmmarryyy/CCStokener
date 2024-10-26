package net.sf.xsltbuddy.xslt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.xsltbuddy.XSLTBuddy;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * This class is used to provide utilities to XML routines (like xml feeds, etc)
 */
public class XMLUtil {

    private XSLTBuddy buddy;

    private static final Category logger = Category.getInstance(XMLUtil.class.getName());

    /** Main constructor
   *
   * @throws Exception
   */
    public XMLUtil(XSLTBuddy buddy) throws Exception {
        this.buddy = buddy;
    }

    /** Get XML Node Feed
   *
   * @param key
   * @param value
   * @throws Exception
   */
    public Node getFeed(String source) throws Exception {
        HTTPUtil httpUtil = new HTTPUtil(this.buddy);
        InputSource isource = new InputSource(httpUtil.getURLStream(source));
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(isource);
        return (Node) doc.getDocumentElement();
    }

    /** Parse into DOM element
   *
   * @param url
   * @return Document element
   */
    public Element toXML(URL url) throws Exception {
        return this.toXML(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /** Parse into DOM element
   *
   * @param xml
   * @return Document element
   */
    public Element toXML(String xml) throws Exception {
        StringReader r = new StringReader(xml);
        Element el = this.toXML(r);
        return el;
    }

    /** Parse into DOM element
   *
   * @param xml
   * @return document element
   */
    public Element toXML(Reader xml) throws Exception {
        InputSource isource = new InputSource(xml);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(isource);
        return doc.getDocumentElement();
    }
}
