package org.xfc.help;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 * 
 * @author Devon Carew
 */
abstract class XHelpLibrary {

    private static Log log = LogFactory.getLog(XHelpLibrary.class);

    private boolean open;

    private boolean errored;

    private String title;

    private XTableOfContents tableOfContents;

    private File jarFileReference;

    private ZipFile zipFile;

    protected XHelpLibrary(File jarFile) throws IOException {
        if (jarFile == null) throw new IllegalArgumentException();
        jarFileReference = jarFile;
        if (!jarFileReference.exists() || !jarFileReference.canRead()) throw new IOException("unable to read " + jarFileReference.getName());
    }

    public abstract String parseTitle() throws IOException;

    protected abstract XTableOfContents parseTableOfContents() throws IOException;

    public String getTitle() {
        if (isErrored()) return title;
        if (title == null) {
            try {
                title = parseTitle();
            } catch (IOException e) {
                log.error("Unable to parse " + jarFileReference, e);
                setErrored(true);
            }
        }
        return title;
    }

    public XTableOfContents getTableOfContents() {
        if (isErrored()) return tableOfContents;
        if (tableOfContents == null) {
            try {
                tableOfContents = parseTableOfContents();
            } catch (IOException e) {
                log.error("Unable to parse " + jarFileReference, e);
                tableOfContents = new XTableOfContents(title != null ? title : jarFileReference.getName());
                tableOfContents.setError(true);
                setErrored(true);
            }
        }
        return tableOfContents;
    }

    public boolean isOpen() {
        return open;
    }

    public void open() throws IOException {
        if (!isOpen()) zipFile = new ZipFile(jarFileReference);
    }

    public void close() {
        if (!isOpen()) {
            try {
                zipFile.close();
            } catch (IOException ioe) {
            }
            zipFile = null;
        }
    }

    protected ZipFile getZipFile() throws IOException {
        if (!isOpen()) open();
        return zipFile;
    }

    public byte[] getContent(String entryName) throws IOException {
        ZipEntry zipEntry = getZipFile().getEntry(entryName);
        if (zipEntry == null) throw new FileNotFoundException(entryName);
        InputStream in = getZipFile().getInputStream(zipEntry);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int count = in.read(temp);
        while (count != -1) {
            out.write(temp, 0, count);
            count = in.read(temp);
        }
        return out.toByteArray();
    }

    public URL getContentAsURL(String entryName) throws IOException {
        URL url = new URL("help", "localhost", -1, entryName, new HelpStreamHandler());
        return url;
    }

    public boolean isErrored() {
        return errored;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    protected EntityResolver getEntityResolver() {
        return null;
    }

    protected String getXMLSystemID() {
        return null;
    }

    protected Element parseXMLEntry(String entryName) throws IOException {
        return parseXMLEntry(entryName, false);
    }

    protected String makeAbsoluteEntry(String entryName, String relativeTo) {
        if (relativeTo != null && !entryName.startsWith("/")) {
            int index = relativeTo.lastIndexOf('/');
            if (index != -1) return relativeTo.substring(0, index + 1) + entryName;
            index = relativeTo.lastIndexOf('\\');
            if (index != -1) return relativeTo.substring(0, index + 1) + entryName;
        }
        return entryName;
    }

    protected Element parseXMLEntry(String entryName, boolean cleanXML) throws IOException {
        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.apache.crimson.parser.XMLReaderImpl");
            EntityResolver resolver = getEntityResolver();
            if (resolver != null) xmlReader.setEntityResolver(resolver);
            Builder xomBuilder = new Builder(xmlReader, false);
            byte[] data = getContent(entryName);
            if (cleanXML) cleanXMLStream(data);
            Document xomDocument = xomBuilder.build(new ByteArrayInputStream(data), getXMLSystemID());
            return xomDocument.getRootElement();
        } catch (ValidityException ve) {
            IOException ioe = new IOException("Error parsing " + entryName);
            ioe.initCause(ve);
            throw ioe;
        } catch (SAXException saxE) {
            if (!cleanXML) {
                return parseXMLEntry(entryName, true);
            } else {
                IOException ioe = new IOException("Error parsing " + entryName);
                ioe.initCause(saxE);
                throw ioe;
            }
        } catch (ParsingException pe) {
            if (!cleanXML) {
                return parseXMLEntry(entryName, true);
            } else {
                IOException ioe = new IOException("Error parsing " + entryName);
                ioe.initCause(pe);
                throw ioe;
            }
        }
    }

    /**
	 * A method to strip out all processing instructions from an XML file. This is a last-ditch effort
	 * to clean up the XML.
	 * 
	 * @param data
	 */
    protected void cleanXMLStream(byte[] data) {
        strip(data, 0, data.length);
    }

    class HelpStreamHandler extends URLStreamHandler {

        protected URLConnection openConnection(URL url) throws IOException {
            return new HelpURLConnection(url);
        }
    }

    class HelpURLConnection extends URLConnection {

        String entryName;

        HelpURLConnection(URL url) {
            super(url);
        }

        public void connect() throws IOException {
        }

        public String getContentType() {
            String fileName = getURL().getFile();
            return getFileNameMap().getContentTypeFor(fileName);
        }

        public InputStream getInputStream() throws IOException {
            String entryName = getURL().getFile();
            ZipEntry zipEntry = getZipFile().getEntry(entryName);
            if (zipEntry == null) throw new FileNotFoundException(entryName);
            return getZipFile().getInputStream(zipEntry);
        }
    }

    private static void strip(byte[] data, int offset, int length) {
        boolean isStripping = false;
        char lastChar = ' ';
        int stopPos = offset + length;
        for (int i = offset; i < stopPos; i++) {
            char current = (char) data[i];
            if (!isStripping) {
                if (lastChar == '<' && current == '!') {
                    isStripping = true;
                    if (i > offset) data[i - 1] = ' ';
                }
            }
            if (isStripping) data[i] = ' ';
            if (isStripping && current == '>') isStripping = false;
            lastChar = current;
        }
    }
}
