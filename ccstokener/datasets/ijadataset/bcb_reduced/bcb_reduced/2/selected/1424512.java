package org.codehaus.classworlds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Implementation of <code>ClassRealm</code>.  The realm is the class loading gateway.
 * The search is proceded as follows:
 * <ol>
 * <li>Search the parent class loader (passed via the constructor) if there
 * is one.</li>
 * <li>Search the imports.</li>
 * <li>Search this realm's constituents.</li>
 * <li>Search the parent realm.</li>
 * </ol>
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @version $Id: DefaultClassRealm.java 126 2006-01-12 04:17:51Z  $
 * @todo allow inheritance to be turn on/off at runtime.
 * @todo allow direction of search
 */
public class DefaultClassRealm implements ClassRealm {

    private ClassWorld world;

    private String id;

    private TreeSet imports;

    private ClassLoader foreignClassLoader;

    private RealmClassLoader classLoader;

    private ClassRealm parent;

    public DefaultClassRealm(ClassWorld world, String id) {
        this(world, id, null);
    }

    public DefaultClassRealm(ClassWorld world, String id, ClassLoader foreignClassLoader) {
        this.world = world;
        this.id = id;
        imports = new TreeSet();
        if (foreignClassLoader != null) {
            this.foreignClassLoader = foreignClassLoader;
        }
        if ("true".equals(System.getProperty("classworlds.bootstrapped"))) {
            classLoader = new UberJarRealmClassLoader(this);
        } else {
            classLoader = new RealmClassLoader(this);
        }
    }

    public URL[] getConstituents() {
        return classLoader.getURLs();
    }

    public ClassRealm getParent() {
        return parent;
    }

    public void setParent(ClassRealm parent) {
        this.parent = parent;
    }

    public String getId() {
        return this.id;
    }

    public ClassWorld getWorld() {
        return this.world;
    }

    public void importFrom(String realmId, String packageName) throws NoSuchRealmException {
        imports.add(new Entry(getWorld().getRealm(realmId), packageName));
        imports.add(new Entry(getWorld().getRealm(realmId), packageName.replace('.', '/')));
    }

    public void addConstituent(URL constituent) {
        classLoader.addConstituent(constituent);
    }

    /**
     *  Adds a byte[] class definition as a constituent for locating classes.
     *  Currently uses BytesURLStreamHandler to hold a reference of the byte[] in memory.
     *  This ensures we have a unifed URL resource model for all constituents.
     *  The code to cache to disk is commented out - maybe a property to choose which method?
     *
     *  @param constituent class name
     *  @param b the class definition as a byte[]
     */
    public void addConstituent(String constituent, byte[] b) throws ClassNotFoundException {
        try {
            File path, file;
            if (constituent.lastIndexOf('.') != -1) {
                path = new File("byteclass/" + constituent.substring(0, constituent.lastIndexOf('.') + 1).replace('.', File.separatorChar));
                file = new File(path, constituent.substring(constituent.lastIndexOf('.') + 1) + ".class");
            } else {
                path = new File("byteclass/");
                file = new File(path, constituent + ".class");
            }
            addConstituent(new URL(null, file.toURL().toExternalForm(), new BytesURLStreamHandler(b)));
        } catch (java.io.IOException e) {
            throw new ClassNotFoundException("Couldn't load byte stream.", e);
        }
    }

    public ClassRealm locateSourceRealm(String classname) {
        for (Iterator iterator = imports.iterator(); iterator.hasNext(); ) {
            Entry entry = (Entry) iterator.next();
            if (entry.matches(classname)) {
                return entry.getRealm();
            }
        }
        return this;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ClassRealm createChildRealm(String id) throws DuplicateRealmException {
        ClassRealm childRealm = getWorld().newRealm(id);
        childRealm.setParent(this);
        return childRealm;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("org.codehaus.classworlds.")) {
            return getWorld().loadClass(name);
        }
        try {
            if (foreignClassLoader != null) {
                try {
                    return foreignClassLoader.loadClass(name);
                } catch (ClassNotFoundException e) {
                }
            }
            ClassRealm sourceRealm = locateSourceRealm(name);
            if (sourceRealm == this) {
                return classLoader.loadClassDirect(name);
            } else {
                try {
                    return sourceRealm.loadClass(name);
                } catch (ClassNotFoundException cnfe) {
                    return classLoader.loadClassDirect(name);
                }
            }
        } catch (ClassNotFoundException e) {
            if (getParent() != null) {
                return getParent().loadClass(name);
            }
            throw e;
        }
    }

    public URL getResource(String name) {
        URL resource = null;
        name = UrlUtils.normalizeUrlPath(name);
        if (foreignClassLoader != null) {
            resource = foreignClassLoader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        ClassRealm sourceRealm = locateSourceRealm(name);
        if (sourceRealm == this) {
            resource = classLoader.getResourceDirect(name);
        } else {
            resource = sourceRealm.getResource(name);
            if (resource == null) {
                resource = classLoader.getResourceDirect(name);
            }
        }
        if (resource == null && getParent() != null) {
            resource = getParent().getResource(name);
        }
        return resource;
    }

    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        InputStream is = null;
        if (url != null) {
            try {
                is = url.openStream();
            } catch (IOException e) {
            }
        }
        return is;
    }

    public Enumeration findResources(String name) throws IOException {
        name = UrlUtils.normalizeUrlPath(name);
        Vector resources = new Vector();
        if (foreignClassLoader != null) {
            for (Enumeration res = foreignClassLoader.getResources(name); res.hasMoreElements(); ) {
                resources.addElement(res.nextElement());
            }
        }
        ClassRealm sourceRealm = locateSourceRealm(name);
        if (sourceRealm != this) {
            for (Enumeration res = sourceRealm.findResources(name); res.hasMoreElements(); ) {
                resources.addElement(res.nextElement());
            }
        }
        for (Enumeration direct = classLoader.findResourcesDirect(name); direct.hasMoreElements(); ) {
            resources.addElement(direct.nextElement());
        }
        if (parent != null) {
            for (Enumeration parent = getParent().findResources(name); parent.hasMoreElements(); ) {
                resources.addElement(parent.nextElement());
            }
        }
        return resources.elements();
    }

    public void display() {
        ClassRealm cr = this;
        System.out.println("-----------------------------------------------------");
        showUrls(cr);
        while (cr.getParent() != null) {
            System.out.println("\n");
            cr = cr.getParent();
            showUrls(cr);
        }
        System.out.println("-----------------------------------------------------");
    }

    private void showUrls(ClassRealm classRealm) {
        System.out.println("this realm = " + classRealm.getId());
        URL[] urls = classRealm.getConstituents();
        for (int i = 0; i < urls.length; i++) {
            System.out.println("urls[" + i + "] = " + urls[i]);
        }
        System.out.println("Number of imports: " + imports.size());
        for (Iterator i = imports.iterator(); i.hasNext(); ) {
            System.out.println("import: " + i.next());
        }
    }
}
