package ch10.p6;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Assignment p10.6
 * @author Andreas
 *
 */
public class SpellChecker {

    public static final String DICTIONARY_REST_API = "https://www.google.com/tbproxy/spell?lang=en&hl=en";

    public static Document doSpellCheck(String pText) {
        Document doc;
        try {
            long start = System.nanoTime();
            StringBuffer requestXML = new StringBuffer();
            requestXML.append("<spellrequest textalreadyclipped=\"0\"" + " ignoredups=\"1\"" + " ignoredigits=\"1\" ignoreallcaps=\"0\"><text>");
            requestXML.append(pText);
            requestXML.append("</text></spellrequest>");
            URL url = new URL(DICTIONARY_REST_API);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(requestXML.toString());
            out.close();
            InputStream in = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            long end = System.nanoTime();
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = br.read()) != -1) {
                sb.append((char) c);
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(sb.toString()));
            doc = db.parse(is);
            in.close();
            System.out.println((end - start) / 1000000);
            System.out.println(sb.toString());
            return doc;
        } catch (Exception e) {
            System.out.println("Exception " + e);
            return null;
        }
    }
}
