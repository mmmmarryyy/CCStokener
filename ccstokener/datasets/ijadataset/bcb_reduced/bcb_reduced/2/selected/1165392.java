package com.icteam.fiji.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.icteam.fiji.persistence.conf.VelQueryTemplates;
import com.icteam.fiji.persistence.template.IQueryTemplateFactory;
import com.icteam.fiji.persistence.template.QueryTemplateConfiguration;
import com.icteam.fiji.util.LoadingUtils;
import com.icteam.fiji.util.XMLDocumentBuilderUtils;

public class FijiSessionFactoryImpl implements FijiSessionFactory {

    private static Log logger = LogFactory.getLog(FijiSessionFactoryImpl.class);

    private static final String QUERY_TEMPLATES_FILE = "conf/query-templates.cfg.xml";

    private static final ThreadLocal<FijiSession> FIJI_SESSIONS = new ThreadLocal<FijiSession>();

    private final IQueryTemplateFactory queryTemplateFactory;

    public FijiSessionFactoryImpl() {
        this.queryTemplateFactory = this.createQueryTemplateFactoryInstance();
    }

    public FijiSession getFijiSession(Session session) {
        return lookupFijiSession(session);
    }

    /**
   * Creazione della fiji session.
   * Viene tenuto a livello di Thread un riferimento a FijiSession valido solo nel caso
   * in cui la sessione passata in input sia uguale a quella wrappata da FijiSession.
   * 
   * Il remove della FijiSession e' demandata al garbage collector
   * (la classe ThreadLocal utilizza WeakReference) 
   * 
   * @param session oggetto Session recuperato da hibernate 
   * @return Implementazione di FijiSession
   */
    private FijiSession lookupFijiSession(Session session) {
        if (session == null) {
            return null;
        }
        FijiSession fijiSession = FIJI_SESSIONS.get();
        if (fijiSession == null || fijiSession.getWrappedSession() != session) {
            fijiSession = new FijiSessionImpl(session, this.queryTemplateFactory);
            FIJI_SESSIONS.set(fijiSession);
        }
        return fijiSession;
    }

    private IQueryTemplateFactory createQueryTemplateFactoryInstance() {
        IQueryTemplateFactory queryTemplateFactory = null;
        try {
            logger.info("Parsing QueryTemplateConfiguration");
            QueryTemplateConfiguration configuration = this.parseQueryTemplateConfiguration();
            logger.info("Building QueryTemplateConfiguration");
            queryTemplateFactory = configuration.buildTemplateQueryFactory();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return queryTemplateFactory;
    }

    private QueryTemplateConfiguration parseQueryTemplateConfiguration() throws IOException, JAXBException, SAXException {
        QueryTemplateConfiguration configuration = new QueryTemplateConfiguration();
        this.addVelQueryTemplates(configuration);
        return configuration;
    }

    private void addVelQueryTemplates(QueryTemplateConfiguration queryTemplateConfiguration) throws IOException, JAXBException, SAXException {
        Enumeration<URL> en = LoadingUtils.getResources(QUERY_TEMPLATES_FILE);
        VelQueryTemplates templates = null;
        URL url = null;
        while (en.hasMoreElements()) {
            url = en.nextElement();
            logger.info("Reading Query Template module: [" + url.toExternalForm() + "]");
            templates = this.readVelQueryTemplates(url);
            queryTemplateConfiguration.addQueryConfiguration(templates);
        }
    }

    private VelQueryTemplates readVelQueryTemplates(URL url) throws IOException, JAXBException, SAXException {
        VelQueryTemplates templates = null;
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            templates = this.unmarshallConfiguration(inputStream);
        } catch (JAXBException jaxbException) {
            logger.error("JAXBException nella lettura del template: [" + url.toExternalForm() + "]", jaxbException);
            throw jaxbException;
        } catch (SAXException saxException) {
            logger.error("SAXException nella lettura del template: [" + url.toExternalForm() + "]", saxException);
            throw saxException;
        } catch (IOException ioException) {
            logger.error("IOException nella lettura del template: [" + url.toExternalForm() + "]", ioException);
            throw ioException;
        } finally {
            LoadingUtils.quietlyClose(inputStream);
        }
        return templates;
    }

    private VelQueryTemplates unmarshallConfiguration(InputStream is) throws SAXException, IOException, JAXBException {
        JAXBContext jc = JAXBContext.newInstance(VelQueryTemplates.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        Document document = this.parseConfiguration(is);
        VelQueryTemplates velQueryTemplates = (VelQueryTemplates) unmarshaller.unmarshal(document);
        return velQueryTemplates;
    }

    private Document parseConfiguration(InputStream is) throws SAXException, IOException {
        DocumentBuilder docBuilder = XMLDocumentBuilderUtils.getLocalDocumentBuilderInstance();
        return docBuilder.parse(is);
    }
}
