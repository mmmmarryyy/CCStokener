package org.freeworld.jmultiplug.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.freeworld.jmultiplug.intl.IntlString;
import org.freeworld.jmultiplug.logging.Logger;
import static org.freeworld.jmultiplug.config.ConfigIntlStrings.*;

public class Preloader {

    private static final String PRE_LOAD_CLASS_DEFINITION = "class";

    private static final IntlString INTL_PRELOAD_FAILURE = new IntlString(Preloader.class, "INTL_PRELOAD_FAILURE", "Couldn''t pre-load {0} at root key {1}");

    public static void safePreload(String initializerType, String preloadMasterKey) {
        try {
            propertiesPreloader(initializerType, preloadMasterKey);
            spiPreloader(initializerType, preloadMasterKey);
        } catch (Throwable t) {
            try {
                Logger.warn(INTL_PRELOAD_FAILURE.setArgs(initializerType, preloadMasterKey), t);
            } catch (Throwable t1) {
                t1.printStackTrace();
            }
        }
    }

    protected static void propertiesPreloader(String initializerType, String preloadMasterKey) {
        String className = null;
        try {
            List<String> keys = Config.getNestedKeys(preloadMasterKey);
            if (keys != null) {
                for (int i = 0; i < keys.size(); i++) {
                    className = Config.getProperty(keys.get(i));
                    try {
                        Class.forName(className);
                        if (Logger.isInitialized()) Logger.debug(INTL_PLUGIN_PRELOADED.setArgs(className, initializerType));
                    } catch (Throwable t) {
                        if (Logger.isInitialized()) Logger.warn(INTL_PLUGIN_INITIALIZER_ERROR.setArgs(className, initializerType), t); else t.printStackTrace();
                    }
                }
            }
        } catch (Throwable t) {
            if (Logger.isInitialized()) Logger.warn(INTL_INITIALIZER_TYPE_ERROR.setArgs(initializerType, preloadMasterKey), t); else t.printStackTrace();
        }
    }

    protected static void spiPreloader(String initializerType, String preloadMasterKey) {
        try {
            Enumeration<URL> nums = ClassLoader.getSystemResources("META-INF/services/autoLoader/" + preloadMasterKey);
            Class<?> cls = null;
            List<String> enumeratedFields = new ArrayList<String>();
            while (nums.hasMoreElements()) {
                URL url = null;
                Properties props = null;
                InputStream is = null;
                url = nums.nextElement();
                is = url.openStream();
                props = new Properties();
                props.load(is);
                is.close();
                if (ValueEvaluator.isSet(props.getProperty(PRE_LOAD_CLASS_DEFINITION))) {
                    enumeratedFields.add(props.getProperty(PRE_LOAD_CLASS_DEFINITION));
                }
                for (String key : Config.getNestedKeys(props, PRE_LOAD_CLASS_DEFINITION + ".")) {
                    enumeratedFields.add(props.getProperty(key));
                }
            }
            for (String clsName : enumeratedFields) {
                try {
                    cls = Class.forName(clsName);
                    if (Logger.isInitialized()) Logger.debug(INTL_PLUGIN_PRELOADED.setArgs(cls.getName(), initializerType));
                } catch (Exception e) {
                    if (Logger.isInitialized()) Logger.warn(INTL_PLUGIN_INITIALIZER_ERROR.setArgs(clsName, initializerType), e);
                }
            }
        } catch (IOException e) {
            Logger.warn(INTL_PRELOAD_CANNOT_FIND_PATH.setArgs(preloadMasterKey, initializerType), e);
        }
    }
}
