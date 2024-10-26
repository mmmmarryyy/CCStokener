package main.com.pyjioh.core;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import main.com.pyjioh.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class ImageDownloader {

    private Map<String, Bitmap> mImageCache = new TreeMap<String, Bitmap>();

    public void download(String url, ImageView imageView) {
        BitmapDownloaderTask task = new BitmapDownloaderTask(imageView, mImageCache);
        task.execute(url);
    }
}

class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {

    private static final int IMAGE_CACHE_LIMIT = 3;

    private static final String LOG_TAG = "ImageDownloader";

    private Map<String, Bitmap> mImageCache;

    private ProgressDialog mProgressDlg;

    private ImageView mImageView;

    private Bitmap mBitmap;

    public BitmapDownloaderTask(ImageView imageView, Map<String, Bitmap> imageCache) {
        mImageView = imageView;
        mImageCache = imageCache;
    }

    private Bitmap downloadBitmap(String url) {
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);
        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(LOG_TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            getRequest.abort();
            Log.w(LOG_TAG, "Error while retrieving bitmap from " + url, e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    private Bitmap getBitmap(String url) {
        Bitmap bitmap = mImageCache.get(url);
        if (bitmap == null) bitmap = downloadBitmap(url);
        return bitmap;
    }

    private void putBitmapToMap(String url, Bitmap bitmap) {
        if (bitmap != null && !mImageCache.containsKey(url)) {
            if (mImageCache.size() >= IMAGE_CACHE_LIMIT) mImageCache.clear();
            mImageCache.put(url, bitmap);
        }
    }

    private void setImageBitmapToView(Bitmap bitmap) {
        if (bitmap != null) mImageView.setImageBitmap(bitmap); else mImageView.setImageResource(R.drawable.no_image);
    }

    @Override
    protected void onPreExecute() {
        mProgressDlg = ProgressDialog.show(mImageView.getContext(), null, mImageView.getContext().getText(R.string.msg_loading));
        mProgressDlg.setIndeterminate(true);
        mProgressDlg.setCancelable(true);
        mProgressDlg.show();
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        mBitmap = getBitmap(params[0]);
        putBitmapToMap(params[0], mBitmap);
        return mBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (!isCancelled()) setImageBitmapToView(bitmap);
        mProgressDlg.dismiss();
    }
}
