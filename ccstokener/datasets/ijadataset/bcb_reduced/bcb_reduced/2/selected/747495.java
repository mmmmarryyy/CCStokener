package org.apache.myfaces.trinidad.bean;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;

/**
 * Base interface for FacesBean storage.
 *
 */
public class FacesBeanFactory {

    public static FacesBean createFacesBean(Class<?> ownerClass, String rendererType) {
        if (ownerClass == null) return null;
        String className = ownerClass.getName();
        FacesBean bean = createFacesBean(className, rendererType);
        if (bean == null && rendererType != null) bean = createFacesBean(className, null);
        if (bean == null) bean = createFacesBean(ownerClass.getSuperclass(), rendererType);
        return bean;
    }

    public static FacesBean createFacesBean(String beanType, String rendererType) {
        String typeKey = (rendererType != null) ? beanType + "|" + rendererType : beanType;
        String className = (String) _TYPES_MAP.get(typeKey);
        if (className == null) return null;
        try {
            Class<?> type = _getClassLoader().loadClass(className);
            return (FacesBean) type.newInstance();
        } catch (ClassNotFoundException cnfe) {
            _LOG.severe("CANNOT_FIND_FACESBEAN", className);
            _LOG.severe(cnfe);
        } catch (IllegalAccessException iae) {
            _LOG.severe("CANNOT_CREATE_FACESBEAN_INSTANCE", className);
            _LOG.severe(iae);
        } catch (InstantiationException ie) {
            _LOG.severe("CANNOT_CREATE_FACESBEAN_INSTANCE", className);
            _LOG.severe(ie);
        }
        return null;
    }

    private static void _initializeBeanTypes() {
        _TYPES_MAP = new HashMap<Object, Object>();
        List<URL> list = new ArrayList<URL>();
        try {
            Enumeration<URL> en = _getClassLoader().getResources("META-INF/faces-bean.properties");
            while (en.hasMoreElements()) {
                list.add(en.nextElement());
            }
            Collections.reverse(list);
        } catch (IOException ioe) {
            _LOG.severe(ioe);
            return;
        }
        if (list.isEmpty()) {
            if (_LOG.isInfo()) _LOG.info("NO_FACES_BEAN_PROPERTIES_FILES_LOCATED");
        }
        for (URL url : list) {
            _initializeBeanTypes(url);
        }
    }

    private static void _initializeBeanTypes(URL url) {
        try {
            Properties properties = new Properties();
            InputStream is = url.openStream();
            try {
                properties.load(is);
                if (_LOG.isFine()) _LOG.fine("Loading bean factory info from " + url);
                _TYPES_MAP.putAll(properties);
            } finally {
                is.close();
            }
        } catch (IOException ioe) {
            _LOG.severe("CANNOT_LOAD_URL", url);
            _LOG.severe(ioe);
        }
    }

    private static ClassLoader _getClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = FacesBeanFactory.class.getClassLoader();
        return loader;
    }

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(FacesBeanFactory.class);

    private static Map<Object, Object> _TYPES_MAP;

    static {
        _initializeBeanTypes();
    }
}
