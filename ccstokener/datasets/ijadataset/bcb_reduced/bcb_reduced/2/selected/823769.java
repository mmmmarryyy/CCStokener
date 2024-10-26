package org.iocframework.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.iocframework.Factory;
import org.iocframework.Singleton;
import com.taliasplayground.io.PropertiesUtils;

/**
 * @author David M. Sledge
 */
public class PropertiesFactoryStaffer {

    public static Factory hireWorkers(URL url, Factory parent) throws IOException {
        Factory factory = new Factory(parent);
        Map<String, String> props = new HashMap<String, String>();
        InputStream is = url.openStream();
        try {
            PropertiesUtils.load(is, props);
        } finally {
            is.close();
        }
        for (Entry<? extends String, ? extends String> entry : props.entrySet()) {
            String key = entry.getKey();
            factory.setManager(key, new Singleton(entry.getValue(), url + " " + key));
        }
        return factory;
    }
}
