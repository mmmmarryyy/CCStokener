package frost;

import java.io.*;
import java.net.*;
import java.util.logging.*;
import frost.util.*;

/**
 * Does some things that have to be done when starting Frost.
 */
public class Startup {

    private static final Logger logger = Logger.getLogger(Startup.class.getName());

    /**
     * The Main method, check if allowed to run
     * and starts the other startup work.
     */
    public static void startupCheck(final SettingsClass settings) {
        checkDirectories(settings);
        copyFiles();
        cleanTempDir(settings);
        final File oldJarFile = new File("lib/mckoidb.jar");
        if (oldJarFile.isFile()) {
            oldJarFile.delete();
        }
    }

    private static void copyFiles() {
        final String fileSeparator = System.getProperty("file.separator");
        try {
            boolean copyResource = false;
            final File systrayDllFile = new File("exec" + fileSeparator + "JSysTray.dll");
            if (!systrayDllFile.isFile()) {
                copyResource = true;
            } else {
                final URL url = MainFrame.class.getResource("/data/JSysTray.dll");
                final URLConnection urlConn = url.openConnection();
                final long len = urlConn.getContentLength();
                if (len != systrayDllFile.length()) {
                    systrayDllFile.delete();
                    copyResource = true;
                }
            }
            if (copyResource) {
                FileAccess.copyFromResource("/data/JSysTray.dll", systrayDllFile);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    private static void checkDirectories(final SettingsClass settings) {
        final File downloadDirectory = new File(settings.getValue(SettingsClass.DIR_DOWNLOAD));
        if (!downloadDirectory.isDirectory()) {
            logger.warning("Creating download directory");
            downloadDirectory.mkdirs();
        }
        final File execDirectory = new File("exec");
        if (!execDirectory.isDirectory()) {
            logger.warning("Creating exec directory");
            execDirectory.mkdirs();
        }
        final File tempDirectory = new File(settings.getValue(SettingsClass.DIR_TEMP));
        if (!tempDirectory.isDirectory()) {
            logger.warning("Creating temp directory");
            tempDirectory.mkdirs();
        }
        final File storeDirectory = new File(settings.getValue(SettingsClass.DIR_STORE));
        if (!storeDirectory.isDirectory()) {
            logger.warning("Creating store directory");
            storeDirectory.mkdirs();
        }
    }

    private static void cleanTempDir(final SettingsClass settings) {
        final File[] entries = new File(settings.getValue(SettingsClass.DIR_TEMP)).listFiles();
        for (final File entry : entries) {
            if (entry.isDirectory() == false) {
                entry.delete();
            }
        }
    }
}
