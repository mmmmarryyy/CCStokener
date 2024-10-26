package com.myspace.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public class MSDrawableManager {

    private final Map<String, Drawable> drawableMap;

    private ExecutorService executorService;

    static MSDrawableManager instance;

    public static MSDrawableManager getInstance() {
        if (instance == null) {
            synchronized (MSDrawableManager.class) {
                if (instance == null) {
                    instance = new MSDrawableManager();
                }
            }
        }
        return instance;
    }

    private MSDrawableManager() {
        drawableMap = new HashMap<String, Drawable>();
        executorService = Executors.newFixedThreadPool(2);
    }

    public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
        if (drawableMap.containsKey(urlString)) {
            imageView.setImageDrawable(drawableMap.get(urlString));
        }
        imageView.setImageDrawable(null);
        final Handler handler = new Handler() {

            @Override
            public void handleMessage(Message message) {
                imageView.setImageDrawable((Drawable) message.obj);
            }
        };
        executorService.execute(new Runnable() {

            public void run() {
                Drawable drawable = fetchDrawable(urlString);
                Message message = handler.obtainMessage(1, drawable);
                handler.sendMessage(message);
            }
        });
    }

    public Drawable fetchDrawable(String urlString) {
        if (drawableMap.containsKey(urlString)) {
            return drawableMap.get(urlString);
        }
        try {
            InputStream is = fetch(urlString);
            Drawable drawable = Drawable.createFromStream(is, "src");
            drawableMap.put(urlString, drawable);
            return drawable;
        } catch (Exception e) {
            return null;
        }
    }

    private InputStream fetch(String urlString) throws MalformedURLException, IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(urlString);
        HttpResponse response = httpClient.execute(request);
        return response.getEntity().getContent();
    }
}
