package org.snipsnap.util;

import org.radeox.util.logging.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * After the Service class from Sun and the Apache project.
 * With help from Fr�d�ric Miserey.
 *
 * @author Matthias L. Jugel
 * @version $id$
 */
public class Service {

    static HashMap services = new HashMap();

    public static synchronized Iterator providers(Class cls) {
        ClassLoader classLoader = cls.getClassLoader();
        String providerFile = "META-INF/services/" + cls.getName();
        List providers = (List) services.get(providerFile);
        if (providers != null) {
            return providers.iterator();
        }
        providers = new ArrayList();
        services.put(providerFile, providers);
        try {
            Enumeration providerFiles = classLoader.getResources(providerFile);
            while (providerFiles.hasMoreElements()) {
                try {
                    URL url = (URL) providerFiles.nextElement();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                    String line = reader.readLine();
                    while (line != null) {
                        try {
                            int idx = line.indexOf('#');
                            if (idx != -1) {
                                line = line.substring(0, idx);
                            }
                            line = line.trim();
                            if (line.length() > 0) {
                                Object obj = classLoader.loadClass(line).newInstance();
                                providers.add(obj);
                            }
                        } catch (Exception ex) {
                        }
                        line = reader.readLine();
                    }
                } catch (Exception ex) {
                }
            }
        } catch (IOException ioe) {
        }
        return providers.iterator();
    }
}
