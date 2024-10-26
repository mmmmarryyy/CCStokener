package com.fourspaces.scratch.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import com.fourspaces.scratch.ScratchException;

/**
 * 
 * A default MappingRegistry that auto-discovers Controllers by looking for
 * files named "com.fourspaces.scratch.Controller". These files should contain
 * the absolute class name of a Controller to auto-configure.
 * <p>
 * ControllerMappings are created from BasicControllerMapping, which uses cached instances (singletons)
 * to process requests.
 * 
 */
public class BasicMappingRegistry extends AbstractRegExMappingRegistry {

    /**
	 * 
	 */
    private static final long serialVersionUID = 3916593992208904599L;

    public static final String REGISTRY_FILENAME = "com.fourspaces.scratch.Controller";

    /**
	 * Initializes the Registry. Auto-discovers controllers to load from files named com.fourspaces.scratch.Controller. 
	 * @throws ScratchException 
	 */
    public void init(ServletContext context) throws ScratchException {
        try {
            log.debug("Attempting to load Controllers from file: " + REGISTRY_FILENAME);
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = classLoader.getResources(REGISTRY_FILENAME);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                log.debug("Found: " + url);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String className = null;
                while ((className = reader.readLine()) != null) {
                    className = className.trim();
                    if (!"".equals(className) && !className.startsWith("#")) {
                        log.debug("Found class: " + className);
                        Class<?> clazz = classLoader.loadClass(className);
                        addClass(clazz);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log.error(e);
        }
    }

    protected ControllerMapping buildControllerMapping(Class<?> clazz, Method method) {
        return new BasicControllerMapping(clazz, method);
    }
}
