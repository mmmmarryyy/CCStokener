package net.sf.appia.jgcs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import net.sf.jgcs.JGCSException;

/**
 * This class defines a AppiaServiceList
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class AppiaServiceList {

    private static final String PROPS_FILE = "/services.properties";

    private static Properties services = null;

    private static boolean booted = false;

    private AppiaServiceList() {
    }

    public static void boot() throws JGCSException {
        if (booted) return;
        services = new Properties();
        try {
            URL props_url = AppiaServiceList.class.getResource(PROPS_FILE);
            if (props_url == null) throw new IOException("Could not find properties file: " + PROPS_FILE);
            InputStream is = props_url.openStream();
            services.load(is);
            is.close();
        } catch (IOException e) {
            throw new JGCSException("Unable to boot the Services List.", e);
        }
    }

    public static Integer getValueForService(String s) throws JGCSException {
        boot();
        if (exists(s)) return Integer.parseInt(services.getProperty(s)); else throw new JGCSException("Service does not exist.");
    }

    public static boolean exists(String s) throws JGCSException {
        boot();
        return services.getProperty(s) != null;
    }
}
