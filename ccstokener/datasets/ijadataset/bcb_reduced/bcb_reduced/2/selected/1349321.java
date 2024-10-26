package edu.cmu.cs.euklas.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class provides functionality for downloading
 * code from a given URL and saving this code in a
 * file on disk.
 * 
 * @author Andrew Faulring and Christian Doerner
 *
 */
public class HTTPDownloader {

    /**
	 * This method downloads the data at a given URL and
	 * saves this data in a file on disk.
	 * 
	 * @param url The URL to download from
	 * @param dir The directory to store the file in
	 * @param fileName The name of the file
	 * @param maxFileDownloadSize The maximum size of the file that should be downloaded
	 */
    public static void downloadWebpage(URL url, String dir, String fileName, int maxFileDownloadSize) {
        Integer totalBytesRead = null;
        File tempFile = null;
        try {
            File localFile = new File(dir, fileName);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
            urlConnection.setConnectTimeout(6000);
            urlConnection.setReadTimeout(6000);
            InputStream inputStream = urlConnection.getInputStream();
            tempFile = File.createTempFile("euklas-", null, new File(dir));
            tempFile.deleteOnExit();
            OutputStream outputStream = new FileOutputStream(tempFile);
            totalBytesRead = pump(inputStream, outputStream, maxFileDownloadSize);
            if (totalBytesRead != null) {
                localFile.getParentFile().mkdirs();
                if (!tempFile.renameTo(localFile)) {
                    System.err.println("[HTTPDownloader]: Error while trying to rename file '" + tempFile.getAbsolutePath() + "'!");
                }
            }
        } catch (IOException e) {
            System.err.println("[HTTPDownloader]: Error while trying to read from URL: " + url.toExternalForm());
        } finally {
            if ((tempFile != null) && (tempFile.exists())) {
                if (!tempFile.delete()) {
                    System.err.println("[HTTPDownloader]: Error while trying to delete file '" + tempFile.getAbsolutePath() + "'!");
                }
            }
        }
    }

    /**
	 * This method 'pumps' an input stream into an output stream
	 * 
	 * @param src The source stream
	 * @param dest The destination stream
	 * @param maxBytesToRead The max. number of bytes to read
	 * @return How many bytes were read
	 * @throws IOException If something goes wrong
	 */
    private static int pump(InputStream src, OutputStream dest, int maxBytesToRead) throws IOException {
        int totalBytesRead = 0;
        byte[] buf = new byte[8096];
        long start = System.currentTimeMillis();
        try {
            while (totalBytesRead <= maxBytesToRead) {
                long msSinceStart = System.currentTimeMillis() - start;
                if (msSinceStart > 180000) {
                    throw new SocketTimeoutException("aborting after " + msSinceStart + "ms");
                }
                int bytesAvailable = src.available();
                if (bytesAvailable == 0) {
                    break;
                }
                int bytesRead = src.read(buf, 0, Math.min(bytesAvailable, buf.length));
                if (bytesRead < 0) {
                    break;
                }
                dest.write(buf, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
        } finally {
            try {
                src.close();
            } catch (IOException ex) {
                System.err.println("closing input stream");
            }
            try {
                dest.flush();
                dest.close();
            } catch (IOException ex) {
                System.err.println("closing input stream");
            }
        }
        return totalBytesRead;
    }
}
