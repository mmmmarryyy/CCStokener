package sfljtse.tsf.coreLayer.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @title       : XMLdocument       
 * @description :   
 * @date        : 26-lug-2005   
 * @author      : Alberto Sfolcini  <a.sfolcini@gmail.com>
 */
public class XMLdocument {

    /**
     * This method gets an XML document from the given URL.
     */
    public static Document getDocument(DocumentBuilder db, String urlString) {
        try {
            URL url = new URL(urlString);
            try {
                URLConnection URLconnection = url.openConnection();
                HttpURLConnection httpConnection = (HttpURLConnection) URLconnection;
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream in = httpConnection.getInputStream();
                    try {
                        Document doc = db.parse(in);
                        return doc;
                    } catch (org.xml.sax.SAXException e) {
                    }
                }
            } catch (IOException e) {
            }
        } catch (MalformedURLException e) {
        }
        return null;
    }

    /**
     * This method gets an XML document from the given FILE.
     */
    public static Document getFileDocument(DocumentBuilder db, File filePath) {
        try {
            Document doc = db.parse(filePath);
            return doc;
        } catch (SAXException e) {
        } catch (IOException e) {
        }
        return null;
    }
}
