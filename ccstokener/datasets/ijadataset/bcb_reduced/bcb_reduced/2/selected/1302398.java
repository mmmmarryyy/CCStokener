package edu.psu.its.lionshare.gui;

import com.limegroup.gnutella.ErrorService;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Enumeration;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.StartupSettings;
import com.limegroup.gnutella.UPnPManager;
import com.limegroup.gnutella.util.SystemUtils;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.bugs.BugManager;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.settings.StartupSettings;
import com.limegroup.gnutella.util.SystemUtils;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.gui.init.*;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.MessageHandler;
import com.limegroup.gnutella.gui.MessageResourceHandler;
import com.limegroup.gnutella.gui.ErrorHandler;
import com.limegroup.gnutella.gui.GURLHandler;
import edu.psu.its.lionshare.gui.chat.ConnectionManager;
import edu.psu.its.lionshare.util.ZipUtility;
import edu.psu.its.lionshare.database.PeerserverHost;
import edu.psu.its.lionshare.database.VirtualDirectorySelect;
import edu.psu.its.lionshare.database.DatabaseInitializer;
import edu.psu.its.lionshare.database.ChatHost;
import edu.psu.its.lionshare.database.ChatHostSelect;
import edu.psu.its.lionshare.database.UserData;
import edu.psu.its.lionshare.database.UserDataSelect;
import edu.psu.its.lionshare.database.VirtualDirectory;
import edu.psu.its.lionshare.gui.chat.ChatMediator;
import edu.psu.its.lionshare.gui.security.LionShareAuthenticationDialog;
import edu.psu.its.lionshare.gui.security.AuthenticateUserDialog;
import edu.psu.its.lionshare.search.local.SearchIndex;
import edu.psu.its.lionshare.security.DefaultSecurityManager;
import edu.psu.its.lionshare.security.SecurityManagerListener;
import edu.psu.its.lionshare.settings.LionShareApplicationSettings;
import edu.psu.its.lionshare.userprofile.LDAPAttributeExtractor;
import edu.psu.its.lionshare.userprofile.UserProfileConstants;
import edu.psu.its.lionshare.util.RunnableProcessor;
import edu.psu.its.lionshare.share.gnutella.LionShareRouterService;
import java.awt.Frame;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import java.lang.reflect.*;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * LionShare Project - 2005
 *
 * Initializer class for LionShare related items to load on startup.
 *
 * @author Todd C. Beehler
 * Created on Jan 27, 2005
 */
public class LionShareInitializer {

    private static final Log LOG = LogFactory.getLog(LionShareInitializer.class);

    public static final String PERCENT_20 = "%20";

    public static final String SPACE = " ";

    private static volatile boolean isStartup = false;

    public static void main(String args[]) {
        try {
            initialize(args, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void initialize(String args[], Frame awt_splash) throws Throwable {
        ErrorService.setErrorCallback(new ErrorHandler());
        com.limegroup.gnutella.MessageService.setCallback(new MessageHandler());
        com.limegroup.gnutella.MessageResourceService.setCallback(new MessageResourceHandler());
        if (CommonUtils.isMacOSX()) {
            GURLHandler.getInstance().register();
            CocoaApplicationEventHandler.instance().register();
            SystemUtils.setOpenFileLimit(1024);
        }
        if (CommonUtils.isWindows()) {
            try {
                new java.io.File("donotremove.htm").createNewFile();
            } catch (Exception exp) {
                LOG.debug("Could not create empty file donotremove.htm");
            }
        }
        if (StartupSettings.RUN_ON_STARTUP.getValue()) {
            Thread.yield();
        }
        if (args.length >= 1 && "-startup".equals(args[0])) {
            isStartup = true;
        }
        if (isStartup) {
            if (!StartupSettings.RUN_ON_STARTUP.getValue()) System.exit(0);
        }
        if (!StartupSettings.ALLOW_MULTIPLE_INSTANCES.getValue()) {
            ExternalControl.checkForActiveLimeWire();
        }
        LionShareInitializer.setSystemProperties();
        LionShareInitializer.setOSXSystemProperties();
        ResourceManager.instance();
        SplashWindow splash = null;
        if (!isStartup) splash = new SplashWindow();
        if (awt_splash != null) awt_splash.dispose();
        SplashWindow.setStatusText(LionShareGUIMediator.getStringResource("SPLASH_STATUS_HTML_ENGINE"));
        BasicHTML.createHTMLView(new JLabel(), "<html>.</html>");
        BugManager.instance();
        SetupManager setupManager = new SetupManager();
        if (!setupManager.shouldShowFirewallWindow()) {
            if (CommonUtils.isJava14OrLater() && !ConnectionSettings.DISABLE_UPNP.getValue()) {
                LOG.trace("START UPnPManager");
                UPnPManager.instance().start();
                LOG.trace("STOP UPnPManager");
            }
        }
        SplashWindow.setStatusText(LionShareGUIMediator.getStringResource("SPLASH_STATUS_INTERFACE"));
        LionShareInitializer.init();
        SplashWindow.setStatusText(LionShareGUIMediator.getStringResource("SPLASH_STATUS_CORE_COMPONENTS"));
        setupManager.createIfNeeded();
        SaveDirectoryHandler.handleSaveDirectory();
        ActivityCallback ac = new LionShareActivityCallback();
        LionShareRouterService routerService = new LionShareRouterService(ac);
        LionShareGUIMediator mediator = LionShareGUIMediator.getInstance();
        mediator.setRouterService(routerService);
        NotifyUserProxy notifyProxy = NotifyUserProxy.instance();
        notifyProxy.hideNotify();
        LionShareAuthenticationDialog lsAuthDialog = new LionShareAuthenticationDialog();
        if (splash != null) splash.dispose();
        LionShareGUIMediator.allowVisibility();
        if (!isStartup) {
            System.out.println("Setting app visible");
            LionShareGUIMediator.setAppVisible(true);
        } else {
            LionShareGUIMediator.startupHidden();
        }
        LionShareGUIMediator.setSplashScreenString(LionShareGUIMediator.getStringResource("SPLASH_STATUS_ICONS"));
        IconManager.instance();
        LionShareGUIMediator.setSplashScreenString(LionShareGUIMediator.getStringResource("SPLASH_STATUS_I18N"));
        I18NConvert.instance();
        routerService.start();
        mediator.startTimer();
        LionShareInitializer.loadChatProperties();
        LionShareGUIMediator.getInstance().loadFinished();
        if (CommonUtils.isMacOSX()) UIManager.put("ProgressBar.repaintInterval", new Integer(500));
    }

    static void setSystemProperties() {
        try {
            Method setPropertyMethod = System.class.getDeclaredMethod("setProperty", new Class[] { String.class, String.class });
            setPropertyMethod.invoke(null, new String[] { "http.agent", CommonUtils.getHttpServer() });
        } catch (IllegalAccessException e1) {
        } catch (InvocationTargetException e1) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    static void setOSXSystemProperties() {
        if (!CommonUtils.isMacOSX()) return;
        try {
            Method setPropertyMethod = System.class.getDeclaredMethod("setProperty", new Class[] { String.class, String.class });
            if (CommonUtils.isJava14OrLater()) {
                setPropertyMethod.invoke(null, new String[] { "apple.laf.useScreenMenuBar", "true" });
            } else {
                setPropertyMethod.invoke(null, new String[] { "com.apple.macos.useScreenMenuBar", "true" });
                setPropertyMethod.invoke(null, new String[] { "com.apple.mrj.application.apple.menu.about.name", "LionShare" });
                setPropertyMethod.invoke(null, new String[] { "com.apple.macos.use-file-dialog-packages", "true" });
            }
        } catch (IllegalAccessException e1) {
        } catch (InvocationTargetException e1) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    static void setStartup() {
        isStartup = true;
    }

    public static void init() {
        try {
            String sPath = Thread.currentThread().getContextClassLoader().getResource("jaas_client.conf").toString();
            Pattern p0 = Pattern.compile(PERCENT_20);
            Matcher m0 = p0.matcher(sPath);
            sPath = m0.replaceAll(SPACE);
            DefaultSecurityManager.getInstance().addListener(new UserProfileSecurityManagerListener());
            LOG.debug("sPath is: " + sPath);
            System.setProperty("java.security.auth.login.config", sPath);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "LionShare");
            System.setProperty("derby.storage.fileSyncTransactionLog", "true");
            loadDefaultEclConfiguration();
            loadDefaultMetadataConfiguration();
            loadDefaultSecurityProperties();
            DatabaseInitializer.init(true);
            try {
                DatabaseInitializer.checkDataBaseConnection();
                loadDefaultChatServer();
                loadChatConnectionManager();
            } catch (Exception e) {
                LOG.trace("", e);
                JOptionPane.showMessageDialog(GUIMediator.getAppFrame(), "<html>LionShare was unable to obtain a connection to the " + "embedded database <br><br> Do you have another copy of " + "lionshare running on your system? <br> " + e.getMessage() + e + "</html>", "No Database Connection", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.trace("", e);
        }
        DefaultSecurityManager.getInstance().setAuthenticateUserCallback(new AuthenticateUserDialog());
    }

    public static void loadDefaultSecurityProperties() {
    }

    /**
   * Loads chat related properties from the LionShareApplicationSettings.
   */
    public static void loadChatProperties() {
        boolean bIsVisible = LionShareApplicationSettings.IS_VISIBLE_BUDDY_LIST.getValue();
        ChatMediator.getInstance().getBuddyListFrame().setVisible(bIsVisible);
    }

    public static void loadChatConnectionManager() {
        ConnectionManager.getInstance().setVisible(false);
    }

    public static void loadDefaultChatServer() {
        try {
            List list = ChatHostSelect.getChatHosts();
            if (list != null && list.size() < 1) {
                String host = System.getProperty("lionshare.default.chat.server.host");
                String port = System.getProperty("lionshare.default.chat.server.port");
                String name = System.getProperty("lionshare.default.chat.server.name");
                if (host != null && port != null) {
                    if (name == null) name = "Default Chat";
                    ChatHost chost = new ChatHost(name, host, new Integer(port).intValue());
                    chost.setAutoLogin(true);
                    Session session = ChatHostSelect.getSession();
                    ChatHostSelect.insert(chost, session);
                    ChatHostSelect.closeSession(session);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadDefaultEclConfiguration() throws Exception {
        java.io.File conf_dir = new java.io.File(CommonUtils.getUserSettingsDir().getAbsolutePath() + java.io.File.separator + "conf");
        java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("ecl_conf.jar");
        if (!conf_dir.exists()) {
            try {
                ZipUtility.unZipURLToDirectory(url, CommonUtils.getUserSettingsDir().getAbsolutePath());
                LionShareApplicationSettings.ECL_CONF_LAST_MODIFIED.setValue(url.openConnection().getLastModified());
            } catch (Exception e) {
            }
        } else {
            long nLastModified = url.openConnection().getLastModified();
            if (LionShareApplicationSettings.ECL_CONF_LAST_MODIFIED.getValue() != nLastModified) {
                try {
                    ZipUtility.unZipURLToDirectory(url, CommonUtils.getUserSettingsDir().getAbsolutePath());
                } catch (Exception e) {
                }
                LionShareApplicationSettings.ECL_CONF_LAST_MODIFIED.setValue(nLastModified);
            }
        }
    }

    public static void loadDefaultMetadataConfiguration() throws IOException {
        java.io.File conf_dir = new java.io.File(CommonUtils.getUserSettingsDir().getAbsolutePath());
        java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("nmd.jar");
        if (!conf_dir.exists()) {
            try {
                ZipUtility.unZipURLToDirectory(url, CommonUtils.getUserSettingsDir().getAbsolutePath());
                LionShareApplicationSettings.NMD_CONF_LAST_MODIFIED.setValue(url.openConnection().getLastModified());
            } catch (Exception e) {
            }
        } else {
            if (LionShareApplicationSettings.NMD_CONF_LAST_MODIFIED.getValue() != url.openConnection().getLastModified()) {
                try {
                    ZipUtility.unZipURLToDirectory(url, CommonUtils.getUserSettingsDir().getAbsolutePath());
                } catch (Exception e) {
                }
                LionShareApplicationSettings.NMD_CONF_LAST_MODIFIED.setValue(url.openConnection().getLastModified());
            }
        }
    }

    private static class UserProfileSecurityManagerListener implements SecurityManagerListener {

        public void fireSecurityStateChanged() {
            if (DefaultSecurityManager.getInstance().isAuthenticated()) {
                String sPrincipal = DefaultSecurityManager.getInstance().getEmail();
                LOG.debug("LionShareInitializer.fireSecurityStateChanged() - " + "sPrincipal is: " + sPrincipal);
                UserData userData = UserDataSelect.getUserByPrincipal(sPrincipal);
                LOG.debug("LionShareInitializer.fireSecurityStateChanged() - " + "userData is: " + userData);
                if (userData == null) {
                    Runnable runnable = new Runnable() {

                        public void run() {
                            try {
                                LDAPAttributeExtractor extractor = LDAPAttributeExtractor.getInstance();
                                HashMap attrHash = new HashMap();
                                String sPrincipalName = DefaultSecurityManager.getInstance().getEmail();
                                String sUsername = sPrincipalName;
                                if (sPrincipalName.indexOf("@") != -1) {
                                    sUsername = sPrincipalName.substring(0, sPrincipalName.indexOf("@"));
                                }
                                try {
                                    extractor.fetchAttributes(sUsername, attrHash);
                                } catch (Exception ex) {
                                    LOG.debug("Error fetching LDAP Attributes: " + ex.getMessage());
                                }
                                if (attrHash.size() > 0) {
                                    UserData userDataNew = null;
                                    userDataNew = createUserDataObject(attrHash);
                                    if (userDataNew != null) {
                                        Session session = null;
                                        try {
                                            session = UserDataSelect.getSession();
                                            UserDataSelect.insert(userDataNew, session);
                                            UserDataSelect.closeSession(session);
                                            SearchIndex.getInstance().addUserDocument(userDataNew, SearchIndex.ALL_FILE_INDEX_PATH);
                                            LOG.debug("The user data object was added to the DB " + "and indexed into lucene.");
                                        } catch (Exception ex) {
                                            LOG.debug("UserProfileSecurityManagerListener." + "fireSecurityStateChanged() - Error inserting " + "UserData object.");
                                            LOG.debug(ex.getMessage());
                                        }
                                    }
                                }
                            } catch (Throwable t) {
                                ErrorService.error(t);
                            }
                        }
                    };
                    RunnableProcessor rp = RunnableProcessor.getInstance();
                    rp.execute(runnable);
                } else {
                    LOG.debug("The user data object already exists.");
                }
            }
        }

        private UserData createUserDataObject(HashMap attrHash) {
            String sCountryValue = null;
            String sStateValue = null;
            String sCityValue = null;
            String sCampusValue = null;
            String sOrgValue = null;
            String sOrgUnitValue = null;
            String sNameValue = null;
            String sMailValue = null;
            String sURLValue = null;
            String sPrincipalValue = null;
            String sPrimaryAffValue = null;
            String sTitleValue = null;
            String sCirriculumValue = null;
            String sUserCreated = null;
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_C_VALUE)) {
                sCountryValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_C_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_ST_VALUE)) {
                sStateValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_ST_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_CITY_VALUE)) {
                sCityValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_CITY_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_L_VALUE)) {
                sCampusValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_L_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_O_VALUE)) {
                sOrgValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_O_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_OU_VALUE)) {
                sOrgUnitValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_OU_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_CN_VALUE)) {
                sNameValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_CN_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_MAIL_VALUE)) {
                sMailValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_MAIL_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_URL_VALUE)) {
                sURLValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_URL_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_PRINCIPAL_VALUE)) {
                sPrincipalValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_PRINCIPAL_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_PRIMARYAFF_VALUE)) {
                sPrimaryAffValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_PRIMARYAFF_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_TITLE_VALUE)) {
                sTitleValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_TITLE_VALUE);
            }
            if (attrHash.containsKey(UserProfileConstants.LDAPMETA_CIRRICULUM_VALUE)) {
                sCirriculumValue = (String) attrHash.get(UserProfileConstants.LDAPMETA_CIRRICULUM_VALUE);
            }
            if (sNameValue != null && sPrincipalValue != null && sMailValue != null) {
                return new UserData(sNameValue, true, sPrincipalValue, true, sMailValue, true, sCampusValue, true, sOrgValue, true, sOrgUnitValue, true, sPrimaryAffValue, true, sTitleValue, true, sCirriculumValue, true, sURLValue, true, sCityValue, true, sStateValue, true, sCountryValue, true, sUserCreated, true);
            } else {
                LOG.debug("LionShareInitializer().createUserDataObject() - returning " + "a null UserData object.");
                return null;
            }
        }
    }
}
