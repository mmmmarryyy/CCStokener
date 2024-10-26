package oracle.toplink.essentials.ejb.cmp3.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class is written to deal with various URLs that can be returned by
 * {@link javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl()}
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ArchiveFactoryImpl {

    private Logger logger;

    public ArchiveFactoryImpl() {
        this(Logger.global);
    }

    public ArchiveFactoryImpl(Logger logger) {
        this.logger = logger;
    }

    public Archive createArchive(URL url) throws URISyntaxException, IOException {
        logger.entering("ArchiveFactoryImpl", "createArchive", new Object[] { url });
        Archive result;
        String protocol = url.getProtocol();
        logger.logp(Level.FINER, "ArchiveFactoryImpl", "createArchive", "protocol = {0}", protocol);
        if ("file".equals(protocol)) {
            URI uri = null;
            try {
                uri = url.toURI();
            } catch (URISyntaxException exception) {
                uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
            }
            File f = new File(uri);
            if (f.isDirectory()) {
                result = new DirectoryArchive(f);
            } else {
                result = new JarFileArchive(new JarFile(f));
            }
        } else if ("jar".equals(protocol)) {
            JarURLConnection conn = JarURLConnection.class.cast(url.openConnection());
            JarEntry je = conn.getJarEntry();
            if (je == null) {
                result = new JarFileArchive(conn.getJarFile());
            } else if (je.isDirectory()) {
                result = new DirectoryInsideJarURLArchive(url);
            } else {
                result = new JarInputStreamURLArchive(url);
            }
        } else if (isJarInputStream(url)) {
            result = new JarInputStreamURLArchive(url);
        } else {
            result = new URLArchive(url);
        }
        logger.exiting("ArchiveFactoryImpl", "createArchive", result);
        return result;
    }

    /**
     * This method is called for a URL which has neither jar nor file protocol.
     * This attempts to find out if we can treat it as a URL from which a JAR
     * format InputStream can be obtained.
     * @param url
     */
    private boolean isJarInputStream(URL url) throws IOException {
        InputStream in = null;
        try {
            in = url.openStream();
            if (in == null) {
                return false;
            }
            JarInputStream jis = new JarInputStream(in);
            jis.close();
            return true;
        } catch (IOException ioe) {
            if (in != null) {
                in.close();
            }
            return false;
        }
    }
}
