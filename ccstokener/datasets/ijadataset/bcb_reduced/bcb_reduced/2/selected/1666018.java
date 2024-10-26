package net.sf.gimme;

import net.sf.gimme.event.DownloadSizeChanged;
import net.sf.gimme.event.Event;
import net.sf.gimme.event.EventListener;
import net.sf.gimme.event.Mediator;
import net.sf.gimme.event.SegmentFinished;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import net.sf.gimme.event.DownloadStatusChanged;

/**
 *
 * @author aiden
 */
public class Download implements EventListener, Runnable {

    private File tempFolder;

    private URL url;

    private String name = "";

    private int size, totalSize;

    private Segment[] segments;

    private File dest;

    boolean finished = false;

    private String status = "";

    public Download(URL url, int segs) {
        this.url = url;
        Mediator.register(this);
        status = "Starting...";
        try {
            totalSize = url.openConnection().getContentLength();
            name = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
            if (name.isEmpty()) {
                name = "UNKNOWN";
            }
            tempFolder = new File(Configuration.PARTS_FOLDER, getName());
            tempFolder.mkdir();
        } catch (IOException ex) {
            Logger.post(Logger.Level.WARNING, "URL could not be opened: " + url);
        }
        dest = new File(System.getProperty("user.home") + File.separator + name);
        if (segs > totalSize) {
            segs = totalSize;
        }
        Properties props = new Properties();
        props.setProperty("url", getUrl().toString());
        props.setProperty("segments", String.valueOf(segs));
        try {
            props.storeToXML(new FileOutputStream(new File(getTempFolder(), "index.xml")), "Warning: Editing this file may compromise the integrity of the download");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        segments = new Segment[segs];
        for (int i = 0; i < segs; i++) {
            segments[i] = new Segment(this, i);
        }
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        status = "Downloading...";
        Mediator.post(new DownloadStatusChanged(this));
        Logger.post(Logger.Level.INFO, "Starting download: " + getName());
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public URL getUrl() {
        return url;
    }

    public Segment[] getSegments() {
        return segments;
    }

    public void notify(Event event) {
        if (event instanceof SegmentFinished) {
            boolean done = true;
            for (Segment seg : segments) {
                if (seg.isFinished() == false) {
                    done = false;
                }
            }
            if (done) {
                finish();
            }
        }
    }

    public long getSpeed() {
        long speed = 0;
        for (Segment seg : segments) {
            speed += seg.getSpeed();
        }
        return speed;
    }

    public String getStatus() {
        return status;
    }

    private void finish() {
        status = "Finishing...";
        Mediator.post(new DownloadStatusChanged(this));
        finished = true;
        if (dest.exists()) {
            Logger.post(Logger.Level.WARNING, "File already exists: " + getName());
        } else {
            try {
                FileOutputStream out = new FileOutputStream(dest);
                for (Segment seg : segments) {
                    FileInputStream in = new FileInputStream(seg.getTempFile());
                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    out.flush();
                    in.close();
                    if (!seg.getTempFile().delete()) {
                        Logger.post(Logger.Level.WARNING, "Could not delete temp file: " + seg.getTempFile());
                    }
                }
                out.close();
                status = "Done";
                Mediator.post(new DownloadStatusChanged(this));
                Logger.post(Logger.Level.INFO, "Finished download: " + getName());
            } catch (IOException ex) {
                Logger.post(Logger.Level.SEVERE, ex.getMessage());
            }
        }
        removeTempFolder();
    }

    public void run() {
        while (!finished) {
            Thread.yield();
        }
    }

    void incrementSize() {
        size++;
        Mediator.post(new DownloadSizeChanged(this));
    }

    void incrementSize(long amount) {
        size += amount;
        Mediator.post(new DownloadSizeChanged(this));
    }

    public void pause() {
        if (!finished) {
            status = "Paused";
            Mediator.post(new DownloadStatusChanged(this));
            for (Segment seg : segments) {
                seg.pause();
            }
        }
    }

    public void resume() {
        if (!finished) {
            status = "Downloading...";
            Mediator.post(new DownloadStatusChanged(this));
            for (Segment seg : segments) {
                seg.resume();
            }
        }
    }

    public File getTempFolder() {
        return tempFolder;
    }

    void removeTempFolder() {
        if (getTempFolder().isDirectory()) {
            for (File f : getTempFolder().listFiles()) {
                f.delete();
            }
            getTempFolder().delete();
        }
    }
}
