package net.sf.gridarta.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility-class for Gridarta's I/O.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 */
public class IOUtils {

    /**
     * Encoding to use for maps and other data. The encoding given here is used
     * for reading and writing maps. You shouldn't change this only because you
     * use an UTF-8 system or so, since the encoding used here MUST be
     * compatible with the server and the client and the maps. The paragraph
     * sign § is the critical character.
     * @todo once the mailing list decided on the § character, eventually
     * replace by "us-ascii".
     */
    public static final String MAP_ENCODING = "iso-8859-1";

    /**
     * Utility class - do not instantiate.
     */
    private IOUtils() {
    }

    /**
     * Get the {@link URL} of a resource.
     * @param dir the directory to read from
     * @param fileName the file name of the file to read
     * @return the URL for reading from <var>fileName</var>
     * @throws FileNotFoundException in case all tries getting a URL to the file
     * failed
     */
    @NotNull
    public static URL getResource(@Nullable final File dir, @NotNull final String fileName) throws FileNotFoundException {
        try {
            final File file = new File(dir, fileName);
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (final MalformedURLException ignored) {
        }
        try {
            final File file = new File(fileName);
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (final MalformedURLException ignored) {
        }
        final URI currentDir = new File(System.getProperty("user.dir")).toURI();
        final String relWithDir = currentDir.relativize(new File(dir, fileName).toURI()).toString();
        final String relPlain = currentDir.relativize(new File(fileName).toURI()).toString();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        @Nullable final URL url1 = contextClassLoader.getResource(relWithDir);
        if (url1 != null) {
            return url1;
        }
        @Nullable final URL url2 = contextClassLoader.getResource(relPlain);
        if (url2 != null) {
            return url2;
        }
        @Nullable final URL url3 = ClassLoader.getSystemResource(relWithDir);
        if (url3 != null) {
            return url3;
        }
        @Nullable final URL url4 = ClassLoader.getSystemResource(relPlain);
        if (url4 != null) {
            return url4;
        }
        throw new FileNotFoundException("couldn't find '" + new File(fileName) + "'.");
    }

    /**
     * Returns a {@link File} instance for a resource that is a regular file on
     * the file system. Returns the passed file if it is a regular file.
     * Otherwise copies the passed file into a temporary regular file and
     * returns the copy.
     * @param dir directory to read from
     * @param fileName file name of file to read
     * @return the file
     * @throws IOException if the file does not exist or cannot be copied to the
     * file system
     */
    @NotNull
    public static File getFile(@Nullable final File dir, @NotNull final String fileName) throws IOException {
        final URL url = getResource(dir, fileName);
        final String urlString = url.toString();
        if (urlString.startsWith("file:")) {
            final File file = new File(urlString.substring(5));
            if (file.exists()) {
                return file;
            }
        }
        final File tmpFile = File.createTempFile("gridarta", null);
        tmpFile.deleteOnExit();
        final InputStream inputStream = url.openStream();
        try {
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            try {
                final FileOutputStream outputStream = new FileOutputStream(tmpFile);
                try {
                    final byte[] buf = new byte[65536];
                    while (true) {
                        final int len = bufferedInputStream.read(buf);
                        if (len == -1) {
                            break;
                        }
                        outputStream.write(buf, 0, len);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                bufferedInputStream.close();
            }
        } finally {
            inputStream.close();
        }
        return tmpFile;
    }

    /**
     * Searches for <code>file</code> on all paths specified by the environment
     * variable "PATH".
     * @param name the file to search for
     * @return the matching file
     * @throws IOException if the file was not found
     */
    @NotNull
    public static File findPathFile(@NotNull final String name) throws IOException {
        final File absoluteFile = new File(name);
        if (absoluteFile.isAbsolute()) {
            return absoluteFile;
        }
        final String pathSpec = System.getenv("PATH");
        if (pathSpec == null) {
            throw new IOException("environment variable PATH is undefined");
        }
        final String[] tmp = pathSpec.split(Pattern.quote(File.pathSeparator), -1);
        for (final String path : tmp) {
            final File dir = new File(path.isEmpty() ? "." : path);
            final File file = new File(dir, name);
            if (file.exists()) {
                return file;
            }
        }
        throw new IOException("'" + name + "' not found in " + pathSpec);
    }
}
