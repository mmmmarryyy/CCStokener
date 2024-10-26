package com.commonsware.android.EMusicDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class ImageLoader {

    private HashMap<String, Bitmap> cache = new HashMap<String, Bitmap>();

    private File cacheDir;

    public ImageLoader(Context context) {
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "LazyList");
        } else {
            cacheDir = context.getCacheDir();
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public ImageLoader(Context context, HashMap<String, Bitmap> cachein) {
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);
        cache = cachein;
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "LazyList");
        } else {
            cacheDir = context.getCacheDir();
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    final int stub_id = R.drawable.stub;

    public void DisplayImage(String url, ImageView imageView) {
        if (cache.containsKey(url)) {
            imageView.setImageBitmap(cache.get(url));
        } else {
            queuePhoto(url, imageView);
            imageView.setImageResource(stub_id);
        }
    }

    public void DisplayImage(String url) {
        if (cache.containsKey(url)) {
        } else {
            queuePhoto(url, null);
        }
    }

    private void queuePhoto(String url, ImageView imageView) {
        if (imageView != null) {
            photosQueue.Clean(imageView);
        }
        PhotoToLoad p = new PhotoToLoad(url, imageView);
        synchronized (photosQueue.photosToLoad) {
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }
        try {
            if (photoLoaderThread.getState() == Thread.State.NEW) {
                photoLoaderThread.start();
            }
        } catch (Exception ef) {
        }
    }

    private Bitmap getBitmap(String url) {
        try {
            String filename = String.valueOf(url.hashCode());
            File f = new File(cacheDir, filename);
            Bitmap b = decodeFile(f);
            if (b != null) {
                return b;
            }
            Bitmap bitmap = null;
            InputStream is = new URL(url).openStream();
            OutputStream os = new FileOutputStream(f);
            Utils.CopyStream(is, os);
            os.close();
            is.close();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Bitmap decodeFile(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
            final int REQUIRED_SIZE = 70;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale++;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (Exception e) {
        }
        return null;
    }

    private class PhotoToLoad {

        public String url;

        public ImageView imageView;

        public PhotoToLoad(String u, ImageView i) {
            url = u;
            imageView = i;
        }
    }

    PhotosQueue photosQueue = new PhotosQueue();

    public void stopThread() {
        photoLoaderThread.interrupt();
    }

    class PhotosQueue {

        private Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();

        public void Clean(ImageView image) {
            for (int j = 0; j < photosToLoad.size(); ) {
                ImageView tempimage = null;
                try {
                    tempimage = photosToLoad.get(j).imageView;
                } catch (Exception ef) {
                }
                if (tempimage == image) {
                    try {
                        photosToLoad.remove(j);
                    } catch (Exception ef) {
                    }
                } else {
                    ++j;
                }
            }
        }
    }

    class PhotosLoader extends Thread {

        public void run() {
            try {
                while (true) {
                    if (photosQueue.photosToLoad.size() == 0) {
                        synchronized (photosQueue.photosToLoad) {
                            photosQueue.photosToLoad.wait();
                        }
                    }
                    if (photosQueue.photosToLoad.size() != 0) {
                        PhotoToLoad photoToLoad;
                        synchronized (photosQueue.photosToLoad) {
                            photoToLoad = photosQueue.photosToLoad.pop();
                        }
                        Bitmap bmp = getBitmap(photoToLoad.url);
                        cache.put(photoToLoad.url, bmp);
                        if (photoToLoad.imageView != null) {
                            if (((String) photoToLoad.imageView.getTag()).equals(photoToLoad.url)) {
                                BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad.imageView);
                                Activity a = (Activity) photoToLoad.imageView.getContext();
                                a.runOnUiThread(bd);
                            }
                        }
                    }
                    if (Thread.interrupted()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    PhotosLoader photoLoaderThread = new PhotosLoader();

    class BitmapDisplayer implements Runnable {

        Bitmap bitmap;

        ImageView imageView;

        public BitmapDisplayer(Bitmap b, ImageView i) {
            bitmap = b;
            imageView = i;
        }

        public void run() {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(stub_id);
            }
        }
    }

    public void clearCache() {
        cache.clear();
        File[] files = cacheDir.listFiles();
        for (File f : files) {
            f.delete();
        }
    }
}
