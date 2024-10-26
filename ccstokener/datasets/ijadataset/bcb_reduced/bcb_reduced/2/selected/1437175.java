package org.lightframework.mvc.internal.clazz;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility class to find classes or resources
 *
 * @author fenghm (live.fenghm@gmail.com)
 *
 * @since 1.0.0
 */
public class ClassFinder {

    private static final Logger log = LoggerFactory.getLogger(ClassFinder.class);

    public static Set<String> findClassNames(URL url, String packagee) throws IOException {
        return findClassNames(url, packagee, true);
    }

    public static Set<String> findClassNames(URL url, String packagee, boolean deep) throws IOException {
        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            return findClassNames(((JarURLConnection) conn).getJarFile(), packagee, deep);
        }
        String urlProtocol = url.getProtocol();
        String urlFileName = url.getFile();
        int jarStringIndex = urlFileName.indexOf("!/");
        if (jarStringIndex > 0) {
            urlFileName = urlFileName.substring(0, jarStringIndex);
        }
        if (urlFileName.startsWith(urlProtocol + ":/")) {
            urlFileName = urlFileName.substring(urlProtocol.length() + 2);
        } else if (urlFileName.startsWith("file:/")) {
            urlFileName = urlFileName.substring(6);
        }
        if (urlProtocol.equalsIgnoreCase("jar") || urlProtocol.equalsIgnoreCase("jar") || urlFileName.endsWith(".jar") || urlFileName.endsWith(".zip")) {
            String jarFileName = URLDecoder.decode(urlFileName, System.getProperty("file.encoding"));
            File file = new File(jarFileName);
            if (file.exists()) {
                JarFile jar = new JarFile(file);
                try {
                    return findClassNames(new JarFile(file), packagee, deep);
                } finally {
                    try {
                        jar.close();
                    } catch (IOException e) {
                        ;
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[mvc] -> jar '{}' not exists", jarFileName);
                }
            }
        } else {
            String fileName = URLDecoder.decode(urlFileName, System.getProperty("file.encoding"));
            File file = new File(fileName);
            if (file.exists()) {
                return findClassNames(file, packagee, deep);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[mvc] -> classes dir '{}' not exists", fileName);
                }
            }
        }
        return new HashSet<String>();
    }

    public static Set<String> findClassNames(JarFile jar, String packagee, boolean deep) {
        String path = packagee.replace('.', '/') + "/";
        Set<String> names = new HashSet<String>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(path)) {
                if (!deep && name.indexOf("/", path.length()) > 0) {
                    break;
                }
                int index = name.lastIndexOf(".class");
                if (index > 0) {
                    names.add(name.substring(0, index).replace('/', '.'));
                }
            }
        }
        return names;
    }

    public static Set<String> findClassNames(File dir, String packagee, boolean deep) {
        Set<String> names = new LinkedHashSet<String>();
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (name.charAt(0) == '.') {
                continue;
            }
            int index = name.lastIndexOf(".class");
            if (index > 0) {
                names.add(packagee + "." + name.substring(0, index));
                continue;
            }
            if (deep && file.isDirectory()) {
                names.addAll(findClassNames(file, packagee + "." + file.getName(), deep));
            }
        }
        return names;
    }
}
