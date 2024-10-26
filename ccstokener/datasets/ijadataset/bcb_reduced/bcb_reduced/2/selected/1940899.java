package com.android.zweibo.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.graphics.drawable.Drawable;

public class NetworkUtil {

    public static Drawable getUserIcon(URL url) {
        if (null != url) {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                return Drawable.createFromStream(conn.getInputStream(), "image");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
