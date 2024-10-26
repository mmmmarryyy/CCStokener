package net.yama.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import net.yama.android.managers.config.ConfigurationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

public class DrawableManager {

    private static final int BUFFER_SIZE = 25600;

    static Map<String, SoftReference<Drawable>> drawableCache;

    BlockingQueue<MessageContainer> imageWorkQueue;

    private ImageFetcher fetcher;

    Thread imageThread;

    List<ImageFetcher> imageThreads;

    public static DrawableManager instance = new DrawableManager();

    private DrawableManager() {
        drawableCache = new HashMap<String, SoftReference<Drawable>>();
        imageWorkQueue = new ArrayBlockingQueue<MessageContainer>(50);
        int fetchingThreadCount = ConfigurationManager.instance.getImageThreads();
        if (fetchingThreadCount == 0) fetchingThreadCount = 3;
        imageThreads = new ArrayList<ImageFetcher>(fetchingThreadCount);
        for (int i = 0; i < fetchingThreadCount; i++) {
            fetcher = new ImageFetcher();
            imageThread = new Thread(fetcher);
            imageThreads.add(fetcher);
            imageThread.start();
        }
    }

    final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message message) {
            MessageContainer cont = (MessageContainer) message.obj;
            ImageView view = cont.getView();
            Drawable drawable = cont.getDrawable();
            view.setImageDrawable(drawable);
        }
    };

    /**
	 * @param urlString
	 * @return
	 */
    public Drawable fetchDrawable(String urlString) {
        SoftReference<Drawable> drawableRef = drawableCache.get(urlString);
        if (drawableRef != null) {
            Drawable drawable = drawableRef.get();
            if (drawable != null) return drawable;
            drawableCache.remove(urlString);
        }
        try {
            File image = fetch(urlString);
            Bitmap photo = null;
            Drawable drawable = null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(image), null, opts);
            opts.inJustDecodeBounds = false;
            if (opts.outWidth > 1000) {
                opts.inSampleSize = 4;
                photo = BitmapFactory.decodeStream(new FileInputStream(image), null, opts);
            } else {
                photo = BitmapFactory.decodeStream(new FileInputStream(image));
            }
            drawable = new BitmapDrawable(photo);
            drawableRef = new SoftReference<Drawable>(drawable);
            drawableCache.put(urlString, drawableRef);
            return drawableRef.get();
        } catch (Exception e) {
            return null;
        }
    }

    private File fetch(String urlString) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        String fileName = urlString.substring(urlString.lastIndexOf("/"));
        File imageStorage = Helper.getImagesDirectory();
        File image = new File(imageStorage, fileName);
        if (image.exists()) {
            return image;
        }
        FileOutputStream fos = new FileOutputStream(image);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        byte[] buffer = new byte[25600];
        int len = -1;
        if (conn.getResponseCode() == 200) {
            InputStream is = conn.getInputStream();
            len = is.read(buffer, 0, 25600);
            while (len != -1) {
                fos.write(buffer, 0, len);
                len = is.read(buffer, 0, BUFFER_SIZE);
            }
        }
        conn.disconnect();
        fos.close();
        fos = null;
        return image;
    }

    public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
        SoftReference<Drawable> drawableRef = drawableCache.get(urlString);
        if (drawableRef != null) {
            Drawable drawable = drawableRef.get();
            if (drawable != null) {
                imageView.setImageDrawable(drawableRef.get());
                return;
            }
            drawableCache.remove(urlString);
        }
        if (Looper.myLooper() == null) Looper.prepare();
        try {
            imageWorkQueue.put(new MessageContainer(imageView, urlString));
        } catch (InterruptedException e) {
        }
    }

    @Override
    protected void finalize() throws Throwable {
        for (ImageFetcher fetcher : imageThreads) fetcher.stop = true;
        super.finalize();
    }

    /**
	 * Does the actual work of fetching.
	 * @author Rohit Kumbhar
	 */
    class ImageFetcher implements Runnable {

        public boolean stop = false;

        public void run() {
            while (!stop) {
                try {
                    MessageContainer work = imageWorkQueue.take();
                    Drawable drawable = fetchDrawable(work.getImageUrl());
                    work.setDrawable(drawable);
                    Message message = handler.obtainMessage(1, work);
                    handler.sendMessage(message);
                    System.gc();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
	 * As the name says, a message container to pass info between caller, fetching threads and handler
	 * @author Rohit Kumbhar
	 *
	 */
    class MessageContainer {

        private ImageView view;

        private Drawable drawable;

        private String imageUrl;

        public MessageContainer(ImageView view, Drawable drawable) {
            super();
            this.view = view;
            this.drawable = drawable;
        }

        public MessageContainer(ImageView view, String imageUrl) {
            super();
            this.view = view;
            this.imageUrl = imageUrl;
        }

        public ImageView getView() {
            return view;
        }

        public void setView(ImageView view) {
            this.view = view;
        }

        public Drawable getDrawable() {
            return drawable;
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
