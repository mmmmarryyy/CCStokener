package net.sf.mzmine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Internet related utilities
 */
public class InetUtils {

    /**
	 * Opens a connection to the given URL (typically HTTP) and retrieves the
	 * data from server. Data is assumed to be in UTF-8 encoding.
	 */
    public static String retrieveData(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-agent", "MZmine 2");
        InputStream is = connection.getInputStream();
        if (is == null) {
            throw new IOException("Could not establish a connection to " + url);
        }
        StringBuffer buffer = new StringBuffer();
        try {
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            char[] cb = new char[1024];
            int amtRead = reader.read(cb);
            while (amtRead > 0) {
                buffer.append(cb, 0, amtRead);
                amtRead = reader.read(cb);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        is.close();
        return buffer.toString();
    }
}
