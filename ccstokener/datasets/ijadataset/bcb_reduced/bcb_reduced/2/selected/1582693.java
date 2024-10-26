package net.sourceforge.javautil.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Common routines for loading/saving settings.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: SettingsUtil.java 1536 2009-12-03 22:51:08Z ponderator $
 */
public class SettingsUtil {

    /**
	 * @param url The URL from which to read the properties
	 * @return The properties that were read
	 * @throws IOException
	 */
    public static Properties loadProperties(URL url) throws IOException {
        return loadProperties(url.openStream());
    }

    /**
	 * Assumes non-xml.
	 * 
	 * @see #load(InputStream, boolean)
	 */
    public static Properties loadProperties(InputStream input) throws IOException {
        return loadProperties(input, false);
    }

    /**
	 * @param input The input stream
	 * @param xml True if the input stream provides XML based properties
	 * @return The properties loaded from the input stream
	 * @throws IOException
	 */
    public static Properties loadProperties(InputStream input, boolean xml) throws IOException {
        try {
            Properties properties = new Properties();
            if (xml) properties.loadFromXML(input); else properties.load(input);
            return properties;
        } finally {
            input.close();
        }
    }
}
