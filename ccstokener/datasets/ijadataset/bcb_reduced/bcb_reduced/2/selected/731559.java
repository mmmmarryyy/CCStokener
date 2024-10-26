package be.vds.jtbtaskplanner.client.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import be.vds.jtbtaskplanner.core.exceptions.TransferException;
import be.vds.jtbtaskplanner.core.logging.Syslog;

/**
 * This manages the different resources and the different path to them.
 * 
 * @author Gautier Vanderslyen
 * 
 */
public class ResourceManager {

    private static final Syslog LOGGER = Syslog.getLogger(ResourceManager.class);

    private static final String FILE_SEPARATOR = String.valueOf(File.separatorChar);

    private static Map<String, ImageIcon> imagesMap = new HashMap<String, ImageIcon>();

    public static final String USER_HOME = System.getProperty("user.home");

    public static final String JTB_HOME = USER_HOME + FILE_SEPARATOR + ".jtb";

    private static final String CONFIG_FOLDER_NAME = "config";

    private static final String PROPERTY_LAST_RELEASE = "release.last.stable";

    private static ResourceManager instance;

    private String defaultApplication;

    private String log4jFileName = "log4j.properties";

    private String settingsFile = "settings.xml";

    private String preferencesFile = "preferences.txt";

    private String layoutFile = "layout.txt";

    private ResourceManager() {
    }

    public void setLayoutFile(String layoutFile) {
        this.layoutFile = layoutFile;
    }

    public void setSettingsFile(String settingsFile) {
        this.settingsFile = settingsFile;
    }

    public String getLog4jFileName() {
        return log4jFileName;
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    public URL getPropertyResource(String propertyFileName) {
        String path = "resources/properties/" + propertyFileName;
        return ResourceManager.class.getClassLoader().getResource(path);
    }

    public InputStream getPropertyResourceAsInputStream(String propertyFileName) {
        String path = "resources/properties/" + propertyFileName;
        return ResourceManager.class.getClassLoader().getResourceAsStream(path);
    }

    public InputStream getResourceAsInputStream(String resourceName) {
        String path = resourceName;
        return ResourceManager.class.getClassLoader().getResourceAsStream(path);
    }

    public void setDefaultApplication(String application) {
        defaultApplication = application;
    }

    public File getLocalLog4jFile() {
        StringBuilder sb = new StringBuilder();
        sb.append(getApplicationConfigFolder()).append(FILE_SEPARATOR).append(log4jFileName);
        return new File(sb.toString());
    }

    public URL getLog4jFile() {
        File f = getLocalLog4jFile();
        if (f.exists()) {
            try {
                URL url = f.toURI().toURL();
                LOGGER.info("Using custom logger");
                return url;
            } catch (MalformedURLException e) {
                LOGGER.info("Using built in logger");
                return getPropertyResource("log4j.properties");
            }
        } else {
            LOGGER.info("Using built in logger");
            return getPropertyResource("log4j.properties");
        }
    }

    private String getApplicationFolder() {
        return JTB_HOME + FILE_SEPARATOR + defaultApplication;
    }

    private String getApplicationConfigFolder() {
        return getApplicationFolder() + FILE_SEPARATOR + CONFIG_FOLDER_NAME;
    }

    public String getConfigFolder() {
        return getApplicationFolder() + FILE_SEPARATOR + CONFIG_FOLDER_NAME;
    }

    /**
	 * returns the configuration file of the application. The File might be
	 * inexistant on the drive
	 * 
	 * @return
	 * @throws IOException
	 */
    public File getConfigFile(boolean createIfNotExists) throws IOException {
        File f = new File(getApplicationConfigFolder() + FILE_SEPARATOR + settingsFile);
        if (f.exists()) {
            return f;
        } else {
            if (createIfNotExists) {
                f = new File(JTB_HOME);
                if (!f.exists()) {
                    f.mkdir();
                }
                f = new File(getApplicationFolder());
                if (!f.exists()) {
                    f.mkdir();
                }
                f = new File(getApplicationConfigFolder());
                if (!f.exists()) {
                    f.mkdir();
                }
                f = new File(getApplicationConfigFolder() + FILE_SEPARATOR + settingsFile);
                f.createNewFile();
                return f;
            }
        }
        return null;
    }

    public BufferedImage getImage(String name) {
        URL url = ResourceManager.class.getClassLoader().getResource("resources/images/" + name);
        if (url == null) {
            LOGGER.warn("*** failed to find image resource '" + name + "'");
            return null;
        }
        BufferedImage i = null;
        try {
            i = ImageIO.read(url);
        } catch (IOException ex) {
            LOGGER.warn("*** failed to find image resource '" + name + "'");
        }
        return i;
    }

    public ImageIcon getImageIcon(String name) {
        ImageIcon img = imagesMap.get(name);
        if (null != img) {
            return img;
        }
        URL url = ResourceManager.class.getClassLoader().getResource("resources/images/" + name);
        if (null != url) {
            img = new ImageIcon(url);
            imagesMap.put(name, img);
            return img;
        } else {
            LOGGER.warn("*** can't find image : " + name);
            return null;
        }
    }

    public String getBundleBase() {
        return "resources/languages/language";
    }

    public Icon getCountryImageIcon(String image) {
        return getImageIcon("countries/" + image);
    }

    public URL getBinResource(String name) {
        return ResourceManager.class.getClassLoader().getResource("resources/bin/" + name);
    }

    public InputStream getReportAsInputStream(String report) {
        return ResourceManager.class.getClassLoader().getResourceAsStream("resources/reports/" + report);
    }

    public File getLayoutFile(boolean createIfNotExists) throws IOException {
        File f = new File(getApplicationConfigFolder() + FILE_SEPARATOR + layoutFile);
        if (f.exists()) {
            return f;
        } else {
            if (createIfNotExists) {
                f = new File(JTB_HOME);
                if (!f.exists()) {
                    f.mkdir();
                }
                f = new File(getApplicationFolder());
                if (!f.exists()) {
                    f.mkdir();
                }
                f = new File(getApplicationConfigFolder());
                if (!f.exists()) {
                    f.mkdir();
                }
                f = new File(getApplicationConfigFolder() + FILE_SEPARATOR + layoutFile);
                f.createNewFile();
                return f;
            }
        }
        return null;
    }

    public File getUserPreferencesFile() {
        return new File(getApplicationConfigFolder() + FILE_SEPARATOR + preferencesFile);
    }

    public URL getResourceAsURL(String resourceName) {
        return ResourceManager.class.getClassLoader().getResource(resourceName);
    }

    public URL getLicenceURL(String licence) {
        return getResourceAsURL("resources/licences/" + licence);
    }

    public String getLastReleaseVersion() throws TransferException {
        try {
            URL url = new URL("http://jtbdivelogbook.sourceforge.net/version.properties");
            URLConnection urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            urlConn.setReadTimeout(20000);
            urlConn.setConnectTimeout(10000);
            Properties props = new Properties();
            InputStream is = urlConn.getInputStream();
            props.load(is);
            is.close();
            String lastVersion = props.getProperty(PROPERTY_LAST_RELEASE);
            if (lastVersion == null) {
                LOGGER.warn("Couldn't find property " + PROPERTY_LAST_RELEASE);
            }
            return lastVersion;
        } catch (MalformedURLException e) {
            LOGGER.error(e);
            throw new TransferException(e);
        } catch (IOException e) {
            LOGGER.error(e);
            throw new TransferException(e);
        }
    }
}
