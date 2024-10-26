package org.vrspace.server;

import java.net.*;
import java.lang.reflect.*;
import java.lang.ref.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import org.vrspace.util.*;

/**
Big bad class loader.
It delegates all calls for all classes specified along with package name to parent classloader,
this usually means system class loader. If this loader fails, looks for class
in URLs specified by <b>vrspace.loader.classpath</b> property or by addURL/addDir calls.
Behaviour changes when a class is specified without package name.
Such classes are searched by package path.
This allows us to change any class with another class without need to recompile.
Still we need to respect polymorhpism etc.
To use this feature, you'll have to
1) create new VRSpaceLoader explicitly
or
2) use Thread.getContextClassLoader().loadClass() instead of Class.forName()
To specify package search path, use <b>vrspace.loader.packages</b> property or
call addPackage/removePackage.
In properties, delimit urls/packages with ;
*/
public class VRSpaceLoader extends URLClassLoader implements Runnable {

    static VRSpaceLoader mainLoader;

    static HashMap classNames = new HashMap();

    static HashMap classes = new HashMap();

    static Object lock = new Object();

    static ReferenceQueue refQ = new ReferenceQueue();

    static HashMap jarClasses = new HashMap();

    static Vector classPath = getClassPath();

    static Thread cleanup;

    static boolean active = false;

    static final String fileUrlPrefix = "file:";

    private HashMap resources = new HashMap();

    Vector packages = new Vector();

    protected static String clPreload = "VRObject PublicVRObject PrivateVRObject PassiveVRObject DBObject PrivateDBObject PublicDBObject Transform Client AuthInfo Site ObserverSensor ProximitySensor File Portal Administrator Console Tomcat ClientGate James WorldEditor Movie Item Shooter VrmlFile User Alice Jetty PointLight PublicVrmlFile Boardgame ImageElevationGrid VisitorsList Chatroom BoardgameManager JXTA Text Gate WebGate Tunnell Mirror Terrain ImageTerrain Picture Robot";

    private static Vector getClassPath() {
        String path = System.getProperty("java.class.path");
        Logger.logDebug("System classpath: " + path);
        StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
        Vector ret = new Vector();
        while (st.hasMoreTokens()) {
            String element = st.nextToken();
            URL url = null;
            try {
                try {
                    url = new URL(element);
                } catch (MalformedURLException mue) {
                    url = new URL(fileUrlPrefix + element);
                }
                try {
                    checkURL(url);
                    ret.add(url);
                    Logger.logDebug("Path element: " + url);
                } catch (Exception e) {
                    Logger.logWarning("Can't add path element " + url + " - " + e);
                }
            } catch (Throwable t) {
                Logger.logError("Could not add path element " + url, t);
            }
        }
        return ret;
    }

    public VRSpaceLoader() {
        super(new URL[0]);
        init();
    }

    public VRSpaceLoader(URL[] urls) {
        super(urls);
        init();
    }

    public VRSpaceLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        init();
    }

    public VRSpaceLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        init();
    }

    /**
  Add a package to package search path.
  */
    public void addPackage(String name) {
        packages.add(name);
        Logger.logInfo("Added package " + name + " to package search path");
    }

    /**
  Remove a package from package search path.
  */
    public void removePackage(String name) {
        packages.remove(name);
        Logger.logInfo("Removed package " + name + " from package search path");
    }

    void init() {
        if (cleanup == null) {
            mainLoader = this;
            Logger.logDebug("First loader instance");
            cleanup = new Thread(this, "VRSpaceLoader");
            cleanup.start();
            new Logger();
        }
        addPackage("org.vrspace.server");
        addPackage("org.vrspace.server.object");
    }

    protected void addURL(URL url) {
        if (classPath.contains(url)) {
        } else {
            super.addURL(url);
            classPath.add(url);
        }
    }

    protected static void checkURL(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        JarInputStream jar = new JarInputStream(in);
        ZipEntry entry;
        while ((entry = jar.getNextEntry()) != null) {
            if (entry.isDirectory()) {
            } else {
                jarClasses.put(entry.getName().replace('/', '.'), url);
            }
        }
        in.close();
    }

    /**
  Adds all files in a local directory to classpath
  */
    public void addDir(String dir) throws IOException, MalformedURLException {
        if (!dir.endsWith(System.getProperty("file.separator"))) {
            dir += System.getProperty("file.separator");
        }
        File[] files = (new File(dir)).listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getCanonicalPath().endsWith("ar")) {
                    URL url = new URL(fileUrlPrefix + files[i].getCanonicalPath());
                    try {
                        checkURL(url);
                        addURL(url);
                        Logger.logDebug("Added " + url + " to classpath");
                    } catch (Exception e) {
                        Logger.logDebug("Didn't add " + url + " to classpath - " + e);
                    }
                }
            }
        } else {
            addURL(new URL(fileUrlPrefix + (new File(dir)).getCanonicalPath()));
        }
    }

    /**
  This returns full class search path in a string, delimited by File.pathSeparator
  */
    public String getPath() {
        Iterator it = classPath.iterator();
        StringBuffer scp = new StringBuffer();
        while (it.hasNext()) {
            URL url = (URL) it.next();
            scp.append(url.getFile());
            scp.append(File.pathSeparator);
        }
        return scp.toString();
    }

    /**
  Returns HashMap containing loaded class names and classes.
  Class names (keys) do not contain package names.
  */
    public HashMap getClasses() {
        HashMap ret = new HashMap();
        synchronized (lock) {
            Iterator it = classNames.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ClassReference ref = (ClassReference) classes.get(entry.getValue());
                if (ref == null) {
                    Logger.logError("Cannot get class reference for loaded class " + entry.getValue());
                }
                Class cls = (Class) ref.get();
                if (cls != null) ret.put(entry.getKey(), cls);
            }
        }
        return ret;
    }

    /**
  Adds all files in specified directories to classpath
  */
    public void addDirs(String[] dirs) throws IOException, MalformedURLException {
        for (int i = 0; i < dirs.length; i++) {
            addDir(dirs[i].trim());
        }
    }

    /**
  Tries to guess package name of the specified class
  */
    protected Class findClass(String name) throws ClassNotFoundException {
        Class ret = null;
        if (name.indexOf(".") > -1 || name.indexOf("/") > -1) {
            ret = super.findClass(name);
        } else {
            synchronized (lock) {
                ret = getByName(name);
            }
        }
        return ret;
    }

    /**
  This searches packages for a class specified without package name
  */
    private Class getByName(String name) throws ClassNotFoundException {
        Class ret = null;
        String fullName = (String) classNames.get(name);
        if (fullName == null) {
            try {
                ret = getParent().loadClass(name);
                synchronized (lock) {
                    classNames.put(name, name);
                }
                fullName = name;
                Logger.logDebug("No package for " + name);
            } catch (Throwable t) {
                Logger.logDebug("Parent could not find " + name + " - " + t);
            }
            for (int i = 0; fullName == null && i < packages.size(); i++) {
                try {
                    Logger.logDebug("Looking for class " + name + " in package " + packages.elementAt(i));
                    ret = loadClass(packages.elementAt(i) + "." + name);
                    fullName = packages.elementAt(i) + "." + name;
                    synchronized (lock) {
                        classNames.put(name, fullName);
                    }
                    Logger.logDebug("Class " + name + " mapped to " + fullName);
                } catch (ClassNotFoundException e) {
                } catch (Throwable t) {
                    Logger.logError("Unexpected exception in ClassLoader", t);
                }
            }
            if (ret == null) {
            } else {
                classes.put(fullName, new ClassReference(ret, refQ, name));
            }
        } else {
            Reference ref = (Reference) classes.get(fullName);
            ret = (Class) ref.get();
        }
        return ret;
    }

    /**
  This actually loads a class. If parent fails, searches for class definition
  all jars/dirs/urls specified in the classpath.
  */
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        byte[] bytes;
        Class ret = null;
        String slashName = null;
        ret = findLoadedClass(name);
        if (ret == null) {
            if (name.startsWith("java")) {
                ret = super.loadClass(name, resolve);
            } else if (name.indexOf(".") > 0 || name.indexOf("/") > 0) {
                StringTokenizer st = new StringTokenizer(name, ".");
                StringBuffer tmp = new StringBuffer(st.nextToken());
                while (st.hasMoreTokens()) {
                    tmp.append('/');
                    tmp.append(st.nextToken());
                }
                slashName = tmp.toString();
            } else {
                ret = getByName(name);
            }
            if (ret == null) {
                URL[] path = (URL[]) classPath.toArray(new URL[classPath.size()]);
                for (int i = 0; i < path.length; i++) {
                    URL jar = path[i];
                    if (new File(jar.getFile()).isDirectory()) continue;
                    try {
                        JarFile jarFile = new JarFile(jar.getFile());
                        JarEntry entry = (JarEntry) jarFile.getEntry(name + ".class");
                        if (entry == null) {
                            entry = (JarEntry) jarFile.getEntry(name.replace('.', '/') + ".class");
                        }
                        if (entry != null) {
                            InputStream stream = jarFile.getInputStream(entry);
                            int size = (int) entry.getSize();
                            byte[] code = new byte[size];
                            for (int j = 0; j < size; j++) {
                                code[j] = (byte) stream.read();
                            }
                            stream.close();
                            try {
                                ret = defineClass(name, code, 0, code.length);
                            } catch (NoClassDefFoundError shit) {
                                ret = defineClass(slashName, code, 0, code.length);
                            }
                            break;
                        } else {
                        }
                    } catch (Throwable t) {
                        Logger.logWarning("Error loading " + jar + "!/" + name + ".class" + " - " + t);
                    }
                }
            }
            if (ret == null) {
                ret = super.loadClass(name, resolve);
            }
            if (ret == null) {
                throw new ClassNotFoundException("Could not load class " + name);
            } else if (resolve) {
                resolveClass(ret);
            }
        }
        return ret;
    }

    /**
  Work in progress
  */
    public class ClassReference extends SoftReference {

        String name;

        public ClassReference(Object obj, ReferenceQueue q, String name) {
            super(obj, q);
            this.name = name;
        }
    }

    public void run() {
        active = true;
        while (active) {
            try {
                Reference ref = refQ.remove();
                Object obj = ref.get();
                Logger.logDebug("Removed ref to " + obj);
                if (ref instanceof ClassReference) {
                    synchronized (lock) {
                        classes.remove(((ClassReference) ref).name);
                        classNames.remove(((ClassReference) ref).name);
                    }
                }
            } catch (InterruptedException e) {
                Logger.logInfo("Interrupt, shutting down");
                active = false;
            }
        }
    }

    public static void main(String[] args) {
        try {
            URL[] urls = (URL[]) classPath.toArray(new URL[classPath.size()]);
            VRSpaceLoader loader = new VRSpaceLoader(urls);
            String path = System.getProperty("vrspace.loader.path");
            if (path != null && path.length() > 0) {
                StringTokenizer st = new StringTokenizer(path, " ;");
                while (st.hasMoreTokens()) {
                    String file = st.nextToken();
                    if (file.indexOf(":") == -1) {
                        File dir = new File(file);
                        if (dir.isDirectory()) {
                            loader.addDir(file);
                            continue;
                        } else {
                            file = "file:" + file;
                        }
                    }
                    loader.addURL(new URL(file));
                }
            }
            Logger.logInfo("Loader classpath: " + loader.getPath());
            loader.startServer(args);
        } catch (Throwable t) {
            Logger.logError(t);
        }
    }

    /**
  Starts vrspace server with specified arguments
  @see Server
  */
    public void startServer(String[] args) throws Exception {
        String packageNames = System.getProperty("vrspace.loader.packages");
        if (packageNames != null && packageNames.length() > 0) {
            StringTokenizer st = new StringTokenizer(packageNames, " ;");
            while (st.hasMoreTokens()) {
                String packageName = st.nextToken();
                addPackage(packageName);
            }
        }
        String mainClass = System.getProperty("vrspace.loader.mainclass", "org.vrspace.server.Server");
        Class cl = loadClass(mainClass);
        Class[] paramCl = new Class[] { String[].class };
        Method main = cl.getMethod("main", paramCl);
        Thread.currentThread().setContextClassLoader(this);
        clPreload = System.getProperty("vrspace.loader.preload", "org.vrspace.server.object");
        preload();
        main.invoke(null, new Object[] { args });
    }

    public void preload() {
        StringTokenizer st = new StringTokenizer(clPreload, "; ");
        while (st.hasMoreTokens()) {
            String classSpec = st.nextToken();
            Iterator it = jarClasses.keySet().iterator();
            while (it.hasNext()) {
                String className = (String) it.next();
                if (className.startsWith(classSpec) && className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6);
                    try {
                        Class cls = loadClass(className);
                        Object obj = cls.newInstance();
                        String name = org.vrspace.util.Util.getClassName(cls);
                        synchronized (lock) {
                            classNames.put(name, className);
                            classes.put(className, new ClassReference(cls, refQ, name));
                        }
                        Logger.logDebug("Class " + name + " " + className + " accessed sucessfully from " + Util.getLocation(obj));
                    } catch (Exception e) {
                        Logger.logWarning("Can't access class " + className + " - " + e);
                    }
                }
            }
        }
    }

    private static void testUnload(ClassLoader loader) {
        try {
            Logger.logDebug("Testing unload:");
            (loader.loadClass("VRObject")).newInstance();
            System.gc();
            Logger.logDebug("GC done");
        } catch (Throwable t) {
            Logger.logError(t);
        }
    }
}
