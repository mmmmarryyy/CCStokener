package com.nepxion.util.io;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;

public class JarUtil {

    /**
	 * Gets the jarURLConnection by an url.
	 * @param url the instance of URL
	 * @return the instance of JarURLConnection
	 * @throws IOException
	 */
    public static JarURLConnection getJarURLConnection(URL url) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        return connection;
    }
}
