package jpatch.auxilary;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import jpatch.boundary.*;

/**
 * @author sascha
 *
 */
public class JoglInstall {

    public static final int OTHER = 0;

    public static final int WINDOWS_X86 = 1;

    public static final int LINUX_X86 = 2;

    public static final int OSX_PPC = 3;

    public static final int SOLARIS_SPARC = 4;

    public static final int SOLARIS_X86 = 5;

    public static final String[] OS_ARCH = new String[] { "OTHER", "Windows/x86", "Linux/x86", "OS X/Power PC", "Solaris/Sparc", "Solaris/x86" };

    private static Properties properties = System.getProperties();

    public static boolean isInstalled() {
        String javaLibraryPath = properties.getProperty("java.library.path");
        String[] folders = javaLibraryPath.split(properties.getProperty("path.separator"));
        String lib = System.mapLibraryName("jogl");
        for (int i = 0; i < folders.length; i++) {
            File file = new File(folders[i], lib);
            if (file.exists()) return true;
        }
        return false;
    }

    public static boolean isOsSupported() {
        int arch = getArch();
        return (arch == LINUX_X86 || arch == WINDOWS_X86);
    }

    public static int getArch() {
        String name = properties.getProperty("os.name");
        String arch = properties.getProperty("os.arch");
        if (name.startsWith("Windows")) {
            if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586") || arch.equals("i686")) {
                return WINDOWS_X86;
            }
        } else if (name.equals("Linux")) {
            if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586") || arch.equals("i686")) {
                return LINUX_X86;
            }
        } else if (name.equals("Mac OS X")) {
            if (arch.equals("ppc") || arch.equals("PowerPC")) {
                return OSX_PPC;
            }
        } else if (name.equals("Solaris")) {
            if (arch.equals("sparc")) {
                return SOLARIS_SPARC;
            } else if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586") || arch.equals("i686")) {
                return SOLARIS_X86;
            }
        }
        return OTHER;
    }

    public static void extractJoglLibraries(int platform, String dir) throws IOException {
        switch(platform) {
            case WINDOWS_X86:
                {
                    copyFromURL(ClassLoader.getSystemResource("native/windows/jogl.dll"), dir);
                }
                break;
            case LINUX_X86:
                {
                    copyFromURL(ClassLoader.getSystemResource("native/linux_x86/libjogl.so"), dir);
                }
                break;
        }
    }

    public static String chooseLocation(String[] locations) {
        JComboBox comboBox = new JComboBox(locations);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("Choose a folder to copy the jogl shared libraries into:"), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(MainFrame.getInstance(), panel, "JOGL shared libraries installation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) return (String) comboBox.getSelectedItem();
        return null;
    }

    public static void install() {
        if (!isOsSupported()) {
            JOptionPane.showMessageDialog(MainFrame.getInstance(), new JLabel("Only Windows and Linux versions of JOGL are included in this distribution"), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String javaLibraryPath = properties.getProperty("java.library.path");
        String[] folders = javaLibraryPath.split(properties.getProperty("path.separator"));
        for (; ; ) {
            String destination = chooseLocation(folders);
            if (destination == null) return;
            try {
                extractJoglLibraries(getArch(), destination);
                JOptionPane.showMessageDialog(MainFrame.getInstance(), new JLabel("JOGL shared libraries installed."), "OK", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(MainFrame.getInstance(), new JLabel("Unable to copy JOGL shared libraries into specified folder."), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static File copyFromURL(URL url, String dir) throws IOException {
        File urlFile = new File(url.getFile());
        File dest = new File(dir, urlFile.getName());
        FileOutputStream os = new FileOutputStream(dest);
        InputStream is = url.openStream();
        byte data[] = new byte[4096];
        int ct;
        while ((ct = is.read(data)) >= 0) os.write(data, 0, ct);
        is.close();
        os.close();
        return dest;
    }
}
