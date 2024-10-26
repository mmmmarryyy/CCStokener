package ru.nxt.rvacheva;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.prefs.BackingStoreException;
import java.awt.EventQueue;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.jnlp.BasicService;
import instead.launcher.DialogJedy;
import instead.launcher.settings.Settings;
import static instead.launcher.DialogJedy.Message.*;
import static ru.nxt.rvacheva.Helper.PACKAGE.*;

/**
 * @author 7ectant
 */
public final class Helper {

    static {
        System.setSecurityManager(null);
        setDefaultLaF();
    }

    public static final Logger logger = Logger.getLogger("instead4j");

    public static final int USER_CANCELLED = 1;

    public static final int FATAL_ERROR = 2;

    private static final SYSTEM currentSystem = detectCurrentSystem();

    private static final PACKAGE packageType = findOutPackageType();

    private static boolean newInstallation = detectIfNewInstallation();

    private static boolean jnlpEnabled = false;

    private static BasicService basicService = null;

    private static SYSTEM detectCurrentSystem() {
        SYSTEM result = null;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            result = SYSTEM.Windows;
        } else if (os.indexOf("mac") >= 0) {
            result = SYSTEM.MacOSX;
        } else {
            result = SYSTEM.Linux;
        }
        return result;
    }

    private static PACKAGE findOutPackageType() {
        PACKAGE result = null;
        String savedValue = Settings.p.get(Settings.PACKAGE_TYPE, Settings._instead_path);
        if (!Settings._package_type.equals(savedValue)) {
            try {
                result = PACKAGE.valueOf(savedValue);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Некорректное тип пакета в настройках", t);
            }
        }
        if (result == null) {
            result = detectPackageType();
            Settings.p.put(Settings.PACKAGE_TYPE, result.name());
        }
        return result;
    }

    private static PACKAGE detectPackageType() {
        PACKAGE result = null;
        switch(currentSystem) {
            case Windows:
                result = exe;
                break;
            case MacOSX:
                result = dmg;
                break;
            default:
                {
                    result = detectLinuxPackageManager();
                    if (result == null) {
                        result = DialogJedy.askForPackageManager();
                    }
                }
        }
        return result;
    }

    private static PACKAGE detectLinuxPackageManager() {
        PACKAGE result = null;
        ProcessBuilder pb = new ProcessBuilder();
        Map<String, String> programs = new HashMap<String, String>();
        programs.put("rpm", rpm.name());
        programs.put("dpkg", deb.name());
        programs.put("emerge", ebuild.name());
        for (String p : programs.keySet()) {
            try {
                pb.command(p, "--version");
                pb.start();
                result = PACKAGE.valueOf(programs.get(p));
                break;
            } catch (IOException ioe) {
            }
        }
        return result;
    }

    private static boolean detectIfNewInstallation() {
        boolean result = true;
        try {
            result = !Settings.p.nodeExists(Settings.GAME_LIST_NODE_NAME);
        } catch (BackingStoreException bse) {
            logger.log(Level.SEVERE, "Ошибка при чтении настроек", bse);
            DialogJedy.showMsg(BACKING_STORE_ERROR);
        }
        return result;
    }

    public static void init() {
        initProxySupport();
        initJNLP();
    }

    public static void initProxySupport() {
        if (Settings.p.getBoolean(Settings.PROXY_ENABLED, Settings._proxy_enabled)) {
            System.setProperty("http.proxyHost", Settings.p.get(Settings.PROXY_HOST, Settings._proxy_host));
            System.setProperty("http.proxyPort", Settings.p.get(Settings.PROXY_PORT, Settings._proxy_port));
        } else {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.setProperty("java.net.useSystemProxies", "true");
        }
    }

    private static void setDefaultLaF() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Ошибка при установке LaF", e);
                }
            }
        });
    }

    public static void downloadUrlToFile(URL url, File targetFile) throws IOException {
        FileOutputStream targetFileWriting = null;
        InputStream urlDownloading = null;
        try {
            URLConnection conn = url.openConnection();
            urlDownloading = conn.getInputStream();
            targetFileWriting = new FileOutputStream(targetFile);
            int length = conn.getContentLength();
            targetFileWriting.getChannel().transferFrom(Channels.newChannel(urlDownloading), 0, length != -1 ? length : 1 << 24);
        } finally {
            if (urlDownloading != null) {
                try {
                    urlDownloading.close();
                } catch (IOException ioe) {
                    Helper.logger.log(Level.SEVERE, "Не удалось закрыть поток", ioe);
                }
            }
            if (targetFileWriting != null) {
                try {
                    targetFileWriting.close();
                } catch (IOException ioe) {
                    Helper.logger.log(Level.SEVERE, "Не удалось закрыть поток", ioe);
                }
            }
        }
    }

    public static void extractArchive(File sourceFile, File destDir) throws IOException {
        destDir.mkdirs();
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(sourceFile)));
            ReadableByteChannel zipEntryUnpacking = Channels.newChannel(zis);
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                FileOutputStream destFileStream = null;
                String currentEntry = entry.getName();
                File destFile = new File(destDir, currentEntry);
                File destinationParent = destFile.getParentFile();
                destinationParent.mkdirs();
                try {
                    if (!entry.isDirectory()) {
                        destFileStream = new FileOutputStream(destFile);
                        destFileStream.getChannel().transferFrom(zipEntryUnpacking, 0, 1 << 24);
                    }
                } catch (IOException ioe) {
                    Helper.logger.log(Level.SEVERE, "Ошибка при распаковке " + entry.getName(), ioe);
                } finally {
                    if (destFileStream != null) {
                        try {
                            destFileStream.close();
                        } catch (Exception e) {
                            Helper.logger.log(Level.SEVERE, "Ошибка при закрытии потока", e);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (Exception e) {
                    Helper.logger.log(Level.SEVERE, "Ошибка при закрытии потока", e);
                }
            }
        }
    }

    public static boolean isNewInstallation() {
        return newInstallation;
    }

    public static SYSTEM getCurrentSystem() {
        return currentSystem;
    }

    public static PACKAGE getPackageType() {
        return packageType;
    }

    static void setSingleInstance(final SingleInstanceListener sil) {
        if (jnlpEnabled) {
            try {
                final SingleInstanceService sis = (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
                sis.addSingleInstanceListener(sil);
                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        sis.removeSingleInstanceListener(sil);
                    }
                });
            } catch (UnavailableServiceException use) {
                Helper.logger.log(Level.SEVERE, "SingleInstanceService не поддерживается", use);
            }
        }
    }

    private static void initJNLP() {
        try {
            basicService = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
            jnlpEnabled = true;
        } catch (UnavailableServiceException use) {
            logger.log(Level.SEVERE, "JNLP-сервисы недоступны", use);
        }
    }

    public static boolean isJnlpEnabled() {
        return jnlpEnabled;
    }

    public static URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public enum PACKAGE {

        deb, rpm, ebuild, exe, dmg, unknown;

        @Override
        public String toString() {
            String result;
            if (equals(unknown)) {
                result = "Да я ставлю софт силой мысли!!";
            } else {
                result = name();
            }
            return result;
        }
    }

    public enum SYSTEM {

        Windows, Linux, MacOSX
    }
}
