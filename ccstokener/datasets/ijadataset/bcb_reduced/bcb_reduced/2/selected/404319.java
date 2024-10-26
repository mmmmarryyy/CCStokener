package org.agile.dfs.core.common;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassFinder {

    private static final Logger log = LoggerFactory.getLogger(ClassFinder.class);

    public static void main(String[] args) {
        List childClz = ClassFinder.getInstanceClassesByParentClass(java.lang.Integer.class);
        for (int i = 0; i < childClz.size(); i++) {
            System.out.println(childClz.get(i));
        }
    }

    public static List getInstanceClassesByParentClass(Class parentClass) {
        Set ps = getClasses(parentClass.getPackage());
        Iterator it = ps == null ? null : ps.iterator();
        List list = new ArrayList();
        while (it != null && it.hasNext()) {
            Class childClass = (Class) it.next();
            if (isChildOfParent(parentClass, childClass)) {
                if (Modifier.isPublic(childClass.getModifiers()) && !Modifier.isAbstract(childClass.getModifiers())) {
                    list.add(childClass);
                }
            }
        }
        return list;
    }

    public static List getInstanceClassesByInterface(Class interfaceClass) {
        Set ps = getClasses(interfaceClass.getPackage());
        Iterator it = ps == null ? null : ps.iterator();
        List list = new ArrayList();
        while (it != null && it.hasNext()) {
            Class childClass = (Class) it.next();
            if (isChildOfInterface(interfaceClass, childClass)) {
                if (Modifier.isPublic(childClass.getModifiers()) && !Modifier.isAbstract(childClass.getModifiers())) {
                    list.add(childClass);
                }
            }
        }
        return list;
    }

    private static boolean isChildOfParent(Class parentClass, Class childClass) {
        Class currentClass = childClass.getSuperclass();
        while (currentClass != null && !currentClass.getName().equals("java.lang.Object")) {
            if (parentClass.getName().equals(currentClass.getName())) {
                return true;
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
    }

    private static boolean isChildOfInterface(Class interfaceClass, Class childClass) {
        Class[] interfaces = childClass.getInterfaces();
        for (int i = 0; interfaces != null && i < interfaces.length; i++) {
            Class currentClass = interfaces[i];
            if (currentClass.getName().equals(interfaceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    private static Set getClasses(Package pack) {
        Set classes = new LinkedHashSet();
        boolean recursive = true;
        String packageName = pack.getName();
        String packageDirName = packageName.replace('.', '/');
        Enumeration dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = (URL) dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = (JarEntry) entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                if (idx != -1) {
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                if ((idx != -1) || recursive) {
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            log.error("Add custom class to set error!", e);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("Scan jar file happen io exception!", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Find class happen io exception!", e);
        }
        return classes;
    }

    private static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        for (int i = 0; i < dirfiles.length; i++) {
            File file = dirfiles[i];
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    log.error("Add custom class to set error!", e);
                }
            }
        }
    }
}
