package org.openfrag.OpenCDS.core.download;

import org.openfrag.OpenCDS.core.init.ShutdownListener;
import org.openfrag.OpenCDS.core.logging.Logger;
import org.openfrag.OpenCDS.core.init.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.openfrag.OpenCDS.core.exceptions.DownloadException;
import org.openfrag.OpenCDS.core.exceptions.InvalidActionException;

/**
 * The DownloadManager handles all downloads that have been given. It downloads
 *  the files in a seperate thread so you can continue doing your stuff.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class DownloadManager {

    /** The download is still busy downloading. */
    public static final int STATE_DOWNLOADING = 0;

    /** The download is queued and waiting for download. */
    public static final int STATE_QUEUED = 1;

    /** The download is added to the system, but is not queued, thus not being
         downloaded as of this moment. */
    public static final int STATE_IDLE = 2;

    /** The download has completed and is about to be removed from the system */
    public static final int STATE_DONE = 3;

    /** The download has failed. Either we couldn't connect to the address, we
         couldn't log in when it is an FTP server, or any additional checks
         failed. */
    public static final int STATE_FAILED = 4;

    private DownloadQueue m_Queue;

    private List<Download> m_DownloadList = new ArrayList<Download>();

    private List<DownloadManagerListener> m_Listeners = new ArrayList<DownloadManagerListener>();

    boolean m_QueueRunning = false;

    boolean m_QueueStop = false;

    /**
     * The DownloadManager constructor.
    */
    public DownloadManager() {
        m_Queue = new DownloadQueue();
        ShutdownManager.getInstance().addShutdownListener(new ShutdownListener() {

            public void onShutdown() {
                for (Iterator<Download> it = m_DownloadList.iterator(); it.hasNext(); ) {
                    Download dl = (Download) it.next();
                    if (dl.getState() == STATE_DOWNLOADING) {
                        dl.setState(STATE_FAILED);
                        fireDownloadStateChanged(dl, STATE_DOWNLOADING, STATE_FAILED);
                    }
                }
            }
        });
    }

    /**
     * Adds a download to the system. The download is added with the state IDLE
     *  and can be downloaded via <i>queueDownload</i>.
     *
     * @param   address     The address of the download.
     * @param   toFile      The destination of the download.
     * \return  The download created.
    */
    public Download addDownload(String address, String toFile) throws InvalidActionException {
        if (address == null || toFile == null) {
            throw new InvalidActionException("The download does not contain" + "an address or an destination.");
        }
        Download dl = new Download(address, toFile, STATE_IDLE);
        m_DownloadList.add(dl);
        fireDownloadAdded(dl);
        return dl;
    }

    /**
     * Adds a download to the system. The download is added with the state IDLE
     *  and can be download via <i>queueDownload</i>. If the state of the given
     *  download is not IDLE, it will be set to IDLE, therefore two events will
     *  be fired when you have a listener: downloadAdded, and 
     *  downloadStateChanged.
     *
     * @param   download    The download to add.
    */
    public void addDownload(Download download) throws InvalidActionException {
        int oldState = -1;
        if (download.getState() != STATE_IDLE) {
            oldState = download.getState();
            download.setState(STATE_IDLE);
        }
        if (download.getAddress() == null || download.getToFile() == null) {
            throw new InvalidActionException("The download does not contain" + "an address or an destination.");
        }
        m_DownloadList.add(download);
        fireDownloadAdded(download);
        if (oldState != -1) {
            fireDownloadStateChanged(download, oldState, STATE_IDLE);
        }
    }

    /**
     * Remove an download.
     *
     * @param   download    The download to remove.
     * @return  True if successful, false if not.
    */
    public boolean removeDownload(Download download) {
        boolean ret = m_DownloadList.remove(download);
        fireDownloadRemoved(download);
        return ret;
    }

    /**
     * Remove an download.
     *
     * @param   index   The index of the download.
     * @return  True if successful, false if not.
    */
    public boolean removeDownload(int index) {
        Download dl = m_DownloadList.get(index);
        boolean ret = m_DownloadList.remove(dl);
        fireDownloadRemoved(dl);
        return ret;
    }

    /**
     * Get a download.
     *
     * @param   index   The index of the download.
     * @return  Reference to the download.
    */
    public Download getDownload(int index) throws InvalidActionException {
        Download dl = null;
        try {
            dl = m_DownloadList.get(index);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            throw new InvalidActionException(e.getMessage());
        } finally {
            return dl;
        }
    }

    /**
     * Get a download.
     *
     * @param   address     The address of the download to get.
     * @param   toFile      The destination of the download to get.
     * @return  Reference to the download, otherwise null.
    */
    public Download getDownload(String address, String toFile) {
        for (Iterator<Download> it = m_DownloadList.iterator(); it.hasNext(); ) {
            Download dl = (Download) it.next();
            if (dl.getAddress().equals(address) && dl.getToFile().equals(toFile)) {
                return dl;
            }
        }
        return null;
    }

    /**
     * Checks whether this download is present in the system or not.
     *
     * @param   dl    The download to check.
     * @return  True if so, false if not.
    */
    public boolean isDownloadPresent(Download dl) {
        return m_DownloadList.contains(dl);
    }

    /**
     * Enqueue a download. Creates one if it does not exist. This method
     *  differs from <i>addDownload</i> (only if the download does not exist
     *  within the system) that it sets the state to QUEUE, and so that it will 
     *  be downloaded as soon as possible. Same as for <i>addDownload</i>, the
     *  events fired are: downloadAdded and downloadStateChanged, as this will
     *  set the state to QUEUED, whatever state it was in.
     *
     * @param   dl    The download enqueue.
     * @return  True if successful, false if not.
    */
    public boolean enqueueDownload(Download dl) {
        if (m_QueueRunning) {
            throw new InvalidActionException("You cannot edit the queue" + " while it is running. Use stopQueue first.");
        }
        if (!isDownloadPresent(dl)) {
            if (dl.getAddress() != null && dl.getToFile() != null) {
                m_DownloadList.add(dl);
                m_Queue.addDownload(dl);
                int oldState = dl.getState();
                dl.setState(STATE_QUEUED);
                fireDownloadAdded(dl);
                fireDownloadStateChanged(dl, oldState, STATE_QUEUED);
                return true;
            } else {
                return false;
            }
        } else {
            m_Queue.addDownload(dl);
            int oldState = dl.getState();
            if (oldState != STATE_QUEUED) {
                dl.setState(STATE_QUEUED);
                fireDownloadStateChanged(dl, oldState, STATE_QUEUED);
            }
            return true;
        }
    }

    /**
     * Dequeues a download. Dequeues means that it's removed from the download
     *  queue and then set back to IDLE.
     *
     * @param   dl    The download to dequeue.
     * @return  True if successful, false if not.
    */
    public boolean dequeueDownload(Download dl) throws InvalidActionException {
        if (dl.getState() != STATE_DOWNLOADING) {
            if (m_QueueRunning) {
                throw new InvalidActionException("You cannot edit the queue" + " while it is running. Use stopQueue first.");
            }
            m_Queue.removeDownload(dl);
            dl.setState(STATE_IDLE);
            fireDownloadStateChanged(dl, STATE_QUEUED, STATE_IDLE);
        } else {
            throw new InvalidActionException("The file that you try to remove" + " is in progress.");
        }
        return true;
    }

    /**
     * Starts the queue, so all the downloads in the queue are started once
     *  at a time.
    */
    public void startQueue() throws InvalidActionException {
        if (m_QueueRunning) {
            throw new InvalidActionException("The queue is already running.");
        }
        m_QueueStop = false;
        m_QueueRunning = true;
        Thread thread = new Thread(new Runnable() {

            public void run() {
                Download dl = null;
                while (m_Queue.getNext() != null) {
                    if (m_QueueStop) {
                        break;
                    }
                    dl = m_Queue.retrieveNext();
                    if (dl.getAddress().startsWith("http") || dl.getAddress().startsWith("https")) {
                        downloadHTTP(dl);
                    } else if (dl.getAddress().startsWith("ftp")) {
                        downloadFTP(dl);
                    }
                }
            }
        });
        thread.start();
    }

    /**
     * Downloads the next in line, from HTTP.
     *
     * @param   dl  The download to retrieve.
    */
    private void downloadHTTP(Download dl) {
        dl.setState(STATE_DOWNLOADING);
        fireDownloadStateChanged(dl, STATE_QUEUED, STATE_DOWNLOADING);
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(dl.getAddress());
            File file = new File(dl.getToFile());
            if (!file.exists()) {
                if (file.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.createNewFile();
                }
            }
            out = new BufferedOutputStream(new FileOutputStream(file));
            conn = url.openConnection();
            double fileSize = conn.getContentLength();
            dl.setFileSize((fileSize * 1024) * 1024);
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                dl.setProgress(numWritten);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        dl.setState(STATE_DONE);
        fireDownloadStateChanged(dl, STATE_DOWNLOADING, STATE_DONE);
        m_Queue.removeDownload(dl);
        removeDownload(dl);
    }

    /**
     * Download the next in line, from FTP.
     *
     * @param   dl  The file to download.
    */
    private void downloadFTP(Download dl) throws DownloadException {
        try {
            String address = dl.getAddress();
            String[] parts = address.split("/");
            String host = parts[2];
            int dirStart = address.indexOf(host);
            String dirName = address.substring(dirStart + host.length(), address.length());
            int dirWhFile = dirName.lastIndexOf("/");
            String dirWithoutFile = dirName.substring(0, dirWhFile);
            String fileName = dirName.substring(dirWhFile + 1, dirName.length());
            FTP ftp = new FTP(dl);
            ftp.open(host);
            if (!ftp.login("anonymous", "openfrag@dummy.org")) {
                dl.setState(STATE_FAILED);
                fireDownloadStateChanged(dl, STATE_DOWNLOADING, STATE_FAILED);
                throw new DownloadException("FTP: Anonymous connection is" + " not allowed.", dl);
            }
            if (dirWithoutFile.length() != 0) {
                ftp.cd(dirWithoutFile);
            }
            dl.setFileSize(ftp.size(fileName));
            ftp.get(fileName, new File(dl.getToFile()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the queue. If theres a download currently in progress, it is
     *  finished first.
    */
    public void stopQueue() throws InvalidActionException {
        if (m_QueueRunning) {
            m_QueueStop = true;
            m_QueueRunning = false;
        } else {
            throw new InvalidActionException("The queue is not running.");
        }
    }

    /**
     * Clears the queue. If there queue was running, an exception is thrown.
    */
    public void clearQueue() throws InvalidActionException {
        if (!m_QueueRunning) {
            m_Queue.removeAllDownloads();
        } else {
            throw new InvalidActionException("The queue was not running.");
        }
    }

    /**
     * Adds a listener.
     *
     * @param   listener    The listener.
    */
    public void addListener(DownloadManagerListener listener) {
        m_Listeners.add(listener);
    }

    /**
     * Removes a listeners.
     *
     * @param   listener    The listener to remove.
    */
    public void removeListener(DownloadManagerListener listener) {
        m_Listeners.remove(listener);
    }

    /**
     * Fire an downloadAdded event.
     *
     * @param   download    The download that has been added.
    */
    private void fireDownloadAdded(Download download) {
        for (int i = 0; i < m_Listeners.size(); i++) {
            DownloadManagerListener listener = (DownloadManagerListener) m_Listeners.get(i);
            listener.downloadAdded(download);
        }
    }

    /**
     * Fire an downloadRemoved event.
     *
     * @param   download    The download that has been removed.
    */
    private void fireDownloadRemoved(Download download) {
        for (int i = 0; i < m_Listeners.size(); i++) {
            DownloadManagerListener listener = (DownloadManagerListener) m_Listeners.get(i);
            listener.downloadRemoved(download);
        }
    }

    /**
     * Fire an downloadStateChanged event.
     *
     * @param   download    The download that has been added.
     * @param   oldState    The old state of the download.
     * @param   newState    The new state of the download.
    */
    private void fireDownloadStateChanged(Download download, int oldState, int newState) {
        for (int i = 0; i < m_Listeners.size(); i++) {
            DownloadManagerListener listener = (DownloadManagerListener) m_Listeners.get(i);
            listener.downloadStateChanged(download, oldState, newState);
        }
    }

    /**
     * Fire an downloadCompleted event.
     *
     * @param   download    The download that has been completed.
    */
    private void fireDownloadCompleted(Download download) {
        for (int i = 0; i < m_Listeners.size(); i++) {
            DownloadManagerListener listener = (DownloadManagerListener) m_Listeners.get(i);
            listener.downloadDone(download);
        }
    }
}
