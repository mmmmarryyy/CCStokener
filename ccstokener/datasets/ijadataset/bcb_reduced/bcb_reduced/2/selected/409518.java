package com.google.code.ebmlviewer.viewer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/** The resource bundle control for plain text files. */
public class PlainTextResourceBundleControl extends ResourceBundle.Control {

    /** The extension of the resource bundle files. */
    private final String extension;

    /** The encoding of the resource bundle files. */
    private final String encoding;

    /** Creates a new resource bundle control for text files with the "txt" extension and UTF-8 encoding. */
    public PlainTextResourceBundleControl() {
        this("txt", "UTF-8");
    }

    /**
     * Creates a new resource bundle control for text files with the specified extension and encoding.
     *
     * @param extension the extension of the resource bundle files
     * @param encoding the encoding of the resource bundle files
     *
     * @throws IllegalArgumentException if {@code extension} or {@code encoding} is {@code null}
     */
    public PlainTextResourceBundleControl(String extension, String encoding) {
        if (extension == null) {
            throw new IllegalArgumentException("extension is null");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("encoding is null");
        }
        this.extension = extension;
        this.encoding = encoding;
    }

    @Override
    public List<String> getFormats(String baseName) {
        if (baseName == null) {
            throw new NullPointerException("baseName is null");
        }
        return Arrays.asList(extension);
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IOException {
        if (baseName == null) {
            throw new NullPointerException("baseName is null");
        }
        if (locale == null) {
            throw new NullPointerException("locale is null");
        }
        if (format == null) {
            throw new NullPointerException("format is null");
        }
        if (!extension.equals(format)) {
            throw new IllegalArgumentException("unsupported format: " + format);
        }
        if (loader == null) {
            throw new NullPointerException("loader is null");
        }
        String resourceName = toResourceName(toBundleName(baseName, locale), format);
        InputStream stream = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }
        if (stream == null) {
            return null;
        }
        try {
            return readBundle(stream);
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private ResourceBundle readBundle(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(stream, encoding);
        try {
            return new PropertyResourceBundle(reader);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
        return false;
    }
}
