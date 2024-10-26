package util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Simple utility for loading properties files from the classpath.
 * 
 * @author Michael Tremel (mtremel@email.arizona.edu)
 */
public class PropertyLoader {

    /**
	 * Returns an instance of Properties loaded with the data from the specified
	 * path. This path should be from the root of the classpath. This static
	 * method is especially useful in loading configuration files.
	 * 
	 * @param path
	 *            classpath path to the properties file, including the extension
	 * @return Properties instance with loaded values
	 */
    public static Properties loadProperties(String path) {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource(path);
        try {
            props.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
