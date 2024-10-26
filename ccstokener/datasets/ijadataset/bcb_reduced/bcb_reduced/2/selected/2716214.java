package org.liris.schemerger.core.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.liris.schemerger.core.event.IEvent;
import org.liris.schemerger.core.pattern.IDec;
import org.liris.schemerger.utils.Factory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 
 * @author Damien Cram
 * 
 * @param <T>
 */
public abstract class AbstractReader<E extends IEvent, T extends IDec, U> implements IReader<E, T, U> {

    protected Class<E> eventClass;

    protected Class<T> decClass;

    protected Factory factory = Factory.getInstance();

    public AbstractReader(Class<E> eventClass, Class<T> decClass) {
        super();
        this.eventClass = eventClass;
        this.decClass = decClass;
    }

    public U readFromString(String stringRepresentation, TypeAdapter adapter) {
        try {
            return readFromStream(adapter, new ByteArrayInputStream(stringRepresentation.getBytes()));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public U read(String urlToFile, TypeAdapter adapter) {
        try {
            URL url = new URL(urlToFile);
            InputStream stream = url.openStream();
            return readFromStream(adapter, stream);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private U readFromStream(TypeAdapter adapter, InputStream stream) throws SAXException, IOException, Exception {
        Document document;
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = builder.parse(stream);
        U t = extractObject(document, adapter);
        stream.close();
        return t;
    }

    protected abstract U extractObject(Document document, TypeAdapter adapter) throws Exception;
}
