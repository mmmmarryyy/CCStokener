package com.abiquo.twitxr.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility network methods
 *  
 * @author slizardo
 *
 */
public class NetUtils {

    public static void saveURL(String address, File outputFile) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(false);
        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(outputFile);
        byte[] chunk = new byte[128];
        int read = is.read(chunk);
        while (read != -1) {
            fos.write(chunk, 0, read);
            read = is.read(chunk);
        }
        is.close();
        fos.close();
    }
}
