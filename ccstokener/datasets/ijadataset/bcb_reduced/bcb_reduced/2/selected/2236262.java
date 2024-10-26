package it.jwallpaper.util;

import it.jwallpaper.JWallpaperChanger;
import it.jwallpaper.JawcContext;
import it.jwallpaper.config.ConfigurationSupport;
import it.jwallpaper.platform.Platform;
import it.jwallpaper.platform.TrayIconMessageType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VersionUtils {

    private static String CHECKVERSIONURL = "http://jawc-wallpaperc.sourceforge.net/version.txt";

    private static String DOWNLOADURL = "https://sourceforge.net/project/showfiles.php?group_id=257943";

    public static String VERSION = "version";

    public static String VERSIONDATE = "versionDate";

    public static String AUTHOR = "author";

    private static String LASTVERSIONCHECKEDFILE = "Jawc-LastCheckedForNewVersion.ser";

    private static File lastCheckedFile;

    private static Log logger = LogFactory.getLog(VersionUtils.class);

    private static Map<Class, ResourceBundle> bundles = new HashMap<Class, ResourceBundle>();

    static {
        lastCheckedFile = new File(ConfigurationSupport.getConfigurationFolder(), LASTVERSIONCHECKEDFILE);
    }

    private static ResourceBundle getBundle(Class clazz) {
        if (!bundles.containsKey(clazz)) {
            String className = clazz.getName();
            className = className.substring(className.lastIndexOf('.') + 1);
            ResourceBundle bundle = ResourceBundle.getBundle(className + "-version", Locale.getDefault(), clazz.getClassLoader());
            bundles.put(clazz, bundle);
        }
        return bundles.get(clazz);
    }

    public static String getVersionInfo(Class clazz, String key) {
        String message = "";
        try {
            message = getBundle(clazz).getString(key);
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return message;
    }

    public static boolean shouldCheckForNewVersion() throws Exception {
        GregorianCalendar lastChecked;
        int checkEvery = JawcContext.getInstance().getJawcConfiguration().getCheckForNewVersionEvery();
        if (checkEvery > 0) {
            lastChecked = (GregorianCalendar) SerializationUtils.load(lastCheckedFile);
            if (lastChecked == null) {
                return true;
            } else {
                GregorianCalendar checkLimit = new GregorianCalendar();
                lastChecked.add(GregorianCalendar.DAY_OF_YEAR, checkEvery);
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                return lastChecked.before(checkLimit);
            }
        }
        return false;
    }

    public static void checkVersion() {
        Version serverVersion;
        Version localVersion;
        try {
            serverVersion = new Version(getServerVersion());
            localVersion = new Version(VersionUtils.getVersionInfo(JWallpaperChanger.class, VersionUtils.VERSION));
            if (localVersion.compareTo(serverVersion) < 0) {
                String title = MessageUtils.getMessage(JWallpaperChanger.class, "newVersion.title");
                String message = MessageUtils.getMessage(JWallpaperChanger.class, "newVersion.message");
                JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, UiUtils.getIcon(JWallpaperChanger.class, "/images/network-wireless-32.png"));
                JDialog dialog = pane.createDialog(null, title);
                dialog.setAlwaysOnTop(true);
                dialog.setModal(true);
                dialog.setVisible(true);
                Object selectedValue = pane.getValue();
                if (selectedValue.equals(JOptionPane.OK_OPTION)) {
                    Platform.getPlatform().openUrl(new URL(DOWNLOADURL));
                }
                SerializationUtils.save(lastCheckedFile, new GregorianCalendar());
            }
        } catch (Exception exc) {
            Platform.getPlatform().showBalloonMessage(JWallpaperChanger.class, "error", "error.checkForNewVersion", TrayIconMessageType.ERROR);
        }
    }

    public static String getServerVersion() throws IOException {
        URL url;
        url = new URL(CHECKVERSIONURL);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(false);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        InputStream in = httpURLConnection.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        out.flush();
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        String buffer;
        String[] lines;
        String version = "";
        buffer = out.toString();
        lines = StringUtils.split(buffer);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("version=")) {
                version = lines[i].substring(8).trim();
                break;
            }
        }
        return version;
    }
}
