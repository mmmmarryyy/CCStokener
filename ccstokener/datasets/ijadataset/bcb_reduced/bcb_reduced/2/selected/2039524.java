package net.dzzd.extension.loader;

import java.io.*;
import java.util.zip.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedAction;
import java.security.Permission;
import net.dzzd.access.*;
import net.dzzd.utils.Log;

public class ExtensionLoader extends ClassLoader implements IExtensionLoader {

    private static ExtensionLoader loader = null;

    private Hashtable classPool;

    private String extDir;

    private ExtensionLoader(Class parent) throws Exception {
        super(parent.getClassLoader());
        Thread.currentThread().setContextClassLoader(this);
        System.setSecurityManager(new MySecurityManager());
        classPool = new Hashtable();
        extDir = System.getProperty("user.home") + File.separator;
    }

    public static IExtensionLoader getLoader(final Class parent) throws Exception {
        if (loader != null) return loader;
        loader = null;
        return (ExtensionLoader) AccessController.doPrivileged(new PrivilegedExceptionAction() {

            public Object run() throws Exception {
                loader = new ExtensionLoader(parent);
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    try {
                        sm.checkWrite("ExtensionLoader");
                        sm.checkPropertiesAccess();
                    } catch (SecurityException se) {
                        throw new Exception("User rejected certificate");
                    }
                }
                return loader;
            }
        });
    }

    protected void addClass(String name, byte[] classData) throws Exception {
        if (classPool.containsKey(name)) {
            throw new Exception("ExtensionLoader: Cannot add class " + name + ", already in classpool");
        }
        String temp;
        temp = name.replace(File.separator.charAt(0), '.');
        temp = name.replace('\\', '.');
        temp = name.replace('/', '.');
        classPool.put(temp, classData);
    }

    protected long readTimeStamp(File indexFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(indexFile));
            try {
                String str = reader.readLine();
                return Long.parseLong(str);
            } finally {
                reader.close();
            }
        } catch (Exception ex) {
        }
        return -1;
    }

    protected void writeTimeStamp(File indexFile, long timestamp) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
            try {
                writer.write("" + timestamp + "\n");
                writer.flush();
            } finally {
                writer.close();
            }
        } catch (Exception ex) {
        }
    }

    protected void downloadJar(URL downloadURL, File jarFile, IProgressListener pl) {
        BufferedOutputStream out = null;
        InputStream in = null;
        URLConnection urlConnection = null;
        try {
            urlConnection = downloadURL.openConnection();
            out = new BufferedOutputStream(new FileOutputStream(jarFile));
            in = urlConnection.getInputStream();
            int len = in.available();
            Log.log("downloading jar with size: " + urlConnection.getContentLength());
            if (len < 1) len = 1024;
            byte[] buffer = new byte[len];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    protected void addJar(File jarFile) throws Exception {
        byte[] chunk;
        InputStream is;
        ByteArrayOutputStream bos;
        ZipFile zf = new ZipFile(jarFile);
        ZipEntry ze;
        for (Enumeration e = zf.entries(); e.hasMoreElements(); ) {
            ze = (ZipEntry) e.nextElement();
            if (!ze.isDirectory()) {
                if (ze.getName().endsWith(".class")) {
                    Log.log("addjar: found class in jarfile: " + ze.getName());
                    is = zf.getInputStream(ze);
                    bos = new ByteArrayOutputStream();
                    int length = is.available();
                    if (length < 1) length = 1024;
                    chunk = new byte[length];
                    int bytesRead = 0;
                    while ((bytesRead = is.read(chunk, 0, length)) != -1) {
                        bos.write(chunk, 0, bytesRead);
                    }
                    is.close();
                    bos.flush();
                    addClass(ze.getName(), bos.toByteArray());
                    bos.reset();
                }
            }
        }
    }

    public void loadJar(final String extName, final String url, final String fileName, final IProgressListener pl) throws Exception {
        pl.setName(fileName);
        pl.setProgress(0);
        pl.setFinished(false);
        pl.setStarted(true);
        String installDirName = extDir + File.separator + extName;
        Log.log("extension installation directory: " + installDirName);
        File installDir = new File(installDirName);
        if (!installDir.exists()) {
            if (!installDir.mkdirs()) {
                throw new Exception("ExtensionLoader.loadJar: Cannot create install directory: " + installDirName);
            }
        }
        URL downloadURL = new URL(url + fileName);
        File jarFile = new File(installDirName, fileName);
        File indexFile = null;
        long urlTimeStamp = downloadURL.openConnection().getLastModified();
        String indexFileName = "";
        int idx = fileName.lastIndexOf(".");
        if (idx > 0) {
            indexFileName = fileName.substring(0, idx);
        } else {
            indexFileName = fileName;
        }
        indexFileName = indexFileName + ".idx";
        Log.log("index filename: " + indexFileName);
        boolean isDirty = true;
        if (jarFile.exists()) {
            Log.log("extensionfile already exists: " + fileName);
            indexFile = new File(installDir, indexFileName);
            if (indexFile.exists()) {
                Log.log("indexfile already exists");
                long cachedTimeStamp = readTimeStamp(indexFile);
                isDirty = !(cachedTimeStamp == urlTimeStamp);
                Log.log("cached file dirty: " + isDirty + ", url timestamp: " + urlTimeStamp + " cache stamp: " + cachedTimeStamp);
            } else {
                Log.log("indexfile doesn't exist, assume cache is dirty");
            }
        }
        if (isDirty) {
            if (jarFile.exists()) {
                if (indexFile != null && indexFile.exists()) {
                    Log.log("deleting old index file");
                    indexFile.delete();
                }
                indexFile = new File(installDirName, indexFileName);
                Log.log("deleting old cached file");
                jarFile.delete();
            }
            downloadJar(downloadURL, jarFile, pl);
            indexFile = new File(installDir, indexFileName);
            Log.log("writing timestamp to index file");
            writeTimeStamp(indexFile, urlTimeStamp);
        }
        addJar(jarFile);
    }

    protected Class findClass(String name) {
        String fullName = name.concat(".class");
        if (classPool.containsKey(fullName)) {
            byte[] data = (byte[]) classPool.get(fullName);
            return defineClass(name, data, 0, data.length);
        }
        return null;
    }

    public Object loadExtension(String baseURL, String jarFile, String extensionClass, String localDirectory, IProgressListener pl, String mainClassName) {
        pl.setProgress(0);
        pl.setFinished(false);
        pl.setError(false);
        try {
            loader.loadJar(localDirectory, baseURL, jarFile, pl);
            Class c = loader.loadClass(extensionClass);
            if (c == null) Log.log("cannot find extension class : extensionClass");
            IExtension jl = (IExtension) c.newInstance();
            jl.load(baseURL, localDirectory, pl, loader);
            Object o = loader.loadClass(mainClassName).newInstance();
            return o;
        } catch (Exception e) {
            pl.setProgress(100);
            pl.setFinished(true);
            pl.setError(true);
            e.printStackTrace();
        }
        return null;
    }

    private class MySecurityManager extends SecurityManager {

        public MySecurityManager() {
            super();
        }

        public void checkPermission(Permission perm) throws SecurityException, NullPointerException {
        }

        public void checkPermission(Permission perm, Object context) throws SecurityException, NullPointerException {
        }
    }
}
