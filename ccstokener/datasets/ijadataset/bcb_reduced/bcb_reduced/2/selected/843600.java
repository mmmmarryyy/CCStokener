package net.sf.webwarp.util.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

/**
 * @author  atr
 */
public class WebwarpAnnotationSessionFactoryBean extends AnnotationSessionFactoryBean {

    private static final Logger log = Logger.getLogger(WebwarpAnnotationSessionFactoryBean.class);

    public static final String PERSISTENT_BEANS_DEFINITION_LOCATION = "META-INF/persistentBeans.properties";

    public static final String XML_MAPPING_SUFFIX = ".hbm.xml";

    private static final String JODA_TYPES_PACKAGE_NAME = "net.sf.webwarp.util.joda.hibernate";

    private static final String JODA_TYPES_PACKAGE_INFO_CLASS_NAME = JODA_TYPES_PACKAGE_NAME + ".package-info";

    private static final String HIBERNAT_POJO_PACKAGE_NAME = "net.sf.webwarp.util.hibernate.dao";

    private static final String HIBERNAT_POJO_PACKAGE_INFO_CLASS_NAME = HIBERNAT_POJO_PACKAGE_NAME + ".package-info";

    private List<String> excludedModuleNames = new ArrayList<String>();

    @Override
    protected void postProcessAnnotationConfiguration(AnnotationConfiguration config) throws HibernateException {
        Set<String> classes = loadBeanClasses();
        for (String className : classes) {
            if (!StringUtils.isBlank(className)) {
                className = className.trim();
                if (log.isDebugEnabled()) {
                    log.debug("add resource: " + className);
                }
                if (className.endsWith(XML_MAPPING_SUFFIX)) {
                    config.addResource(className);
                } else {
                    try {
                        Class<?> clazz = ClassUtils.getClass(className);
                        config.addAnnotatedClass(clazz);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (existsJodaPackage()) {
            config.addPackage(JODA_TYPES_PACKAGE_NAME);
        }
        if (existsHibernatePojoPackage()) {
            config.addPackage(HIBERNAT_POJO_PACKAGE_NAME);
        }
    }

    private boolean existsJodaPackage() {
        try {
            org.springframework.util.ClassUtils.getDefaultClassLoader().loadClass(JODA_TYPES_PACKAGE_INFO_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean existsHibernatePojoPackage() {
        try {
            org.springframework.util.ClassUtils.getDefaultClassLoader().loadClass(HIBERNAT_POJO_PACKAGE_INFO_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> loadBeanClasses() {
        Set<String> results = new HashSet<String>();
        try {
            Enumeration<URL> resources = org.springframework.util.ClassUtils.getDefaultClassLoader().getResources(PERSISTENT_BEANS_DEFINITION_LOCATION);
            Module: while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                InputStream is = null;
                try {
                    URLConnection con = url.openConnection();
                    con.setUseCaches(false);
                    is = con.getInputStream();
                    List<String> lines = IOUtils.readLines(is, "ISO-8859-1");
                    String moduleName = null;
                    if (lines != null) {
                        for (Iterator<String> iterator = lines.iterator(); iterator.hasNext(); ) {
                            String line = iterator.next();
                            if (!line.startsWith("#")) {
                                if (moduleName == null) {
                                    moduleName = line;
                                    if (excludedModuleNames != null && excludedModuleNames.contains(moduleName)) {
                                        continue Module;
                                    }
                                } else {
                                    results.add(line);
                                }
                            }
                        }
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    /**
	 * @param excludedModuleNames
	 */
    public void setExcludedModuleNames(List<String> excludedModuleNames) {
        this.excludedModuleNames = excludedModuleNames;
    }
}
