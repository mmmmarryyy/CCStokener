package org.opensourcephysics.display;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;
import java.lang.reflect.Method;
import java.awt.Component;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.tools.Translator;
import java.net.URL;
import java.net.JarURLConnection;

/**
 * This defines static methods related to the runtime environment.
 * 
 * @author Douglas Brown
 * @author Wolfgang Chrstian
 * @version 1.0
 */
public class OSPRuntime {

    static String version = "1.3";

    static String releaseDate = "March 1, 2008";

    /** Disables drawing for faster start-up and to avoid screen flash in Drawing Panels. */
    public static volatile boolean disableAllDrawing = false;

    /** Shared Translator, if available. */
    public static Translator translator;

    /** Array of default OSP Locales. */
    public static Locale[] defaultLocales = new Locale[] { Locale.ENGLISH, new Locale("es"), new Locale("de"), new Locale("da"), new Locale("sk"), Locale.TAIWAN };

    /** Set <I>true</I> if a program is being run within Launcher. */
    protected static boolean launcherMode = false;

    /** True if running as an applet. */
    public static boolean appletMode;

    /** Static reference to an applet for document/code base access. */
    public static JApplet applet;

    /** True if launched by WebStart. */
    public static boolean webStart;

    /** True if users allowed to author internal parameters such as Locale strings. */
    protected static boolean authorMode = true;

    /** Look and feel property for the graphical user interface. */
    public static boolean javaLookAndFeel = false;

    /** Path of the launch jar, if any. */
    private static String launchJarPath;

    /** Path of the launch jar, if any. */
    private static String launchJarName;

    /** The launch jar, if any. */
    private static JarFile launchJar = null;

    /** File Chooser starting directory. */
    public static String chooserDir;

    /** Location of OSP icon. */
    public static final String OSP_ICON_FILE = "/org/opensourcephysics/resources/controls/images/osp_icon.gif";

    /** True if always launching in single vm (applet mode, etc). */
    public static boolean launchingInSingleVM;

    /**
    * Sets default properties for OSP.
    */
    static {
        JFrame.setDefaultLookAndFeelDecorated(OSPRuntime.javaLookAndFeel);
        JDialog.setDefaultLookAndFeelDecorated(OSPRuntime.javaLookAndFeel);
        try {
            OSPRuntime.chooserDir = System.getProperty("user.dir", null);
        } catch (Exception ex) {
            OSPRuntime.chooserDir = null;
        }
        try {
            Class translatorClass = Class.forName("org.opensourcephysics.tools.TranslatorTool");
            Method m = translatorClass.getMethod("getTool", (Class[]) null);
            translator = (Translator) m.invoke(null, (Object[]) null);
        } catch (Exception ex) {
        }
    }

    /**
    * Private constructor to prevent instantiation.
    */
    private OSPRuntime() {
    }

    /**
    * Shows the about dialog.
    */
    public static void showAboutDialog(Component parent) {
        String aboutString = "OSP Library " + version + " released " + releaseDate + "\n" + "Open Source Physics Project \n" + "www.opensourcephysics.org";
        JOptionPane.showMessageDialog(parent, aboutString, "About Open Source Physics", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
    * Determines if OS is Windows
    *
    * @return true if Windows
    */
    public static boolean isWindows() {
        try {
            return (System.getProperty("os.name", "").toLowerCase().startsWith("windows"));
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
    * Determines if OS is Mac
    *
    * @return true if Mac
    */
    public static boolean isMac() {
        try {
            return (System.getProperty("os.name", "").toLowerCase().startsWith("mac"));
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
    * Determines if OS is Linux
    *
    * @return true if Linux
    */
    public static boolean isLinux() {
        try {
            return (System.getProperty("os.name", "").toLowerCase().startsWith("linux"));
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
    * Determines if OS is Vista
    *
    * @return true if Vistsa
    */
    public static boolean isVista() {
        if (System.getProperty("os.name", "").toLowerCase().indexOf("vista") > -1) {
            return true;
        }
        return false;
    }

    /**
    * Determines if launched by WebStart
    *
    * @return true if launched by WebStart
    */
    public static boolean isWebStart() {
        if (!webStart) {
            try {
                webStart = Class.forName("javax.jnlp.BasicService") != null;
            } catch (Exception ex) {
            }
        }
        return webStart;
    }

    /**
    * Determines if running as an applet
    *
    * @return true if running as an applet
    */
    public static boolean isAppletMode() {
        return appletMode;
    }

    /**
    * Determines if running in author mode
    *
    * @return true if running in author mode
    */
    public static boolean isAuthorMode() {
        return authorMode;
    }

    /**
    * Sets the authorMode property.
    * AuthorMode allows users to author internal parameters such as Locale strings.
    *
    * @param b boolean
    */
    public static void setAuthorMode(boolean b) {
        authorMode = b;
    }

    /**
    * Sets the launcherMode property.
    * LauncherMode disables access to propertes, such as Locale, that affect the VM.
    *
    * @param b boolean
    */
    public static void setLauncherMode(boolean b) {
        launcherMode = b;
    }

    /**
    * Gets the launcherMode property.
    * LauncherMode disables access to propertes, such as Locale, that affect the VM.
    *
    * @return boolean
    */
    public static boolean isLauncherMode() {
        return launcherMode;
    }

    /**
    * Sets the launch jar path.
    * param path the path
    */
    public static void setLaunchJarPath(String path) {
        if (path == null || launchJarPath != null) return;
        if (!path.endsWith(".jar")) {
            int n = path.indexOf(".jar!");
            if (n > -1) {
                path = path.substring(0, n + 4);
            } else return;
        }
        launchJarPath = path;
        launchJarName = path.substring(path.lastIndexOf("/") + 1);
    }

    /**
    * Gets the launch jar nsme, if any.
    * @return launch jar path, or null if not launched from a jar
    */
    public static String getLaunchJarName() {
        return launchJarName;
    }

    /**
    * Gets the launch jar path, if any.
    * @return launch jar path, or null if not launched from a jar
    */
    public static String getLaunchJarPath() {
        return launchJarPath;
    }

    /**
    * Gets the launch jar directory, if any.
    * @return path to the directory containing the launch jar. May be null.
    */
    public static String getLaunchJarDirectory() {
        if (applet != null) return null;
        return launchJarPath == null ? null : XML.getDirectoryPath(launchJarPath);
    }

    /**
    * Gets the jar from which the progam was launched.
    * @return JarFile
    */
    public static JarFile getLaunchJar() {
        if (launchJar != null) return launchJar;
        if (launchJarPath == null) return null;
        boolean isWebFile = launchJarPath.startsWith("http:");
        try {
            if ((OSPRuntime.applet == null) && !isWebFile) {
                launchJar = new JarFile(launchJarPath);
            } else {
                URL url;
                if (isWebFile) {
                    url = new URL("jar:" + launchJarPath + "!/");
                } else {
                    url = new URL("jar:file:/" + launchJarPath + "!/");
                }
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                launchJar = conn.getJarFile();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return launchJar;
    }

    /**
    * Gets Locales for languages that have properties files in the core library.
    * @return Locale[]
    */
    public static Locale[] getDefaultLocales() {
        return defaultLocales;
    }

    /**
    * Gets Locales for languages that have properties files in the core library.
    * @return Locale[]
    */
    public static Locale[] getInstalledLocales() {
        ArrayList list = new ArrayList();
        list.add(Locale.ENGLISH);
        if (getLaunchJarPath() != null) {
            JarFile jar = getLaunchJar();
            if (jar != null) {
                for (Enumeration e = jar.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = (JarEntry) e.nextElement();
                    String path = entry.toString();
                    int n = path.indexOf(".properties");
                    if (path.indexOf(".properties") > -1) {
                        int m = path.indexOf("display_res_");
                        if (m > -1) {
                            String loc = path.substring(m + 12, n);
                            if (loc.equals("zh_TW")) {
                                list.add(Locale.TAIWAN);
                            } else {
                                Locale next = new Locale(loc);
                                if (!next.equals(Locale.ENGLISH)) {
                                    list.add(next);
                                }
                            }
                        }
                    }
                }
            } else {
                defaultLocales = new Locale[] { Locale.ENGLISH };
                return defaultLocales;
            }
        }
        return (Locale[]) list.toArray(new Locale[0]);
    }

    /**
    * Gets the translator, if any.
    * @return translator, or null if none available
    */
    public static Translator getTranslator() {
        return translator;
    }

    private static JFileChooser chooser;

    /**
    * Gets a file chooser.
    *
    * The choose is static and will therefore be the same for all OSPFrames.
    *
    * @return the chooser
    */
    public static JFileChooser getChooser() {
        if (chooser != null) {
            return chooser;
        }
        try {
            chooser = (OSPRuntime.chooserDir == null) ? new JFileChooser() : new JFileChooser(new File(OSPRuntime.chooserDir));
        } catch (Exception e) {
            System.err.println("Exception in OSPFrame getChooser=" + e);
            return null;
        }
        javax.swing.filechooser.FileFilter defaultFilter = chooser.getFileFilter();
        javax.swing.filechooser.FileFilter xmlFilter = new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                if (f == null) {
                    return false;
                }
                if (f.isDirectory()) {
                    return true;
                }
                String extension = null;
                String name = f.getName();
                int i = name.lastIndexOf('.');
                if ((i > 0) && (i < name.length() - 1)) {
                    extension = name.substring(i + 1).toLowerCase();
                }
                if ((extension != null) && (extension.equals("xml"))) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return DisplayRes.getString("OSPRuntime.FileFilter.Description.XML");
            }
        };
        javax.swing.filechooser.FileFilter txtFilter = new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                if (f == null) {
                    return false;
                }
                if (f.isDirectory()) {
                    return true;
                }
                String extension = null;
                String name = f.getName();
                int i = name.lastIndexOf('.');
                if ((i > 0) && (i < name.length() - 1)) {
                    extension = name.substring(i + 1).toLowerCase();
                }
                if ((extension != null) && extension.equals("txt")) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return DisplayRes.getString("OSPRuntime.FileFilter.Description.TXT");
            }
        };
        chooser.addChoosableFileFilter(xmlFilter);
        chooser.addChoosableFileFilter(txtFilter);
        chooser.setFileFilter(defaultFilter);
        return chooser;
    }

    /**
    * Uses a JFileChooser to ask for a name.
    * @param chooser JFileChooser
    * @return String The absolute pah of the filename. Null if cancelled
    */
    public static String chooseFilename(JFileChooser chooser) {
        return chooseFilename(chooser, null, true);
    }

    /**
    * Uses a JFileChooser to ask for a name.
    * @param chooser JFileChooser
    * @param parent Parent component for messages
    * @param toSave true if we will save to the chosen file, false if we will read from it
    * @return String The absolute pah of the filename. Null if cancelled
    */
    public static String chooseFilename(JFileChooser chooser, Component parent, boolean toSave) {
        String fileName = null;
        int result;
        if (toSave) {
            result = chooser.showSaveDialog(parent);
        } else {
            result = chooser.showOpenDialog(parent);
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
            File file = chooser.getSelectedFile();
            if (toSave) {
                if (file.exists()) {
                    int selected = JOptionPane.showConfirmDialog(parent, DisplayRes.getString("DrawingFrame.ReplaceExisting_message") + " " + file.getName() + DisplayRes.getString("DrawingFrame.QuestionMark"), DisplayRes.getString("DrawingFrame.ReplaceFile_option_title"), JOptionPane.YES_NO_CANCEL_OPTION);
                    if (selected != JOptionPane.YES_OPTION) {
                        return null;
                    }
                }
            } else {
                if (!file.exists()) {
                    JOptionPane.showMessageDialog(parent, DisplayRes.getString("GUIUtils.FileDoesntExist") + " " + file.getName(), DisplayRes.getString("GUIUtils.FileChooserError"), JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }
            fileName = file.getAbsolutePath();
            if ((fileName == null) || fileName.trim().equals("")) {
                return null;
            }
        }
        return fileName;
    }

    /**
    * Creates a JFileChooser with given desription and extensions
    * @param description String A description string
    * @param extensions String[] An array of allowed extensions
    * @return JFileChooser
    */
    public static javax.swing.JFileChooser createChooser(String description, String[] extensions) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(new File(OSPRuntime.chooserDir));
        ExtensionFileFilter filter = new ExtensionFileFilter();
        for (int i = 0; i < extensions.length; i++) {
            filter.addExtension(extensions[i]);
        }
        filter.setDescription(description);
        chooser.setFileFilter(filter);
        return chooser;
    }

    /**
    * This file filter matches all files with a given set of
    * extensions.
    */
    private static class ExtensionFileFilter extends javax.swing.filechooser.FileFilter {

        private String description = "";

        private java.util.ArrayList extensions = new java.util.ArrayList();

        /**
       *  Adds an extension that this file filter recognizes.
       *  @param extension a file extension (such as ".txt" or "txt")
       */
        public void addExtension(String extension) {
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            extensions.add(extension.toLowerCase());
        }

        /**
       *  Sets a description for the file set that this file filter
       *  recognizes.
       *  @param aDescription a description for the file set
       */
        public void setDescription(String aDescription) {
            description = aDescription;
        }

        /**
       *  Returns a description for the file set that this file
       *  filter recognizes.
       *  @return a description for the file set
       */
        public String getDescription() {
            return description;
        }

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String name = f.getName().toLowerCase();
            for (int i = 0; i < extensions.size(); i++) {
                if (name.endsWith((String) extensions.get(i))) {
                    return true;
                }
            }
            return false;
        }
    }
}
