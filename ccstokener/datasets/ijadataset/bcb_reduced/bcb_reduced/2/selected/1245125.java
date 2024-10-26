package org.synthful.net;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.InputStream;

/**
 * 
 * 
 * @author Blessed Geek
 */
public class ReadURL {

    /**
	 * Read url.
	 * 
	 * @param urlstr
	 * 
	 * @return Read url as String
	 */
    public static String ReadURL(String urlstr) {
        try {
            URL url = new URL(urlstr);
            return ReadURL(url, true);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Read url.
	 * 
	 * @param urlstr
	 * @param textonly
	 * 
	 * @return Read url as String
	 */
    public static String ReadURL(String urlstr, boolean textonly) {
        try {
            URL url = new URL(urlstr);
            return ReadURL(url, textonly);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Read url.
	 * 
	 * @param url
	 * @param textonly
	 * 
	 * @return Read url as String
	 */
    public static String ReadURL(URL url, boolean textonly) {
        try {
            URLConnection uconn = url.openConnection();
            Object ucont = uconn.getContent();
            if (ucont instanceof InputStream) return ReadInputStream((java.io.InputStream) ucont, textonly); else return "" + ucont;
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Read input stream.
	 * 
	 * @param instr
	 * @param textonly
	 * 
	 * @return Read input stream as String
	 * 
	 * @throws IOException
	 */
    public static String ReadInputStream(InputStream instr, boolean textonly) throws java.io.IOException {
        StringBuffer contbuf = new StringBuffer();
        int in = 0;
        int minchar = textonly ? 32 : 0;
        while (in >= 0) {
            in = instr.read();
            if (in >= minchar) contbuf.append((char) in);
        }
        return "" + contbuf;
    }
}
