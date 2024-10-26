package com.foobnix.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageUtil {

    public static Bitmap fetchImage(String urlstr) {
        try {
            URL url = new URL(urlstr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setDoInput(true);
            c.connect();
            InputStream is = c.getInputStream();
            Bitmap img;
            img = BitmapFactory.decodeStream(is);
            return img;
        } catch (MalformedURLException e) {
            LOG.e(e);
        } catch (IOException e) {
            LOG.e(e);
        }
        return null;
    }
}
