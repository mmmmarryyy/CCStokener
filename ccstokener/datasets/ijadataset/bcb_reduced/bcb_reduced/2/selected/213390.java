package org.spamerdos.app;

import java.io.*;
import java.net.*;
import java.util.*;

/**
@author Thomas Cherry jceaser@mac.com
*/
public class Downloader extends Observable implements Runnable {

    private static int objCount;

    private Integer objId;

    private int counter = 0;

    private URL url;

    private List fileList;

    private Thread thread;

    private Integer pauseTime;

    private boolean download;

    /**
	@param url document that is to be downloaded
	*/
    public Downloader(URL url) {
        this(url, null);
    }

    public Downloader(URL url, Observer o) {
        this(url, new Integer(1000), o);
    }

    /**
	@param url document that is to be downloaded
	@param pauseTime number of ms to wait between downloads
	*/
    public Downloader(URL url, Integer pauseTime, Observer o) {
        if (o != null) {
            this.addObserver(o);
        }
        this.objId = new Integer(Downloader.objCount++);
        this.pauseTime = pauseTime;
        this.url = url;
        this.thread = new Thread(this);
        this.download = true;
        this.thread.start();
        System.out.println("Downloader() " + Downloader.objCount + " created with " + url.toString());
    }

    /**
	just to make sure no JIT compiler thinks that the data is not used; Not sure 
	if this is needed.
	@param i data from url
	@return modified data from url
	*/
    protected int fool(int i) {
        return i + i;
    }

    /**
	@return number of downloads
	*/
    public int getCount() {
        return this.counter;
    }

    /**
	unit test
	@param args not used
	*/
    public static void main(String args[]) {
        URL site = null;
        try {
            site = new URL("http://www.spamcop.net/w3m?action=inprogress&type=www");
        } catch (MalformedURLException mue) {
            System.err.println("bad url: " + mue.toString());
        }
        if (site != null) {
            Downloader[] downloader = new Downloader[5];
            for (int y = 0; y < downloader.length; y++) {
                downloader[y] = new Downloader(site, new Integer(y * 1000), null);
                downloader[y].start();
            }
            Thread t = new Thread();
            try {
                t.sleep(9000);
            } catch (java.lang.InterruptedException ie) {
                System.out.println("Thread did not sleep well.");
            }
            for (int y = 0; y < downloader.length; y++) {
                downloader[y].stop();
            }
        }
    }

    /** Thread loop, downloads the url over and over */
    public void run() {
        if (fileList == null) {
            this.fileList = new Vector();
            this.fileList.add(this.url);
            try {
                this.url.openStream().close();
                while (shouldContinue()) {
                    InputStream page = this.url.openStream();
                    while (page.available() > 0) {
                        int i = fool(page.read());
                    }
                    page.close();
                    this.counter++;
                    this.setChanged();
                    this.notifyObservers("count");
                    this.thread.sleep(this.pauseTime.intValue());
                }
            } catch (java.io.IOException ioe) {
                System.err.println("URL does not exist, we may have won.");
            } catch (java.lang.InterruptedException ie) {
                System.out.println("Thread did not sleep well.");
            }
        }
        System.out.println("dead");
    }

    /**
	@return true if downloads should continue
	*/
    public boolean shouldContinue() {
        return this.download;
    }

    /** starts the download thread */
    public void start() {
        this.download = true;
        this.thread.start();
        System.out.println("start");
    }

    /** will stop the thread the next time it wakes up*/
    public void stop() {
        this.download = false;
    }

    public String toString() {
        return "Downloader[id=" + this.objId + ", downloads=" + this.counter + "]";
    }
}
