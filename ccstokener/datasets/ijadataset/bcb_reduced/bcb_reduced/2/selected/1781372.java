package org.mss.quartzjobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Policy;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.mss.db.hibernateutil.hibernatecompiler.HibernateCompilerImpl;
import org.mss.db.hibernateutil.hibernatecompiler.IHibernateCompiler;
import org.mss.db.hibernateutil.model.TableModel;
import org.mss.db.hibernateutil.utils.DOM4JUtil;
import org.mss.db.hibernateutil.utils.HibernateUtil;
import org.mss.db.hibernateutil.utils.ReflectionUtil;
import org.mss.quartzjobs.model.hibernatemodel.TableModelList;
import org.mss.quartzjobs.model.jobmodel.JobOverview;
import org.mss.quartzjobs.model.jobmodel.JobOverviewList;
import org.mss.quartzjobs.repository.Repository;
import org.mss.quartzjobs.utils.QuartzUtil;
import org.mss.quartzjobs.views.ViewJobOverview;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CorePlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.mss.core";

    public static final String LOGGER_PREFERENCES_EXTENSION_POINT = PLUGIN_ID + ".loggerPreferences";

    private QuartzUtil quartzUtil = new QuartzUtil();

    private HibernateUtil hibernateutil = new HibernateUtil();

    private IHibernateCompiler ihibernatecompiler = new HibernateCompilerImpl();

    private ReflectionUtil reflectionutil = new ReflectionUtil();

    private DOM4JUtil dom4jutil = new DOM4JUtil();

    private static Repository repository;

    private Log log = LogFactory.getLog(getClass());

    private static CorePlugin plugin;

    /**
	 * The constructor
	 */
    public CorePlugin() {
        log.debug("Consturctor of CorePlugin");
    }

    /**
	 * MK 19.09.08
	 * 
	 * get Repository
	 * Repository is defined as extension point !!
	 * @return
	 */
    public Repository getRepository() {
        if (repository == null) {
            repository = new Repository();
        }
        return repository;
    }

    public HibernateUtil getHibernateUtil() {
        return hibernateutil;
    }

    public IHibernateCompiler getHibernateCompiler() {
        return ihibernatecompiler;
    }

    public ReflectionUtil getReflectionUtil() {
        return reflectionutil;
    }

    public DOM4JUtil getDOM4JUtil() {
        return dom4jutil;
    }

    public QuartzUtil getQuartzUtil() {
        return quartzUtil;
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        System.setProperty("workspace_loc", Platform.getLocation().toPortableString());
        configureLogging();
        LogListener logListener = new LogListener();
        Platform.addLogListener(logListener);
        getLog().addLogListener(logListener);
        initJobScheduler();
        TableModelList tablelist = getRepository().unmarshalTableModels();
        Set<String> keyset = tablelist.getKeySet();
        Iterator<String> keyiterator = keyset.iterator();
        CorePlugin.getDefault().getHibernateUtil().initHibernateConfiguration();
        Bundle hibernatestructuresbundle = Platform.getBundle(IHibernateCompiler.HIBERNATE_MAPPING_PLUGIN);
        while (keyiterator.hasNext()) {
            TableModel model = tablelist.getTableModelbyName(keyiterator.next());
            boolean compilerstatus = CorePlugin.getDefault().getHibernateCompiler().compileModelinBundle(model);
            if (compilerstatus) {
                Class hibernateclass = hibernatestructuresbundle.loadClass(model.getTablename());
                System.out.println("Class " + model.getTablename() + " successfully loaded");
                CorePlugin.getDefault().getHibernateUtil().addClass(hibernateclass);
            }
        }
        hibernatestructuresbundle.stop();
        hibernatestructuresbundle.start();
        CorePlugin.getDefault().getHibernateUtil().initSessionFactory();
        CorePlugin.getDefault().getHibernateUtil().executeSQLQuery("show tables");
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        if (repository != null) repository.dispose();
        CorePlugin.getDefault().getHibernateUtil().commitTransaction();
        CorePlugin.getDefault().getHibernateUtil().closeSessionFactory();
        repository.marshalTableModels();
        super.stop(context);
    }

    /**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
    public static CorePlugin getDefault() {
        return plugin;
    }

    /**
	 * MK 18.09.2008
	 * 
	 * init automatic job scheduler
	 */
    public void initJobScheduler() {
        try {
            URL quartzproperties = this.getBundle().getResource("/org/mss/quartzjobs/quartz.properties");
            log.debug(" Initialize Quartz Job Scheduler: " + quartzproperties.toExternalForm());
            String quartzpropertiespath = quartzproperties.getPath();
            quartzUtil.initscheduler(quartzpropertiespath);
            quartzUtil.deleteAllJobs();
        } catch (Exception e) {
            System.out.println(" Initialize Quartz Job Scheduler error");
            log.error("Error in initJobScheduler " + e.toString());
            e.printStackTrace();
        } finally {
            log.debug("Quartz Job Scheduller successfully initialized ");
        }
    }

    /**
	 * Log4j configurator
	 */
    public void configureLogging() {
        try {
            PreferenceStore preferences = new PreferenceStore();
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint extensionPoint = registry.getExtensionPoint(CorePlugin.LOGGER_PREFERENCES_EXTENSION_POINT);
            IConfigurationElement[] members = extensionPoint.getConfigurationElements();
            for (int i = 0; i < members.length; i++) {
                IConfigurationElement element = members[i];
                if (element.getName().equals("logger")) {
                    if (element.getAttribute("defaultValue") != null) {
                        String[] item = element.getAttribute("name").split(";");
                        for (int x = 0; x < item.length; x++) preferences.setDefault("log4j.logger." + item[x], element.getAttribute("defaultValue"));
                    }
                }
            }
            try {
                URL url = CorePlugin.getDefault().getBundle().getResource("log4j.properties");
                Properties properties = new Properties();
                properties.load(url.openStream());
                for (Iterator iter = properties.keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    preferences.setDefault(key, (String) properties.get(key));
                }
                File file = CorePlugin.getDefault().getStateLocation().append("log4j.properties").toFile();
                if (file.exists()) preferences.load(new FileInputStream(file));
            } catch (Exception e) {
                CorePlugin.logException(e);
            }
            Properties properties = new Properties();
            String[] names = preferences.preferenceNames();
            for (int i = 0; i < names.length; i++) properties.put(names[i], preferences.getString(names[i]));
            PropertyConfigurator.configure(properties);
        } catch (Exception e) {
            BasicConfigurator.configure();
            logException(e);
        }
    }

    private void getPluginState(int pluginstate) {
        switch(pluginstate) {
            case Bundle.ACTIVE:
                System.out.println(" ACTIVE ");
                break;
            case Bundle.INSTALLED:
                System.out.println("INSTALLED");
                break;
            case Bundle.RESOLVED:
                System.out.println("RESOLVED");
                break;
            case Bundle.STOPPING:
                System.out.println("STOPPING");
                break;
            case Bundle.STARTING:
                System.out.println("STARTING");
                break;
            case Bundle.UNINSTALLED:
                System.out.println("UNINSTALLED");
                break;
        }
    }

    private void setSecurityManager() {
        SecurityManager sm = new SecurityManager();
        Policy eclispePolicy = new EclipsePolicy(Policy.getPolicy());
        Policy.setPolicy(eclispePolicy);
        System.setSecurityManager(sm);
    }

    /**
	 * MK 05.12.2008 log exception
	 * @param e
	 */
    public static void logException(Exception e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        getDefault().getLog().log(new Status(Status.ERROR, PLUGIN_ID, 0, msg, e));
    }

    /**
	 * MK 08.11.08
	 * 
	 * get Directory of hibernateutil bundle to locate HBM Files
	 * 
	 * 
	 */
    public void getFilesinBundle(String bundlename) {
        URL url = null;
        File directory = null;
        Bundle bundle = CorePlugin.getDefault().getBundle();
        try {
            url = FileLocator.resolve(bundle.getEntry("/"));
            directory = new File(url.getFile());
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                for (int count = 0; count < files.length; count++) {
                    System.out.println(count + " " + files[count].getName());
                }
            }
        } catch (IOException e) {
            System.out.println("/hibernate.cfg.xml file");
        }
    }

    /**
	 * 
	 * @param bundlename
	 * @return
	 */
    public String getBundleLocation(String bundlename) {
        File xmlConfig = null;
        URL url = null;
        String directory = null;
        Bundle bundle = Platform.getBundle(bundlename);
        try {
            url = FileLocator.resolve(bundle.getEntry(File.separator));
            xmlConfig = new File(url.getFile());
            directory = xmlConfig.getAbsolutePath();
        } catch (IOException e) {
            System.out.println(" Error in getBundleLocation ");
        }
        return directory;
    }
}
