package jblip.gui.data.images;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.SwingUtilities;

public class ImageCache {

    private static void load(final File cache_file, final Map<URL, ImageRequest> cache) {
        if (!cache_file.canRead()) {
            System.err.println("Can't read from cache file: " + cache_file.getPath());
            return;
        }
        ObjectInputStream input = null;
        int total_read = 0;
        try {
            input = new ObjectInputStream(new FileInputStream(cache_file));
            while (true) {
                final ImageRequest imgreq = (ImageRequest) input.readObject();
                if (imgreq.img_filename.exists()) {
                    cache.put(imgreq.img_url, imgreq);
                }
                total_read++;
            }
        } catch (EOFException eof) {
            System.err.println("Read cache for " + total_read + " images");
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }

    private static void store(final File cache_file, final Map<URL, ImageRequest> cache) {
        System.err.println("Storing image cache");
        try {
            final ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(cache_file));
            int total_wrote = 0;
            for (ImageRequest imgreq : cache.values()) {
                if (!imgreq.hasImage()) {
                    continue;
                }
                output.writeObject(imgreq);
                total_wrote++;
            }
            System.err.println("Wrote cache for " + total_wrote + " images");
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final File download_dir;

    private final Queue<ImageRequest> requests;

    private final Map<URL, ImageRequest> cache;

    private final Thread download_thread;

    public ImageCache(final File cache_dir) {
        if (!cache_dir.isDirectory() || !cache_dir.canWrite()) {
            throw new IllegalArgumentException("Can't access or write to " + cache_dir.getPath());
        }
        cache = new HashMap<URL, ImageRequest>();
        final File cache_file = new File(cache_dir, "imgcache");
        load(cache_file, cache);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                store(cache_file, cache);
            }
        }));
        requests = new ConcurrentLinkedQueue<ImageRequest>();
        download_dir = cache_dir;
        download_thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    while (!requests.isEmpty()) {
                        ImageRequest request = requests.poll();
                        final File file = request.img_filename;
                        System.err.println("Downloading: " + request.img_url);
                        try {
                            URLConnection conn = (URLConnection) request.img_url.openConnection();
                            conn.connect();
                            if (file.exists()) {
                                file.delete();
                            }
                            file.createNewFile();
                            FileOutputStream output = new FileOutputStream(file);
                            InputStream input = conn.getInputStream();
                            final byte[] buffer = new byte[1024];
                            int last_read = 0;
                            do {
                                last_read = input.read(buffer);
                                if (last_read > 0) {
                                    output.write(buffer, 0, last_read);
                                }
                            } while (last_read != -1);
                            input.close();
                            output.close();
                        } catch (IOException e) {
                            requests.add(request);
                            e.printStackTrace();
                        }
                        request.notifyFileFetched();
                    }
                    synchronized (ImageCache.this) {
                        try {
                            ImageCache.this.wait(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "Images downloader");
        download_thread.setDaemon(true);
        download_thread.start();
    }

    public synchronized void getImage(final URL url, final ImageChangeListener listener) {
        ImageRequest request = cache.get(url);
        if (request == null) {
            request = new ImageRequest(url, getCacheFile(url));
            request.addReceiver(listener);
            requests.add(request);
            cache.put(url, request);
        } else {
            request.addReceiver(listener);
        }
    }

    private File getCacheFile(URL url) {
        final String url_path = url.getPath();
        String filename = url_path.substring(url_path.lastIndexOf('/'));
        if (filename.equals("")) {
            filename = "unknown";
        }
        int last_dot = filename.lastIndexOf('.');
        String filename_head = download_dir.getPath() + File.separator;
        String filename_ext;
        if (last_dot == -1) {
            filename_head += filename;
            filename_ext = "";
        } else {
            filename_head += filename.substring(0, last_dot);
            filename_ext = filename.substring(last_dot);
        }
        int ext_index = 1;
        filename = filename_head + filename_ext;
        File cache_file = new File(filename);
        while (cache_file.exists()) {
            filename = String.format("%s_%d%s", filename_head, ext_index++, filename_ext);
            cache_file = new File(filename);
        }
        return cache_file;
    }
}

class ImageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    final URL img_url;

    final File img_filename;

    private final ArrayList<ImageChangeListener> receivers;

    private transient Image img;

    public ImageRequest(final URL url, final File url_file) {
        this.img_url = url;
        this.img_filename = url_file;
        this.receivers = new ArrayList<ImageChangeListener>(4);
    }

    public synchronized boolean hasImage() {
        return img != null;
    }

    public synchronized Image getImage() {
        return img;
    }

    synchronized void addReceiver(ImageChangeListener listener) {
        receivers.add(listener);
        if (hasImage()) {
            notifyListeners();
        }
    }

    synchronized void notifyFileFetched() {
        this.img = Toolkit.getDefaultToolkit().createImage(img_filename.getPath());
        notifyListeners();
    }

    @SuppressWarnings("unchecked")
    synchronized void notifyListeners() {
        if (this.img == null) {
            throw new NullPointerException("Image stored is null");
        }
        SwingUtilities.invokeLater(new Runnable() {

            private final List<ImageChangeListener> listeners = (List<ImageChangeListener>) ImageRequest.this.receivers.clone();

            @Override
            public void run() {
                for (ImageChangeListener p : this.listeners) {
                    p.imageChange(ImageRequest.this.img);
                }
            }
        });
        receivers.clear();
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.img_filename.canRead()) {
            this.img = Toolkit.getDefaultToolkit().createImage(img_filename.getPath());
        }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        img = null;
        receivers.clear();
        out.defaultWriteObject();
    }
}
