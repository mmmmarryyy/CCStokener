package com.net128.beatportapps.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import org.apache.log4j.Logger;

public class DownloadWorker extends Observable implements Runnable {

    private static final Logger log = Logger.getLogger(DownloadWorker.class);

    private static final int MAX_BUFFER_SIZE = 8192;

    private final String tmpExt = ".part";

    public static final String STATUSES[] = { "Waiting", "Downloading", "Paused", "Complete", "Cancelled", "Error" };

    public static final int WAITING = 0;

    public static final int DOWNLOADING = 1;

    public static final int PAUSED = 2;

    public static final int COMPLETE = 3;

    public static final int CANCELLED = 4;

    public static final int ERROR = 5;

    private String downloadId;

    private long size;

    private int downloaded;

    private int status;

    private long time = 0;

    private long started = 0;

    private long stopped = 0;

    private long paused = 0;

    private float progress = 0;

    private int rate = 0;

    private String fileName;

    private String user;

    private String password;

    private String sessionCookies;

    private StatusChangedObserver statusChangedObserver;

    private static long totalDownloaded = 0;

    private static long total = 0;

    public DownloadWorker(String downloadId, String fileName, String user, String password, StatusChangedObserver statusChangedObserver, long size) {
        this.downloadId = downloadId;
        this.size = size;
        downloaded = 0;
        this.fileName = fileName;
        this.user = user;
        this.password = password;
        this.statusChangedObserver = statusChangedObserver;
        if (!new File(fileName).exists()) {
            status = WAITING;
            changeTotal(0, size);
        } else {
            complete();
        }
    }

    public String getFile() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public float getProgress() {
        return progress;
    }

    public int getRate() {
        return rate;
    }

    public int getStatus() {
        return status;
    }

    public long getStarted() {
        return started;
    }

    public void resume() {
        if (status == ERROR || status == COMPLETE) {
            return;
        }
        status = DOWNLOADING;
        if (status == PAUSED) {
            paused += System.currentTimeMillis() - stopped;
        }
        stateChanged();
        download();
    }

    public void cancel() {
        status = CANCELLED;
        stateChanged();
    }

    private void error() {
        status = ERROR;
        stateChanged();
    }

    private void complete() {
        status = COMPLETE;
        stateChanged();
    }

    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public static synchronized void initTotals() {
        total = 0;
        totalDownloaded = 0;
    }

    public static synchronized long getTotal() {
        return total;
    }

    public static synchronized long getToalDownloaded() {
        return totalDownloaded;
    }

    private String getContentAsString(HttpURLConnection connection) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void waitRandom() {
        try {
            Thread.sleep(Math.round((Math.random() * 20000)));
        } catch (Exception e) {
        }
    }

    private String tmpFileName(String fileName) {
        return fileName + tmpExt;
    }

    public void run() {
        if (status == COMPLETE) {
            return;
        }
        waitRandom();
        RandomAccessFile file = null;
        InputStream inputStream = null;
        boolean success = false;
        URL url = null;
        try {
            BeatportAccessor beatportAccessor = new BeatportAccessor();
            if (sessionCookies == null) {
                sessionCookies = beatportAccessor.getSessionCookies(user, password);
            }
            url = new URL(new BeatportAccessor().getTrackDownloadUrl(downloadId, sessionCookies));
            log.info("Open (DownloadWorker.run): " + url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", BeatportAccessor.userAgent);
            connection.setRequestProperty("Cookie", sessionCookies);
            connection.connect();
            if (connection.getResponseCode() / 100 != 2) {
                log.error("Unexpected response from server: " + connection.getResponseCode());
                error();
                return;
            }
            if (connection.getContentType().indexOf("text") >= 0) {
                log.error("Tried to request: " + url.toString());
                log.error("Unexpected file content type from server: " + connection.getContentType());
                log.error("Server replied:\n" + getContentAsString(connection));
                error();
                return;
            }
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                log.error("Invalid content length: " + connection.getContentLength());
                error();
                return;
            }
            if (size == -1) {
                size = contentLength;
                stateChanged();
            } else if (size != contentLength) {
                changeTotal(0, contentLength - size);
            }
            if (new File(tmpFileName(fileName)).exists()) {
                if (!new File(tmpFileName(fileName)).delete()) {
                    throw new Exception("Could not delete file: " + tmpFileName(fileName));
                }
            }
            file = new RandomAccessFile(tmpFileName(fileName), "rw");
            file.seek(downloaded);
            inputStream = connection.getInputStream();
            if (started == 0) {
                started = System.currentTimeMillis();
                time = started;
            }
            statusChangedObserver.statusChanged(status);
            byte buffer[] = new byte[MAX_BUFFER_SIZE];
            while (status == DOWNLOADING) {
                if (size - downloaded < MAX_BUFFER_SIZE) {
                    buffer = new byte[(int) (size - downloaded)];
                }
                int read = inputStream.read(buffer);
                if (read == -1) break;
                file.write(buffer, 0, read);
                downloaded += read;
                changeTotal(read, 0);
                stateChanged();
            }
            if (status == DOWNLOADING) {
                stopped = System.currentTimeMillis();
                status = COMPLETE;
            }
            statusChangedObserver.statusChanged(status);
            stateChanged();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            error();
        } finally {
            try {
                file.close();
            } catch (Exception e) {
            }
            if (success) {
                try {
                    if (!new File(tmpFileName(fileName)).renameTo(new File(fileName))) {
                        throw new Exception("Rename Failed");
                    }
                } catch (Exception e) {
                    log.error("Error moving temp file " + tmpFileName(fileName) + " file: " + fileName, e);
                }
            } else {
                try {
                    new File(tmpFileName(fileName)).delete();
                } catch (Exception e) {
                    log.error("Error deleting erroneous temp file: " + tmpFileName(fileName), e);
                }
            }
            try {
                inputStream.close();
            } catch (Exception e) {
                log.error("Error closing input stream of: " + url + " / " + fileName, e);
            }
        }
    }

    private static synchronized void changeTotal(long dDownloaded, long dTotal) {
        total += dTotal;
        totalDownloaded += dDownloaded;
    }

    private void stateChanged() {
        time = System.currentTimeMillis();
        progress = ((float) downloaded / size) * 100;
        if (started > 0) {
            rate = (int) (downloaded / 1.024 / (time - started - paused));
        }
        setChanged();
        notifyObservers();
        if (status != COMPLETE) {
            statusChangedObserver.statusChanged(status);
        }
    }
}
