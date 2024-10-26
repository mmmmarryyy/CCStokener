package de.psisystems.dmachinery.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import de.psisystems.dmachinery.core.exeptions.PrintException;
import de.psisystems.dmachinery.io.IOUtil;

public class MarshallUtil {

    private MarshallUtil() {
    }

    public static void toXML(Writer writer, Marshallable vo) throws PrintException {
        XMLStreamWriter xmlStreamWriter = null;
        try {
            xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
            vo.toXML(xmlStreamWriter, true);
        } catch (XMLStreamException e) {
            throw new PrintException(e.getMessage(), e);
        } finally {
            IOUtil.flushAndClose(xmlStreamWriter);
        }
    }

    public static void toXML(OutputStream stream, Marshallable vo) throws PrintException {
        XMLStreamWriter xmlStreamWriter = null;
        try {
            xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stream, "UTF-8");
            vo.toXML(xmlStreamWriter, true);
        } catch (XMLStreamException e) {
            throw new PrintException(e.getMessage(), e);
        } finally {
            IOUtil.flushAndClose(xmlStreamWriter);
        }
    }

    public static void toXML(URL dest, Marshallable vo) throws PrintException {
        XMLStreamWriter xmlStreamWriter = null;
        OutputStream os = null;
        try {
            URLConnection urlConnection = dest.openConnection();
            urlConnection.setDoOutput(true);
            os = urlConnection.getOutputStream();
            xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8");
            vo.toXML(xmlStreamWriter, true);
        } catch (XMLStreamException e) {
            throw new PrintException(e.getMessage(), e);
        } catch (IOException e) {
            throw new PrintException(e.getMessage(), e);
        } finally {
            IOUtil.flushAndClose(xmlStreamWriter);
            IOUtil.close(os);
        }
    }
}
