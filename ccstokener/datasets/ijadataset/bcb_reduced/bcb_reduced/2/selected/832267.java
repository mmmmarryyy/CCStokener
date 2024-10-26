package org.dwgsoftware.raistlin.repository.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.naming.directory.Attributes;
import org.dwgsoftware.raistlin.repository.Artifact;
import org.dwgsoftware.raistlin.repository.Repository;
import org.dwgsoftware.raistlin.repository.RepositoryException;
import org.dwgsoftware.raistlin.repository.meta.FactoryDescriptor;
import org.dwgsoftware.raistlin.repository.provider.Builder;
import org.dwgsoftware.raistlin.repository.provider.Factory;
import org.dwgsoftware.raistlin.repository.provider.InitialContext;
import org.dwgsoftware.raistlin.repository.provider.RepositoryCriteria;
import org.dwgsoftware.raistlin.repository.util.LoaderUtils;
import org.dwgsoftware.raistlin.repository.util.RepositoryUtils;

/**
 * Sets up the environment to create repositories by downloading the required 
 * jars, preparing a ClassLoader and delegating calls to repository factory 
 * methods using the newly configured ClassLoader.
 * 
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $
 */
public class DefaultInitialContext extends AbstractBuilder implements InitialContext {

    /**
    * Group identifier manifest key.
    */
    public static final String BLOCK_GROUP_KEY = "Block-Group";

    /** 
    * The application key.
    */
    private final String m_key;

    /** 
    * The instantiated delegate cache manager factory.
    */
    private final Factory m_factory;

    /**
    * The initial cache directory.
    */
    private final File m_cache;

    /**
    * The base working directory.
    */
    private final File m_base;

    /**
    * System repository established by the intial context.
    */
    private final Repository m_repository;

    private final LoaderUtils m_loader;

    /**
    * The online connection policy.
    */
    private boolean m_online;

    /**
    * The initial remote host names.
    */
    private String[] m_hosts;

    /**
     * Creates an initial repository context.
     *
     * @param parent the parent classloader
     * @param artifact an artifact referencing the default implementation
     * @param candidates factory artifact sequence for registration
     * @param base the base working directory
     * @param cache the cache directory
     * @param hosts a set of initial remote repository addresses 
     * @throws RepositoryException if an error occurs during establishment
     */
    DefaultInitialContext(String key, ClassLoader parent, Artifact artifact, Artifact[] candidates, File base, File cache, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String[] hosts, boolean online) throws RepositoryException {
        if (null == key) throw new NullPointerException("key");
        if (null == base) throw new NullPointerException("base");
        if (null == parent) throw new NullPointerException("parent");
        if (null == artifact) throw new NullPointerException("artifact");
        if (null == cache) throw new NullPointerException("cache");
        if (null == hosts) throw new NullPointerException("hosts");
        if (null == candidates) throw new NullPointerException("candidates");
        m_key = key;
        m_base = base;
        m_cache = cache;
        m_online = online;
        m_hosts = hosts;
        m_loader = new LoaderUtils(m_online);
        setupProxy(proxyHost, proxyPort, proxyUsername, proxyPassword);
        Attributes attributes = loadAttributes(m_cache, m_hosts, artifact);
        FactoryDescriptor descriptor = new FactoryDescriptor(attributes);
        String factory = descriptor.getFactory();
        if (null == factory) {
            final String error = "Required property 'raistlin.artifact.factory' not present in artifact: " + artifact + " under the active cache: [" + m_cache + "] using the " + "attribute sequence: " + attributes;
            throw new IllegalArgumentException(error);
        }
        Artifact[] dependencies = descriptor.getDependencies();
        int n = dependencies.length;
        URL[] urls = new URL[n + 1];
        for (int i = 0; i < n; i++) {
            urls[i] = m_loader.getResource(dependencies[i], m_hosts, m_cache, true);
        }
        urls[n] = m_loader.getResource(artifact, m_hosts, m_cache, true);
        ClassLoader classloader = new URLClassLoader(urls, parent);
        Class clazz = loadFactoryClass(classloader, factory);
        try {
            m_factory = createDelegate(classloader, clazz, this);
            RepositoryCriteria criteria = (RepositoryCriteria) m_factory.createDefaultCriteria();
            criteria.setCacheDirectory(m_cache);
            criteria.setHosts(m_hosts);
            criteria.setOnlineMode(online);
            criteria.setFactoryArtifacts(candidates);
            m_repository = (Repository) m_factory.create(criteria);
        } catch (Throwable e) {
            final String error = "Unable to establish a factory for the supplied artifact:";
            StringBuffer buffer = new StringBuffer(error);
            buffer.append("\n artifact: " + artifact);
            buffer.append("\n build: " + descriptor.getBuild());
            buffer.append("\n factory: " + descriptor.getFactory());
            buffer.append("\n source: " + clazz.getProtectionDomain().getCodeSource().getLocation());
            buffer.append("\n cache: " + m_cache);
            throw new RepositoryException(buffer.toString(), e);
        }
    }

    private void setupProxy(final String host, final int port, final String username, final String password) {
        if (null == host) return;
        Properties system = System.getProperties();
        system.put("proxySet", "true");
        system.put("proxyHost", host);
        system.put("proxyPort", String.valueOf(port));
        if (null != username) {
            Authenticator authenticator = new DefaultAuthenticator(username, password);
            Authenticator.setDefault(authenticator);
        }
    }

    /**
    * Return the inital repository.
    * @return the repository
    */
    public Repository getRepository() {
        return m_repository;
    }

    /**
    * Get the online mode of the repository.
    *
    * @return the online mode
    */
    public boolean getOnlineMode() {
        return m_online;
    }

    /**
     * Return the application key.  The value of the key may be used 
     * to resolve property files by using the convention 
     * [key].properties.
     * 
     * @return the application key.
     */
    public String getApplicationKey() {
        return m_key;
    }

    /**
     * Return the base working directory.
     * 
     * @return the base directory
     */
    public File getInitialWorkingDirectory() {
        return m_base;
    }

    /**
     * Return cache root directory.
     * 
     * @return the cache directory
     */
    public File getInitialCacheDirectory() {
        return m_cache;
    }

    /**
     * Return the initial set of host names.
     * @return the host names sequence
     */
    public String[] getInitialHosts() {
        return m_hosts;
    }

    /**
    * Return the initial repository factory.
    * @return the initial repository factory
    */
    public Factory getInitialFactory() {
        return m_factory;
    }

    /**
    * Create a factory builder using a supplied artifact.
    * @param artifact the factory artifact
    * @return the factory builder
    * @exception Exception if a builder creation error occurs
    */
    public Builder newBuilder(Artifact artifact) throws Exception {
        return new DefaultBuilder(this, artifact);
    }

    /**
    * Create a factory builder using a supplied artifact.
    * @param classloader the parent classloader
    * @param artifact the factory artifact
    * @return the factory
    * @exception Exception if a factory creation error occurs
    */
    public Builder newBuilder(ClassLoader classloader, Artifact artifact) throws Exception {
        return new DefaultBuilder(this, classloader, artifact);
    }

    /**
    * Install a block archive into the repository cache.
    * @param url the block archive url
    * @return the block manifest
    */
    public Manifest install(URL url) throws RepositoryException {
        String path = url.getFile();
        try {
            File temp = File.createTempFile("raistlin-", "-bar");
            temp.delete();
            m_loader.getResource(url.toString(), temp, true);
            temp.deleteOnExit();
            StringBuffer buffer = new StringBuffer();
            Manifest manifest = expand(temp.toURL(), buffer);
            System.out.println(buffer.toString());
            return manifest;
        } catch (RepositoryException e) {
            throw e;
        } catch (Throwable e) {
            final String error = "Cannot install target: " + url;
            throw new RepositoryException(error, e);
        }
    }

    /**
    * Expand a block archive into the repository.
    * @param url the block archive url
    * @param buffer a string buffer against which messages may be logged
    * @return the block manifest
    */
    private Manifest expand(URL url, StringBuffer buffer) throws RepositoryException {
        try {
            URL jurl = new URL("jar:" + url.toString() + "!/");
            JarURLConnection connection = (JarURLConnection) jurl.openConnection();
            Manifest manifest = connection.getManifest();
            final String group = getBlockGroup(manifest);
            buffer.append("\nBlock Group: " + group);
            final File root = new File(m_cache, group);
            buffer.append("\nLocal target: " + root);
            JarFile jar = connection.getJarFile();
            Enumeration entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.getName().startsWith("META-INF")) {
                    installEntry(buffer, root, jar, entry);
                }
            }
            buffer.append("\nInstall successful.");
            return manifest;
        } catch (Throwable e) {
            final String error = "Could not install block: " + url;
            throw new RepositoryException(error, e);
        }
    }

    private String getBlockGroup(Manifest manifest) {
        return (String) manifest.getMainAttributes().getValue(BLOCK_GROUP_KEY);
    }

    /**
    * Internal utility to install a entry from a jar file into the local repository.
    * @param buffer the buffer to log messages to
    * @param root the root directory corresponding to the bar group
    * @param jar the block archive
    * @param entry the entry from the archive to install
    */
    private void installEntry(StringBuffer buffer, File root, JarFile jar, ZipEntry entry) throws Exception {
        if (entry.isDirectory()) return;
        final String name = entry.getName();
        File file = new File(root, name);
        long timestamp = entry.getTime();
        if (file.exists()) {
            if (file.lastModified() == timestamp) {
                buffer.append("\nEntry: " + name + " (already exists)");
                return;
            } else if (file.lastModified() > timestamp) {
                buffer.append("\nEntry: " + name + " (local version is more recent)");
                return;
            } else {
                buffer.append("\nEntry: " + name + " (updating local version)");
            }
        } else {
            buffer.append("\nEntry: " + name);
        }
        InputStream is = jar.getInputStream(entry);
        if (is == null) {
            final String error = "Entry returned a null input stream: " + name;
            buffer.append("\n  " + error);
            throw new IOException(error);
        }
        file.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buf = new byte[100 * 1024];
        int length;
        while ((length = is.read(buf)) >= 0) {
            fos.write(buf, 0, length);
        }
        fos.close();
        is.close();
        if (timestamp < 0) {
            file.setLastModified(System.currentTimeMillis());
        } else {
            file.setLastModified(timestamp);
        }
    }

    private Attributes loadAttributes(File cache, String[] hosts, Artifact artifact) throws RepositoryException {
        try {
            return RepositoryUtils.getAttributes(cache, artifact);
        } catch (RepositoryException re) {
            return RepositoryUtils.getAttributes(hosts, artifact);
        }
    }
}
