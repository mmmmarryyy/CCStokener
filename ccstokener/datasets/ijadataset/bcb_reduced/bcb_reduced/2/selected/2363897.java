package se.oktad.permgencleaner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import se.oktad.permgencleaner.exception.PermGenCleanerException;
import se.oktad.permgencleaner.logging.Logger;
import se.oktad.permgencleaner.logging.LoggerFactory;
import se.oktad.permgencleaner.module.PermGenCleanerModule;
import se.oktad.permgencleaner.util.PermGenCleanerUtil;

public class PropertyFileModuleEnumerator implements ModuleEnumerator {

    public static final String PROPERTY_FILE_NAME = "permgencleaner-modules.properties";

    private Logger logger = LoggerFactory.getLogger(PropertyFileModuleEnumerator.class);

    private PermGenCleanerUtil clUtil = new PermGenCleanerUtil();

    public List getModules() {
        return getModulesFromProperties();
    }

    private List getModulesFromProperties() {
        List modules = new ArrayList();
        Properties props = readProperties();
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String className = keys.nextElement() + "";
            PermGenCleanerModule module;
            Object o;
            try {
                o = Class.forName(className, true, clUtil.getCurrentClassLoader());
            } catch (ClassNotFoundException e) {
                throw new PermGenCleanerException("The class " + className + " does not exist");
            }
            if (o instanceof PermGenCleanerModule) {
                module = (PermGenCleanerModule) o;
            } else {
                throw new PermGenCleanerException("The class " + className + " does not implement the interface PermGenCleanerModule");
            }
            StringTokenizer tokenizer = new StringTokenizer(props.getProperty(className), "|");
            if (tokenizer.hasMoreElements()) {
                module.setShortDescription(tokenizer.nextToken());
            }
            if (tokenizer.hasMoreElements()) {
                module.setLongDescription(tokenizer.nextToken());
            }
            logger.debug("Adding module " + module.getName() + " (" + className + ") from property file");
            modules.add(module);
        }
        return modules;
    }

    private Properties readProperties() {
        Properties props = new Properties();
        try {
            Enumeration urls = new PermGenCleanerUtil().getCurrentClassLoader().getResources(PROPERTY_FILE_NAME);
            while (urls.hasMoreElements()) {
                URL url = (URL) urls.nextElement();
                logger.debug("Loading settings from property file '" + url + "'");
                InputStream is = url.openStream();
                props.load(is);
                is.close();
            }
        } catch (IOException e) {
            throw new PermGenCleanerException("Error while loading settings from property file", e);
        }
        return props;
    }
}
