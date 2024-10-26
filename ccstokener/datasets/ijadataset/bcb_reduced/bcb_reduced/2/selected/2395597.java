package net.jalan.jws.search.hotel;

import org.w3c.dom.Node;
import java.net.URL;
import java.util.ArrayList;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.view.View;
import android.os.Handler;

public class HotelPicture {

    public URL url;

    public String caption;

    private Bitmap cache;

    public HotelPicture(Node xml) {
        try {
            url = new URL(xml.getFirstChild().getNodeValue());
            caption = xml.getNextSibling().getFirstChild().getNodeValue();
        } catch (Exception e) {
        }
    }

    public Bitmap getBitmap() {
        return getBitmap(true);
    }

    public Bitmap getBitmap(Boolean useCache) {
        if (cache != null && useCache) return cache;
        if (url == null) return null;
        Bitmap bm = null;
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
        }
        if (bm != null) cache = bm;
        return bm;
    }

    public void load(final ImageView view) {
        Bitmap bmp = getBitmap();
        if (bmp != null) view.setImageBitmap(bmp); else view.setVisibility(View.GONE);
    }

    public void loadAsync(final ImageView view) {
        final Handler h = new Handler();
        final Thread t = new Thread(new Runnable() {

            public void run() {
                h.post(new Runnable() {

                    public void run() {
                        load(view);
                    }
                });
            }
        });
        t.start();
    }
}
