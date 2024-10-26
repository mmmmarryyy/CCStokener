package com.whale.util.image;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * 图片下载缓存类,为了更高的效率，使用了两个缓存。根据LRU原则，会把常用的放在mHardBitmapCache中
 * 最近没有被使用的放在mSoftBitmapCache中。如果此类30秒没有被调用，那么会自动清除缓存，从而释放内存空间
 * 
 * @author lzk
 * 
 */
public class ImageDownloader {

    private static final String TAG = "ImageDownloader";

    private static final int HARD_CACHE_CAPACITY = 30;

    private static final int DELAY_BEFORE_CLEAR = 30 * 1000;

    private Integer mDefaultImage;

    @SuppressWarnings("serial")
    private final HashMap<String, Bitmap> mHardBitmapCache = new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                mSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                return true;
            } else return false;
        }
    };

    /**
	 * 当mHardBitmapCache的key大于30的时候，会根据LRU算法把最近没有被使用的key放入到这个缓存中。
	 * Bitmap使用了SoftReference，当内存空间不足时，此cache中的bitmap会被垃圾回收掉
	 */
    private static final ConcurrentHashMap<String, SoftReference<Bitmap>> mSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);

    private final Handler clearHandler = new Handler();

    private final Runnable clear = new Runnable() {

        public void run() {
            clearCache();
        }
    };

    /**
	 * 设置默认图片
	 * 
	 * @param resid
	 * @param imageView
	 */
    public void setDefaultImage(Integer resid, ImageView imageView) {
        mDefaultImage = resid;
        imageView.setImageResource(mDefaultImage);
    }

    /**
	 * 从缓存中获取图片，如果没有的话直接下载
	 */
    public void download(String url, ImageView imageView) {
        resetClearTimer();
        Bitmap bitmap = getBitmapFromCache(url);
        if (bitmap == null) {
            downloadBitmap(url, imageView);
        } else {
            cancelPotentialDownload(url, imageView);
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
	 * 下载图片
	 */
    private void downloadBitmap(String url, ImageView imageView) {
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }
        if (cancelPotentialDownload(url, imageView)) {
            ImageDownloaderTask task = new ImageDownloaderTask(imageView);
            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
            imageView.setImageDrawable(downloadedDrawable);
            task.execute(url);
        }
    }

    /**
	 * 取消现有的下载线程
	 */
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        ImageDownloaderTask mImageDownloaderTask = getBitmapDownloaderTask(imageView);
        if (mImageDownloaderTask != null) {
            String bitmapUrl = mImageDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                mImageDownloaderTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
	 * 获得当前的imageView的下载线程
	 * 
	 * @param imageView
	 * @return
	 */
    private static ImageDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    /**
	 * 从缓存中获取图片
	 */
    private Bitmap getBitmapFromCache(String url) {
        synchronized (mHardBitmapCache) {
            final Bitmap bitmap = mHardBitmapCache.get(url);
            if (bitmap != null) {
                mHardBitmapCache.remove(url);
                mHardBitmapCache.put(url, bitmap);
                return bitmap;
            }
        }
        SoftReference<Bitmap> bitmapReference = mSoftBitmapCache.get(url);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                return bitmap;
            } else {
                mSoftBitmapCache.remove(url);
            }
        }
        return null;
    }

    /**
	 * 异步下载图片
	 */
    class ImageDownloaderTask extends AsyncTask<String, Void, Bitmap> {

        private static final int IO_BUFFER_SIZE = 4 * 1024;

        private String url;

        private final WeakReference<ImageView> imageViewReference;

        public ImageDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            url = params[0];
            final HttpGet getRequest = new HttpGet(url);
            try {
                HttpResponse response = client.execute(getRequest);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    Log.w(TAG, "从" + url + "中下载图片时出错!,错误码:" + statusCode);
                    return null;
                }
                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        inputStream = entity.getContent();
                        final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                        outputStream = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
                        copy(inputStream, outputStream);
                        outputStream.flush();
                        final byte[] data = dataStream.toByteArray();
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        return bitmap;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            } catch (IOException e) {
                getRequest.abort();
                Log.w(TAG, "I/O error while retrieving bitmap from " + url, e);
            } catch (IllegalStateException e) {
                getRequest.abort();
                Log.w(TAG, "Incorrect URL: " + url);
            } catch (Exception e) {
                getRequest.abort();
                Log.w(TAG, "Error while retrieving bitmap from " + url, e);
            } finally {
                if (client != null) {
                    client.close();
                }
            }
            return null;
        }

        /**
		 * 异步任务完成后，在主线程中把bitmap绑定到imageView上
		 */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            if (bitmap != null) {
                synchronized (mHardBitmapCache) {
                    mHardBitmapCache.put(url, bitmap);
                }
            }
            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                ImageDownloaderTask mImageDownloaderTask = getBitmapDownloaderTask(imageView);
                if (this == mImageDownloaderTask) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        public void copy(InputStream in, OutputStream out) throws IOException {
            byte[] b = new byte[IO_BUFFER_SIZE];
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
            }
        }
    }

    /**
	 * 清除缓存
	 */
    public void clearCache() {
        mHardBitmapCache.clear();
        mSoftBitmapCache.clear();
    }

    /**
	 * 重设缓存的过期时间
	 */
    private void resetClearTimer() {
        clearHandler.removeCallbacks(clear);
        clearHandler.postDelayed(clear, DELAY_BEFORE_CLEAR);
    }

    static class DownloadedDrawable extends ColorDrawable {

        private final WeakReference<ImageDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(ImageDownloaderTask mImageDownloaderTask) {
            super(Color.BLACK);
            bitmapDownloaderTaskReference = new WeakReference<ImageDownloaderTask>(mImageDownloaderTask);
        }

        public ImageDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }
}
