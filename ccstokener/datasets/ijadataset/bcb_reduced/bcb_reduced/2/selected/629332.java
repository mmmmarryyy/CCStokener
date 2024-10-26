package org.jives.implementors.network.jxse.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.jives.implementors.network.jxse.JXSEImplementor;
import org.jives.implementors.network.jxse.peer.PeerConnection;
import org.jives.implementors.network.jxse.peer.PeerManager;
import org.jives.implementors.network.jxse.rendezvous.RendezvousRelayConnection;
import org.jives.implementors.network.jxse.rendezvous.RendezvousRelayManager;
import org.jives.network.NetworkAddress;
import org.jives.utils.Log;

/**
 * Uses a different class loader than the system one to be able to instance more
 * than one endpoint in the same JVM. Indeed, it is extended both by
 * {@link RendezvousRelayManager} and {@link PeerManager} and implements common
 * endpoint methods like <code>sendMessage</code> and <code>stopNetwork</code>
 * 
 * All the public methods of this class are here because they need to be
 * executed in the context of the endpoint class loader
 * 
 * @author simonesegalini
 */
public abstract class MultiInstanceEndpoint extends URLClassLoader implements Runnable {

    /**
	 * The class of the endpoint that extends this manager. Must be set when it
	 * is launched
	 */
    protected Class<?> endpointClass;

    /**
	 * The object that represents the endpoint, one of
	 * {@link RendezvousRelayConnection} or {@link PeerConnection}
	 */
    protected Object endpoint;

    /**
	 * The cache where all loaded classes are stored for a quick
	 * retrieval
	 */
    protected static HashMap<String, byte[]> cache;

    protected Thread mainThread;

    protected MultiInstanceEndpoint(Thread mainThread) {
        super(new URL[0]);
        this.mainThread = mainThread;
        URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        List<URL> oldClassPaths = (Arrays.asList(urls));
        for (URL entry : oldClassPaths) {
            addURL(entry);
            if (entry.getFile().endsWith(".jar")) {
                try {
                    JarInputStream jar = new JarInputStream(entry.openStream());
                    Manifest mf = jar.getManifest();
                    if (mf != null && mf.getMainAttributes() != null) {
                        String dependencies = mf.getMainAttributes().getValue("Class-Path");
                        if (dependencies != null) {
                            for (String dependency : dependencies.split(" ")) {
                                URL url = new URL("file:" + dependency);
                                addURL(url);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.debug(MultiInstanceEndpoint.class, "Failed to retrieve dependencies from " + entry.getFile());
                }
            }
        }
        if (cache == null) {
            buildCache();
        }
    }

    /**
	 * Load the class data from the network
	 * 
	 * @param className
	 *            the name of the class
	 * 
	 * @return the array of data bytes
	 */
    private byte[] loadClassData(String className) {
        Log.debug(this, "Loading " + className + ".class");
        return cache.get(className);
    }

    private void buildCache() {
        cache = new HashMap<String, byte[]>();
        JarInputStream jis = null;
        BufferedInputStream bis = null;
        URL[] urls = getURLs();
        for (URL url : urls) {
            try {
                if (url.getPath().endsWith(".jar")) {
                    jis = new JarInputStream(url.openStream());
                    bis = new BufferedInputStream(jis);
                    JarEntry jarEntry = null;
                    while ((jarEntry = jis.getNextJarEntry()) != null) {
                        String name = jarEntry.getName();
                        if (!jarEntry.isDirectory() && name.toLowerCase().endsWith(".class")) {
                            String className = name.replaceAll("/", ".").substring(0, name.length() - 6);
                            if (isClassLoaderConditonVerified(className)) {
                                ByteArrayOutputStream baos = null;
                                BufferedOutputStream bos = null;
                                try {
                                    baos = new ByteArrayOutputStream();
                                    bos = new BufferedOutputStream(baos);
                                    int i = -1;
                                    while ((i = bis.read()) != -1) {
                                        bos.write(i);
                                    }
                                    bos.flush();
                                    cache.put(className, baos.toByteArray());
                                } finally {
                                    if (baos != null) {
                                        try {
                                            baos.close();
                                        } catch (IOException ignore) {
                                        }
                                    }
                                    if (bos != null) {
                                        try {
                                            bos.close();
                                        } catch (IOException ex) {
                                        }
                                    }
                                }
                                jis.closeEntry();
                            }
                        }
                    }
                    try {
                        jis.close();
                    } catch (IOException ignore) {
                    }
                } else {
                    File file = new File(url.getFile());
                    buildCacheFromFile(file, null);
                }
            } catch (IOException ex) {
                continue;
            }
        }
    }

    private void buildCacheFromFile(File file, String root) {
        if (file.isDirectory()) {
            if (root == null) {
                root = file.getPath();
            }
            for (File innerFile : file.listFiles()) {
                buildCacheFromFile(innerFile, root);
            }
        } else {
            String name = file.getPath();
            if (name.toLowerCase().endsWith(".class")) {
                if (name.startsWith("/")) {
                    name = name.substring(root.length() + 1);
                } else {
                    name = name.substring(root.length());
                }
                String className = name.replaceAll("/", ".").substring(0, name.length() - 6);
                if (isClassLoaderConditonVerified(className)) {
                    try {
                        URL fileURL = new URL("file:" + file.getPath());
                        long classSize = 0;
                        InputStream is = null;
                        if (fileURL != null) {
                            is = fileURL.openStream();
                            classSize = new File(fileURL.getFile()).length();
                        }
                        byte[] bytes = new byte[(int) classSize];
                        is.read(bytes, 0, bytes.length);
                        try {
                            is.close();
                        } catch (IOException ignore) {
                        }
                        cache.put(className, bytes);
                    } catch (IOException e) {
                        Log.debug(this, e);
                    }
                }
            }
        }
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        byte[] bytes = loadClassData(className);
        if (bytes == null) {
            throw new ClassNotFoundException("Class " + className + " not found by " + getClass().getCanonicalName());
        }
        return defineClass(className, bytes, 0, bytes.length);
    }

    protected abstract Thread getEndpointThread();

    /**
	 * Use reflection to invoke a method on an endpoint, being this a peer or a
	 * rendezvous. This is necessary because the class loader is aware of
	 * multiple instance of those object
	 * 
	 * @param methodName
	 *            The method to call
	 * 
	 * @param paramTypes
	 *            The types of the method parameters
	 * 
	 * @param paramValues
	 *            The values of the method parameters
	 * 
	 * @return The invocation result
	 * 
	 * @throws NoSuchMethodException
	 *             If the method does not exist
	 * @throws SecurityException
	 *             If reflection fails invocation
	 * @throws IllegalArgumentException
	 *             If reflection fails invocation
	 * @throws IllegalAccessException
	 *             If reflection fails invocation
	 * @throws InvocationTargetException
	 *             If reflection fails invocation
	 */
    protected Object invoke(String methodName, Class<?>[] paramTypes, Object[] paramValues) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
        Method method = endpointClass.getMethod(methodName, paramTypes);
        return method.invoke(endpoint, paramValues);
    }

    /**
	 * Method used to load a specific class with a specified class name
	 * 
	 * @param className
	 *            the name of the class to be loaded
	 * 
	 * @param resolve
	 *            if true then resolve the class
	 * 
	 * @return the resulting Class object
	 */
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> cls = findLoadedClass(className);
        if (cls != null) {
            return cls;
        }
        if (!isClassLoaderConditonVerified(className)) {
            try {
                JXSEImplementor.class.getClassLoader();
                cls = mainThread.getContextClassLoader().loadClass(className);
                return cls;
            } catch (ClassNotFoundException e) {
                Log.fatal(this, "System classloader unable to resolve class " + className);
                throw e;
            }
        }
        cls = findClass(className);
        if (resolve) {
            resolveClass(cls);
        }
        return cls;
    }

    private boolean isClassLoaderConditonVerified(String className) {
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("org.omg.") || className.startsWith("org.w3c.dom.") || className.startsWith("org.xml.sax.") || className.startsWith("sun.") || className.startsWith("org.apache.log4j") || className.startsWith("org.jives.implementors.network.jxse.JXSEImplementor") || className.startsWith("org.jives.implementors.network.jxse.rendezvous.RendezvousRelayManager") || className.startsWith("org.jives.implementors.network.jxse.peer.PeerManager") || className.startsWith("org.jives.implementors.network.jxse.utils.FileManager") || className.startsWith("org.jives.implementors.network.jxse.utils.Tools") || className.startsWith("org.jives.implementors.network.jxse.utils.XMLConfigParser") || className.startsWith("org.jives.implementors.network.jxse.utils.NetworkLog") || (className.startsWith("org.jives.") && !className.startsWith("org.jives.implementors.network.jxse."))) {
            return false;
        }
        if (className.startsWith("org.jboss.netty.") || className.startsWith("net.jxse.") || className.startsWith("net.jxta.") || className.startsWith("org.mortbay.") || className.startsWith("org.h2.") || className.startsWith("org.bouncycastle.") || className.startsWith("org.jives.implementors.network.jxse.")) {
            return true;
        }
        return false;
    }

    /**
	 * Method used to send a message through the sendMessage method of the
	 * endpoint.
	 * 
	 * @param destination
	 *            The network address of the destination
	 * 
	 * @param message
	 *            The string to be sent as message
	 */
    public void sendMessage(NetworkAddress destination, String message) {
        try {
            getEndpointThread().interrupt();
            invoke("sendMessage", new Class<?>[] { NetworkAddress.class, String.class }, new Object[] { destination, message });
        } catch (Exception e) {
            Log.error(this, "Send message failed: " + e.getMessage());
        }
    }

    /**
	 * Method used to close an endpoint.
	 */
    public void stopNetwork() {
        try {
            Thread endpointThread = getEndpointThread();
            if (endpointThread != null) {
                endpointThread.interrupt();
                invoke("stopConnection", new Class[0], new Object[0]);
            }
        } catch (Exception e) {
            Log.error(this, "Stop connection failed: " + e.getMessage());
        }
    }

    /**
	 * Method to get names and PIDs of the remote users connected to the
	 * network. Due to the JXSE network functioning this method has to be called
	 * only once in a minute, in such a way the JXSE network can refresh the
	 * remote advertisements of the peers connected
	 * 
	 * @param includeSelf
	 *            the boolean value indicating if the local peer is included
	 *            into the list of remote peers returned by the Jives Network
	 * 
	 * @return a list of strings representing all the names and PIDs of the
	 *         peers running the same JivesScript connected to the Jives Network
	 */
    public List<String> getRemotePeersInfo(boolean includeSelf) {
        List<String> listPeers = new ArrayList<String>();
        try {
            String[] infos = (String[]) invoke("getRemotePeers", new Class[] { Boolean.TYPE }, new Object[] { includeSelf });
            for (String serialInfo : infos) {
                if (!listPeers.contains(serialInfo)) {
                    listPeers.add(serialInfo);
                }
            }
        } catch (Exception e) {
            Log.error(this, "Error retrieving remote peers info: " + e);
        }
        return listPeers;
    }
}
