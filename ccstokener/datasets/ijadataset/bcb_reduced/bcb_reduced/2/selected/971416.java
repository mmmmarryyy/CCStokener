package gov.sns.tools.findclass;

import java.io.*;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.jar.*;
import java.util.zip.*;
import java.util.Enumeration;

/**
 * This utility class is looking for all the classes implementing or 
 * inheriting from a given interface or class.
 * (RunTime Subclass Identification)
 *
 * @author <a href="mailto:daniel@satlive.org">Daniel Le Berre</a>
 * @version 1.0
 */
public class RTSI {

    /**
     * Display all the classes inheriting or implementing a given
     * class in the currently loaded packages.
     * @param tosubclassname the name of the class to inherit from
     */
    public static void find(String tosubclassname) {
        try {
            Class<?> tosubclass = Class.forName(tosubclassname);
            Package[] pcks = Package.getPackages();
            for (int i = 0; i < pcks.length; i++) {
                find(pcks[i].getName(), tosubclass);
            }
        } catch (ClassNotFoundException ex) {
            System.err.println("Class " + tosubclassname + " not found!");
        }
    }

    /**
     * Display all the classes inheriting or implementing a given
     * class in a given package.
     * @param pckgname the fully qualified name of the package
     * @param tosubclass the name of the class to inherit from
     */
    public static void find(String pckname, String tosubclassname) {
        try {
            Class<?> tosubclass = Class.forName(tosubclassname);
            find(pckname, tosubclass);
        } catch (ClassNotFoundException ex) {
            System.err.println("Class " + tosubclassname + " not found!");
        }
    }

    /**
     * Display all the classes inheriting or implementing a given
     * class in a given package.
     * @param pckgname the fully qualified name of the package
     * @param tosubclass the Class object to inherit from
     */
    public static void find(String pckgname, Class<?> tosubclass) {
        String name = new String(pckgname);
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        name = name.replace('.', '/');
        URL url = tosubclass.getResource(name);
        System.out.println(name + "->" + url);
        if (url == null) return;
        File directory = new File(url.getFile());
        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    String classname = files[i].substring(0, files[i].length() - 6);
                    try {
                        Object o = Class.forName(pckgname + "." + classname).newInstance();
                        if (tosubclass.isInstance(o)) {
                            System.out.println(classname);
                        }
                    } catch (ClassNotFoundException cnfex) {
                        System.err.println(cnfex);
                    } catch (InstantiationException iex) {
                    } catch (IllegalAccessException iaex) {
                    }
                }
            }
        } else {
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                String starts = conn.getEntryName();
                JarFile jfile = conn.getJarFile();
                Enumeration<JarEntry> e = jfile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String entryname = entry.getName();
                    if (entryname.startsWith(starts) && (entryname.lastIndexOf('/') <= starts.length()) && entryname.endsWith(".class")) {
                        String classname = entryname.substring(0, entryname.length() - 6);
                        if (classname.startsWith("/")) classname = classname.substring(1);
                        classname = classname.replace('/', '.');
                        try {
                            Object o = Class.forName(classname).newInstance();
                            if (tosubclass.isInstance(o)) {
                                System.out.println(classname.substring(classname.lastIndexOf('.') + 1));
                            }
                        } catch (ClassNotFoundException cnfex) {
                            System.err.println(cnfex);
                        } catch (InstantiationException iex) {
                        } catch (IllegalAccessException iaex) {
                        }
                    }
                }
            } catch (IOException ioex) {
                System.err.println(ioex);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            find(args[0], args[1]);
        } else {
            if (args.length == 1) {
                find(args[0]);
            } else {
                System.out.println("Usage: java RTSI [<package>] <subclass>");
            }
        }
    }
}
