package com.sugree.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.text.Html;
import android.widget.ImageView;
import android.util.Log;

public class DownloadTasks {

    protected static final String TAG = "DownloadTasks";

    public static final int HTTP_STATUS_OK = 200;

    public static Bitmap getBitmap(String source) {
        try {
            final InputStream is = getUrlStream(source);
            final Bitmap bm = BitmapFactory.decodeStream(is);
            is.close();
            return bm;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return null;
        }
    }

    public static Drawable getDrawable(Resources resources, Bitmap bitmap) {
        final Drawable d = new BitmapDrawable(resources, bitmap);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        return d;
    }

    public static Drawable getDrawable(Resources resources, String source) {
        try {
            final InputStream is = getUrlStream(source);
            final Bitmap bm = BitmapFactory.decodeStream(is);
            final Drawable d = new BitmapDrawable(resources, bm);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            is.close();
            return d;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return null;
        }
    }

    public static String getUrlContent(String url) throws IOException {
        InputStream is = getUrlStream(url);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int bytesRead;
        byte[] buffer = new byte[2048];
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
        os.close();
        is.close();
        return new String(os.toByteArray());
    }

    public static InputStream getUrlStream(String url) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HTTP_STATUS_OK) {
                throw new IOException(status.toString());
            }
            HttpEntity entity = response.getEntity();
            return new BufferedHttpEntity(entity).getContent();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
    }
}
