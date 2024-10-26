package org.jasen.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.jasen.io.NonBlockingStreamReader;
import org.jasen.io.StreamReaderListener;

/**
 * <P>
 * 	General WWW and net utility methods.
 * </P>
 * @author Jason Polites
 */
public class WebUtils {

    public static final String URL_REGEX = "";

    /**
     * Returns true iff the text passed represents a url.
     * <p>
     * A url may take many forms:
     * <br/><br/>
     * For example:
     * <ul>
     * 	<li/>www.google.com
     * 	<li/>http://www.google.com
     * 	<li/>google.com
     * 	<li/>toolbar.google.com
     * 	<li/>a.b.c.d.e.f.g.google.com
     * 	<li/>etc...
     * </ul>
     * </p>
     * <p>
     * Unfortunately there doesn't seem to be an "elegant" way to determine<br/>
     * definatively if an unknown String is in fact a URL.  We are just going<br/>
     * to use the java.net.URL class and assume a MalformedURLException implies<br/>
     * that the string is not a url.
     * </p>
     * @param text
     * @return True if the text is a valid URL, false otherwise
     */
    public static boolean isUrl(String text) {
        try {
            new URL(text);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Attempts to download the file given by the URL and saves it to the output stream provided
     * <br/>
     * NOTE: The given output stream will NOT be closed within this method
     * @param url The fully qualified url path to the remote resource
     * @param out The output stream to which the data read will be written
     * @param bufferSize The buffer size to use for reading bytes
     * @param timeout The timeout (in milliseconds) to wait for a response from the remote server
     * @throws IOException If an error occurred during transit
     */
    public static void get(URL url, OutputStream out, int bufferSize, long timeout) throws IOException {
        get(url, out, bufferSize, timeout, null);
    }

    /**
     * Attempts to download the file given by the URL and saves it to the output stream provided
     * <br/>
     * NOTE: The given output stream will NOT be closed within this method
     * @param url The fully qualified url path to the remote resource
     * @param out The output stream to which the data read will be written
     * @param bufferSize The buffer size to use for reading bytes
     * @param timeout The timeout (in milliseconds) to wait for a response from the remote server
     * @param listener A listener which will report when bytes have been written to the output stream
     * @throws IOException If an error occurred during transit
     */
    public static void get(URL url, OutputStream out, int bufferSize, long timeout, StreamReaderListener listener) throws IOException {
        InputStream in = null;
        NonBlockingStreamReader reader = null;
        try {
            in = url.openConnection().getInputStream();
            reader = new NonBlockingStreamReader(listener);
            reader.read(in, out, bufferSize, timeout, null);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
