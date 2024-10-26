package org.quantumleaphealth.transform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Download a file's contents from a URL if it has a different length from a local file.
 * @author Tom Bechtold
 * @version 2008-10-06
 */
public class ImportURL {

    /**
     * Logger
     */
    static final Logger LOGGER = Logger.getLogger(ImportURL.class.getName());

    /**
     * Buffer size.
     * Optimal for TCP is 32k bytes.
     */
    private static final int BUFFER_SIZE = 32 * 1024;

    /**
     * Downloads a remote file to a local one if its length is different.
     * Syntax for a FTP remote file is <tt>ftp://username:password@ftp.server.com/filename.ext;type=i</tt>
     * @param remoteURL the remote file as specified by <code>java.net.URL(String)</code>
     * @param localFilename the local file as specified by <code>java.io.File(String)</code>
     * @return whether the remote file was downloaded
     * @throws IllegalArgumentException if either parameter is <code>null</code>
     *         or <code>remote</code> is not a valid URL
     * @throws IOException if we cannot write to or get length from <code>local</code>
     *         or a reading/writing error occurs
     */
    public static boolean downloadFile(String remoteURL, String localFilename) throws IllegalArgumentException, IOException {
        if (remoteURL == null) throw new IllegalArgumentException("Must specify remote url");
        if (localFilename == null) throw new IllegalArgumentException("Must specify remote file");
        try {
            return downloadFile(new URL(remoteURL), new File(localFilename));
        } catch (MalformedURLException malformedURLException) {
            throw new IllegalArgumentException(malformedURLException);
        }
    }

    /**
     * Downloads a remote file to a local one if its length is different.
     * @param remote the remote file
     * @param local the local file
     * @return whether the remote file was downloaded
     * @throws IllegalArgumentException if either parameter is <code>null</code>
     * @throws IOException if we cannot write to or get length from <code>local</code>
     *         or a reading/writing error occurs
     */
    private static boolean downloadFile(URL remote, File local) throws IllegalArgumentException, IOException {
        if (local == null) throw new IllegalArgumentException("Must specify remote file");
        if (local.exists() && !local.canWrite()) throw new IOException("Cannot write to " + local);
        long lengthCurrent = local.exists() ? local.length() : -1l;
        if (lengthCurrent == 0) throw new IOException("Cannot get the length for " + local);
        long lastModifiedCurrent = local.exists() ? local.lastModified() : -1l;
        if (remote == null) throw new IllegalArgumentException("Must specify remote url");
        URLConnection urlConnection = remote.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        long lengthRemote = urlConnection.getContentLength();
        long lastModifiedRemote = urlConnection.getLastModified();
        if (lengthRemote == lengthCurrent) {
            LOGGER.fine("Not downloading " + remote + " of length " + lengthRemote + " and date " + (lastModifiedRemote > 0 ? new Date(lastModifiedRemote).toString() : "n/a") + " for " + local + " of length " + lengthCurrent + " and date " + new Date(lastModifiedCurrent));
            return false;
        }
        BufferedInputStream inputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            inputStream = new BufferedInputStream(urlConnection.getInputStream(), BUFFER_SIZE);
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(local));
            byte[] buffer = new byte[BUFFER_SIZE];
            do {
                int count = inputStream.read(buffer);
                if (count < 0) break;
                bufferedOutputStream.write(buffer, 0, count);
            } while (true);
            return true;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Throwable throwable) {
            }
            try {
                if (bufferedOutputStream != null) bufferedOutputStream.close();
            } catch (Throwable throwable) {
            }
        }
    }

    /**
     * Converts local file names and/or local directories and 
     * downloads remote files to local locations.
     * This method only downloads those remote files specified with the 
     * <tt>ftp</tt> or <tt>http</tt> protocols and whose local locations
     * do not already contain the file's content. 
     * @param spec each element is either a local files, local directory
     *        or remote file -- identified with either the 
     *        <tt>ftp</tt> or <tt>http</tt> protocols;
     *        if a remote file then the parameter following specifies 
     *        the filename of its local location 
     * @return the local files, local directories and local locations
     *         of updated remote files; guaranteed non-<code>null</code>
     */
    public static List<File> downloadFiles(String[] spec) {
        LinkedList<File> list = new LinkedList<File>();
        if ((spec == null) || (spec.length == 0)) return list;
        for (int index = 0; index < spec.length; index++) {
            spec[index] = spec[index].trim();
            if (spec[index].length() == 0) {
                LOGGER.log(Level.WARNING, "Unnamed input file # " + index + " skipped");
                continue;
            }
            if (spec[index].startsWith("ftp://") || spec[index].startsWith("http://")) {
                if ((index + 1) >= spec.length) {
                    LOGGER.log(Level.WARNING, "Unspecified local location for " + spec[index]);
                    continue;
                }
                try {
                    if (!ImportURL.downloadFile(spec[index], spec[index + 1])) index++;
                } catch (IllegalArgumentException illegalArgumentException) {
                    LOGGER.log(Level.WARNING, "Skipping remote location " + spec[index] + " having local location " + spec[index + 1], illegalArgumentException);
                } catch (IOException ioException) {
                    LOGGER.log(Level.WARNING, "Skipping remote location " + spec[index] + " having local location " + spec[index + 1], ioException);
                }
            } else {
                File file = new File(spec[index]);
                if (file.isDirectory() || file.canRead()) list.add(file); else LOGGER.log(Level.WARNING, "Skipping unreadable local location " + spec[index]);
            }
        }
        return list;
    }

    /**
     * Downloads a remote file if different from a local file.
     * This method displays an error message if less than two arguments.
     * Otherwise it displays the result status of the download.
     * @param args the first argument is the remote URL
     *        as specified by <code>java.net.URL(String)</code>, 
     *        the second is the pathname of the local file
     *        as specified by <code>java.io.File(String)</code>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) LOGGER.severe("Args: remoteURL localFile e.g., ftp://username:password@ftp.server.com/filename.ext;type=i /filename.ext"); else if (downloadFile(args[0], args[1])) LOGGER.info("Downloaded"); else LOGGER.info("Not downloaded");
    }
}
