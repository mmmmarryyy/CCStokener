package eu.pisolutions.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import eu.pisolutions.io.Closeables;
import eu.pisolutions.lang.Validations;

/**
 * {@link java.util.Properties} reader.
 *
 * @author Laurent Pireyn
 */
public final class PropertiesReader extends Object {

    public static Properties readProperties(InputStream in) throws IOException {
        Validations.notNull(in, "input stream");
        final Properties properties = new Properties();
        properties.load(in);
        return properties;
    }

    public static Properties readProperties(File file) throws IOException {
        Validations.notNull(file, "file");
        return PropertiesReader.readPropertiesAndClose(new BufferedInputStream(new FileInputStream(file)));
    }

    public static Properties readProperties(URL url) throws IOException {
        Validations.notNull(url, "URL");
        return PropertiesReader.readPropertiesAndClose(url.openStream());
    }

    private static Properties readPropertiesAndClose(InputStream in) throws IOException {
        try {
            return PropertiesReader.readProperties(in);
        } finally {
            Closeables.closeQuietly(in);
        }
    }

    private PropertiesReader() {
        super();
    }
}
