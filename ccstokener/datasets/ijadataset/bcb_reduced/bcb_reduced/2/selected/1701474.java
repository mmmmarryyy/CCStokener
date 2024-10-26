package org.lightcommons.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import org.lightcommons.logger.LogFactory;

/**
 *
 * @author GL
 * @since 2008-4-14 ����04:57:30
 */
@Deprecated
public class IOUtil {

    public static String FILE_SEP = System.getProperty("file.separator");

    public static Properties getProperties(final String resource) {
        final Properties prop = new Properties();
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        try {
            if (is != null) {
                prop.load(is);
            }
        } catch (final IOException e) {
            LogFactory.getLog(IOUtil.class).error(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                }
            }
        }
        return prop;
    }

    private static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static URL getResource(final String name) {
        return getClassLoader().getResource(name);
    }

    public static String getText(final String resource, final String charset) {
        final InputStream is = getClassLoader().getResourceAsStream(resource);
        if (is == null) {
            return null;
        }
        return getText(is, charset);
    }

    public static String getText(final InputStream is, final String charset) {
        BufferedReader r;
        try {
            r = new BufferedReader(new InputStreamReader(is, charset));
        } catch (final UnsupportedEncodingException e1) {
            throw new RuntimeException("Unsupport charset:" + charset);
        }
        String l = null;
        String ret = "";
        try {
            while ((l = r.readLine()) != null) {
                ret = ret + l + "\n";
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * cycle load resources load /path/to/pack/xx.yy try
     * /path/to/pack/xx_zh_CN.yy try /path/to/pack/xx_zh.yy try
     * /path/to/pack/xx.yy try /path/to/xx.yy try /path/xx.yy try /xx.yy
     *
     * @param resourceName
     * @return
     */
    public static URL cycleGetUrl(final String resourceName) {
        final ClassLoader cl = getClassLoader();
        final String rn = resourceName.startsWith("/") ? resourceName : "/" + resourceName;
        final String[] parts = rn.split("/");
        final int l = parts.length;
        for (int i = l - 1; i > 0; i--) {
            String p = "";
            for (int j = 0; j < i; j++) {
                p = p + parts[j] + "/";
            }
            p = p + parts[l - 1];
            final URL url = cl.getResource(p.substring(1));
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    public static InputStream cycleGetResource(final String resourceName) {
        final URL url = cycleGetUrl(resourceName);
        if (url != null) {
            try {
                return url.openStream();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static InputStream cycleGetResource(final String resourceName, final Locale locale) {
        return null;
    }

    public static String cycleGetText(final String resourceName, final String charset) {
        return getText(cycleGetResource(resourceName), charset);
    }

    public static BufferedReader getReader(final String resource) {
        return new BufferedReader(new InputStreamReader(getClassLoader().getResourceAsStream(resource)));
    }

    public static List<URL> list(final URL url) {
        try {
            final List<URL> urls = new ArrayList<URL>();
            if ("file".equals(url.getProtocol())) {
                final File dir = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
                final String[] files = dir.list();
                for (final String f : files) {
                    urls.add(new File(dir.getPath() + FILE_SEP + f).toURL());
                }
            } else if ("jar".equals(url.getProtocol())) {
                final String path = URLDecoder.decode(url.getPath(), "UTF-8");
                final int e = path.lastIndexOf(".jar!") + 4;
                final String jarUrl = path.substring(0, e);
                String jar = jarUrl;
                if (jar.startsWith("file:/")) {
                    jar = jar.substring(6);
                }
                final String name = path.substring(e + 2);
                final JarFile jarFile = new JarFile(jar);
                final Enumeration<JarEntry> jes = jarFile.entries();
                while (jes.hasMoreElements()) {
                    final JarEntry je = jes.nextElement();
                    if (je.getName().startsWith(name)) {
                        final URL url2 = new URL("jar:" + jarUrl + "!/" + je.getName());
                        urls.add(url2);
                    }
                }
            }
            return urls;
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static List<URL> getResourcesForPackage(final String pkgName) throws ClassNotFoundException {
        final List<URL> urls = new ArrayList<URL>();
        try {
            final ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            final String path = pkgName.replace('.', '/');
            final Enumeration<URL> resources = cld.getResources(path);
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                urls.addAll(list(url));
            }
            return urls;
        } catch (final Throwable x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Attempts to list all the classes in the specified package as determined
     * by the context class loader
     *
     * @param pckgname
     *            the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException
     *             if something went wrong
     */
    public static List<Class> getClassesForPackage(final String pckgname) throws ClassNotFoundException {
        return null;
    }

    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    /**
     * @return
     */
    public static String inputln() {
        try {
            return br.readLine();
        } catch (final IOException e) {
        }
        return null;
    }
}
