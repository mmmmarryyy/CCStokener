package org.makagiga.commons;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;
import org.makagiga.commons.annotation.ConfigEntry;
import org.makagiga.commons.annotation.Uninstantiable;
import org.makagiga.commons.cache.FileCache;

public final class Net {

    /**
	 * @since 2.0
	 */
    public static final int DOWNLOAD_NO_CACHE_UPDATE = 1;

    /**
	 * @since 2.0
	 */
    public static final int DOWNLOAD_USE_CACHE = 1 << 1;

    /**
	 * Accept "gzip,deflate,compress" encoding.
	 *
	 * @since 2.0
	 */
    public static final int SETUP_COMPRESS = 1 << 2;

    /**
	 * Accept "pack200-gzip" encoding.
	 * 
	 * @since 2.0
	 */
    public static final int SETUP_PACK200_GZIP = 1 << 3;

    /**
	 * @since 4.2
	 */
    @ConfigEntry("Net.connectTimeout")
    public static final IntegerProperty connectTimeout = new IntegerProperty(30000, IntegerProperty.SECURE_WRITE);

    /**
	 * @since 4.2
	 */
    @ConfigEntry("Net.readTimeout")
    public static final IntegerProperty readTimeout = new IntegerProperty(30000, IntegerProperty.SECURE_WRITE);

    /**
	 * @since 4.0
	 */
    public static final URI ABOUT_BLANK = URI.create("about:blank");

    /**
	 * Checks for the @c HttpURLConnection.HTTP_MOVED_TEMP (302) HTTP Status-Code.
	 * Reopens and returns a new connection using the new "Location" field if necessary.
	 *
	 * @param connection the URL connection
	 * @param flags the flags used by @c setupConnection
	 *
	 * @since 3.6
	 */
    @edu.umd.cs.findbugs.annotation.SuppressWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
    public static URLConnection checkTemporaryRedirect(final URLConnection connection, final int flags) throws IOException {
        if (!(connection instanceof HttpURLConnection)) return connection;
        HttpURLConnection http = (HttpURLConnection) connection;
        if (http.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            http.disconnect();
            String newLocation = connection.getHeaderField("Location");
            if (newLocation == null) return connection;
            URLConnection result = new URL(newLocation).openConnection();
            setupConnection(result, flags);
            return result;
        } else {
            return connection;
        }
    }

    /**
	 * Try to fix malformed URI
	 * by escaping illegal characters.
	 *
	 * @since 3.8.3
	 */
    public static URI fixURI(final String uriString) {
        StringBuilder s = new StringBuilder(uriString);
        for (int i = 0; i < 10; i++) {
            try {
                return URI.create(s.toString());
            } catch (IllegalArgumentException runtimeException) {
                URISyntaxException exception = (URISyntaxException) runtimeException.getCause();
                int pos = exception.getIndex();
                if (pos == -1) throw runtimeException;
                String reason = exception.getReason();
                if ((reason != null) && reason.startsWith("Illegal character in fragment") && (s.charAt(pos) == '#')) {
                    s.deleteCharAt(pos);
                    continue;
                }
                if ((reason != null) && !reason.startsWith("Illegal character")) throw runtimeException;
                char illegalChar = s.charAt(pos);
                s.deleteCharAt(pos);
                s.insert(pos, TK.escapeURL(Character.toString(illegalChar)));
            }
        }
        throw new IllegalArgumentException(new URISyntaxException(uriString, "Malformed link address"));
    }

    /**
	 * @since 2.0
	 */
    public static InputStream getInputStream(final URLConnection connection) throws IOException {
        String contentEncoding = connection.getContentEncoding();
        if (contentEncoding != null) {
            if (contentEncoding.contains("compress")) {
                return new ZipInputStream(connection.getInputStream());
            } else if (contentEncoding.contains("gzip")) {
                return new GZIPInputStream(connection.getInputStream());
            } else if (contentEncoding.contains("deflate")) {
                return new InflaterInputStream(connection.getInputStream());
            } else if (contentEncoding.contains("pack200-gzip")) {
                return new GZIPInputStream(connection.getInputStream());
            }
        }
        return connection.getInputStream();
    }

    public static boolean isHTTP(final String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
	 * @since 4.0
	 */
    public static boolean isLocalFile(final String location) {
        return location.startsWith("file:/") || TK.startsWith(location, '/');
    }

    /**
	 * @since 1.2
	 */
    public static void setupConnection(final URLConnection connection, final int flags) {
        Flags f = Flags.valueOf(flags);
        String acceptEncoding = null;
        if (f.isSet(SETUP_COMPRESS)) acceptEncoding = "gzip,deflate,compress";
        if (f.isSet(SETUP_PACK200_GZIP)) {
            if (acceptEncoding == null) acceptEncoding = "pack200-gzip"; else acceptEncoding += ",pack200-gzip";
        }
        if (acceptEncoding != null) connection.setRequestProperty("Accept-Encoding", acceptEncoding);
        int min = 1000;
        int max = 1000 * 300;
        connection.setConnectTimeout(connectTimeout.get(min, max));
        connection.setReadTimeout(readTimeout.get(min, max));
    }

    /**
	 * @since 3.0
	 */
    public static void setupOutputProperties(final URLConnection connection, final String contentType, final long contentLength) {
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-type", contentType);
        connection.setRequestProperty("Content-length", Long.toString(contentLength));
    }

    @Uninstantiable
    private Net() {
    }

    /**
	 * @since 2.0
	 */
    public static final class DownloadInfo {

        private Class<? extends InputStream> inputType;

        private File destinationFile;

        private File file;

        private FS.ProgressListener progressListener;

        private InputStream input;

        private int flags;

        private long lastModified;

        private long length = -1;

        private static final MLogger log = MLogger.get("download");

        private OutputStream output;

        private final String cacheSuffix;

        private final URL url;

        private URLConnection connection;

        /**
		 * @since 3.4
		 */
        public DownloadInfo(final URL url, final String cacheSuffix, final int flags) {
            this.url = url;
            this.cacheSuffix = cacheSuffix;
            this.flags = flags;
        }

        public void cancelDownload() {
            shutDownConnection();
            FileCache.getDownloadGroup().remove(url);
            if (file != null) {
                file.delete();
                file = null;
            }
        }

        public String getCacheSuffix() {
            return cacheSuffix;
        }

        public URLConnection getConnection() {
            return connection;
        }

        /**
		 * @since 3.4
		 */
        public File getDestinationFile() {
            return destinationFile;
        }

        /**
		 * @since 3.4
		 */
        public void setDestinationFile(final File value) {
            destinationFile = value;
        }

        public File getFile() {
            return file;
        }

        public Class<? extends InputStream> getInputType() {
            return inputType;
        }

        public long getLastModified() {
            return lastModified;
        }

        public long getLength() {
            return length;
        }

        public URL getURL() {
            return url;
        }

        public void setProgressListener(final FS.ProgressListener value) {
            progressListener = value;
        }

        public void shutDownConnection() {
            try {
                FS.close(input);
            } catch (NullPointerException exception) {
                MLogger.exception(exception);
            }
            FS.close(output);
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection.class.cast(connection).disconnect();
                connection = null;
            }
        }

        /**
		 * @since 3.4
		 */
        public void startDownload() throws IOException {
            Flags f = Flags.valueOf(flags);
            FileCache.Group cache = f.isSet(DOWNLOAD_NO_CACHE_UPDATE | DOWNLOAD_USE_CACHE) ? FileCache.getDownloadGroup() : null;
            try {
                if (f.isSet(DOWNLOAD_NO_CACHE_UPDATE)) {
                    file = cache.getFile(url, FileCache.IGNORE_DATE);
                    if (file != null) {
                        length = file.length();
                        if (destinationFile != null) FS.copyFile(file, destinationFile);
                        return;
                    }
                }
                connection = url.openConnection();
                setupConnection(connection, f.intValue());
                connection = checkTemporaryRedirect(connection, f.intValue());
                lastModified = connection.getLastModified();
                if (f.isSet(DOWNLOAD_USE_CACHE)) {
                    file = cache.getFile(url, lastModified);
                    if (file != null) {
                        log.debugFormat("Using file from cache: %s", file);
                        length = file.length();
                        if (destinationFile != null) FS.copyFile(file, destinationFile);
                        return;
                    }
                    if (lastModified == FileCache.NO_DATE) file = cache.newPath(url, FileCache.IGNORE_DATE, cacheSuffix).toFile(); else file = cache.newPath(url, lastModified, cacheSuffix).toFile();
                    log.debugFormat("Creating new cache entry: %s", file);
                } else {
                    file = File.createTempFile("download", cacheSuffix);
                }
                log.debugFormat("Downloading \"%s\"...", url);
                input = getInputStream(connection);
                inputType = input.getClass();
                input = new BufferedInputStream(input);
                output = new FS.BufferedFileOutput(file);
                length = connection.getContentLengthLong();
                FS.copyStream(input, output, FS.COPY_BUF_LENGTH, length, progressListener);
                FS.close(output);
                if (destinationFile != null) FS.copyFile(file, destinationFile);
            } catch (IOException exception) {
                if (cache != null) cache.remove(url);
                throw exception;
            } catch (Exception exception) {
                if (cache != null) cache.remove(url);
                throw new IOException(exception);
            } finally {
                shutDownConnection();
            }
        }
    }
}
