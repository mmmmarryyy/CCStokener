package org.jcompany.jdoc.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;
import javax.servlet.ServletContext;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.jcompany.jdoc.commons.PlcJDocGeneratelUtil;
import org.xml.sax.SAXException;

public class PlcHibernateConfigDigester {

    protected static Logger log = Logger.getLogger(PlcHibernateConfigDigester.class);

    protected Vector hibernateConfigsVector;

    protected java.util.List fabricas;

    protected PlcHibernateConfig hibernateConfig;

    protected Vector factoriesView;

    public PlcHibernateConfigDigester(ServletContext servletContext) {
        log.debug("######## Entrou em PlcHibernateConfigDigester");
        hibernateConfigsVector = new Vector();
        try {
            this.parse(servletContext, "hibernate.cfg.xml");
            this.processaFactories(servletContext);
            factoriesView = new Vector();
            for (int c = 0; c < hibernateConfigsVector.size(); c++) {
                PlcHibernateConfig plcHibernateConfig = (PlcHibernateConfig) hibernateConfigsVector.get(c);
                PlcHibernateSessionFactory plcHibernateSessionFactory = (PlcHibernateSessionFactory) plcHibernateConfig.getHibernateSessionFactories().get(0);
                factoriesView.add(plcHibernateSessionFactory);
                servletContext.setAttribute("factoriesView", factoriesView);
            }
        } catch (SAXException saex) {
            saex.printStackTrace();
        } catch (IOException ioex) {
            ioex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void parse(ServletContext servletContext, String nomeArquivo) throws IOException, SAXException {
        if (log.isDebugEnabled()) log.debug("Entrou para fazer parse do arquivo " + nomeArquivo);
        Digester hibernateConfigDigester = new Digester();
        hibernateConfigDigester.addObjectCreate("hibernate-configuration", PlcHibernateConfig.class);
        hibernateConfigDigester.addObjectCreate("hibernate-configuration/session-factory", PlcHibernateSessionFactory.class);
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory", "name", "name");
        hibernateConfigDigester.addObjectCreate("hibernate-configuration/session-factory/property", PlcHibernateProperty.class);
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/property", "name", "propertyName");
        hibernateConfigDigester.addBeanPropertySetter("hibernate-configuration/session-factory/property", "propertyValue");
        hibernateConfigDigester.addSetNext("hibernate-configuration/session-factory/property", "addHibernateProperty");
        hibernateConfigDigester.addObjectCreate("hibernate-configuration/session-factory/mapping", PlcHibernateMapping.class);
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/mapping", "resource", "resource");
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/mapping", "file", "file");
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/mapping", "jar", "jar");
        hibernateConfigDigester.addSetNext("hibernate-configuration/session-factory/mapping", "addHibernateMapping");
        hibernateConfigDigester.addObjectCreate("hibernate-configuration/session-factory/jcs-class-cache", PlcHibernateJcsClassCache.class);
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/jcs-class-cache", "class", "class");
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/jcs-class-cache", "region", "region");
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/jcs-class-cache", "usage", "usage");
        hibernateConfigDigester.addSetNext("hibernate-configuration/session-factory/jcs-class-cache", "addHibernateJcsClassCache");
        hibernateConfigDigester.addObjectCreate("hibernate-configuration/session-factory/jcs-collection-cache", PlcHibernateJcsCollectionCache.class);
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/jcs-collection-cache", "collection", "collection");
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/jcs-collection-cache", "region", "region");
        hibernateConfigDigester.addSetProperties("hibernate-configuration/session-factory/jcs-collection-cache", "usage", "usage");
        hibernateConfigDigester.addSetNext("hibernate-configuration/session-factory/jcs-collection-cache", "addHibernateJcsCollectionCache");
        hibernateConfigDigester.addSetNext("hibernate-configuration/session-factory", "addHibernateSessionFactory");
        String servidor = servletContext.getInitParameter("servidor").trim();
        InputStream is;
        if (log.isDebugEnabled()) log.debug("jDoc - Vai testar o arquivo " + nomeArquivo);
        if (!nomeArquivo.endsWith(".cfg.xml")) nomeArquivo = nomeArquivo + ".cfg.xml";
        if (log.isDebugEnabled()) log.debug("jDoc - Apos o teste o arquivo ficou " + nomeArquivo);
        if (servidor.equals("silverstream")) {
            ClassLoader loader = this.getClass().getClassLoader();
            URL url = loader.getResource(nomeArquivo);
            is = url.openStream();
        } else {
            try {
                is = servletContext.getResourceAsStream("/WEB-INF/classes/" + nomeArquivo);
            } catch (Exception e) {
                is = servletContext.getResourceAsStream("\\WEB-INF\\classes\\" + nomeArquivo);
            }
        }
        if (is != null) {
            try {
                hibernateConfig = (PlcHibernateConfig) hibernateConfigDigester.parse(is);
            } catch (Exception e) {
                log.error("Erro ao tentar fazer parse nos arquivos da Hibernate " + e, e);
                e.printStackTrace();
            }
            hibernateConfigsVector.add(hibernateConfig);
        } else {
            log.warn("jDoc - Nao conseguiu recuperar arquivo de configuracao hibernate: " + nomeArquivo);
        }
    }

    private void processaFactories(ServletContext servletContext) throws SAXException, IOException {
        String sessionFactories = servletContext.getInitParameter("sessionFactories");
        PlcJDocGeneratelUtil helper = new PlcJDocGeneratelUtil();
        if ((!sessionFactories.equals("#")) & (!sessionFactories.equals(""))) {
            fabricas = helper.separaListaTermos(sessionFactories);
            for (int h = 0; h < fabricas.size(); h++) {
                String fabricaAtual = ((String) fabricas.get(h)).trim();
                this.parse(servletContext, fabricaAtual);
            }
        } else {
            log.debug("Session factories adicionais nao foram declaradas no arquivo web.xml");
            return;
        }
    }

    public Vector getHibernateConfigsVector() {
        return hibernateConfigsVector;
    }

    public void setHibernateConfigsVector(Vector newVector) {
        this.hibernateConfigsVector = newVector;
    }

    public PlcHibernateConfig getHibernateConfig() {
        return hibernateConfig;
    }

    public void setHibernateConfig(PlcHibernateConfig newHibernateConfig) {
        hibernateConfig = newHibernateConfig;
    }
}
