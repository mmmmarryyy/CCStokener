package uk.ac.shef.wit.runes.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

public class UtilFiles {

    private static final Logger log = Logger.getLogger(UtilFiles.class.getName());

    public static final String CLASSPATH_SEPARATOR = System.getProperty("os.name").contains("indows") ? ";" : ":";

    private static final int BUFFER_SIZE = 1 << 13;

    /**
    * <p>Reads content from an URL into a string buffer.</p>
    *
    * @param url the url to get the content from.
    * @return string buffer with the contents of the url.
    * @throws IOException problem reading the url stream.
    */
    public static StringBuilder getContent(final URL url) throws IOException {
        final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
            final char[] readBuffer = new char[BUFFER_SIZE];
            int numRead = 0;
            do {
                int offset = 0;
                while (BUFFER_SIZE > offset && 0 <= (numRead = reader.read(readBuffer, offset, BUFFER_SIZE - offset))) offset += numRead;
                buffer.append(readBuffer, 0, offset);
            } while (0 <= numRead);
        } finally {
            if (reader != null) reader.close();
        }
        buffer.trimToSize();
        return buffer;
    }

    /**
    * <p>Adds a separator character to the end of the filename if it does not have one already.</p>
    *
    * @param filename the filename.
    * @return the filename with a separator at the end.
    */
    public static String addSeparator(final String filename) {
        if (filename != null && !filename.endsWith(File.separator)) return filename + File.separator; else return filename;
    }

    private UtilFiles() {
    }
}
