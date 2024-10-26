package net.dadajax.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * @author dadajax
 *
 */
public class BaseDownloader implements Downloader, Runnable {

    /** Size of file, which is already downloaded. */
    protected long downloadedSize = 0;

    /** Current download speed. */
    protected long currentSpeed;

    /** Status of this download. */
    protected int status;

    /** Path, where will be file downloaded. */
    protected String destination;

    /** File, which represent downloaded content */
    protected File destFile;

    /** URL of external file. */
    protected URL url;

    /** Random access file is only temporary file. */
    protected RandomAccessFile randomAccessFile;

    /** Thread in which this download runs */
    protected Thread downloaderThread;

    /** Link to downloading file */
    protected Link link;

    /** InputStream connected to file URL */
    protected InputStream inputStream;

    /** 
	 * List of download listeners, which are interested
	 * in recieving information about download changes.
	 */
    protected List<DownloadListener> downloadListeners;

    /** This will provide ability to connect to remote file */
    protected HttpURLConnection connection;

    /** This is helper variable and contain count of nanoseconds in one second. */
    protected static final long ONE_SECOND = 1000000000;

    /** Cookie will help to recieve a cookies from web page */
    protected Cookie cookie;

    /** Max size of download buffer. */
    protected static final int MAX_BUFFER_SIZE = 1024;

    /** This object is only lock object for downloadThred */
    protected final Object lock = new Object();

    /** Number of attempts to reconnect */
    protected static final int MAX_ATTEMPTS = 5;

    public BaseDownloader(Link link, String destination) {
        this.link = link;
        try {
            this.url = new URL(link.getUrl());
        } catch (MalformedURLException e) {
            Logger.getRootLogger().error("url cannot be created: " + link.getUrl());
        }
        this.destination = destination;
        if (!destination.endsWith("/")) {
            this.destination = destination + "/";
        }
        this.status = STATUS_WAITING;
        this.currentSpeed = 0;
        downloadListeners = new ArrayList<DownloadListener>();
        String fileName = getFileNameFromURL(url);
        destFile = new File(this.destination + fileName);
        downloadedSize = getDownloadedSize();
        downloaderThread = new Thread(this);
        initConnection();
        if (getSize() <= 0) {
            long size = getFileSize();
            link.setSize(size);
        }
        downloaderThread.setName("T-" + fileName);
    }

    @Override
    public void addDownloadListener(DownloadListener listener) {
        downloadListeners.add(listener);
    }

    @Override
    public long getCurrentDownloadSpeed() {
        return currentSpeed;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    @Override
    public long getDownloadedSize() {
        if (destFile != null) {
            return destFile.length();
        }
        return 0;
    }

    @Override
    public File getFile() {
        return destFile;
    }

    @Override
    public long getSize() {
        return link.getSize();
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void pause() {
        status = Downloader.STATUS_PAUSED;
    }

    @Override
    public void removeDownloadListener(DownloadListener listener) {
        downloadListeners.remove(listener);
        Logger.getRootLogger().debug("download listener removed");
    }

    @Override
    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public void setStatus(int status) {
        if (getSize() != getDownloadedSize()) {
            this.status = status;
        } else {
            this.status = STATUS_DOWNLOADED;
        }
    }

    @Override
    public synchronized void start() {
        try {
            if (getDownloadedSize() == getSize()) {
                status = STATUS_DOWNLOADED;
                Logger.getRootLogger().debug("Downloader " + url.toString() + " was set to STATUS_DOWNLOADED");
            }
            if (downloaderThread.getState() == Thread.State.NEW) {
                Logger.getRootLogger().debug("Thread.State.NEW");
                downloaderThread.start();
            } else if (downloaderThread.getState() == Thread.State.WAITING) {
                Logger.getRootLogger().debug("Thread.State.WAITING");
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
            status = STATUS_RUNNING;
            Logger.getRootLogger().debug("Downloader " + url.toString() + " was set to STATUS_RUNNING");
        } catch (Exception e) {
            Logger.getRootLogger().error("task start error", e);
            status = STATUS_ERROR;
            notifyStatusChanged();
        }
    }

    /**
	 * Notify all registered download listeners about
	 * speed change.
	 */
    protected synchronized void notifySpeedChanged() {
        DownloadListener[] listeners = downloadListeners.toArray(new DownloadListener[downloadListeners.size()]);
        for (DownloadListener listener : listeners) {
            listener.speedChanged(this);
        }
    }

    /**
	 * Notify all registered download listeners about
	 * change in downloaded size. It is called always, when
	 * some next data are downloaded.
	 */
    protected synchronized void notifySizeChanged() {
        DownloadListener[] listeners = downloadListeners.toArray(new DownloadListener[downloadListeners.size()]);
        for (DownloadListener listener : listeners) {
            listener.downloadedSizeChanged(this);
        }
    }

    /**
	 * Notify all registered download listeners about
	 * change in downloader status.
	 */
    protected synchronized void notifyStatusChanged() {
        DownloadListener[] listeners = downloadListeners.toArray(new DownloadListener[downloadListeners.size()]);
        for (DownloadListener listener : listeners) {
            if (status == STATUS_DOWNLOADED || status == STATUS_PAUSED) {
                currentSpeed = 0;
            }
            listener.statusChanged(this);
        }
    }

    /**
	 * This method should try to create new connection and continue from
	 * last downloaded byte.
	 */
    protected synchronized void reconnect() {
        try {
            Logger.getRootLogger().debug("Trying to reconnect");
            initConnection();
            inputStream = connection.getInputStream();
        } catch (Exception e) {
            Logger.getRootLogger().error("cannot connect to url " + url.toString(), e);
        }
    }

    /**
	 * Init connection to url.
	 */
    protected void initConnection() {
        connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            prepareConnectionBeforeConnect();
            connection.connect();
        } catch (IOException e) {
            status = STATUS_ERROR;
            Logger.getRootLogger().error("problem in connection", e);
        }
    }

    /**
	 * This method is called from inside of method initConnection(). Here should
	 * be code, which is called after connecion is opened by url.openConnection() and
	 * before connection.connect(); 
	 */
    protected void prepareConnectionBeforeConnect() {
    }

    /**
	 * We must get a file name from URL.
	 * @param url url of downloading file
	 * @return filename. If url doesn't contain filename,
	 * 		return null object.
	 */
    protected String getFileNameFromURL(URL url) {
        String string = url.toString();
        if (string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        int slashPos = string.lastIndexOf("/");
        if (slashPos == 1 || string == "" || string == null) return null;
        string = string.substring(slashPos + 1, string.length());
        return string;
    }

    /**
	 * For internal purposes. Try to connect to remote file and
	 * get information about file size. If there is some problem
	 * return zero value.
	 * @return Size of file or zero if file doesn't exist.
	 */
    protected long getFileSize() {
        if (link.getSize() > 0) {
            return link.getSize();
        } else if (connection == null) {
            return 0;
        } else {
            Logger.getRootLogger().debug("reading file size from connection");
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                Logger.getRootLogger().warn("file doesn't exist " + url.toString());
                contentLength = 0;
            }
            return contentLength;
        }
    }

    @Override
    public void run() {
        notifyStatusChanged();
        Logger.getRootLogger().debug("start downloading " + url.toString());
        randomAccessFile = null;
        inputStream = null;
        try {
            File helpFile = new File(destination);
            helpFile.mkdirs();
            randomAccessFile = new RandomAccessFile(destFile, "rw");
            randomAccessFile.seek(downloadedSize);
            inputStream = connection.getInputStream();
            long startTime = System.nanoTime();
            long now = System.nanoTime();
            float deltaTime = 0.0f;
            int timeData = 0;
            int attempt = 0;
            long size = connection.getContentLength();
            if (size > 0 && size != getSize()) {
                link.setSize(size);
            }
            while (status != STATUS_DOWNLOADED && status != STATUS_ERROR) {
                if (status == STATUS_PAUSED || status == STATUS_WAITING) {
                    synchronized (lock) {
                        lock.wait();
                    }
                } else if (status == STATUS_RUNNING) {
                    byte[] buffer;
                    if (getSize() - downloadedSize > MAX_BUFFER_SIZE) {
                        buffer = new byte[MAX_BUFFER_SIZE];
                    } else {
                        buffer = new byte[(int) (getSize() - downloadedSize)];
                    }
                    int data = 0;
                    try {
                        data = inputStream.read(buffer);
                    } catch (Exception e) {
                        Logger.getRootLogger().error("inputstream.read failed", e);
                        if (attempt < MAX_ATTEMPTS) {
                            reconnect();
                            attempt++;
                            continue;
                        } else {
                            break;
                        }
                    }
                    if (data == -1) {
                        if (attempt < MAX_ATTEMPTS) {
                            reconnect();
                            attempt++;
                            continue;
                        } else {
                            break;
                        }
                    }
                    attempt = 0;
                    randomAccessFile.write(buffer, 0, data);
                    timeData += data;
                    now = System.nanoTime();
                    if (startTime + ONE_SECOND < now) {
                        now = System.nanoTime();
                        deltaTime = (float) (now - startTime);
                        currentSpeed = (int) ((float) timeData * (ONE_SECOND / deltaTime));
                        startTime = System.nanoTime();
                        timeData = 0;
                        notifySpeedChanged();
                    }
                    downloadedSize += data;
                    if (getSize() == getDownloadedSize()) {
                        status = STATUS_DOWNLOADED;
                    }
                    notifySizeChanged();
                }
            }
        } catch (Exception e) {
            status = STATUS_ERROR;
            Logger.getRootLogger().error("error in downloader", e);
        } finally {
            if (randomAccessFile != null) try {
                randomAccessFile.close();
            } catch (IOException e) {
                Logger.getRootLogger().error("cannot close file", e);
            }
            if (inputStream != null) try {
                inputStream.close();
            } catch (IOException e) {
                Logger.getRootLogger().error("cannot close inputStream", e);
            }
            if (status == STATUS_RUNNING) {
                status = STATUS_ERROR;
            }
            notifyStatusChanged();
        }
    }
}
