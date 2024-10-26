package gnu.infoset;

import gnu.protocol.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;
import java.net.*;

public class Document extends ElementContainer {

    public static boolean isCompatibleSAX() {
        try {
            Class.forName("org.xml.sax.XMLReader");
            return true;
        } catch (ClassNotFoundException x) {
            return false;
        }
    }

    public Document() {
    }

    public Document(URL url) throws InfosetException, IOException {
        read(url);
    }

    public Document(String url) throws InfosetException, IOException {
        read(url);
    }

    public void read(URL url) throws InfosetException, IOException {
        if (url == null) throw new IOException("no URL");
        this.url = url;
        read(new InputStreamReader(url.openStream()));
    }

    public void read(String url) throws InfosetException, IOException {
        read(new URL(url));
    }

    public void read() throws InfosetException, IOException {
        read(url);
    }

    public void write() throws IOException {
        OutputStream stream;
        if (url.getProtocol().equals("file")) {
            stream = new FileOutputStream(url.getFile());
        } else {
            stream = url.openConnection().getOutputStream();
        }
        Writer writer = new OutputStreamWriter(stream);
        writeXML(writer);
        writer.close();
    }

    public void readSystemResource(String resource) throws InfosetException, IOException {
        try {
            read(ClassLoader.getSystemResource(resource));
        } catch (IOException x) {
            File file = FileUtil.findInPaths(resource, System.getProperty("java.library.path"));
            if (file == null) throw new IOException("no URL");
            read(FileUtil.url(file));
        }
    }

    public void read(Reader reader) throws InfosetException, IOException {
        reset();
        XMLReader xmlReader;
        try {
            xmlReader = XMLReaderFactory.createXMLReader();
        } catch (SAXException x) {
            throw new InfosetException(x.toString());
        }
        xmlReader.setContentHandler(new InfosetContentHandler(this));
        try {
            xmlReader.parse(new InputSource(reader));
        } catch (SAXException x) {
            System.err.println("Error " + x.getMessage());
            x.printStackTrace(System.err);
            throw new InfosetException(x.toString());
        }
    }

    public void writeXML(Writer writer, int depth) throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        super.writeXML(writer, depth);
    }

    public void writeXML(Writer writer) throws IOException {
        writeXML(writer, -1);
    }

    protected URL url = null;
}
