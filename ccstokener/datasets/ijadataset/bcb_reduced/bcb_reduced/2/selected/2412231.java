package org.mozilla.javascript;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Attila Szegedi
 */
public abstract class SecureCaller {

    private static final byte[] secureCallerImplBytecode = loadBytecode();

    private static final Map callers = new WeakHashMap();

    public abstract Object call(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args);

    /**
     * Call the specified callable using a protection domain belonging to the 
     * specified code source. 
     */
    static Object callSecurely(final CodeSource codeSource, Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        final Thread thread = Thread.currentThread();
        final ClassLoader classLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return thread.getContextClassLoader();
            }
        });
        Map classLoaderMap;
        synchronized (callers) {
            classLoaderMap = (Map) callers.get(codeSource);
            if (classLoaderMap == null) {
                classLoaderMap = new WeakHashMap();
                callers.put(codeSource, classLoaderMap);
            }
        }
        SecureCaller caller;
        synchronized (classLoaderMap) {
            SoftReference ref = (SoftReference) classLoaderMap.get(classLoader);
            if (ref != null) {
                caller = (SecureCaller) ref.get();
            } else {
                caller = null;
            }
            if (caller == null) {
                try {
                    caller = (SecureCaller) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                        public Object run() throws Exception {
                            ClassLoader effectiveClassLoader;
                            Class thisClass = getClass();
                            if (classLoader.loadClass(thisClass.getName()) != thisClass) {
                                effectiveClassLoader = thisClass.getClassLoader();
                            } else {
                                effectiveClassLoader = classLoader;
                            }
                            SecureClassLoaderImpl secCl = new SecureClassLoaderImpl(effectiveClassLoader);
                            Class c = secCl.defineAndLinkClass(SecureCaller.class.getName() + "Impl", secureCallerImplBytecode, codeSource);
                            return c.newInstance();
                        }
                    });
                    classLoaderMap.put(classLoader, new SoftReference(caller));
                } catch (PrivilegedActionException ex) {
                    throw new UndeclaredThrowableException(ex.getCause());
                }
            }
        }
        return caller.call(callable, cx, scope, thisObj, args);
    }

    private static class SecureClassLoaderImpl extends SecureClassLoader {

        SecureClassLoaderImpl(ClassLoader parent) {
            super(parent);
        }

        Class defineAndLinkClass(String name, byte[] bytes, CodeSource cs) {
            Class cl = defineClass(name, bytes, 0, bytes.length, cs);
            resolveClass(cl);
            return cl;
        }
    }

    private static byte[] loadBytecode() {
        return (byte[]) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return loadBytecodePrivileged();
            }
        });
    }

    private static byte[] loadBytecodePrivileged() {
        URL url = SecureCaller.class.getResource("SecureCallerImpl.clazz");
        try {
            InputStream in = url.openStream();
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (; ; ) {
                    int r = in.read();
                    if (r == -1) {
                        return bout.toByteArray();
                    }
                    bout.write(r);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
