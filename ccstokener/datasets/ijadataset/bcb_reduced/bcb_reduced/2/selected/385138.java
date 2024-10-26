package com.icteam.fiji.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEventListener;
import com.icteam.fiji.persistence.conf.HibConfiguration;
import com.icteam.fiji.persistence.conf.HibMapping;
import com.icteam.fiji.persistence.conf.HibSessionFactory;
import com.icteam.fiji.util.LoadingUtils;

/**
 * Created by IntelliJ IDEA. User: co04978 Date: 10-ago-2005 Time: 10.35.19 To
 * change this template use File | Settings | File Templates.
 */
public class PersistenceManager {

    private static Log logger = LogFactory.getLog(PersistenceManager.class.getName());

    private static final String CONFIG_FILE = "conf/hibernate.properties.xml";

    private static final String MAPPINGS_FILE = "conf/hibernate.cfg.xml";

    private static final Configuration CONFIGURATION = getConfiguration();

    private static final SessionFactory SESSION_FACTORY = getSessionFactory();

    private static final FijiSessionFactory FIJI_SESSION_FACTORY = new FijiSessionFactoryImpl();

    private static final boolean IS_JDBC_MODE = true;

    private static final boolean IS_CMT_MODE = false;

    public static class Transaction {

        private org.hibernate.Transaction m_transaction = null;

        private Session m_session = null;

        private boolean m_alreadyOpened = true;

        public Transaction(Session p_session) {
            if (p_session != null) m_session = p_session; else m_session = getCurrentSession(this);
            m_session.setFlushMode(FlushMode.COMMIT);
        }

        public Session getSession() {
            return m_session;
        }

        public void begin() {
            if (!IS_CMT_MODE) m_transaction = m_session.beginTransaction();
        }

        public void commit() {
            m_session.flush();
            if (!IS_CMT_MODE) {
                m_transaction.commit();
                if (!m_alreadyOpened) m_session.close();
            }
        }

        public void rollback() {
            if (!IS_CMT_MODE) {
                m_transaction.rollback();
                if (!m_alreadyOpened) m_session.close();
            }
        }

        public boolean isActive() {
            return m_transaction.isActive();
        }
    }

    public PersistenceManager() {
    }

    public static Session getCurrentSession() {
        Session session = null;
        try {
            session = SESSION_FACTORY.getCurrentSession();
        } catch (Exception ignore) {
        }
        if (session == null && IS_JDBC_MODE) {
            session = SESSION_FACTORY.openSession();
        }
        return session;
    }

    public static FijiSession getCurrentFijiSession() {
        Session session = getCurrentSession();
        return FIJI_SESSION_FACTORY.getFijiSession(session);
    }

    public static SessionFactory getCurrentSessionFactory() {
        return SESSION_FACTORY;
    }

    public static String getHibernateProperty(String name) {
        return CONFIGURATION.getProperty(name);
    }

    protected static Transaction beginTransaction(Session p_session) {
        Transaction transaction = new Transaction(p_session);
        transaction.begin();
        return transaction;
    }

    public static Transaction beginTransaction() {
        return beginTransaction(null);
    }

    private static Session getCurrentSession(Transaction p_transaction) {
        Session session = null;
        try {
            session = SESSION_FACTORY.getCurrentSession();
        } catch (Exception ignore) {
        }
        if (session == null && IS_JDBC_MODE) {
            session = SESSION_FACTORY.openSession();
            p_transaction.m_alreadyOpened = false;
        }
        return session;
    }

    private static Configuration getConfiguration() {
        logger.debug("Start");
        try {
            Configuration cfg = new Configuration().configure(LoadingUtils.getResource(CONFIG_FILE));
            cfg.getEventListeners().setPreInsertEventListeners(new PreInsertEventListener[] { new InsertListener() });
            cfg.getEventListeners().setPreUpdateEventListeners(new PreUpdateEventListener[] { new UpdateListener() });
            cfg.setInterceptor(new I18NInterceptor());
            loadMappings(cfg);
            return cfg;
        } catch (Exception e) {
            e.printStackTrace();
            logger.fatal("Exception: ", e);
            return null;
        }
    }

    private static SessionFactory getSessionFactory() {
        logger.debug("Start");
        try {
            return CONFIGURATION.buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
            logger.fatal("Exception: ", e);
            return null;
        }
    }

    private static void loadMappings(Configuration cfg) {
        try {
            Enumeration en = LoadingUtils.getResources(MAPPINGS_FILE);
            while (en.hasMoreElements()) {
                URL url = (URL) en.nextElement();
                logger.info("Found mapping module " + url.toExternalForm());
                InputStream inputStream = null;
                try {
                    inputStream = url.openStream();
                    HibConfiguration hm = loadModuleMappings(inputStream);
                    configureModuleMappings(cfg, hm.getSessionFactory());
                } catch (IOException e) {
                    logger.warn("Could not load mappings file \"" + url.toExternalForm() + "\"", e);
                } catch (JAXBException e) {
                    logger.warn("Unable to instantiate JAXBContext ", e);
                } finally {
                    try {
                        if (inputStream != null) inputStream.close();
                    } catch (IOException e) {
                        logger.debug(e);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Could not find any mappings file hibernate.mappings.xml", e);
        }
    }

    private static void configureModuleMappings(Configuration p_cfg, HibSessionFactory hm) throws IOException {
        List<HibMapping> mappingList = hm.getMappings();
        for (HibMapping mapping : mappingList) {
            logger.info("Adding mapping " + mapping.getResource());
            p_cfg.addInputStream(LoadingUtils.getResourceAsStream(mapping.getResource()));
        }
    }

    private static HibConfiguration loadModuleMappings(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance("com.icteam.fiji.persistence.conf");
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return (HibConfiguration) unmarshaller.unmarshal(is);
    }
}
