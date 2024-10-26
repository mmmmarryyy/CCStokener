package javax.media.ding3d.utils.shader;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * Utility class with static methods to read the entire contents of a
 * file, URL, InputStream, or Reader into a single String that is
 * returned to the user.
 *
 * @since Java 3D 1.4
 */
public class StringIO {

    /**
     * Read the entire contents of the specified file and return a
     * single String object containing the contents of the file.
     *
     * @param fileName the name of the file from which to read
     *
     * @return a String containing the contents of the input file
     *
     * @throws IOException if the specified file cannot be opened, or
     * if an I/O error occurs while reading the file
     */
    public static String readFully(String fileName) throws IOException {
        return readFully(new File(fileName));
    }

    /**
     * Read the entire contents of the specified file and return a
     * single String object containing the contents of the file.
     * This method does not return until the end of the input file
     * is reached.
     *
     * @param file a File from which to read
     *
     * @return a String containing the contents of the input file
     *
     * @throws IOException if the specified file cannot be opened, or
     * if an I/O error occurs while reading the file
     */
    public static String readFully(File file) throws IOException {
        return readFully(new FileReader(file));
    }

    /**
     * Read the entire contents of the specified URL and return a
     * single String object containing the contents of the URL.
     * This method does not return until an end of stream is reached
     * for the URL.
     *
     * @param url a URL from which to read
     *
     * @return a String containing the contents of the input URL
     *
     * @throws IOException if the specified URL cannot be opened, or
     * if an I/O error occurs while reading the URL
     */
    public static String readFully(URL url) throws IOException {
        return readFully(url.openStream());
    }

    /**
     * Read the entire contents of the specified InputStream and return a
     * single String object containing the contents of the InputStream.
     * This method does not return until the end of the input
     * stream is reached.
     *
     * @param stream an InputStream from which to read
     *
     * @return a String containing the contents of the input stream
     *
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    public static String readFully(InputStream stream) throws IOException {
        return readFully(new InputStreamReader(stream));
    }

    /**
     * Read the entire contents of the specified Reader and return a
     * single String object containing the contents of the InputStream.
     * This method does not return until the end of the input file or
     * stream is reached.
     *
     * @param reader a Reader from which to read
     *
     * @return a String containing the contents of the stream
     *
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    public static String readFully(Reader reader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuffer buf = new StringBuffer();
        int numChars;
        while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
            buf.append(arr, 0, numChars);
        }
        return buf.toString();
    }

    /**
     * Do not construct an instance of this class.
     */
    private StringIO() {
    }
}
