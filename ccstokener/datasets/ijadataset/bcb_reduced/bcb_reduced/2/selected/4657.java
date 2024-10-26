package org.rascalli.mbe.adaptivemind;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;

public class ByteUtils {

    private static final int DEFAULT_CHUNK_SIZE = 1024;

    public static InputStream getWebSiteAsInputStream(String strUrl) {
        URL url;
        FileOutputStream out = null;
        DataInputStream dis = null;
        URLConnection urlc = null;
        try {
            url = new URL(strUrl);
            urlc = (URLConnection) url.openConnection();
            String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8) Gecko/20051111 Firefox/1.5";
            urlc.setRequestProperty("User-Agent", userAgent);
            urlc.setRequestProperty("Connection", "close");
            urlc.setReadTimeout(5 * 1000 * 60);
            urlc.setAllowUserInteraction(false);
            urlc.setDoInput(true);
            urlc.setDoOutput(false);
            urlc.setUseCaches(false);
            urlc.connect();
            return urlc.getInputStream();
        } catch (SocketTimeoutException e) {
            System.err.println("socket timeout");
        } catch (Exception e) {
            System.err.println("url not found");
        } finally {
            if (out != null) try {
                out.close();
            } catch (Exception e) {
            }
            ;
            if (dis != null) try {
                dis.close();
            } catch (Exception e) {
            }
            ;
        }
        return null;
    }

    /**
     * save bytes to file
     * @param fileName the file to write the supplied bytes
     * @param theBytes the bytes to write to file
     * @throws java.io.IOException reports problems saving bytes to file
     */
    public static void saveBytesToFile(String fileName, byte[] theBytes) throws java.io.IOException {
        saveBytesToStream(new java.io.FileOutputStream(fileName), theBytes);
    }

    /**
     * save bytes to output stream and close the output stream on success and
     * on failure.
     * @param out the output stream to write the supplied bytes
     * @param theBytes the bytes to write out
     * @throws java.io.IOException reports problems saving bytes to output stream
     */
    public static void saveBytesToStream(java.io.OutputStream out, byte[] theBytes) throws java.io.IOException {
        try {
            out.write(theBytes);
        } finally {
            out.flush();
            out.close();
        }
    }

    public static byte[] loadBytesFromUrl(String url) throws java.io.IOException {
        byte[] returnByte = null;
        InputStream is = getWebSiteAsInputStream(url);
        if (is != null) {
            returnByte = IOUtils.toByteArray(is);
        }
        return returnByte;
    }

    /**
     * Loads bytes from the file
     *
     * @param fileName file to read the bytes from
     * @return bytes read from the file
     * @exception java.io.IOException reports IO failures
     */
    public static byte[] loadBytesFromFile(String fileName) throws java.io.IOException {
        return IOUtils.toByteArray(new java.io.FileInputStream(fileName));
    }

    /**
     * Loads bytes from the given input stream until the end of stream
     * is reached.  It reads in at kDEFAULT_CHUNK_SIZE chunks.
     *
     * @param stream to read the bytes from
     * @return bytes read from the stream
     * @exception java.io.IOException reports IO failures
     */
    public static byte[] loadBytesFromStream(java.io.InputStream in) throws java.io.IOException {
        return IOUtils.toByteArray(in);
    }

    /**
     * Loads bytes from the given input stream until the end of stream
     * is reached.  Bytes are read in at the supplied <code>chunkSize</code>
     * rate.
     *
     * @param stream to read the bytes from
     * @return bytes read from the stream
     * @exception java.io.IOException reports IO failures
     */
    public static byte[] loadBytesFromStream(java.io.InputStream in, int chunkSize) throws java.io.IOException {
        if (chunkSize < 1) chunkSize = DEFAULT_CHUNK_SIZE;
        int count;
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[chunkSize];
        try {
            while ((count = in.read(b, 0, chunkSize)) > 0) {
                bo.write(b, 0, count);
            }
            byte[] thebytes = bo.toByteArray();
            return thebytes;
        } finally {
            bo.close();
        }
    }
}
