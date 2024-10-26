package net.sf.gimme;

import net.sf.gimme.event.Mediator;
import net.sf.gimme.event.SegmentFinished;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author aiden
 */
public class Segment implements Runnable {

    private Download download;

    private URL url;

    private int index;

    private long offset = 0;

    private File tempFile;

    private boolean paused;

    private long speed = 0;

    private FTPClient ftp = new FTPClient();

    public boolean isFinished() {
        return finished;
    }

    private boolean finished;

    public Segment(Download download, int index) {
        this.download = download;
        this.url = download.getUrl();
        this.index = index;
        init();
    }

    public Segment(Download download, URL url, int index) {
        this.download = download;
        this.url = url;
        this.index = index;
        init();
    }

    private void init() {
        tempFile = new File(download.getTempFolder(), download.getName() + "." + index);
        offset = (long) Math.ceil((float) download.getTotalSize() / (float) download.getSegments().length) * index;
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private InputStream urlToInputStream(URL url) throws IOException {
        InputStream is = url.openStream();
        if (url.getProtocol().toLowerCase().equals("http")) {
            try {
                HttpClient http = new HttpClient();
                HttpMethod method = new GetMethod(url.toString());
                http.executeMethod(method);
                is = method.getResponseBodyAsStream();
                if (is == null) {
                    throw new NullPointerException();
                }
            } catch (Exception ex) {
                Logger.post(Logger.Level.WARNING, "Use of Jakarta HttpClient failed. Trying another solution...");
            }
        } else if (url.getProtocol().toLowerCase().equals("ftp")) {
        }
        return is;
    }

    public void run() {
        Segment[] segs = download.getSegments();
        while (segs[segs.length - 1] == null) {
            Thread.yield();
        }
        try {
            long downloaded = tempFile.length();
            RandomAccessFile rw = new RandomAccessFile(tempFile, "rw");
            InputStream in = urlToInputStream(url);
            long limit = offset;
            if (index + 1 < segs.length) {
                limit = segs[index + 1].getOffset();
            } else {
                limit = download.getTotalSize();
            }
            rw.seek(downloaded);
            download.incrementSize(downloaded);
            long pos = offset + downloaded;
            long skipped = 0;
            while (skipped < pos) {
                skipped += in.skip(pos - skipped);
            }
            long time = System.currentTimeMillis();
            long oldPos = pos;
            final int CHUNK_SIZE = 16384;
            while (pos < limit) {
                byte buffer[];
                if (limit - pos > CHUNK_SIZE) {
                    buffer = new byte[CHUNK_SIZE];
                } else {
                    buffer = new byte[(int) (limit - pos)];
                }
                int read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                rw.write(buffer, 0, read);
                pos += read;
                download.incrementSize(read);
                if (System.currentTimeMillis() - time >= 1000) {
                    speed = pos - oldPos;
                    oldPos = pos;
                    time = System.currentTimeMillis();
                }
                while (paused) {
                    Thread.yield();
                }
            }
            rw.close();
            in.close();
            if (ftp.isConnected()) {
                ftp.disconnect();
            }
            finished = true;
            Mediator.post(new SegmentFinished(this));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public long getOffset() {
        return offset;
    }

    public File getTempFile() {
        return tempFile;
    }

    void pause() {
        paused = true;
    }

    void resume() {
        paused = false;
    }

    public long getSpeed() {
        return speed;
    }
}
