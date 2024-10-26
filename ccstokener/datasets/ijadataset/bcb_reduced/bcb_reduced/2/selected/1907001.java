package org.cheetah.core.language;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.core.util.ResourceUtils;

public abstract class Languages {

    private static ConcurrentHashMap<String, Language> map = new ConcurrentHashMap<String, Language>();

    static {
        URL[] urls = ResourceUtils.getURLs("classpath:META-INF/cheetah/cheetah.languages");
        if (urls.length > 0) {
            for (int i = 0; i < urls.length; ++i) {
                addLanguages(urls[i]);
            }
        }
    }

    public static void addLanguages(URL url) {
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
                addLanguage((String) entry.getKey(), (Language) cla.newInstance());
            } catch (Throwable t) {
            }
        }
    }

    public static void addLanguage(String name, Language lang) {
        if (map.putIfAbsent(name, lang) != null) {
            throw new IllegalArgumentException("language name " + name + " already exists");
        }
    }

    public static void removeLanguage(String name) {
        map.remove(name);
    }

    public static CompiledExpression compile(String name, String expression) throws Exception {
        Language lang = map.get(name);
        if (lang == null) {
            throw new IllegalArgumentException("unsupported language " + name);
        }
        return lang.compile(expression);
    }
}
