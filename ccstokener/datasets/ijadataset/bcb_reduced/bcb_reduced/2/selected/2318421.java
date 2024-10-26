package demo.jse;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;
import util.WgetUtil;

/**
 * @author reniaL
 */
public class Dom4jDemo {

    public static void main(String[] args) throws IOException, DocumentException, SAXException {
        Dom4jDemo jdom = new Dom4jDemo();
        jdom.testRead();
    }

    private static final String PATH = "/home/test.xml";

    /**
   * �� url
   */
    public void testReadUrl() throws IOException, DocumentException, SAXException {
        String urlStr = "";
        URL url;
        HttpURLConnection conn;
        try {
            url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.getResponseCode();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println(conn.getURL().toString());
        if (urlStr.equals(conn.getURL().toString())) {
            String pageStr = WgetUtil.wgetTxt(conn);
            Document doc = DocumentHelper.parseText(pageStr);
            Element root = doc.getRootElement();
            System.out.println("size: " + root.elements().size());
        } else {
            System.out.println("redirect");
        }
    }

    /**
   * ���ļ�
   */
    @SuppressWarnings("unchecked")
    public void testRead() throws IOException, DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new FileInputStream(PATH));
        Element root = document.getRootElement();
        for (Iterator<Element> iter = root.elementIterator(); iter.hasNext(); ) {
            Element element = iter.next();
            System.out.println("user: " + element.getText());
            System.out.println("name: " + element.attributeValue("name"));
            System.out.println("blog: " + element.attributeValue("blog"));
        }
    }

    /**
   * д�ļ�
   */
    public void testWrite() throws IOException {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("root");
        Element element1 = root.addElement("user");
        element1.addAttribute("name", "reniaL").addAttribute("blog", "���ǵ�").addText("�޼�");
        XMLWriter writer = new XMLWriter(new FileOutputStream(PATH));
        writer.write(document);
        writer.close();
    }
}
