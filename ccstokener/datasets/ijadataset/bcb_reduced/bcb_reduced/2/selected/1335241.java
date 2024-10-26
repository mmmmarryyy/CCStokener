package edu.upmc.opi.caBIG.common;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.xerces.parsers.DOMParser;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import edu.upmc.opi.caBIG.caTIES.server.index.CaTIES_CombinedLuceneAnalyzer;

/**
 * The Class CaTIES_DataSourceManager.
 * 
 * @author mitchellkj@upmc.edu
 * @version $Id: CaTIES_DataSourceManager.java,v 1.2 2010/01/06 20:05:05
 *          girish_c1980 Exp $
 * @since 1.4.2_04
 */
public class CaTIES_DataSourceManager {

    private static Logger logger = Logger.getLogger(CaTIES_DataSourceManager.class);

    public static final int CaTIES_ORACLE_DATA_SOURCE = 1;

    public static final int CaTIES_MYSQL_DATA_SOURCE = 0;

    private int databaseType = CaTIES_MYSQL_DATA_SOURCE;

    private String indexDirectoryName;

    private String hibernateConfigurationFileName = "hibernate.mysql.cfg.xml";

    private SessionFactory sessionFactory;

    private Interceptor sessionInterceptor;

    private Document configDocument;

    private Configuration configuration = new Configuration();

    private final ThreadLocal<Session> session = new ThreadLocal<Session>();

    public void initialize() throws Exception {
        URL url = null;
        try {
            url = new URL(this.hibernateConfigurationFileName);
        } catch (MalformedURLException e) {
            logger.warn("Hibernate Configuration filename not specified as URL. Will treat it as resource name and try to load.");
        }
        if (url != null) {
            fetchHibernateConfigFromURL(url);
        } else {
            logger.debug("Calling fetchHibernateConfigFromFile using " + this.hibernateConfigurationFileName);
            fetchHibernateConfigFromFile(this.hibernateConfigurationFileName);
        }
        if (getConnectionURL() != null) {
            overrideConnectionURL();
        }
        this.configuration.configure(configDocument);
        this.sessionFactory = this.configuration.buildSessionFactory();
        setUpSessionInterceptor();
    }

    private void fetchHibernateConfigFromURL(URL url) throws Exception {
        try {
            fetchHibernateConfig(url.openStream());
        } catch (Exception e) {
            throw new Exception("Could not initialize from Hibernate Configuration URL:" + url, e);
        }
    }

    private void fetchHibernateConfigFromFile(String filename) throws Exception {
        try {
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = loader.getResourceAsStream(filename);
            fetchHibernateConfig(inputStream);
        } catch (Exception e) {
            throw new HibernateException("Could not initialize from Hibernate Configuration file: " + filename, e);
        }
    }

    private void fetchHibernateConfig(InputStream inputStream) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(inputStream));
        this.configDocument = parser.getDocument();
    }

    private String connectionURL = null;

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL, boolean isSecure) {
        this.connectionURL = connectionURL;
        this.isSecureConnection = isSecure;
    }

    boolean isSecureConnection = true;

    private void overrideConnectionURL() {
        if (getConnectionURL() == null) return;
        logger.info("Overriding connection url. New URL:" + connectionURL + ". Is Secure:" + isSecureConnection);
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//property[@name='connection.url']");
            Object result = expr.evaluate(configDocument, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            Node n = nodes.item(0);
            n.setTextContent(getConnectionURL());
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            logger.error("Incorrect xml syntax for the hibernate config file");
        } catch (DOMException e) {
            e.printStackTrace();
            logger.error("Incorrect xml syntax for the hibernate config file");
        }
    }

    public int getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(int databaseType) {
        this.databaseType = databaseType;
        if (this.databaseType == CaTIES_ORACLE_DATA_SOURCE) {
            this.configuration.setNamingStrategy(new CaTIES_OracleNamingStrategy());
        }
    }

    public String getHibernateConfigurationFileName() {
        return hibernateConfigurationFileName;
    }

    public void setHibernateConfigurationFileName(String hibernateConfigurationFileName) {
        this.hibernateConfigurationFileName = hibernateConfigurationFileName;
    }

    public String getIndexDirectoryName() {
        return indexDirectoryName;
    }

    public void setIndexDirectoryName(String indexDirectoryName) {
        this.indexDirectoryName = indexDirectoryName;
    }

    public Interceptor getSessionInterceptor() {
        return sessionInterceptor;
    }

    public void setSessionInterceptor(Interceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    public Session currentSession() throws HibernateException {
        Session s = this.session.get();
        if (s == null) {
            if (this.sessionFactory == null || this.sessionFactory.isClosed()) throw new HibernateException("DataSourceManager already destroyed. Please use new DataSourceManager");
            if (this.sessionInterceptor != null) {
                s = this.sessionFactory.openSession(sessionInterceptor);
            } else {
                s = this.sessionFactory.openSession();
            }
            this.session.set(s);
        }
        return s;
    }

    public void closeSession() throws HibernateException {
        Session s = this.session.get();
        if (s != null && s.isOpen()) s.close();
        this.session.set(null);
    }

    protected void setUpSessionInterceptor() {
        if (this.sessionInterceptor != null && this.sessionInterceptor instanceof CaTIES_LuceneInterceptor && this.indexDirectoryName != null) {
            FSDirectory fsDirectory = createOrOpenIndex();
            ((CaTIES_LuceneInterceptor) this.sessionInterceptor).setDirectory(fsDirectory);
        }
    }

    protected FSDirectory createOrOpenIndex() {
        FSDirectory fsDirectory = null;
        try {
            File f;
            boolean createDirectory = true;
            if ((f = new File(this.indexDirectoryName)).exists() && f.isDirectory()) {
                createDirectory = false;
            } else {
                createDirectory = true;
            }
            fsDirectory = FSDirectory.getDirectory(this.indexDirectoryName, createDirectory);
            IndexWriter writer = new IndexWriter(fsDirectory, new CaTIES_CombinedLuceneAnalyzer(), createDirectory);
            writer.setMergeFactor(20);
            writer.close();
        } catch (Exception x) {
            x.printStackTrace();
            fsDirectory = null;
        }
        return fsDirectory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void destroy() {
        closeSession();
        this.sessionFactory.close();
        this.sessionFactory = null;
    }
}
