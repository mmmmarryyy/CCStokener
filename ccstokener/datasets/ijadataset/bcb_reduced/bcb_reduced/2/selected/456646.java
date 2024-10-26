package org.ttt.salt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * This will handle resolution of PUBLIC names and SYSTEM names.
 *
 * @author Lance Finn Helsten
 * @version $Id: TBXResolver.java 105 2009-04-04 17:40:36Z lanhel $
 * @license Licensed under the Apache License, Version 2.0.
 */
public class TBXResolver implements EntityResolver {

    /** SCM information. */
    public static final String RCSID = "$Id: TBXResolver.java 105 2009-04-04 17:40:36Z lanhel $";

    /** Logger for this package. */
    private static final Logger LOGGER = Logger.getLogger("org.ttt.salt");

    /** Localized resource bundle to be used for this object. */
    private final Map<String, String> uri2url = new java.util.HashMap<String, String>();

    /** Fallback search location. */
    private URL fallbackSearchLoc;

    /**
     * @param fallback The final location to look for an entity.
     */
    public TBXResolver(URL fallback) throws IOException {
        String path = fallback.getPath();
        String ppath = path.substring(0, path.lastIndexOf('/') + 1);
        fallbackSearchLoc = new URL(fallback.getProtocol(), fallback.getHost(), fallback.getPort(), ppath);
        addPublicId("ISO 30042:2008A//DTD TBX core//EN", "/xml/TBXcoreStructV02.dtd");
        addPublicId("ISO 30042:2008A//DTD TBX XCS//EN", "/xml/tbxxcsdtd.dtd");
        addPublicId("Demo XCS", "/xml/TBXDCSv05.xml");
    }

    /**
     * Add a PUBLIC name to the resolver built in mapping.
     *
     * @param publicId The PUBLIC ID of the entity.
     * @param systemId The SYSTEM ID of the entity: uri or url.
     */
    public void addPublicId(String publicId, String systemId) {
        uri2url.put(publicId, systemId);
    }

    /** {@inheritDoc} */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        LOGGER.fine(String.format("Resolve entity PublicID=%s SystemId=%s", publicId, systemId));
        InputStreamReader reader = null;
        try {
            InputStream input;
            if (uri2url.containsKey(publicId)) {
                LOGGER.info("Entity is a known publicId: " + publicId);
                systemId = uri2url.get(publicId);
                input = getClass().getResourceAsStream(uri2url.get(publicId));
            } else if (systemId.matches("\\w+:.+")) {
                LOGGER.info("Entity is a URI: " + systemId);
                URI uri = new URI(systemId);
                input = uri.toURL().openStream();
            } else {
                LOGGER.fine("Unknown schema systemId: " + systemId);
                if (systemId.startsWith("/")) {
                    LOGGER.info("Entity is an absolute path: " + systemId);
                    File file = new File(systemId);
                    if (file.exists()) input = new FileInputStream(file); else input = getClass().getResourceAsStream(systemId);
                } else {
                    systemId = fallbackSearchLoc.getPath() + systemId;
                    URL locurl = new URL(fallbackSearchLoc.getProtocol(), fallbackSearchLoc.getHost(), fallbackSearchLoc.getPort(), systemId);
                    LOGGER.info("Entity is a relative path: " + locurl.toString());
                    input = locurl.openStream();
                }
                if (input == null) throw new FileNotFoundException();
            }
            if (!input.markSupported()) input = new java.io.BufferedInputStream(input);
            String enc = TBXResolver.getEncoding(input);
            reader = new InputStreamReader(input, enc);
        } catch (FileNotFoundException err) {
            String msg = String.format("Entity could not be resolved:\n  PUBLIC: '%s'\n  SYSTEM: '%s'\n  BUILT IN: %s", publicId, systemId, uri2url.containsKey(publicId) ? uri2url.get(publicId) : "NONE");
            throw new FileNotFoundException(msg);
        } catch (UnsupportedEncodingException err) {
            throw new UnsupportedEncodingException(String.format("PUBLIC %s SYSTEM %s", publicId, systemId));
        } catch (URISyntaxException err) {
            throw new SAXException("Invalid System ID format", err);
        }
        InputSource ret = new InputSource(reader);
        ret.setPublicId(publicId);
        ret.setSystemId(systemId);
        return ret;
    }

    public static String getEncoding(InputStream input) throws UnsupportedEncodingException, IOException {
        input.mark(16);
        int b0 = input.read();
        int b1 = input.read();
        int b2 = input.read();
        int b3 = input.read();
        int b4 = input.read();
        input.reset();
        int b1utf = b1 & 0xC0;
        int b2utf = b2 & 0xC0;
        int b3utf = b3 & 0xC0;
        String ret = null;
        if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            b0 = input.read();
            b1 = input.read();
            b2 = input.read();
            ret = "UTF-8";
        } else if (((b0 & 0x80) == 0x00) || ((b0 & 0xE0) == 0xC0 && (b1 & 0xC0) == 0x80) || ((b0 & 0xF0) == 0xE0 && (b1 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80) || ((b0 & 0xF8) == 0xF0 && (b1 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80)) {
            ret = "UTF-8";
        } else if (b0 == 0xfe && b1 == 0xff) {
            b0 = input.read();
            b1 = input.read();
            ret = "UTF-16BE";
        } else if (b0 == 0xff && b1 == 0xfe && b2 != 0x00) {
            b0 = input.read();
            b1 = input.read();
            ret = "UTF-16LE";
        } else if (b0 == 0x00 && b1 == 0x00 && b2 == 0xfe && b3 == 0xff) {
            ret = "UTF-32BE";
            if (!java.nio.charset.Charset.isSupported(ret)) throw new UnsupportedEncodingException("UTF-32BE encoding is unsupported.");
        } else if (b0 == 0xff && b2 == 0xfe && b2 == 0x00 && b3 == 0x00) {
            ret = "UTF-32LE";
            if (!java.nio.charset.Charset.isSupported(ret)) throw new UnsupportedEncodingException("UTF-32LE encoding is unsupported.");
        } else {
            throw new UnsupportedEncodingException("Unknown file encoding");
        }
        return ret;
    }
}
