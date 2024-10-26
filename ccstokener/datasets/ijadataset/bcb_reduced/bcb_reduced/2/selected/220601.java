package br.com.cinepointer.database;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.util.ByteArrayBuffer;
import android.util.Log;

public class ImageDownloader {

    private static final String PATH = "/data/data/br.com.cinepointer/";

    public static boolean DownloadFromUrl(String imageURL, String fileName) {
        try {
            URL url = new URL(imageURL);
            File file = new File(fileName);
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "comecando download");
            Log.d("ImageManager", "url:" + url);
            Log.d("ImageManager", "nome do arquivo:" + fileName);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("ImageManager", "download terminou em" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return true;
        } catch (IOException e) {
            Log.d("ImageManager", "Error:" + e);
            return false;
        }
    }
}
