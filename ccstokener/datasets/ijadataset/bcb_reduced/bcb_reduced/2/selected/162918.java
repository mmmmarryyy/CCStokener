package nu.staldal.lagoon.producer;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import org.xml.sax.*;
import nu.staldal.lagoon.core.*;

public class URLSource extends Source {

    private URL url;

    public void init() throws LagoonException {
        String u = getParam("name");
        if (u == null) {
            throw new LagoonException("name parameter not specified");
        }
        try {
            url = new URL(u);
        } catch (MalformedURLException e) {
            throw new LagoonException("Malformed URL: " + e.getMessage());
        }
    }

    public void start(ContentHandler sax, Target target) throws IOException, SAXException {
        URLConnection conn = url.openConnection();
        InputStream fis = conn.getInputStream();
        XMLReader parser = new org.apache.xerces.parsers.SAXParser();
        parser.setFeature("http://xml.org/sax/features/validation", false);
        parser.setFeature("http://xml.org/sax/features/external-general-entities", true);
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        parser.setContentHandler(sax);
        parser.parse(new InputSource(fis));
        fis.close();
    }

    public boolean hasBeenUpdated(long when) {
        return true;
    }
}
