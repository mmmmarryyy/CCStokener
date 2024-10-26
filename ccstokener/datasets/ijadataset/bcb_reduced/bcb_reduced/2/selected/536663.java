package tivonage;

import java.io.File;
import java.io.FileDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import com.mutchek.vonaje.InvalidPhoneNumberException;
import com.mutchek.vonaje.VonagePhoneNumber;
import com.mutchek.vonaje.VonageAccount;
import com.tivo.hme.bananas.*;

/**
 *
 * @author mlamb
 */
public class TiVonage extends BApplication {

    public static String TITLE = "TiVonage Voice Mail";

    static java.util.Timer timer = new java.util.Timer(true);

    static java.util.Properties versionInfo;

    static File TIVONAGE_HOME;

    static File CACHE;

    static Config config;

    static UpdateChecker checker;

    private VonagePhoneNumber phoneNumber;

    private static java.io.File homeDir;

    static {
        homeDir = new java.io.File(System.getProperty("TIVONAGE_HOME"));
        System.out.println("TIVONAGE_HOME=" + homeDir.getAbsolutePath());
        loadConfig();
        loadVersionInfo();
        addCacheToClasspath();
        launchUpdateChecker();
    }

    MainMenuScreen mainMenuScreen;

    MessageDetailScreen messageDetailScreen;

    DeleteScreen deleteScreen;

    PlayScreen playScreen;

    InfoScreen infoScreen;

    protected void init(Context context) {
        super.init(context);
        mainMenuScreen = new MainMenuScreen(this);
        messageDetailScreen = new MessageDetailScreen(this);
        deleteScreen = new DeleteScreen(this);
        playScreen = new PlayScreen(this);
        infoScreen = new InfoScreen(this);
        push(mainMenuScreen, TRANSITION_NONE);
    }

    public static java.io.File getHomeDir() {
        return (homeDir);
    }

    public void dispatchKeyEvent(com.tivo.hme.sdk.HmeEvent.Key ir) {
        super.dispatchKeyEvent(ir);
    }

    public static Properties getNewerVersionProperties() {
        if (checker == null) {
            return null;
        } else {
            return checker.getNewerVersionProperties();
        }
    }

    private static void loadVersionInfo() {
        versionInfo = new Properties();
        try {
            versionInfo.load(TiVonage.class.getClassLoader().getResourceAsStream("tivonage/tivonage.properties"));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n\nTiVonage v " + versionInfo.getProperty("version", "UNKNOWN"));
        System.out.println("http://www.martiansoftware.com/lab/index.html#tivonage\n\n");
    }

    private static void launchUpdateChecker() {
        if (config.isUpdateCheckEnabled()) {
            checker = new UpdateChecker(versionInfo.getProperty("updateurl"), versionInfo.getProperty("version", "0.0"));
            new Thread(checker).start();
            System.out.println("Automatic update checks enabled.");
        } else {
            System.out.println("Automatic update checks disabled.");
        }
    }

    private static void loadConfig() {
        File f = new File(System.getProperty("user.home"));
        f = new File(f, ".hme");
        TIVONAGE_HOME = new File(f, "TiVonage");
        TIVONAGE_HOME.mkdirs();
        File configFile = new File(TIVONAGE_HOME, "tivonage.conf");
        config = new Config(configFile);
        if (!config.isConfigured()) config.showWizard();
    }

    private static void addCacheToClasspath() {
        CACHE = new File(TIVONAGE_HOME, "cache");
        CACHE.mkdirs();
        try {
            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class sysclass = URLClassLoader.class;
            java.lang.reflect.Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { CACHE.toURL() });
        } catch (Exception e) {
            System.err.println("\n\n*****");
            System.err.println("Unable to add voice mail cache directory " + CACHE.getAbsolutePath() + " to classpath.\n");
            e.printStackTrace();
        }
    }

    synchronized VonagePhoneNumber getPhoneNumber() {
        if (phoneNumber == null) {
            try {
                phoneNumber = new VonagePhoneNumber(new VonageAccount(config.getUsername(), config.getPassword()), config.getPhoneNumber());
                phoneNumber.setMessageCache(CACHE.getAbsolutePath());
            } catch (InvalidPhoneNumberException e) {
                e.printStackTrace();
            }
        }
        return (phoneNumber);
    }

    synchronized MP3Encoder getMP3Encoder() {
        MP3Encoder.SINGLETON.setCommandLine(config.getMp3Encoder());
        return (MP3Encoder.SINGLETON);
    }

    public static void main(String[] args) {
        config.showWizard();
        System.exit(0);
    }

    private static class UpdateChecker implements Runnable {

        private URL url;

        private double version;

        private boolean ok2check = false;

        private Properties newerProperties = null;

        public static final String VERSION_KEY = "version";

        public static final String DATE_KEY = "date";

        public static final String MESSAGE_KEY = "message";

        private static final long UPDATE_CHECK_PERIOD = 24 * 60 * 60 * 1000;

        public Properties getNewerVersionProperties() {
            return (newerProperties);
        }

        public UpdateChecker(String url, String version) {
            try {
                this.url = new URL(url);
                this.version = Double.parseDouble(version);
                ok2check = true;
            } catch (Exception e) {
                System.err.println("Unable to check for updates at " + url + ": " + e.getMessage());
                System.err.println("Will retry in " + UPDATE_CHECK_PERIOD / 1000 + " seconds.");
            }
        }

        public void run() {
            if (ok2check) {
                while (true) {
                    try {
                        Properties updateProps = new Properties();
                        updateProps.load(url.openStream());
                        if (Double.parseDouble(updateProps.getProperty("version", "0.0")) > version) {
                            newerProperties = updateProps;
                            System.out.println("New version available: " + newerProperties.getProperty("version"));
                            System.out.println(newerProperties.getProperty("message"));
                        } else {
                            System.out.println("Version " + version + " is up to date.");
                        }
                    } catch (Exception e) {
                        System.err.println("Exception while checking for updates: ");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(UPDATE_CHECK_PERIOD);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
