package org.jaffa.persistence.engines.jdbcengine.configservice;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.jaffa.config.Config;
import org.jaffa.persistence.engines.jdbcengine.configservice.exceptions.InitFileNotFoundRuntimeException;
import org.jaffa.persistence.engines.jdbcengine.configservice.exceptions.ConfigurationServiceRuntimeException;
import org.jaffa.util.URLHelper;
import org.jaffa.persistence.engines.jdbcengine.configservice.initdomain.Init;
import org.jaffa.persistence.engines.jdbcengine.configservice.initdomain.Database;
import org.jaffa.persistence.engines.jdbcengine.configservice.initdomain.PreloadClass;
import org.jaffa.persistence.engines.jdbcengine.configservice.initdomain.DirLoc;
import org.jaffa.util.DefaultEntityResolver;
import org.jaffa.util.DefaultErrorHandler;
import org.jaffa.util.JAXBHelper;
import org.jaffa.util.XmlHelper;

/** This class implements the Singleton pattern. Use the getInstance() method to get an instance of this class.
 * The Configuration Service reads the init.xml file. It then performs the initializations.
 * This class caches the ClassMetaData objects.
 * It also maintains all the Database objects.
 */
public class ConfigurationService {

    private static final Logger log = Logger.getLogger(ConfigurationService.class);

    private static final String PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    private static final String MAPPING_FILE_PREFIX = ".xml";

    private static final String SCHEMA = "org/jaffa/persistence/engines/jdbcengine/configservice/initdomain/jdbc-engine-init_1_0.xsd";

    private static ConfigurationService m_singleton = null;

    private static Map m_metaCache = new WeakHashMap();

    private static Map m_databases = null;

    private List m_locations = null;

    /** Creates an instance of ConfigurationService, if not already instantiated.
     * This gets the location of the init.xml (relative to the classpath) from the framework.properties file.
     * @return An instance of the ConfigurationService.
     */
    public static ConfigurationService getInstance() {
        if (m_singleton == null) {
            createConfigurationServiceInstance();
        }
        return m_singleton;
    }

    private static synchronized void createConfigurationServiceInstance() {
        if (m_singleton == null) {
            String initfile = (String) Config.getProperty(Config.PROP_JDBC_ENGINE_INIT);
            if (log.isDebugEnabled()) {
                log.debug("Creating an instance of the ConfigurationService using the initialization file: " + initfile);
            }
            m_singleton = new ConfigurationService(initfile);
        }
    }

    /** This reads in the init file and parses it using JAXB.
     * It then maintains a Map of Database objects, a List of locations for finding mapping files.
     * It then preloads the mapping files, as defined in the init file.
     * It throws the runtime exception InitFileNotFoundRuntimeException, if the init file is not found.
     * It throws the runtime exception ConfigurationServiceRuntimeException, if the init file or any of the mapping files give parse errors.
     * It throws the runtime exception ClassMetaDataValidationRuntimeException, if the mapping file and the corresponding class have mismatches.
     */
    private ConfigurationService(String initFile) {
        InputStream stream = null;
        try {
            URL initUrl = URLHelper.newExtendedURL(initFile);
            stream = initUrl.openStream();
            JAXBContext jc = JAXBContext.newInstance("org.jaffa.persistence.engines.jdbcengine.configservice.initdomain");
            Unmarshaller u = jc.createUnmarshaller();
            u.setSchema(JAXBHelper.createSchema(SCHEMA));
            Init init = (Init) u.unmarshal(XmlHelper.stripDoctypeDeclaration(stream));
            m_databases = new HashMap();
            for (Iterator i = init.getDatabase().iterator(); i.hasNext(); ) {
                Database database = (Database) i.next();
                m_databases.put(database.getName(), database);
            }
            m_locations = init.getConfLocation().getDirLoc();
            if (init.getPreload() != null) {
                preLoad(init.getPreload().getPreloadClass());
            }
        } catch (MalformedURLException e) {
            String str = "Initialisation file '" + initFile + "' not found in the classpath";
            log.error(str);
            throw new InitFileNotFoundRuntimeException(str);
        } catch (Exception e) {
            String str = "Error while parsing the init file " + initFile;
            log.error(str, e);
            throw new ConfigurationServiceRuntimeException(str, e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /** Returns a Map of Database objects, keyed by database name, as defined in the init file.
     * @return a Map of Database objects, keyed by database name, as defined in the init file.
     */
    public Map getDatabases() {
        return m_databases;
    }

    /** Returns a Database object for the database name, as defined in the init file.
     * @param name The database name.
     * @return a Database object. A null will be returned, in case the name was not defined in the init file.
     */
    public Database getDatabase(String name) {
        return (Database) m_databases.get(name);
    }

    /** This tries to locate the mapping file for the input classname through the list of locations as defined in the init file.
     * It will create a ClassMetaData object & cache it.
     * It throws the runtime exception ConfigurationServiceRuntimeException, if the the mapping file could not be located.
     * It throws the runtime exception ClassMetaDataValidationRuntimeException, if the mapping file and the corresponding class have mismatches.
     * @param classname The class for which the mapping file is going to be parsed.
     * @return a ClassMetaData object.
     */
    public ClassMetaData getMetaData(String classname) {
        ClassMetaData classMetaData = null;
        if (m_metaCache.containsKey(classname)) {
            classMetaData = (ClassMetaData) m_metaCache.get(classname);
        } else {
            classMetaData = parseNew(classname);
        }
        return classMetaData;
    }

    private void preLoad(List preLoadList) {
        if (preLoadList != null) {
            for (Iterator itr = preLoadList.iterator(); itr.hasNext(); ) {
                PreloadClass preloadClass = (PreloadClass) itr.next();
                parseNew(preloadClass.getName());
            }
        }
    }

    private ClassMetaData parseNew(String classname) {
        if (log.isDebugEnabled()) {
            log.debug("Locating the mapping file for the Persistent class " + classname);
        }
        String shortname = classname.substring(classname.lastIndexOf('.') + 1);
        for (Iterator itr = m_locations.iterator(); itr.hasNext(); ) {
            DirLoc dirLoc = (DirLoc) itr.next();
            String packageName = dirLoc.getDir();
            StringBuffer urlNameBuf = new StringBuffer(packageName);
            if (!packageName.endsWith("/") && !packageName.endsWith("\\")) {
                urlNameBuf.append('/');
            }
            urlNameBuf.append(shortname).append(MAPPING_FILE_PREFIX);
            String urlName = urlNameBuf.toString();
            if (log.isDebugEnabled()) {
                log.debug("Looking for the mapping file " + urlName);
            }
            URL url = null;
            try {
                url = URLHelper.newExtendedURL(urlName);
            } catch (MalformedURLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not find the mapping file " + urlName + ". " + e.getMessage());
                }
            }
            if (url != null) {
                DefaultHandler handler = new MappingParser();
                InputStream stream = null;
                try {
                    XMLReader reader = XMLReaderFactory.createXMLReader(PARSER_NAME);
                    reader.setContentHandler(handler);
                    reader.setEntityResolver(new DefaultEntityResolver());
                    reader.setErrorHandler(new DefaultErrorHandler());
                    stream = url.openStream();
                    reader.parse(new InputSource(stream));
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Error in parsing the mapping file " + urlName + ". Will try and look at another location", e);
                    }
                    continue;
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                    }
                }
                ClassMetaData classMetaData = ((MappingParser) handler).getMetaData();
                if (classMetaData != null && classMetaData.getClassName().equals(classname)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Validating the ClassMetaData object for the mapping file " + urlName);
                    }
                    classMetaData.setXmlFileUrl(url);
                    classMetaData.validate();
                    synchronized (m_metaCache) {
                        if (m_metaCache.containsKey(classname)) {
                            classMetaData = (ClassMetaData) m_metaCache.get(classname);
                        } else {
                            m_metaCache.put(classname, classMetaData);
                        }
                    }
                    return classMetaData;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("The classname in the mapping file " + urlName + ", does not match the required value " + classname + ". Will try and look at another location");
                    }
                }
            }
        }
        String str = "Could not find/parse the mapping file for the class " + classname;
        log.error(str);
        throw new ConfigurationServiceRuntimeException(str);
    }
}
