package com.warserver;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This can Download software. It has several static methods that make this easy.<br>
 * Look at the public static void startDownload(Software software) method.<br>
 * Simply instantiate a Software object and call Download.startDownload(software).<br>
 * <p>
 * Makes use of the com.warserver.ThreadPool to run this object.
 * 
 *
 * @see com.warserver.DownloadListener
 * @see com.warserver.Software
 * @see com.warserver.ThreadPool
 *
 * @author  Kurt Olsen
 * @version 1.0
 */
public class Downloader implements Runnable {

    private boolean cancelled;

    private InputStream inputStream;

    private OutputStream outputStream;

    private Software software;

    public static String HKEY = "com.warserver.Downloader.downloads";

    /**
     * Creates new Downloader
     */
    protected Downloader() {
    }

    /**
     * Convienence Constructor
     */
    public Downloader(Software software) {
        this.software = software;
    }

    /** Getter for property cancelled.
     * @return Value of property cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /** Setter for property cancelled.
     * @param cancelled New value of property cancelled.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Begins downloading the software
     */
    public void run() {
        if (software == null) return;
        Jvm.hashtable(HKEY).put(software, this);
        try {
            software.setException(null);
            software.setDownloaded(false);
            software.setDownloadStartTime(System.currentTimeMillis());
            try {
                software.downloadStarted();
            } catch (Exception dsx) {
            }
            if (software.getDownloadDir() == null) {
                software.setException(new Exception("The DownloadDir is null."));
                software.setDownloadStartTime(0);
                software.setDownloaded(false);
                throw software.getException();
            }
            URL url = new URL(software.getURL());
            URLConnection con = url.openConnection();
            software.setDownloadLength(con.getContentLength());
            inputStream = con.getInputStream();
            File file = new File(software.getDownloadDir(), software.getURLFilename());
            outputStream = new FileOutputStream(file);
            int totalBytes = 0;
            byte[] buffer = new byte[8192];
            while (!cancelled) {
                int bytesRead = Jvm.copyPartialStream(inputStream, outputStream, buffer);
                if (bytesRead == -1) break;
                totalBytes += bytesRead;
                try {
                    software.downloadProgress(totalBytes);
                } catch (Exception dx) {
                }
            }
            if (!cancelled) software.setDownloaded(true);
        } catch (Exception x) {
            software.setException(x);
            software.setDownloadStartTime(0);
            software.setDownloaded(false);
        }
        try {
            software.downloadComplete();
        } catch (Exception dcx) {
        }
        Jvm.hashtable(HKEY).remove(software);
        closeStreams();
    }

    /**
     * Tries to close the input and output streams
     */
    public void closeStreams() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException iox1) {
        }
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException iox2) {
        }
    }

    /** 
     * Begins downloading the given Software object. Calling this method twice on a given
     * piece of Software while it is already downloading is a mistake so don't do it 
     * until the first one has finished!
     *
     * <p>Each time you call this method, a Downloader (which implements Runnable) is created
     * that actully does the download for the Software. The Downloader is first placed in
     * the downloads Hashtable (in the Jvm cache under the key Download.HKEY) 
     * using the Software as its key so that a subsequent call to <code>stopDownload</code> 
     * can lookup the Software (or all of them) and stop the download.
     * 
     * <p>Finally, a thread from the given ThreadPool is assigned to run the Downloader at which time
     * the downloading actually begins. 
     *
     * @param software the software you want downloaded
     * @param threads the threadpool this task is assigned to.
     * @version 1.0 1.0
     * @author Kurt Olsen
     */
    public static void startDownload(Software software, ThreadPool threads) {
        Downloader task = new Downloader(software);
        threads.addRequest(task);
    }

    /**
     * Uses <code>startDownload(software, threads)</code> to download the software using the
     * default threadpool found by using <code>Jvm.threadpool("/warserver/threads")</code>.
     *
     * @param software the software you want downloaded
     */
    public static void startDownload(Software software) {
        startDownload(software, Jvm.threadpool("/warserver/threads"));
    }

    /** 
     * Uses <code>startDownload(Software)</code> to start downloading one or more Software objects.
     * The groupKey is used to enumerate Software objects found in the Jvm cache using the key
     * in the form <code>Jvm.elements(groupKey)</code>.
     */
    public static void startDownload(String groupKey) {
        for (Enumeration e = Jvm.elements(groupKey); e.hasMoreElements(); ) startDownload((Software) e.nextElement());
    }

    /**
     * Stop all downloads that are in progress
     */
    public static void stopAllDownloads() {
        for (Enumeration e = Jvm.elements(Downloader.HKEY); e.hasMoreElements(); ) ((Downloader) e.nextElement()).setCancelled(true);
    }

    /**
     * Stops downloading the given software
     */
    public static void stopDownload(Software software) {
        Downloader downloader = (Downloader) Jvm.hashtable(Downloader.HKEY).get(software);
        if (downloader != null) downloader.setCancelled(true);
    }
}
