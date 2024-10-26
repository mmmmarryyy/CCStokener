package com.beefeng.android.cache;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class ImageLoader implements Runnable {

    public static final int HANDLER_MESSAGE_ID = 0;

    public static final String BITMAP_EXTRA = "imageloader:extra_bitmap";

    public static final String IMAGE_URL_EXTRA = "imageloader:extra_image_url";

    private static final int DEFAULT_RETRY_HANDLER_SLEEP_TIME = 1000;

    private static final int DEFAULT_NUM_RETRIES = 3;

    private static final int DEFAULT_POOL_SIZE = 3;

    private static ThreadPoolExecutor executor;

    private static MemImageCache mMemCache;

    private static DiskImageCache mDiskCache;

    private static int numRetries = DEFAULT_NUM_RETRIES;

    private static Context mContext;

    /**
     * @param numThreads
     *            the maximum number of threads that will be started to download images in parallel
     */
    public static void setThreadPoolSize(int numThreads) {
        executor.setMaximumPoolSize(numThreads);
    }

    /**
     * @param numAttempts
     *            how often the image loader should retry the image download if network connection
     *            fails
     */
    public static void setMaxDownloadAttempts(int numAttempts) {
        ImageLoader.numRetries = numAttempts;
    }

    public static synchronized void initialize(Context context) {
        mContext = context.getApplicationContext();
        if (executor == null) {
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
        }
        if (mMemCache == null) {
            mMemCache = new MemImageCache(10);
        }
        if (mDiskCache == null) {
            mDiskCache = new DiskImageCache(context);
        }
    }

    public static synchronized void clearMemCache() {
        if (mMemCache == null) {
            mMemCache.clearImage();
        }
    }

    private String imageUrl;

    private ImageLoaderHandler handler;

    private ImageLoader(String imageUrl, ImageLoaderHandler handler) {
        this.imageUrl = imageUrl;
        this.handler = handler;
    }

    public static void start(String imageUrl, ImageView imageView, ImageLoaderHandler handler) {
        start(imageUrl, handler);
    }

    public static void start(String imageUrl, ImageLoaderHandler handler) {
        Bitmap bitmap = mMemCache.getBitmap(imageUrl);
        if (null == bitmap) {
            bitmap = mDiskCache.getBitmap(imageUrl);
            if (null == bitmap) {
                executor.execute(new ImageLoader(imageUrl, handler));
            } else {
                mMemCache.putBitmap(imageUrl, bitmap);
            }
        }
        handler.handleImageLoaded(imageUrl, bitmap);
    }

    @Override
    public void run() {
        Bitmap bitmap = mMemCache.getBitmap(imageUrl);
        if (bitmap == null) {
            bitmap = downloadImage();
        }
        notifyImageLoaded(imageUrl, bitmap);
    }

    protected Bitmap downloadImage() {
        boolean save_image = false;
        int timesTried = 1;
        Bitmap bitmap = null;
        while (timesTried <= numRetries) {
            try {
                byte[] imageData = retrieveImageData();
                if (imageData != null) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
                    opts.inTargetDensity = metrics.densityDpi;
                    opts.inScaled = true;
                    opts.inDither = false;
                    opts.inPurgeable = true;
                    opts.inInputShareable = true;
                    bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, opts);
                    mMemCache.putBitmap(imageUrl, bitmap);
                    save_image = mDiskCache.putBitmap(imageUrl, imageData);
                    break;
                }
            } catch (Throwable e) {
                if (e instanceof FileNotFoundException) {
                    break;
                } else {
                }
                e.printStackTrace();
                SystemClock.sleep(DEFAULT_RETRY_HANDLER_SLEEP_TIME);
                timesTried++;
            }
        }
        if (!save_image) {
            mDiskCache.removeImage(imageUrl);
        }
        return bitmap;
    }

    public static boolean isCached(String imageUrl) {
        return mDiskCache.hasImage(imageUrl);
    }

    protected byte[] retrieveImageData() throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize = connection.getContentLength();
        if (fileSize < 0) {
            return null;
        }
        byte[] imageData = new byte[fileSize];
        BufferedInputStream istream = new BufferedInputStream(connection.getInputStream(), 8192);
        int bytesRead = 0;
        int offset = 0;
        while (bytesRead != -1 && offset < fileSize) {
            bytesRead = istream.read(imageData, offset, fileSize - offset);
            offset += bytesRead;
        }
        istream.close();
        connection.disconnect();
        return imageData;
    }

    public void notifyImageLoaded(String url, Bitmap bitmap) {
        Message message = new Message();
        message.what = HANDLER_MESSAGE_ID;
        Bundle data = new Bundle();
        data.putString(IMAGE_URL_EXTRA, url);
        data.putParcelable(BITMAP_EXTRA, bitmap);
        message.setData(data);
        handler.sendMessage(message);
    }
}
