package com.ideo.jso.conf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import com.ideo.jso.util.Resource;
import com.ideo.jso.util.URLUtils;

/**
 * This class represents the contract to override to decide whether the Configuration Files (default is jso.xml) should be reloaded or not.
 * 
 * @author Julien Maupoux
 *
 */
public abstract class AbstractConfigurationLoader {

    private static String CONFIGURATION_FILE_NAME = "jso.xml";

    private static String EXTERNAL_FILE_NAME = "";

    private static String LOCAL_CLASSPATH = "classpath";

    private static final Logger LOG = Logger.getLogger(AbstractConfigurationLoader.class);

    private static AbstractConfigurationLoader instance;

    private String defaultLocation = null;

    private String defaultTimestampPolicy = null;

    private Map groups;

    /**
	 * Return the instance of the configuration loader. This instance must have been created during the initialization
	 * @return the instance of the configuration loader
	 */
    public static AbstractConfigurationLoader getInstance() {
        return instance;
    }

    /**
	 * Initializes the configuration loader
	 * @param configFileName the name of the configuration files
	 * @param clazz the class of implementation of the configuration loader 
	 * @param externalFilePath A file to override/add group to the configuration
	 */
    public static void init(String configFileName, String clazz, String externalFilePath) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (configFileName != null) {
            CONFIGURATION_FILE_NAME = configFileName;
        }
        if (externalFilePath != null) {
            EXTERNAL_FILE_NAME = externalFilePath;
        }
        if (clazz == null) {
            throw new IllegalArgumentException("The key jso.configurationLoaderClass must be filled in the jso.properties.");
        } else {
            instance = (AbstractConfigurationLoader) Class.forName(clazz).newInstance();
        }
    }

    /**
	 * Decides whether a reload must be performed or not
	 * @return true if a reload of the configuration files must be performed, false otherwise
	 */
    protected abstract boolean mustReloadConfigurationFiles();

    /**
	 * Load the groups from the files, if a reload is needed 
	 * @return a map of groups loaded, with their name as key and the group itself as value
	 */
    private synchronized Map load() {
        if (!mustReloadConfigurationFiles()) {
            return groups;
        }
        SAXParser saxParser = null;
        JSODefaultHandler saxHandler = new JSODefaultHandler();
        try {
            final Collection resourcesByOrigin = getConfigResources();
            final LinkedList resourcesList = new LinkedList();
            Iterator iOrigin = resourcesByOrigin.iterator();
            while (iOrigin.hasNext()) {
                Resource resource = (Resource) iOrigin.next();
                String origin = resource.getSource();
                if (origin.startsWith(LOCAL_CLASSPATH) || JarRestrictionManager.getInstance().isJarAllowed(origin)) {
                    LOG.debug("Adding " + CONFIGURATION_FILE_NAME + " from " + origin + ".");
                    resourcesList.addFirst(resource.getUrl());
                } else {
                    LOG.debug("Jar " + origin + " refused. See jso.allowedJar property in jso.properties file.");
                }
            }
            URL external = getExternalResource();
            if (external != null) {
                resourcesList.addFirst(external);
            }
            saxParser = SAXParserFactory.newInstance().newSAXParser();
            Iterator ite = resourcesList.iterator();
            while (ite.hasNext()) {
                final URL url = (URL) ite.next();
                LOG.debug("Parsing of file " + url.toString() + ".");
                InputStream input = null;
                try {
                    input = url.openStream();
                    saxParser.parse(input, saxHandler);
                } catch (SAXException e) {
                    LOG.error("Parsing of file " + url.toString() + " failed! Parsing still continues.", e);
                } catch (IOException e) {
                    LOG.error("Reading of file " + url.toString() + " failed! Parsing still continues.", e);
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            LOG.error("Closing inputstream of file " + url.toString() + " failed! Parsing still continues.", e);
                        }
                    }
                }
            }
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        this.defaultLocation = (String) saxHandler.getDefaultValues().get("location");
        this.defaultTimestampPolicy = (String) saxHandler.getDefaultValues().get("timeStampPolicy");
        if (this.defaultTimestampPolicy == null) this.defaultTimestampPolicy = Group.TIMESTAMP_LOCAL;
        this.groups = saxHandler.getListGroups();
        return this.groups;
    }

    /**
	 * Return an enumeration of the configuration files found through the
	 * getResources(String) method
	 * #JSO-14
	 * @return an enumeration of the configuration files found
	 */
    private Collection getConfigResources() throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration result = cl.getResources(CONFIGURATION_FILE_NAME);
        LinkedList resources = new LinkedList();
        while (result.hasMoreElements()) {
            URL url = (URL) result.nextElement();
            String jarName = URLUtils.extractJarNameFromUrl(url);
            if (jarName == null) {
                jarName = LOCAL_CLASSPATH + url.getPath();
            }
            resources.addLast(new Resource(jarName, url));
        }
        return resources;
    }

    protected URL getExternalResource() throws MalformedURLException {
        return ("".equals(EXTERNAL_FILE_NAME)) ? null : new File(EXTERNAL_FILE_NAME).toURL();
    }

    /**
     * Return the map of the groups as they have been previously computed. 
     * If none have already been read, the load is executed. 
     * @return the map of the groups, with their name as keys and the group itself as value
     */
    public Map getGroups() {
        if (groups == null) groups = getUpdatedGroups();
        return groups;
    }

    /**
     * Return the map of the groups updated if required by the configurator 
     * @return the map of the groups, with their name as keys and the group itself as value
     */
    public Map getUpdatedGroups() {
        groups = load();
        return groups;
    }

    /**
     * Return the configuration file name used in this application
     * Default is jso.xml
     * @return the configuration file name used in this application
     */
    public String getConfigurationFileName() {
        return CONFIGURATION_FILE_NAME;
    }

    public String getDefaultLocation() {
        return defaultLocation;
    }

    public String getDefaultTimestampPolicy() {
        return defaultTimestampPolicy;
    }
}
