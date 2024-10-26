package org.soqqo.vannitator.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import org.soqqo.vannitator.annotation.VannitationType;
import org.soqqo.vannitator.processors.ConfigBean;

/**
 * Manager class of @VannitateOneToOne annotated Annotations. it (a) knows which
 * annotations we supported (b) knows which classes have those annotations
 * 
 * @author rbuckland
 * 
 */
public class AnnotationManager {

    /**
     * The name of the file we expect to find in META-INF which will list the
     * annotations we are looking for.
     */
    Logger logger = Logger.getLogger(AnnotationManager.class.getName());

    @SuppressWarnings("rawtypes")
    private HashMap<KeyPair, ConfigBean> configAnnotations = new HashMap<KeyPair, ConfigBean>();

    private static AnnotationManager instance;

    private HashMap<VannitationType, HashSet<String>> supportedAnnotationTypes = new HashMap<VannitationType, HashSet<String>>();

    public HashSet<String> getSupportedAnnotationTypes(VannitationType type) {
        return supportedAnnotationTypes.get(type);
    }

    private AnnotationManager() {
        loadSupportedAnnotationTypes(VannitationType.VannitateOneToOne);
        loadSupportedAnnotationTypes(VannitationType.VannitateManyToOne);
    }

    public static AnnotationManager instance() {
        if (null == instance) {
            instance = new AnnotationManager();
        }
        return instance;
    }

    @SuppressWarnings("rawtypes")
    public ConfigBean get(KeyPair key) {
        return configAnnotations.get(key);
    }

    @SuppressWarnings("rawtypes")
    public ConfigBean put(KeyPair key, ConfigBean value) {
        return configAnnotations.put(key, value);
    }

    /**
     * A tuple of the annotatedClassFQN and the annotation. This is the key to
     * our cache.
     * 
     * @author rbuckland
     * 
     */
    class KeyPair {

        private String annotatedClassFQN;

        private String annotationClassFQN;

        public KeyPair(String annotatedClassFQN, String annotationClassFQN) {
            this.annotatedClassFQN = annotatedClassFQN;
            this.annotationClassFQN = annotationClassFQN;
        }

        public int hashCode() {
            return (37 * annotationClassFQN.hashCode()) + (23 * annotatedClassFQN.hashCode());
        }
    }

    /**
     * Return all (if any) of our supported annotations found on the class
     * 
     * @return
     */
    public Annotation getSupportedConfAnnotations(VannitationType baseVannitationType, String annotatedClassFQN, String annotationClassFQN) {
        for (Annotation a : getAnnotationsFromFQN(annotatedClassFQN)) {
            if (supportedAnnotationTypes.get(baseVannitationType).contains(annotationClassFQN)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Returns null, if there is no annotation on that class. The ConfigBean
     * from Cache.
     * 
     * @param annotatedClassFQN
     * @param annotation
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> ConfigBean<T> getConfAnnotation(Class<T> baseVannitationClass, String annotatedClassFQN, String annotationClassFQN) {
        String name = baseVannitationClass.getSimpleName();
        HashSet<String> val = supportedAnnotationTypes.get(VannitationType.valueOf(name));
        if (!val.contains(annotationClassFQN)) {
            logger.fine("Annotation [" + annotatedClassFQN + "] is not supported. We support: " + val);
            return null;
        }
        KeyPair k = new KeyPair(annotatedClassFQN, annotationClassFQN);
        if (configAnnotations.containsKey(k)) {
            return configAnnotations.get(k);
        } else {
            Annotation[] annotationsOnTheClass = getAnnotationsFromFQN(annotatedClassFQN);
            for (Annotation a : annotationsOnTheClass) {
                if (a.annotationType().getName().equals(annotationClassFQN)) {
                    ConfigBean<T> configBean = AnnotationConfigMerger.merge(baseVannitationClass, a);
                    configAnnotations.put(k, configBean);
                    return configBean;
                }
            }
            logger.fine("[" + annotatedClassFQN + "] appears not to be annotated with [" + annotationClassFQN + "] (hint: Have you set the retention policy of the annotation to RUNTIME ? @Retention(RetentionPolicy.RUNTIME))");
            return null;
        }
    }

    /**
     * Simple helper
     * 
     * @param fqn
     * @return Array of annotations on the class
     */
    public static Annotation[] getAnnotationsFromFQN(String fqn) {
        try {
            return Class.forName(fqn).getAnnotations();
        } catch (ClassNotFoundException e) {
            return new Annotation[] {};
        }
    }

    /**
     * Hunt the classpath and load all the <code>META-INF/VannitationAnnotations</code> files to a HashSet field
     * 
     * @param vannitationManytoone
     * 
     * @return
     */
    private HashSet<String> loadSupportedAnnotationTypes(VannitationType baseVannitationType) {
        Enumeration<URL> urls = null;
        try {
            urls = this.getClass().getClassLoader().getResources("META-INF/" + baseVannitationType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load the annotations we support", e);
        }
        supportedAnnotationTypes.put(baseVannitationType, new HashSet<String>());
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    supportedAnnotationTypes.get(baseVannitationType).add(line.trim());
                }
                reader.close();
            } catch (Exception e) {
                throw new RuntimeException("Could not open " + url);
            }
        }
        return supportedAnnotationTypes.get(baseVannitationType);
    }
}
