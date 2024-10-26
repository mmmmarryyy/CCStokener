package dash.obtain.initialize;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class provides bootstrapping configuration information for Dash Obtain.
 *
 * <p>Properties are looked up in the following order:
 * <ol>
 *   <li>System Properties prefixed with "dash.obtain."</li>
 *   <li>Values in the property file "/dash-obtain.properties"</li>
 *   <li>Values in the property file "/default-dash-obtain.properties"</li>
 * </ol>
 *
 * @author jheintz
 *
 */
public class Config {

    public static final String DASH_OBTAIN_PROPERTIES = "dash-obtain.properties";

    public static final String DEFAULT_DASH_OBTAIN_PROPERTIES = "default-dash-obtain.properties";

    public static final String PREFIX = "dash.obtain.";

    static Map<String, String> properties;

    public static synchronized String getProperty(String key) {
        String result = System.getProperty(PREFIX + key);
        if (result == null || "".equals(result)) result = getProperties().get(key);
        return result;
    }

    public static synchronized String getProperty(String key, String defaultValue) {
        String result = getProperty(key);
        if (result == null || "".equals(result)) result = defaultValue;
        return result;
    }

    public static synchronized Map<String, String> getProperties() {
        if (properties == null) {
            Map<String, String> map = new HashMap<String, String>();
            tryLoad(map, DEFAULT_DASH_OBTAIN_PROPERTIES);
            tryLoad(map, DASH_OBTAIN_PROPERTIES);
            properties = Collections.unmodifiableMap(map);
        }
        return properties;
    }

    static void tryLoad(Map<String, String> map, String uri) {
        URL url = Config.class.getClassLoader().getResource(uri);
        Properties props = new Properties();
        if (url != null) {
            InputStream is = null;
            try {
                is = url.openStream();
                props.load(is);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    ;
                }
            }
        }
        for (Object key : props.keySet()) {
            map.put((String) key, (String) props.get(key));
        }
    }
}
