package wotlas.common;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import javax.swing.ImageIcon;
import wotlas.libs.aswing.ASwingResourceLocator;
import wotlas.libs.graphics2d.FontResourceLocator;
import wotlas.libs.graphics2d.ImageResourceLocator;
import wotlas.libs.log.LogResourceLocator;
import wotlas.libs.persistence.PersistenceException;
import wotlas.libs.persistence.PropertiesConverter;
import wotlas.libs.sound.MusicResourceLocator;
import wotlas.libs.sound.SoundResourceLocator;
import wotlas.libs.wizard.WizardResourceLocator;
import wotlas.utils.Debug;
import wotlas.utils.FileTools;
import wotlas.utils.ResourceLookup;
import wotlas.utils.Tools;

/** Manages the different resources found in wotlas. Resources are searched in a JAR
 *  or via the provided (or default) base path. If we are in a JAR file ( server or
 *  client jar ) we call 'external resources' resources that are stored outside the
 *  JAR. 
 *
 * @author Aldiss
 */
public class ResourceManager implements LogResourceLocator, ImageResourceLocator, FontResourceLocator, MusicResourceLocator, SoundResourceLocator, WizardResourceLocator, ASwingResourceLocator {

    /** Wotlas Version Number for the code.
     */
    public static final String WOTLAS_VERSION = "1.3";

    /** Default location where are stored game resources when NOT packed in a Jar.
     *  We use this default value if (1) we are not in a JAR file,
     *  (2) no other base path is provided at start-up.
     */
    public static final String DEFAULT_BASE_PATH = "base";

    /** Default location where are stored help docs when NOT packed in a Jar.
     *  We only use this default value if we are not in a JAR file.
     */
    public static final String DEFAULT_HELP_DOCS_PATH = "docs/help";

    /** Default location for binaries and OS dependent scripts
     *  This is always an external directory.
     */
    public static final String DEFAULT_BIN_PATH = "bin";

    /** A part of the Wotlas client JAR file name.
     */
    public static final String WOTLAS_CLIENT_JAR = "client.jar";

    /** A part of the Wotlas Server JAR file name.
     */
    public static final String WOTLAS_SERVER_JAR = "server.jar";

    /** All Wotlas sources JAR file name.
     */
    public static final String WOTLAS_ALL_JAR = "wotlas.jar";

    /** Wotlas common resources JAR.
     */
    public static final String WOTLAS_COMMON_JAR = "common";

    /** Wotlas root dir for resources located in a JAR. We'll search in this
     *  directory for every kind of resources.
     *  MUST START WITH A "/".
     */
    public static final String WOTLAS_JAR_ROOT_RESOURCE_DIR = "/base";

    /** Wotlas root dir for docs located in the JAR
     *  MUST START WITH A "/".
     */
    public static final String WOTLAS_JAR_ROOT_DOCS_DIR = "/docs/help";

    /** Tells in which dir we can store external files. This directory will be created in the
     *  directory where the JAR is stored. See the wotlasJarExternalDir member field for the
     *  complete path.<BR>Must be different from other root dirs.
     */
    public static final String WOTLAS_JAR_EXTERNAL_DIR = "base-ext";

    /** Configs Directory Name
     */
    public static final String CONFIGS_DIR = "configs";

    /** Fonts Directory Name
     */
    public static final String FONTS_DIR = "fonts";

    /** GUI Images Directory Name
     */
    public static final String GUI_IMAGES_DIR = "gui";

    /** Image Library Directory Name
     */
    public static final String IMAGE_LIBRARY_DIR = "graphics/imagelib";

    /** Layouts Directory Name
     */
    public static final String LAYOUTS_DIR = "layouts";

    /** Logs Directory Name
     */
    public static final String LOGS_DIR = "logs";

    /** Configs Macros Directory Name
     */
    public static final String MACROS_DIR = "configs/macros";

    /** Music Directory Name
     */
    public static final String MUSICS_DIR = "music";

    /** Home Directory Name (where players are stored)
     */
    public static final String PLAYERS_HOME_DIR = "home";

    /** Server Configs Directory Name
     */
    public static final String SERVER_CONFIGS_DIR = "servers";

    /** GUI SMILEYS Images Directory Name
     */
    public static final String SMILEYS_IMAGES_DIR = "gui/chat";

    /** Sounds Directory Name
     */
    public static final String SOUNDS_DIR = "sounds";

    /** Universe Data Directory Name
     */
    public static final String UNIVERSE_DATA_DIR = "universe";

    /** Wizard Steps Directory Name
     */
    public static final String WIZARD_STEPS_DIR = "wizard";

    /** Windows Binary Directory Name
     */
    public static final String WIN_BINARY_DIR = "win32";

    /** Unix Binary Directory Name
     */
    public static final String UNIX_BINARY_DIR = "unix";

    /** Transfer script name (without the file extension)
     */
    public static final String TRANSFER_SCRIPT_NAME = "transferScript";

    /**
     * Filename of a file defining the list of resources present in the jars loaded.
     */
    public static final String JAR_RES_LIST_FILENAME = "resources.lst";

    /**
     * Jar directory separator.
     */
    public static final String JAR_SEP = "/";

    /**
     * List of files used for the universe definitions and loaded from jar files.
     */
    public static final String JAR_LIST_UNIVERSE_FILES = WOTLAS_JAR_ROOT_RESOURCE_DIR + JAR_SEP + UNIVERSE_DATA_DIR + JAR_SEP + JAR_RES_LIST_FILENAME;

    /**
     * List of files used in Wotlas and loaded from jar files.
     */
    public static final String JAR_LIST_BASE_FILES = WOTLAS_JAR_ROOT_RESOURCE_DIR + JAR_SEP + JAR_RES_LIST_FILENAME;

    /**
     * List of files for the image library used in Wotlas and loaded from jar files.
     */
    public static final String JAR_LIST_IMAGE_LIBRARY_FILES = WOTLAS_JAR_ROOT_RESOURCE_DIR + JAR_SEP + IMAGE_LIBRARY_DIR + JAR_SEP + JAR_RES_LIST_FILENAME;

    /**
     * List of files for the wizard steps used in Wotlas and loaded from jar files.
     */
    public static final String JAR_LIST_WIZARD_STEPS_FILES = WOTLAS_JAR_ROOT_RESOURCE_DIR + JAR_SEP + WIZARD_STEPS_DIR + JAR_SEP + JAR_RES_LIST_FILENAME;

    /** Where the fonts, graphics, musics, sounds, can be found...
     *  This path is only used if we are not in a JAR file.
     */
    private String basePath;

    /** Tells if we are in a JAR file or not.
     */
    private boolean inJar;

    /** Tells if we are in a wotlas client JAR file or not.
     */
    private boolean inClientJar;

    /** Tells if we are in a wotlas server JAR file or not.
     */
    private boolean inServerJar;

    /** Current Jar Name with full path.
     */
    private String jarName;

    /** Where we store external resources. This path is absolute and points out the JAR directory
     *  plus WOTLAS_JAR_EXTERNAL_DIR directory.
     */
    private String wotlasJarExternalDir;

    /** Constructor that uses default resource locations. Check the default
     *  JAR file names we'll seek (see the beginning of this class).
     *
     *  IMPORTANT : if the wotlas jar file is not found in the classpath
     *  we'll use the local classes and the default directory paths.
     * @param defaultBasePath if not null, the default base directory path;
     * @param serverSide true if it is a server side resourceManager.
     */
    public ResourceManager(String defaultBasePath, boolean serverSide) {
        this.inJar = false;
        this.jarName = getJarName();
        Debug.signal(Debug.NOTICE, this, "ResourceManager loaded from  jar ? " + this.jarName);
        if (this.jarName == null) {
            if (defaultBasePath == null || defaultBasePath.length() == 0) {
                this.basePath = ResourceManager.DEFAULT_BASE_PATH;
            } else {
                this.basePath = defaultBasePath;
            }
            return;
        }
        this.wotlasJarExternalDir = createJarExternalDir(defaultBasePath);
        Debug.signal(Debug.NOTICE, this, "ResourceManager jarExternalDir= " + this.wotlasJarExternalDir);
        if (this.jarName.indexOf(ResourceManager.WOTLAS_CLIENT_JAR) >= 0) {
            this.inClientJar = true;
            this.inJar = true;
            try {
                repairClassPath();
                createExternalClientDirectories();
            } catch (Exception e) {
                e.printStackTrace();
                Tools.displayDebugMessage("Deployment Failed", "Wotlas failed to extract config files to the local directory." + "\nCheck the access rights of the installation directory of wotlas.");
                Debug.exit();
            }
            return;
        }
        if (this.jarName.indexOf(ResourceManager.WOTLAS_SERVER_JAR) >= 0) {
            this.inServerJar = true;
            this.inJar = true;
            try {
                repairClassPath();
                createExternalServerDirectories();
            } catch (Exception e) {
                e.printStackTrace();
                Tools.displayDebugMessage("Deployment Failed", "Wotlas failed to extract config files to the local directory." + "\nCheck the access rights of the installation directory of wotlas.");
                Debug.exit();
            }
            return;
        }
        if (this.jarName.indexOf(ResourceManager.WOTLAS_ALL_JAR) >= 0 || this.jarName.indexOf(ResourceManager.WOTLAS_COMMON_JAR) >= 0) {
            if (serverSide) {
                this.inServerJar = true;
            } else {
                this.inClientJar = true;
            }
            this.inJar = true;
            try {
                repairClassPath();
                if (serverSide) {
                    createExternalServerDirectories();
                } else {
                    createExternalClientDirectories();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Tools.displayDebugMessage("Deployment Failed", "Wotlas failed to extract config files to the local directory." + "\nCheck the access rights of the installation directory of wotlas.");
                Debug.exit();
            }
            return;
        }
        Tools.displayDebugMessage("Wrong Jar Name", "The Jar file name should end with a 'client' or 'server' keyword");
        Debug.exit();
    }

    /**
     * Creation of an absolute path to our external resources.
     * @param defaultBasePath if not null, the default base directory path;
     * @return path for external resources.
     */
    private String createJarExternalDir(String defaultBasePath) {
        String extDir = null;
        String userDir = null;
        try {
            userDir = System.getProperty("user.dir");
        } catch (SecurityException se) {
        }
        if (defaultBasePath != null && defaultBasePath.length() != 0) {
            File extFile = new File(defaultBasePath);
            if (extFile.exists() && extFile.canWrite()) {
                extDir = extFile.getAbsolutePath().replace('\\', '/');
            }
            if (extDir == null && userDir != null && userDir.length() != 0) {
                extFile = new File(userDir, defaultBasePath);
                if (extFile.exists() && extFile.canWrite()) {
                    extDir = extFile.getAbsolutePath().replace('\\', '/');
                }
            }
        }
        if (extDir == null && userDir != null && userDir.length() != 0) {
            File extFile = new File(userDir, ResourceManager.WOTLAS_JAR_EXTERNAL_DIR);
            if (extFile.exists() && extFile.canWrite()) {
                extDir = extFile.getAbsolutePath().replace('\\', '/');
            }
        }
        if (extDir == null) {
            extDir = getJarDir() + ResourceManager.WOTLAS_JAR_EXTERNAL_DIR;
        }
        return extDir;
    }

    /** Changes the base path for resources.
     *  To use only if the resources are NOT in a JAR file.
     * @param basePath where the resources are located
     */
    public void setBasePath(String basePath) {
        if (this.inJar) {
            Debug.signal(Debug.WARNING, this, "Attempt to change the external basePath ! No need to we are in a JAR !");
            return;
        }
        this.basePath = basePath;
    }

    /** To tell if we are in a JAR.
     * @return true if we are in a JAR.
     */
    public boolean inJar() {
        return this.inJar;
    }

    /** Returns the name of the JAR we are into. We return the path+name.
     *  @return null if we are not in a jar.
     */
    protected String getJarName() {
        URL url = this.getClass().getResource("ResourceManager.class");
        if (url == null) {
            return null;
        }
        String sUrl = "" + url;
        if (!sUrl.startsWith("jar:")) {
            return null;
        }
        int end = sUrl.indexOf('!');
        if (end < 0) {
            return null;
        }
        int beg = sUrl.indexOf("/");
        if (beg < 0) {
            return null;
        }
        return sUrl.substring(beg + 1, end);
    }

    /** Returns the absolute path to the directory where this JAR is stored. Please retrieve
     *  the JAR name before calling this method. The path we return ends with a "/".
     */
    protected String getJarDir() {
        if (this.jarName == null) {
            return null;
        }
        int index = this.jarName.lastIndexOf("/");
        if (index < 0) {
            return null;
        }
        return this.jarName.substring(0, index + 1);
    }

    /** Repares the class path with the eventually missing current jar name.
     */
    protected void repairClassPath() {
        if (Tools.hasJar(this.jarName)) {
            return;
        }
        String cp = System.getProperty("java.class.path", ".") + System.getProperty("path.separator", ";") + this.jarName;
        if (this.jarName.indexOf("wotlas-common") != -1) {
            String libdir = this.jarName.substring(0, this.jarName.lastIndexOf("/"));
            cp = cp + System.getProperty("path.separator", ";") + libdir + "/wotlas-base-common.jar";
            cp = cp + System.getProperty("path.separator", ";") + libdir + "/wotlas-base-randland.jar";
        }
        System.setProperty("java.class.path", cp);
    }

    /** To tell if a path point out on an external resource of a JAR.
     *  We just compare the beginning of the file to the wotlasJarExternalDir field.
     *
     * @param pathName path to analyze
     * @return true if this is an external resource, false otherwise
     */
    public boolean isExternal(String pathName) {
        if (pathName.startsWith(this.wotlasJarExternalDir)) {
            return true;
        }
        if (pathName.indexOf(ResourceManager.PLAYERS_HOME_DIR) != -1) {
            return true;
        }
        return false;
    }

    /** Creates external directories if needed (if we are in a JAR).
     */
    public void createExternalClientDirectories() {
        if (!this.inClientJar) {
            return;
        }
        new File(getExternalConfigsDir()).mkdirs();
        new File(getExternalMacrosDir()).mkdirs();
        new File(getExternalLogsDir()).mkdirs();
        new File(getExternalServerConfigsDir()).mkdirs();
    }

    /** Creates external directories if needed (if we are in a JAR).
     */
    public void createExternalServerDirectories() {
        if (!this.inServerJar) {
            return;
        }
        new File(getExternalConfigsDir()).mkdirs();
        new File(getExternalLogsDir()).mkdirs();
        new File(getExternalServerConfigsDir()).mkdirs();
        new File(getExternalPlayersHomeDir()).mkdirs();
        new File(getUniverseDataDir()).mkdirs();
    }

    /** To complete this directory name with the appropriate path begining.
     * @param dirName directory name of the resource directory
     */
    protected String getResourceDir(String dirName) {
        if (this.inJar) {
            return ResourceManager.WOTLAS_JAR_ROOT_RESOURCE_DIR + "/" + dirName + "/";
        }
        return this.basePath + File.separator + dirName + File.separator;
    }

    /** To complete this external directory name with the appropriate path begining.
     *  By external we mean outside of the JAR if there is one. If there is none
     *  the directory returned is the same that would be returned by getResourceDir()
     *
     * @param dirName directory name of the resource directory
     * @return path ending with a "/"
     */
    protected String getExternalResourceDir(String dirName) {
        if (this.inJar) {
            return this.wotlasJarExternalDir + "/" + dirName + "/";
        }
        return this.basePath + File.separator + dirName + File.separator;
    }

    /** To get the Configs Directory (in the inside the eventual JAR).
     */
    public String getConfigsDir() {
        return getResourceDir(ResourceManager.CONFIGS_DIR);
    }

    /** To get the External Configs Directory (external means outside the eventual JAR).
     */
    public String getExternalConfigsDir() {
        return getExternalResourceDir(ResourceManager.CONFIGS_DIR);
    }

    /** To get the Fonts Directory.
     */
    public String getFontsDir() {
        return getResourceDir(ResourceManager.FONTS_DIR);
    }

    /**  To get the GUI Images Directory.
     */
    public String getGuiImageDir() {
        return getResourceDir(ResourceManager.GUI_IMAGES_DIR);
    }

    /**  To get the Image Library Directory.
     */
    public String getImageLibraryDir() {
        return getResourceDir(ResourceManager.IMAGE_LIBRARY_DIR);
    }

    /**  To get the Logs Directory Name.
     */
    public String getExternalLogsDir() {
        return getExternalResourceDir(ResourceManager.LOGS_DIR);
    }

    /** To get the Configs Macros Directory.
     */
    public String getExternalMacrosDir() {
        return getExternalResourceDir(ResourceManager.MACROS_DIR);
    }

    /**  To get the Music Directory.
     */
    public String getMusicsDir() {
        return getResourceDir(ResourceManager.MUSICS_DIR);
    }

    /**  To get the Home Directory (where players are stored)
     */
    public String getExternalPlayersHomeDir() {
        return getExternalResourceDir(ResourceManager.PLAYERS_HOME_DIR);
    }

    /** To get the Server Configs Directory (outside the jar)
     */
    public String getExternalServerConfigsDir() {
        return getExternalResourceDir(ResourceManager.SERVER_CONFIGS_DIR);
    }

    /** To get the GUI SMILEYS Images Directory.
     */
    public String getGuiSmileysDir() {
        return getResourceDir(ResourceManager.SMILEYS_IMAGES_DIR);
    }

    /** To get the Sounds Directory.
     */
    public String getSoundsDir() {
        return getResourceDir(ResourceManager.SOUNDS_DIR);
    }

    /** To get the Universe Data Directory. Server Universe data is always loaded from
     *  outside the eventual JAR.
     */
    public String getUniverseDataDir() {
        if (this.inClientJar) {
            return ResourceManager.WOTLAS_JAR_ROOT_RESOURCE_DIR + "/" + ResourceManager.UNIVERSE_DATA_DIR + "/";
        } else if (this.inServerJar) {
            return this.wotlasJarExternalDir + "/" + ResourceManager.UNIVERSE_DATA_DIR + "/";
        }
        return this.basePath + File.separator + ResourceManager.UNIVERSE_DATA_DIR + File.separator;
    }

    /** To get the Wizard Steps Directory.
     */
    public String getWizardStepsDir() {
        return getResourceDir(ResourceManager.WIZARD_STEPS_DIR);
    }

    /** To get the Layouts Directory.
     */
    public String getLayoutsDir() {
        return getResourceDir(ResourceManager.LAYOUTS_DIR);
    }

    /** To get Help Docs Directory.
     */
    public String getHelpDocsDir() {
        if (this.inJar) {
            return ResourceManager.WOTLAS_JAR_ROOT_DOCS_DIR + "/";
        }
        return ResourceManager.DEFAULT_HELP_DOCS_PATH + File.separator;
    }

    /** To list all the files in the universe directory ( on one level only ).
     *  The provided dirName must be a directory/sub-directory name under the
     *  universe directory. Because we know we are in the Universe directory we
     *  can perform intelligent choices whether to search in a JAR or not :
     *  Server Universe data is ALWAYS outside the JAR.
     *
     *  @param dirName directory name (must be a complete path)
     *  @param ext extension of the files to search, enter "" to get all the files.
     *  @return the files (not sub-dirs) that have the specified extension.
     */
    public String[] listUniverseFiles(String dirName, String ext) {
        if (ext == null) {
            ext = "";
        }
        if (this.inClientJar) {
            return Tools.listFilesInJar(ResourceManager.JAR_LIST_UNIVERSE_FILES, dirName, ext);
        } else {
            return FileTools.listFiles(dirName, ext);
        }
    }

    /** To list all the directories of a directory ( on one level only ).
     *  The provided dirName must be a directory/sub-directory name under the
     *  universe directory. Because we know we are in the Universe directory we
     *  can perform intelligent choices whether to search in a JAR or not :
     *  Server Universe data is ALWAYS outside the JAR.
     *
     *  @param dirName directory name (must be a complete path)
     *  @return the sub-dirs of the given dirName (on one level only).
     */
    public String[] listUniverseDirectories(String dirName) {
        if (this.inClientJar) {
            return Tools.listFilesInJar(ResourceManager.JAR_LIST_UNIVERSE_FILES, dirName, null);
        } else {
            return FileTools.listDirs(dirName);
        }
    }

    /** To load an object from a property file. The filePath MUST be corresponding to a
     *  valid resource path.
     *
     * @param fileName a file name that has been appended to the end of a directory path
     *        provided by this ResourceManager.
     */
    public Object loadObject(String filePath) {
        if (this.inClientJar && !isExternal(filePath)) {
            URL uri = getClassResourceUrl(this.getClass(), filePath);
            try {
                if (uri != null) {
                    InputStream is = uri.openStream();
                    Object o = null;
                    try {
                        o = PropertiesConverter.load(is);
                    } catch (PersistenceException pe) {
                        Debug.signal(Debug.ERROR, this, pe);
                    }
                    is.close();
                    return o;
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
        } else {
            try {
                return PropertiesConverter.load(filePath);
            } catch (PersistenceException pe) {
                Debug.signal(Debug.ERROR, this, "" + pe);
            }
        }
        return null;
    }

    /** To save an object to a property file. The filePath MUST be corresponding to an
     *  external path.
     *
     * @param o object to save.
     * @param fileName a file name that has been appended to the end of a directory path
     *        provided by this ResourceManager.
     */
    public boolean saveObject(Object o, String filePath) {
        try {
            PropertiesConverter.save(o, filePath);
            return true;
        } catch (PersistenceException pe) {
            Debug.signal(Debug.ERROR, this, pe);
            return false;
        }
    }

    /** To load text from a file. The filePath MUST be corresponding to a valid resource
     *  path.
     *
     * @param fileName a file name that has been appended to the end of a directory path
     *        provided by this ResourceManager.
     */
    public String loadText(String filePath) {
        if (this.inJar && !isExternal(filePath)) {
            URL uri = getClassResourceUrl(this.getClass(), filePath);
            try {
                if (uri != null) {
                    InputStream is = uri.openStream();
                    String txt = FileTools.loadTextFromStream(is);
                    is.close();
                    return txt;
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
            return "";
        } else {
            return FileTools.loadTextFromFile(filePath);
        }
    }

    /** To save text to a file. The filePath MUST be corresponding to an external path.
     *
     * @param fileName a file name that has been appended to the end of a directory path
     *        provided by this ResourceManager.
     * @param text text to save.
     */
    public boolean saveText(String filePath, String text) {
        return FileTools.saveTextToFile(filePath, text);
    }

    /** To load properties from a file. The filePath MUST be corresponding to a valid resource
     *  path.
     *
     * @param fileName a file name that has been appended to the end of a directory path
     *        provided by this ResourceManager.
     */
    public Properties loadProperties(String filePath) {
        if (this.inJar && !isExternal(filePath)) {
            URL uri = getClassResourceUrl(this.getClass(), filePath);
            try {
                if (uri != null) {
                    InputStream is = uri.openStream();
                    Properties prop = FileTools.loadPropertiesFromStream(is);
                    is.close();
                    return prop;
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
            return new Properties();
        } else {
            return FileTools.loadPropertiesFile(filePath);
        }
    }

    /** To save text to a file. The filePath MUST be corresponding to an external path.
     *
     * @param props properties to save.
     * @param fileName a file name that has been appended to the end of a directory path
     *        provided by this ResourceManager.
     * @param header some text to display as header in the file.
     */
    public boolean saveProperties(Properties props, String filePath, String header) {
        return FileTools.savePropertiesFile(props, filePath, header);
    }

    /** To list all the files of a directory ( on one level only ).
     *  USE THIS METHOD ONLY IF YOU KNOW THAT THE DIRECTORY GIVEN HERE COULD BE
     *  IN A JAR (if there is a jar, if none we search in the provided base path).
     *
     *  IF you know that the directory is NOT in a JAR then use a simple File.listFiles().
     *
     *  @param dirName directory name (must be a complete path)
     *  @return all the files of the specified directory (not sub-dirs).
     */
    public String[] listFiles(String dirName) {
        return listFiles(dirName, "");
    }

    /** To list all the files of a directory ( on one level only ).
     *  USE THIS METHOD ONLY IF YOU KNOW THAT THE DIRECTORY GIVEN HERE COULD BE
     *  IN A JAR (if there is a jar, if none we search in the provided base path).
     *
     *  IF you know that the directory is NOT in a JAR then use a simple File.listFiles().
     *
     *  @param dirName directory name (must be a complete path)
     *  @param ext extension of the files to search, enter "" to get all the files.
     *  @return the files (not sub-dirs) that have the specified extension.
     */
    public String[] listFiles(String dirName, String ext) {
        if (ext == null) {
            ext = "";
        }
        if (this.inJar && !isExternal(dirName)) {
            if (dirName.indexOf(IMAGE_LIBRARY_DIR) != -1) {
                return Tools.listFilesInJar(ResourceManager.JAR_LIST_IMAGE_LIBRARY_FILES, dirName, ext);
            }
            if (dirName.indexOf(WIZARD_STEPS_DIR) != -1) {
                return Tools.listFilesInJar(ResourceManager.JAR_LIST_WIZARD_STEPS_FILES, dirName, ext);
            }
            Debug.signal(Debug.WARNING, this, "Attempt to find a list of files in the default resources list :" + dirName + ", " + ResourceManager.JAR_LIST_BASE_FILES);
            return Tools.listFilesInJar(ResourceManager.JAR_LIST_BASE_FILES, dirName, ext);
        } else {
            return FileTools.listFiles(dirName, ext);
        }
    }

    /** To list all the directories of a directory ( on one level only ).
     *  USE THIS METHOD ONLY IF YOU KNOW THAT THE DIRECTORY GIVEN HERE COULD BE
     *  IN A JAR (if there is a jar, if none we search in the provided base path).
     *
     *  IF you know that the directory is NOT in a JAR then use a simple File.listFiles().
     *
     *  @param dirName directory name (must be a complete path)
     *  @return the sub-dirs of the given dirName (on one level only).
     */
    public String[] listDirectories(String dirName) {
        if (this.inJar && !isExternal(dirName)) {
            if (dirName.indexOf(IMAGE_LIBRARY_DIR) != -1) {
                return Tools.listFilesInJar(ResourceManager.JAR_LIST_IMAGE_LIBRARY_FILES, dirName, null);
            }
            if (dirName.indexOf(WIZARD_STEPS_DIR) != -1) {
                return Tools.listFilesInJar(ResourceManager.JAR_LIST_WIZARD_STEPS_FILES, dirName, null);
            }
            Debug.signal(Debug.WARNING, this, "Attempt to find a list of directories in the default resources list :" + dirName + ", " + ResourceManager.JAR_LIST_BASE_FILES);
            return Tools.listFilesInJar(ResourceManager.JAR_LIST_BASE_FILES, dirName, null);
        } else {
            return FileTools.listDirs(dirName);
        }
    }

    /** To get a font resource as a stream.
     * @param fontPath font path we'll get as a stream
     * @return InputStream on the wanted font, null if the font was not found
     */
    public InputStream getFontStream(String fontPath) {
        String fDir = getFontsDir();
        if (!fontPath.startsWith(fDir)) {
            fontPath = fDir + fontPath;
        }
        if (this.inJar) {
            try {
                URL uri = getClassResourceUrl(this.getClass(), fontPath);
                if (uri != null) {
                    return uri.openStream();
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
        } else {
            try {
                return new FileInputStream(fontPath);
            } catch (FileNotFoundException fe) {
                Debug.signal(Debug.ERROR, this, fe);
            }
        }
        return null;
    }

    /** To get an inputstream on the wanted music.
     * @param musicName music name such as "tarvalon.mid"
     * @return InputStream on the wanted music, null if the music was not found
     */
    public InputStream getMusicStream(String musicName) {
        String mDir = getMusicsDir();
        if (!musicName.startsWith(mDir)) {
            musicName = mDir + musicName;
        }
        if (this.inJar) {
            try {
                URL uri = getClassResourceUrl(this.getClass(), musicName);
                if (uri != null) {
                    return uri.openStream();
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
        } else {
            try {
                return new FileInputStream(musicName);
            } catch (FileNotFoundException fe) {
                Debug.signal(Debug.ERROR, this, fe);
            }
        }
        return null;
    }

    /** To get an inputstream on the wanted sound.
     * @param soundName sound name such as "boing.wav"
     * @return InputStream on the wanted sound, null if the sound was not found
     */
    public InputStream getSoundStream(String soundName) {
        String sDir = getSoundsDir();
        if (!soundName.startsWith(sDir)) {
            soundName = sDir + soundName;
        }
        if (this.inJar) {
            try {
                URL url = getClassResourceUrl(this.getClass(), soundName);
                if (url != null) {
                    return url.openStream();
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
        } else {
            try {
                return new FileInputStream(soundName);
            } catch (FileNotFoundException fe) {
                Debug.signal(Debug.ERROR, this, fe);
            }
        }
        return null;
    }

    /** To get an inputstream from a wanted file
     * @param filePath complete file path
     * @return InputStream on the wanted file, null if the file was not found
     */
    public InputStream getFileStream(String filePath) {
        if (this.inJar) {
            try {
                URL url = getClassResourceUrl(this.getClass(), filePath);
                if (url != null) {
                    return url.openStream();
                }
            } catch (IOException ioe) {
                Debug.signal(Debug.ERROR, this, ioe);
            }
        } else {
            try {
                return new FileInputStream(filePath);
            } catch (FileNotFoundException fe) {
                Debug.signal(Debug.ERROR, this, fe);
            }
        }
        return null;
    }

    /** Returns the wanted image icon from the base's GUI directory.
     *
     *  @param imageName imageName with or without the complete resource path.
     *  @return ImageIcon, null if the image was not found.
     */
    public ImageIcon getImageIcon(String imageName) {
        String imDir = getGuiImageDir();
        if (!imageName.startsWith(imDir)) {
            imageName = imDir + imageName;
        }
        if (this.inJar) {
            URL url = getClassResourceUrl(this.getClass(), imageName);
            if (url != null) {
                return new ImageIcon(url);
            }
        } else {
            return new ImageIcon(imageName);
        }
        return null;
    }

    /** Returns the wanted image from the base GUI directory.
     *
     *  @param imageName imageName with or without the complete resource path.
     *  @return Image, null if the image was not found.
     */
    public Image getGuiImage(String imageName) {
        String imDir = getGuiImageDir();
        if (!imageName.startsWith(imDir)) {
            imageName = imDir + imageName;
        }
        if (this.inJar) {
            URL url = getClassResourceUrl(this.getClass(), imageName);
            if (url != null) {
                return Toolkit.getDefaultToolkit().getImage(url);
            }
        } else {
            return Toolkit.getDefaultToolkit().getImage(imageName);
        }
        return null;
    }

    /** Returns the wanted image from the Image Library directory.
     *
     *  @param imagePath image name from the library with FULL resource path.
     *  @return Image, null if the image was not found.
     */
    public Image getLibraryImage(String imagePath) {
        if (this.inJar) {
            URL url = getClassResourceUrl(this.getClass(), imagePath);
            if (url != null) {
                return Toolkit.getDefaultToolkit().getImage(url);
            }
        } else {
            return Toolkit.getDefaultToolkit().getImage(imagePath);
        }
        return null;
    }

    /**
     * Returns the wanted resource url from the jars loaded.
     * @param resourceName resource name with FULL resource path.
     * @return url or null if the image was not found.
     */
    public URL getClassResourceUrl(Class claRef, String resourceName) {
        Class cla = claRef;
        if (cla == null) {
            cla = this.getClass();
        }
        URL url = ResourceLookup.getClassResourceUrl(cla, resourceName);
        if (url == null) {
            Debug.signal(Debug.WARNING, this, "Resource (" + resourceName + ") not found !");
        }
        return url;
    }

    /** To get the path to the OS dependent tranfer script. This script will be used to
     *  transfer the server-X.cfg.adr file to the central wotlas web server.
     *  @return full path to the script to use for file transfer. This method uses the system
     *    property "os.name" to return a path to the right script.
     */
    public String getExternalTransferScript() {
        if (this.inClientJar) {
            return null;
        }
        if (Tools.isWindowsOS()) {
            return ResourceManager.DEFAULT_BIN_PATH + File.separator + ResourceManager.WIN_BINARY_DIR + File.separator + ResourceManager.TRANSFER_SCRIPT_NAME + ".bat";
        } else {
            return ResourceManager.DEFAULT_BIN_PATH + File.separator + ResourceManager.UNIX_BINARY_DIR + File.separator + ResourceManager.TRANSFER_SCRIPT_NAME + ".sh";
        }
    }

    /** To get the path to the OS dependent scripts.
     *  @return full path to where the scripts are. This method uses the system
     *    property "os.name" to return a path to the right script.
     */
    public String getExternalScriptsDir() {
        if (this.inClientJar) {
            return null;
        }
        if (Tools.isWindowsOS()) {
            return ResourceManager.DEFAULT_BIN_PATH + File.separator + ResourceManager.WIN_BINARY_DIR + File.separator;
        } else {
            return ResourceManager.DEFAULT_BIN_PATH + File.separator + ResourceManager.UNIX_BINARY_DIR + File.separator;
        }
    }
}
