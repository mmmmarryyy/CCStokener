package hu.schmidtsoft.map.util;

import hu.schmidtsoft.map.util.UtilTime.TimeMeasure;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UtilSAX {

    private static SAXParserFactory instance;

    private static SAXParserFactory getInstance() {
        if (instance != null) {
            return instance;
        }
        try {
            try {
                instance = (SAXParserFactory) Class.forName("com.bluecast.xml.JAXPSAXParserFactory").newInstance();
                return instance;
            } catch (Exception e) {
                UtilLog.error("Error initiating built-in parser: " + e.getMessage());
                UtilLog.error("Trying the Java way... ");
            }
            instance = SAXParserFactory.newInstance();
            return instance;
        } catch (Exception e) {
            UtilLog.error("Error initiating built-in parser: " + e.getMessage());
            UtilLog.error("Trying the Java way... ");
        }
        instance = SAXParserFactory.newInstance();
        return instance;
    }

    public static void parse(URL url, DefaultHandler handler) throws ParserConfigurationException, SAXException, IOException {
        TimeMeasure tm = UtilTime.createTimeMeasure("xml factory");
        InputStream is = url.openStream();
        try {
            SAXParserFactory factory = UtilSAX.getInstance();
            tm.finish();
            tm = UtilTime.createTimeMeasure("parser");
            SAXParser saxParser = factory.newSAXParser();
            tm.finish();
            tm = UtilTime.createTimeMeasure("parse");
            saxParser.parse(is, handler);
            tm.finish();
        } finally {
            is.close();
        }
    }

    public static void parse(InputStream is, DefaultHandler handler) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = UtilSAX.getInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(is, handler);
    }

    public static void parse(String url, DefaultHandler handler) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = UtilSAX.getInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(url, handler);
    }

    public static void parse(File f, DefaultHandler si) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = UtilSAX.getInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(f, si);
    }

    public static ContentHandler export_(OutputStream os) throws TransformerConfigurationException {
        StreamResult streamResult = new StreamResult(os);
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = hd.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        hd.setResult(streamResult);
        return hd;
    }

    public static UtilSaxExport export(OutputStream os) throws IOException, TransformerConfigurationException {
        return new UtilSaxExport(export_(os));
    }
}
