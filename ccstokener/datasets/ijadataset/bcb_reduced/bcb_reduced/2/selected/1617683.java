package com.oscwave.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Some helper methods may useful to work with web content.
 * @author dan
 */
public final class WebUtils {

    private static final SimpleDateFormat DX = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);

    private WebUtils() {
    }

    /**
	 * Converts a rfc 822 string to a date. Supported formats are "Sat, 07 Sep 2002 00:00:01 GMT" and "Sat, 07 Sep 02 00:00:01 GMT"
	 * @param rfc822Date Date string
	 * @return date instance or null if parsing failed
	 */
    public static Date rfc822ToDate(String rfc822Date) {
        try {
            if (rfc822Date != null) return DX.parse(rfc822Date);
        } catch (ParseException e) {
        }
        return null;
    }

    /**
	 * Loads a document from a url and returns the result in a stream.
	 * @param url Url to open
	 * @param proxy Proxy object if proxy configured, else null
	 * @return InputStream to read from
	 * @throws WebException Thrown if something gets out of control...
	 */
    public static InputStream loadDocument(URL url, Proxy proxy) throws WebException {
        byte[] data = new byte[8192];
        URLConnection con = null;
        ByteArrayOutputStream bos = null;
        InputStream is = null;
        ByteArrayInputStream bis = null;
        try {
            try {
                if (proxy == null) con = url.openConnection(); else con = url.openConnection(proxy);
            } catch (IOException e) {
                throw new WebException("Failed to open URL " + url.toString());
            }
            try {
                is = con.getInputStream();
            } catch (IOException e) {
                throw new WebException("Failed to read from URL " + url.toString());
            }
            int i = 0;
            try {
                bos = new ByteArrayOutputStream();
                while ((i = is.read(data)) > 0) {
                    bos.write(data, 0, i);
                }
            } catch (IOException e) {
                throw new WebException("Failed to read data from URL " + url.toString());
            }
            bis = new ByteArrayInputStream(bos.toByteArray());
        } finally {
            try {
                if (bos != null) bos.close();
                if (is != null) is.close();
            } catch (IOException e) {
            }
        }
        return bis;
    }
}
