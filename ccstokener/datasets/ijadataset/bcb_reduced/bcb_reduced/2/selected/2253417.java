package net.sourceforge.javautil.common.xml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import net.sourceforge.javautil.common.ReflectionUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualFile;
import net.sourceforge.javautil.common.reflection.ObjectManager;
import net.sourceforge.javautil.common.reflection.cache.ClassCache;
import net.sourceforge.javautil.common.reflection.cache.ClassDescriptor;
import net.sourceforge.javautil.common.xml.annotation.XmlTag;

/**
 * The base {@link XMLElement} for parsing/serializing from/to XML sources/destinations.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: XMLDocument.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class XMLDocument extends XMLDocumentElementComplexType {

    /**
	 * This will get the output stream from the file and create
	 * and {@link XMLDocument} instance for the instance passed.
	 * 
	 * @see #write(OutputStream, XMLDocument)
	 */
    public static void write(IVirtualFile file, Object instance) {
        try {
            write(file.getOutputStream(), instance);
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * @param output The output stream to write the XML document to
	 * @param document The document to write
	 */
    public static <O extends OutputStream> O write(O output, Object instance) {
        new XMLDocumentSerializer().serialize(XMLDocument.getInstance(instance), output);
        return output;
    }

    /**
	 * This will create a new instance of the class.
	 * 
	 * @param clazz The type of document
	 * 
	 * @see #read(IVirtualFile, Object)
	 */
    public static <D> D read(IVirtualFile file, Class<D> clazz) {
        return read(file, ClassCache.getFor(clazz).newInstance());
    }

    /**
	 * This will create the input stream using the file and generate
	 * and {@link XMLDocument} instance wrapper for the instance passed.
	 * 
	 * @return {@link #read(InputStream, XMLDocument)}
	 */
    public static <D> D read(IVirtualFile file, D instance) {
        try {
            return (D) read(file.getInputStream(), XMLDocument.getInstance(instance));
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * This will get the input stream from the URL.
	 * 
	 * @see #read(InputStream, Class)
	 */
    public static <D> D read(URL url, Class<D> descriptor) {
        try {
            return read(url.openStream(), descriptor);
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * Using the descriptor this will get an appropriate instance of {@link XMLDocument}
	 * and use the descriptor to also initialize a new instance of the corresponding class.
	 * 
	 * @param descriptor The descriptor of the class
	 * 
	 * @see #read(InputStream, XMLDocument)
	 */
    public static <D> D read(InputStream in, Class<D> descriptor) {
        try {
            return (D) read(in, getInstance(descriptor, descriptor.newInstance()));
        } catch (InstantiationException e) {
            throw ThrowableManagerRegistry.caught(e);
        } catch (IllegalAccessException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * @param <D> The type of class/descriptor
	 * @param in The input stream to read XML from
	 * @param document A previously created XML document that will used to fill in the class
	 * @return The same instance that was used by the document
	 */
    public static Object read(InputStream in, XMLDocument document) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(in, new XMLDocumentParser(document));
            return document.instance.getInstance();
        } catch (ParserConfigurationException e) {
            throw ThrowableManagerRegistry.caught(new XMLDocumentException(document, e));
        } catch (SAXException e) {
            throw ThrowableManagerRegistry.caught(new XMLDocumentException(document, e));
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(new XMLDocumentException(document, e));
        }
    }

    public static <T> XMLDocument getInstance(T instance) {
        return getInstance((Class<T>) instance.getClass(), instance);
    }

    /**
	 * @param <T> The type of document
	 * @param descriptor The class descriptor
	 * @return An XML document wrapper for parsing/serialization
	 */
    public static <T> XMLDocument getInstance(Class<T> descriptor, T instance) {
        return new XMLDocument(descriptor, instance);
    }

    /**
	 * This assumes no parent.
	 * 
	 * @see XMLElementChild#XMLElementChild(String, XMLElementChild, ClassDescriptor, Object)
	 */
    public XMLDocument(Class descriptor, Object instance) {
        super(null, new XMLDocumentElementDefinition(descriptor), instance);
    }
}
