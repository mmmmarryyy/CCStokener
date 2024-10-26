package org.cheetah.core.charset;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.core.util.ResourceUtils;

public abstract class Charsets {

    private static final ConcurrentHashMap<String, CharsetProvider> providers;

    static {
        providers = new ConcurrentHashMap<String, CharsetProvider>();
        URL[] urls = ResourceUtils.getURLs("classpath:META-INF/cheetah/cheetah.charsets");
        for (int i = 0; i < urls.length; ++i) {
            addProviders(urls[i]);
        }
    }

    public static void addProviders(URL url) {
        Reader reader = null;
        Properties prop = new Properties();
        try {
            reader = new InputStreamReader(url.openStream());
            prop.load(reader);
        } catch (Throwable t) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable t) {
                }
            }
        }
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            try {
                Class<?> cla = Class.forName((String) entry.getValue(), true, Thread.currentThread().getContextClassLoader());
                providers.put(((String) entry.getKey()).toUpperCase(), (CharsetProvider) cla.newInstance());
            } catch (Throwable t) {
            }
        }
    }

    public static void addProvider(String encoding, CharsetProvider provider) {
        providers.put(encoding, provider);
    }

    public static CharsetProvider getProvider(String encoding) {
        return providers.get(encoding.toUpperCase());
    }
}
