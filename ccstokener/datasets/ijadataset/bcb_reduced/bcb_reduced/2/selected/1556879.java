package org.spbgu.pmpu.athynia.worker.classloader;

import org.apache.log4j.Logger;
import org.spbgu.pmpu.athynia.worker.network.broadcast.BroadcastListeningDaemon;
import org.spbgu.pmpu.athynia.worker.network.Client;
import org.spbgu.pmpu.athynia.worker.network.ResponseHandler;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

/**
 * User: Selivanov
 * Date: 20.12.2006
 */
public class NetworkClassLoader extends URLClassLoader {

    private static final Logger LOG = Logger.getLogger(NetworkClassLoader.class);

    private Hashtable<String, byte[]> cache;

    private URL urlBase;

    public NetworkClassLoader(File dir) throws MalformedURLException {
        super(new URL[] { dir.toURI().toURL() }, null);
        urlBase = dir.toURI().toURL();
        cache = new Hashtable<String, byte[]>();
        LOG.info("NetworkClassLoader has loaded");
    }

    public synchronized Class loadClass(String className) throws ClassNotFoundException {
        try {
            LOG.info("[Default]LOAD class:" + className);
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            LOG.info("[NetworkClassLoader]LOAD class:" + className);
            return super.loadClass(className);
        }
    }

    protected synchronized Class findClass(String className) {
        LOG.info("FIND class:" + className);
        String urlName = className.replace('.', '/');
        byte buf[];
        Class currentClass;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = className.lastIndexOf('.');
            if (i >= 0) sm.checkPackageDefinition(className.substring(0, i));
        }
        buf = cache.get(urlName);
        if (buf != null) {
            LOG.info("Get class from cache:" + className);
            currentClass = defineClass(className, buf, 0, buf.length, (CodeSource) null);
            return currentClass;
        }
        try {
            URL url = new URL(urlBase, urlName + ".class");
            LOG.info("Loading " + url);
            InputStream is = url.openConnection().getInputStream();
            buf = getClassBytes(is);
            currentClass = defineClass(className, buf, 0, buf.length, (CodeSource) null);
            return currentClass;
        } catch (MalformedURLException mE) {
            LOG.warn("Bad url detected", mE);
            return null;
        } catch (IOException e) {
            buf = downloadClass(className);
            if (buf != null) {
                return defineClass(className, buf, 0, buf.length);
            } else {
                LOG.warn("no class found: " + className);
                return null;
            }
        }
    }

    private byte[] getClassBytes(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        boolean eof = false;
        while (!eof) {
            try {
                int i = bis.read();
                if (i == -1) eof = true; else baos.write(i);
            } catch (IOException e) {
                return null;
            }
        }
        return baos.toByteArray();
    }

    private byte[] downloadClass(String className) {
        try {
            if (BroadcastListeningDaemon.centralAddress == null || BroadcastListeningDaemon.centralClassLoaderPort == 0) {
                throw new IOException("No central detected");
            }
            LOG.info("download class:" + className + " from" + BroadcastListeningDaemon.centralAddress + ":" + BroadcastListeningDaemon.centralClassLoaderPort);
            Client client = new Client(BroadcastListeningDaemon.centralAddress, BroadcastListeningDaemon.centralClassLoaderPort);
            Thread clientThread = new Thread(client);
            clientThread.setDaemon(true);
            clientThread.start();
            ResponseHandler handler = new ResponseHandler();
            client.send(className.getBytes(), handler);
            byte[] bytes = handler.waitForResponse();
            LOG.debug("get class bytes, size: " + bytes.length);
            return unzipBytes(bytes);
        } catch (IOException e) {
            LOG.warn("Error while downloading:" + className, e);
        }
        return null;
    }

    private byte[] unzipBytes(byte[] bytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        BufferedInputStream inputStream = new BufferedInputStream(new GZIPInputStream(bis));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        byte[] retval = bos.toByteArray();
        bis.close();
        inputStream.close();
        bos.close();
        return retval;
    }
}
