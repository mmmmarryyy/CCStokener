package com.dotmarketing.servlets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.apache.commons.lang.SystemUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DotCMSInitDb;
import com.dotmarketing.db.DotHibernate;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.HostFactory;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.loggers.mbeans.Log4jConfig;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.campaigns.factories.CampaignFactory;
import com.dotmarketing.portlets.languagesmanager.factories.LanguageFactory;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.job.ShutdownHookThread;
import com.dotmarketing.scheduler.DotScheduler;
import com.dotmarketing.startup.StartupTasksExecutor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DotSpellChecker;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LuceneUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.ReleaseInfo;

public class InitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /**
     * @param permissionAPI
     *            the permissionAPI to set
     */
    public void setPermissionAPI(PermissionAPI permissionAPI) {
        this.permissionAPI = permissionAPI;
    }

    public void destroy() {
        Logger.info(this, "dotCMS shutting down");
        System.gc();
    }

    public static Date startupDate;

    /**
     * Description of the Method
     *
     * @throws DotDataException
     */
    public void init(ServletConfig config) throws ServletException {
        startupDate = new java.util.Date();
        Company company = PublicCompanyFactory.getDefaultCompany();
        TimeZone companyTimeZone = company.getTimeZone();
        TimeZone.setDefault(companyTimeZone);
        Logger.info(this, "InitServlet: Setting Default Timezone: " + companyTimeZone.getDisplayName());
        String _dbType = DbConnectionFactory.getDBType();
        String _dailect = DotHibernate.getDialect();
        String _companyId = PublicCompanyFactory.getDefaultCompanyId();
        Logger.info(this, "");
        Logger.info(this, "   Initializing dotCMS");
        Logger.info(this, "   Using database: " + _dbType);
        Logger.info(this, "   Using dialect : " + _dailect);
        Logger.info(this, "   Company Name  : " + _companyId);
        Logger.info(this, "");
        LuceneUtils.checkAndInitialiazeCurrentIndex();
        Logger.debug(this, "");
        Logger.debug(this, "InitServlet: Setting Application Context!!!!!!");
        String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
        if (velocityRootPath.startsWith("/WEB-INF")) {
            velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
        }
        new java.io.File(velocityRootPath + File.separator + "live").mkdirs();
        new java.io.File(velocityRootPath + File.separator + "working").mkdirs();
        RefreshMenus.deleteMenus();
        Logger.debug(this, "Lazy Mapping = " + Config.getBooleanProperty("LAZY_ASSET_MAPPING"));
        if (!Config.getBooleanProperty("LAZY_ASSET_MAPPING", true)) {
            PublishFactory.publishAllLiveAssets();
            PreviewFactory.mapAllWorkingAssets();
            Logger.debug(this, "Before calling PermissionAPI");
            permissionAPI.mapAllPermissions();
        }
        VirtualLinksCache.mapAllVirtualLinks();
        Language language = LanguageFactory.getDefaultLanguage();
        if (language.getId() == 0) {
            Logger.debug(this, "Creating Default Language");
            LanguageFactory.createDefaultLanguage();
        }
        if (Config.getBooleanProperty("INIT_DICTS_AT_STARTUP")) {
            Logger.info(this, "Calling Initializing SpellChecker Dictionaries Rutine");
            try {
                DotSpellChecker.initializeDicts(false, false);
            } catch (Exception e) {
                Logger.error(this, "Error encountered initializing spellchecker dicts!!", e);
            }
        } else {
            Logger.info(this, "Intialization of spell checker dictionaries at startup is off. Check INIT_DICTS_AT_STARTUP global variable to enable it.");
        }
        DotScheduler.startScheduler();
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());
        deleteFiles(new File(SystemUtils.JAVA_IO_TMPDIR));
        CampaignFactory.unlockAllCampaigns();
        if (Config.getBooleanProperty("INIT_THREAD_DOTCMS")) {
            InitThread it = new InitThread();
            it.start();
        }
        DotHibernate.closeSession();
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("org.dotcms:type=Log4J");
            Log4jConfig mbean = new Log4jConfig();
            mbs.registerMBean(mbean, name);
        } catch (MalformedObjectNameException e) {
            Logger.debug(InitServlet.class, "MalformedObjectNameException: " + e.getMessage(), e);
        } catch (InstanceAlreadyExistsException e) {
            Logger.debug(InitServlet.class, "InstanceAlreadyExistsException: " + e.getMessage(), e);
        } catch (MBeanRegistrationException e) {
            Logger.debug(InitServlet.class, "MBeanRegistrationException: " + e.getMessage(), e);
        } catch (NotCompliantMBeanException e) {
            Logger.debug(InitServlet.class, "NotCompliantMBeanException: " + e.getMessage(), e);
        } catch (NullPointerException e) {
            Logger.debug(InitServlet.class, "NullPointerException: " + e.getMessage(), e);
        }
    }

    protected void deleteFiles(java.io.File directory) {
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                ((java.io.File) files[i]).delete();
            }
        }
    }

    public static Date getStartupDate() {
        return startupDate;
    }

    /**
 *
 * @author will
 * This thread will fire and send all the configured host names to dotcms.org for internal
 * corporate information (we are dying to know who is using dotCMS!).
 * To turn this off, set the dotmarketing-config.properties
 * INIT_THREAD_DOTCMS = false
 *
 */
    public class InitThread extends Thread {

        public void run() {
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                Logger.debug(this, e.getMessage(), e);
            }
            String address = null;
            String hostname = "unknown";
            try {
                InetAddress addr = InetAddress.getLocalHost();
                byte[] ipAddr = addr.getAddress();
                addr = InetAddress.getByAddress(ipAddr);
                address = addr.getHostAddress();
                hostname = addr.getHostName();
            } catch (Exception e) {
                Logger.debug(this, "InitThread broke:", e);
            }
            try {
                String defaultHost = HostFactory.getDefaultHost().getHostname();
                StringBuilder sb = new StringBuilder();
                List<Host> hosts = HostFactory.getAllHosts();
                for (Host h : hosts) {
                    sb.append(h.getHostname() + "\n");
                    if (UtilMethods.isSet(h.getAliases())) {
                        sb.append(h.getAliases());
                    }
                }
                StringBuilder data = new StringBuilder();
                data.append(URLEncoder.encode("ipAddr", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(address, "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("hostname", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(hostname, "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("defaultHost", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(defaultHost, "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("allHosts", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(sb.toString(), "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("version", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(ReleaseInfo.getReleaseInfo(), "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("build", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(ReleaseInfo.getBuildNumber()), "UTF-8"));
                sb.delete(0, sb.length());
                sb.append("h");
                sb.append("tt");
                sb.append("p");
                sb.append(":");
                sb.append("//");
                sb.append("p");
                sb.append("i");
                sb.append("n");
                sb.append("g");
                sb.append(".");
                sb.append("d");
                sb.append("ot");
                sb.append("cms");
                sb.append(".");
                sb.append("or");
                sb.append("g/");
                sb.append("servlets/TB");
                sb.append("Information");
                URL url = new URL(sb.toString());
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(data.toString());
                wr.flush();
                wr.close();
                DataInputStream input = new DataInputStream(conn.getInputStream());
                input.close();
            } catch (UnknownHostException e) {
                Logger.debug(this, "Unable to get Hostname", e);
            } catch (Exception e) {
                Logger.debug(this, "InitThread broke:", e);
            } finally {
                DotHibernate.closeSession();
            }
        }
    }
}
