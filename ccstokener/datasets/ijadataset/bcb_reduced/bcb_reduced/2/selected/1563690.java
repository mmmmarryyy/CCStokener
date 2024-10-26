package org.likken.util;

import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * ByteLoader is a utility class from loading bytes from
 * InputStreams, URLs.  
 *
 * Note that the client is responsible for maintaining
 * and closing input streams passed to ByteLoader methods. 
 *
 * @author Stephane Boisson <s.boisson@focal-net.com> 
 * @version $Revision: 1.2 $ $Date: 2001/02/28 12:14:42 $
 */
public class ByteLoader {

    private static int DEFAULT_CHUNK_SIZE = 256;

    /**
    * Load an array of bytes from the supplied url data source
    *
    * @param url the url to load the bytes from
    * @return bytes from the url
    * @exception reports io errors
    */
    public static byte[] loadBytesFromURL(URL url) throws IOException {
        URLConnection con = url.openConnection();
        int size = con.getContentLength();
        InputStream in = con.getInputStream();
        if (in != null) {
            try {
                return (size != -1) ? loadBytesFromStreamForSize(in, size) : loadBytesFromStream(in);
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
        return null;
    }

    /**
     * Loads the supplied number bytes from the given input stream
     *
     * @param in stream to read the bytes from
     * @param size the number of bytes to read
     * @return bytes read from the stream
     * @exception reports IO errors
     */
    public static byte[] loadBytesFromStreamForSize(InputStream in, int size) throws IOException {
        int count, index = 0;
        byte[] buffer = new byte[size];
        while ((count = in.read(buffer, index, size)) > 0) {
            size -= count;
            index += count;
        }
        return buffer;
    }

    /**
     * Loads bytes from the given input stream until the end of stream
     * is reached. 
     *
     * @param in stream to read the bytes from
     * @return bytes read from the stream
     * @exception reports IO errors
     */
    public static byte[] loadBytesFromStream(InputStream in) throws IOException {
        return loadBytesFromStream(in, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Loads bytes from the given input stream until the end of stream
     * is reached.  Bytes are read in at the supplied chunkSize
     * rate.
     *
     * @param in stream to read the bytes from
     * @param size the reading rate
     * @return bytes read from the stream
     * @exception reports IO errors
     */
    public static byte[] loadBytesFromStream(InputStream in, int chunkSize) throws IOException {
        if (chunkSize < 1) {
            chunkSize = DEFAULT_CHUNK_SIZE;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[chunkSize];
            int count;
            while ((count = in.read(buffer, 0, chunkSize)) > 0) {
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        } finally {
            out.close();
            out = null;
        }
    }
}
