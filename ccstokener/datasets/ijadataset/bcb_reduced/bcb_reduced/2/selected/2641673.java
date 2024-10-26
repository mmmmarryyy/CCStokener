package org.openide.util.lookup;

import org.openide.util.*;
import java.io.*;
import java.net.URL;
import java.util.*;

/** A lookup that implements the JDK1.3 JAR services mechanism and delegates
 * to META-INF/services/name.of.class files.
 * <p>It is not dynamic - so if you need to change the classloader or JARs,
 * wrap it in a ProxyLookup and change the delegate when necessary.
 * Existing instances will be kept if the implementation classes are unchanged,
 * so there is "stability" in doing this provided some parent loaders are the same
 * as the previous ones.
 * <p>If this is to be made public, please move it to the org.openide.util.lookup
 * package; currently used by the core via reflection, until it is needed some
 * other way.
 * @author Jaroslav Tulach, Jesse Glick
 * @see "#14722"
 */
final class MetaInfServicesLookup extends AbstractLookup {

    private static final boolean DEBUG = Boolean.getBoolean("org.openide.util.lookup.MetaInfServicesLookup.DEBUG");

    private static final Map knownInstances = new WeakHashMap();

    /** A set of all requested classes.
     * Note that classes that we actually succeeded on can never be removed
     * from here because we hold a strong reference to the loader.
     * However we also hold classes which are definitely not loadable by
     * our loader.
     */
    private final Set classes = new WeakSet();

    /** class loader to use */
    private final ClassLoader loader;

    /** Create a lookup reading from the classpath.
     * That is, the same classloader as this class itself.
     */
    public MetaInfServicesLookup() {
        this(MetaInfServicesLookup.class.getClassLoader());
    }

    /** Create a lookup reading from a specified classloader.
     */
    public MetaInfServicesLookup(ClassLoader loader) {
        this.loader = loader;
        if (DEBUG) {
            System.err.println("Created: " + this);
        }
    }

    public String toString() {
        return "MetaInfServicesLookup[" + loader + "]";
    }

    protected final void beforeLookup(Lookup.Template t) {
        Class c = t.getType();
        Object listeners;
        synchronized (this) {
            if (classes.add(c)) {
                Collection arr = getPairsAsLHS();
                search(c, arr);
                listeners = setPairsAndCollectListeners(arr);
            } else {
                return;
            }
        }
        notifyCollectedListeners(listeners);
    }

    /** Finds all pairs and adds them to the collection.
     *
     * @param clazz class to find
     * @param result collection to add Pair to
     */
    private void search(Class clazz, Collection result) {
        if (DEBUG) {
            System.err.println("Searching for " + clazz.getName() + " in " + clazz.getClassLoader() + " from " + this);
        }
        String res = "META-INF/services/" + clazz.getName();
        Enumeration en;
        try {
            en = loader.getResources(res);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        List foundClasses = new ArrayList();
        Collection removeClasses = new ArrayList();
        boolean foundOne = false;
        while (en.hasMoreElements()) {
            if (!foundOne) {
                foundOne = true;
                Class realMcCoy = null;
                try {
                    realMcCoy = loader.loadClass(clazz.getName());
                } catch (ClassNotFoundException cnfe) {
                }
                if (realMcCoy != clazz) {
                    if (DEBUG) {
                        if (realMcCoy != null) {
                            System.err.println(clazz.getName() + " is not the real McCoy! Actually found it in " + realMcCoy.getClassLoader());
                        } else {
                            System.err.println(clazz.getName() + " could not be found in " + loader);
                        }
                    }
                    return;
                }
            }
            URL url = (URL) en.nextElement();
            Item currentItem = null;
            try {
                InputStream is = url.openStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        line = line.trim();
                        if (line.startsWith("#position=")) {
                            if (currentItem == null) {
                                assert false : "Found line '" + line + "' but there is no item to associate it with!";
                            }
                            try {
                                currentItem.position = Integer.parseInt(line.substring(10));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (currentItem != null) {
                            insertItem(currentItem, foundClasses);
                            currentItem = null;
                        }
                        if (line.length() == 0) {
                            continue;
                        }
                        boolean remove = false;
                        if (line.charAt(0) == '#') {
                            if ((line.length() == 1) || (line.charAt(1) != '-')) {
                                continue;
                            }
                            remove = true;
                            line = line.substring(2);
                        }
                        Class inst = null;
                        try {
                            inst = Class.forName(line, false, loader);
                        } catch (ClassNotFoundException cnfe) {
                            if (remove) {
                                continue;
                            } else {
                                throw cnfe;
                            }
                        }
                        if (!clazz.isAssignableFrom(inst)) {
                            if (DEBUG) {
                                System.err.println("Not a subclass");
                            }
                            throw new ClassNotFoundException(inst.getName() + " not a subclass of " + clazz.getName());
                        }
                        if (remove) {
                            removeClasses.add(inst);
                        } else {
                            currentItem = new Item();
                            currentItem.clazz = inst;
                        }
                    }
                    if (currentItem != null) {
                        insertItem(currentItem, foundClasses);
                        currentItem = null;
                    }
                } finally {
                    is.close();
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (DEBUG) {
            System.err.println("Found impls of " + clazz.getName() + ": " + foundClasses + " and removed: " + removeClasses + " from: " + this);
        }
        foundClasses.removeAll(removeClasses);
        Iterator it = foundClasses.iterator();
        while (it.hasNext()) {
            Item item = (Item) it.next();
            if (removeClasses.contains(item.clazz)) {
                continue;
            }
            result.add(new P(item.clazz));
        }
    }

    /**
     * Insert item to the list according to item.position value.
     */
    private void insertItem(Item item, List list) {
        if (item.position == -1) {
            list.add(item);
            return;
        }
        int index = -1;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            index++;
            Item i = (Item) it.next();
            if (i.position == -1) {
                list.add(index, item);
                return;
            } else {
                if (i.position > item.position) {
                    list.add(index, item);
                    return;
                }
            }
        }
        list.add(item);
    }

    private static class Item {

        private Class clazz;

        private int position = -1;
    }

    /** Pair that holds name of a class and maybe the instance.
     */
    private static final class P extends Pair {

        /** May be one of three things:
         * 1. The implementation class which was named in the services file.
         * 2. An instance of it.
         * 3. Null, if creation of the instance resulted in an error.
         */
        private Object object;

        public P(Class clazz) {
            this.object = clazz;
        }

        /** Finds the class.
         */
        private Class clazz() {
            Object o = object;
            if (o instanceof Class) {
                return (Class) o;
            } else if (o != null) {
                return o.getClass();
            } else {
                return Object.class;
            }
        }

        public boolean equals(Object o) {
            if (o instanceof P) {
                return ((P) o).clazz().equals(clazz());
            }
            return false;
        }

        public int hashCode() {
            return clazz().hashCode();
        }

        protected boolean instanceOf(Class c) {
            return c.isAssignableFrom(clazz());
        }

        public Class getType() {
            return clazz();
        }

        public Object getInstance() {
            Object o = object;
            if (o instanceof Class) {
                synchronized (o) {
                    try {
                        Class c = ((Class) o);
                        synchronized (knownInstances) {
                            o = knownInstances.get(c);
                        }
                        if (o == null) {
                            o = c.newInstance();
                            synchronized (knownInstances) {
                                knownInstances.put(c, o);
                            }
                        }
                        object = o;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        object = null;
                    }
                }
            }
            return object;
        }

        public String getDisplayName() {
            return clazz().getName();
        }

        public String getId() {
            return clazz().getName();
        }

        protected boolean creatorOf(Object obj) {
            return obj == object;
        }
    }
}
