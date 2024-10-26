package org.weras.commons.file.jbfs;

import static org.weras.commons.file.jbfs.xml.XmlAlias.CONFIGURATION;
import static org.weras.commons.file.jbfs.xml.XmlAlias.LOCAL_DISK_STORE;
import static org.weras.commons.file.jbfs.xml.XmlAlias.PATH;
import java.io.InputStream;
import java.net.URL;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.weras.commons.file.jbfs.storages.LocalSystemFileStorage;
import org.weras.commons.file.jbfs.xml.ConfigurationXmlConverter;
import org.weras.commons.file.jbfs.xml.LocalDiskStoreXmlConverter;
import org.weras.commons.file.jbfs.xml.PathXmlConverter;

public class JBeanFileStorage {

    private Configuration configuration;

    public JBeanFileStorage() {
    }

    /**
	 * Loads configuration from file in class path
	 * 
	 * @param confLocation
	 */
    public void setConfLocation(String confLocation) {
        this.configuration = loadConf(getClass().getClassLoader().getResource(confLocation));
    }

    public PathResolver getPathResolver() {
        return configuration.getPathResolver();
    }

    public FileStorage getStore(String name) {
        return configuration.getStore(name);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration c) {
        configuration = c;
    }

    /**
	 * Loads Configuration given xml file reference
	 * 
	 * @param confFile
	 * @return
	 */
    public static Configuration loadConf(URL url) {
        XStream stream = new XStream(new DomDriver());
        InputStream input;
        try {
            input = url.openStream();
            stream.registerConverter(new ConfigurationXmlConverter());
            stream.registerConverter(new LocalDiskStoreXmlConverter());
            stream.registerConverter(new PathXmlConverter());
            stream.alias(CONFIGURATION, Configuration.class);
            stream.alias(LOCAL_DISK_STORE, LocalSystemFileStorage.class);
            stream.alias(PATH, Path.class);
            Configuration conf = (Configuration) stream.fromXML(input);
            return conf;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
