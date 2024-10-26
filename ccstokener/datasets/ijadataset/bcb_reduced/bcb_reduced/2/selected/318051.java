package it.unicaradio.android.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.util.ByteArrayBuffer;

/**
 * @author paolo.cortis
 */
public class Utils {

    private static String LOG = Utils.class.getName();

    public static byte[] downloadFromUrl(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection ucon = url.openConnection();
        InputStream is = ucon.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayBuffer baf = new ByteArrayBuffer(50);
        int current = 0;
        while ((current = bis.read()) != -1) {
            baf.append((byte) current);
        }
        return baf.toByteArray();
    }
}
