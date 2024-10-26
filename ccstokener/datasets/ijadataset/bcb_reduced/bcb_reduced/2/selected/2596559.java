package jorgan.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collection of utility methods supporting the boostrapping of an application.
 */
public class Bootstrap extends ThreadGroup implements Runnable {

    private static final String MANIFEST = "META-INF/MANIFEST.MF";

    private static final String BOOTSTRAP_CLASSPATH = "Bootstrap-classpath";

    private static final String BOOTSTRAP_CLASS = "Bootstrap-class";

    private static boolean bootstrapped = false;

    private static Logger logger = Logger.getLogger(Bootstrap.class.getName());

    private String[] args;

    private Bootstrap(String[] args) {
        super("bootstrap");
        this.args = args;
    }

    public void run() {
        try {
            Manifest manifest = getManifest();
            URL[] classpath = getClasspath(manifest);
            ClassLoader classloader = new URLClassLoader(classpath);
            Thread.currentThread().setContextClassLoader(classloader);
            Class<?> clazz = classloader.loadClass(getClass(manifest));
            Method method = clazz.getMethod("main", new Class[] { String[].class });
            method.invoke(null, new Object[] { args });
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "bootstrapping failed", t);
        }
    }

    /**
	 * Test is the current running program is bootstrapped, i.e. it was started
	 * through the main method of this class.
	 * 
	 * @return <code>true</code> if bootstrapped
	 */
    public static boolean isBootstrapped() {
        return bootstrapped;
    }

    /**
	 * Bootstrap with dynamically constructed classpath.
	 * 
	 * @param args
	 *            arguments
	 */
    public static void main(final String[] args) {
        bootstrapped = true;
        Bootstrap bootstrap = new Bootstrap(args);
        new Thread(bootstrap, bootstrap).start();
    }

    /**
	 * Get bootstrap class from manifest file information
	 */
    private static String getClass(Manifest mf) {
        String clazz = mf.getMainAttributes().getValue(BOOTSTRAP_CLASS);
        if (clazz == null || clazz.length() == 0) {
            throw new Error("No " + BOOTSTRAP_CLASS + " defined in " + MANIFEST);
        }
        return clazz;
    }

    /**
	 * Assemble classpath from manifest file information (optional).
	 */
    private URL[] getClasspath(Manifest manifest) throws MalformedURLException {
        String classpath = manifest.getMainAttributes().getValue(BOOTSTRAP_CLASSPATH);
        if (classpath == null) {
            classpath = "";
        }
        StringTokenizer tokens = new StringTokenizer(classpath, ",", false);
        List<URL> urls = new ArrayList<URL>();
        File directory = ClassUtils.getDirectory(getClass());
        while (tokens.hasMoreTokens()) {
            File file = new File(directory, tokens.nextToken());
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        urls.add(files[i].toURI().toURL());
                    }
                } else {
                    urls.add(file.toURI().toURL());
                }
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    /**
	 * Get our manifest file. Normally all (parent) classloaders of a class do
	 * provide resources and the enumeration returned on lookup of manifest.mf
	 * will start with the topmost classloader's resources. We're inverting that
	 * order to make sure we're consulting the manifest file in the same jar as
	 * this class if available.
	 */
    private static Manifest getManifest() throws IOException {
        Stack<URL> manifests = new Stack<URL>();
        for (Enumeration<URL> e = Bootstrap.class.getClassLoader().getResources(MANIFEST); e.hasMoreElements(); ) {
            manifests.add(e.nextElement());
        }
        while (!manifests.isEmpty()) {
            URL url = manifests.pop();
            InputStream in = url.openStream();
            Manifest manifest = new Manifest(in);
            in.close();
            if (manifest.getMainAttributes().getValue(BOOTSTRAP_CLASS) != null) {
                return manifest;
            }
        }
        throw new Error("No " + MANIFEST + " with " + BOOTSTRAP_CLASS + " found");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof ThreadDeath) {
            return;
        }
        logger.log(Level.WARNING, "uncaught exception", e);
    }
}
