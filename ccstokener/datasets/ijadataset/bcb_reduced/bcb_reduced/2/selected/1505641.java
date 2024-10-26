package net.sf.csutils.core.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.xml.registry.Connection;
import javax.xml.registry.JAXRException;
import net.sf.csutils.core.registry.ConnectionProvider;
import net.sf.csutils.core.registry.ModelDrivenRegistryFacade;
import net.sf.csutils.core.registry.ROMetaModelAccessor;
import net.sf.csutils.core.registry.ROModelAccessor;
import net.sf.csutils.core.registry.SimpleModelDrivenRegistryFacade;
import net.sf.csutils.core.registry.jaxmas.JaxMasConnectionProvider;
import net.sf.csutils.core.registry.jaxmas.JaxMasRegistryInfo;
import org.apache.labs.jaxmas.registry.infomodel.ConnectionImpl;
import org.apache.labs.jaxmas.registry.schema.DbInitializer;
import org.apache.log4j.Logger;

/**
 * Abstract base class for implementing tests, which require a JaxMas registry.
 * This class is intentionally not dependent on JUnit, or JaxMas, at compile
 * time, so that JUnit and JaxMas doesn't appear in the dependency list. OTOH,
 * this means that any test class must contain something similar to
 * <pre>
 *   @BeforeClass public static void setUpClass() {
 *       initProperties();
 *       initDb();
 *   }
 * </pre>
 */
public abstract class AbstractJaxMasTestCase {

    private static final Logger log = Logger.getLogger(AbstractJaxMasTestCase.class);

    private static Properties properties;

    private static File getDbDir() {
        return new File(new File("target"), "db");
    }

    public static void initProperties() throws IOException {
        final File dir = getDbDir();
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        final String p = "JaxmasRegistryTestCase.properties";
        final URL url = Thread.currentThread().getContextClassLoader().getResource(p);
        if (url == null) {
            throw new IllegalStateException("Missing resource file: " + p);
        }
        properties = new Properties();
        properties.load(url.openStream());
    }

    /**
     * Creates a new database connection provider.
     * @return A connection provider to use for creating the connection.
     */
    protected static ConnectionProvider newConnectionProvider() {
        final JaxMasConnectionProvider provider = new JaxMasConnectionProvider();
        provider.setDriver(properties.getProperty("db.driver"));
        provider.setUrl(properties.getProperty("db.url"));
        provider.setUser(properties.getProperty("db.user"));
        provider.setPassword(properties.getProperty("db.password"));
        provider.setLocale(properties.getProperty("db.locale"));
        return provider;
    }

    /**
     * Initializes the database schema.
     * @throws JAXRException The operation failed.
     */
    public static void initDb() throws JAXRException {
        final String mName = "initDb";
        Connection conn = null;
        try {
            conn = newConnectionProvider().getConnection();
            new DbInitializer((ConnectionImpl) conn).initialize();
            conn.close();
            conn = null;
        } catch (JAXRException e) {
            log.error(mName, e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    /**
     * This method is used to initialize the database before running a test case.
     * @throws Exception The operation failed.
     */
    public static void initDatabase() throws Exception {
        initProperties();
        initDb();
    }

    private ModelDrivenRegistryFacade facade;

    private ROMetaModelAccessor metaModelAccessor;

    private ROModelAccessor accessor;

    protected ModelDrivenRegistryFacade newFacade() throws JAXRException {
        final Connection conn = newConnectionProvider().getConnection();
        return new SimpleModelDrivenRegistryFacade(JaxMasRegistryInfo.getInstance(), conn);
    }

    protected void open() throws JAXRException {
        close();
        facade = newFacade();
        metaModelAccessor = facade.getMetaModelAccessor();
        accessor = facade.getModelAccessor();
    }

    protected void close() throws JAXRException {
        if (facade != null) {
            close(facade);
            facade = null;
        }
    }

    protected void close(ModelDrivenRegistryFacade pFacade) throws JAXRException {
        pFacade.getConnection().close();
    }

    protected ModelDrivenRegistryFacade getFacade() {
        return facade;
    }

    protected ROModelAccessor getModelAccessor() {
        return accessor;
    }

    protected ROMetaModelAccessor getMetaModelAccessor() {
        return metaModelAccessor;
    }
}
