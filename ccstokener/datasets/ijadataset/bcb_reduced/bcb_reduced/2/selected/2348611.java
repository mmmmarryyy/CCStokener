package com.baculsoft.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 
 * @author Natalino Nugeraha
 */
public final class IOUtil {

    private IOUtil() {
    }

    /**
	 * 
	 * @param contextURL
	 * @param spec
	 * @return
	 * @throws MalformedURLException
	 */
    public static final URL getURL(URL contextURL, String spec) throws MalformedURLException {
        try {
            return new URL(contextURL, spec);
        } catch (MalformedURLException e) {
            File tempFile = new File(spec);
            if (contextURL == null || (tempFile.isAbsolute())) {
                return tempFile.toURL();
            }
            throw e;
        }
    }

    /**
	 * 
	 * @param url
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
    public static final InputStream getContentAsInputStream(URL url) throws SecurityException, IllegalArgumentException, IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null.");
        }
        try {
            InputStream content = url.openStream();
            if (content == null) {
                throw new IllegalArgumentException("No content.");
            }
            return content;
        } catch (SecurityException e) {
            throw new SecurityException("Your JVM's SecurityManager has " + "disallowed this.");
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("This file was not found: " + url);
        }
    }
}
