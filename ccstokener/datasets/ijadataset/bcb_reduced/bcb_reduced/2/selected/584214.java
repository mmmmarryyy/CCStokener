package de.innot.avreclipse.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import de.innot.avreclipse.AVRPlugin;

/**
 * Class to download files.
 * <p>
 * To download a file use the {@link #download(URL, IProgressMonitor)} method, which returns a
 * <code>java.io.File</code> object.
 * </p>
 * <p>
 * This Class is thread safe and can be called multiple times. Callers can check if the download of
 * an URL is already in progress with the {@link #isDownloading(URL)} method
 * </p>
 * <p>
 * The URLDownloadManager maintains a cache of all downloaded files. Files having been downloaded
 * before are taken from the cache. The current cache mechanism is simplistic and does not account
 * for the same file from different URL sources.
 * </p>
 * <p>
 * The cache is created in the Plugin storage area, usually at
 * {Workplace_loc}/.metadata/.plugins/de.innot.avreclipse.core/cache
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class URLDownloadManager {

    private static final IPath CACHEPATH = new Path("downloads");

    private static DownloadRateCalculator fDRC = new DownloadRateCalculator();

    private static final int EOF = -1;

    private static final List<URL> fCurrentDownloads = new ArrayList<URL>();

    /**
	 * Test if a download of an URL file is already in progress.
	 * <p>
	 * This can be used by the caller to inhibit multiple downloads of the same file by nervous
	 * users.
	 * </p>
	 * 
	 * @param url
	 * @return <code>true</code> if the download in already in progress by some other thread.
	 */
    public static boolean isDownloading(URL url) {
        boolean result = false;
        synchronized (fCurrentDownloads) {
            result = fCurrentDownloads.contains(url);
        }
        return result;
    }

    /**
	 * Use the given {@link DownloadRateCalculator} to calculate the download rate of the next and
	 * all following downloads.
	 * 
	 * @param dac
	 *            A new rate calculator with a superclass of <code>DownloadRateCalculator</code>
	 */
    public static void setDownloadRateCalculator(final DownloadRateCalculator dac) {
        Assert.isNotNull(dac);
        fDRC = dac;
    }

    /**
	 * Delete all files from the cache.
	 * <p>
	 * This method will block until all downloads currently in progress are finished.
	 * </p>
	 * 
	 * @return <code>true</code> is all files were deleted from the cache, <code>false</code> if
	 *         some files could not be deleted.
	 */
    public static boolean clearCache() {
        synchronized (fCurrentDownloads) {
            while (!fCurrentDownloads.isEmpty()) {
                try {
                    fCurrentDownloads.wait(0);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            IPath cachelocation = getCacheLocation();
            File cache = cachelocation.toFile();
            File[] allfiles = cache.listFiles();
            int failures = 0;
            if (allfiles.length > 0) {
                for (File file : allfiles) {
                    if (!file.delete()) {
                        failures++;
                    }
                }
            }
            return failures == 0;
        }
    }

    /**
	 * Checks if the given URL has already been downloaded and is in the cache.
	 * 
	 * @param url
	 *            The <code>URL</code> to check
	 * @return <code>true</code> if the URL is already in the cache, <code>false</code>
	 *         otherwise.
	 */
    public static boolean inCache(URL url) {
        File targetfile = getCacheFileFromURL(url);
        if (targetfile.canRead()) {
            return true;
        }
        return false;
    }

    /**
	 * Download the given URL.
	 * <p>
	 * If the file is already in the cache, it is taken from there.
	 * </p>
	 * s *
	 * <p>
	 * This method returns a {@link URLDownloadException} Object, containing both a
	 * <code>java.io.File</code> pointing to the downloaded file in the cache and an IStatus
	 * object for any errors encountered during the download. The severity of the IStatus is either
	 * <ul>
	 * <li>Status.OK: download was completed successful</li>
	 * <li>Status.ERROR: download failed</li>
	 * </ul>
	 * For failed downloads the following is returned in the IStatus object:
	 * <UL>
	 * <li><code>Status.getCode()</code>: the ordinal number of the {@link FailCode} enum</li>
	 * <li><code>Status.getMessage()</code>: the human readable reason for the error</li>
	 * <li><code>Status.getException()</code>: the low level reason for the error</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If the download is canceled by the User via the IProgressMonitor, an unchecked
	 * <code>OperationCanceledException</code> is thrown, which will be caught by the Job the
	 * caller is currently running in.
	 * </p>
	 * 
	 * @param url
	 *            The URL to download. It must be valid and not <code>null</code>
	 * @param monitor
	 *            <code>IProgressMonitor</code> for tracking the download progress and canceling
	 *            it.
	 * @return <code>URLDownloadException</code> with the downloaded file and a download status
	 *         message.
	 */
    public static File download(final URL url, final IProgressMonitor monitor) throws URLDownloadException {
        File file = internalDownload(url, monitor);
        return file;
    }

    /**
	 * Internal method to do the actual work.
	 */
    private static File internalDownload(final URL url, final IProgressMonitor monitor) throws URLDownloadException {
        File targetfile = null;
        File tempfile = null;
        InputStream sourcestream = null;
        OutputStream targetstream = null;
        boolean finished = false;
        final URLConnection connection;
        if (url == null) {
            return null;
        }
        try {
            monitor.beginTask("Downloading from " + url.toString(), 100);
            targetfile = getCacheFileFromURL(url);
            if (targetfile.canRead()) {
                monitor.worked(100);
                finished = true;
                return targetfile;
            }
            synchronized (fCurrentDownloads) {
                fCurrentDownloads.add(url);
            }
            String targetextension = targetfile.getName().substring(targetfile.getName().lastIndexOf('.'));
            try {
                tempfile = File.createTempFile("download", targetextension, targetfile.getParentFile());
                targetstream = new FileOutputStream(tempfile);
            } catch (IOException ioe) {
                throw new URLDownloadException("Could not create temporary file", ioe);
            }
            monitor.subTask("Opening Connection");
            try {
                connection = url.openConnection();
                connection.setReadTimeout(10 * 1000);
            } catch (IOException ioe) {
                throw new URLDownloadException("Could not connect to " + url.getHost(), ioe);
            }
            monitor.worked(5);
            monitor.subTask("Preparing Download");
            try {
                Object contenthandler = connection.getContent();
                if (contenthandler instanceof InputStream) {
                    sourcestream = (InputStream) contenthandler;
                } else {
                    throw new URLDownloadException("Unknown type of remote file \"" + url.getFile() + "\" on \"" + url.getHost() + "\"");
                }
            } catch (UnknownHostException e) {
                throw new URLDownloadException("Host \"" + url.getHost() + "\" unknown, check address", e);
            } catch (FileNotFoundException fnfe) {
                throw new URLDownloadException("File \"" + url.getFile() + "\" not found on \"" + url.getHost() + "\"", fnfe);
            } catch (IOException ioe) {
                throw new URLDownloadException("Could not read file \"" + url.getFile() + "\" on host \"" + url.getHost() + "\"", ioe);
            }
            monitor.worked(5);
            try {
                monitor.subTask("Downloading " + url.toString());
                int length = connection.getContentLength();
                if (length == -1) {
                    length = IProgressMonitor.UNKNOWN;
                }
                internalStreamCopyWithProgress(sourcestream, targetstream, targetfile.getName(), length, new SubProgressMonitor(monitor, 95));
                sourcestream.close();
                targetstream.close();
                if (tempfile.renameTo(targetfile) == false) {
                    throw new URLDownloadException("Could not rename temporary file " + tempfile.toString() + " to " + targetfile.toString());
                }
                finished = true;
                return targetfile;
            } catch (SocketTimeoutException ste) {
                throw new URLDownloadException("Connection to " + connection.getURL().getHost() + " timed out", ste);
            } catch (SecurityException se) {
                throw new URLDownloadException("Permission denied by Java Security Manager", se);
            } catch (IOException ioe) {
                throw new URLDownloadException("Error downloading \nfrom: " + connection.getURL().toExternalForm() + "\nto:   " + targetfile.toString(), ioe);
            }
        } finally {
            monitor.done();
            synchronized (fCurrentDownloads) {
                fCurrentDownloads.remove(url);
                fCurrentDownloads.notifyAll();
            }
            if (!finished) {
                try {
                    if (sourcestream != null) {
                        sourcestream.close();
                    }
                    if (targetstream != null) {
                        targetstream.close();
                    }
                } catch (IOException ioe) {
                }
                if ((tempfile != null) && (tempfile.exists())) {
                    if (!tempfile.delete()) {
                        IStatus status = new Status(IStatus.WARNING, AVRPlugin.PLUGIN_ID, "Could not delete temporary file [" + tempfile.toString() + "]", null);
                        AVRPlugin.getDefault().log(status);
                    }
                }
                if ((targetfile != null) && (targetfile.exists())) {
                    if (!targetfile.delete()) {
                        IStatus status = new Status(IStatus.WARNING, AVRPlugin.PLUGIN_ID, "Could not delete temporary target file [" + targetfile.toString() + "]", null);
                        AVRPlugin.getDefault().log(status);
                    }
                }
            }
        }
    }

    /**
	 * Copy an InputStream to an OutputStream with a IProgressMonitor. The copy is done in 1KByte
	 * chunks and the ProgressMonitor is updated every 10 chunks.
	 * 
	 * @param sourcestream
	 *            InputStream of the source
	 * @param targetstream
	 *            OutputStream of the traget
	 * @param filename
	 *            Name to display in the ProgressMonitor
	 * @param sourcelength
	 *            Number of bytes in the InputSource. Used by the ProgressMonitor to show the
	 *            current Progress.
	 * @param monitor
	 *            IProgressMonitor
	 * 
	 * @throws SocketTimeoutException
	 *             Connection timed out
	 * @throws IOException
	 *             Any error reading from the sourcestream or writing to the targetstream.
	 * @throws OperationCanceledException
	 *             Download canceled by user
	 */
    private static void internalStreamCopyWithProgress(final InputStream sourcestream, final OutputStream targetstream, String filename, int sourcelength, final IProgressMonitor monitor) throws SocketTimeoutException, IOException {
        try {
            monitor.beginTask("Downloading", sourcelength);
            final DownloadRateCalculator mydrc = fDRC;
            byte[] buffer = new byte[1024];
            int readlength = 0;
            int byteswritten = 0;
            mydrc.setSampleSize(sourcelength > 0 ? sourcelength / 1024 / 10 : 50);
            mydrc.start();
            int blockcount = 0;
            while ((readlength = sourcestream.read(buffer)) != EOF) {
                targetstream.write(buffer, 0, readlength);
                byteswritten += readlength;
                String currentrate = mydrc.getCurrentRateString(readlength);
                if (blockcount++ % 10 == 0) {
                    monitor.subTask("Downloading " + filename + " [" + byteswritten / 1024 + "k / " + sourcelength / 1024 + "k] at " + currentrate);
                }
                monitor.worked(readlength);
                if (monitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
            }
        } finally {
            monitor.done();
        }
    }

    /**
	 * Gets an IPath to the cache folder in the plugin storage location. If the cache folder does
	 * not exist, it is created.
	 */
    private static IPath getCacheLocation() {
        IPath cachelocation = AVRPlugin.getDefault().getStateLocation().append(CACHEPATH);
        File cachelocationfile = cachelocation.toFile();
        if (!cachelocationfile.exists()) {
            if (!cachelocationfile.mkdirs()) {
                IStatus status = new Status(IStatus.WARNING, AVRPlugin.PLUGIN_ID, "Could not create download cache folder [" + cachelocationfile.toString() + "]", null);
                AVRPlugin.getDefault().log(status);
            }
        }
        return cachelocation;
    }

    /**
	 * Get a <code>java.io.File</code> reference for the file in the cache.
	 * <p>
	 * This is simplistic and just takes the file at the end of the path part of the given URL as
	 * the new filename.<br>
	 * This will not work correctly if two different URLs have the same filename.
	 * </p>
	 * 
	 * @param url
	 * @return
	 */
    private static File getCacheFileFromURL(final URL url) {
        IPath cachelocation = getCacheLocation();
        String pathname = url.getPath();
        String filename = pathname.substring(pathname.lastIndexOf('/') + 1);
        IPath cachefilepath = cachelocation.append(filename);
        File cachefile = cachefilepath.toFile();
        return cachefile;
    }
}
