package cn.edu.wuse.musicxml.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLUtil {

    private static DocumentBuilderFactory factory = null;

    private static DocumentBuilder builder = null;

    private static TransformerFactory tff;

    private static ErrorHandlerMusicXmlImpl handler = null;

    static {
        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            builder = factory.newDocumentBuilder();
            tff = TransformerFactory.newInstance();
            builder.setEntityResolver(new NoEntityResolver());
            handler = new ErrorHandlerMusicXmlImpl();
            builder.setErrorHandler(handler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void call() {
    }

    private XMLUtil() {
    }

    /**
	 * 从文件建立一个Document的实例
	 * @param file xml文件实例
	 * @return Document实例
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws Exception 可能是IO,SAX相关例外
	 */
    public static Document createDocument(File file) throws Exception {
        return builder.parse(file);
    }

    /**
	 * 从一个输入流创建Document的实例
	 * @param inputStream xml输入流实例
	 * @return Document实例
	 * @throws Exception 可能是IO,SAX相关例外
	 */
    public static Document createDocument(InputStream inputStream) throws Exception {
        return builder.parse(inputStream);
    }

    /**
	 * 从远程url读取文件创建Document实例
	 * @param uri 远程uri字符串
	 * @return Document实例
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
    public static Document createDocument(String uri) throws Exception {
        URL url = new URL(uri);
        return builder.parse(url.openStream());
    }

    public static Document createDocument(URL url) throws Exception {
        return builder.parse(url.openStream());
    }

    /**
	 * 读取Docuement实例中的根
	 * @param doc Document的实例
	 * @return Element的实例即树根
	 */
    public static Element readRoot(Document document) {
        return document.getDocumentElement();
    }

    /**
	 * 在element类的元素中，文本本身也是结点，在很多时候知道节点结构是<element>text</element>
	 * ，需要读取文本，提供此方法.
	 * 读取一个指定的元素的文本结点
	 * 注意:只能在一个元素的数据类型是
	 * #PCDATA才能使用此方法
	 * @param element 指定名称的元素实例
	 * @return String的文本信息<br>
	 * 如果元素为空将返回个空串类似 getContentText
	 */
    public static String readText(Node element) {
        if (element == null) return ""; else return element.getTextContent();
    }

    /**
	 * 在element类的元素中，文本本身也是结点，在很多时候知道节点结构是<element>text</element>
	 * ，需要读取文本，提供此方法.
	 * 读取一个指定的元素的剔除了空格的文本结点
	 * @param element 指定名称的元素实例
	 * @return String的文本信息
	 */
    public static String readTrimedText(Node element) {
        if (element == null) return ""; else return element.getTextContent().trim();
    }

    /**
	 * 读取一个指定的元素的指定名称的属性结点的属性值
	 * 如果没有指定名称的结点时,将返回null.
	 * @param element 指定的元素的实例
	 * @param name 属性结点的名称
	 * @return 属性值的String文本
	 */
    public static String readAttribute(Node element, String attributeName) {
        if (element == null) return null;
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) return null;
        Node value = attributes.getNamedItem(attributeName);
        if (value == null) return null;
        return value.getTextContent();
    }

    /**
	 * 返回指定的元素的指定名称的第一个子结点的实例
	 * 对于一个父元素下只有一个某种名称的的子元素
	 * 也可以使用此方法
	 * 如果元素没有子结点,将返回null
	 * @param parent 父结点实例
	 * @param name 指定的结点的名称
	 * @return 指定名称的第一个结点实例
	 */
    public static Element readFirstChild(Node parentNode, String nodeName) {
        if (parentNode != null) {
            NodeList children = parentNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getNodeName().equals(nodeName)) return (Element) children.item(i);
            }
        }
        return null;
    }

    /**
	 * 返回指定的元素的指定名称的子元素的列表
	 * 如果每一偶返回一个长度为0的列表
	 * @param parentNode 父结点实例
	 * @param nodeName 指定的结点的名称
	 * @return 元素的列表(ArrayList)
	 */
    public static List<Element> readChildren(Node parentNode, String nodeName) {
        ArrayList<Element> ret = new ArrayList<Element>();
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals(nodeName)) ret.add((Element) children.item(i));
        }
        return ret;
    }

    /**
	 * 返回指定的元素的所有的子元素的列表
	 * 如果每一偶返回一个长度为0的列表
	 * @param parentNode 父结点实例
	 * @return 元素的列表(ArrayList)
	 */
    public static List<Element> readChildren(Node parentNode) {
        ArrayList<Element> ret = new ArrayList<Element>();
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) ret.add((Element) children.item(i));
        }
        return ret;
    }

    public static void save(Document doc, String filepath) {
        try {
            Transformer tf = tff.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(filepath);
            tf.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
