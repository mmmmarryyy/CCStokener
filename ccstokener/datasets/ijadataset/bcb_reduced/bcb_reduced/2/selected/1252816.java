package proguard;

import java.io.*;
import java.net.URL;

/**
 * A <code>WordReader</code> that returns words from a file or a URL.
 *
 * @author Eric Lafortune
 */
public class FileWordReader extends WordReader {

    private String fileName;

    private LineNumberReader reader;

    /**
     * Creates a new FileWordReader for the given file name.
     */
    public FileWordReader(String fileName) throws IOException {
        this.fileName = fileName;
        this.reader = new LineNumberReader(new BufferedReader(new FileReader(fileName)));
    }

    /**
     * Creates a new FileWordReader for the given URL.
     */
    public FileWordReader(URL url) throws IOException {
        this.fileName = url.getPath();
        this.reader = new LineNumberReader(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /**
     * Closes the FileWordReader.
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    protected String nextLine() throws IOException {
        return reader.readLine();
    }

    protected String lineLocationDescription() {
        return "line " + reader.getLineNumber() + " of file '" + fileName + "'";
    }
}
