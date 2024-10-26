package org.eveapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 *
 * @author sscoble
 */
public class eveapi {

    public static boolean getCharInfo(Vector apiInfo) {
        return true;
    }

    public static String getItems() {
        return "";
    }

    public static void getSkillTree() {
    }

    public static String getRefTypes() {
        try {
            URL url = new URL(props.baseURL + props.refTypes);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            String toParse = "";
            while ((inputLine = reader.readLine()) != null) {
                toParse += inputLine;
            }
            System.out.print(toParse);
            InputSource is = new InputSource(conn.getInputStream());
            Document d = db.parse(is);
            Element root = d.getElementById("eveapi");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "Error";
    }
}
