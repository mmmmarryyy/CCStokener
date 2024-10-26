package minghai.practice;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class PhotoResource {

    private String url;

    private String thumburl;

    private String title;

    private Bitmap bitmap;

    private Bitmap thumbnail;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Bitmap getBitmap() {
        if (bitmap == null) {
            bitmap = downloadBitmap(url);
        }
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getThumbnail() {
        if (thumbnail == null) {
            thumbnail = downloadBitmap(thumburl);
        }
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getThumburl() {
        return thumburl;
    }

    public void setThumburl(String thumburl) {
        this.thumburl = thumburl;
    }

    public Bitmap downloadBitmap(String url) {
        HttpURLConnection huc = null;
        InputStream is = null;
        Bitmap bm = null;
        try {
            huc = ((HttpURLConnection) (new URL(url).openConnection()));
            huc.setDoInput(true);
            huc.connect();
            is = huc.getInputStream();
            bm = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Log.d("TEST", e.getMessage(), e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                Log.d("TEST", e.getMessage(), e);
            }
            if (huc != null) huc.disconnect();
        }
        return bm;
    }
}
