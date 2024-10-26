package guestbook;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.String;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class parsingSites {

    public static String[] parsingData(String location, String field, int flag, int fieldsNum, int modResult) {
        try {
            MyUrl url = new MyUrl(location);
            String xmlAsString = createStringFromHtml(url);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlAsString));
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();
            NodeList nodeLst = doc.getElementsByTagName(field);
            String[] data = new String[16];
            for (int i = 0; i < nodeLst.getLength(); i++) {
                Node fstNode = nodeLst.item(i);
                if (flag == 0) {
                    data[i] = fstNode.getTextContent();
                    if ((data[i] == "null") || (data[i] == null)) return null;
                }
            }
            if (nodeLst.getLength() == 0) return null;
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[] getDataFromXml(String[] result, String field, String url) {
        int i = 0;
        result = null;
        while ((result == null) && (i < 5)) {
            result = parsingData(url, field, 0, 3, 2);
            i++;
        }
        return result;
    }

    public static String createStringFromHtml(MyUrl url) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.getUrl().openStream(), "UTF-8"));
            String line;
            String xmlAsString = "";
            while ((line = reader.readLine()) != null) {
                xmlAsString += line;
            }
            reader.close();
            return xmlAsString;
        } catch (Exception e) {
            return null;
        }
    }
}
