package net.sourceforge.scrollrack;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import net.sourceforge.scrollrack.event.DownloadEvent;

public class DownloadThread extends Thread {

    private String url;

    private String filename;

    private EventQueue queue;

    private DownloadEvent event;

    public DownloadThread(String url, String filename, EventQueue queue, DownloadEvent event) {
        this.url = url;
        this.filename = filename;
        this.queue = queue;
        this.event = event;
        start();
    }

    public void run() {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.connect();
            InputStream istream = connection.getInputStream();
            new File(filename).getParentFile().mkdirs();
            FileOutputStream ostream = new FileOutputStream(filename);
            byte[] buffer = new byte[8192];
            int size;
            while ((size = istream.read(buffer)) > 0) ostream.write(buffer, 0, size);
            ostream.close();
            istream.close();
            queue.enqueue(event);
        } catch (Exception exception) {
            event.exception = exception;
            queue.enqueue(event);
        }
    }
}
