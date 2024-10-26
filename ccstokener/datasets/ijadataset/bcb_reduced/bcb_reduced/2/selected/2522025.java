package net.ucef.server.kernel;

/** <p>Custom classloader for dynamic loading of classes from multiple classpaths.</p>
 *
 * <p>This classloader has several features not provided by the Java bootstrap
 * loader. The features are:<ul>
 * <li>Loading of classes from any place, including filesystems and network</li>
 * <li>Caching of loaded classes</li>
 * <li>Unloading of classes no more used</li>
 * <li>Support of updating class implementations at runtime</li></ul><br>
 * The classloader is implemented as singleton. It forces the classloader to be
 * the default classloader of classes once they are loaded by this
 * classloader.<br>
 * The cache integrated increase performance. However, for development purposes
 * the cache can be virtually deactivated by setting the lifetime of a class to
 * 0. The clearing of the cache is done automatically in the background. No
 * interaction is necessery.<br>
 * Changings to any of the classloader references is reflected to all
 * classloaders of the same type. This includes changes to the cache management
 * as well as to the classpaths monitored by this classloader.<br>
 * Derivation of this classloader should be done only after exhausive studies of
 * this implementation since all features are fully accessible by derivations.</p>
 *
 * @author Bjoern Wuest, Germany
 * @version 20020702, 0.1
 */
public class CClassLoader extends java.lang.ClassLoader implements java.lang.Runnable {

    /** <p>Mapping of class to timestamp.</p>
   *
   * <p>This inner class is used by the cache manager to determine when a class
   * should be subject for unloading if the virtual machine can do so. For this,
   * each classloader provides a timer to determine the maximum age of a class
   * mapping. If this timer expires, the class is removed from the cache
   * mapping. Depending on the cache policy, further loading of the class may
   * result in exceptions until the class is unloaded from the virtual machine,
   * simply return the reference to the class without further changes (default)
   * or reestablish the mapping with a new expiration time.</p>
   *
   * @author Bjoern Wuest, Germany
   * @version 20020702, 0.1
   */
    protected class CClassEntry {

        /** <p>Class of mapping.</p> */
        protected java.lang.Class p_ClassInstance;

        /** <p>Creation timestamp.</p> */
        protected long p_CreationTimestamp;

        /** <p>Creates class mapping.</p>
     *
     * @param Instance The class of the mapping.
     * @param Timestamp The time of creation of this mapping.
     */
        public CClassEntry(java.lang.Class Instance, long Timestamp) {
            p_ClassInstance = Instance;
            p_CreationTimestamp = Timestamp;
        }

        /** <p>Returns class of this mapping.</p>
     *
     * @return Class of this mapping.
     */
        public java.lang.Class getClassInstance() {
            return p_ClassInstance;
        }

        /** <p>Returns time of creation of this mapping.</p>
     *
     * @return Time of creation of this mapping.
     */
        public long getCreationTimestamp() {
            return p_CreationTimestamp;
        }
    }

    /** <p>Instance of classloader to provide singleton functionality.</p> */
    protected static CClassLoader p_Instance = null;

    /** <p>List of registered classpaths.</p> */
    protected static final java.util.Set p_Classpaths = new java.util.LinkedHashSet();

    /** <p>List of loaded classes. This is the cache of the classloader.</p> */
    protected static final java.util.Map p_LoadedClasses = new java.util.TreeMap();

    /** <p>Default class lifetime. Set to 1 day.</p> */
    protected static int p_ClassLifetime;

    /** <p>Default cleaner sleeptime. Set to 6 hours.</p> */
    protected static int p_CleanerSleeptime;

    /** <p>Status of shutdown, if <b>true</b> cleaner manager should shutdown.</p> */
    protected static boolean p_Shutdown = false;

    /** <p>Current cache policy.</p> */
    protected static int p_CachePolicy = 0x0;

    /** <p>Clears the cache policy.</p> */
    public static final int CACHEPOLICY_NONE = 0x0;

    /** <p>Set the enforce reload policy for cache.</p>
   *
   * <p>Whenever this policy is set, a class must be completely unloaded first
   * before new objects can be created. A class is unloaded when there are no
   * objects of this class in the virtual machine.<br>
   * If a class should be reloaded while this policy is active and the class is
   * not unloaded now, the classloader will thrown an exception indicating that
   * the class can't be reloaded and no instances of the class can be created.</p>
   */
    public static final int CACHEPOLICY_ENFORCERELOAD = 0x1;

    /** <p>Set weak reload policy for cache.</p>
   *
   * <p>If this policy is set, a class is added to the cache once it's unloaded
   * but requested again while objects with this class are still active in the
   * virtual machine. An active object is an object not garbage collected until
   * now.<br>
   * This policy is ignored if the {@link CACHEPOLICY_ENFORCERELOAD enforce
   * reload policy} is set.</p>
   */
    public static final int CACHEPOLICY_WEAKRELOAD = 0x2;

    /** <p>Internal, short hand constructor.</p>
   *
   * @param Parent The parent classloader of this classloader.
   */
    protected CClassLoader(ClassLoader Parent) {
        super(Parent);
    }

    /** <p>Internal method to load class.</p>
   *
   * <p>The method is responsible to dynamically load classes from the classpath
   * provided in the classloader. The classpaths are scanned in sequential
   * order and the first class found is returned.</p>
   *
   * @param Classname The classname to load.
   * @return Instance of {@link java.lang.Class java.lang.Class} representing <i>Classname</i>.
   * @throws ClassNotFoundException is thrown if the class could not be loaded from any classpath.
   */
    protected java.lang.Class p_loadClass(java.lang.String Classname) throws java.lang.ClassNotFoundException {
        java.util.Iterator iter = p_Classpaths.iterator();
        while (iter.hasNext()) {
            try {
                java.net.URLConnection urlConnection = (new java.net.URL(((java.lang.String) (iter.next())).concat(Classname.replace('.', '/')).concat(".class"))).openConnection();
                int dataSize = urlConnection.getContentLength();
                byte[] data = new byte[dataSize];
                dataSize = (new java.io.BufferedInputStream(urlConnection.getInputStream())).read(data);
                java.lang.Class cls = defineClass(Classname, data, 0, dataSize);
                resolveClass(cls);
                p_LoadedClasses.put(Classname, new CClassLoader.CClassEntry(cls, (new java.util.Date()).getTime()));
                return cls;
            } catch (java.io.IOException Ex) {
            }
        }
        throw new java.lang.ClassNotFoundException("The requested class of name '" + Classname + "' could not be loaded from registered classpaths.");
    }

    /** <p>Internal method to remove expired classes.</p> */
    protected synchronized void p_removeExpiredClasses() {
        long lifetimeThreshold = (new java.util.Date()).getTime() - p_ClassLifetime;
        java.util.Iterator iter = p_LoadedClasses.keySet().iterator();
        while (iter.hasNext()) {
            java.lang.Object key = iter.next();
            CClassLoader.CClassEntry classEntry = (CClassLoader.CClassEntry) (p_LoadedClasses.get(key));
            if (classEntry.getCreationTimestamp() < lifetimeThreshold) {
                iter.remove();
            }
        }
    }

    /** <p>Get instance of this classloader.</p>
   *
   * @param Classpaths The classpath to add to the classloader. If an instance of the classloader is already existing, the classpaths are added to the classpaths of all classloaders where possible. Duplicated entries will be silently ignored.
   * @param Lifetime The lifetime of cached classes in the classloader. If an instance of the classloader is alredy existing, the lifetime for loaded class is overwritten.
   * @param Sleeptime The sleeptime of the cleaner for internal cache. If an instance of the classloader is already existing, the sleeptime is changed for all classloader instances.
   * @param Parent The parent of this classloader. This is set only in the case a classloader object is not already existing.
   * @return The instance of the classloader.
   */
    public static CClassLoader newInstance(java.lang.String[] Classpaths, int Lifetime, int Sleeptime, java.lang.ClassLoader Parent) {
        if (p_Instance == null) {
            p_Instance = new CClassLoader(Parent);
            (new java.lang.Thread(p_Instance)).start();
        }
        addClasspath(Classpaths);
        setClassLifetime(Lifetime);
        setCleanerSleeptime(Sleeptime);
        return p_Instance;
    }

    /** <p>Cleanup process for the cache.</p>
   *
   * <p>The cleanup process is looking through the cache of the classloader and
   * deleting all elements from the cache that are indicated to be to old.<br>
   * <b>Never call this method since it's called automatically!</b></p>
   */
    public void run() {
        while (!p_Shutdown) {
            p_removeExpiredClasses();
            try {
                synchronized (this) {
                    wait(p_CleanerSleeptime);
                }
            } catch (java.lang.InterruptedException Ex) {
            }
        }
    }

    /** <p>Returns the cache policy of the classloader.</p>
   *
   * @return Cache policy.
   * @see #setCachePolicy(int)
   */
    public static synchronized int getCachePolicy() {
        return p_CachePolicy;
    }

    /** <p>Returns the average lifetime of a loaded class in the cache.</p>
   *
   * @return Lifetime of a loaded class in the cache in milliseconds.
   * @see #setClassLifetime(int)
   */
    public static synchronized int getClassLifetime() {
        return p_ClassLifetime;
    }

    /** <p>Returns array of all currently set classpaths.</p>
   *
   * @return {@link java.lang.String String} array of all currently set classpaths.
   */
    public static synchronized java.lang.String[] getClasspaths() {
        return (java.lang.String[]) (p_Classpaths.toArray(new java.lang.String[0]));
    }

    /** <p>Returns the sleeptime of the cache cleaning process.</p>
   *
   * @return Sleeptime of the cache cleaning process.
   * @see #setCleanerSleeptime(int)
   */
    public static synchronized int getCleanerSleeptime() {
        return p_CleanerSleeptime;
    }

    /** <p>String representation of this classloader.</p>
   *
   * @return String representation of this classloader.
   */
    public synchronized java.lang.String toString() {
        return (p_Instance.getClass().getName() + "[Registered classpaths: " + p_Classpaths.size() + " ; Loaded classes: " + p_LoadedClasses.size() + "]");
    }

    /** <p>Set caching policy.</p>
   *
   * <p>The caching policy is determining how class will be loaded that are no
   * more referenced by the class loaders cache but where objects still active.
   * The caching policy can be changed any time and affect all classes and
   * objects using this classloader to be created.</p>
   *
   * @param Policy The new policy to set.
   */
    public static synchronized void setCachePolicy(int Policy) {
        p_CachePolicy = Policy;
    }

    /** <p>Set new lifetime for classes.</p>
   *
   * <p>The lifetime of a class determines how long an instance of {@link
   * java.lang.Class java.lang.Class} remains in the cache until it's cleared
   * from the cache. A clearance from the cache doesn't means that an updated
   * version of the class can be loaded on next request. First, all objects of
   * the old class must be released and garbage collected.</p>
   *
   * @param NewLifetime The lifetime of a class handle in the cache in milliseconds. Negative values are converted to positive values and a 0 is concerted to a 1.
   */
    public static synchronized void setClassLifetime(int NewLifetime) {
        p_ClassLifetime = java.lang.Math.abs(NewLifetime);
    }

    /** <p>Set new timer for clean-up process of internal cache table.</p>
   *
   * <p>The sleeptime is the amount of time the cleaning process will wait
   * before looking through the cache for expired values. Depending on your
   * update strategy, a time interval of 6 hours should be a good choice.</p>
   *
   * @param NewSleeptime The sleeptime to set in milliseconds. Negative values are converted to positive values and a 0 is concerted to a 1.
   */
    public static synchronized void setCleanerSleeptime(int NewSleeptime) {
        p_CleanerSleeptime = java.lang.Math.abs(NewSleeptime);
        if (p_CleanerSleeptime == 0) {
            p_CleanerSleeptime++;
        }
    }

    /** <p>Enforce shutdown of class loader cache manager.</p>
   *
   * <p>The cache manager is loaded automatically by the classloader when
   * created.</p>
   */
    public void shutdown() {
        p_Shutdown = true;
    }

    /** <p>Add classpath to registered classpaths.</p>
   *
   * @param Classpath The classpath to add.
   */
    public static synchronized void addClasspath(java.lang.String Classpath) {
        p_Classpaths.add(Classpath);
    }

    /** <p>Add array of classpaths to registered classpaths.</p>
   *
   * @param Classpaths The classpaths to add.
   */
    public static synchronized void addClasspath(java.lang.String[] Classpaths) {
        p_Classpaths.addAll(java.util.Arrays.asList(Classpaths));
    }

    /** <p>Remove given classpath from registered classpaths.</p>
   *
   * @param Classpath The classpath to remove.
   * @return <b>true</b> if the classpath could be removed, <b>false</b> otherwise.
   */
    public static synchronized boolean removeClasspath(java.lang.String Classpath) {
        return p_Classpaths.remove(Classpath);
    }

    /** <p>Remove given array of classpaths from registered classpaths.</p>
   *
   * @param Classpaths The classpaths to remove.
   * @return <b>true</b> if all classpaths could be removed, <b>false</b> otherwise.
   */
    public static synchronized boolean removeClasspath(java.lang.String[] Classpaths) {
        return p_Classpaths.removeAll(java.util.Arrays.asList(Classpaths));
    }

    /** <p>Load class of given name.</p>
   *
   * <p>Delegates to {@link #loadClass(java.lang.String)
   * loadClass(java.lang.String)}. This, the parameter <i>Resolve</i> is
   * ignored.</p>
   *
   * @param Classname The name of class to load.
   * @param Resolve Ignored. Present for reasons of compatability.
   * @return Instance of newly created class.
   * @throws java.lang.ClassNotFoundException is thrown if the class could not be loaded.
   * @see #loadClass(java.lang.String)
   */
    public java.lang.Class loadClass(java.lang.String Classname, boolean Resolve) throws java.lang.ClassNotFoundException {
        return loadClass(Classname);
    }

    /** <p>Load class of given name.</p>
   *
   * <p>This method locates, loads and resolves the class representad by the
   * given <i>Classname</i>.<br>
   * The location process works as follows:<br><ol>
   * <li>Check internal cache</li>
   * <li>Check parent classloader if any</li>
   * <li>Check system classloader, usually known as the bootstrap loader</li>
   * <li>Check system environment if loaded by unreferenced classloader</li>
   * <li>Delegate to {@link #p_loadClass(java.lang.String)
   *     p_loadClass(java.lang.String)</li>
   * </ul><br>
   * A class that is loaded by an unreferenced classloader usually is a class
   * where an object remains active somewhere in the virtual machine. As long as
   * the virtual machine has any active object of a class, the class will not be
   * unloaded, even if the reference to the class and it's classloader is
   * destroyed already. In such a case, the caching policy is checked.<br>
   * If the cache policy enforces a reload, the loading fails with exception
   * stating that there is still any object of class to load. If a weak cache
   * policy is set, the class will be rebound to the internal cache. If no
   * policy is set, the reference of the class will be returned without further
   * manipulation.</p>
   *
   * @param Classname The class to load. This is a representation string, like <i>java.lang.String</i>.
   * @return Instance of {@link java.lang.Class java.lang.Class} representing the class passed by <i>Classname</i>. The class holds instance of this classloader. All operations on the classloader are fully transparent to all classloaders of this class.
   * @throws ClassNotFoundException Is thrown if the class identified by <i>Classname</i> could not be found and loaded.
   * @see java.lang.ClassLoader#loadClass(java.lang.String)
   */
    public synchronized java.lang.Class loadClass(java.lang.String Classname) throws java.lang.ClassNotFoundException {
        if (p_LoadedClasses.containsKey(Classname)) {
            return ((CClassLoader.CClassEntry) (p_LoadedClasses.get(Classname))).getClassInstance();
        }
        if (getParent() != null) {
            try {
                return getParent().loadClass(Classname);
            } catch (java.lang.ClassNotFoundException Ex) {
            }
        }
        try {
            return getSystemClassLoader().loadClass(Classname);
        } catch (java.lang.ClassNotFoundException Ex) {
        }
        java.lang.Class cls;
        if ((cls = findLoadedClass(Classname)) != null) {
            if ((p_CachePolicy & CACHEPOLICY_ENFORCERELOAD) == CACHEPOLICY_ENFORCERELOAD) {
                throw new ClassNotFoundException("The class of name '" + Classname + "' is still in use. Policy restrictions enforce reolading of classes. Please wait until all instance of the given class are garbage collected and unloaded by the Java Virtual Machine.");
            }
            if ((p_CachePolicy & CACHEPOLICY_WEAKRELOAD) == CACHEPOLICY_WEAKRELOAD) {
                p_LoadedClasses.put(Classname, new CClassLoader.CClassEntry(cls, new java.util.Date().getTime()));
            }
            return cls;
        }
        return ((new CClassLoader(getParent())).p_loadClass(Classname));
    }
}
