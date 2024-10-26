package com.liferay.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import com.dotmarketing.util.Logger;

/**
 * <a href="ExtPropertiesLoader.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class ExtPropertiesLoader {

    public void init(String name) {
        Properties p = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            URL url = classLoader.getResource(name + ".properties");
            if (url != null) {
                InputStream is = url.openStream();
                p.load(is);
                is.close();
                Logger.info(this, "Loading " + url);
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
        try {
            URL url = classLoader.getResource(name + "-ext.properties");
            if (url != null) {
                InputStream is = url.openStream();
                p.load(is);
                is.close();
                Logger.info(this, "Loading " + url);
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
        PropertiesUtil.fromProperties(p, _props);
    }

    public boolean containsKey(String key) {
        return _props.containsKey(key);
    }

    public String get(String key) {
        return (String) _props.get(key);
    }

    public void set(String key, String value) {
        _props.put(key, value);
    }

    public String[] getArray(String key) {
        String value = get(key);
        if (value == null) {
            return new String[0];
        } else {
            return StringUtil.split(value);
        }
    }

    public Properties getProperties() {
        return PropertiesUtil.fromMap(_props);
    }

    private Map _props = CollectionFactory.getSyncHashMap();
}
