package org.dmpotter.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.dmpotter.util.event.DownloadEvent;
import org.dmpotter.util.event.DownloadListener;

/**
 * Downloads a file, sending update events to any listeners.
 * @author dmpotter
 * @version $Revision: 1.3 $
 */
public class Downloader {

    private URL url;

    private File downloadFile;

    private DownloadListener listeners[];

    public Downloader(URL url, File saveTo) {
        this.url = url;
        downloadFile = saveTo;
        listeners = new DownloadListener[0];
    }

    public void addDownloadListener(DownloadListener listener) {
        DownloadListener dl[] = new DownloadListener[listeners.length + 1];
        if (listeners.length > 0) System.arraycopy(listeners, 0, dl, 0, listeners.length);
        dl[listeners.length] = listener;
        listeners = dl;
    }

    private void fireDownloadUpdated(DownloadEvent d) {
        for (int i = 0; i < listeners.length; i++) listeners[i].downloadUpdated(d);
    }

    private void fireDownloadCompleted(DownloadEvent d) {
        for (int i = 0; i < listeners.length; i++) listeners[i].downloadCompleted(d);
    }

    private void fireDownloadAborted(DownloadEvent d, Throwable t) {
        for (int i = 0; i < listeners.length; i++) listeners[i].downloadAborted(d, t);
    }

    public void startDownload() {
        new DownloadThread();
    }

    private class DownloadThread implements Runnable {

        public DownloadThread() {
            new Thread(this).start();
        }

        public void run() {
            int contentLength = 0, downloaded = 0;
            try {
                URLConnection conn;
                conn = url.openConnection();
                conn.connect();
                contentLength = conn.getContentLength();
                InputStream in = conn.getInputStream();
                int down = 0;
                byte buf[] = new byte[4096];
                FileOutputStream out = new FileOutputStream(downloadFile);
                downloaded = 0;
                while (true) {
                    down = in.read(buf);
                    if (down == -1) break;
                    downloaded += down;
                    out.write(buf, 0, down);
                    fireDownloadUpdated(new DownloadEvent(downloaded, contentLength));
                }
                in.close();
                out.close();
                contentLength = downloaded;
                fireDownloadCompleted(new DownloadEvent(downloaded, contentLength));
            } catch (IOException ioe) {
                fireDownloadAborted(new DownloadEvent(downloaded, contentLength), ioe);
            }
        }
    }
}
