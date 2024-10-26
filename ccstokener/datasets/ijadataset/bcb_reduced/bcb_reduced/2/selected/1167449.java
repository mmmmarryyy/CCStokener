package org.openremote.android.console.image;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

/**
 * this does the multithreaded image loading/caching. Without this, the app
 * shows "timeout" errors if loading them in the foreground. It caches both
 * bitmaps and prepopulated view objects and "typers" which are objects from the
 * correct activity which are used to pick ImageView or ImageButton depending on
 * the size of the image.
 * 
 * @author Andrew C. Oliver <acoliver at osintegrators.com>
 */
public class ImageLoader implements Runnable {

    private Map<String, Bitmap> images;

    private Map<String, List<ImageView>> views;

    private Map<String, List<Typer>> typers;

    private Thread thread;

    private boolean stop;

    private boolean hasRun;

    public interface Typer {

        ImageView type(Bitmap b);
    }

    public interface Sizer {

        int[] getHandW(Bitmap b);
    }

    public ImageLoader() {
        this.stop = false;
        this.images = new HashMap<String, Bitmap>();
        images = Collections.synchronizedMap(this.images);
        this.views = new HashMap<String, List<ImageView>>();
        this.views = Collections.synchronizedMap(this.views);
        this.typers = new HashMap<String, List<Typer>>();
        this.typers = Collections.synchronizedMap(this.typers);
    }

    public void load(String url, Typer typer) {
        if (this.images.get(url) == null) {
            this.images.put(url, null);
        }
        List<ImageView> iviews = this.views.get(url);
        if (iviews == null) {
            iviews = new ArrayList<ImageView>();
        }
        this.views.put(url, iviews);
        List<Typer> itypers = this.typers.get(url);
        if (itypers == null) {
            itypers = new ArrayList<Typer>();
        }
        if (typer != null) {
            itypers.add(typer);
        }
        this.typers.put(url, itypers);
    }

    public void start() {
        if (thread == null) {
            this.thread = new Thread(this);
            thread.start();
        } else if (thread.isAlive()) {
            this.stop = true;
            while (this.stop == true && thread != null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(this.toString(), "imageloader thread interrupted", e);
                }
            }
            this.thread = null;
            this.thread = new Thread(this);
            thread.start();
        } else {
            this.thread = null;
            this.thread = new Thread(this);
            thread.start();
        }
    }

    public boolean isStopped() {
        return this.thread == null || thread.isAlive() == false;
    }

    public boolean isLoaded(String url) {
        return this.images.get(url) != null;
    }

    public int getWidth(String url) {
        return this.images.get(url).getWidth();
    }

    public int getHeight(String url) {
        return this.images.get(url).getHeight();
    }

    @Override
    public void run() {
        Map<String, Bitmap> locImages = new HashMap<String, Bitmap>();
        locImages.putAll(this.images);
        Set<String> imageUrls = locImages.keySet();
        for (String string : imageUrls) {
            try {
                if (this.images.get(string) == null) {
                    Log.d(this.toString(), "loading a bitmap " + string);
                    long time = System.currentTimeMillis();
                    URL url = new URL(string);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    InputStream stream = con.getInputStream();
                    Bitmap b = BitmapFactory.decodeStream(stream);
                    stream.close();
                    Log.d(this.toString(), "loaded a bitmap in " + (System.currentTimeMillis() - time));
                    this.images.put(string, b);
                }
                if (this.stop == true) {
                    break;
                }
            } catch (Exception e) {
                this.images.put(string, null);
                Log.e(this.toString(), string + " could not be loaded", e);
            }
        }
        Map<String, List<ImageView>> locViews = new HashMap<String, List<ImageView>>();
        locViews.putAll(this.views);
        Set<String> keys = locViews.keySet();
        for (String key : keys) {
            if (this.stop == true) {
                break;
            }
            Bitmap b = this.images.get(key);
            if (b != null) {
                List<ImageView> iviews = this.views.get(key);
                List<Typer> itypers = this.typers.get(key);
                for (Typer typer : itypers) {
                    Log.d(this.toString(), "adding a image for " + key);
                    ImageView iview = typer.type(this.images.get(key));
                    iviews.add(iview);
                    iview.setImageBitmap(b);
                }
            }
        }
        this.stop = false;
        this.hasRun = true;
    }

    public Bitmap getBitmap(String url) {
        if (!isLoaded(url) && !hasRun) {
            while (!isLoaded(url) && !hasRun) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(this.toString(), "interrupted in getBitmap in ImageLoader", e);
                    return this.images.get(url);
                }
            }
        }
        return this.images.get(url);
    }

    public List<ImageView> getView(String url) {
        Log.d(this.toString(), "get view " + url + " hasrun=" + hasRun + " loaded=" + isLoaded(url));
        if ((!isLoaded(url) || this.views.get(url).size() == 0) && !hasRun) {
            while ((!isLoaded(url) || this.views.get(url).size() == 0) && !hasRun) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(this.toString(), "interrupted in getView in ImageLoader", e);
                    return this.views.get(url);
                }
            }
        }
        return this.views.get(url);
    }

    public ImageLoader reset() {
        if (this.thread != null && thread.isAlive()) {
            this.stop = true;
            while (stop) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e(this.toString(), "interrupted thread while reset called", e);
                }
            }
        }
        this.views = new HashMap<String, List<ImageView>>();
        this.views = Collections.synchronizedMap(this.views);
        this.typers = new HashMap<String, List<Typer>>();
        this.typers = Collections.synchronizedMap(this.typers);
        this.hasRun = false;
        return this;
    }
}
