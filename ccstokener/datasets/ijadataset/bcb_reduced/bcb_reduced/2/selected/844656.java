package org.nightlabs.jfire.classloader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.InflaterInputStream;
import javax.ejb.CreateException;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.nightlabs.ModuleException;
import org.nightlabs.classloader.ClassDataLoaderDelegate;
import org.nightlabs.classloader.IClassLoadingDelegator;
import org.nightlabs.j2ee.InitialContextProvider;
import org.nightlabs.util.CacheDirTag;
import org.nightlabs.util.IOUtil;

/**
 * FIXME Remove the use of deprecated members.
 *
 * @author marco
 */
@SuppressWarnings("deprecation")
public class JFireRCDLDelegate implements ClassDataLoaderDelegate {

    /**
	 * LOG4J logger used by this class
	 */
    private static final Logger logger = Logger.getLogger(JFireRCDLDelegate.class);

    private static JFireRCDLDelegate _sharedInstance = null;

    public static JFireRCDLDelegate sharedInstance() {
        if (_sharedInstance == null) throw new IllegalStateException("No shared instance existing! You must call JFireRCDLDelegate.createSharedInstance()!");
        return _sharedInstance;
    }

    /**
	 *
	 */
    public static synchronized JFireRCDLDelegate createSharedInstance(InitialContextProvider initialContextProvider, File cacheDir) throws FileNotFoundException, IOException {
        if (_sharedInstance != null) throw new IllegalStateException("There should exist only one instance of this class! You must call JFireRCDLDelegate.createSharedInstance() exactly once!");
        JFireRCDLDelegate icld = new JFireRCDLDelegate(initialContextProvider, cacheDir);
        _sharedInstance = icld;
        return _sharedInstance;
    }

    public static boolean isSharedInstanceExisting() {
        return _sharedInstance != null;
    }

    protected boolean registered = false;

    public synchronized void register(IClassLoadingDelegator delegator) {
        if (registered) return;
        delegator.addDelegate(this);
        registered = true;
    }

    public synchronized void unregister(IClassLoadingDelegator delegator) {
        if (!registered) return;
        delegator.removeDelegate(this);
        registered = false;
    }

    protected InitialContextProvider initialContextProvider;

    protected File cacheDir;

    protected JFireRCDLDelegate(InitialContextProvider _initialContextProvider, File _cacheDir) throws FileNotFoundException, IOException {
        this.initialContextProvider = _initialContextProvider;
        this.cacheDir = _cacheDir;
        CacheDirTag cdt = new CacheDirTag(cacheDir);
        cdt.tag("http://JFire.org - JFireRemoteClassLoader", true, false);
    }

    private JFireRCDLDelegateFilter filter = null;

    public void setFilter(JFireRCDLDelegateFilter filter) {
        this.filter = filter;
    }

    public JFireRCDLDelegateFilter getFilter() {
        return filter;
    }

    private JFireRCLBackendPool jFireRCLBackendPool = new JFireRCLBackendPool();

    private JFireRCLBackendHome jFireRCLBackendHome = null;

    private long jFireRCLBackendHomeCreateTimestamp = 0;

    private static final long homeLifetimeMSec = 30L * 60L * 1000L;

    protected JFireRCLBackend acquireJFireRCLBackend() throws RemoteException, LoginException, CreateException, NamingException {
        JFireRCLBackend jFireRCLBackend = jFireRCLBackendPool.getJFireRCLBackend();
        if (jFireRCLBackend != null) return jFireRCLBackend;
        if (jFireRCLBackendHome == null || (System.currentTimeMillis() - jFireRCLBackendHomeCreateTimestamp) > homeLifetimeMSec) {
            jFireRCLBackendHome = JFireRCLBackendUtil.getHome(initialContextProvider.getInitialContextProperties());
            jFireRCLBackendHomeCreateTimestamp = System.currentTimeMillis();
        }
        return jFireRCLBackendHome.create();
    }

    protected void releaseJFireRCLBackend(JFireRCLBackend jFireRCLBackend) {
        jFireRCLBackendPool.putJFireRCLBackend(jFireRCLBackend);
    }

    public ClassData getClassData(String name) {
        if (remoteBusy_class.get().booleanValue()) return null;
        remoteBusy_class.set(Boolean.TRUE);
        try {
            try {
                String resourceName = name.replace('.', '/').concat(".class");
                List<URL> resources = getResources(resourceName, true);
                if (resources == null || resources.isEmpty()) return null;
                URL url = resources.get(0);
                return new ClassData(url.openStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            remoteBusy_class.set(Boolean.FALSE);
        }
    }

    private Map<String, List<ResourceMetaData>> resources = null;

    private Object resourcesMutex = new Object();

    /**
	 * This method downloads (if necessary, there's a local cache) a Map
	 * containing the resource as key (e.g. "org/nightlabs/util/Utils.class") and
	 * their corresponding ResourceMetaData as value. Because one resource can
	 * exist in multiple locations, the value is a <code>List</code> of {@link ResourceMetaData}
	 * rather than one single instance. In most cases, there should exist only one entry in
	 * that <code>List</code>, however.
	 *
	 * @return Returns the index map that has been downloaded from the server.
	 * @throws LoginException
	 * @throws CreateException
	 * @throws NamingException
	 * @throws ClassLoaderException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    protected Map<String, List<ResourceMetaData>> getResourcesMetaDataMap() throws LoginException, CreateException, NamingException, ClassLoaderException, IOException, ClassNotFoundException {
        if (resources != null) return resources;
        synchronized (resourcesMutex) {
            if (resources == null) {
                long start = System.currentTimeMillis();
                File resourcesMapFile;
                JFireRCLBackend jfireCLBackend = acquireJFireRCLBackend();
                try {
                    resourcesMapFile = new File(cacheDir, "resourceMetaDataMap.res");
                    long resourceMetaDataMapTimestamp = jfireCLBackend.getResourcesMetaDataMapBytesTimestamp();
                    boolean download = !resourcesMapFile.exists();
                    if (!download) {
                        if (Math.abs(resourceMetaDataMapTimestamp - resourcesMapFile.lastModified()) > 10000) download = true;
                    }
                    if (download) {
                        if (logger.isDebugEnabled()) logger.debug("getResourcesMetaDataMap: loading resources map from server.");
                        File dir = resourcesMapFile.getParentFile();
                        if (!dir.exists() && !dir.mkdirs()) logger.error("Creating directory \"" + dir.getPath() + "\" failed!");
                        byte[] resMapBytes = jfireCLBackend.getResourcesMetaDataMapBytes();
                        InputStream in = new InflaterInputStream(new ByteArrayInputStream(resMapBytes));
                        try {
                            OutputStream out = new BufferedOutputStream(new FileOutputStream(resourcesMapFile));
                            try {
                                IOUtil.transferStreamData(in, out);
                            } finally {
                                out.close();
                            }
                        } finally {
                            in.close();
                        }
                        resourcesMapFile.setLastModified(resourceMetaDataMapTimestamp);
                    } else {
                        if (logger.isDebugEnabled()) logger.debug("getResourcesMetaDataMap: local resources map is already up-to-date (no need to load it from server).");
                    }
                } finally {
                    releaseJFireRCLBackend(jfireCLBackend);
                }
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(resourcesMapFile));
                try {
                    resources = (Map<String, List<ResourceMetaData>>) in.readObject();
                } finally {
                    in.close();
                }
                if (logger.isDebugEnabled()) logger.debug("getResourcesMetaDataMap: loading resources map into RAM took " + (System.currentTimeMillis() - start) + " msec.");
            }
            return resources;
        }
    }

    /**
	 * @param name name must be relative! It must not start with "/".
	 * @return a list of resource-descriptors for the given name
	 * @throws NamingException
	 * @throws CreateException
	 * @throws LoginException
	 * @throws NamingException
	 * @throws CreateException
	 * @throws LoginException
	 * @throws ClassLoaderException
	 * @throws ModuleException
	 * @throws IOException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ClassNotFoundException
	 */
    protected List<ResourceMetaData> getResourcesMetaData(String name) throws LoginException, CreateException, NamingException, ClassLoaderException, IOException, ClassNotFoundException {
        return getResourcesMetaDataMap().get(name);
    }

    protected Set<String> getResourceNames() throws LoginException, CreateException, NamingException, ClassLoaderException, IOException, ClassNotFoundException {
        return getResourcesMetaDataMap().keySet();
    }

    public Set<String> getPublishedRemotePackages() {
        Set<String> resNames;
        try {
            resNames = getResourceNames();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SortedSet<String> result = new TreeSet<String>();
        for (String resName : resNames) {
            int lastSlashPos = resName.lastIndexOf("/");
            String packageKey = lastSlashPos < 0 ? resName : resName.substring(0, lastSlashPos);
            packageKey = packageKey.replaceAll("\\/", "\\.");
            result.add(packageKey);
        }
        return result;
    }

    private ThreadLocal<Boolean> remoteBusy_class = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private ThreadLocal<Boolean> remoteBusy_res = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private Map<File, Object> resource2mutex = new HashMap<File, Object>();

    protected Object getResourceMutex(File resource) {
        synchronized (resource2mutex) {
            Object mutex = resource2mutex.get(resource);
            if (mutex == null) {
                mutex = new Object();
                resource2mutex.put(resource, mutex);
            }
            return mutex;
        }
    }

    public List<URL> getResources(String name, boolean returnAfterFoundFirst) throws IOException {
        if (remoteBusy_res.get().booleanValue()) return null;
        boolean debug = logger.isDebugEnabled();
        List<URL> resources = new LinkedList<URL>();
        long totalStart = debug ? System.currentTimeMillis() : 0;
        remoteBusy_res.set(Boolean.TRUE);
        try {
            try {
                if (name.startsWith("/")) name = name.substring(1);
                List<ResourceMetaData> rmds = getResourcesMetaData(name);
                if (rmds == null) {
                    if (debug) logger.debug("getResources: Resource \"" + name + "\" not found in ResourcesMetaData! Returning null.");
                    return null;
                }
                if (filter != null && !filter.includeResource(name)) {
                    if (debug) logger.debug("getResources: Resource \"" + name + "\" was excluded by filter \"" + filter + "\"! Returning null.");
                    return null;
                }
                JFireRCLBackend jfireRCLBackend = null;
                try {
                    for (ResourceMetaData rmd : rmds) {
                        File localRes = new File(cacheDir, rmd.getRepositoryName() + File.separatorChar + rmd.getJar() + File.separatorChar + name);
                        if (!isLocalResourceOutOfSync(localRes, rmd)) {
                            if (debug) logger.debug("getResources: Locally cached resource is up-to-date (no need to download): " + localRes.getAbsolutePath());
                        } else {
                            File dir = localRes.getParentFile();
                            if (!dir.exists() && !dir.mkdirs()) logger.error("Creating directory \"" + localRes.getParent() + "\" failed!");
                            if (jfireRCLBackend == null) {
                                long obtainBeanStart = debug ? System.currentTimeMillis() : 0;
                                jfireRCLBackend = acquireJFireRCLBackend();
                                if (debug) logger.debug("getResources: Obtaining EJB proxy JFireRCLBackend took " + (System.currentTimeMillis() - obtainBeanStart) + " msec.");
                            }
                            long downloadStart = debug ? System.currentTimeMillis() : 0;
                            byte[] res = jfireRCLBackend.getResourceBytes(rmd);
                            if (debug) logger.debug("getResources: Downloading resource from server took " + (System.currentTimeMillis() - downloadStart) + " msec: " + localRes.getAbsolutePath());
                            synchronized (getResourceMutex(localRes)) {
                                if (!isLocalResourceOutOfSync(localRes, rmd)) logger.info("The resource has been downloaded by another thread in the meantime! Will not write it again: " + localRes); else {
                                    File tmpFile = File.createTempFile(localRes.getName(), ".tmp", localRes.getParentFile());
                                    FileOutputStream fo = new FileOutputStream(tmpFile);
                                    try {
                                        fo.write(res);
                                    } finally {
                                        fo.close();
                                    }
                                    tmpFile.setLastModified(rmd.getTimestamp());
                                    long start = System.currentTimeMillis();
                                    while (true) {
                                        if (System.currentTimeMillis() - start > 20000) throw new IOException("Could not delete the resource within timeout: " + localRes.getAbsolutePath());
                                        if (!localRes.exists()) break;
                                        if (!localRes.delete()) {
                                            try {
                                                Thread.sleep(500);
                                            } catch (InterruptedException x) {
                                            }
                                            continue;
                                        }
                                    }
                                    if (!tmpFile.renameTo(localRes)) {
                                        if (!localRes.exists()) throw new IOException("Could not replace the resource \"" + localRes.getAbsolutePath() + "\" by the new version \"" + tmpFile.getAbsolutePath() + "\"! Renaming failed and the resource does not exist now!");
                                        if (localRes.exists() && tmpFile.exists()) throw new IOException("Could not replace the resource \"" + localRes.getAbsolutePath() + "\" by the new version \"" + tmpFile.getAbsolutePath() + "\"! But the resource exists - hence it was probably created by another process!");
                                        if (localRes.exists() && !tmpFile.exists()) logger.warn("Renaming the temporary file \"" + tmpFile.getAbsolutePath() + "\" to the resource \"" + localRes.getAbsolutePath() + "\" failed, but the resource now exists and the temporary file does not. So it seems to have succeeded."); else throw new IllegalStateException("This should never happen!!!");
                                    }
                                }
                            }
                        }
                        resources.add(localRes.toURI().toURL());
                        if (returnAfterFoundFirst) break;
                    }
                } finally {
                    if (jfireRCLBackend != null) releaseJFireRCLBackend(jfireRCLBackend);
                }
                return resources;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            remoteBusy_res.set(Boolean.FALSE);
            if (debug) logger.debug("getResources: Resource \"" + name + "\": total duration: " + (System.currentTimeMillis() - totalStart) + " msec.");
        }
    }

    private boolean isLocalResourceOutOfSync(File localResource, ResourceMetaData rmd) {
        boolean outOfSync = !localResource.exists();
        if (!outOfSync) {
            if (localResource.length() != rmd.getSize()) outOfSync = true; else {
                outOfSync = Math.abs(localResource.lastModified() - rmd.getTimestamp()) > 10000;
            }
        }
        return outOfSync;
    }
}
