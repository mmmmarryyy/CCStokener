package de.iritgo.aktera.core.container;

import org.apache.avalon.fortress.MetaInfoManager;
import org.apache.avalon.fortress.RoleManager;
import org.apache.avalon.fortress.impl.role.AbstractMetaInfoManager;
import org.apache.avalon.fortress.util.Service;
import org.apache.avalon.framework.activity.Initializable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This MetaInfoManager overrides the standard Fortress one very slightly:
 * instead of getResources() to find meta-data, we use only those resources
 * available through the URLClassLoader.
 */
public final class KeelMetaInfoManager extends AbstractMetaInfoManager implements Initializable {

    /**
	 * Create a ServiceMetaManager.
	 */
    public KeelMetaInfoManager() {
        super((MetaInfoManager) null);
    }

    /**
	 * Create a ServiceMetaManager with a parent RoleManager.
	 *
	 * @param parent
	 */
    public KeelMetaInfoManager(final RoleManager parent) {
        super(parent);
    }

    /**
	 * Create a ServiceMetaManager with a parent RoleManager.
	 *
	 * @param parent
	 */
    public KeelMetaInfoManager(final MetaInfoManager parent) {
        super(parent);
    }

    /**
	 * Create a ServiceMetaManager with the supplied classloader and
	 * parent RoleManager.
	 *
	 * @param parent
	 * @param loader
	 */
    public KeelMetaInfoManager(final MetaInfoManager parent, final ClassLoader loader) {
        super(parent, loader);
    }

    /**
	 * Initialize the ServiceMetaManager by looking at all the services and
	 * classes available in the system.
	 *
	 * @throws Exception if there is a problem
	 */
    public void initialize() throws Exception {
        final Set services = new HashSet();
        Enumeration enumeration = null;
        if (getLoader() instanceof URLClassLoader) {
            URLClassLoader ul = (URLClassLoader) getLoader();
            enumeration = cleanUrlList(ul.findResources("services.list"));
        } else {
            enumeration = cleanUrlList(getLoader().getResources("services.list"));
        }
        int serviceCount = 0;
        while (enumeration.hasMoreElements()) {
            serviceCount++;
            readEntries(services, (URL) enumeration.nextElement());
        }
        if (serviceCount == 0) {
            System.err.println("[KeelMetaInfoManager] WARNING: No services configured");
        }
        final Iterator it = services.iterator();
        while (it.hasNext()) {
            final String role = (String) it.next();
            getLogger().debug("Adding service: " + role);
            try {
                setupImplementations(role);
            } catch (Exception e) {
                getLogger().debug("Specified service '" + role + "' is not available", e);
            }
        }
    }

    public Enumeration cleanUrlList(Enumeration originalURLs) {
        Vector v = new Vector();
        int origSize = 0;
        int valids = 0;
        ClassLoader l = getClass().getClassLoader();
        if (l instanceof URLClassLoader) {
            URLClassLoader ul = (URLClassLoader) l;
            URL[] validUrls = ul.getURLs();
            if (validUrls.length == 0) {
                return originalURLs;
            }
            while (originalURLs.hasMoreElements()) {
                origSize++;
                URL oneURL = (URL) originalURLs.nextElement();
                StringTokenizer stk = new StringTokenizer(oneURL.getFile(), "!");
                String checkUrl = stk.nextToken();
                for (int i = 0; i < validUrls.length; i++) {
                    if (validUrls[i].toString().equals(checkUrl)) {
                        v.addElement(oneURL);
                        valids++;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Incorrect type of classloader: Using a " + l.getClass().getName());
        }
        if (valids == 0) {
            System.err.println("[KeelMetaInfoManager] WARNING: There were no valid URLs");
        }
        return v.elements();
    }

    /**
	 * Get all the implementations of a service and set up their meta
	 * information.
	 *
	 * @param role  The role name we are reading implementations for.
	 *
	 * @throws ClassNotFoundException if the role or component cannot be found
	 */
    private void setupImplementations(final String role) throws ClassNotFoundException {
        final Iterator it = Service.providers(getLoader().loadClass(role), getLoader());
        while (it.hasNext()) {
            final String impl = ((Class) it.next()).getName();
            getLogger().debug("Reading meta info for " + impl);
            if (!isAlreadyAdded(impl)) {
                readMeta(role, impl);
            } else {
                addComponent(role, impl, null, null);
            }
        }
    }

    /**
	 * Read the meta information in and actually add the role.
	 *
	 * @param role
	 * @param implementation
	 */
    private void readMeta(final String role, final String implementation) {
        final Properties meta = new Properties();
        final List deps = new ArrayList();
        try {
            final InputStream stream = getLoader().getResourceAsStream(getMetaFile(implementation));
            if (stream != null) {
                meta.load(stream);
            } else {
                getLogger().error("Meta information for " + implementation + " unavailable, skipping this class.");
                return;
            }
        } catch (IOException ioe) {
            getLogger().error("Could not load meta information for " + implementation + ", skipping this class.");
            return;
        }
        try {
            URL depURL = getLoader().getResource(getDepFile(implementation));
            if (depURL == null) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("No dependencies for " + implementation + ".");
                }
            } else {
                HashSet set = new HashSet();
                readEntries(set, depURL);
                deps.addAll(set);
            }
        } catch (Exception ioe) {
            getLogger().debug("Could not load dependencies for " + implementation + ".", ioe);
        }
        addComponent(role, implementation, meta, deps);
    }

    /**
	 * Translate a class name into the meta file name.
	 *
	 * @param implementation
	 * @return String
	 */
    private String getMetaFile(final String implementation) {
        String entry = implementation.replace('.', '/');
        entry += ".meta";
        return entry;
    }

    /**
	 * Translate a class name into the meta file name.
	 *
	 * @param implementation
	 * @return String
	 */
    private String getDepFile(final String implementation) {
        String entry = implementation.replace('.', '/');
        entry += ".deps";
        return entry;
    }

    /**
	 * Read entries in a list file and add them all to the provided Set.
	 *
	 * @param entries
	 * @param url
	 *
	 * @throws IOException if we cannot read the entries
	 */
    private void readEntries(final Set entries, final URL url) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            String entry = reader.readLine();
            while (entry != null) {
                entries.add(entry);
                entry = reader.readLine();
            }
        } finally {
            reader.close();
        }
    }

    protected void addComponent(final String role, final String className, final Properties meta, final List deps) {
        super.addComponent(role, className, meta, deps);
        getLogger().info("[KeelMetaInfoManager] Added role '" + role + "' with class '" + className + "'");
    }

    /** Get the classloader used for the RoleManager for any class that
	 * extends this one.
	 *
	 * @return ClassLoader
	 */
    protected ClassLoader getLoader() {
        return getClass().getClassLoader();
    }
}
