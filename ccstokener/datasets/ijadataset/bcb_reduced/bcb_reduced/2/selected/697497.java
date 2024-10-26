package com.theoryinpractise.dbng;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Derricutt
 * Date: 24/03/2007
 * Time: 18:41:42
 * To change this template use File | Settings | File Templates.
 */
public class ClassWalker {

    public static Set<Class> findMigrationClassesInPackage(String packageName, ClassWalkerAcceptor classWalkerAcceptor) {
        Set<Class> acceptedClasses = new HashSet<Class>();
        try {
            String packageOnly = packageName;
            boolean recursive = false;
            if (packageName.endsWith(".*")) {
                packageOnly = packageName.substring(0, packageName.lastIndexOf(".*"));
                recursive = true;
            }
            String packageDirName = packageOnly.replace('.', '/');
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                if ("file".equals(url.getProtocol())) {
                    findClassesInDirPackage(packageOnly, URLDecoder.decode(url.getFile(), "UTF-8"), recursive, acceptedClasses, classWalkerAcceptor);
                } else if ("jar".equals(url.getProtocol())) {
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!name.endsWith("/")) {
                            String className = name.replaceAll("/", ".").replaceAll("\\.class", "");
                            checkValidClass(className, acceptedClasses, classWalkerAcceptor);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return acceptedClasses;
    }

    private static void checkValidClass(String className, Set<Class> acceptedClasses, ClassWalkerAcceptor classWalkerAcceptor) {
        try {
            Class classClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (classWalkerAcceptor.accept(classClass)) {
                acceptedClasses.add(classClass);
            }
        } catch (ClassNotFoundException e) {
        } catch (NoClassDefFoundError e) {
        }
    }

    private static void findClassesInDirPackage(String packageName, String packagePath, final boolean recursive, Set<Class> classes, ClassWalkerAcceptor classWalkerAcceptor) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findClassesInDirPackage(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes, classWalkerAcceptor);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                checkValidClass(packageName + "." + className, classes, classWalkerAcceptor);
            }
        }
    }
}
