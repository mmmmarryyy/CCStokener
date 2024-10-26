package com.adactus.mpeg21.xml.schema;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javolution.context.ObjectFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Create a w3c XML Document object. Pattern:<br>
 * <br>
 * 1. {@link DocumentBuilderFactory}<br>
 * 2. {@link DocumentBuilder}<br>
 * 3. {@link Document}<br>
 * <br>
 * Any errors and warnings are printed during the parsing by using the ErrorHandling interface.
 *
 * This class it not thread safe.
 *
 * @author Thomas Rørvik Skjølberg
 */
public class DocumentSource extends ErrorHandlerImpl {

    private static final ObjectFactory<DocumentSource> FACTORY = new ObjectFactory<DocumentSource>() {

        protected DocumentSource create() {
            return new DocumentSource(true);
        }

        protected void cleanup(DocumentSource obj) {
            obj.reset();
        }
    };

    public static DocumentSource newInstance() {
        return FACTORY.object();
    }

    public static void recycle(DocumentSource ck) {
        FACTORY.recycle(ck);
    }

    protected static URL getResourceURL(String name) throws MalformedURLException {
        URL result = null;
        ClassLoader classLoader = name.getClass().getClassLoader();
        if (classLoader != null) {
            result = classLoader.getResource(name);
        }
        if (result == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                result = classLoader.getResource(name);
            }
        }
        if (result == null && (name.indexOf("://") == -1)) {
            File file = new File(name);
            if (file.exists()) {
                return file.toURI().toURL();
            }
        }
        return result;
    }

    protected DocumentBuilder documentBuilder;

    protected DocumentBuilderFactory documentFactory;

    protected EntityResolver entityResolver;

    private boolean namespaceAware;

    /**
	 * Create a non-validating document source.
	 * 
	 * @param namespaceAware namespace aware enabled or disabled
	 */
    public DocumentSource(boolean namespaceAware) {
        super(true);
        this.namespaceAware = namespaceAware;
    }

    public DocumentSource() {
        this(true);
    }

    /**
	 * 
	 * Initialize {@link DocumentBuilder}.
	 * 
	 * @throws Exception
	 */
    public void initBuilder() throws Exception {
        documentBuilder = documentFactory.newDocumentBuilder();
        documentBuilder.setErrorHandler(this);
        if (entityResolver != null) {
            documentBuilder.setEntityResolver(entityResolver);
        }
    }

    /**
	 * 
	 * Initialize {@link DocumentBuilderFactory}.
	 * 
	 * @throws Exception
	 */
    public void initFactory() throws Exception {
        documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(namespaceAware);
        documentFactory.setIgnoringElementContentWhitespace(true);
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
        if (documentBuilder != null) {
            documentBuilder.setEntityResolver(entityResolver);
        }
    }

    /**
	 * Create document from an {@link InputStream}.
	 * 
	 * @param reader the reader
	 * @return the document
	 * @throws Exception
	 */
    public Document getDocument(Reader reader) throws Exception {
        if (documentBuilder == null) {
            initFactory();
            initBuilder();
        }
        reset();
        Document root = documentBuilder.parse(new InputSource(reader));
        root.normalizeDocument();
        return root;
    }

    /**
	 * Create document from an {@link InputStream}.
	 * 
	 * @param in the input stream
	 * @return the document
	 * @throws Exception
	 */
    public Document getDocument(InputStream in) throws Exception {
        if (documentBuilder == null) {
            initFactory();
            initBuilder();
        }
        reset();
        Document root = documentBuilder.parse(in);
        root.normalizeDocument();
        return root;
    }

    /**
	 * 
	 * Create a new, empty {@link Document}.
	 * 
	 * @return a new document
	 */
    public Document newDocument() {
        return documentBuilder.newDocument();
    }

    /**
	 * Create document from an {@link URL}.
	 * 
	 * @param url the location of the input
	 * @return the document
	 * @throws Exception
	 */
    public Document getDocument(URL url) throws Exception {
        return getDocument(url.openStream());
    }

    /**
	 * Create document from a {@link String}.
	 * 
	 * @param urlString the input url location
	 * @return the document
	 * @throws Exception
	 */
    public Document getDocument(String urlString) throws Exception {
        return getDocument(getURL(urlString));
    }

    protected URL getURL(String urlString) throws MalformedURLException {
        URL url;
        if (urlString.indexOf("://") != -1) {
            url = new URL(urlString);
        } else {
            url = getResourceURL(urlString);
        }
        if (url == null) throw new RuntimeException(urlString + " not found");
        return url;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }
}
