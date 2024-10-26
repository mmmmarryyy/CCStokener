package org.equanda.bindings;

import org.apache.tapestry.Binding;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

/**
 * Binding prefix which allows you to get a value from a manifest file. As multiple files may contain a value for the
 * same key, the first encountered is used.
 *
 * @author <a href="mailto:joachim@progs.be">Joachim Van der Auwera</a>
 */
public class ManifestBindingPrefix implements Binding {

    private static final Map<String, String> cache = new ConcurrentHashMap<String, String>();

    private String expression;

    public ManifestBindingPrefix(String expression) {
        this.expression = expression;
    }

    public Object get() {
        try {
            String res = cache.get(expression);
            if (res != null) return res;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (null == cl) cl = this.getClass().getClassLoader();
            Enumeration<URL> urls = cl.getResources("META-INF/MANIFEST.MF");
            while (null == res && urls.hasMoreElements()) {
                URL url = urls.nextElement();
                res = get(url);
            }
            if (res != null) cache.put(expression, res);
            return res;
        } catch (Exception ex) {
            return null;
        }
    }

    private String get(URL url) throws IOException {
        InputStream in = null;
        try {
            in = url.openStream();
            Manifest mf = new Manifest(in);
            return mf.getMainAttributes().getValue(expression);
        } finally {
            if (null != in) in.close();
        }
    }

    @SuppressWarnings("unchecked")
    public Class getBindingType() {
        return String.class;
    }

    public boolean isInvariant() {
        return false;
    }

    public void set(Object value) {
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }
}
