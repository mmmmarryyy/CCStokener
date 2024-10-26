package name.angoca.zemucan.tools.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import name.angoca.zemucan.AbstractZemucanException;

/**
 * Abstract exception that defines a problem while reading a file.
 * <p>
 * <b>Control Version</b>
 * <p>
 * <ul>
 * <li>1.0.0 Class creation.</li>
 * </ul>
 *
 * @author Andres Gomez Casanova <a
 *         href="mailto:a n g o c a at y a h o o dot c o m">(AngocA)</a>
 * @version 1.0.0 2010-08-22
 */
public class FileReader {

    /**
     * Returns the stream that corresponds to the requested file.
     *
     * @param fileName
     *            Name of the file.
     * @return The stream of the requested file.
     * @throws AbstractZemucanException
     *             If there is a problem while retrieving the content.
     */
    public static InputStream getStream(final String fileName) throws AbstractZemucanException {
        if (fileName == null) {
            throw new FileNotDefinedException();
        }
        InputStream inputStream = null;
        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (loader != null) {
            URL url = loader.getResource(fileName);
            if (url == null) {
                url = loader.getResource("/" + fileName);
            }
            if (url != null) {
                try {
                    inputStream = url.openStream();
                } catch (final IOException exception) {
                    throw new CorruptFileException(exception);
                }
            } else {
                final File file = new File(fileName);
                if (file.exists()) {
                    try {
                        inputStream = new FileInputStream(file);
                    } catch (final java.io.FileNotFoundException e) {
                        throw new name.angoca.zemucan.tools.file.FileNotFoundException(fileName, e);
                    }
                } else {
                    throw new name.angoca.zemucan.tools.file.FileNotFoundException(fileName);
                }
            }
        }
        assert inputStream != null;
        return inputStream;
    }
}
