package com.utils.xmlparser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XML_Parser {

    public XML_Element data;

    public static final int REQUEST_TIMEOUT = 3500;

    /**
	 * 
	 * @param what
	 * @param isXML
	 */
    public XML_Parser(String what, boolean isXML) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser parser = saxParserFactory.newSAXParser();
        DefaultHandler handler = new XML_ParserHandler();
        InputStream input = null;
        if (!isXML) {
            URL url = new URL(what);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(XML_Parser.REQUEST_TIMEOUT);
            connection.connect();
            input = connection.getInputStream();
        } else {
            input = new ByteArrayInputStream(what.getBytes());
        }
        parser.parse(input, handler);
        data = ((XML_ParserHandler) handler).getData();
    }

    /** @return parsed data in XML_Element form */
    public XML_Element getData() {
        return data;
    }
}
