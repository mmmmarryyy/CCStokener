package com.vgkk.hula.config;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import com.vgkk.hula.i18n.PropertiesLocalizer;
import com.vgkk.hula.xml.SimpleErrorHandler;
import com.vgkk.hula.xml.XMLUtil;

public class Config {

    private static Config singletonConfig = null;

    private String appClass = "com.vgkk.hula.HulaApplication";

    private String configClass = "com.vgkk.hula.config.Config";

    private String xmlMakerClass = "com.vgkk.hula.config.ConfigXMLMaker";

    private File configFile;

    private File rootDirectory;

    private boolean debugMode = false;

    private int webUIPort = 9999;

    private String webUIRoot = "uidocs";

    private Locale defaultLocale = Locale.ENGLISH;

    private String dbClassName = "";

    private String dbDriver = "";

    private String dbConnect = "";

    private String dbUser = "";

    private String dbPasswd = "";

    private int dbPoolMaxSize = 1;

    private long dbPoolExpire = 100;

    private int sessionTimeout = 15 * 60;

    private String logSubDir = "logs";

    private Set<LoggingConfig> logConfigs = new HashSet<LoggingConfig>();

    /**
     * Creates a config object and attempts to initalize it's values from
     * a resource file named hula_config.xml located in the classpath. If an
     * error occirs while loading the file it is logged but the object is
     * initalized
     * @author (2004-Dec-17) Tim Romero CR: Daniel Leuck
     */
    public Config() {
        String resource = "hula_config.xml";
        URL url = ClassLoader.getSystemClassLoader().getResource(resource);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(resource);
        }
        if (url != null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(false);
            try {
                DocumentBuilder builder = dbf.newDocumentBuilder();
                builder.setErrorHandler(new SimpleErrorHandler());
                load(builder.parse(url.openStream()));
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the singleton. Subclasses can use setInstance to
     * make sure this Sigleton returns the approprate subclass.
     * This is named getBaseInstance so subclasses can override it
     * using the more natural getInstance() syntax
     * @see #setInstance
     * @return the Config Singletion
     * @author (2004-Oct-19) Tim Romero CR: Daniel Leuck
     * 
     * ?? Why is getInstance synchronized but not setInstance()? -Dan
     */
    public static synchronized Config getInstance() {
        return singletonConfig;
    }

    /**
     * Subclasses can set the config to ensure the approprate subclass is returned.
     * @param config the config subclass
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public static void setInstance(Config config) {
        singletonConfig = config;
    }

    /**
     * Loads the core elements of the confiuration file. Subclases that
     * add additionial tags ahould override the load(Document) method instead
     * of this one.
     * @param configFile The config file
     * @see #load(Document)
     * @author (2004-Dec-22) Tim Romero CR: Daniel Leuck
     */
    public final void load(File configFile) throws IOException, ParserConfigurationException, SAXException {
        rootDirectory = configFile.getParentFile().getParentFile();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        builder.setErrorHandler(new SimpleErrorHandler());
        load(builder.parse(configFile));
        this.configFile = configFile;
    }

    /**
     * Loads the any elements specified in the xml documnet  //this is the one to override
     * it does not overwrite any settings that are not contained in the documents
     * @param configDocument The config file       todo doc
     * @author (2005-Jul-11) Daniel Leuck CR: ??
     */
    protected void load(Document configDocument) throws IOException {
        Element docElem = configDocument.getDocumentElement();
        Element appElem = XMLUtil.getFirstElement(docElem, XML.APP_CONTAINER);
        if (appElem != null) {
            Element baseClass = XMLUtil.getFirstElement(appElem, XML.APP_TAG_BASE);
            if (baseClass != null) {
                appClass = baseClass.getAttribute(XML.APP_ATTRIB_CLASS);
            }
            Element conifgClass = XMLUtil.getFirstElement(appElem, XML.APP_TAG_CONFIG);
            if (conifgClass != null) {
                configClass = conifgClass.getAttribute(XML.APP_ATTRIB_CLASS);
            }
            Element xmlMaker = XMLUtil.getFirstElement(appElem, XML.APP_TAG_XML);
            if (xmlMaker != null) {
                xmlMakerClass = xmlMaker.getAttribute(XML.APP_ATTRIB_CLASS);
            }
        }
        if (docElem.hasAttribute(XML.ATTRIB_APP_DEBUG)) {
            debugMode = XMLUtil.getAttributeAsBoolean(docElem, XML.ATTRIB_APP_DEBUG);
        }
        Element uiElem = XMLUtil.getFirstElement(docElem, XML.UI_TAG);
        if (uiElem != null) {
            String uiPortString = uiElem.getAttribute(XML.UI_ATTRIB_PORT);
            if (uiPortString != null && uiPortString.length() != 0) {
                webUIPort = Integer.parseInt(uiPortString);
            }
            String uiRootString = uiElem.getAttribute(XML.UI_ATTRIB_ROOT);
            if (uiRootString != null && uiRootString.length() != 0) {
                webUIRoot = uiRootString;
            }
            String sessionTimeoutString = uiElem.getAttribute(XML.UI_ATTRIB_TIMEOUT);
            if (sessionTimeoutString != null && sessionTimeoutString.length() != 0) {
                int colIndex = sessionTimeoutString.indexOf(':');
                if (colIndex == -1) {
                    sessionTimeout = Integer.parseInt(sessionTimeoutString) * 60;
                } else {
                    int minutes = Integer.parseInt(sessionTimeoutString.substring(0, colIndex));
                    int seconds = Integer.parseInt(sessionTimeoutString.substring(colIndex + 1));
                    sessionTimeout = minutes * 60 + seconds;
                }
            }
        }
        Element locElem = XMLUtil.getFirstElement(docElem, XML.LOCALE_TAG);
        if (locElem != null) {
            String locString = locElem.getAttribute(XML.LOCALE_ATTRIB_DEFAULT);
            if (locString != null && locString.length() != 0) {
                String[] parts = locString.split("[_-]");
                if (parts.length == 1) {
                    defaultLocale = new Locale(parts[0].toLowerCase());
                } else if (parts.length == 2) {
                    defaultLocale = new Locale(parts[0].toLowerCase(), parts[1].toUpperCase());
                } else {
                    throw new InitializationException("Malformed local " + locString);
                }
                PropertiesLocalizer ploc = PropertiesLocalizer.getInstance();
                if (ploc.getProperties(defaultLocale) == null) throw new InitializationException("There are no properties for " + "the specified default locale " + defaultLocale);
            }
        }
        Element logElem = XMLUtil.getFirstElement(docElem, XML.LOG_CONTAINER);
        if (logElem != null) {
            String logDir = logElem.getAttribute(XML.LOG_ATTRIB_DIR);
            logSubDir = logDir == null ? "logs" : logDir;
            NodeList nodes = logElem.getElementsByTagName(XML.LOG_TAG);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element elm = (Element) nodes.item(i);
                String name = elm.getAttribute(XML.LOG_ATTRIB_NAME);
                LoggingConfig conf = null;
                if (LoggingJettyConfig.LOG_NAME.equals(name)) {
                    conf = new LoggingJettyConfig();
                } else {
                    conf = new LoggingConfig(name);
                }
                String file = elm.getAttribute(XML.LOG_ATTRIB_FILE);
                if (file != null) conf.setFileName(file);
                String level = elm.getAttribute(XML.LOG_ATTRIB_LEVEL);
                if (level != null) conf.setLogLevel(level);
                logConfigs.remove(conf);
                logConfigs.add(conf);
            }
        }
        Element databaseElem = XMLUtil.getFirstElement(docElem, XML.DB_TAG);
        if (databaseElem != null) {
            dbClassName = databaseElem.getAttribute(XML.DB_ATTRIB_CLASS);
            dbDriver = databaseElem.getAttribute(XML.DB_ATTRIB_DRIVER);
            Element conElem = XMLUtil.getFirstElement(databaseElem, XML.DB_TAG_CONNECT);
            dbConnect = conElem.getAttribute(XML.DB_ATTRIB_URL);
            dbUser = conElem.getAttribute(XML.DB_ATTRIB_USER);
            dbPasswd = conElem.getAttribute(XML.DB_ATTRIB_PASSWORD);
            Element poolElem = XMLUtil.getFirstElement(databaseElem, XML.DB_TAG_POOL);
            if (poolElem != null) {
                dbPoolMaxSize = Integer.parseInt(poolElem.getAttribute(XML.DB_ATTRIB_MAXSIZE));
                dbPoolExpire = Long.parseLong(poolElem.getAttribute(XML.DB_ATTRIB_EXPIRE));
            }
        }
    }

    /**
     * Returns the XMLMaker required to save the settings.
     * Subclasses can be defined in the hula_config.xml file
     * @see ConfigXMLMaker
     * @return the ConfigXMLMaker needs to save the config to a file
     * @author (2004-Dec-24) Tim Romero CR: Daniel Leuck
     */
    private ConfigXMLMaker getConfigXmlMaker() throws TransformerException {
        try {
            return (ConfigXMLMaker) Class.forName(xmlMakerClass).newInstance();
        } catch (Exception e) {
            throw new TransformerException("Invalid ConfigXMLWriter: " + xmlMakerClass, e);
        }
    }

    /**
     * Saves the config file overwriting the file used to load it
     * @throws TransformerException if the XMLFIle write is not correctly configured
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public void save() throws TransformerException {
        save(configFile);
    }

    /**
     * Returns the XML file this config has been loaded from or
     * null if the config has not been loaded.
     * @return the XML Config file or null if the config has not been loaded
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    protected File getConfigFile() {
        return configFile;
    }

    /**
     * Saves the conig to the specified file.
     * @param configFile the file to which the configuration data is to be writted
     * @throws TransformerException if the XMLMaker generats invalid XML or there
     * is a problem writing to the file system
     * @author (2004-Oct-19) Tim Romero  CR: Velin Doychinov
     */
    public void save(File configFile) throws TransformerException {
        ConfigXMLMaker saxReader = getConfigXmlMaker();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        SAXSource source = new SAXSource(saxReader, new InputSource());
        StreamResult result = new StreamResult(configFile);
        transformer.transform(source, result);
    }

    /**
     * Initalizes or reiniaizes the logs defined in the configuration file
     * This method should be called only after the Config object has been loaded. 
     * @author (2005-Jul-22) Tim Romero  CR: Daniel Leuck
     */
    public synchronized void initalizeLogging() throws IOException {
        File logDir = new File(logSubDir);
        if (!logDir.isAbsolute()) {
            logDir = new File(getRootDirectory(), logSubDir);
        }
        for (LoggingConfig logConf : logConfigs) {
            logConf.initLogger(logDir);
        }
    }

    /**
     * Returns the inital session locale for the UI
     * 
     * @return the default UI session language
     * @author (2005-Jul-08) Daniel Leuck CR: ??
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Returns the HulaApplication subcalss that is to be run with
     * this Config.
     * @return the name of the HulaApplication class
     * @author (2004-Dec-16) Tim Romero CR: Daniel Leuck
     */
    public String getApplicationClass() {
        return appClass;
    }

    /**
     * Returns the ConfigXMLMaker that is used to save the config file.
     * This is used primaraly for testing since an application will generally
     * not alter it's config file directly.
     * @return the XMLMaker for this application
     * @author (2004-Dec-24) Tim Romero CR: Daniel Leuck
     * 
     * ?? TODO testcase
     */
    public String getXmlMakerClass() {
        return xmlMakerClass;
    }

    /**
     * Returns the HulaConfig subcalss that is to be run with
     * this application.
     * @return the name of the HulaConfig class
     * @author (2004-Dec-16) Tim Romero CR: Daniel Leuck
     */
    public String getConfigClass() {
        return configClass;
    }

    /**
     * Returns true if the application is running in debug mode.
     * DebugMode has no pre-determined behivior. It is up to implementing
     * applications as to how it is to be used.
     * @return true if the application is running in debug mode
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Set debug mode.
     * DebugMode has no pre-determined behivior. It is up to implementing
     * applications as to how it is to be used.
     * @param debugMode wheather the application should run in debug mode
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Returns the groups file.
     * @return the groups Directory
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Returns the port on which the webUI is to listen
     * @return  the port on which the webUI is to listen
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public int getWebUIPort() {
        return webUIPort;
    }

    /**
     * Sets the port on which the webUI is to listen
     * @param  webUIPort the port number
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    void setWebUIPort(int webUIPort) {
        this.webUIPort = webUIPort;
    }

    /**
     * Returns the subdirectory name in which the webUI documents are stored
     * @return  the web ui documnet root
     * @author (2004-Oct-20) Tim Romero CR: Velin Doychinov
     */
    public String getWebUIRoot() {
        return webUIRoot;
    }

    /**
     * Sets the name of the subdirectory in which the WebUI documents are to
     * be stored
     * @param  webUIRoot the name of the sub directory
     * @author (2004-Oct-20) Tim Romero CR: Velin Doychinov
     */
    void setWebUIRoot(String webUIRoot) {
        this.webUIRoot = webUIRoot;
    }

    /**
     * Returns the session timeout in seconds
     * 
     * @return the session timeout in seconds.
     * @author (2005-Oct-25) Daniel Leuck CR: ??
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Set the session timeout in seconds
     * 
     * @param sessionTimeout the session timeout in seconds.
     */
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * Returns a set of the current logging configuration definitioins
     * @return the current logging confirguratioin
     * @author (2005-Jul-21) Tim Romero CR: Daniel Leuck
     */
    public Set<LoggingConfig> getLoggingConfigs() {
        return logConfigs;
    }

    /**
     * Returns the directory name into which the logs are written.
     * @return the log subdirectory
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public String getLogDir() {
        return logSubDir;
    }

    /**
     * Sets the directory into which the logs are written. If this is not 
     * a fully qualified path, the directory is assumed to relative to the
     * application root. 
     * @param logSubDir the log directory
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    void setLogDir(String logSubDir) {
        this.logSubDir = logSubDir;
    }

    /**
     * The class name of the Database subclass the application is to use.
     * @return The name of the database subclassclass
     * @author (2004-Oct-22) Tim Romero CR: Velin Doychinov
     */
    public String getDBClassName() {
        return dbClassName;
    }

    /**
     * Sets the class name of the Database subclass the application is to use.
     * This method should be used for testing purposes only
     * @param dbClassName the Database subclass class to use
     * @author (2004-Oct-22) Tim Romero CR: Velin Doychinov
     */
    void setDBClassName(String dbClassName) {
        this.dbClassName = dbClassName;
    }

    /**
     * The database connect string.
     * @return The database connect string.
     * @author (2004-Oct-22) Tim Romero CR: Velin DOychinov
     */
    public String getDBConnect() {
        return dbConnect;
    }

    /**
     * Sets the database connection String
     * @param dbConnect the database connection string
     * @author (2004-Oct-22) Tim Romero CR: Velin Doychinov
     */
    void setDBConnect(String dbConnect) {
        this.dbConnect = dbConnect;
    }

    /**
     * Returns the database user used to get connections
     * @return  the database user used to get connections
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public String getDBUser() {
        return dbUser;
    }

    /**
     * Sets the database user used to get connections.
     * @param dbUser user used to get connections.
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    void setDBUser(String dbUser) {
        this.dbUser = dbUser;
    }

    /**
     * Returns the database password used to get connections
     * @return  the database password used to get connections
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    public String getDBPasswd() {
        return dbPasswd;
    }

    /**
     * Sets the database password used to get connections
     * @param dbPasswd password used to get connections
     * @author (2004-Oct-19) Tim Romero CR: Velin Doychinov
     */
    void setDBPasswd(String dbPasswd) {
        this.dbPasswd = dbPasswd;
    }

    /**
     * Gets DB driver path
     * @return DB driver
     * @author (2004-Dec-29) Michael Petrov CR: Tim Romero
     * */
    public String getDBDriver() {
        return dbDriver;
    }

    /**
     * Sets DB driver path
     * @param dbDriver driver
     * @author (2004-Dec-29) Michael Petrov CR: Tim Romero
     * */
    public void setDBDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    /**
     * Gets max size of DB pool
     * @return max size of DB pool
     * @author (2004-Dec-29) Michael Petrov CR: Tim Romero
     * */
    public int getDBPoolMaxSize() {
        return dbPoolMaxSize;
    }

    /**
     * Sets max size of DB pool
     * @param dbPoolMaxSize DB pool max size
     * @author (2004-Dec-29) Michael Petrov CR: Tim Romero
     * */
    public void setDBPoolMaxSize(int dbPoolMaxSize) {
        this.dbPoolMaxSize = dbPoolMaxSize;
    }

    /**
     * Gets DB pool expire
     * @return DB pool expire
     * @author (2004-Dec-29) Michael Petrov CR: Tim Romero
     * */
    public long getDBPoolExpire() {
        return dbPoolExpire;
    }

    /**
     * Sets DB pool expire
     * @param dbPoolExpire DB pool expire
     * @author (2004-Dec-29) Michael Petrov CR: Tim Romero
     * */
    public void setDBPoolExpire(long dbPoolExpire) {
        this.dbPoolExpire = dbPoolExpire;
    }

    /**
     * An exception thrown when a configuration related problem is encountered
     * during initialization.
     * 
     * @author (2005-Jul-27) Daniel Leuck CR: ??
     */
    @SuppressWarnings("serial")
    public static class InitializationException extends RuntimeException {

        public InitializationException(String message) {
            super(message);
        }
    }
}
