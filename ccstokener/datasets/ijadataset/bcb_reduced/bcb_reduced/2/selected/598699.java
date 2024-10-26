package org.jmx4odp.j4oNet;

import java.io.Serializable;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

/**
 * This object acts as a wrapper to a DOM parser. It has convient methods for
 * creating and loading a stream from a URL, file, etc. Using the DOM parser
 * this object will return a Document object based upon the source.
 *
 * @author Lucas McGregor
 **/
public class XmlDataLoader implements Serializable {

    public static final boolean VERBOSE = false;

    public XmlDataLoader() {
    }

    /**
     * Give the URL complient String for an XML document's location,
     *  this will return an Document object
     **/
    public Document getDocument(String urlString) throws SAXException, IOException, MalformedURLException {
        if (VERBOSE) System.out.println("XmlDataLoader: reading xml file " + urlString);
        try {
            URL url = new URL(urlString);
            return getDocument(url);
        } catch (Exception e) {
            if (VERBOSE) {
                System.out.println("XmlDataLoader: Error in reading data");
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Will return an XML document object for a XML file found
     * at the URL's location.
     **/
    public Document getDocument(URL url) throws SAXException, IOException {
        InputStream ins = url.openStream();
        return getDocument(ins);
    }

    /**
     * Will return an XML document object for a XML file.
     **/
    public Document getDocument(File file) throws SAXException, IOException {
        return getDocument(new FileInputStream(file));
    }

    /**
     * Will return an XML document object for a XML file read
     * from the InputStream.
     **/
    public Document getDocument(InputStream ins) throws SAXException, IOException {
        InputSource is = new InputSource(ins);
        DOMParser domParser = new DOMParser();
        domParser.parse(is);
        return domParser.getDocument();
    }
}
