package guestbook;

import java.net.URL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class URLGrabber {

    public static InputStream getDocumentAsInputStream(URL url) throws IOException {
        InputStream in = url.openStream();
        return in;
    }

    public static InputStream getDocumentAsInputStream(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsInputStream(u);
    }

    public static String getDocumentAsString(URL url) throws IOException {
        StringBuffer result = new StringBuffer();
        InputStream in = url.openStream();
        int c;
        while ((c = in.read()) != -1) result.append((char) c);
        String result_to_string = result.toString();
        return result_to_string;
    }

    public static String getDocumentAsString(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsString(u);
    }

    public static Document stringToDom(String xmlSource) throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlSource)));
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            try {
                String doc = URLGrabber.getDocumentAsString(args[i]);
                System.out.println(doc);
            } catch (MalformedURLException e) {
                System.err.println(args[i] + " cannot be interpreted as a URL.");
            } catch (IOException e) {
                System.err.println("Unexpected IOException: " + e.getMessage());
            }
        }
    }
}
