package org.owasp.jxt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * JxtLoader
 *
 * @author Jeffrey Ichnowski
 * @version $Revision: 8 $
 */
final class JxtLoader extends URLClassLoader {

    private ClassLoader _parent;

    public JxtLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        _parent = parent;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class cls = findLoadedClass(name);
        if (cls != null) {
            if (resolve) {
                resolveClass(cls);
            }
            return cls;
        }
        if (!name.startsWith(JxtEngine.JXT_PACKAGE + '.')) {
            cls = _parent.loadClass(name);
            if (resolve) {
                resolveClass(cls);
            }
            return cls;
        }
        return findClass(name);
    }

    /** Delegate to parent */
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream in = _parent.getResourceAsStream(name);
        if (in == null) {
            URL url = findResource(name);
            if (url != null) {
                try {
                    in = url.openStream();
                } catch (IOException e) {
                    in = null;
                }
            }
        }
        return in;
    }
}
