package org.mbari.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import org.mbari.io.IOUtilities;

/**
 *
 * @author brian
 */
public class URLUtilities {

    /** Creates a new instance of URLUtilities */
    public URLUtilities() {
    }

    /**
     * Method description
     *
     *
     * @param url
     * @param file
     */
    public static void copy(URL url, File file) throws IOException {
        InputStream inputStream = new BufferedInputStream(url.openStream());
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
        try {
            IOUtilities.copy(inputStream, outputStream);
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    /**
     * Create a temp file for copying the contents of the URL into. This
     * temp file will have the same extension as the URL
     */
    public static File urlToTempFile(URL url) throws IOException {
        String externalForm = url.toExternalForm();
        String[] pathParts = externalForm.split("/");
        String name = pathParts[pathParts.length - 1];
        String[] nameParts = name.split("\\.");
        String ext = nameParts[nameParts.length - 1];
        if (nameParts.length <= 1) {
            ext = "";
        }
        return File.createTempFile("temp", ext);
    }

    public static File copyToTempFile(URL url) throws IOException {
        File tmpFile = urlToTempFile(url);
        copy(url, tmpFile);
        return tmpFile;
    }

    /**
     * Method description
     *
     *
     * @param in
     *
     * @return
     *
     * @throws java.io.IOException
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] byteArray = null;
        try {
            IOUtilities.copy(in, bo);
            byteArray = bo.toByteArray();
        } finally {
            bo.close();
        }
        return byteArray;
    }

    /**
     * Convert a local URL to a File object
     * @param url The URL to convert
     * @return the corresponding file object.
     */
    public static File toFile(URL url) {
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f;
    }
}
