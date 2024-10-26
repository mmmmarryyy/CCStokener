package org.wi.ctrl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.wi.etc.Util;
import org.wi.ui.SecondActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;

public class ImageLoader {

    /**
	 * determine the order to load the image, 
	 * 		notice it will conflict between two threads 
	 * true: from local cache first
	 * false: from Internet first
	 */
    public static boolean usingCacheFirst = true;

    private Context context;

    private static ImageLoader instance = null;

    private Thread imageLoadThread = null;

    private Queue<ThreadBlock> loadQueue = new LinkedBlockingQueue<ImageLoader.ThreadBlock>();

    public static ImageLoader getInstance(Context context) {
        if (null != instance) return instance; else return new ImageLoader(context);
    }

    public static ImageLoader getInstance() {
        return instance;
    }

    public ImageLoader(Context context) {
        this.context = context;
        imageLoadThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (loadQueue.isEmpty()) {
                        try {
                            synchronized (imageLoadThread) {
                                imageLoadThread.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.i("ImageLoader::imageLoadThread", "Loading image in special thread...");
                    ThreadBlock block = null;
                    synchronized (loadQueue) {
                        block = loadQueue.poll();
                    }
                    if (null != block) {
                        try {
                            Method callback = block.receiver.getClass().getMethod(block.callbackname, View.class, Bitmap.class);
                            Bitmap bitmap = BitmapFactory.decodeStream(new URL(block.urlstr).openStream());
                            FileOutputStream fos = ImageLoader.this.context.openFileOutput(block.fileName, Context.MODE_PRIVATE);
                            Log.i("ImageLoader::loadImageAsync", "Save image to " + block.fileName);
                            bitmap.compress(CompressFormat.PNG, 1, fos);
                            fos.close();
                            callback.invoke(block.receiver, block.view, bitmap);
                        } catch (Exception e) {
                            Log.e("ImageLoader::loadImageAsync", e.getMessage());
                        }
                    }
                }
            }
        });
        imageLoadThread.setDaemon(true);
        imageLoadThread.start();
    }

    /**
	 * Load an image file from local cache.
	 * @param urlstr
	 * @return Bitmap, otherwise null
	 */
    public Bitmap loadImageFromCache(final String urlstr) {
        String fileName = Util.namingImage(urlstr);
        try {
            FileInputStream fis = context.openFileInput(fileName);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            Log.i("ImageLoader::loadImageFromCache", "We found a image cache file and load it.");
            return bitmap;
        } catch (Exception e) {
            Log.w("ImageLoader::loadImageFromCache", "No resource in cache: " + e.getMessage());
        }
        return null;
    }

    /**
	 * Load an image file from specified uri.
	 * @param urlstr
	 * @return Bitmap, otherwise null
	 */
    public Bitmap loadImageFromNetwork(final String urlstr) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new URL(urlstr).openStream());
            Log.i("ImageLoader::loadImageFromCache", "We found a image cache file and load it.");
            return bitmap;
        } catch (Exception e) {
            Log.w("ImageLoader::loadImageFromCache", "No resource in cache: " + e.getMessage());
        }
        return null;
    }

    /**
	 * Load image asynchrony.
	 * @param receiver - receive object
	 * @param callbackname - CALLBACK name
	 * @param view - image view to display it
	 * @param urlstr - where to load
	 */
    public void loadImageAsync(final Object receiver, final String callbackname, final View view, final String urlstr) {
        if (null == urlstr || "".equals(urlstr.trim())) {
            return;
        }
        final String fileName = Util.namingImage(urlstr);
        if (usingCacheFirst) {
            try {
                FileInputStream fis = context.openFileInput(fileName);
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                fis.close();
                if (null == bitmap) throw new Exception("Image not available.");
                Method callback = receiver.getClass().getMethod(callbackname, View.class, Bitmap.class);
                callback.invoke(receiver, view, bitmap);
                Log.i("ImageLoader::loadImageAsync", "We found a image cache file and load it.");
                return;
            } catch (Exception e) {
                Log.w("ImageLoader::loadImageAsync", "No/Bad resource in cache: " + e.getMessage());
            }
        }
        synchronized (loadQueue) {
            loadQueue.add(new ThreadBlock(receiver, callbackname, view, urlstr, fileName));
        }
        synchronized (imageLoadThread) {
            imageLoadThread.notify();
        }
    }

    protected class ThreadBlock {

        public Object receiver;

        public String callbackname;

        public View view;

        public String urlstr;

        public String fileName;

        public ThreadBlock(final Object receiver, final String callbackname, final View view, final String urlstr, final String fileName) {
            this.receiver = receiver;
            this.callbackname = callbackname;
            this.view = view;
            this.urlstr = urlstr;
            this.fileName = fileName;
        }
    }
}
