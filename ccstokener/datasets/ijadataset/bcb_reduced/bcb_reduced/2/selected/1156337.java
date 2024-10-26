package jict;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author liadao
 */
public class Jict {

    public Word xmlPaser(String searchWord) {
        Document document = getDocument(searchWord);
        Word word = null;
        if (document != null) {
            word = this.processDocument(document);
            word.setWord(searchWord);
        }
        return word;
    }

    private Document getDocument(String searchWord) {
        Document document = null;
        try {
            URL url = new URL("http://dict.cn/ws.php?utf8=true&q=" + searchWord);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = conn.getInputStream();
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
                document = documentBuilder.parse(in);
            }
        } catch (SAXException ex) {
            Logger.getLogger(Jict.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Jict.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Jict.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Jict.class.getName()).log(Level.SEVERE, null, ex);
        }
        return document;
    }

    private Word processDocument(Document document) {
        Word word = new Word();
        document.normalize();
        Element element = document.getDocumentElement();
        NodeList nodeList = element.getChildNodes();
        int size = nodeList.getLength();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Node node = nodeList.item(i);
                if (node != null) {
                    String nodeName = node.getNodeName();
                    String nodeValue = null;
                    if ("audio".equals(nodeName)) {
                        nodeValue = node.getFirstChild().getNodeValue();
                        word.setAudio(nodeValue);
                    } else if ("pron".equals(nodeName)) {
                        nodeValue = node.getFirstChild().getNodeValue();
                        word.setPron(nodeValue);
                    } else if ("def".equals(nodeName)) {
                        nodeValue = node.getFirstChild().getNodeValue();
                        word.setDef(nodeValue);
                    } else if ("rel".equals(nodeValue)) {
                        nodeValue = node.getFirstChild().getNodeValue();
                        word.setRel(nodeValue);
                    } else if ("sugg".equals(nodeName)) {
                        nodeValue = node.getFirstChild().getNodeValue();
                        word.setSugg(nodeValue);
                    }
                }
            }
        }
        return word;
    }
}
