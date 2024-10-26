package alt.jiapi.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import alt.jiapi.InstrumentationContext;
import alt.jiapi.Runtime;
import alt.jiapi.reflect.JiapiClass;
import alt.jiapi.JiapiException;

/**
 * A sample ClassLoader which can be used when instrumenting Java applications.
 * It knows how to load classes from CLASSPATH.
 * <p>
 * NOTE: Should the jar files found from jre/lib/ext
 * (System.getProperty("java.ext.dirs")) also be added to search path?
 * 
 * @author Mika Riekkinen
 * @author Joni Suominen
 * @version $Revision: 1.9 $ $Date: 2011/08/19 12:47:08 $
 */
public class InstrumentingClassLoader extends URLClassLoader {

    private static Category log = Runtime.getLogCategory(InstrumentingClassLoader.class);

    protected Map<String, Class<?>> classes;

    protected InstrumentationContext ctx;

    /**
	 * Creates a new InstrumentingClassloader.
	 */
    public static ClassLoader createClassLoader() {
        return createClassLoader((InstrumentationContext) null);
    }

    /**
	 * Creates a new InstrumentingClassloader.
	 */
    public static ClassLoader createClassLoader(InstrumentationContextProvider icp) throws JiapiException {
        return createClassLoader(icp.getInstrumentationContext());
    }

    /**
	 * Creates a new InstrumentingClassloader.
	 */
    public static ClassLoader createClassLoader(InstrumentationContextProvider icp, ClassLoader parent) throws JiapiException {
        return createClassLoader(icp.getInstrumentationContext(), parent);
    }

    /**
	 * Creates a new InstrumentingClassloader.
	 */
    public static ClassLoader createClassLoader(InstrumentationContext ctx) {
        return createClassLoader(ctx, getSystemClassLoader());
    }

    /**
	 * Creates a new InstrumentingClassloader.
	 */
    public static ClassLoader createClassLoader(InstrumentationContext ctx, ClassLoader parent) {
        URL urls[] = getClassPathUrls();
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkCreateClassLoader();
        }
        return new InstrumentingClassLoader(ctx, urls, parent);
    }

    protected InstrumentingClassLoader(InstrumentationContext ctx, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.ctx = ctx;
        classes = Collections.synchronizedMap(new HashMap());
    }

    protected synchronized Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> cl = null;
        JiapiClass jiapiClass = null;
        log.debug("loadClass(" + className + ")");
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("sun.") || className.startsWith("alt.jiapi.")) {
            return getParent().loadClass(className);
        }
        if ((cl = classes.get(className)) == null) {
            log.debug("cache miss: " + className);
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                int i = className.lastIndexOf('.');
                if (i != -1) {
                    sm.checkPackageAccess(className.substring(0, i));
                }
            }
            if (ctx == null) {
                return bootstrap(className);
            }
            String path = className.replace('.', '/').concat(".class");
            URL location = super.findResource(path);
            if (location == null) {
                return getParent().loadClass(className);
            }
            try {
                jiapiClass = ctx.getLoader().loadClass(className, location);
            } catch (java.io.IOException ioe) {
                throw new ClassNotFoundException(className);
            }
            ctx.instrument(jiapiClass);
            byte[] bytes = jiapiClass.getByteCode();
            if (System.getProperty("dump") != null) {
                try {
                    jiapiClass.dump(new java.io.FileOutputStream(jiapiClass.getName() + ".dump"));
                } catch (Throwable t) {
                }
            }
            if (bytes != null) {
                CodeSource cs = createCodeSource(location);
                cl = defineClass(className, bytes, 0, bytes.length, cs);
            }
            log.debug(cl + " was loaded with " + cl.getClassLoader());
            classes.put(className, cl);
        }
        if (resolve) {
            resolveClass(cl);
        }
        return cl;
    }

    /**
	 * Form a CodeSource for loaded class.
	 * 
	 * @param location
	 *            a location where the class was loaded from
	 */
    protected CodeSource createCodeSource(URL location) {
        Certificate[] certs = null;
        if (location.getProtocol().equals("jar")) {
            try {
                JarURLConnection jarConnection = (JarURLConnection) location.openConnection();
                certs = jarConnection.getCertificates();
                location = jarConnection.getJarFileURL();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new RuntimeException(ioe.getMessage());
            }
        }
        return new CodeSource(location, certs);
    }

    /**
	 * Creates an array of URLs which are used for seaching classes.
	 */
    private static URL[] getClassPathUrls() {
        String string = System.getProperty("java.class.path");
        List<URL> urls = new ArrayList<URL>();
        if (string != null) {
            StringTokenizer st = new StringTokenizer(string, File.pathSeparator);
            while (st.hasMoreTokens()) {
                try {
                    urls.add((new File(st.nextToken())).toURI().toURL());
                } catch (IOException ioe) {
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    /**
	 * Set the context for the ClassLoader.
	 * 
	 * @param ctx
	 *            an InstrumentationContext to be used
	 */
    public void setContext(InstrumentationContext ctx) {
        this.ctx = ctx;
    }

    public Class<?> bootstrap(String className) throws ClassNotFoundException {
        String s = className.replace('.', '/') + ".class";
        return findClass(s);
    }

    byte[] byteBuffer = new byte[65000];

    public Class<?> findClass(String name) throws ClassNotFoundException {
        log.debug("findClass(" + name + ")");
        Class<?> c = null;
        try {
            URL url = findResource(name);
            if (url == null) {
                throw new ClassNotFoundException(name);
            }
            java.io.InputStream is = url.openStream();
            int count = 0;
            while (is.available() > 0) {
                int elemsRead = is.read(byteBuffer, count, is.available());
                if (elemsRead == -1) {
                    break;
                }
                count += elemsRead;
            }
            c = defineClass(name.substring(0, name.lastIndexOf('.')), byteBuffer, 0, count);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            throw new ClassNotFoundException(name);
        }
        classes.put(c.getName(), c);
        return c;
    }
}
