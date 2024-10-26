package genie.core;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;

/**
 * 
 *
 * @author T. Kia Ntoni
 * 
 * 1 janv. 2005 
 * Engine @version 
 */
public class Engine extends Manager {

    private static Log log = LogFactory.getLog(Engine.class);

    public static final String GENIE_CORE_ENGINE_CONTEXT = "genie.core.engine.context";

    public static final String GENIE_CORE_ENGINE_CONFIG = "genie.core.engine.config";

    public static final String GENIE_CORE_ENGINE_CONFIG_PATH = "genie.core.engine.config.path";

    public static final String ARG_CONFIG_FILE = "-gfile";

    public static final String ARG_CONFIG_FIND = "-gfind";

    public static final String ARG_DEFAULT_PROP_FILE = "-dpfile";

    public static final String ARG_DEFAULT_PROP_FIND = "-dpfind";

    public static final String ARG_PROP_FIND = "-pfind";

    public static final String ARG_PROP_FILE = "-pfile";

    public static final String ARG_ENTITYRESOLVER = "-entityresolver";

    private static final String ARG_RULESET = "-ruleset";

    /**
     * 
     */
    public Engine() {
        super();
    }

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("C:/EclipseBase/workspace/geniesphere/log4j.properties");
        if (log.isDebugEnabled()) {
            log.debug("main(String[]) - start");
        }
        if (StringUtils.isBlank(System.getProperty("genie.home"))) {
            File dir = new File(".");
            String home = dir.getCanonicalFile().getAbsolutePath().endsWith("\\bin") ? new File("../").getCanonicalPath() : dir.getCanonicalPath();
            System.setProperty("genie.home", home);
            if (log.isDebugEnabled()) {
                log.debug("main(String[]) - setted system property genie.home {" + home + "}");
            }
        }
        try {
            Manager engine = preprocess(args);
            engine.execute(null);
        } catch (Exception e) {
            log.error("main(String[])", e);
            e.printStackTrace();
            throw e;
        }
        if (log.isDebugEnabled()) {
            log.debug("main(String[]) - end");
        }
    }

    private static Manager preprocess(String[] args) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("preprocess(String[]) - start");
            log.debug("GENIE_HOME : " + System.getProperty("genie.home"));
        }
        URL defaultPropertiesUrl = getDefaultPropertiesUrl(args);
        Properties defaults = getDefaultProperties(defaultPropertiesUrl);
        URL customPropertiesUrl = getCustomPropertiesUrl(args, defaults);
        Properties customs = loadCustomProperties(customPropertiesUrl, defaults);
        EntityResolver resolver = getEntityResolver(args, customs);
        URL configUrl = getConfigUrl(args, customs);
        Document config = getConfig(configUrl, resolver);
        RuleSet ruleSet = getRuleSet(args, customs);
        Manager engine = new Engine();
        engine.setProperties(customs);
        engine.setConfig(config);
        Digester digester = getDigester(resolver, ruleSet);
        digester.push(engine);
        Manager returnManager = (Manager) digester.parse(configUrl.openStream());
        if (log.isDebugEnabled()) {
            log.debug("preprocess(String[]) - end");
        }
        return returnManager;
    }

    private static URL findResource(String name, boolean find) throws Exception {
        if (StringUtils.isBlank(name)) {
            if (log.isDebugEnabled()) {
                log.debug("findResource(" + name + "," + find + ") - {" + null + "}");
            }
            return null;
        }
        URL url = Engine.class.getResource(name);
        if ((url == null && !find) || url != null) {
            if (log.isDebugEnabled()) {
                log.debug("findResource(" + name + "," + find + ") - {" + url + "}");
            }
            return url;
        }
        if (new File(name).isAbsolute()) {
            if (log.isDebugEnabled()) {
                log.debug("findResource(" + name + "," + find + ") - {" + url + "}");
            }
            return url;
        }
        try {
            File userHomeBased = new File(System.getProperty("user.home") + "/" + name);
            if (userHomeBased.exists()) {
                url = userHomeBased.toURI().toURL();
                if (log.isDebugEnabled()) {
                    log.debug("findResource(" + name + "," + find + ") - {" + url + "}");
                }
                return url;
            }
            String home = StringUtils.isNotBlank(System.getProperty("genie.home")) ? System.getProperty("genie.home") : "";
            File genieDirBased = new File(home + "/" + name);
            if (genieDirBased.exists()) {
                url = genieDirBased.toURI().toURL();
                if (log.isDebugEnabled()) {
                    log.debug("findResource(" + name + "," + find + ") - {" + url + "}");
                }
                return url;
            }
            File parent = userHomeBased.getAbsoluteFile().getCanonicalFile().getParentFile();
            url = findFromBase(name, parent);
            if (url != null) {
                if (log.isDebugEnabled()) {
                    log.debug("findResource(" + name + "," + find + ") - {" + url + "}");
                }
                return url;
            }
            parent = genieDirBased.getAbsoluteFile().getCanonicalFile().getParentFile();
            url = findFromBase(name, parent);
            if (log.isDebugEnabled()) {
                log.debug("findResource(" + name + "," + find + ") - {" + url + "}");
            }
            return url;
        } catch (Exception e) {
            log.error("findResource(" + name + "," + find + ")", e);
            throw e;
        }
    }

    private static URL findFromBase(String name, File parent) throws Exception {
        if (StringUtils.isBlank(name)) {
            if (log.isDebugEnabled()) {
                log.debug("findFromBase(" + "name" + "," + parent + ") - {" + null + "}");
            }
            return null;
        }
        while (parent != null) {
            parent = parent.getAbsoluteFile().getCanonicalFile().getParentFile();
            File file = new File(parent != null ? parent.getAbsoluteFile().getCanonicalPath() + "/" + name : name);
            if (file.exists()) {
                URL returnURL = file.toURI().toURL();
                if (log.isDebugEnabled()) {
                    log.debug("findFromBase(" + "name" + "," + parent + ") - {" + returnURL + "}");
                }
                return returnURL;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("findFromBase(" + "name" + "," + parent + ") - {" + null + "}");
        }
        return null;
    }

    private static URL getConfigUrl(String[] args, Properties defaults) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("getConfigUrl(String[], Properties) - start");
        }
        String find = getKeyValue(args, ARG_CONFIG_FIND).length == 2 ? getKeyValue(args, ARG_CONFIG_FIND)[1] : defaults.getProperty("genie.core.engine.config.find", "true");
        String name = getKeyValue(args, ARG_CONFIG_FILE).length == 2 ? getKeyValue(args, ARG_CONFIG_FILE)[1] : defaults.getProperty("genie.core.engine.config.url", "genie.xml");
        URL returnURL = findResource(name, find.trim().equalsIgnoreCase("false") ? false : true);
        if (log.isDebugEnabled()) {
            log.debug("getConfigUrl(String[], Properties) - name {" + name + "}");
            log.debug("getConfigUrl(String[], Properties) - url {" + returnURL + "}");
            log.debug("getConfigUrl(String[], Properties) - end");
        }
        return returnURL;
    }

    private static Document getConfig(URL url, EntityResolver entityResolver) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("getConfig(URL, EntityResolver) - start");
        }
        try {
            SAXBuilder builder = new SAXBuilder(true);
            builder.setEntityResolver(entityResolver);
            Document genieDoc = builder.build(url.openStream());
            if (log.isDebugEnabled()) {
                log.debug("getConfig(URL, EntityResolver) - end");
            }
            return genieDoc;
        } catch (Exception e) {
            log.error("getConfig(URL, EntityResolver)", e);
            throw e;
        }
    }

    private static void setConfigUrlProperty(String[] args, Properties properties) throws Exception {
        URL url = getConfigUrl(args, properties);
        if (url != null && properties != null) {
            properties.setProperty("genie.core.engine.config.url", url.toURI().toString());
        }
    }

    private static URL getCustomPropertiesUrl(String[] args, Properties defaults) throws Exception {
        String find = getKeyValue(args, ARG_PROP_FIND).length == 2 ? getKeyValue(args, ARG_PROP_FIND)[1] : defaults.getProperty("genie.core.engine.user.properties.find", "true");
        String name = getKeyValue(args, ARG_PROP_FILE).length == 2 ? getKeyValue(args, ARG_PROP_FILE)[1] : defaults.getProperty("genie.core.engine.user.properties.url");
        return findResource(name, find.trim().equalsIgnoreCase("false") ? false : true);
    }

    private static Properties loadCustomProperties(URL url, Properties defaults) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("loadCustomProperties(URL, Properties) - start");
        }
        Properties properties = defaults != null ? new Properties(defaults) : new Properties();
        if (url != null && url.toURI().toString().endsWith(".xml")) {
            properties.loadFromXML(url.openStream());
        } else if (url != null && !url.toURI().toString().endsWith(".xml")) {
            properties.load(url.openStream());
        }
        if (log.isDebugEnabled()) {
            log.debug("loadCustomProperties(URL, Properties) - end");
        }
        return properties;
    }

    private static Properties getDefaultProperties(URL url) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("loadDefaultProperties(URL) - url {" + url + "}");
        }
        Properties properties = new Properties();
        if (url != null && url.toURI().toString().endsWith(".properties")) {
            properties.load(url.openStream());
        } else if (url != null && url.toURI().toString().endsWith(".xml")) {
            properties.loadFromXML(url.openStream());
        }
        if (log.isDebugEnabled()) {
            log.debug("loadDefaultProperties(URL) - loaded properties");
        }
        return properties;
    }

    private static URL getDefaultPropertiesUrl(String[] args) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("getDefaultPropertiesUrl(String[], Properties) - start");
        }
        String find = getKeyValue(args, ARG_DEFAULT_PROP_FIND).length == 2 ? getKeyValue(args, ARG_DEFAULT_PROP_FIND)[1] : "true";
        String name = getKeyValue(args, ARG_DEFAULT_PROP_FILE).length == 2 ? getKeyValue(args, ARG_DEFAULT_PROP_FILE)[1] : "/src/conf/genie_default.properties";
        URL returnURL = findResource(name, find.trim().equalsIgnoreCase("false") ? false : true);
        if (log.isDebugEnabled()) {
            log.debug("getDefaultPropertiesUrl(String[], Properties) - name {" + name + "}");
            log.debug("getDefaultPropertiesUrl(String[], Properties) - url {" + returnURL + "}");
            log.debug("getDefaultPropertiesUrl(String[], Properties) - end");
        }
        return returnURL;
    }

    private static void updateProperties(String key, String value, Properties properties) throws Exception {
    }

    private static void setProperty(String[] args, Properties properties, String regex, String key) {
        if (args != null && properties != null && regex != null && key != null) {
            String string = ArrayUtils.toString(args);
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                String arg = matcher.group();
                String[] keyValue = arg.split("=");
                if (keyValue.length == 2) {
                    properties.setProperty(key, keyValue[1]);
                }
            }
        }
    }

    private static void setDProperties(String[] args, Properties properties) {
        if (args != null && properties != null) {
            String string = ArrayUtils.toString(args);
            Pattern pattern = Pattern.compile("-D\\w*=\\w*\b");
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                while (matcher.find()) {
                    String arg = matcher.group();
                    String[] keyValue = arg.split("=");
                    if (keyValue.length == 2) {
                        properties.setProperty(keyValue[0].substring(2), keyValue[1]);
                        System.out.print("Setting Dproperty [" + keyValue[0].substring(2) + "," + keyValue[1] + "]");
                    }
                }
            }
        }
    }

    private static Digester getDigester(EntityResolver entityResolver, RuleSet ruleSet) throws Exception {
        Digester digester = new Digester();
        digester.setEntityResolver(entityResolver);
        digester.setValidating(true);
        digester.addRuleSet(ruleSet);
        return digester;
    }

    private static RuleSet getRuleSet(String[] args, Properties properties) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("getRuleSet(String[], Properties) - start");
        }
        String clsName = null;
        if (args != null && getKeyValue(args, ARG_RULESET).length == 2) {
            clsName = getKeyValue(args, ARG_RULESET)[1];
            if (log.isDebugEnabled()) {
                log.debug("getRuleSet(String[], Properties) - RuleSet implementation from command line {" + clsName + "}");
            }
        } else if (properties != null) {
            clsName = properties.getProperty("genie.core.engine.ruleset", ConfigRuleSet.class.getName());
            if (log.isDebugEnabled()) {
                log.debug("getRuleSet(String[], Properties) - RuleSet implementation from properties {" + clsName + "}");
            }
        }
        RuleSet returnRuleSet = (RuleSet) Class.forName(clsName).newInstance();
        if (log.isDebugEnabled()) {
            log.debug("getRuleSet(String[], Properties) - end");
        }
        return returnRuleSet;
    }

    private static void setRuleSetProperty(String[] args, Properties properties) {
        setProperty(args, properties, "-ruleset=\\w*\b", "genie.core.engine.ruleset");
    }

    private static EntityResolver getEntityResolver(String[] args, Properties properties) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("getEntityResolver(String[], Properties) - start");
        }
        EntityResolver entityResolver = null;
        String entityResolverClass = null;
        if (args != null && getKeyValue(args, ARG_ENTITYRESOLVER).length == 2) {
            entityResolverClass = getKeyValue(args, ARG_ENTITYRESOLVER)[1];
            if (log.isDebugEnabled()) {
                log.debug("getEntityResolver(String[], Properties) - EntityResolver implementation from command line argument {" + entityResolverClass + "}");
            }
        }
        if (entityResolver == null && properties != null) {
            entityResolverClass = properties.getProperty("genie.core.engine.entityResolver", EntityValidator.class.getName());
            if (log.isDebugEnabled()) {
                log.debug("getEntityResolver(String[], Properties) - EntityResolver implementation from properties {" + entityResolverClass + "}");
            }
        }
        entityResolver = (EntityResolver) Class.forName(entityResolverClass).newInstance();
        if (entityResolver instanceof IEntityResolver) {
            String localSchemaId = System.getProperty("genie.home") + "/conf/genie_1_0.dtd";
            ((IEntityResolver) entityResolver).register("http://geniesphere.sourceforge.net/dtd/genie_1_0.dtd", localSchemaId);
        }
        if (properties != null && entityResolver instanceof IEntityResolver) {
            Enumeration names = properties.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if (name.endsWith(".schema_id") && StringUtils.isNotBlank(properties.getProperty(name + "_mapper"))) {
                    ((IEntityResolver) entityResolver).register(properties.getProperty(name), properties.getProperty(name + "_mapper"));
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("getEntityResolver(String[], Properties) - end");
        }
        return entityResolver;
    }

    private static void setEntityResolverProperty(String[] args, Properties properties) {
        if (args != null && properties != null) {
            setProperty(args, properties, "-entityResolver=\\w*\b", "genie.core.engine.entityResolver");
        }
    }

    private static String[] getKeyValue(String[] args, String regexPrefix) {
        if (log.isDebugEnabled()) {
            log.debug("getKeyValue(" + ArrayUtils.toString(args) + "," + regexPrefix + ")");
        }
        if (args != null && regexPrefix != null) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals(regexPrefix)) {
                    if (log.isDebugEnabled()) {
                        log.debug("getKeyValue(" + ArrayUtils.toString(args) + "," + regexPrefix + ") {" + ArrayUtils.toString(arg.split("=")) + "}");
                    }
                    return arg.split("=");
                }
                if (arg.startsWith(regexPrefix + "=")) {
                    if (log.isDebugEnabled()) {
                        log.debug("getKeyValue(" + ArrayUtils.toString(args) + "," + regexPrefix + ") {" + ArrayUtils.toString(arg.split("=")) + "}");
                    }
                    return arg.split("=");
                }
            }
        }
        String[] returnStringArray = new String[0];
        if (log.isDebugEnabled()) {
            log.debug("getKeyValue(String[], String) - end");
        }
        return returnStringArray;
    }

    private static Collection<String[]> getKeyValuetest(String[] args, String regexPrefix) {
        if (log.isDebugEnabled()) {
            log.debug("getKeyValuetest(String[], String) - start");
        }
        Vector<String[]> result = new Vector<String[]>();
        if (args != null) {
            String string = ArrayUtils.toString(args);
            Pattern pattern = Pattern.compile(regexPrefix + "=\\w*\b");
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                while (matcher.find()) {
                    String arg = matcher.group();
                    result.add(arg.split("="));
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("getKeyValuetest(String[], String) - end");
        }
        return result;
    }

    private static Collection<String[]> getKeyValues(String[] args, String regexPrefix) {
        Vector<String[]> result = new Vector<String[]>();
        if (args != null) {
            String string = ArrayUtils.toString(args);
            Pattern pattern = Pattern.compile(regexPrefix + "=\\w*\b");
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                while (matcher.find()) {
                    String arg = matcher.group();
                    result.add(arg.split("="));
                }
            }
        }
        return result;
    }
}
