package ch.bfh.CityExplorer.Activities;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ImageReceiver extends Thread {

    private static final String TAG = "ImageReceiver";

    String url;

    ImageReceivedCallback callback;

    ImageView view;

    Display display;

    ProgressBar pbar;

    public ImageReceiver(String url, ImageReceivedCallback callback, ImageView view, Display display, ProgressBar pbar) {
        this.url = url;
        this.callback = callback;
        this.view = view;
        this.display = display;
        this.pbar = pbar;
        start();
    }

    public void run() {
        try {
            URLConnection conn = (new URL(url)).openConnection();
            conn.connect();
            Bitmap bm = BitmapFactory.decodeStream(conn.getInputStream());
            if (bm == null) {
                ImageDisplayer displayer = new ImageDisplayer(view, null, pbar, display);
                callback.onImageReceived(displayer);
            } else {
                ImageDisplayer displayer = new ImageDisplayer(view, bm, pbar, display);
                callback.onImageReceived(displayer);
            }
        } catch (IOException e) {
            ImageDisplayer displayer = new ImageDisplayer(view, null, pbar, display);
            callback.onImageReceived(displayer);
            Log.e(TAG, "Error downloading bitmap: " + url, e);
        }
    }
}
