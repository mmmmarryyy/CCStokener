package org.gromurph.xml;

import java.io.*;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Reads a document consisting of one or more PersistentObjects which
 * have been persisted to XML. This class reads XML from some data source
 * and instantiates PersistentObjects based on the contents.
 * <P>
 * This code was developed by NASA's Goddard Space Flight Center, Code 588
 * for the Science Goal Monitor (SGM) project.
 * 
 * @author Jeremy Jones
**/
public class DocumentReader {

    public static final String CLASS_PROPERTY = "class";

    /**
	 * Stream containing the XML.
	**/
    private InputSource fInput;

    /**
	 * Creates a new DocumentReader for reading objects from the
	 * specified input stream.
	 *
	 * @param stream will read objects from this stream
	**/
    public DocumentReader(InputStream stream) {
        fInput = new InputSource(stream);
    }

    /**
	 * Creates a new DocumentReader for reading objects from the
	 * specified URL.
	 *
	 * @param url will read objects from this location
	 * @throws IOException thrown if I/O-related error occurs
	**/
    public DocumentReader(URL url) throws IOException {
        fInput = new InputSource(url.openStream());
    }

    /**
	 * Creates a new DocumentReader for reading objects from the
	 * specified input file.
	 *
	 * @param f will read objects from this file
	 * @throws FileNotFoundException if unable to open input file
	**/
    public DocumentReader(File f) throws FileNotFoundException {
        fInput = new InputSource(new BufferedInputStream(new FileInputStream(f)));
    }

    /**
	 * Creates a new DocumentReader for reading objects from the
	 * specified Reader
	 *
	 * @param f will read objects from this file
	**/
    public DocumentReader(Reader r) {
        fInput = new InputSource(r);
    }

    /**
	 * Reads the contents of the document from the input stream,
	 * instantiates objects from the document contents, and returns the new
	 * objects.  The stream is closed before returning.
	 * <P>
	 * The XML document must follow the following form: a single
	 * root tag (any name) containing one or more branches each of which
	 * contains the node information for a single PersistentObject of
	 * class specified as an argument to this method. These PersistentObjects
	 * may contain any configuration of child objects that it wishes
	 * (via its readObject() and writeObject() methods), but DocumentReader
	 * must know the class to instantiate for the top-level objects.
	 *
	 * @param objectType PersistentObject impl to read from the input stream
	 * @return new objects created from contents of input stream
	 * @throws DocumentException thrown if document-related error occurs
	 * @throws IOException thrown if I/O-related error occurs
	**/
    public PersistentObject[] readObjects(Class objectType) throws DocumentException, IOException {
        if (!PersistentObject.class.isAssignableFrom(objectType)) {
            throw new IllegalArgumentException("DocumentReader.readObjects() argument must implement PersistentObject interface");
        }
        PersistentNode root = readDocument();
        if (root == null) {
            throw new DocumentException("Unable to reconstruct document");
        }
        PersistentNode[] objNodes = root.getElements();
        PersistentObject[] objects = new PersistentObject[objNodes.length];
        for (int i = 0; i < objNodes.length; ++i) {
            objects[i] = createObject(objectType, objNodes[i]);
        }
        return objects;
    }

    /**
	 * Reads the contents of the document from the input stream,
	 * instantiates objects from the document contents, and returns the new
	 * objects.  The stream is closed before returning.
	 * <P>
	 * readObject() differs from readObjects() in that it returns a single
	 * PersistentObject derived from the root of the document tree.
	 * Thus there is no enclosing root tag - the root *is* the PersistentNode.
	 *
	 * @param objectType PersistentObject impl to read from the input stream
	 * @return new object created from contents of input stream
	 * @throws DocumentException thrown if document-related error occurs
	 * @throws IOException thrown if I/O-related error occurs
	**/
    public PersistentObject readObject(Class objectType) throws DocumentException, IOException {
        if (!PersistentObject.class.isAssignableFrom(objectType)) {
            throw new IllegalArgumentException("DocumentReader.readObjects() argument must implement PersistentObject interface");
        }
        PersistentNode root = readDocument();
        if (root == null) {
            throw new DocumentException("Unable to reconstruct document");
        }
        return createObject(objectType, root);
    }

    /**
     * Reads the contents of the document from the input stream,
     * instantiates objects from the document contents, and returns the new
     * objects.  The stream is closed before returning.
     * The type of the newly created object is determined from the document.
     * <P>
     * readObject() differs from readObjects() in that it returns a single
     * PersistentObject derived from the root of the document tree.
     * Thus there is no enclosing root tag - the root *is* the PersistentNode.
     *
     * @return new object created from contents of input stream
     * @throws DocumentException thrown if document-related error occurs
     * @throws IOException thrown if I/O-related error occurs
    **/
    public PersistentObject readObject() throws DocumentException, IOException {
        PersistentNode root = readDocument();
        if (root == null) {
            throw new DocumentException("Unable to reconstruct document");
        }
        if (!root.hasAttribute(CLASS_PROPERTY)) {
            throw new DocumentException("Document has no root class type defined.");
        }
        Class objectType = null;
        try {
            objectType = Class.forName(root.getAttribute(CLASS_PROPERTY));
        } catch (ClassNotFoundException e) {
            throw new DocumentException("Root class type not found", e);
        }
        return createObject(objectType, root);
    }

    private static final String[] sEncodings = new String[] { null, "UTF-8", "ISO-8859-1", "US-ASCII" };

    private String fEncoding = XMLOutputter.DEFAULT_ENCODING;

    public String getEncoding() {
        return (fEncoding == null) ? XMLOutputter.DEFAULT_ENCODING : fEncoding;
    }

    /**
	 * Reads the document contents and returns the root PersistentNode.
	 *
	 * @return root node for the document
	 * @throws DocumentException thrown if document-related error occurs
	 * @throws IOException thrown if I/O-related error occurs
	**/
    public PersistentNode readDocument() throws DocumentException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        DocumentContentHandler handler = new DocumentContentHandler();
        Exception lastE = null;
        for (int i = 0; i < sEncodings.length; i++) {
            try {
                fEncoding = sEncodings[i];
                fInput.setEncoding(fEncoding);
                SAXParser saxParser = factory.newSAXParser();
                saxParser.parse(fInput, handler);
                PersistentNode node = handler.getDocument();
                lastE = null;
                return node;
            } catch (ParserConfigurationException ex) {
                Throwable cause = ex.getCause();
                String detail = (cause == null) ? ex.toString() : cause.toString();
                lastE = new DocumentException(detail, ex);
            } catch (SAXException ex) {
                Throwable cause = ex.getCause();
                String detail = (cause == null) ? ex.toString() : cause.toString();
                lastE = new DocumentException(detail, ex);
            }
        }
        throw new IOException(lastE.toString());
    }

    /**
	 * Creates an object of objectType using the data in PersistentNode.
	 *
	 * @param objectType creates instance of this type
	 * @param node       populates with data from this node
	 * @return           new object instance
	 * @throws DocumentException thrown if document-related error occurs
	**/
    protected PersistentObject createObject(Class objectType, PersistentNode node) throws DocumentException {
        PersistentObject object = null;
        try {
            object = (PersistentObject) objectType.newInstance();
        } catch (IllegalAccessException ex) {
            throw new DocumentException("Unable to instantiate " + objectType.getName());
        } catch (InstantiationException ex) {
            throw new DocumentException("Unable to instantiate " + objectType.getName());
        }
        object.xmlRead(node, object);
        return object;
    }
}
