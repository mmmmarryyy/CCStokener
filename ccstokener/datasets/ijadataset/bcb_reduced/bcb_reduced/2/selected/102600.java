package org.illico.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public final class PropertiesUtils {

    private PropertiesUtils() {
    }

    public static Properties load(String mappingFile) throws IOException {
        return load(getMappingURLs(mappingFile));
    }

    public static Properties load(Collection<URL> urls) throws IOException {
        Properties result = new Properties();
        for (URL url : urls) {
            InputStream is = url.openStream();
            try {
                if (url.getFile().endsWith(".xml")) {
                    result.loadFromXML(is);
                } else {
                    result.load(is);
                }
            } finally {
                is.close();
            }
        }
        return result;
    }

    public static Collection<URL> getMappingURLs(String mappingFile) throws IOException {
        Collection<URL> urls = Collections.list(PropertiesUtils.class.getClassLoader().getResources(mappingFile + ".properties"));
        urls.addAll(Collections.list(PropertiesUtils.class.getClassLoader().getResources(mappingFile + ".xml")));
        return urls;
    }
}
