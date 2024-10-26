package net.stickycode.mockwire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AnnotationFinder {

    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation>[] load(String group, String type) {
        String name = "META-INF/" + group + "/" + type + ".annotations";
        List<Class<? extends Annotation>> annotations = new ArrayList<Class<? extends Annotation>>();
        Enumeration<URL> controlNames = loadResources(name);
        for (URL url : Collections.list(controlNames)) {
            loadUrl(annotations, url);
        }
        return (Class<? extends Annotation>[]) annotations.toArray(new Class[annotations.size()]);
    }

    private static Enumeration<URL> loadResources(String name) {
        try {
            return AnnotationFinder.class.getClassLoader().getResources(name);
        } catch (IOException e) {
            throw new RuntimeException("Could not find annotation manifest " + name, e);
        }
    }

    private static void loadUrl(List<Class<? extends Annotation>> annotations, URL url) {
        try {
            loadUrlStream(annotations, url);
        } catch (IOException e) {
            throw new RuntimeException("Could not find annotation manifest " + url, e);
        }
    }

    private static void loadUrlStream(List<Class<? extends Annotation>> annotations, URL url) throws IOException {
        InputStream stream = url.openStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                annotations.add(loadClass(line));
            }
        } finally {
            stream.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadClass(String line) {
        Class<?> klass = unsafeLoadClass(line);
        if (!Annotation.class.isAssignableFrom(klass)) throw new RuntimeException(line + " was supposed to be an annotation but its not!");
        return (Class<? extends Annotation>) klass;
    }

    private static Class<?> unsafeLoadClass(String line) {
        try {
            return AnnotationFinder.class.getClassLoader().loadClass(line);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cound not load class for annotation " + line, e);
        }
    }
}
