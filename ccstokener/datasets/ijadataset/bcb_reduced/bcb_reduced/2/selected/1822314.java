package jp.ac.tokai.et.lifemode2;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

public class HCamera implements ICameraSource {

    private static final int CONNECT_TIMEOUT = 1000;

    private static final int SOCKET_TIMEOUT = 1000;

    private final String url;

    private Rect bounds;

    private Paint paint = new Paint();

    private Bitmap bitmap2 = null;

    private Bitmap view;

    public HCamera(String url) {
        this.url = url;
    }

    @Override
    public boolean capture(Canvas canvas) {
        Bitmap bitmap = null;
        InputStream in = null;
        if (canvas == null) throw new IllegalArgumentException("null canvas");
        try {
            in = connectURL(this.url);
            bitmap = BitmapFactory.decodeStream(in);
            Rect bounds = this.bounds;
            Rect dest = bounds;
            canvas.drawBitmap(bitmap, null, dest, paint);
            bitmap2 = bitmap;
            in.close();
        } catch (RuntimeException e) {
            Log.i(LOG_TAG, "Failed to obtain image over network", e);
            return false;
        } catch (IOException e) {
            Log.i(LOG_TAG, "Failed to obtain image over network", e);
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        return;
    }

    @Override
    public int getHeight() {
        if (bitmap2 == null) {
            return 0;
        }
        return bitmap2.getHeight();
    }

    @Override
    public int getWidth() {
        if (bitmap2 == null) {
            return 0;
        }
        return bitmap2.getWidth();
    }

    @Override
    public boolean open() {
        paint.setFilterBitmap(true);
        bounds = new Rect(0, 0, 320, 240);
        return true;
    }

    public boolean open(Rect bounds_ex) {
        open();
        bounds = bounds_ex;
        return true;
    }

    private InputStream connectURL(String aurl) throws IOException {
        InputStream in = null;
        int response = -1;
        URL url = new URL(aurl);
        URLConnection conn = url.openConnection();
        if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection.");
        HttpURLConnection httpConn = (HttpURLConnection) conn;
        response = getResponse(httpConn);
        if (response == HttpURLConnection.HTTP_OK) {
            in = httpConn.getInputStream();
        } else throw new IOException("Response Code: " + response);
        return in;
    }

    private int getResponse(HttpURLConnection httpConn) throws ProtocolException, IOException {
        int response = -1;
        httpConn.setAllowUserInteraction(false);
        httpConn.setConnectTimeout(CONNECT_TIMEOUT);
        httpConn.setReadTimeout(SOCKET_TIMEOUT);
        httpConn.setInstanceFollowRedirects(true);
        httpConn.setRequestMethod("GET");
        httpConn.connect();
        response = httpConn.getResponseCode();
        return response;
    }
}
