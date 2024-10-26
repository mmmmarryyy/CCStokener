package ch.jester.orm;

import java.net.URL;
import java.sql.Connection;
import java.util.Scanner;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleContext;
import ch.jester.common.ui.activator.AbstractUIActivator;
import ch.jester.commonservices.api.logging.ILogger;
import ch.jester.commonservices.api.persistency.IDatabaseManager;
import ch.jester.commonservices.api.persistency.IORMConfiguration;
import ch.jester.orm.internal.ORMAutoDBHandler;

/**
 * Singleton Plugin welches Zugriff auf die EntityManager/Factory bietet.
 * Setzt das Property jester.activeDatabase im System mit dem aktuellen DB Bundle.
 *
 */
public class ORMPlugin extends AbstractUIActivator {

    public static final String EP_CONFIGURATION = "Configuration";

    public static final String EP_CONFIGURATION_DATABASEMANAGERCLZ = "DatabaseManagerClass";

    public static final String EP_CONFIGURATION_ORMCONFIGURATION = "ORMConfiguration";

    private static ORMPlugin mPlugin;

    private static ILogger mLogger;

    private static ORMAutoDBHandler handler;

    private static EntityManager mManager;

    public ORMPlugin() {
        mPlugin = this;
    }

    @Override
    public void startDelegate(BundleContext context) {
        handler = new ORMAutoDBHandler(this);
        mLogger = getActivationContext().getLogger();
        mLogger.info("ORMPlugin started");
        handler.initialize();
        System.setProperty("jester.activeDatabase", handler.getPreferenceManager("ch.jester.orm").getPropertyByInternalKey("selectedDB").getValue().toString());
    }

    @Override
    public void stopDelegate(BundleContext context) {
        IDatabaseManager manager = handler.getDatabaseManager();
        if (manager != null) {
            manager.stop();
            manager.shutdown();
        }
        getJPAEntityManager().close();
        getJPAEntityManager().getEntityManagerFactory().close();
        mLogger.info("ORMPlugin stopped");
    }

    /**
	 * Returns the shared instance.
	 */
    public static ORMPlugin getDefault() {
        return mPlugin;
    }

    /**
	 * liefert die Id dieses Plugins aus dem Manifest
	 * 
	 * @return
	 */
    public String getPluginId() {
        return getActivationContext().getPluginId();
    }

    public static EntityManagerFactory getJPAEntityManagerFactory() {
        return handler.getORMConfiguration().getJPAEntityManagerFactory();
    }

    public static EntityManager getJPAEntityManager() {
        if (mManager == null) {
            synchronized (ORMPlugin.class) {
                if (mManager == null) {
                    mManager = getJPAEntityManagerFactory().createEntityManager();
                    checkIndex();
                }
            }
        }
        return mManager;
    }

    private static void checkIndex() {
        try {
            URL url = Platform.getBundle("ch.jester.orm").getEntry("META-INF/index_drop.idx");
            buildIndex(url);
            mLogger.debug("Dropped indices");
        } catch (Exception e) {
            mLogger.debug("-WARN: " + e.getLocalizedMessage());
        }
        try {
            URL url = Platform.getBundle("ch.jester.orm").getEntry("META-INF/index_create.idx");
            buildIndex(url);
            mLogger.debug("Created indices");
        } catch (Exception e) {
            mLogger.debug("-WARN: " + e.getLocalizedMessage());
        }
    }

    private static void buildIndex(URL url) throws Exception {
        Scanner scanner = new Scanner(url.openStream());
        EntityManager worker = getJPAEntityManagerFactory().createEntityManager();
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.startsWith("#") || line.isEmpty()) continue;
            EntityTransaction trx = worker.getTransaction();
            trx.begin();
            worker.createNativeQuery(line).executeUpdate();
            trx.commit();
        }
    }

    /**
	 * liefert die Configuration
	 * 
	 * @return
	 */
    public static IORMConfiguration getConfiguration() {
        return handler.getORMConfiguration();
    }

    /**
	 * liefert einen neue Connection
	 * @return
	 */
    public static Connection getConnection() {
        return handler.getORMConfiguration().getConnection();
    }

    /**
	 * liefert den IDataBaseManager oder null
	 * @return
	 */
    public IDatabaseManager getDataBaseManager() {
        return handler.getDatabaseManager();
    }

    /**
	 * Den Database Type
	 * @return den Typ
	 */
    public String getDataBaseTypeName() {
        return handler.getDataBaseTypeName();
    }
}
