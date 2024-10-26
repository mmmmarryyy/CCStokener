package com.incendiaryblue.applet;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 *	Cache provided for applets that are handing XML. This cache will contain
 *	a set number of XML documents and map them based on the URLs that were used
 *	to acquire them.<P>
 *
 *	This is a singleton class.
 */
public class DocumentCache {

    /**
	 *	The singleton instance of this class. Use <CODE>getInstance<CODE> to
	 *	acquire this.
	 *
	 *	@see	#getInstance
	 */
    private static DocumentCache instance;

    /**
	 *	Map of cached documents. The keys of this map are the URLs of the
	 *	documents cached. Each key maps to its associated XML document.
	 */
    private Map documentMap = new HashMap();

    /**
	 *	Maximum number of documents able to be cached. Once the cache has
	 *	reached capacity, it is cleared and any subsequent requests are
	 *	re-cached.
	 */
    private int maxDocuments = 10;

    /**
	 *	Obtains an instance of the document cache, creating one if necessary.
	 *	The document cache singleton object is thread-safe on creation and use.
	 */
    public static DocumentCache getInstance() {
        if (instance == null) {
            synchronized (DocumentCache.class) {
                if (instance == null) {
                    instance = new DocumentCache();
                }
            }
        }
        return instance;
    }

    /**
	 *	Obtains the document specified from the given URL. The given URL is
	 *	first sought in the cache and, if not found, a new stream is opened
	 *	from it to obtain the document.
	 *
	 *	@param	url	URL of XML document to obtain.
	 *
	 *	@return	XML document contained at the given URL.
	 */
    public Document getDocument(URL url) throws IOException, ParserConfigurationException, SAXException {
        Object document = this.documentMap.get(url);
        if (document == null) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            try {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                document = docBuilder.parse(inputStream);
                if (this.documentMap.size() == this.maxDocuments) {
                    this.documentMap.clear();
                }
                this.documentMap.put(url, document);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    System.err.println("Document cache warning: Could not " + "close XML input stream");
                }
            }
        }
        return (Document) document;
    }

    /**
	 *	Empties the document cache. Subsequent new requests will be re-cached.
	 */
    public void empty() {
        this.documentMap.clear();
    }
}
