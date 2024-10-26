package sun.misc;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLClassLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;

public class MIDletClassLoader extends URLClassLoader {

    URL myBase[];

    String[] systemPkgs;

    private MemberFilter memberChecker;

    private PermissionCollection perms;

    private HashSet badMidletClassnames = new HashSet();

    private MIDPImplementationClassLoader implementationClassLoader;

    private AccessControlContext ac = AccessController.getContext();

    private boolean enableFilter;

    private ClassLoader auxClassLoader;

    public MIDletClassLoader(URL base[], String systemPkgs[], PermissionCollection pc, MemberFilter mf, MIDPImplementationClassLoader parent, boolean enableFilter, ClassLoader auxClassLoader) {
        super(base, parent);
        myBase = base;
        this.systemPkgs = systemPkgs;
        memberChecker = mf;
        perms = pc;
        implementationClassLoader = parent;
        this.enableFilter = enableFilter;
        this.auxClassLoader = auxClassLoader;
    }

    protected PermissionCollection getPermissions(CodeSource cs) {
        URL srcLocation = cs.getLocation();
        for (int i = 0; i < myBase.length; i++) {
            if (srcLocation.equals(myBase[i])) {
                return perms;
            }
        }
        return super.getPermissions(cs);
    }

    private boolean packageCheck(String pkg) {
        String forbidden[] = systemPkgs;
        int fLength = forbidden.length;
        for (int i = 0; i < fLength; i++) {
            if (pkg.startsWith(forbidden[i])) {
                return true;
            }
        }
        return MIDPPkgChecker.checkPackage(pkg);
    }

    private Class loadFromUrl(String classname) throws ClassNotFoundException {
        Class newClass;
        try {
            newClass = super.findClass(classname);
        } catch (Exception e) {
            return null;
        }
        if (newClass == null) return null;
        int idx = classname.lastIndexOf('.');
        if (idx != -1) {
            String pkg = classname.substring(0, idx);
            if (packageCheck(pkg)) {
                throw new ClassNotFoundException(classname + ". Prohibited package name: " + pkg);
            }
        }
        try {
            if (enableFilter) {
                memberChecker.checkMemberAccessValidity(newClass);
            }
            return newClass;
        } catch (Error e) {
            badMidletClassnames.add(classname);
            throw new ClassNotFoundException(e.getMessage());
        }
    }

    public synchronized Class loadClass(String classname, boolean resolve) throws ClassNotFoundException {
        Class resultClass;
        Throwable err = null;
        int i = classname.lastIndexOf('.');
        if (i != -1) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPackageAccess(classname.substring(0, i));
            }
        }
        classname = classname.intern();
        if (badMidletClassnames.contains(classname)) {
            throw new ClassNotFoundException(classname.concat(" contains illegal member reference"));
        }
        resultClass = findLoadedClass(classname);
        if (resultClass == null) {
            try {
                resultClass = implementationClassLoader.loadClass(classname, false, enableFilter);
            } catch (ClassNotFoundException e) {
            } catch (NoClassDefFoundError e) {
            }
        }
        if (resultClass == null) {
            try {
                resultClass = loadFromUrl(classname);
            } catch (ClassNotFoundException e) {
                err = e;
            } catch (NoClassDefFoundError e) {
                err = e;
            }
        }
        if (resultClass == null && auxClassLoader != null) {
            resultClass = auxClassLoader.loadClass(classname);
        }
        if (resultClass == null) {
            if (err == null) {
                throw new ClassNotFoundException(classname);
            } else {
                if (err instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) err;
                } else {
                    throw (NoClassDefFoundError) err;
                }
            }
        }
        if (resolve) {
            resolveClass(resultClass);
        }
        return resultClass;
    }

    public InputStream getResourceAsStream(String name) {
        if (name.endsWith(".class")) {
            return null;
        }
        int i;
        while ((i = name.indexOf("/./")) >= 0) {
            name = name.substring(0, i) + name.substring(i + 2);
        }
        i = 0;
        int limit;
        while ((i = name.indexOf("/../", i)) > 0) {
            if ((limit = name.lastIndexOf('/', i - 1)) >= 0) {
                name = name.substring(0, limit) + name.substring(i + 3);
                i = 0;
            } else {
                i = i + 3;
            }
        }
        if (name.startsWith("/") || name.startsWith(File.separator)) {
            name = name.substring(1);
        }
        final String n = name;
        InputStream retval;
        retval = (InputStream) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                URL url = findResource(n);
                try {
                    return url != null ? url.openStream() : null;
                } catch (IOException e) {
                    return null;
                }
            }
        }, ac);
        return retval;
    }
}
