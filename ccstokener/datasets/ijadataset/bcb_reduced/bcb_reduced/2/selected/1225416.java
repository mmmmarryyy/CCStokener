package net.sourceforge.bricksviewer.xml;

import java.io.*;
import java.net.URL;
import java.util.zip.*;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ZipXMLDOMLoader extends XMLDOMLoader {

    public Document loadDocumentFromZipEntry(File file, String entryName) throws LocalFileXMLLoadingException {
        Document doc = null;
        InputStream inStream = null;
        ZipEntry xmlEntry = null;
        ZipFile zipFile = null;
        if (file.exists()) {
            try {
                zipFile = new ZipFile(file);
            } catch (IOException e) {
                throw new LocalFileXMLLoadingException("Problem opening file '" + file.getPath() + "' as zip file", e);
            }
            xmlEntry = zipFile.getEntry(entryName);
            try {
                inStream = zipFile.getInputStream(xmlEntry);
            } catch (IOException e) {
                throw new LocalFileXMLLoadingException("Problem getting stream for entry '" + entryName + "' in zip file '" + file.getPath() + "'", e);
            }
            try {
                doc = loadDocumentFromStream(inStream);
            } catch (XMLLoadingException e) {
                throw new LocalFileXMLLoadingException("Problem occured loading document from zip file '" + file.getPath() + "'", e);
            }
        }
        return doc;
    }

    public Document loadDocumentFromZipEntry(URL url, String entryName) throws XMLLoadingException {
        Document doc = null;
        InputStream inStream;
        ZipEntry entry = null, xmlEntry = null;
        ZipInputStream zipInStream;
        try {
            inStream = url.openStream();
        } catch (IOException e) {
            throw new XMLLoadingException("Problem opening stream from url '" + url + "'", e);
        }
        try {
            zipInStream = new ZipInputStream(inStream);
            do {
                entry = zipInStream.getNextEntry();
                if (entryName.equals(entry.getName())) {
                    xmlEntry = entry;
                }
            } while ((entry != null) && (xmlEntry == null));
        } catch (IOException e) {
            throw new XMLLoadingException("Problem fetching next zip entry in stream from url '" + url + "'", e);
        }
        if (xmlEntry == null) {
            throw new XMLLoadingException("Couldn't find entry '" + entryName + "' in file at url '" + url + "'");
        }
        try {
            doc = loadDocumentFromStream(zipInStream);
        } catch (XMLLoadingException e) {
            throw new LocalFileXMLLoadingException("Problem occured loading document from zip url '" + url + "'", e);
        }
        return doc;
    }
}
