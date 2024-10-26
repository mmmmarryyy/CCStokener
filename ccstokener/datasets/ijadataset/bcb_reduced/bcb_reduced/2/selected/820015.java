package gr.fresh.trash.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class ResourceUtils {

    /**
	 * @param name
	 * @return
	 */
    public static URL getResource(String name) {
        return ResourceUtils.class.getResource(name);
    }

    /**
	 * @param name
	 * @return
	 */
    public static InputStream getResourceAsStream(String name) {
        InputStream is = ResourceUtils.class.getResourceAsStream(name);
        return is;
    }

    /**
	 * @param string
	 * @return
	 */
    public static Properties loadProperties(String string) {
        Properties properties = new Properties();
        URL url = ResourceUtils.getResource(string);
        try {
            properties.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
