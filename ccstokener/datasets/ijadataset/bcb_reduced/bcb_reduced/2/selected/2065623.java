package net.joindesk.let.definite;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import net.joindesk.api.WebApp;
import net.joindesk.api.exception.SysException;
import net.joindesk.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.core.io.Resource;

public class DeskletClasspathLoader implements DeskletLoader {

    private WebApp app;

    private VelocityEngine ve;

    private Properties velocityConfig;

    /**
	 * Map<String, DefaultHtmlFileInfo>
	 */
    private Map defaultHtmlsMap = new HashMap();

    private String htmlFileExt = "html";

    private String javaScriptFileExt = "js";

    private Properties mappingParsers;

    private List parsers;

    private static final String DEFAULT_PROPERTY_FILE_NAME = "classpath*:desklet.properties";

    private static String DEFAULT_TEMPLATE_HTML_FILE_NAME = "template.html";

    private static final Log log = LogFactory.getLog(DeskletClasspathLoader.class);

    private static final String DEFAULT_HTML_FILE_CONFIG = "joindesk.desklet.loader.templateHtmlFile";

    private static final String DESKLET_PROPERTY_FILE_CONFIG = "joindesk.desklet.loader.deskletPropertiesFile";

    private static final String ANNOTATION_ENABLE = "joindesk.desklet.loader.annotation";

    private static Class defaultDeskletClass;

    static {
        try {
            defaultDeskletClass = Class.forName("net.joindesk.let.DefaultDesklet");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public DeskletClasspathLoader() {
    }

    private void init() {
        parsers = new ArrayList();
        Iterator iter = mappingParsers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getKey().toString().equalsIgnoreCase("annotation")) {
                String annotationConfig = app.getConfig(ANNOTATION_ENABLE);
                if (annotationConfig == null || annotationConfig.equalsIgnoreCase("true") == false) continue;
            }
            try {
                parsers.add(Class.forName((String) entry.getValue()).newInstance());
            } catch (Exception e) {
                e.printStackTrace();
                throw new SysException("instance Mapping parser error.", e);
            }
        }
        ve = new VelocityEngine();
        if (velocityConfig == null) {
            velocityConfig = new Properties();
            velocityConfig.setProperty(Velocity.RESOURCE_LOADER, "net.joindesk.Loader");
            velocityConfig.setProperty("net.joindesk.Loader.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            velocityConfig.setProperty("net.joindesk.Loader.resource.loader.cache", "true");
        }
        try {
            ve.init(velocityConfig);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SysException("init VelocityEngine error.");
        }
    }

    public void setVelocityConfig(Properties velocityConfig) {
        this.velocityConfig = velocityConfig;
    }

    /**
	 * Map<String, DeskletDef>
	 */
    public Map load(WebApp app) {
        this.app = app;
        init();
        String propertyFile = app.getConfig(DESKLET_PROPERTY_FILE_CONFIG);
        if (propertyFile == null) propertyFile = DEFAULT_PROPERTY_FILE_NAME;
        log.info("Desklet properties files : " + propertyFile);
        String[] properties = propertyFile.split(";");
        Map map = new HashMap();
        for (int i = 0; i < properties.length; i++) {
            load(map, properties[i], app);
        }
        return map;
    }

    private void load(Map map, String property, WebApp app) {
        Resource[] resources = app.getResources(property);
        try {
            for (int i = 0; i < resources.length; i++) {
                if (resources[i].exists()) {
                    URL url = resources[i].getURL();
                    if (url == null) {
                        log.info("Desklet properties file " + property + " not found.");
                        return;
                    }
                    log.info("Load property from " + url);
                    Properties pp = new Properties();
                    pp.load(url.openStream());
                    Iterator iter = pp.entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry entry = (Entry) iter.next();
                        String name = (String) entry.getValue();
                        String path = (String) entry.getKey();
                        DeskletDef def = loadDef(path, name);
                        if (map.get(path) != null) {
                            throw new SysException("Load desklet file " + def + " error. same path desklet " + map.get(path) + " already exist.");
                        }
                        map.put(path, def);
                    }
                    log.info("Load property from " + url + " finished.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new SysException("Load property file " + property + " error.");
        }
    }

    private DeskletDef loadDef(String path, String name) {
        Class theClass = null;
        try {
            theClass = Class.forName(name);
            String temp = "/" + name.replace('.', '/') + ".class";
            URL url = DeskletClasspathLoader.class.getResource(temp);
            if (log.isDebugEnabled()) log.debug("Load desklet [" + path + "] java class: " + url == null ? temp : url.getPath() + ".");
        } catch (ClassNotFoundException e) {
            theClass = defaultDeskletClass;
            if (log.isDebugEnabled()) log.debug("Load desklet [" + path + "] java class: " + defaultDeskletClass.getName() + ".");
        }
        DeskletDef def = new DeskletDef(path, name, theClass);
        loadForm(def, theClass, name);
        loadHtml(def, theClass, name);
        if (theClass != null) {
            Iterator iter = parsers.iterator();
            while (iter.hasNext()) {
                ((MappingParser) iter.next()).parse(def, theClass);
            }
        }
        return def;
    }

    private void loadForm(DeskletDef def, Class theClass, String name) {
        String formFileName = name.replace('.', '/') + "." + javaScriptFileExt;
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (theClass != null) classLoader = theClass.getClassLoader();
        URL url = classLoader.getResource(formFileName);
        if (url == null) {
            if (log.isDebugEnabled()) log.debug("Load desklet [" + def.getPath() + "] javascript: NULL .");
            return;
        }
        try {
            String form = Utils.read(url);
            def.setPageDef(form);
            def.setPageTemplate(createTemplate(formFileName, form));
            if (log.isDebugEnabled()) log.debug("Load desklet [" + def.getPath() + "] javascript: " + url.getPath() + ".");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SysException("Load " + formFileName + " error when load desklet definite. " + theClass);
        }
    }

    private void loadHtml(DeskletDef def, Class theClass, String name) {
        String shortFilename = name.replace('.', '/');
        String htmlFileName = shortFilename + "." + htmlFileExt;
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (theClass != null) classLoader = theClass.getClassLoader();
        URL url = classLoader.getResource(htmlFileName);
        try {
            if (url == null) {
                DefaultHtmlFileInfo info = getDefaultHtmlInfo(shortFilename, classLoader);
                if (info != null) {
                    def.setHtmlDef(info.text);
                    def.setHtmlTemplate(info.template);
                    if (log.isDebugEnabled()) log.debug("Load desklet [" + def.getPath() + "] html file: " + info.url + ".");
                }
            } else {
                String htmlText = Utils.read(url);
                def.setHtmlDef(htmlText);
                def.setHtmlTemplate(createTemplate(htmlFileName, htmlText));
                if (log.isDebugEnabled()) log.debug("Load desklet [" + def.getPath() + "]  html file: " + url.getPath() + ".");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SysException("Load " + htmlFileName + " error when load desklet definite. " + def);
        }
    }

    private DefaultHtmlFileInfo getDefaultHtmlInfo(String className, ClassLoader classLoader) throws IOException {
        int lastIndex = 0;
        String defaultHtmlFileName = app.getConfig(DEFAULT_HTML_FILE_CONFIG);
        if (defaultHtmlFileName == null) defaultHtmlFileName = DEFAULT_TEMPLATE_HTML_FILE_NAME;
        do {
            String htmlFileName = null;
            lastIndex = className.lastIndexOf("/");
            if (lastIndex == -1) htmlFileName = defaultHtmlFileName; else {
                className = className.substring(0, lastIndex);
                htmlFileName = className + "/" + defaultHtmlFileName;
            }
            DefaultHtmlFileInfo info = (DefaultHtmlFileInfo) defaultHtmlsMap.get(htmlFileName);
            if (info != null) return info;
            URL url = classLoader.getResource(htmlFileName);
            if (url != null) {
                info = new DefaultHtmlFileInfo();
                info.text = Utils.read(url);
                info.template = createTemplate(htmlFileName, info.text);
                info.url = url.getPath();
                defaultHtmlsMap.put(htmlFileName, info);
                return info;
            }
        } while (lastIndex != -1);
        if (log.isDebugEnabled()) log.debug("Not found default html template file " + defaultHtmlFileName + " . ");
        return null;
    }

    private Template createTemplate(String fileName, String text) {
        try {
            if (text.indexOf("$") == -1 && text.indexOf("#") == -1) return null;
            return ve.getTemplate(fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SysException("Create template error for " + fileName + ".");
        }
    }

    public void setVe(VelocityEngine ve) {
        this.ve = ve;
    }

    public void setMappingParsers(Properties mappingParsers) {
        this.mappingParsers = mappingParsers;
    }

    public void setHtmlFileExt(String htmlFileExt) {
        this.htmlFileExt = htmlFileExt;
    }

    public void setJavaScriptFileExt(String javaScriptFileExt) {
        this.javaScriptFileExt = javaScriptFileExt;
    }

    private class DefaultHtmlFileInfo {

        public String text;

        public String url;

        public Template template;
    }
}
