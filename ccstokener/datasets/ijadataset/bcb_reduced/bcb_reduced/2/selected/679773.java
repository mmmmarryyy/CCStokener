package org.mozilla.intl.chardet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 
 * @author liqiang 2009-01-15
 * judge charset from InputStream , File  and URL
 *
 */
public class Charset {

    private static String encoding = null;

    private static boolean found = false;

    /**
	 * judge charset from byte stream source 
	 * @param in  byte_stream
	 * @return    encoding 
	 * @throws IOException
	 */
    public static String guess(InputStream in) throws IOException {
        nsDetector det = new nsDetector();
        det.Init(new nsICharsetDetectionObserver() {

            public void Notify(String charset) {
                encoding = charset;
            }
        });
        byte[] buf = new byte[1024];
        int len;
        boolean done = false;
        boolean isAscii = true;
        while ((len = in.read(buf, 0, buf.length)) != -1) {
            if (isAscii) isAscii = det.isAscii(buf, len);
            if (!isAscii && !done) done = det.DoIt(buf, len, false);
        }
        det.DataEnd();
        if (isAscii) {
            found = true;
        }
        if (!found) {
            String prob[] = det.getProbableCharsets();
            for (int i = 0; i < prob.length; i++) {
            }
        }
        return encoding;
    }

    public static String guess(URL url) throws IOException {
        InputStream in = url.openStream();
        return guess(in);
    }

    public static String guess(String path) throws IOException {
        InputStream in = new FileInputStream(path);
        return guess(in);
    }

    /**
	 * @param args
	 * @throws IOException 
	 */
    public static void main(String[] args) throws IOException {
        System.out.println(Charset.guess(new URL("http://www.google.com/ig/api?hl=zh_cn&weather=beijing")));
        System.out.println(Charset.guess("d:/solidot.xml"));
    }
}
