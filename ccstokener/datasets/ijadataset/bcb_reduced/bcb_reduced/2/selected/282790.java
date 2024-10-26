package gnu.xml.libxmlj.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import gnu.xml.libxmlj.transform.GnomeTransformerFactory;
import gnu.xml.dom.ls.ReaderInputStream;
import gnu.xml.dom.ls.WriterOutputStream;

/**
 * Utility functions for libxmlj.
 */
public final class XMLJ {

    static class XMLJShutdownHook implements Runnable {

        public void run() {
            System.gc();
            Runtime.getRuntime().runFinalization();
            GnomeTransformerFactory.freeLibxsltGlobal();
        }
    }

    private static boolean initialised = false;

    public static void init() {
        if (!initialised) {
            System.loadLibrary("xmlj");
            XMLJShutdownHook hook = new XMLJShutdownHook();
            Runtime.getRuntime().addShutdownHook(new Thread(hook));
        }
        initialised = true;
    }

    private static final int LOOKAHEAD = 50;

    /**
   * Returns an input stream for the specified input source.
   * This returns a pushback stream that libxmlj can use to detect the
   * character encoding of the stream.
   */
    public static NamedInputStream getInputStream(InputSource input) throws IOException {
        InputStream in = input.getByteStream();
        String systemId = input.getSystemId();
        if (in == null) {
            Reader r = input.getCharacterStream();
            if (r != null) in = new ReaderInputStream(r);
        }
        if (in == null) {
            in = getInputStream(systemId);
        }
        return new NamedInputStream(systemId, in, LOOKAHEAD);
    }

    /**
   * Returns an input stream for the specified transformer source.
   * This returns a pushback stream that libxmlj can use to detect the
   * character encoding of the stream.
   */
    public static NamedInputStream getInputStream(Source source) throws IOException {
        if (source instanceof SAXSource) {
            return getInputStream(((SAXSource) source).getInputSource());
        }
        InputStream in = null;
        String systemId = source.getSystemId();
        if (source instanceof StreamSource) {
            in = ((StreamSource) source).getInputStream();
        }
        if (in == null) {
            in = getInputStream(systemId);
        }
        return new NamedInputStream(systemId, in, LOOKAHEAD);
    }

    private static InputStream getInputStream(String systemId) throws IOException {
        if (systemId == null) {
            throw new IOException("no system ID");
        }
        try {
            return new URL(systemId).openStream();
        } catch (MalformedURLException e) {
            return new FileInputStream(systemId);
        }
    }

    /**
   * Returns an input stream for the specified URL.
   * This returns a pushback stream that libxmlj can use to detect the
   * character encoding of the stream.
   */
    public static NamedInputStream getInputStream(URL url) throws IOException {
        return new NamedInputStream(url.toString(), url.openStream(), LOOKAHEAD);
    }

    /**
   * Convenience method for xmljDocLoader
   */
    static NamedInputStream xmljGetInputStream(String base, String url) throws IOException {
        try {
            if (base != null) {
                url = new URL(new URL(base), url).toString();
            }
        } catch (MalformedURLException e) {
        }
        InputStream in = getInputStream(url);
        return new NamedInputStream(url, in, LOOKAHEAD);
    }

    /**
   * Returns an output stream for the specified transformer result.
   */
    public static OutputStream getOutputStream(Result result) throws IOException {
        OutputStream out = null;
        if (result instanceof StreamResult) {
            out = ((StreamResult) result).getOutputStream();
        }
        if (out == null) {
            Writer w = ((StreamResult) result).getWriter();
            if (w != null) out = new WriterOutputStream(w);
        }
        if (out == null) {
            String systemId = result.getSystemId();
            if (systemId == null) {
                throw new IOException("no system ID");
            }
            try {
                URL url = new URL(systemId);
                URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                out = connection.getOutputStream();
            } catch (MalformedURLException e) {
                out = new FileOutputStream(systemId);
            }
        }
        return out;
    }

    /**
   * Returns the absolute form of the specified URI.
   * If the URI is already absolute, returns it as-is.
   * Otherwise returns a new URI relative to the given base URI.
   */
    public static String getAbsoluteURI(String base, String uri) {
        if (uri != null && base != null && (uri.length() > 0) && (uri.indexOf(':') == -1) && (uri.charAt(0) != '/')) {
            if (base.charAt(base.length() - 1) != '/') {
                int i = base.lastIndexOf('/');
                base = base.substring(0, i + 1);
            }
            return base + uri;
        } else {
            return uri;
        }
    }

    public static String getBaseURI(String uri) {
        if (uri != null) {
            int si = uri.lastIndexOf('/');
            if (si != -1) {
                uri = uri.substring(0, si + 1);
            }
        }
        return uri;
    }
}
