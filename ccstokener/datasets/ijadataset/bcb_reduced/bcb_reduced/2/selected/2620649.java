package mp3.extras;

import java.awt.Component;
import java.awt.Container;
import mp3.extras.wrappers.ThemeWrapper;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import mp3.pluginsSupport.Plugin;
import mp3.services.PluginFolderManager;
import mp3.services.ServiceSetter;

/**
 *
 * @author JESUS VILLAFAÑEZ
 */
public class Utilidades {

    /**
 * devuelve los nombres de las clases contenidas en el fichero jar
 * @param jarName nombre del jar en el que se va a buscar
 * @param packageName nombre del paquete que se va a buscar en el jar, poner "" para
 * listar todos los paquetes
 * @return lista de paquetes encontrados
 */
    public static List<String> getClasseNamesInPackage(String jarName, String packageName) {
        ArrayList<String> classes = new ArrayList<String>();
        packageName = packageName.replaceAll("\\.", "/");
        try {
            JarInputStream jarFile = new JarInputStream(new FileInputStream(jarName));
            JarEntry jarEntry;
            while (true) {
                jarEntry = jarFile.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                if ((jarEntry.getName().startsWith(packageName)) && (jarEntry.getName().endsWith(".class"))) {
                    classes.add(jarEntry.getName().replaceAll("/", "\\."));
                }
            }
        } catch (Exception e) {
            Logger.getLogger(Utilidades.class.getName()).log(Level.SEVERE, null, e);
        }
        return classes;
    }

    /**
 * devuelve la localizacion donde se está ejecutando el programa
 * @return ruta absoluta de la localización
 */
    public static String getExecutionLocalization() {
        File fich;
        try {
            fich = new File(Utilidades.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return fich.getAbsolutePath();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
 * Util para ejecucion en jar para saber la carpeta contenedora
 * @return ruta absoluta de la carpeta
 */
    public static String getExecutionFolder() {
        File fich;
        fich = new File(Utilidades.getExecutionLocalization());
        return fich.getParent();
    }

    /**
 * get the plugins folder location
 * @return the pugins folder location
 */
    public static String getPluginsFolder() {
        return getExecutionFolder() + File.separator + "plugins";
    }

    /**
 * añade la carpeta "plugins" y sus jar al classpath del hilo actual
 * El context class loader se actualiza con la localizacion del plugins folder
 * El class loader generalmente no es necesario
 * @return class loader con el plugins folder añadadido al classpath
 */
    public static URLClassLoader addPluginsFolderToClasspath() {
        URLClassLoader ucl;
        ClassLoader cl;
        ArrayList<URL> arr;
        File fold;
        File[] fichs;
        URL[] u;
        int i;
        arr = new ArrayList<URL>();
        fold = new File(Utilidades.getExecutionFolder().concat(File.separator + "plugins" + File.separator));
        if (!fold.exists()) {
            System.err.println(java.util.ResourceBundle.getBundle("Bundle").getString("Plugins.Error"));
        } else {
            try {
                arr.add(fold.toURI().toURL());
            } catch (MalformedURLException ex) {
            }
            fichs = fold.listFiles();
            for (i = 0; i < fichs.length; i++) {
                if (fichs[i].getAbsolutePath().endsWith(".jar")) try {
                    arr.add(fichs[i].toURI().toURL());
                } catch (MalformedURLException ex) {
                }
            }
        }
        u = arr.toArray(new URL[arr.size()]);
        cl = Thread.currentThread().getContextClassLoader();
        ucl = new URLClassLoader(u, cl);
        Thread.currentThread().setContextClassLoader(ucl);
        return ucl;
    }

    /**
 * Use with caution. This method is recursive.
 * @param dir
 * @return
 */
    private static List<URL> getAllDirRecur(File dir, List<URL> foundDirs) {
        for (File f : dir.listFiles()) {
            if (f != null && f.isDirectory()) {
                try {
                    foundDirs.add(f.toURI().toURL());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Utilidades.class.getName()).log(Level.SEVERE, null, ex);
                }
                getAllDirRecur(f, foundDirs);
            }
        }
        return foundDirs;
    }

    /**
 * Some components retrieve this theme wrappers. Adding here a theme wrapper will
 * update the component in the future
 * @return
 */
    public static List<ThemeWrapper> getLocalLookAndFeel() {
        ArrayList<ThemeWrapper> lista = new ArrayList<ThemeWrapper>();
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (!info.getClassName().equalsIgnoreCase(UIManager.getCrossPlatformLookAndFeelClassName()) && !info.getClassName().equalsIgnoreCase(UIManager.getSystemLookAndFeelClassName())) {
                lista.add(new ThemeWrapper(info.getName(), info.getClassName()));
            }
        }
        lista.add(new ThemeWrapper("System Specific", "System Specific"));
        lista.add(new ThemeWrapper("Cross Platform", "Cross Platform"));
        return lista;
    }

    /**
 * Get a name list of classes which might be a Look&Feel (UIManager.setLookAndFeel)
 * It searches inside "plugins" folder, and inside jar files of that folder
 * @return list with class names which might be a Look&Feel
 */
    public static List<ThemeWrapper> getAllLookAndFeel() {
        List<ThemeWrapper> lista = new ArrayList<ThemeWrapper>();
        PluginFolderManager manager = (PluginFolderManager) ServiceSetter.getServiceSetter().getServiceByName(PluginFolderManager.class.getName());
        manager.fasterScan();
        List<String> themes = manager.getLafClassNames();
        for (String theme : themes) {
            lista.add(new ThemeWrapper(theme, theme));
        }
        return lista;
    }

    /**
 * Version actual del programa
 */
    public static final String actualVersion = "1.9";

    public static String getId(boolean forceCreation) {
        IdGen id = new IdGen();
        try {
            if (id.getId() == null) {
                if (!forceCreation) return null;
                id.genId();
                UUID myid = id.getId();
                return myid.toString();
            } else {
                return id.getId().toString();
            }
        } catch (IOException e) {
            Logger.getLogger(Utilidades.class.getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    /**
 * consigue la información de la ultima versión disponible notificada en el sitio
 * @return la versión notificada
 */
    public static String getLatestVersion() {
        URL url;
        BufferedReader bs;
        String s;
        try {
            url = new URL("http://jmusicmanager.sourceforge.net/version.txt");
        } catch (MalformedURLException ex) {
            return null;
        }
        try {
            bs = new BufferedReader(new InputStreamReader(url.openStream()));
            s = bs.readLine();
            bs.close();
            return s;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Get the code of the style which will be used for create the Font instance
     * @param name style name
     * @return the associated code or -1 if it doesnt exists
     */
    public static int getFontStyleCode(String name) {
        if (name.equalsIgnoreCase("Plain")) return Font.PLAIN;
        if (name.equalsIgnoreCase("Italic")) return Font.ITALIC;
        if (name.equalsIgnoreCase("Bold")) return Font.BOLD;
        if (name.equalsIgnoreCase("Italic Bold")) return Font.ITALIC | Font.BOLD;
        if (name.equalsIgnoreCase("Bold Italic")) return Font.BOLD | Font.ITALIC;
        return -1;
    }

    /**
 * This is not my code!
 * Found on http://snippets.dzone.com/posts/show/4831
 * Added support for jar files (resource protocol should be "jar")
 * and it has been modified slightly to allow custom class loaders
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @param loader the loader who will load the packages or null to use the default
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static List<Class<?>> getClasses(String packageName, ClassLoader loader) throws ClassNotFoundException, IOException, URISyntaxException {
        ClassLoader classLoader;
        if (loader == null) classLoader = Thread.currentThread().getContextClassLoader(); else classLoader = loader;
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("jar")) {
                String jarFile = resource.getPath().replaceAll("file:", "").replaceAll("!.*$", "").replaceAll("%20", " ");
                List<String> ls = Utilidades.getClasseNamesInPackage(jarFile, packageName);
                for (String s : ls) {
                    if (!s.contains("$")) {
                        if (s.endsWith(".class")) {
                            s = s.substring(0, s.length() - 6);
                        }
                        classes.add(classLoader.loadClass(s));
                    }
                }
            } else dirs.add(new File(resource.toURI()));
        }
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName, loader));
        }
        return classes;
    }

    /**
     * This is not my code!
     * Found on http://snippets.dzone.com/posts/show/4831
     *
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private static List<Class<?>> findClasses(File directory, String packageName, ClassLoader loader) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                assert !fileName.contains(".");
                classes.addAll(findClasses(file, packageName + "." + fileName, loader));
            } else if (fileName.endsWith(".class") && !fileName.contains("$")) {
                Class _class;
                try {
                    if (loader == null) _class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6)); else _class = loader.loadClass(packageName + '.' + fileName.substring(0, fileName.length() - 6));
                } catch (ExceptionInInitializerError e) {
                    _class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6), false, Thread.currentThread().getContextClassLoader());
                }
                classes.add(_class);
            }
        }
        return classes;
    }

    public static void changeTreeFont(Container comp, Font font) {
        for (Component c : comp.getComponents()) {
            c.setFont(font);
            if (c instanceof Container) {
                changeTreeFont((Container) c, font);
            }
            if (c instanceof JMenu) {
                changeTreeFont(((JMenu) c).getPopupMenu(), font);
            }
        }
    }

    /**
     * Test whether the plugin is said to be supported by this version program
     * @param plug plugin to be tested
     * @return true if it is supported
     */
    public static boolean isPluginSupported(Plugin plug) {
        double[] versions = plug.getJmmmTestedVersions();
        for (int i = 0; i < versions.length; i++) {
            if (versions[i] == Double.parseDouble(actualVersion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple utility to parse command-line arguments
     * @param cl command-line to execute a program
     * @return list of all arguments (including the program name)
     */
    public static List<String> parseClString(String cl) {
        List<String> result = new ArrayList<String>();
        String[] splitString = cl.split(" +");
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < splitString.length; i++) {
            sb.delete(0, sb.length());
            sb.append(splitString[i]);
            char first = splitString[i].charAt(0);
            if ((first == '\'') || (first == '\"')) {
                if (i + 1 < splitString.length) {
                    for (int j = i + 1; j < splitString.length; j++) {
                        char last = splitString[j].charAt(splitString[j].length() - 1);
                        if (last == first) {
                            sb.append(' ').append(splitString[j]);
                            i = j;
                            break;
                        } else {
                            sb.append(' ').append(splitString[j]);
                        }
                    }
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(0);
            }
            result.add(sb.toString());
        }
        return result;
    }
}
