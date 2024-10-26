package com.akop.spark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.util.Log;

public class ImageCache implements Runnable {

    private class Task {

        public OnImageReadyListener listener;

        public String imageUrl;

        public Object param;

        public int id;

        public boolean alwaysRun;

        public boolean force;

        public Task(String imageUrl, int id, Object param, OnImageReadyListener listener, boolean alwaysRun, boolean force) {
            this.listener = listener;
            this.imageUrl = imageUrl;
            this.param = param;
            this.id = id;
            this.alwaysRun = alwaysRun;
            this.force = force;
        }
    }

    private Thread mThread;

    private boolean mBusy;

    private Task mCurrentTask;

    private HashSet<OnImageReadyListener> mListeners;

    private BlockingQueue<Task> mTasks;

    private static ImageCache inst = null;

    private File mCacheDir;

    private ImageCache(Application application) {
        mTasks = new LinkedBlockingQueue<Task>();
        mListeners = new HashSet<OnImageReadyListener>();
        mCurrentTask = null;
        mCacheDir = application.getCacheDir();
        mThread = new Thread(this);
        mThread.start();
    }

    public static synchronized ImageCache getInstance(Application application) {
        if (inst == null) inst = new ImageCache(application);
        return inst;
    }

    public static synchronized ImageCache getInstance() {
        return inst;
    }

    public boolean isBusy() {
        return mBusy;
    }

    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                mCurrentTask = mTasks.take();
                if (mCurrentTask.alwaysRun || mListeners.contains(mCurrentTask.listener)) {
                    mBusy = true;
                    Bitmap bmp = loadBitmapSynchronous(mCurrentTask.imageUrl, mCurrentTask.force);
                    if (mCurrentTask.alwaysRun || mListeners.contains(mCurrentTask.listener)) mCurrentTask.listener.onImageReady(mCurrentTask.id, mCurrentTask.param, bmp);
                    mCurrentTask = null;
                }
            } catch (Exception e) {
                if (Spark.LOGV) {
                    Log.v(Spark.LOG_TAG, "Error running task", e);
                    e.printStackTrace();
                }
            }
            mBusy = false;
        }
    }

    public void addListener(OnImageReadyListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(OnImageReadyListener listener) {
        mListeners.remove(listener);
    }

    private File getCacheFile(String imageUrl) {
        int hashCode = imageUrl.hashCode();
        String hashStr;
        if (hashCode < 0) hashStr = "0" + (-hashCode); else hashStr = "" + hashCode;
        String filename = hashStr + ".cbm";
        return new File(mCacheDir, filename);
    }

    private Bitmap loadBitmapSynchronous(String imageUrl, boolean force) {
        File file = getCacheFile(imageUrl);
        if (!force && file.canRead()) {
            if (Spark.LOGV) Log.v(Spark.LOG_TAG, "Cache hit: " + file.getAbsolutePath());
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        byte[] blob;
        int length;
        try {
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream stream = conn.getInputStream();
            try {
                if ((length = conn.getContentLength()) <= 0) return null;
                blob = new byte[length];
                for (int r = 0; r < length; r += stream.read(blob, r, length - r)) ;
            } finally {
                if (stream != null) stream.close();
            }
        } catch (IOException e) {
            if (Spark.LOGV) e.printStackTrace();
            return null;
        }
        {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(blob);
                fos.close();
                if (Spark.LOGV) Log.v(Spark.LOG_TAG, "Wrote to cache: " + file.getAbsolutePath());
            } catch (IOException e) {
                if (Spark.LOGV) e.printStackTrace();
            }
        }
        return BitmapFactory.decodeByteArray(blob, 0, length);
    }

    public void clearCachedBitmap(String imageUrl) {
        File file = getCacheFile(imageUrl);
        if (file.canWrite()) {
            if (Spark.LOGV) Spark.logv("Purging %s from cache", imageUrl);
            file.delete();
        }
    }

    public Bitmap getCachedBitmap(String imageUrl) {
        File file = getCacheFile(imageUrl);
        if (!file.canRead()) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public boolean requestImage(String imageUrl, OnImageReadyListener listener, int id, Object param, boolean force) {
        return requestImage(imageUrl, listener, id, param, false, force);
    }

    public boolean requestImage(String imageUrl, OnImageReadyListener listener, int id, Object param) {
        return requestImage(imageUrl, listener, id, param, false, false);
    }

    public synchronized boolean requestImage(String imageUrl, OnImageReadyListener listener, int id, Object param, boolean alwaysRun, boolean force) {
        if (mCurrentTask != null && mCurrentTask.imageUrl.equals(imageUrl) && mCurrentTask.listener.equals(listener)) {
            if (Spark.LOGV) Log.v(Spark.LOG_TAG, "Image '" + imageUrl + "' already in queue");
            return false;
        }
        for (Task command : mTasks) {
            if (command.imageUrl.equals(imageUrl) && command.listener.equals(listener)) {
                if (Spark.LOGV) Log.v(Spark.LOG_TAG, "Image '" + imageUrl + "' already in queue");
                return false;
            }
        }
        if (Spark.LOGV) Log.v(Spark.LOG_TAG, "Image requested: " + imageUrl);
        try {
            mTasks.put(new Task(imageUrl, id, param, listener, alwaysRun, force));
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        return true;
    }
}
