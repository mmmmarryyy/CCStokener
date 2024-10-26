package org.translationcomponent.api.impl.test.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import org.apache.commons.io.IOUtils;

public class ResourceHelper {

    public static String convertCRLFtoLF(String s) throws IOException {
        return s.replace("\r\n", "\n");
    }

    public static String getContent(String name) throws IOException {
        final URL url = getURL(name);
        Reader reader = new InputStreamReader(url.openStream());
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(reader, writer);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
        return writer.toString();
    }

    public static File getFile(String name) throws IOException {
        final URL url = getURL(name);
        String p = url.getPath();
        return new File(p);
    }

    public static URL getURL(String name) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IOException(name + " not found. URL returned is null");
        }
        return url;
    }
}
