package org.eclipse.emf.common.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.eclipse.emf.common.util.URI;

/**
 * A connection that can access an entry in an archive, and then recursively an entry in that archive, and so on.
 * For example, it can be used just like jar: or zip:, only the archive paths can repeat, e.g.,
 *<pre>
 *  archive:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
 *</pre>
 * The general recursive pattern is
 *<pre>
 *  archive:$nestedURL${/!$archivePath$}+
 *</pre>
 * So the nested URL for the example above is
 *<pre>
 *  file:///c:/temp/example.zip
 *</pre>
 * 
 * <p>
 * Since the nested URL may itself contain archive schemes,
 * the subsequence of the archive paths that should be associated with the nested URL 
 * is determined by finding the nth archive separator, i.e., the nth !/, 
 * where n is the number of ":"s before the first "/" of the nested URL, i.e., the number of nested schemes.
 * For example, for a more complex case where the nested URL is itself an archive-based scheme, e.g.,
 *<pre>
 *  archive:jar:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
 *</pre>
 * the nested URL is correctly parsed to skip to the second archive separator as
 *<pre>
 *  jar:file:///c:/temp/example.zip!/org/example/nested.zip
 *</pre>
 * </p>
 *
 * <p>
 * The logic for accessing archives can be tailored and reused independant from its usage as a URL connection.
 * This is normally done by using the constructor {@link #ArchiveURLConnection(String)}
 * and overriding {@link #createInputStream(String)} and {@link #createOutputStream(String)}.
 * The behavior can be tailored by overriding {@link #emulateArchiveScheme()} and {@link #useZipFile()}.
 * </p>
 */
public class ArchiveURLConnection extends URLConnection {

    /**
   * The cached string version of the {@link #url URL}.
   */
    protected String urlString;

    /**
   * Constructs a new connection for the URL.
   * @param url the URL of this connection.
   */
    public ArchiveURLConnection(URL url) {
        super(url);
        urlString = url.toString();
    }

    /**
   * Constructs a new archive accessor.
   * This constructor forwards a null URL to be super constructor, 
   * so an instance built with this constructor <b>cannot</b> be used as a URLConnection.
   * The logic for accessing archives and for delegating to the nested URL can be reused in other applications,
   * without creating an URLs.
   * @param url the URL of the archive.
   */
    protected ArchiveURLConnection(String url) {
        super(null);
        urlString = url;
    }

    /**
   * </p>
   * Returns whether the implementation will handle all the archive accessors directly.
   * For example, whether
   *<pre>
   *  archive:jar:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
   *</pre>
   * will be handled as if it were specified as
   *<pre>
   *  archive:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
   *</pre>
   * Override this only if you are reusing the logic of retrieving an input stream into an archive 
   * and hence are likely to be overriding createInputStream, 
   * which is the point of delegation to the nested URL for recursive stream creation.
   * </p>
   * @return whether the implementation will handle all the archive accessors directly.
   */
    protected boolean emulateArchiveScheme() {
        return false;
    }

    /**
   * Returns whether to handle the special case of a nested URL with file: schema using a {@link ZipFile}.
   * This gives more efficient direct access to the root entry, e.g., 
   *<pre>
   *  archive:file:///c:/temp/example.zip!/org/example/nested.html
   *</pre>
   * @return whether to handle the special case of a nested URL with file: schema using a ZipFile.
   */
    protected boolean useZipFile() {
        return false;
    }

    /**
   * Record that this is connected.
   */
    @Override
    public void connect() throws IOException {
        connected = true;
    }

    protected String getNestedURL() throws IOException {
        int archiveSeparator = urlString.indexOf("!/");
        if (archiveSeparator < 0) {
            throw new MalformedURLException("missing archive separators " + urlString);
        }
        int start = urlString.indexOf(':') + 1;
        if (start > urlString.length() || urlString.charAt(start) == '/') {
            throw new IllegalArgumentException("archive protocol must be immediately followed by another URL protocol " + urlString);
        }
        for (int i = start, end = urlString.indexOf("/") - 1; (i = urlString.indexOf(":", i)) < end; ) {
            if (emulateArchiveScheme()) {
                start = ++i;
            } else {
                archiveSeparator = urlString.indexOf("!/", archiveSeparator + 2);
                if (archiveSeparator < 0) {
                    throw new MalformedURLException("too few archive separators " + urlString);
                }
                ++i;
            }
        }
        return urlString.substring(start, archiveSeparator);
    }

    /**
   * Creates the input stream for the URL.
   * @return the input stream for the URL.
   */
    @Override
    public InputStream getInputStream() throws IOException {
        String nestedURL = getNestedURL();
        int archiveSeparator = urlString.indexOf(nestedURL) + nestedURL.length();
        int nextArchiveSeparator = urlString.indexOf("!/", archiveSeparator + 2);
        InputStream inputStream;
        ZipEntry inputZipEntry = null;
        if (!useZipFile() || !nestedURL.startsWith("file:")) {
            inputStream = createInputStream(nestedURL);
        } else {
            String entry = URI.decode(nextArchiveSeparator < 0 ? urlString.substring(archiveSeparator + 2) : urlString.substring(archiveSeparator + 2, nextArchiveSeparator));
            archiveSeparator = nextArchiveSeparator;
            nextArchiveSeparator = urlString.indexOf("!/", archiveSeparator + 2);
            final ZipFile zipFile = new ZipFile(URI.decode(nestedURL.substring(5)));
            inputZipEntry = zipFile.getEntry(entry);
            InputStream zipEntryInputStream = inputZipEntry == null ? null : zipFile.getInputStream(inputZipEntry);
            if (zipEntryInputStream == null) {
                throw new IOException("Archive entry not found " + urlString);
            }
            inputStream = new FilterInputStream(zipEntryInputStream) {

                @Override
                public void close() throws IOException {
                    super.close();
                    zipFile.close();
                }
            };
        }
        LOOP: while (archiveSeparator > 0) {
            inputZipEntry = null;
            String entry = URI.decode(nextArchiveSeparator < 0 ? urlString.substring(archiveSeparator + 2) : urlString.substring(archiveSeparator + 2, nextArchiveSeparator));
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            while (zipInputStream.available() >= 0) {
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                if (zipEntry == null) {
                    break;
                } else if (entry.equals(zipEntry.getName())) {
                    inputZipEntry = zipEntry;
                    inputStream = zipInputStream;
                    archiveSeparator = nextArchiveSeparator;
                    nextArchiveSeparator = urlString.indexOf("!/", archiveSeparator + 2);
                    continue LOOP;
                }
            }
            zipInputStream.close();
            throw new IOException("Archive entry not found " + urlString);
        }
        return yield(inputZipEntry, inputStream);
    }

    protected InputStream yield(ZipEntry zipEntry, InputStream inputStream) throws IOException {
        return inputStream;
    }

    /**
   * Creates an input stream for the nested URL by calling {@link URL#openStream() opening} a stream on it.
   * @param nestedURL the nested URL for which a stream is required.
   * @return the open stream of the nested URL.
   */
    protected InputStream createInputStream(String nestedURL) throws IOException {
        return new URL(nestedURL).openStream();
    }

    /**
   * Creates the output stream for the URL.
   * @return the output stream for the URL.
   */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return getOutputStream(false, -1);
    }

    public void delete() throws IOException {
        getOutputStream(true, -1).close();
    }

    public void setTimeStamp(long timeStamp) throws IOException {
        getOutputStream(false, timeStamp).close();
    }

    private OutputStream getOutputStream(boolean delete, long timeStamp) throws IOException {
        final String nestedURL = getNestedURL();
        final File tempFile = File.createTempFile("Archive", "zip");
        InputStream sourceInputStream = null;
        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new FileOutputStream(tempFile);
            try {
                sourceInputStream = createInputStream(nestedURL);
            } catch (IOException exception) {
            }
            OutputStream outputStream = tempOutputStream;
            InputStream inputStream = sourceInputStream;
            int archiveSeparator = urlString.indexOf(nestedURL) + nestedURL.length();
            int nextArchiveSeparator = urlString.indexOf("!/", archiveSeparator + 2);
            ZipOutputStream zipOutputStream;
            final byte[] bytes = new byte[4096];
            ZipEntry outputZipEntry;
            boolean found = false;
            for (; ; ) {
                String entry = URI.decode(nextArchiveSeparator < 0 ? urlString.substring(archiveSeparator + 2) : urlString.substring(archiveSeparator + 2, nextArchiveSeparator));
                zipOutputStream = null;
                ZipInputStream zipInputStream = inputStream == null ? null : new ZipInputStream(inputStream);
                inputStream = zipInputStream;
                while (zipInputStream != null && zipInputStream.available() >= 0) {
                    ZipEntry zipEntry = zipInputStream.getNextEntry();
                    if (zipEntry == null) {
                        break;
                    } else {
                        boolean match = entry.equals(zipEntry.getName());
                        if (!found) {
                            found = match && nextArchiveSeparator < 0;
                        }
                        if (timeStamp != -1 || !match) {
                            if (zipOutputStream == null) {
                                zipOutputStream = new ZipOutputStream(outputStream);
                                outputStream = zipOutputStream;
                            }
                            if (timeStamp != -1 && match && nextArchiveSeparator < 0) {
                                zipEntry.setTime(timeStamp);
                            }
                            zipOutputStream.putNextEntry(zipEntry);
                            for (int size; (size = zipInputStream.read(bytes, 0, bytes.length)) > -1; ) {
                                zipOutputStream.write(bytes, 0, size);
                            }
                        }
                    }
                }
                archiveSeparator = nextArchiveSeparator;
                nextArchiveSeparator = urlString.indexOf("!/", archiveSeparator + 2);
                if ((delete || timeStamp != -1) && archiveSeparator < 0) {
                    if (!found) {
                        throw new IOException("Archive entry not found " + urlString);
                    }
                    outputZipEntry = null;
                    break;
                } else {
                    outputZipEntry = new ZipEntry(entry);
                    if (zipOutputStream == null) {
                        zipOutputStream = new ZipOutputStream(outputStream);
                        outputStream = zipOutputStream;
                    }
                    zipOutputStream.putNextEntry(outputZipEntry);
                    if (archiveSeparator > 0) {
                        continue;
                    } else {
                        break;
                    }
                }
            }
            tempOutputStream = null;
            final boolean deleteRequired = sourceInputStream != null;
            FilterOutputStream result = new FilterOutputStream(zipOutputStream == null ? outputStream : zipOutputStream) {

                protected boolean isClosed;

                @Override
                public void close() throws IOException {
                    if (!isClosed) {
                        isClosed = true;
                        super.close();
                        boolean useRenameTo = nestedURL.startsWith("file:");
                        if (useRenameTo) {
                            File targetFile = new File(URI.decode(nestedURL.substring(5)));
                            if (deleteRequired && !targetFile.delete()) {
                                throw new IOException("cannot delete " + targetFile.getPath());
                            } else if (!tempFile.renameTo(targetFile)) {
                                useRenameTo = false;
                            }
                        }
                        if (!useRenameTo) {
                            InputStream inputStream = null;
                            OutputStream outputStream = null;
                            try {
                                inputStream = new FileInputStream(tempFile);
                                outputStream = createOutputStream(nestedURL);
                                for (int size; (size = inputStream.read(bytes, 0, bytes.length)) > -1; ) {
                                    outputStream.write(bytes, 0, size);
                                }
                            } finally {
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (outputStream != null) {
                                    outputStream.close();
                                }
                            }
                        }
                    }
                }
            };
            return outputZipEntry == null ? result : yield(outputZipEntry, result);
        } finally {
            if (tempOutputStream != null) {
                tempOutputStream.close();
            }
            if (sourceInputStream != null) {
                sourceInputStream.close();
            }
        }
    }

    protected OutputStream yield(ZipEntry zipEntry, OutputStream outputStream) throws IOException {
        return outputStream;
    }

    /**
   * Creates an output stream for the nested URL by calling {@link URL#openConnection() opening} a stream on it.
   * @param nestedURL the nested URL for which a stream is required.
   * @return the open stream of the nested URL.
   */
    protected OutputStream createOutputStream(String nestedURL) throws IOException {
        URL url = new URL(nestedURL);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        return urlConnection.getOutputStream();
    }
}
