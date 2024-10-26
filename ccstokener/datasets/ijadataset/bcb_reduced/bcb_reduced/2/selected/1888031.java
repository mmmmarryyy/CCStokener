package com.jedox.etl.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import com.jedox.etl.core.component.ConfigurationException;

/**
 * Reader Class for XML
 * @author Christian Schwarzinger. Mail: christian.schwarzinger@proclos.com
 *
 */
public class XMLReader {

    private URL url;

    public XMLReader() {
    }

    /**
	 * gets an XML-Document from a file
	 * @param filename
	 * @return the XML Document
	 * @throws ConfigurationException
	 */
    public Document readDocument(String filename) throws ConfigurationException {
        try {
            URL url = new URL(filename);
            return readDocument(url);
        } catch (Exception e) {
        }
        ;
        try {
            File f = new File(filename);
            return readDocument(f.toURI().toURL());
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    /**
	 * gets an XML-Document from a file
	 * @param reader Reader that contains that String
	 * @return the XML Document
	 * @throws ConfigurationException
	 */
    public Document readDocument(Reader reader) throws ConfigurationException {
        try {
            Document document = new SAXBuilder().build(reader);
            return document;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    /**
	 * gets the URL of the XML resource read by this reader
	 * @return the URL
	 */
    public URL getURL() {
        return url;
    }

    /**
	 * reads a XML Document from an URL
	 * @param url
	 * @return the XML Document
	 * @throws IOException
	 * @throws JDOMException
	 */
    private Document readDocument(URL url) throws IOException, JDOMException {
        this.url = url;
        Reader r = new BufferedReader(new InputStreamReader(url.openStream(), "UTF8"));
        Document document = new SAXBuilder().build(r);
        return document;
    }
}
