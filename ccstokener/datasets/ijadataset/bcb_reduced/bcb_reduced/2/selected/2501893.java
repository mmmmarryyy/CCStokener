package vkmc;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import vkmc.events.downloader.ProgressChanged;
import vkmc.events.downloader.ProgressChangedListener;

/**
 *
 * @author Exit93
 */
public class AsyncDownloader extends Thread {

    private String url;

    private String path;

    private int progress;

    private int size = -1;

    private boolean canceled = false;

    private boolean completed = false;

    ProgressChanged pc = new ProgressChanged(this);

    protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();

    public void addProgressChangedListener(ProgressChangedListener listener) {
        listenerList.add(ProgressChangedListener.class, listener);
    }

    public void removeProgressChangedListener(ProgressChangedListener listener) {
        listenerList.remove(ProgressChangedListener.class, listener);
    }

    void raiseProgressChanged(ProgressChanged evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProgressChangedListener.class) {
                ((ProgressChangedListener) listeners[i + 1]).ProgressChanged(evt);
            }
        }
    }

    public AsyncDownloader(String url, String path) {
        super("VKMCAsyncDL:" + url);
        this.url = url;
        this.path = path;
        start();
    }

    @Override
    public void run() {
        try {
            DownloadFile(url, path);
        } catch (MalformedURLException ex) {
            Logger.getLogger(AsyncDownloader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AsyncDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void DownloadFile(String surl, String path) throws MalformedURLException, IOException {
        java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.net.URL(surl).openStream());
        java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
        java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte[] data = new byte[1024];
        int written = 0;
        int x = 0;
        while (((x = in.read(data, 0, 1024)) >= 0) && !canceled) {
            written += 1024;
            progress = CalculatePercentage(written);
            raiseProgressChanged(pc);
            bout.write(data, 0, x);
        }
        bout.close();
        in.close();
        if (canceled) {
            File file = new File(path);
            file.delete();
        }
        completed = true;
        progress = CalculatePercentage(written);
        raiseProgressChanged(pc);
    }

    public int getFileSize() {
        if (size == -1) {
            try {
                URL u = new URL(url);
                URLConnection conn = u.openConnection();
                size = conn.getContentLength();
            } catch (IOException ex) {
                Logger.getLogger(AsyncDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return size;
    }

    private int CalculatePercentage(int x) {
        if (canceled) {
            return -2;
        }
        if (completed) {
            return 100;
        }
        int i = (100 * x) / getFileSize();
        if (i > 99) {
            return 99;
        }
        return i;
    }

    public int getProgress() {
        return progress;
    }

    public void cancelDownload() {
        canceled = true;
    }
}
