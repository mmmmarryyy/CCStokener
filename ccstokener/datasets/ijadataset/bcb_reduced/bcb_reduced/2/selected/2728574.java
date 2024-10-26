package net.itsite.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class ReflectUtils {

    public static List<Class<?>> listSubCalss(final Class<?> cType, final String extenion) {
        final List<Class<?>> result = new ArrayList<Class<?>>();
        final List<String> cPath = ReflectUtils.listClassCanonicalNameOfPackage(cType, extenion);
        for (final String path : cPath) {
            try {
                Class<?> cla = Class.forName(path, false, Thread.currentThread().getContextClassLoader());
                if (Modifier.isAbstract(cla.getModifiers())) {
                    continue;
                }
                result.add(cla);
            } catch (final Exception e) {
            }
        }
        return result;
    }

    public static List<Class<?>> listClassOfPackage(final Class<?> cType, final String extenion) {
        final List<Class<?>> result = new ArrayList<Class<?>>();
        final List<String> cPath = ReflectUtils.listClassCanonicalNameOfPackage(cType, extenion);
        for (final String path : cPath) {
            try {
                result.add(Class.forName(path, false, Thread.currentThread().getContextClassLoader()));
            } catch (final Exception e) {
            }
        }
        return result;
    }

    public static List<String> listClassCanonicalNameOfPackage(final Class<?> clazz, final String extenion) {
        return ReflectUtils.listNameOfPackage(clazz, extenion, true);
    }

    public static List<String> listNameOfPackage(final Class<?> clazz, final String extenion, final boolean fullPkgName) {
        return ReflectUtils.listNameOfPackage(clazz.getName().replace('.', '/') + ".class", extenion, fullPkgName);
    }

    public static List<String> listNameOfPackage(final String clazzPkg, final String extenion, final boolean fullPkgName) {
        final List<String> result = new ArrayList<String>();
        final StringBuffer pkgBuf = new StringBuffer(clazzPkg);
        if (pkgBuf.charAt(0) != '/') pkgBuf.insert(0, '/');
        final URL urlPath = ReflectUtils.class.getResource(pkgBuf.toString());
        if (null == urlPath) return result;
        String checkedExtenion = extenion;
        if (!extenion.endsWith(".class")) checkedExtenion = extenion + ".class";
        if (pkgBuf.toString().endsWith(".class")) pkgBuf.delete(pkgBuf.lastIndexOf("/"), pkgBuf.length());
        pkgBuf.deleteCharAt(0);
        final StringBuffer fileUrl = new StringBuffer();
        try {
            fileUrl.append(URLDecoder.decode(urlPath.toExternalForm(), "UTF-8"));
        } catch (final UnsupportedEncodingException e1) {
            fileUrl.append(urlPath.toExternalForm());
        }
        if (fileUrl.toString().startsWith("file:")) {
            fileUrl.delete(0, 5);
            if (fileUrl.indexOf(":") != -1) fileUrl.deleteCharAt(0);
            final String baseDir = fileUrl.substring(0, fileUrl.lastIndexOf("classes") + 8);
            ReflectUtils.doListNameOfPackageInDirectory(new File(baseDir), new File(baseDir), result, pkgBuf.toString(), checkedExtenion, fullPkgName);
        } else {
            ReflectUtils.doListNameOfPackageInJar(urlPath, urlPath, result, pkgBuf.toString(), checkedExtenion, fullPkgName);
        }
        return result;
    }

    /**
	* @param baseUrl
	* @param urlPath
	* @param result
	* @param clazz
	* @param extenion
	* @param fullPkgName
	*/
    private static void doListNameOfPackageInJar(final URL baseUrl, final URL urlPath, final List<String> result, final String clazzPkg, final String extenion, final boolean fullPkgName) {
        try {
            final JarURLConnection conn = (JarURLConnection) urlPath.openConnection();
            final JarFile jfile = conn.getJarFile();
            final Enumeration<JarEntry> e = jfile.entries();
            ZipEntry entry;
            String entryname;
            while (e.hasMoreElements()) {
                entry = e.nextElement();
                entryname = entry.getName();
                if (entryname.startsWith(clazzPkg) && entryname.endsWith(extenion)) {
                    if (fullPkgName) result.add(entryname.substring(0, entryname.lastIndexOf('.')).replace('/', '.')); else result.add(entryname.substring(entryname.lastIndexOf('/') + 1, entryname.lastIndexOf('.')));
                }
            }
        } catch (final IOException ioex) {
        }
    }

    /**
	 * @param directory
	 * @param fullPkgName
	 * @param extenion
	 * @param clazz
	 * @param result
	 */
    private static void doListNameOfPackageInDirectory(final File baseDirectory, final File directory, final List<String> result, final String clazzPkg, final String extenion, final boolean fullPkgName) {
        File[] files = directory.listFiles();
        if (null == files) files = new File[] {};
        String clazzPath;
        final int baseDirLen = baseDirectory.getAbsolutePath().length() + 1;
        for (final File file : files) {
            if (file.isDirectory()) {
                ReflectUtils.doListNameOfPackageInDirectory(baseDirectory, file, result, clazzPkg, extenion, fullPkgName);
            } else {
                if (!file.getName().endsWith(extenion)) continue;
                if (fullPkgName) {
                    clazzPath = file.getAbsolutePath().substring(baseDirLen);
                    clazzPath = clazzPath.substring(0, clazzPath.length() - 6);
                    result.add(clazzPath.replace(File.separatorChar, '.'));
                } else {
                    result.add(file.getName().substring(0, file.getName().length() - 6));
                }
            }
        }
    }
}
