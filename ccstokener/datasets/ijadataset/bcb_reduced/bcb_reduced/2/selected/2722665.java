package fi.arcusys.acj.util.spring;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;
import org.slf4j.Logger;
import fi.arcusys.acj.util.LoggerFactoryUtil;

/**
 * @todo Add documentation to type ApplicationProperties
 * @since 0.10
 * @author mikko
 * @copyright (C) 2009 Arcusys Oy
 */
public class ApplicationProperties extends Properties {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactoryUtil.getLoggerIfAvailable(ApplicationProperties.class);

    public static final String SHARED_RESOURCE_NAME = "application.properties";

    public static final String LOCAL_RESOURCE_NAME = "application.local.properties";

    public static final String PROPERTY_NAME = "application.name";

    public static final String PROPERTY_DEFAULT_PROFILE = "application.defaultProfile";

    public static final String PROPERTY_CURRENT_PROFILE = "application.profile";

    public static final String PROFILE_TEST = "test";

    public static final String PROFILE_DEVELOPMENT = "development";

    public static final String PROFILE_PRODUCTION = "production";

    public static final String DEFAULT_PROFILE = PROFILE_PRODUCTION;

    public static final String DEFAULT_NAME = "application";

    public ApplicationProperties() {
        super();
    }

    public ApplicationProperties(Properties defaults) {
        super(defaults);
    }

    void doLoad(String resName, URL url, boolean requireExists) throws IOException {
        if (null == url && requireExists) {
            throw new FileNotFoundException("Resource " + resName + " does not exist");
        } else if (null == url) {
            LOG.warn("Resource {} does not exist", resName);
        } else {
            Reader rdr = null;
            try {
                rdr = new InputStreamReader(url.openStream());
                super.load(rdr);
            } finally {
                try {
                    rdr.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * Load contents of resources "application.properties" and
	 * optionally "application.local.properties" using the
	 * specified {@link ClassLoader}.
	 * 
	 * @param classLoader the <code>ClassLoader</code> for loading the 
	 *        <code>application.properties</code> resource
	 * @param requireExists specifies if the method should throw a
	 *        {@link FileNotFoundException} if the resource does not exist
	 * @throws IOException if an I/O error occurrs while reading contents of
	 *         the resource or if the resource doesn't exist and parameter
	 *         <code>requireExists</code> is set to <code>true</code>
	 */
    public synchronized void load(ClassLoader classLoader, boolean requireExists) throws IOException {
        URL url = classLoader.getResource(SHARED_RESOURCE_NAME);
        doLoad(SHARED_RESOURCE_NAME, url, requireExists);
        url = classLoader.getResource(LOCAL_RESOURCE_NAME);
        if (null != url) {
            doLoad(LOCAL_RESOURCE_NAME, url, false);
        }
    }

    /**
	 * Load contents of resources "application.properties" and
	 * optionally "application.local.properties" using the
	 * specified {@link Class}.
	 * 
	 * @param clazz the <code>Class</code> for loading the 
	 *        <code>application.properties</code> resource
	 * @param requireExists specifies if the method should throw a
	 *        {@link FileNotFoundException} if the resource does not exist
	 * @throws IOException if an I/O error occurrs while reading contents of
	 *         the resource or if the resource doesn't exist and parameter
	 *         <code>requireExists</code> is set to <code>true</code>
	 */
    public synchronized void load(Class<?> clazz, boolean requireExists) throws IOException {
        URL url = clazz.getResource("/" + SHARED_RESOURCE_NAME);
        doLoad(SHARED_RESOURCE_NAME, url, requireExists);
        url = clazz.getResource("/" + LOCAL_RESOURCE_NAME);
        if (null != url) {
            doLoad(LOCAL_RESOURCE_NAME, url, false);
        }
    }

    /**
	 * Load contents of resources "application.properties" and (optionally)
	 * "application.local.properties".
	 * 
	 * <p>Internally this method uses the context class loader of the current
	 * thread and calls {@link #load(ClassLoader, boolean)}.</p>
	 * 
	 * @param requireExists specifies if the method should throw a
	 *        {@link FileNotFoundException} if the resource does not exist
	 * @throws IOException if an I/O error occurrs while reading contents of
	 *         the resource or if the resource doesn't exist and parameter
	 *         <code>requireExists</code> is set to <code>true</code>
	 */
    public synchronized void load(boolean requireExists) throws IOException {
        ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
        load(ctxLoader, requireExists);
    }
}
