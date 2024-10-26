package net.sf.nic.util;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * TODO, Create Description
 *
 * @author Juergen_Kellerer, 2009-12-12
 * @version 1.0
 */
public final class IOUtils {

    static final int TRANSFER_BUFFER_SIZE = 8 * 1024;

    static final String FILE_SCHEME = "file";

    static final class PreservingFileOutputStream extends FileOutputStream {

        File file;

        long lastModified;

        PreservingFileOutputStream(File file, long lastModified) throws FileNotFoundException {
            super(file);
            this.file = file;
            this.lastModified = lastModified;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (lastModified > 0) {
                if (!file.setLastModified(lastModified)) {
                    throw new IOException("Failed to apply the last modified date of " + new Date(lastModified) + " to file " + file);
                }
            }
        }
    }

    /**
	 * Returns the last modification date for the given url.
	 *
	 * @param url The url to return the last modification date for.
	 * @return The date of the last modification.
	 * @throws java.io.IOException In case of the date cannot be retrieved.
	 */
    public static long getLastModified(URI url) throws IOException {
        if (FILE_SCHEME.equalsIgnoreCase(url.getScheme())) return new File(url).lastModified(); else return getLastModified(url.toURL());
    }

    /**
	 * Returns the last modification date for the given url.
	 *
	 * @param url The url to return the last modification date for.
	 * @return The date of the last modification.
	 * @throws java.io.IOException In case of the date cannot be retrieved.
	 */
    public static long getLastModified(URL url) throws IOException {
        if (FILE_SCHEME.equalsIgnoreCase(url.getProtocol())) {
            try {
                return new File(url.toURI()).lastModified();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        } else {
            URLConnection uc = url.openConnection();
            uc.setDoInput(false);
            return uc.getLastModified();
        }
    }

    /**
	 * Opens the input stream that may be used to read the data from.
	 *
	 * @param url The url to read from.
	 * @return the input stream that may be used to read the data from.
	 * @throws IOException In case of the input stream cannot be opened.
	 */
    public static InputStream openInputStream(URI url) throws IOException {
        if (FILE_SCHEME.equalsIgnoreCase(url.getScheme())) return new FileInputStream(new File(url)); else return openInputStream(url.toURL());
    }

    /**
	 * Opens the input stream that may be used to read the data from.
	 *
	 * @param url The url to read from.
	 * @return the input stream that may be used to read the data from.
	 * @throws IOException In case of the input stream cannot be opened.
	 */
    public static InputStream openInputStream(URL url) throws IOException {
        return url.openStream();
    }

    /**
	 * Opens the output stream that may be used to write the data to.
	 *
	 * @param url		  The url to write to.
	 * @param lastModified The last modification date to set or -1 to use the current date.
	 * @return the input stream that may be used to read the data from.
	 * @throws IOException In case of the input stream cannot be opened.
	 */
    public static OutputStream openOutputStream(URI url, long lastModified) throws IOException {
        if (FILE_SCHEME.equalsIgnoreCase(url.getScheme())) return new PreservingFileOutputStream(new File(url), lastModified); else return openOutputStream(url.toURL(), lastModified);
    }

    /**
	 * Opens the input stream that may be used to read the data from.
	 *
	 * @param url		  The url to read from.
	 * @param lastModified The last modification date to set or -1 to use the current date.
	 * @return the input stream that may be used to read the data from.
	 * @throws IOException In case of the input stream cannot be opened.
	 */
    public static OutputStream openOutputStream(URL url, long lastModified) throws IOException {
        if (FILE_SCHEME.equalsIgnoreCase(url.getProtocol())) {
            try {
                return new PreservingFileOutputStream(new File(url.toURI()), lastModified);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        } else {
            URLConnection uc = url.openConnection();
            uc.setDoInput(false);
            uc.setDoOutput(true);
            uc.connect();
            return uc.getOutputStream();
        }
    }

    /**
	 * Extracts the path form the given uri string.
	 *
	 * @param uri	  The uri to get the path from.
	 * @param fileOnly Sets, whether the full path or only the filename should be returned.
	 * @return The extracted path.
	 */
    public static String extractPath(String uri, boolean fileOnly) {
        int p = uri.indexOf('?');
        if (p != -1) uri = uri.substring(0, p);
        p = uri.indexOf('#');
        if (p != -1) uri = uri.substring(0, p);
        if (fileOnly) {
            if (uri.endsWith("/")) uri = uri.substring(0, uri.length() - 1);
            p = uri.lastIndexOf('/');
            if (p != -1) uri = uri.substring(p + 1);
        } else uri = URI.create(uri).getPath();
        return uri;
    }

    /**
	 * Removes double slashes, converts backslashes and normalized relative paths.
	 *
	 * @param sourcePath	  The path to normalize.
	 * @param startsWithSlash Whether the normalized path should start with a slash.
	 * @param endsWithSlash   Whether the normalized path should end with a slash.
	 * @return The normalized path.
	 */
    public static String normalizePath(String sourcePath, boolean startsWithSlash, boolean endsWithSlash) {
        if (sourcePath != null && !sourcePath.isEmpty()) {
            Deque<String> pathElements = new ArrayDeque<String>(16);
            StringTokenizer t = new StringTokenizer(sourcePath, "\\/");
            while (t.hasMoreTokens()) {
                String token = t.nextToken();
                if (".".equals(token)) continue;
                if ("..".equals(token)) {
                    if (!pathElements.isEmpty() && !token.equals(pathElements.getFirst())) pathElements.poll(); else pathElements.push(token);
                } else pathElements.push(token);
            }
            StringBuffer normalized = new StringBuffer(sourcePath.length());
            if (startsWithSlash) normalized.append('/');
            for (Iterator<String> i = pathElements.descendingIterator(); i.hasNext(); ) {
                normalized.append(i.next());
                if (endsWithSlash || i.hasNext()) normalized.append('/');
            }
            sourcePath = normalized.toString();
        }
        return sourcePath;
    }

    /**
	 * Transfers the source into the target stream.
	 *
	 * @param source  the source url.
	 * @param target  the target url.
	 * @param ifNewer Whether to do it when the target is newer only.
	 * @throws java.io.IOException In case of the transfer fails.
	 */
    public static void transfer(URI source, URI target, boolean ifNewer) throws IOException {
        long sourceLastModified = getLastModified(source);
        long targetLastModified = ifNewer ? getLastModified(target) : -1;
        if (sourceLastModified > targetLastModified) transfer(openInputStream(source), openOutputStream(target, sourceLastModified), true);
    }

    /**
	 * Transfers the source into the target stream.
	 *
	 * @param source  the source url.
	 * @param target  the target url.
	 * @param ifNewer Whether to do it when the target is newer only.
	 * @throws java.io.IOException In case of the transfer fails.
	 */
    public static void transfer(URL source, URL target, boolean ifNewer) throws IOException {
        long sourceLastModified = getLastModified(source);
        long targetLastModified = ifNewer ? getLastModified(target) : -1;
        if (sourceLastModified > targetLastModified) transfer(openInputStream(source), openOutputStream(target, sourceLastModified), true);
    }

    /**
	 * Transfers the source into the target stream.
	 *
	 * @param source the source stream.
	 * @param target the target stream.
	 * @param close  Whether the stream should be closed or not.
	 * @throws java.io.IOException In case of the transfer fails.
	 */
    public static void transfer(InputStream source, OutputStream target, boolean close) throws IOException {
        try {
            int r = 0, read = 0;
            byte[] buf = new byte[TRANSFER_BUFFER_SIZE];
            do {
                while (r < buf.length) {
                    read = source.read(buf, r, buf.length - r);
                    if (read == -1) break;
                    r += read;
                }
                target.write(buf, 0, r);
                r = 0;
            } while (read != -1);
        } finally {
            if (close) {
                target.close();
                source.close();
            }
        }
    }

    private IOUtils() {
    }
}
