package edu.upmc.opi.caBIG.caTIES.connector;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import edu.upmc.opi.caBIG.caTIES.connector.bridge.CaTIES_Driver;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.ActivityLogImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.AddressImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.DistributionProtocolAssignmentImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.DistributionProtocolImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.DistributionProtocolOrganizationImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.DocumentDataImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.DocumentImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.DocumentTypeImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.HL7DocumentImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.IdentifiedDocumentImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.IdentifiedPatientImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.IdentifiedSectionImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.OrderItemImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.OrderSetImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.OrganizationImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.OrganizationKeyImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.PatientEventImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.PatientImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.PatientListImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.QueryImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.SectionImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.SectionTypeImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.SectionValueHistogramImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.UserImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.UserKeyImpl;
import edu.upmc.opi.caBIG.caTIES.database.domain.impl.UserOrganizationImpl;

public class CaTIES_DataSourceManager {

    private SessionFactory sessionFactory;

    private Session session;

    private static Logger logger = Logger.getLogger(CaTIES_DataSourceManager.class);

    public CaTIES_DataSourceManager() {
        ;
    }

    public boolean initializeFromConfigFile(String hibernateConfigurationFileName, String overrideConnectionURL) {
        Document configDoc = null;
        try {
            URL url = new URL(hibernateConfigurationFileName);
            configDoc = fetchHibernateConfigFromURL(url);
        } catch (MalformedURLException e1) {
            logger.warn("Hibernate Configuration filename not specified as URL. Will treat it as resource name and try to load.");
            configDoc = null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Encountered error loading hibernate config. as URL. Trying as filename");
            configDoc = null;
        }
        if (configDoc == null) {
            logger.debug("Calling fetchHibernateConfigFromFile using " + hibernateConfigurationFileName);
            try {
                configDoc = fetchHibernateConfigFromFile(hibernateConfigurationFileName);
            } catch (Exception e1) {
                e1.printStackTrace();
                logger.error(e1.getMessage());
                configDoc = null;
            }
        }
        if (configDoc == null) return false;
        try {
            if (overrideConnectionURL != null) {
                new URL(overrideConnectionURL);
                overrideConnectionURL(configDoc, overrideConnectionURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Connection URL Override Failed:" + e.getMessage());
        }
        Configuration configuration = new Configuration();
        configuration.configure(configDoc);
        return initialize(configuration);
    }

    public boolean initialize(Configuration configuration) {
        this.sessionFactory = configuration.buildSessionFactory();
        return !sessionFactory.isClosed();
    }

    public SessionFactory buildSessionFactoryFromConfiguration(Configuration configuration) {
        configuration = addAnnotatedClasses(configuration);
        logger.debug("Successfully added annotated classes.");
        final SessionFactory sessionFactory = configuration.buildSessionFactory();
        logger.debug("Successfully created the sessionFactory.");
        return sessionFactory;
    }

    public boolean initializeWithActivityId(String connectionURL, String activityId) {
        try {
            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.connection.driver_class", CaTIES_Driver.class.getName());
            configuration.setProperty("hibernate.connection.url", connectionURL);
            configuration.setProperty("hibernate.connection.remote.drer.handle", activityId);
            configuration.setProperty("hibernate.connection.is.secure", "true");
            return initialize(configuration);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to connect to the database." + e.getMessage());
            return false;
        }
    }

    public boolean initializeFromHBMFiles(String connectionURL, String activityId) {
        try {
            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
            configuration.setProperty("hibernate.hbm2ddl.auto", "update");
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.connection.driver_class", CaTIES_Driver.class.getName());
            configuration.setProperty("hibernate.connection.url", connectionURL);
            configuration.setProperty("hibernate.connection.remote.drer.handle", activityId);
            configuration.setProperty("hibernate.connection.is.secure", "true");
            configuration = addHBMFiles(configuration);
            configuration.configure();
            return initialize(configuration);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to connect to the database." + e.getMessage());
            return false;
        }
    }

    public SessionFactory initializeFromSystemProperties(String hibernateConnectionUrl, String hibernateConnectionUserName, String hibernateConnectionPassword) {
        String hibernateDialect = System.getProperty("hibernate.dialect");
        hibernateDialect = (hibernateDialect == null) ? org.hibernate.dialect.Oracle10gDialect.class.getName() : hibernateDialect;
        String hibernateHbm2ddlAuto = System.getProperty("hibernate.hbm2ddl.auto");
        hibernateHbm2ddlAuto = (hibernateHbm2ddlAuto == null) ? "update" : hibernateHbm2ddlAuto;
        hibernateHbm2ddlAuto = "update";
        String hibernateShowSql = System.getProperty("hibernate.show_sql");
        hibernateShowSql = (hibernateShowSql == null) ? "true" : hibernateShowSql;
        String hibernateDriver = System.getProperty("hibernate.connection.driver_class");
        hibernateDriver = (hibernateDriver == null) ? "oracle.jdbc.driver.OracleDriver" : hibernateDriver;
        hibernateConnectionPassword = (hibernateConnectionPassword == null) ? "caties" : hibernateConnectionPassword;
        System.getProperty("hibernate.connection.remote.drer.handle");
        String hibernateConnectionIsSecure = System.getProperty("hibernate.connection.is.secure");
        hibernateConnectionIsSecure = (hibernateConnectionIsSecure == null) ? "false" : hibernateConnectionIsSecure;
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", hibernateDialect);
        configuration.setProperty("hibernate.hbm2ddl.auto", hibernateHbm2ddlAuto);
        configuration.setProperty("hibernate.show_sql", hibernateShowSql);
        configuration.setProperty("hibernate.connection.driver_class", hibernateDriver);
        configuration.setProperty("hibernate.connection.url", hibernateConnectionUrl);
        configuration.setProperty("hibernate.connection.username", hibernateConnectionUserName);
        configuration.setProperty("hibernate.connection.password", hibernateConnectionPassword);
        if (initialize(configuration)) return this.sessionFactory;
        return null;
    }

    private Document fetchHibernateConfigFromURL(URL url) throws Exception {
        try {
            return fetchHibernateConfig(url.openStream());
        } catch (Exception e) {
            throw new Exception("Could not initialize from Hibernate Configuration URL:" + url, e);
        }
    }

    private Document fetchHibernateConfigFromFile(String filename) throws Exception {
        try {
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = loader.getResourceAsStream(filename);
            return fetchHibernateConfig(inputStream);
        } catch (Exception e) {
            throw new HibernateException("Could not initialize from Hibernate Configuration file: " + filename, e);
        }
    }

    private Document fetchHibernateConfig(InputStream inputStream) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(inputStream));
        return parser.getDocument();
    }

    private Document overrideConnectionURL(Document configDoc, String newConnectionURLString) {
        logger.info("Overriding connection url. New URL:" + newConnectionURLString);
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//property[@name='connection.url']");
            Object result = expr.evaluate(configDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            Node n = nodes.item(0);
            n.setTextContent(newConnectionURLString);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            logger.error("Incorrect xml syntax for the hibernate config file");
        } catch (DOMException e) {
            e.printStackTrace();
            logger.error("Incorrect xml syntax for the hibernate config file");
        }
        return configDoc;
    }

    public Session currentSession() throws HibernateException {
        if (this.session == null) {
            if (this.sessionFactory == null || this.sessionFactory.isClosed()) {
                throw new HibernateException("DataSourceManager already destroyed. Please use new DataSourceManager");
            }
            this.session = this.sessionFactory.openSession();
        }
        if (!this.session.isOpen()) this.session = this.sessionFactory.openSession();
        return this.session;
    }

    public void closeSession() throws HibernateException {
        if (this.session != null && this.session.isOpen()) {
            this.session.close();
        }
        this.session = null;
    }

    public void closeSessionFactory() throws HibernateException {
        if (this.sessionFactory != null && !this.sessionFactory.isClosed()) {
            this.sessionFactory.close();
        }
        this.sessionFactory = null;
    }

    public void destroy() {
        closeSession();
        closeSessionFactory();
        logger.debug("destroyed CaTIES_DataSourceManager : session and sessionFactory removed.");
    }

    public Session getMySQLSession(String hibernateConnectionUrl, String hibernateHbm2ddlAuto) {
        return getMySQLSession(hibernateConnectionUrl, "caties", "caties", hibernateHbm2ddlAuto);
    }

    public Session getMySQLSession(String hibernateConnectionUrl, String userName, String userPassword, String hibernateHbm2ddlAuto) {
        logger.debug("Entering getMySQLSession with url " + hibernateConnectionUrl);
        logger.debug("Entering getMySQLSession with username " + userName);
        logger.debug("Entering getMySQLSession with password " + userPassword);
        logger.debug("New MySQL Connection: hibernateHbm2ddlAuto ==> " + hibernateHbm2ddlAuto);
        try {
            Configuration configuration = buildMySQLConfiguration(hibernateConnectionUrl, userName, userPassword, hibernateHbm2ddlAuto);
            logger.debug("Initializing configuration.");
            this.sessionFactory = buildSessionFactoryFromConfiguration(configuration);
            logger.debug("Finished initializing configuration.");
            logger.debug("Opening session...");
            this.session = this.sessionFactory.openSession();
            logger.debug("Connected to database. Made a hibernateSession.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("Failed to connect to the database.");
        }
        logger.debug("Exiting getMySQLSession");
        return this.session;
    }

    public static Configuration buildMySQLConfiguration(String hibernateConnectionUrl, String userName, String userPassword, String hibernateHbm2ddlAuto) {
        return buildConfiguration(org.hibernate.dialect.MySQLDialect.class.getName(), "com.mysql.jdbc.Driver", hibernateConnectionUrl, userName, userPassword, hibernateHbm2ddlAuto);
    }

    public static Configuration buildOracleConfiguration(String hibernateConnectionUrl, String userName, String userPassword, String hibernateHbm2ddlAuto) {
        return buildConfiguration(org.hibernate.dialect.Oracle10gDialect.class.getName(), "oracle.jdbc.driver.OracleDriver", hibernateConnectionUrl, userName, userPassword, hibernateHbm2ddlAuto);
    }

    public static Configuration buildConfiguration(String hibernateDialect, String hibernateDriver, String hibernateConnectionUrl, String userName, String userPassword, String hibernateHbm2ddlAuto) {
        logger.debug("Entering buildConfiguration");
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", hibernateDialect);
        configuration.setProperty("hibernate.connection.driver_class", hibernateDriver);
        configuration.setProperty("hibernate.show_sql", "false");
        configuration.setProperty("hibernate.connection.url", hibernateConnectionUrl);
        configuration.setProperty("hibernate.connection.username", userName);
        configuration.setProperty("hibernate.connection.password", userPassword);
        if (hibernateHbm2ddlAuto != null) {
            configuration.setProperty("hibernate.hbm2ddl.auto", hibernateHbm2ddlAuto);
        }
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        logger.debug("Exiting buildConfiguration");
        return configuration;
    }

    private static Configuration addAnnotatedClasses(Configuration configuration) {
        configuration.addAnnotatedClass(ActivityLogImpl.class);
        configuration.addAnnotatedClass(AddressImpl.class);
        configuration.addAnnotatedClass(DistributionProtocolAssignmentImpl.class);
        configuration.addAnnotatedClass(DistributionProtocolImpl.class);
        configuration.addAnnotatedClass(DistributionProtocolOrganizationImpl.class);
        configuration.addAnnotatedClass(DocumentTypeImpl.class);
        configuration.addAnnotatedClass(DocumentDataImpl.class);
        configuration.addAnnotatedClass(DocumentImpl.class);
        configuration.addAnnotatedClass(HL7DocumentImpl.class);
        configuration.addAnnotatedClass(IdentifiedDocumentImpl.class);
        configuration.addAnnotatedClass(IdentifiedPatientImpl.class);
        configuration.addAnnotatedClass(IdentifiedSectionImpl.class);
        configuration.addAnnotatedClass(OrderItemImpl.class);
        configuration.addAnnotatedClass(OrderSetImpl.class);
        configuration.addAnnotatedClass(OrganizationImpl.class);
        configuration.addAnnotatedClass(OrganizationKeyImpl.class);
        configuration.addAnnotatedClass(PatientEventImpl.class);
        configuration.addAnnotatedClass(PatientImpl.class);
        configuration.addAnnotatedClass(PatientListImpl.class);
        configuration.addAnnotatedClass(QueryImpl.class);
        configuration.addAnnotatedClass(SectionImpl.class);
        configuration.addAnnotatedClass(SectionTypeImpl.class);
        configuration.addAnnotatedClass(SectionValueHistogramImpl.class);
        configuration.addAnnotatedClass(UserImpl.class);
        configuration.addAnnotatedClass(UserKeyImpl.class);
        configuration.addAnnotatedClass(UserOrganizationImpl.class);
        return configuration;
    }

    private static Configuration addHBMFiles(Configuration configuration) {
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/ActivityLogImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/AddressImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/DistributionProtocolAssignmentImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/DistributionProtocolImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/DistributionProtocolOrganizationImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/DocumentDataImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/DocumentImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/DocumentTypeImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/HL7DocumentImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/IdentifiedDocumentImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/IdentifiedPatientImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/IdentifiedSectionImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/OrderItemImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/OrderSetImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/OrganizationImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/OrganizationKeyImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/PatientEventImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/PatientImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/PatientListImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/QueryImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/SectionImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/SectionTypeImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/SectionValueHistogramImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/UserImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/UserKeyImpl.hbm.xml"));
        configuration.addURL(CaTIES_DataSourceManager.class.getResource("../caTIES/database/domain/hbm/UserOrganizationImpl.hbm.xml"));
        return configuration;
    }

    public Session getMySQLSecureAdministrationRemoteSession(String spiritIp, String spiritPort) {
        return getMySQLRemoteSessionWithActivityId(spiritIp, spiritPort, "SecureAdmin");
    }

    public Session getMySQLRemoteSessionWithActivityId(String spiritIp, String spiritPort, String activityId) {
        try {
            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.connection.driver_class", CaTIES_Driver.class.getName());
            configuration.setProperty("hibernate.connection.url", "http://" + spiritIp + ":" + spiritPort + "/wsrf/services/dai");
            configuration.setProperty("hibernate.connection.remote.drer.handle", activityId);
            configuration.setProperty("hibernate.connection.is.secure", "true");
            addAnnotatedClasses(configuration);
            configuration.configure();
            this.sessionFactory = configuration.buildSessionFactory();
            this.session = sessionFactory.openSession();
            logger.debug("Connected to database. Made a hibernateSession.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Failed to connect to the database.");
        }
        return this.session;
    }

    public Session getSession() {
        return this.currentSession();
    }
}
