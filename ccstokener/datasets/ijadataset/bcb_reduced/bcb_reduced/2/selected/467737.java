package iconsensus.common.volumedata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlLoader {

    Document dom = null;

    File file = null;

    URL url = null;

    public XmlLoader(URL u) throws IOException {
        url = u;
        parseXmlFile();
    }

    public XmlLoader(File f) throws IOException {
        file = f;
        parseXmlFile();
    }

    private void parseXmlFile() throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (file != null) {
                dom = db.parse(file);
            } else {
                dom = db.parse(url.openStream());
            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        }
    }

    /**
	 * I take a xml element and the tag name, look for the tag and get the text
	 * content i.e for <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is name I will return John
	 * @param ele
	 * @param tagName
	 * @return
	 */
    public String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }

    /**
	 * Calls getTextValue and returns a int value
	 * 
	 * @param ele
	 * @param tagName
	 * @return
	 */
    public int getIntValue(Element ele, String tagName) {
        return Integer.parseInt(getTextValue(ele, tagName));
    }

    /**
	 * Calls getTextValue and returns a double value
	 * 
	 * @param ele
	 * @param tagName
	 * @return
	 */
    public double getDoubleValue(Element ele, String tagName) {
        return Double.parseDouble(getTextValue(ele, tagName));
    }

    public Document getDom() {
        return dom;
    }

    /**
	 * Helper method that gets all elements nested within an XML node that is
	 * nested within a given parent node
	 * 
	 * @param tagName
	 * @param parentTagName
	 * @return
	 */
    public ArrayList getElementsForSpecifiedTagWithinParentTag(String parentTagName, String tagName) {
        ArrayList childrenElementList = new ArrayList();
        Document rootDom = this.getDom();
        NodeList nl = rootDom.getElementsByTagName(parentTagName);
        if (nl != null && nl.getLength() > 0) {
            NodeList childNodeList = nl.item(0).getChildNodes();
            NodeList childNodesOfParent = null;
            if (childNodeList != null && childNodeList.getLength() > 0) {
                for (int k = 0; k < childNodeList.getLength(); k++) {
                    Node childOfParentTag = childNodeList.item(k);
                    if (childOfParentTag instanceof Element) {
                        Element childOfParentTagEl = (Element) (childOfParentTag);
                        if (childOfParentTagEl.getTagName().equalsIgnoreCase(tagName)) {
                            childNodesOfParent = childOfParentTagEl.getChildNodes();
                            break;
                        }
                    }
                }
                if (childNodesOfParent != null && childNodesOfParent.getLength() > 0) {
                    for (int i = 0; i < childNodesOfParent.getLength(); i++) {
                        Node childElement = childNodesOfParent.item(i);
                        if (childElement instanceof Element) {
                            childrenElementList.add(childElement);
                        }
                    }
                }
            }
        }
        return childrenElementList;
    }

    /**
	 * 
	 * @param elementName
	 * @param parentTag
	 * @return
	 */
    public String getTextValue(String parentTag, String elementName) {
        Document rootDom = this.getDom();
        NodeList nl = rootDom.getElementsByTagName(parentTag);
        Element ele = null;
        if (nl != null && nl.getLength() > 0) {
            ele = (Element) nl.item(0);
            return getTextValue(ele, elementName);
        }
        return null;
    }

    /**
	 * 
	 * @param elementName
	 * @param nodeName
	 * @return
	 */
    public Element getElementUnderSpecifiedNodeName(String nodeName, String elementName) {
        Document rootDom = this.getDom();
        NodeList nl = rootDom.getElementsByTagName(nodeName);
        Element nodeTagEl = null;
        if (nl != null && nl.getLength() > 0) {
            nodeTagEl = (Element) nl.item(0);
            NodeList elementSearcResults = nodeTagEl.getElementsByTagName(elementName);
            if (elementSearcResults.getLength() > 0) {
                return ((Element) (elementSearcResults.item(0)));
            }
        }
        return null;
    }

    /**
	 * 
	 * @param elementName
	 * @param nodeName
	 * @return
	 */
    public Element getElementUnderSpecifiedParentElement(Element parentEl, String elementName) {
        NodeList elementSearcResults = parentEl.getElementsByTagName(elementName);
        if (elementSearcResults.getLength() > 0) {
            return ((Element) (elementSearcResults.item(0)));
        }
        return null;
    }

    /**
	 * 
	 * @param element
	 * @return
	 */
    public ArrayList getElementsUnderSpecifiedElement(Element element) {
        ArrayList resultList = new ArrayList();
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                resultList.add(child);
            }
        }
        return resultList;
    }

    /**
	 * 
	 * @param element
	 * @return
	 */
    public ArrayList getElementsUnderSpecifiedElement(String elementName) {
        ArrayList resultList = new ArrayList();
        Document rootDom = this.getDom();
        NodeList nl = rootDom.getElementsByTagName(elementName);
        Element ele = null;
        if (nl != null && nl.getLength() > 0) {
            ele = (Element) nl.item(0);
            return getElementsUnderSpecifiedElement(ele);
        }
        return resultList;
    }

    /**
	 * 
	 * @param element
	 * @return
	 */
    public ArrayList getAllElementsWithSpecifiedNameUnderParentElement(String parentElement, String elementName) {
        ArrayList resultList = new ArrayList();
        Document rootDom = this.getDom();
        NodeList nl = rootDom.getElementsByTagName(parentElement);
        Element ele = null;
        if (nl != null && nl.getLength() > 0) {
            ele = (Element) nl.item(0);
            NodeList allElementsList = ele.getElementsByTagName(elementName);
            for (int i = 0; i < allElementsList.getLength(); i++) {
                resultList.add(allElementsList.item(i));
            }
        }
        return resultList;
    }

    /**
	 * 
	 * @param elementName
	 * @param parentTagEl
	 * @return
	 */
    public String getStringValueForElementUnderParentTag(Element parentTagEl, String elementName) {
        String result = "Unspecified";
        NodeList elementNames = parentTagEl.getElementsByTagName(elementName);
        if (elementNames.getLength() > 0) {
            result = ((Element) (elementNames.item(0))).getTextContent().trim();
        }
        return result;
    }
}
