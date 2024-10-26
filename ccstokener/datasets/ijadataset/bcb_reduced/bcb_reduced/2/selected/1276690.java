package com.ad_oss.merkat;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

public class IconDownloader extends AsyncTask<Uri, Integer, Uri> {

    private final IconHolder mTarget;

    private final int mDimension;

    private final Context mContext;

    private Bitmap mBitmap;

    interface IconHolder {

        void preDownloadIcon(IconDownloader me);

        void postDownloadIcon(IconDownloader me, Bitmap bitmap);
    }

    public IconDownloader(Context context, IconHolder holder, int dimension) {
        mTarget = holder;
        mDimension = dimension;
        mContext = context;
    }

    @Override
    protected Uri doInBackground(Uri... params) {
        try {
            InputStream is = mContext.getContentResolver().openInputStream(params[0]);
            mBitmap = BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            try {
                URL url = new URL(params[0].toString());
                URLConnection conn = url.openConnection();
                conn.setDoOutput(false);
                conn.setDefaultUseCaches(true);
                conn.setUseCaches(true);
                conn.setConnectTimeout(5000);
                InputStream is = conn.getInputStream();
                mBitmap = BitmapFactory.decodeStream(is);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (FileNotFoundException fe) {
                fe.printStackTrace();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        if (mBitmap == null) return null;
        if (mBitmap.getHeight() > mDimension || mBitmap.getWidth() > mDimension) {
            int width = mBitmap.getWidth() > mDimension ? mDimension : (mDimension * mBitmap.getWidth() / mBitmap.getHeight());
            int height = mBitmap.getHeight() > mDimension ? mDimension : (mDimension * mBitmap.getHeight() / mBitmap.getWidth());
            mBitmap = Bitmap.createScaledBitmap(mBitmap, width, height, true);
        }
        return params[0];
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mTarget.preDownloadIcon(this);
    }

    @Override
    protected void onPostExecute(Uri result) {
        super.onPostExecute(result);
        mTarget.postDownloadIcon(this, mBitmap);
    }
}
