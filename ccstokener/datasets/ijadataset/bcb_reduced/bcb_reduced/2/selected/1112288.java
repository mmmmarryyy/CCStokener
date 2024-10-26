package onepoint.project.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import onepoint.util.XIOHelper;

/**
 * Environment manager class for the Onepoint applications.
 *
 * @author horia.chiorean
 */
public class OpEnvironmentManager {

    private static final String DEFAULT_OPEN_START_FORM = "/forms/start.oxf.xml";

    private static final String DEFAULT_OPEN_LOGIN_FORM = "/forms/login.oxf.xml";

    private static final String DEFAULT_COMMERCIAL_LOGIN_FORM = "/team/forms/login.oxf.xml";

    private static final String DEFAULT_COMMERCIAL_START_FORM = "/team/forms/start.oxf.xml";

    protected static final Object MUTEX = new Object();

    /**
    * The simbolic name of the onepoint home environment variable
    */
    public static final String ONEPOINT_HOME_ENV_VAR = "ONEPOINT_PROJECT_HOME";

    public static final String ONEPOINT_HOME_CONFIG_VAR = "onepoint_project_home";

    public static final String ONEPOINT_HOME_LOCAL_FOLDER = "Onepoint Project Files";

    public static final String ONEPOINT_HOME_SERVER_FOLDER = "Onepoint Project Home";

    public static final String DATA_FOLDER = "data";

    public static final String CONFIGURATION_FILE_NAME = "configuration.oxc.xml";

    public static final String DEMODATA_FOLDER = "demodata";

    public static final String DEMODATA_FILE_NAME = "demodata.opx.xml";

    public static final String BACKUP_FOLDER = "backup";

    public static final String LOGS_FOLDER = "logs";

    public static final String LOG_FILE = "opproject.log";

    public static final String CALENDARS_FOLDER = "calendars";

    public String configurationFileName = CONFIGURATION_FILE_NAME;

    /**
    * A map of [productCode, boolean] pairs, indicating which application is multi user and which is not.
    */
    private static final Map<String, Boolean> PRODUCT_CODES_MAP = new HashMap<String, Boolean>();

    /**
    * A map of [productCode, humanReadableName] pairs
    */
    private static final Map<String, String> PRODUCT_CODE_HUMAN_NAME_MAP = new HashMap<String, String>();

    /**
    * A map of [productCode, String] pairs, indicating which start form should be used for each type of application.
    */
    private static final Map<String, String> CODE_START_FORM_MAP = new HashMap<String, String>();

    /**
    * A map of [productCode (String), about image path (String)] pairs
    */
    private static final Map<String, String> ABOUT_IMAGE_MAP = new HashMap<String, String>();

    /**
    * A map of [productCode, String] pairs, indicating which start form should be used for each type of application
    * in the case of autologin.
    */
    private static final Map<String, String> CODE_START_FORM_AUTO_LOGIN_MAP = new HashMap<String, String>();

    /**
    * The product code used in the initialization process
    */
    private String productCode = null;

    private File onepointHome = null;

    private File installationHome = null;

    private File dataFolder = null;

    private File backupsFolder = null;

    private File calendarsFolder = null;

    private File systemCalendarsFolder = null;

    private File configurationFile = null;

    private File demoDataFile = null;

    private File logFile = null;

    private String productVersion = null;

    private static OpEnvironmentManager instance;

    private static Class<? extends OpEnvironmentManager> instanceClass = OpEnvironmentManager.class;

    /**
    * Initialize the product codes map
    */
    static {
        PRODUCT_CODES_MAP.put(OpProjectConstants.BASIC_EDITION_CODE, Boolean.FALSE);
        PRODUCT_CODES_MAP.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, Boolean.FALSE);
        PRODUCT_CODES_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, Boolean.TRUE);
        PRODUCT_CODES_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, Boolean.TRUE);
        PRODUCT_CODES_MAP.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, Boolean.TRUE);
        PRODUCT_CODES_MAP.put(OpProjectConstants.NETWORK_EDITION_CODE, Boolean.TRUE);
        PRODUCT_CODES_MAP.put(OpProjectConstants.MASTER_EDITION_CODE, Boolean.FALSE);
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.BASIC_EDITION_CODE, "Basic");
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, "Professional");
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, "Community Server");
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, "Enterprise Server");
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, "Enterprise Cloud");
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.NETWORK_EDITION_CODE, "Group Server");
        PRODUCT_CODE_HUMAN_NAME_MAP.put(OpProjectConstants.MASTER_EDITION_CODE, "Master");
        CODE_START_FORM_MAP.put(OpProjectConstants.BASIC_EDITION_CODE, DEFAULT_OPEN_START_FORM);
        CODE_START_FORM_MAP.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, DEFAULT_COMMERCIAL_START_FORM);
        CODE_START_FORM_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, DEFAULT_OPEN_LOGIN_FORM);
        CODE_START_FORM_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, DEFAULT_COMMERCIAL_LOGIN_FORM);
        CODE_START_FORM_MAP.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, "/od/forms/login.oxf.xml");
        CODE_START_FORM_MAP.put(OpProjectConstants.NETWORK_EDITION_CODE, DEFAULT_COMMERCIAL_LOGIN_FORM);
        CODE_START_FORM_MAP.put(OpProjectConstants.MASTER_EDITION_CODE, DEFAULT_COMMERCIAL_START_FORM);
        ABOUT_IMAGE_MAP.put(OpProjectConstants.BASIC_EDITION_CODE, "/application/about_basic.png");
        ABOUT_IMAGE_MAP.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, "/professional/about_pro.png");
        ABOUT_IMAGE_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, "/servlet/about_open.png");
        ABOUT_IMAGE_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, "/servlet/about_enterprise.png");
        ABOUT_IMAGE_MAP.put(OpProjectConstants.NETWORK_EDITION_CODE, "/servlet/about_network.png");
        ABOUT_IMAGE_MAP.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, "/od/servlet/about_ondemand.png");
        ABOUT_IMAGE_MAP.put(OpProjectConstants.MASTER_EDITION_CODE, "/master/about_master.png");
        CODE_START_FORM_AUTO_LOGIN_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, DEFAULT_OPEN_START_FORM);
        CODE_START_FORM_AUTO_LOGIN_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, DEFAULT_COMMERCIAL_START_FORM);
        CODE_START_FORM_AUTO_LOGIN_MAP.put(OpProjectConstants.ON_DEMAND_EDITION_CODE, "/od/forms/start.oxf.xml");
        CODE_START_FORM_AUTO_LOGIN_MAP.put(OpProjectConstants.NETWORK_EDITION_CODE, DEFAULT_COMMERCIAL_START_FORM);
    }

    /**
    * Private constructor
    */
    protected OpEnvironmentManager() {
    }

    public static OpEnvironmentManager getInstance() {
        synchronized (MUTEX) {
            if (instance == null) {
                System.out.println("creating initializer of type: " + instanceClass.getName());
                try {
                    instance = instanceClass.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return instance;
        }
    }

    public static void setInstanceClass(Class<? extends OpEnvironmentManager> instanceClass) {
        OpEnvironmentManager.instanceClass = instanceClass;
    }

    /**
    * Gets the home directory path of the application.
    *
    * @return a <code>String</code> representing the home directory of the application.
    */
    public File getOnepointHome() {
        return onepointHome;
    }

    /**
    * Gets the home directory path of the application.
    *
    * @return a <code>String</code> representing the home directory of the application.
    */
    public File getInstallationHome() {
        if (installationHome == null) {
            installationHome = new File("").getAbsoluteFile();
        }
        return installationHome;
    }

    public void setInstallationHome(File installationHome) {
        this.installationHome = installationHome.getAbsoluteFile();
    }

    /**
    * Sets the home directory path of the application.
    *
    * @param onepointHome a <code>String</code> representing the path of the application home. Path uses "/" separator.
    * @return 
    * @throws FileNotFoundException
    */
    public void setOnepointHome(File onepointHome) throws FileNotFoundException {
        if (this.onepointHome != null && !this.onepointHome.equals(onepointHome)) {
            throw new IllegalStateException("Onepoint Home has already been initialized, this can not be done twice!");
        }
        onepointHome = onepointHome.getAbsoluteFile();
        createStructure(onepointHome);
        this.onepointHome = onepointHome;
        System.out.println("Onepoint Project Home set to : " + onepointHome.getAbsolutePath());
    }

    /**
    * Sets the application data folder path of the application.
    *
    * @param dataFolder a <code>String</code> representing the path of the application data folder.
    * @throws FileNotFoundException
    */
    public void setDataFolder(File dataFolder) throws FileNotFoundException {
        if (onepointHome == null) {
            throw new IllegalStateException("OnepointHome not initialized, doe that first!");
        }
        if (!dataFolder.isAbsolute()) {
            dataFolder = new File(getOnepointHome(), dataFolder.getPath());
        }
        ensureDir(dataFolder);
        this.dataFolder = dataFolder;
    }

    protected void createStructure(File home) throws FileNotFoundException {
        home = home.getAbsoluteFile();
        ensureDir(home);
        File backupFolder = new File(home, BACKUP_FOLDER);
        boolean copyDemoData = !backupFolder.exists();
        ensureDir(backupFolder);
        ensureDir(new File(home, CALENDARS_FOLDER));
        ensureDir(new File(home, LOGS_FOLDER));
        if (copyDemoData) {
            File backupFile = new File(DEMODATA_FOLDER, DEMODATA_FILE_NAME).getAbsoluteFile();
            if (backupFile.exists()) {
                try {
                    XIOHelper.copy(backupFile, new File(home, BACKUP_FOLDER).getAbsoluteFile());
                } catch (IOException e) {
                    System.out.println("could not copy demodata to backup folder");
                }
            }
        }
    }

    protected static void ensureDir(File dir) throws FileNotFoundException {
        if (dir == null) {
            return;
        }
        dir = dir.getAbsoluteFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new FileNotFoundException("could not create missing directories for '" + dir.getAbsolutePath() + "' (check required write file permissions)!");
            }
        } else if (!dir.isDirectory()) {
            throw new FileNotFoundException("could not create directory for '" + dir.getAbsolutePath() + "' (directory is a file not a directory)!");
        }
    }

    /**
    * Returns the path to the onepoint data directory. This path is system dependent.
    *
    * @return Path of the onepoint data folder. Path uses "/" separator.
    */
    public File getDataFolder() {
        synchronized (MUTEX) {
            if (dataFolder == null) {
                dataFolder = new File(getOnepointHome(), DATA_FOLDER);
                try {
                    ensureDir(dataFolder);
                } catch (FileNotFoundException e) {
                }
            }
        }
        return dataFolder;
    }

    public File getSystemCalendarsFolder() {
        synchronized (MUTEX) {
            if (systemCalendarsFolder == null) {
                systemCalendarsFolder = new File(getInstallationHome(), CALENDARS_FOLDER);
            }
        }
        return systemCalendarsFolder;
    }

    public File getCalendarsFolder() {
        synchronized (MUTEX) {
            if (calendarsFolder == null) {
                calendarsFolder = new File(getOnepointHome(), CALENDARS_FOLDER);
                try {
                    ensureDir(calendarsFolder);
                } catch (FileNotFoundException e) {
                }
            }
        }
        return calendarsFolder;
    }

    public File getConfigurationFile() {
        synchronized (MUTEX) {
            if (configurationFile == null) {
                configurationFile = new File(getOnepointHome(), configurationFileName);
            }
        }
        return configurationFile;
    }

    public void setConfigurationFile(File configurationFile) throws FileNotFoundException {
        if (onepointHome == null) {
            throw new IllegalStateException("OnepointHome not initialized, doe that first!");
        }
        if (!configurationFile.isAbsolute()) {
            configurationFile = new File(getOnepointHome(), configurationFile.getPath());
        }
        ensureDir(configurationFile.getParentFile());
        this.configurationFile = configurationFile;
    }

    public void setConfigurationFileName(String configurationFileName) throws FileNotFoundException {
        setConfigurationFile(new File(configurationFileName));
    }

    public File getDemoDataFile() {
        synchronized (MUTEX) {
            if (demoDataFile == null) {
                demoDataFile = new File(new File(getInstallationHome(), DEMODATA_FOLDER), DEMODATA_FILE_NAME);
            }
        }
        System.out.println("loading demodata from file: " + demoDataFile.getAbsolutePath());
        return demoDataFile;
    }

    public File getLogFile() {
        synchronized (MUTEX) {
            if (logFile == null) {
                logFile = new File(new File(getOnepointHome(), LOGS_FOLDER), LOG_FILE);
            }
        }
        return logFile;
    }

    public void setLogFile(File logFile) throws FileNotFoundException {
        if (onepointHome == null) {
            throw new IllegalStateException("OnepointHome not initialized, doe that first!");
        }
        if (!logFile.isAbsolute()) {
            logFile = new File(new File(getOnepointHome(), LOGS_FOLDER), logFile.getPath());
        }
        if (logFile.isDirectory()) {
            logFile = new File(logFile, LOG_FILE);
        }
        ensureDir(logFile.getParentFile());
        this.logFile = logFile;
    }

    public File getBackupsFolder() {
        synchronized (MUTEX) {
            if (backupsFolder == null) {
                backupsFolder = new File(getOnepointHome(), BACKUP_FOLDER);
            }
        }
        return backupsFolder;
    }

    public void setBackupsFolder(File backupsFolder) throws FileNotFoundException {
        if (onepointHome == null) {
            throw new IllegalStateException("OnepointHome not initialized, doe that first!");
        }
        if (!backupsFolder.isAbsolute()) {
            backupsFolder = new File(getOnepointHome(), backupsFolder.getPath());
        }
        ensureDir(backupsFolder);
        this.backupsFolder = backupsFolder;
    }

    /**
    * Returns the value of the multi-user flag, using the product code.
    *
    * @return true if the application is in multi-user mode
    */
    public boolean isMultiUser() {
        Boolean isMultiUser = PRODUCT_CODES_MAP.get(productCode);
        if (isMultiUser == null) {
            throw new UnsupportedOperationException("Cannot determine whether application is multi user or not");
        }
        return isMultiUser.booleanValue();
    }

    public boolean isNetworkEdition() {
        return productCode.equals(OpProjectConstants.NETWORK_EDITION_CODE);
    }

    public boolean isOnDemand() {
        return OpProjectConstants.ON_DEMAND_EDITION_CODE.equals(productCode);
    }

    public boolean isOpenEdition() {
        return OpProjectConstants.BASIC_EDITION_CODE.equals(productCode) || OpProjectConstants.OPEN_EDITION_CODE.equals(productCode);
    }

    /**
    * Returns the product code - one of the keys in PRODUCT_CODES_MAP
    *
    * @return product code.
    */
    public String getProductCode() {
        return productCode;
    }

    public String getProductCodeName() {
        return PRODUCT_CODE_HUMAN_NAME_MAP.get(productCode);
    }

    /**
    * Returns the path of the start form of the application based on the product code.
    *
    * @return start form path.
    */
    public String getStartForm() {
        return CODE_START_FORM_MAP.get(getProductCode());
    }

    /**
    * Returns the path of the about image based on the product code.
    *
    * @return about image path.
    */
    public String getAboutImage() {
        return ABOUT_IMAGE_MAP.get(getProductCode());
    }

    /**
    * Returns the path of the start form of the application based on the product code when the user is logged in automatically.
    *
    * @return start form path.
    */
    public String getAutoLoginStartForm() {
        return CODE_START_FORM_AUTO_LOGIN_MAP.get(getProductCode());
    }

    /**
    * sets the product code
    *
    * @param productCode new product code
    */
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    /**
    * Returns the product code, together the version numbers.
    *
    * @return a <code>String</code> composed of: "productCodeName" + "majorVersion" + "minorVersion";
    */
    public String getProductString() {
        return productCode + OpProjectConstants.CODE_VERSION_MAJOR_NUMBER;
    }

    public static Manifest getManifest() throws IOException, FileNotFoundException {
        URL url = OpEnvironmentManager.class.getResource("");
        System.out.println("Looking for Manifest at: " + url);
        Manifest mf = null;
        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            JarURLConnection jconn = (JarURLConnection) conn;
            mf = jconn.getManifest();
        } else {
            File mfFile = new File("META-INF/MANIFEST.MF");
            if (mfFile.exists()) {
                FileInputStream in = new FileInputStream(mfFile);
                mf = new Manifest(in);
                in.close();
            }
        }
        if (mf == null) {
            System.out.println("Manifest NOT found at: " + url);
        }
        return mf;
    }

    public String getProductVersion() {
        if (productVersion == null) {
            productVersion = initProductVersion();
        }
        return productVersion;
    }

    private static String initProductVersion() {
        try {
            Manifest mf = getManifest();
            if (mf != null) {
                Attributes attr = mf.getAttributes("Implementation");
                if (attr != null) {
                    return attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "unknown";
    }
}
