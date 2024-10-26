package de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Contains all templates referenced in templidx.xml.
 * <p>
 * templidx.xml contains one line for each available template file.
 * For each template, a name and a url must be given.
 * The name is displayed in the wizard window when creating a new project.
 * The url is the location of the template file relative to templidx.xml.
 * </p>
 * <p>
 * This is a sample file:<br><br>
 * <code>
 * &lt;template-index&gt;<br>
 * &nbsp;&nbsp;&nbsp;&lt;template name="Project template 1" url="template1.xml"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&lt;template name="Project template 2" url="template2.xml"/&gt;<br>
 * &lt;/template-index&gt;<br>
 * </code>
 * <p/>
 * $Id: $
 *
 * <p/>
 * &copy; 2001-2008 CENIT AG Systemhaus (Stuttgart, Germany) <br/>
 * All Rights Reserved.
 * Licensed Material - Property of CENIT AG Systemhaus
 *
 * @author matysiak
 *
 * <!-- add optional tags @version, @see, @since, @deprecated here -->
 *
 */
public class TemplateContainer {

    /** required by JGear
    * @clientCardinality 1
    * @supplierCardinality 0..*
    */
    private de.cenit.eb.sm.tools.eclipse.projectfromtemplate.data.Template lnkTemplate;

    /** available templates from templidx.xml */
    private HashMap<String, Template> templates = new HashMap<String, Template>();

    /** document builder to parse xmldoc */
    private DocumentBuilder documentBuilder;

    /** XML document for list of templates (templidx.xml) */
    private Document xmldoc;

    /** XPath expression */
    private XPath xpath;

    /** base URL of list of templates */
    private String baseUrl;

    /**
    * Creates a new TemplateContainer object.
    *
    * @param instream [in] input stream for list of templates
    * @param baseUrl [in] base URL of input file
    * @throws ParserConfigurationException if parsing fails
    * @throws SAXException if parsing fails
    * @throws IOException if file cannot be read
    * @throws XPathExpressionException if XPath expression is invalid
    */
    public TemplateContainer(InputStream instream, String baseUrl) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        setDocumentBuilder(DocumentBuilderFactory.newInstance().newDocumentBuilder());
        setXmldoc(getDocumentBuilder().parse(instream));
        setXpath(XPathFactory.newInstance().newXPath());
        setBaseUrl(baseUrl);
        initializeTemplates();
    }

    /** Read all templates referenced in templidx.xml.
    *
    * For each template, a new Template object is created.
    *
    * @throws IOException if file cannot be read
    * @throws SAXException if parsing fails
    * @throws ParserConfigurationException if parsing fails
    * @throws XPathExpressionException if XPath expression is invalid
    *
    * @.author matysiak
    *
    * @.threadsafe no
    *
    * <!-- add optional tags @version, @see, @since, @deprecated here -->
    */
    protected void initializeTemplates() throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        NodeList elements = (NodeList) getXpath().evaluate("/template-index/template", getXmldoc(), XPathConstants.NODESET);
        for (int i = 0; i < elements.getLength(); i++) {
            Node element = elements.item(i);
            NamedNodeMap attr = element.getAttributes();
            Node nameNode = attr.getNamedItem("name");
            String name = nameNode.getNodeValue();
            Node urlNode = attr.getNamedItem("url");
            String urlString = null;
            if (null != urlNode) {
                urlString = getBaseUrl() + urlNode.getNodeValue();
            } else {
                Node absUrlNode = attr.getNamedItem("absurl");
                if (null != absUrlNode) {
                    urlString = absUrlNode.getNodeValue();
                }
            }
            URL url = new URL(urlString);
            URLConnection con = url.openConnection();
            Template templ = new Template(con.getInputStream(), getBaseUrl());
            if (null != templ) {
                getTemplates().put(name, templ);
            }
        }
    }

    /**
    * Sets templates.
    *
    * @param templates [in] The templates to set.
    */
    public void setTemplates(HashMap<String, Template> templates) {
        this.templates = templates;
    }

    /**
    * Getter for templates.
    *
    * @return [HashMap] Returns the templates.
    */
    public HashMap<String, Template> getTemplates() {
        return this.templates;
    }

    /**
    * Sets documentBuilder.
    *
    * @param documentBuilder [in] The documentBuilder to set.
    */
    private void setDocumentBuilder(DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    /**
    * Getter for documentBuilder.
    *
    * @return [DocumentBuilder] Returns the documentBuilder.
    */
    private DocumentBuilder getDocumentBuilder() {
        return this.documentBuilder;
    }

    /**
    * Sets xmldoc.
    *
    * @param xmldoc [in] The xmldoc to set.
    */
    private void setXmldoc(Document xmldoc) {
        this.xmldoc = xmldoc;
    }

    /**
    * Getter for xmldoc.
    *
    * @return [Document] Returns the xmldoc.
    */
    private Document getXmldoc() {
        return this.xmldoc;
    }

    /**
    * Sets xpath.
    *
    * @param xpath [in] The xpath to set.
    */
    private void setXpath(XPath xpath) {
        this.xpath = xpath;
    }

    /**
    * Getter for xpath.
    *
    * @return [XPath] Returns the xpath.
    */
    private XPath getXpath() {
        return this.xpath;
    }

    /**
    * Sets baseUrl.
    *
    * @param baseUrl [in] The baseUrl to set.
    */
    private void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
    * Getter for baseUrl.
    *
    * @return [String] Returns the baseUrl.
    */
    private String getBaseUrl() {
        return this.baseUrl;
    }
}
