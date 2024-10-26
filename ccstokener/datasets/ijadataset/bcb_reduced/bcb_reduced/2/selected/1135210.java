package com.objectwave.customClassLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author  dhoag
 * @version  $Id: URLClassLoader.java,v 2.0 2001/06/11 15:54:25 dave_hoag Exp $
 */
public class URLClassLoader extends com.objectwave.customClassLoader.MultiClassLoader {

    private String urlString;

    /**
	 * @param  urlString
	 */
    public URLClassLoader(String urlString) {
        this.urlString = urlString;
    }

    /**
	 *  Every custom class loader will need to be able to get the content from a
	 *  modified URL. This URL will have the protocol of CCLP
	 *
	 * @param  url
	 * @return  The Content value
	 * @exception  java.io.IOException
	 * @fixme  - DAH - Not yet implemented
	 */
    public Object getContent(java.net.URL url) throws java.io.IOException {
        return null;
    }

    /**
	 *  Every custom class loader will need to be able to get the input stream from
	 *  a modified URL. This URL will have the protocol of CCLP
	 *
	 * @param  url
	 * @return  The InputStream value
	 * @exception  java.io.IOException
	 * @fixme  - DAH - Not yet implemented
	 */
    public java.io.InputStream getInputStream(java.net.URL url) throws java.io.IOException {
        return null;
    }

    /**
	 *  ---------- Abstract Implementation ---------------------
	 *
	 * @param  className
	 * @return
	 */
    protected byte[] loadClassBytes(String className) {
        className = formatClassName(className);
        try {
            URL url = new URL(urlString + className);
            URLConnection connection = url.openConnection();
            if (sourceMonitorOn) {
                print("Loading from URL: " + connection.getURL());
            }
            monitor("Content type is: " + connection.getContentType());
            InputStream inputStream = connection.getInputStream();
            int length = connection.getContentLength();
            monitor("InputStream length = " + length);
            byte[] data = new byte[length];
            inputStream.read(data);
            inputStream.close();
            return data;
        } catch (Exception ex) {
            print("### URLClassLoader.loadClassBytes() - Exception:");
            ex.printStackTrace();
            return null;
        }
    }
}
