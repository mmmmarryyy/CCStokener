package n3_project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import eulergui.project.N3Source;
import eulergui.project.Project;
import eulergui.util.ReaderUtils;
import eulergui.util.URLHelper;

/** IO Manager for N3 sources */
public class IOManager {

    static final String N3_EXT = ".n3";

    public static void fillFileWithContent(File f, String content) throws FileNotFoundException {
        final PrintWriter pw = new PrintWriter(f);
        pw.append(content);
        pw.close();
    }

    public static InputStream getInputStream(URL location) throws IOException {
        if (URLHelper.isLocal(location)) {
            File file;
            try {
                if (location.toURI().isAbsolute()) {
                    file = new File(location.toURI());
                } else {
                    file = new File(location.toString());
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                return new FileInputStream(file);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        } else {
            return location.openStream();
        }
        return null;
    }

    public static InputStream getInputStream(URI location) throws IOException {
        if (URLHelper.isLocal(location.toString()) || !location.isAbsolute()) {
            File file;
            try {
                if (location.isAbsolute()) {
                    file = new File(location);
                } else {
                    file = new File(location.toString());
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                return new FileInputStream(file);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        } else {
            return location.toURL().openStream();
        }
        return null;
    }

    public static InputStream getInputStream(N3Source n3) throws IOException {
        return getInputStream(n3.getLocation());
    }

    public static InputStream getInputStream(Project project) throws IOException {
        return getInputStream(project.getLocation());
    }

    public static OutputStream getOutputStream(Project project) throws IOException {
        return getOutputStream(project.getLocation());
    }

    public static OutputStream getOutputStream(N3Source n3) throws IOException {
        return getOutputStream(n3.getLocation());
    }

    public static OutputStream getOutputStream(URL url) throws IOException {
        try {
            if (url.getProtocol().equals("file")) {
                return new FileOutputStream(new File(url.toURI()));
            } else {
                final URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                return connection.getOutputStream();
            }
        } catch (final URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getOutputFile(URL url) throws IOException {
        if (url.getProtocol().equals("file")) {
            try {
                return new File(url.toURI());
            } catch (final URISyntaxException e) {
                throw new RuntimeException("getOutputFile", e);
            }
        } else {
            throw new UnsupportedOperationException("Project can only be saved locally");
        }
    }

    public static File getOutputFileN3P(URL url) {
        File n3PProjectFile = null;
        if (url.getProtocol().equals("file")) {
            try {
                final File projectFile = new File(url.toURI());
                n3PProjectFile = new File(projectFile.getAbsolutePath() + N3_EXT);
            } catch (final URISyntaxException e) {
                throw new RuntimeException("getOutputFile", e);
            }
        } else {
            throw new UnsupportedOperationException("Project can only be saved locally");
        }
        return n3PProjectFile;
    }

    /** Create a Temporary File With given Suffix */
    public static File getTemporaryFileWithSuffix(String suffix) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile("eg-", suffix);
            tmpFile.deleteOnExit();
            Logger.getLogger("theDefault").fine("IOManager.getTemporaryFileWithSuffix()");
            return tmpFile;
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Panic: unable to create a TemporaryFile!");
        }
    }

    /** Create a Temporary N3 File */
    public static File getTemporaryN3File() {
        return getTemporaryFileWithSuffix(N3_EXT);
    }

    /** get new cache file in temporary directory */
    public static File getCacheFile() {
        try {
            return File.createTempFile("eg-", null);
        } catch (final IOException e) {
            throw new RuntimeException("Panic: getCacheFile: unable to create a TemporaryFile!");
        }
    }

    /** get the Local N3 Cache File, or the file itself if it is Local and native N3;
     * if it does not exist already, create the local Cache File;
     * the Local N3 Cache File is the translation into N3 in the case of a non native N3 source
     *
     * only called in N3Source */
    public static File getLocalCache(N3Source n3Source) {
        return N3Source.manageN3Cache(n3Source);
    }

    /** download if necessary, that is, if not local N3 source
	 * @param n3Source
	 */
    public static void downloadN3ToLocalCache(N3Source n3Source, File file) {
        final URL url = n3Source.getLocation();
        if (!URLHelper.isLocal(url)) {
            downloadURLToLocalCache(url, file);
        }
    }

    public static int downloadURLToLocalCache(URL url, File file) {
        try {
            int trialCount = 0;
            while (trialCount < 5) {
                InputStream inputStream = null;
                try {
                    trialCount++;
                    final URLConnection openConnection = new SourceFactory().makeN3URLConnection(url);
                    inputStream = openConnection.getInputStream();
                    int returnVal = ReaderUtils.copyReader(new InputStreamReader(inputStream), new FileWriter(file));
                    return returnVal;
                } catch (java.net.SocketException se) {
                    System.err.println("IOManager.downloadURLToLocalCache(): " + se + "\n\tretry: " + url);
                } catch (java.net.SocketTimeoutException se) {
                    System.err.println("IOManager.downloadURLToLocalCache(): " + se + "\n\tretry: " + url);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }
        } catch (final IOException e) {
            String message = "downloadURLToLocalCache: " + url + " ==> " + file;
            System.err.println(message);
            e.printStackTrace();
            System.err.println("downloadURLToLocalCache: throw new RuntimeException " + e.getMessage());
            throw new RuntimeException(message, e);
        }
        return -1;
    }

    public static File downloadURLToLocalCache(URL url) {
        final File temporaryFile = getCacheFile();
        downloadURLToLocalCache(url, temporaryFile);
        return temporaryFile;
    }
}
