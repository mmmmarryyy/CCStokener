package org.mcisb.jws.util;

import java.io.*;
import java.net.*;
import java.util.*;
import org.mcisb.util.io.*;
import org.mcisb.util.net.*;

/**
 * Provides programatic upload / download to JWS Online.
 * 
 * NOT USED IN THE APPLET...!
 * 
 * @author Neil Swainston
 */
public class JwsInterface {

    /**
	 * 
	 */
    private static final String URL_SEPARATOR = "/";

    /**
	 * 
	 */
    private static final String SERVER = "http://jjj.biochem.sun.ac.za";

    /**
	 * 
	 */
    private static final String DOWNLOAD_BASE_URL = SERVER + URL_SEPARATOR + "database/";

    /**
	 * 
	 */
    private static final String UPLOAD_URL = SERVER + URL_SEPARATOR + "webMathematica/upload/upload.jsp";

    /**
	 * 
	 */
    private static final String EXTENSION = ".xml";

    /**
	 * 
	 * @param id
	 * @return InputStream
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
    public InputStream get(final String id) throws MalformedURLException, IOException {
        final String url = DOWNLOAD_BASE_URL + id + URL_SEPARATOR + id + EXTENSION;
        return new URL(url).openStream();
    }

    /**
	 * 
	 * @param file
	 * @return String
	 * @throws IOException 
	 */
    public String put(final File file) throws IOException {
        HttpURLConnection http = null;
        try {
            final URL url = new URL(UPLOAD_URL);
            final Map<String, Object> nameValuePairs = new HashMap<String, Object>();
            nameValuePairs.put("upfile", file);
            http = NetUtils.doPostMultipart(url, nameValuePairs);
            return new String(StreamReader.read(http.getInputStream()));
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /**
	 * 
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        System.out.println(new JwsInterface().put(new File(args[0])));
    }
}
