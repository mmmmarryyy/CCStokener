package org.stheos.pos.config;

import org.stheos.pos.object.AppPort;
import org.stheos.pos.object.AppProperty;
import org.stheos.pos.service.ConfigMgr;
import org.stheos.utils.File.Directory;
import java.awt.Image;
import org.w3c.dom.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import javax.xml.namespace.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.swing.UIManager;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.stheos.pos.object.Currency;
import org.stheos.security.Crypto;
import org.stheos.utils.process.Launcher;

public class CoreConfig {

    /** Récupére l'instance unique de la class Singleton.<p>
    * Remarque : le constructeur est rendu inaccessible
    */
    public static CoreConfig getInstance() {
        if (null == instance) {
            instance = new CoreConfig();
        }
        return instance;
    }

    /** Constructeur redéfini comme étant privé pour interdire
    * son appel et forcer à passer par la méthode <link
    */
    private CoreConfig() {
        String osName = System.getProperty("os.name");
        logger.log(Level.INFO, "OS name : " + osName);
        if (osName.toUpperCase().startsWith("WINDOWS")) {
            shellCmd = "cmd";
            shellOpt = "/k";
            logger.log(Level.INFO, "command shell : " + shellCmd);
        } else if (osName.toUpperCase().startsWith("LINUX")) {
            shellCmd = "/bin/sh";
            shellOpt = "-c";
            logger.log(Level.INFO, "command shell : " + shellCmd);
        }
        logger = Logger.getLogger("org.stheos.pos.logger.debug");
        Directory.mkdirsIfNotExist(new String[] { System.getProperty("user.dir") + "/conf", System.getProperty("user.dir") + "/data", System.getProperty("user.dir") + "/logs", System.getProperty("user.dir") + "/reports", System.getProperty("user.dir") + "/plugins", System.getProperty("user.dir") + "/backup" });
        loadConfig();
    }

    /** L'instance statique */
    private static CoreConfig instance;

    /********************************************************************************/
    public static Logger logger = logger = Logger.getLogger("org.stheos.pos.logger.debug");

    public static final String APP_NAME = "KafePOS";

    public static final String APP_PATH = System.getProperty("user.dir");

    public static final String CONF_PATH = APP_PATH + "/conf";

    public static final String CONF_FILE_NAME = "kpos.cfg.xml";

    public static final String CONF_FILE_PATH = CONF_PATH + "/" + CONF_FILE_NAME;

    public static final String INIT_CMD_FILE = CONF_PATH + "/init.ini";

    public static final int LOADING_OK = 0;

    public static final int LOADING_FIRST_RUN = -1;

    public static final int LOADING_ERR_NO_CONF = -11;

    public static final int LOADING_ERR_DB_CONN = -21;

    public static final int LOADING_ERR_DB_STRUCT = -31;

    public static final int LOADING_ERR_DB_DATA = -32;

    public static final int LOADING_ERR_APP_DIR = -41;

    public static final int LOADING_ERR_UNKNOW = -99;

    public static final short TICKET_INIT = 1;

    public static final short TICKET_OPENED = 10;

    public static final short TICKET_PENDING = 20;

    public static final short TICKET_IN_PAYMT = 80;

    public static final short TICKET_CLOSED = 90;

    public static final short TICKET_CANCELED = 99;

    public static final short UI_MENU = 1;

    public static final short UI_CASH_REG = 10;

    public static final String DEFAULT_COLOR = "#C3C3D2";

    public static final String SCHEMA_NAME = "POS";

    public static final String DEFAULT_MYSQL_PORT = "3306";

    public static final String DEFAULT_JAVADB_PORT = "1555";

    public static final String[][] SUPPORTED_DB = { { "javadb", "JavaDB Embedded", DEFAULT_JAVADB_PORT, "org.apache.derby.jdbc.EmbeddedDriver" }, { "mysql", "MySQL Server", DEFAULT_MYSQL_PORT, "com.mysql.jdbc.Driver" } };

    static final TreeMap mapLookNFeel = new TreeMap();

    static Properties initCmdProp = new Properties();

    private static String shellCmd = "";

    private static String shellOpt = "";

    private static ResourceBundle resBundle = ResourceBundle.getBundle("org/stheos/pos/i18n/messages");

    private int loadingRes = LOADING_ERR_UNKNOW;

    private static short idPos;

    private static Boolean forcedToCheckData;

    private static String drvClass;

    private static String dialect;

    private static String dbUser;

    private static String dbPass;

    private static String dbType;

    private static String dbPort;

    private static String dbName;

    private static String dbHost;

    private static String dbUrl;

    private static String dbLabel;

    private static String dirReport;

    private static List dbDefCmd = new ArrayList<String>();

    private static String UIScreenRes;

    private static String urlReports = "";

    private static String defaultDataSampleUrl = "file:data/basicGuadeloupe.jar";

    private static String devReceptPrinterName;

    private static String devReceptPrinterProtocol;

    private static String jPosDevCheckPrinter;

    private static String jPosDevStandardPrinter;

    private static String jPosDevSmartCard;

    private static String jPosDevKeyboard;

    private static String jPosDevCashDrawer;

    private static String jPosDevLineDisplay;

    private static String receptPrinterLineLen;

    private static String jPosReceptPrinterXmlPosCompliant;

    private static String jPosReceptPrinterLang;

    private static String jPosReceptPrinterPort;

    private static String checkProcessDevice;

    private static String smartCardProtocol;

    private static String smartCardPortID;

    private static String smartCardPortOpenTime;

    private static String smartCardPortBufferSize;

    private static String smartCardPortTimeOut;

    private static String smartCardPortBauds;

    private static String smartCardPortDataBits;

    private static String smartCardPortStopBit;

    private static String smartCardPortParity;

    private static String behaviorTva;

    private static String behaviorTvaRecept;

    public static Currency posCurrency;

    private static AppPort smartCardPort = new AppPort();

    private static String lastErrorMsgs;

    private static String themeResFile;

    private static String moneyResFile;

    public static Image[] prodImgBg;

    public static Map moneyResImg = new HashMap<String, ImageIcon>();

    public static Map themeResImg = new HashMap<String, ImageIcon>();

    public static Map themeResUrl = new HashMap<String, URL>();

    public static Map themeResStyle = new HashMap<String, String>();

    private static Integer[] availableCoinImg;

    private static Integer[] availableBillImg;

    public static Boolean useCurrencyImg = true;

    public static void setDebugLogger(boolean act) {
        if (act) {
            logger = Logger.getLogger("org.stheos.pos.logger.debug");
        } else {
            logger = Logger.getLogger("org.stheos.pos.logger.standard");
        }
    }

    public static Connection getConnection() {
        Connection dbCon = null;
        try {
            Class.forName(drvClass).newInstance();
            dbCon = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPass);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Connexion impossible : " + e);
            lastErrorMsgs += "Connexion impossible : " + e + "\n";
        }
        return dbCon;
    }

    public static ResourceBundle getBundle() {
        return resBundle;
    }

    public void loadConfig() {
        File cfgFile = new File(CONF_FILE_PATH);
        logger.log(Level.INFO, "Fichier config : " + CONF_FILE_PATH);
        lastErrorMsgs = "";
        forcedToCheckData = true;
        File firstRunFile = new File(CoreConfig.CONF_PATH + "/firstRun");
        if (firstRunFile.exists()) {
            loadDefaultConfValues();
            loadingRes = LOADING_FIRST_RUN;
        } else {
            if (!cfgFile.exists()) {
                logger.log(Level.INFO, "PAS DE FICHIER DE CONFIG : chargements des paramètres par défaut");
                loadDefaultConfValues();
                loadingRes = LOADING_ERR_NO_CONF;
            } else {
                loadDOMConfigFile();
                new File(System.getProperty("user.dir") + "/logs").mkdir();
                try {
                    Class.forName(drvClass).newInstance();
                    java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPass);
                    loadingRes = LOADING_OK;
                } catch (Exception e) {
                    logger.log(Level.ERROR, "Connexion impossible : " + e);
                    lastErrorMsgs += "Connexion impossible : " + e + "\n";
                    loadingRes = LOADING_ERR_DB_CONN;
                }
            }
        }
        smartCardPortID = "1";
        loadThemeRessources();
        loadThemeStyle();
        loadCurrencyRessources();
        UIScreenRes = "1024x768";
    }

    public void loadThemeRessources() {
        String resFile;
        ImageIcon img = new ImageIcon();
        URL sndUrl;
        themeResFile = "default.jar";
        resFile = System.getProperty("user.dir") + "/conf/themes/" + themeResFile;
        themeResImg.clear();
        img = getRessourceImg(resFile, "/ressources/icons/prev24.png");
        themeResImg.put("iconPrev24", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 24px OK!"); else CoreConfig.logger.debug("iconPrev 24px NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/prev24w.png");
        themeResImg.put("iconPrev24w", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 24px wide OK!"); else CoreConfig.logger.debug("iconPrev 24px wide NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/prev32.png");
        themeResImg.put("iconPrev32", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 32px OK!"); else CoreConfig.logger.debug("iconPrev 32px NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/prev32w.png");
        themeResImg.put("iconPrev32w", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 32px wide OK!"); else CoreConfig.logger.debug("iconPrev 32px wide NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/prev48.png");
        themeResImg.put("iconPrev48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 48px OK!"); else CoreConfig.logger.debug("iconPrev 48px NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/prev48w.png");
        themeResImg.put("iconPrev48w", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 48px wide OK!"); else CoreConfig.logger.debug("iconPrev 48px wide NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/prev48t.png");
        themeResImg.put("iconPrev48t", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconPrev 48px thin OK!"); else CoreConfig.logger.debug("iconPrev 48px thin NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/next24.png");
        themeResImg.put("iconNext24", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconNext 24px OK!"); else CoreConfig.logger.debug("iconNext 24px NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/next24w.png");
        themeResImg.put("iconNext24w", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconNext 24px wide OK!"); else CoreConfig.logger.debug("iconNext 24px wide NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/next32.png");
        themeResImg.put("iconNext32", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconNext 32px OK!"); else CoreConfig.logger.debug("iconNext 32px NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/next48.png");
        themeResImg.put("iconNext48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconNext 48px OK!"); else CoreConfig.logger.debug("iconNext 48px NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/next48t.png");
        themeResImg.put("iconNext48t", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconNext 48px thin OK!"); else CoreConfig.logger.debug("iconNext 48px thin NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/exit48.png");
        themeResImg.put("iconExit48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconExit 48px OK!"); else CoreConfig.logger.debug("iconExit 48px  NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/client48.png");
        themeResImg.put("iconClient48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconClient 48px OK!"); else CoreConfig.logger.debug("iconClient 48px  NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/mgr_customers.png");
        themeResImg.put("iconMgrCustomers", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconMgrCustomers OK!"); else CoreConfig.logger.debug("iconMgrCustomers NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/mgr_users.png");
        themeResImg.put("iconMgrUsers", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconMgrUsers OK!"); else CoreConfig.logger.debug("iconMgrUsers NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/mgr_stocks.png");
        themeResImg.put("iconMgrStocks", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("iconMgrStocks OK!"); else CoreConfig.logger.debug("iconMgrStocks NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_PEND_TICKET_48.png");
        themeResImg.put("icon_PEND_TICKET_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_PEND_TICKET_48 OK!"); else CoreConfig.logger.debug("icon_PEND_TICKET_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_RECALL_TICKET_48.png");
        themeResImg.put("icon_RECALL_TICKET_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_RECALL_TICKET_48 OK!"); else CoreConfig.logger.debug("icon_RECALL_TICKET_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_CANCEL_TICKET_48.png");
        themeResImg.put("icon_CANCEL_TICKET_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_CANCEL_TICKET_48 OK!"); else CoreConfig.logger.debug("icon_CANCEL_TICKET_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_SELECT_CLIENT_48.png");
        themeResImg.put("icon_SELECT_CLIENT_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_SELECT_CLIENT_48 OK!"); else CoreConfig.logger.debug("SELECT_CLIENT NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_SELECT_GIFT_48.png");
        themeResImg.put("icon_SELECT_GIFT_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_SELECT_GIFT_48 OK!"); else CoreConfig.logger.debug("icon_SELECT_GIFT_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_GO_TO_BACKOFFICE_48.png");
        themeResImg.put("icon_GO_TO_BACKOFFICE_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_GO_TO_BACKOFFICE_48 OK!"); else CoreConfig.logger.debug("icon_GO_TO_BACKOFFICE_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_OPEN_CASHDRAWER_48.png");
        themeResImg.put("icon_OPEN_CASHDRAWER_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_OPEN_CASHDRAWER_48 OK!"); else CoreConfig.logger.debug("icon_OPEN_CASHDRAWER_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_LOCK_POS_48.png");
        themeResImg.put("icon_LOCK_POS_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_LOCK_POS_48 OK!"); else CoreConfig.logger.debug("icon_LOCK_POS_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_CASH_TAKE_48.png");
        themeResImg.put("icon_CASH_TAKE_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_CASH_TAKE_48 OK!"); else CoreConfig.logger.debug("icon_CASH_TAKE_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_CANCEL_PRODUCT_48.png");
        themeResImg.put("icon_CANCEL_PRODUCT_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_CANCEL_PRODUCT_48 OK!"); else CoreConfig.logger.debug("icon_CANCEL_PRODUCT_48 NULL!");
        }
        img = getRessourceImg(resFile, "/ressources/icons/48/act_OPEN_HELP_48.png");
        themeResImg.put("icon_OPEN_HELP_48", img);
        if (logger.isDebugEnabled()) {
            if (img != null) CoreConfig.logger.debug("icon_OPEN_HELP_48 OK!"); else CoreConfig.logger.debug("icon_OPEN_HELP_48 NULL!");
        }
        sndUrl = getRessourceSnd(resFile, "/ressources/sounds/bip.wav");
        themeResUrl.put("sndBip", sndUrl);
        if (logger.isDebugEnabled()) {
            if (sndUrl != null) CoreConfig.logger.debug("sound bip.wav OK!"); else CoreConfig.logger.debug("sound bip.wav NOK!");
        }
    }

    public static Integer[] getAvailableCoinImg() {
        return availableCoinImg;
    }

    public static Integer[] getAvailableBillImg() {
        return availableBillImg;
    }

    public void loadCurrencyRessources() {
        String resFile, coinsOrBills, str;
        ImageIcon img = new ImageIcon();
        String[] workArr;
        URL url;
        int i;
        moneyResFile = "default.jar";
        resFile = System.getProperty("user.dir") + "/conf/currencies/" + moneyResFile;
        moneyResImg.clear();
        availableBillImg = null;
        availableCoinImg = null;
        coinsOrBills = "";
        try {
            Properties props = new Properties();
            url = new URL("jar:file:" + resFile + "!/description");
            if (url != null) {
                try {
                    props.load(url.openStream());
                    if (props.getProperty("COINS") != null) coinsOrBills = (props.getProperty("COINS"));
                    workArr = coinsOrBills.split(",");
                    availableCoinImg = new Integer[workArr.length];
                    for (i = 0; i < workArr.length; i++) {
                        str = workArr[i].trim();
                        availableCoinImg[i] = Integer.parseInt(str);
                        img = getRessourceImg(resFile, "/ressources/icons/coin." + str + ".png");
                        moneyResImg.put("iconCoin" + str, img);
                        if (logger.isDebugEnabled()) {
                            if (img != null) CoreConfig.logger.debug("iconCoin" + str + " OK!"); else CoreConfig.logger.debug("iconCoin" + str + "  NULL!");
                        }
                    }
                    if (props.getProperty("BILLS") != null) coinsOrBills = (props.getProperty("BILLS"));
                    workArr = coinsOrBills.split(",");
                    availableBillImg = new Integer[workArr.length];
                    for (i = 0; i < workArr.length; i++) {
                        str = workArr[i].trim();
                        availableBillImg[i] = Integer.parseInt(str);
                        img = getRessourceImg(resFile, "/ressources/icons/bill." + str + ".png");
                        moneyResImg.put("iconBill" + str, img);
                        if (logger.isDebugEnabled()) {
                            if (img != null) CoreConfig.logger.debug("iconBill" + str + " OK!"); else CoreConfig.logger.debug("iconBill" + str + "  NULL!");
                        }
                    }
                } catch (IOException ex) {
                    CoreConfig.logger.error("error whille loading icons for currency!\n" + ex);
                }
            }
        } catch (MalformedURLException ex) {
            CoreConfig.logger.error("error whille loading icons for currency!\n" + ex);
        }
    }

    public void loadThemeStyle() {
        String resFile;
        URL url;
        resFile = System.getProperty("user.dir") + "/conf/themes/" + themeResFile;
        try {
            Properties props = new Properties();
            url = new URL("jar:file:" + resFile + "!/ressources/style");
            if (url != null) {
                try {
                    props.load(url.openStream());
                    if (props.getProperty("BG_TICKET_STATUS_INIT") != null) {
                        themeResStyle.put("BG_TICKET_STATUS_INIT", props.getProperty("BG_TICKET_STATUS_INIT"));
                    } else {
                        themeResStyle.put("BG_TICKET_STATUS_INIT", "#FF0000");
                    }
                    if (props.getProperty("BG_TICKET_STATUS_OPENED") != null) {
                        themeResStyle.put("BG_TICKET_STATUS_OPENED", props.getProperty("BG_TICKET_STATUS_OPENED"));
                    } else {
                        themeResStyle.put("BG_TICKET_STATUS_OPENED", "#FF0000");
                    }
                    if (props.getProperty("BG_TICKET_STATUS_CLOSED") != null) {
                        themeResStyle.put("BG_TICKET_STATUS_CLOSED", props.getProperty("BG_TICKET_STATUS_CLOSED"));
                    } else {
                        themeResStyle.put("BG_TICKET_STATUS_CLOSED", "#FF0000");
                    }
                    if (props.getProperty("BG_TICKET_STATUS_PENDING") != null) {
                        themeResStyle.put("BG_TICKET_STATUS_PENDING", props.getProperty("BG_TICKET_STATUS_PENDING"));
                    } else {
                        themeResStyle.put("BG_TICKET_STATUS_PENDING", "#FF0000");
                    }
                    if (props.getProperty("BG_TICKET_STATUS_CANCELLED") != null) {
                        themeResStyle.put("BG_TICKET_STATUS_CANCELLED", props.getProperty("BG_TICKET_STATUS_CANCELLED"));
                    } else {
                        themeResStyle.put("BG_TICKET_STATUS_CANCELLED", "#FF0000");
                    }
                } catch (IOException ex) {
                    CoreConfig.logger.error("error whille loading theme style!\n" + ex);
                }
            } else {
                CoreConfig.logger.error("unable to load ressource file for style : " + resFile);
            }
        } catch (MalformedURLException ex) {
            CoreConfig.logger.error("error whille loading theme style!\n" + ex);
        }
    }

    public static Map<String, ImageIcon> getThemeRessources() {
        return themeResImg;
    }

    public static Map<String, ImageIcon> getMoneyImg() {
        return moneyResImg;
    }

    public static Map<String, ImageIcon> getThemeRessourcesImg() {
        return themeResImg;
    }

    public static Map<String, URL> getThemeRessourcesUrl() {
        return themeResUrl;
    }

    public static Map<String, String> getThemeRessourcesStyle() {
        return themeResStyle;
    }

    public static ImageIcon getRessourceImg(String resFile, String resData) {
        String strUrl;
        strUrl = "jar:file:" + resFile.replace('\\', '/') + "!" + resData;
        try {
            URL url = new URL(strUrl);
            if (url != null) {
                ImageIcon img = new ImageIcon(url);
                if (img != null) {
                    if (logger.isDebugEnabled()) logger.debug("Ressource loaded with success from " + strUrl);
                    return img;
                } else {
                    logger.error("Error while loading ressource from " + strUrl + " > ImageIcon null");
                    return null;
                }
            } else {
                logger.error("Error while loading ressource from " + strUrl + " > URL null");
                return null;
            }
        } catch (MalformedURLException ex) {
            logger.error("Error while loading ressource from " + strUrl + "\n" + ex);
            return null;
        }
    }

    public static URL getRessourceSnd(String resFile, String resData) {
        String strUrl;
        strUrl = "jar:file:" + resFile.replace('\\', '/') + "!" + resData;
        if (logger.isDebugEnabled()) logger.debug("Ressource for sound => " + strUrl);
        try {
            URL url = new URL(strUrl);
            if (url != null) {
                if (logger.isDebugEnabled()) logger.debug("Ressource loaded with success from " + strUrl);
                return url;
            } else {
                logger.error("Error while loading ressource from " + strUrl + " > URL null");
                return null;
            }
        } catch (MalformedURLException ex) {
            logger.error("Error while loading ressource from " + strUrl + "\n" + ex);
            return null;
        }
    }

    public void loadDefaultConfValues() {
        String cxStr;
        idPos = 1;
        drvClass = "org.apache.derby.jdbc.EmbeddedDriver";
        dialect = "org.hibernate.dialect.DerbyDialect";
        dbUser = "kafepos";
        dbPass = "kafepos";
        dbType = "javadb";
        dbPort = String.valueOf(DEFAULT_JAVADB_PORT);
        dbName = "pos";
        dbHost = "Embedded";
        dbUrl = "jdbc:derby:javadb/pos;create=true";
        dbLabel = "Java Embedded Database";
        cxStr = "java -Dij.database=" + dbUrl + " -Dij.user=kafepos -Dij.password=kafepos -jar lib/derbyrun.jar ij conf/db/javadb_struct.sql";
        dbDefCmd.add(cxStr);
        String[] procCmd;
        for (int i = 0; i < dbDefCmd.size(); i++) {
            try {
                procCmd = CoreConfig.getOsShellCmd((String) dbDefCmd.get(i));
                CoreConfig.logger.info("Create database/schema command : " + procCmd[2]);
                Launcher launcher = new Launcher();
                launcher.exec(procCmd);
            } catch (Exception e) {
                CoreConfig.logger.fatal("ERROR creating database : " + e);
            }
        }
    }

    public String getLastErrorMsgs() {
        return lastErrorMsgs;
    }

    public int getLoadingResult() {
        return this.loadingRes;
    }

    public void setProdImgBgRessources(Image[] resArray) {
        prodImgBg = resArray;
    }

    public void createXMLConfFile() {
        logger.log(Level.INFO, "INIT : création du fichier de config");
        if (dbType.equals("javadb")) {
            dbLabel = "Java Embedded Database";
            drvClass = "org.apache.derby.jdbc.EmbeddedDriver";
        } else if (dbType.equals("mysql")) {
            dbLabel = "MySQL Database Server";
            drvClass = "com.mysql.jdbc.Driver";
        } else if (dbType.equals("access")) {
            dbLabel = "Microsoft Access Database";
            drvClass = "com.mysql.jdbc.MSAccess";
        } else {
            dbLabel = "Java Embedded Database";
            drvClass = "org.apache.derby.jdbc.EmbeddedDriver";
        }
        logger.log(Level.INFO, "Type db utilisé " + dbType);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder constructeur = factory.newDocumentBuilder();
            Document document = constructeur.newDocument();
            document.setXmlVersion("1.0");
            document.setXmlStandalone(true);
            Element root = document.createElement("posconfig");
            root.appendChild(document.createComment("Fichier de configuration application KafePOS"));
            Element pos = document.createElement("pos");
            pos.setAttribute("id", String.valueOf(idPos));
            root.appendChild(pos);
            Element database = document.createElement("database");
            database.setAttribute("id", dbType);
            database.setAttribute("label", dbLabel);
            root.appendChild(database);
            Element connexion = document.createElement("connexion");
            connexion.setAttribute("host", dbHost);
            connexion.setAttribute("dbname", dbName);
            connexion.setAttribute("user", dbUser);
            connexion.setAttribute("pass", dbPass);
            connexion.setAttribute("port", dbPort);
            database.appendChild(connexion);
            Element driver = document.createElement("driver");
            driver.setAttribute("class", drvClass);
            database.appendChild(driver);
            Element folders = document.createElement("folders");
            root.appendChild(folders);
            Element reports = document.createElement("reports");
            reports.setAttribute("url", "reports");
            folders.appendChild(reports);
            Element export = document.createElement("export");
            export.setAttribute("url", "export");
            folders.appendChild(export);
            document.appendChild(root);
            Source source = new DOMSource(document);
            new File(CONF_PATH).mkdir();
            Result result = new StreamResult(CONF_FILE_PATH);
            TransformerFactory tfact = TransformerFactory.newInstance();
            Transformer transformer = tfact.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDOMConfigFile() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder construct = factory.newDocumentBuilder();
            Document doc = construct.parse(CONF_FILE_PATH);
            String idDb = new String(getValueFromXPath(doc, "/posconfig/database", "id"));
            idPos = Short.parseShort(getValueFromXPath(doc, "/posconfig/pos", "id"));
            dbType = getValueFromXPath(doc, "/posconfig/database", "id");
            drvClass = getValueFromXPath(doc, "/posconfig/database/driver", "class");
            dbHost = getValueFromXPath(doc, "/posconfig/database/connexion", "host");
            dbPort = getValueFromXPath(doc, "/posconfig/database/connexion", "port");
            dbName = getValueFromXPath(doc, "/posconfig/database/connexion", "dbname");
            dbUser = getValueFromXPath(doc, "/posconfig/database/connexion", "user");
            dbPass = getValueFromXPath(doc, "/posconfig/database/connexion", "pass");
            urlReports = getValueFromXPath(doc, "/posconfig/folders/reports", "url");
            if (idDb.equals("javadb")) {
                dialect = "org.hibernate.dialect.DerbyDialect";
                dbUrl = "jdbc:derby:javadb/" + dbName;
                logger.log(Level.INFO, "*** USE JAVADB  : " + dbUrl);
            } else if (idDb.equals("mysql")) {
                dialect = "org.hibernate.dialect.MySQLDialect";
                dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
                logger.log(Level.INFO, "*** USE MYSQL : " + dbUrl);
            } else {
                dialect = "org.hibernate.dialect.DerbyDialect";
                dbUrl = "jdbc:derby:javadb/" + dbName;
                logger.log(Level.INFO, "*** USE JAVADB  : " + dbUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.ERROR, e);
        }
    }

    public String getValueFromXPath(String docPath, String xPathExp, String xValue) {
        try {
            DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
            DocumentBuilder construct = fabrique.newDocumentBuilder();
            Document doc = construct.parse(docPath);
            NodeList list = (NodeList) evaluerDOM(doc, xPathExp, XPathConstants.NODESET);
            String value = list.item(0).getAttributes().getNamedItem(xValue).getNodeValue();
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.ERROR, xPathExp + " - " + xValue + " -> NOK!");
            return "";
        }
    }

    public String getValueFromXPath(Document doc, String xPathExp, String xValue) {
        try {
            NodeList list = (NodeList) evaluerDOM(doc, xPathExp, XPathConstants.NODESET);
            String value = list.item(0).getAttributes().getNamedItem(xValue).getNodeValue();
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.ERROR, xPathExp + " - " + xValue + " -> NOK!");
            return "";
        }
    }

    public Object evaluerDOM(Document document, String expression, QName retour) {
        try {
            XPathFactory fabrique = XPathFactory.newInstance();
            XPath xpath = fabrique.newXPath();
            XPathExpression exp = xpath.compile(expression);
            Object resultat = exp.evaluate(document, retour);
            return resultat;
        } catch (XPathExpressionException xpee) {
            xpee.printStackTrace();
        }
        return null;
    }

    public static String getInitCmdProp(String key) throws FileNotFoundException, IOException {
        if (initCmdProp.size() == 0) {
            initCmdProp.load(new FileInputStream(new File(INIT_CMD_FILE)));
        }
        return (initCmdProp.getProperty(key, ""));
    }

    public static String[] getOsShellCmd(String cmd) {
        return new String[] { shellCmd, shellOpt, cmd };
    }

    public static String getDialect() {
        return dialect;
    }

    public static Short getIdPos() {
        return idPos;
    }

    public static String getDrvClass() {
        return drvClass;
    }

    public static String getDbType() {
        return dbType;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static String getDbPass(String key) {
        dbPass = Crypto.decryptInBytesWithAES(dbPass, key);
        return dbPass;
    }

    public static String getDbPass() {
        return dbPass;
    }

    public static String getDbHost() {
        return dbHost;
    }

    public static String getDbName() {
        return dbName;
    }

    public static String getDbPort() {
        return dbPort;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDefaultDataSampleUrl() {
        return defaultDataSampleUrl;
    }

    public static String getUrlReports() {
        return urlReports;
    }

    public static String getReceptPrinterName() {
        AppProperty receptPrinterProp = ConfigMgr.getProperty("devReceptPrinter");
        String name, protocol;
        if (receptPrinterProp != null) {
            if (!receptPrinterProp.getValue().isEmpty()) {
                return receptPrinterProp.getValue().split(";", 2)[0];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getReceptPrinterLayout() {
        AppProperty prop = ConfigMgr.getProperty("receptPrinterLayout");
        if (prop != null) {
            if (!prop.getValue().isEmpty()) {
                return prop.getValue().split(";", 2)[0];
            } else {
                return "default";
            }
        } else {
            return "default";
        }
    }

    public void setDbUser(String user) {
        this.dbUser = user;
    }

    public void setDrvClass(String drvClass) {
        this.drvClass = drvClass;
    }

    public void setDbPass(String passwd) {
        this.dbPass = passwd;
    }

    public void setDbType(String type) {
        this.dbType = type;
    }

    public void setDbHost(String host) {
        this.dbHost = host;
    }

    public void setDbPort(String port) {
        this.dbPort = port;
    }

    public void setDbName(String name) {
        this.dbName = name;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    /**
     * Getter for property dirReport.
     * @return Value of property dirReport.
     */
    public static String getDirReport() {
        AppProperty repDirProp;
        dirReport = APP_PATH + "/reports/";
        repDirProp = ConfigMgr.getProperty("reportUserDirectory");
        if (repDirProp != null) {
            if (new File(repDirProp.getValue()).exists()) {
                dirReport = repDirProp.getValue();
            }
        }
        return dirReport;
    }

    /**
     * Setter for property dirReport.
     * @param dirReport New value of property dirReport.
     */
    public void setDirReport(String dirReport) {
        this.dirReport = dirReport;
    }

    /**
     * Holds value of property jPosDevReceptPrinter.
     */
    public static String getJPosDevReceptPrinter() {
        return devReceptPrinterName;
    }

    public void setJPosDevReceptPrinter(String jPosDevReceptPrinter) {
        this.devReceptPrinterName = jPosDevReceptPrinter;
    }

    /**
     * Holds value of property jPosDevCheckPrinter.
     */
    public static String getJPosDevCheckPrinter() {
        return jPosDevCheckPrinter;
    }

    public void setJPosDevCheckPrinter(String jPosDevCheckPrinter) {
        this.jPosDevCheckPrinter = jPosDevCheckPrinter;
    }

    /**
     * Holds value of property jPosDevStandardPrinter.
     */
    public static String getJPosDevStandardPrinter() {
        return jPosDevStandardPrinter;
    }

    public void setJPosDevStandardPrinter(String jPosDevStandardPrinter) {
        this.jPosDevStandardPrinter = jPosDevStandardPrinter;
    }

    /**
     * Holds value of property jPosDevSmartCard.
     */
    public static String getJPosDevSmartCard() {
        return jPosDevSmartCard;
    }

    public void setJPosDevSmartCard(String jPosDevSmartCard) {
        this.jPosDevSmartCard = jPosDevSmartCard;
    }

    /**
     * Holds value of property jPosDevKeyboard.
     */
    public static String getJPosDevKeyboard() {
        return jPosDevKeyboard;
    }

    public void setJPosDevKeyboard(String jPosDevKeyboard) {
        this.jPosDevKeyboard = jPosDevKeyboard;
    }

    /**
     * Holds value of property jPosDevCashDrawer.
     */
    public static String getJPosDevCashDrawer() {
        return jPosDevCashDrawer;
    }

    public void setJPosDevCashDrawer(String jPosDevCashDrawer) {
        this.jPosDevCashDrawer = jPosDevCashDrawer;
    }

    /**
     * Holds value of property jPosDevLineDisplay.
     */
    public static String getJPosDevLineDisplay() {
        return jPosDevLineDisplay;
    }

    public void setJPosDevLineDisplay(String jPosDevLineDisplay) {
        this.jPosDevLineDisplay = jPosDevLineDisplay;
    }

    /**
     * Holds value of property jPosReceptPrinterNbChar.
     */
    public static String getReceptPrinterLineLen() {
        return receptPrinterLineLen;
    }

    public void setReceptPrinterLineLen(String receptPrinterLineLen) {
        this.receptPrinterLineLen = receptPrinterLineLen;
    }

    /**
     * Holds value of property jPosReceptPrinterXmlPosCompliant.
     */
    public static String getJPosReceptPrinterXmlPosCompliant() {
        return jPosReceptPrinterXmlPosCompliant;
    }

    public void setJPosReceptPrinterXmlPosCompliant(String JPosReceptPrinterXmlPosCompliant) {
        this.jPosReceptPrinterXmlPosCompliant = JPosReceptPrinterXmlPosCompliant;
    }

    /**
     * Holds value of property jPosReceptPrinterLang.
     */
    public static String getJPosReceptPrinterLang() {
        return jPosReceptPrinterLang;
    }

    public void setJPosReceptPrinterLang(String jPosReceptPrinterLang) {
        this.jPosReceptPrinterLang = jPosReceptPrinterLang;
    }

    /**
     * Holds value of property jPosReceptPrinterPort.
     */
    public static String getJPosReceptPrinterPort() {
        return jPosReceptPrinterPort;
    }

    public void setJPosReceptPrinterPort(String jPosReceptPrinterPort) {
        this.jPosReceptPrinterPort = jPosReceptPrinterPort;
    }

    /**
     * Holds value of property checkProcessDevice.
     */
    public static String getCheckProcessDevice() {
        return checkProcessDevice;
    }

    public void setCheckProcessDevice(String checkProcessDevice) {
        this.checkProcessDevice = checkProcessDevice;
    }

    /**
     * Holds value of property smartCardProtocol.
     */
    public static String getSmartCardProtocol() {
        return smartCardProtocol;
    }

    public void setSmartCardProtocol(String smartCardProtocol) {
        this.smartCardProtocol = smartCardProtocol;
    }

    /**
     * Holds value of property smartCardPort.
     */
    public static AppPort getPaymtTermPort() {
        smartCardPort = ConfigMgr.getPort(Short.parseShort(smartCardPortID));
        return smartCardPort;
    }

    /**
     * Holds value of property smartCardPortOpenTime.
     */
    public static String getSmartCardPortOpenTime() {
        return smartCardPortOpenTime;
    }

    public void setSmartCardPortOpenTime(String smartCardPortOpenTime) {
        this.smartCardPortOpenTime = smartCardPortOpenTime;
    }

    /**
     * Holds value of property smartCardPortBufferSize.
     */
    public static String getSmartCardPortBufferSize() {
        return smartCardPortBufferSize;
    }

    public void setSmartCardPortBufferSize(String smartCardPortBufferSize) {
        this.smartCardPortBufferSize = smartCardPortBufferSize;
    }

    /**
     * Holds value of property smartCardPortTimeOut.
     */
    public static String getSmartCardPortTimeOut() {
        return smartCardPortTimeOut;
    }

    public void setSmartCardPortTimeOut(String smartCardPortTimeOut) {
        this.smartCardPortTimeOut = smartCardPortTimeOut;
    }

    /**
     * Holds value of property smartCardPortBauds.
     */
    public static String getSmartCardPortBauds() {
        return smartCardPortBauds;
    }

    public void setSmartCardPortBauds(String smartCardPortBauds) {
        this.smartCardPortBauds = smartCardPortBauds;
    }

    /**
     * Holds value of property smartCardPortDataBits.
     */
    public static String getSmartCardPortDataBits() {
        return smartCardPortDataBits;
    }

    public void setSmartCardPortDataBits(String smartCardPortDataBits) {
        this.smartCardPortDataBits = smartCardPortDataBits;
    }

    /**
     * Holds value of property smartCardPortStopBit.
     */
    public static String getSmartCardPortStopBit() {
        return smartCardPortStopBit;
    }

    public void setSmartCardPortStopBit(String smartCardPortStopBit) {
        this.smartCardPortStopBit = smartCardPortStopBit;
    }

    /**
     * Holds value of property smartCardPortParity.
     */
    public static String getSmartCardPortParity() {
        return smartCardPortParity;
    }

    public void setSmartCardPortParity(String smartCardPortParity) {
        this.smartCardPortParity = smartCardPortParity;
    }

    /**
     * Holds value of property behaviorTva.
     */
    public static String getBehaviorTva() {
        return behaviorTva;
    }

    public void setBehaviorTva(String behaviorTva) {
        this.behaviorTva = behaviorTva;
    }

    /**
     * Holds value of property behaviorTvaRecept.
     */
    public static String getBehaviorTvaRecept() {
        return behaviorTvaRecept;
    }

    public void setBehaviorTvaRecept(String behaviorTvaRecept) {
        this.behaviorTvaRecept = behaviorTvaRecept;
    }

    public static Boolean isForcedToCheckData() {
        return forcedToCheckData;
    }

    public static void setForcedToCheckData(Boolean value) {
        forcedToCheckData = value;
    }

    public TreeMap getMapLookNFeel() {
        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < info.length; i++) {
            String nomLF = info[i].getName();
            String nomClasse = info[i].getClassName();
            mapLookNFeel.put(nomLF, nomClasse);
        }
        return mapLookNFeel;
    }

    public static List<Locale> getSupportedLocales() {
        List<Locale> langs = new ArrayList<Locale>();
        langs.add(Locale.getDefault());
        langs.add(Locale.FRANCE);
        langs.add(Locale.FRENCH);
        langs.add(Locale.ENGLISH);
        return langs;
    }

    public static String UIScreenRes() {
        return UIScreenRes;
    }
}
