package jaxlib.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import jaxlib.lang.Classes;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: ServiceListScanner.java 1586 2006-01-26 20:56:24Z joerg_wassmer $
 */
public class ServiceListScanner<T> extends Object {

    private final LinkedHashSet<? extends ClassLoader> classLoaders;

    private final Class<T> serviceType;

    private String serviceListResourceName;

    private boolean acceptAbstract = false;

    private boolean acceptNonPublic = false;

    private boolean initClasses = false;

    public ServiceListScanner(final Class<T> serviceType, ClassLoader... classLoaders) {
        this(serviceType, Arrays.asList(classLoaders));
    }

    public ServiceListScanner(final Class<T> serviceType, Collection<? extends ClassLoader> classLoaders) {
        super();
        CheckArg.notNull(serviceType, "serviceType");
        CheckArg.notNull(classLoaders, "classLoaders");
        if (serviceType.isAnnotation()) {
            final Retention retention = serviceType.getAnnotation(Retention.class);
            if ((retention == null) || (retention.value() != RetentionPolicy.RUNTIME)) {
                throw new IllegalArgumentException("Specified annotation type is not a RUNTIME visible annotation: " + serviceType);
            } else {
                final Target target = serviceType.getAnnotation(Target.class);
                if (target != null) {
                    boolean haveTypeType = false;
                    for (final ElementType elementType : target.value()) {
                        if (elementType == ElementType.TYPE) {
                            haveTypeType = true;
                            break;
                        }
                    }
                    if (!haveTypeType) {
                        throw new IllegalArgumentException("Specified annotation type is not suitable to be used as class annotation: " + serviceType);
                    }
                }
            }
        } else {
            CheckArg.notArray(serviceType, "serviceType");
            CheckArg.notPrimitive(serviceType, "serviceType");
        }
        this.classLoaders = new LinkedHashSet<ClassLoader>(classLoaders);
        if (this.classLoaders.contains(null)) throw new IllegalArgumentException("Classloaders contains null: " + classLoaders);
        this.serviceListResourceName = "META-INF/services/" + serviceType.getName();
        this.serviceType = serviceType;
    }

    @SuppressWarnings("unchecked")
    protected boolean acceptClass(final ClassLoader classLoader, final Class<?> c) {
        final Class<T> serviceType = getServiceType();
        final int modifiers = c.getModifiers();
        return (c != serviceType) && (Modifier.isPublic(modifiers) || isAcceptNonPublic()) && (!Modifier.isAbstract(modifiers) || isAcceptAbstract()) && serviceType.isAnnotation() ? c.isAnnotationPresent((Class<? extends Annotation>) serviceType) : serviceType.isAssignableFrom(c) && Classes.classLoaderEquals(c, classLoader);
    }

    protected boolean acceptClassName(final ClassLoader classLoader, final URL serviceListResourceURL, final String className) {
        return true;
    }

    protected boolean acceptServiceListResourceURL(final ClassLoader classLoader, final URL url) {
        return true;
    }

    protected Class<?> findClass(final ClassLoader classLoader, final String className) {
        try {
            return Class.forName(className, isInitClasses(), classLoader);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    protected void handleIOException(final ClassLoader classLoader, final URL url, final IOException ex) throws IOException {
        if (url == null) {
            throw (IOException) new IOException("Exception while accessing classloader: " + classLoader).initCause(ex);
        } else {
            throw (IOException) new IOException("Exception accessing url " + url + " of classloader " + classLoader).initCause(ex);
        }
    }

    public Set<Class<?>> getClasses() throws IOException {
        final Map<? extends ClassLoader, Set<String>> namesByClassLoader = getClassNamesByClassLoader();
        final LinkedHashSet<Class<?>> classes = new LinkedHashSet<Class<?>>(namesByClassLoader.size());
        for (final Map.Entry<? extends ClassLoader, Set<String>> e : namesByClassLoader.entrySet()) {
            for (final String className : e.getValue()) {
                if (className != null) {
                    final Class<?> serviceClass = findClass(e.getKey(), className);
                    if ((serviceClass != null) && !classes.contains(serviceClass) && acceptClass(e.getKey(), serviceClass)) {
                        classes.add(serviceClass);
                    }
                }
            }
        }
        return classes;
    }

    public Map<? extends ClassLoader, Set<Class<?>>> getClassesByClassLoader() throws IOException {
        final Map<? extends ClassLoader, Set<String>> namesByClassLoader = getClassNamesByClassLoader();
        final LinkedHashMap<ClassLoader, Set<Class<?>>> map = new LinkedHashMap<ClassLoader, Set<Class<?>>>(namesByClassLoader.size());
        final HashSet<Class<?>> allClasses = new HashSet<Class<?>>(map.size());
        final boolean initClasses = isInitClasses();
        for (final Map.Entry<? extends ClassLoader, Set<String>> e : namesByClassLoader.entrySet()) {
            LinkedHashSet<Class<?>> classes = null;
            for (final String className : e.getValue()) {
                if (className != null) {
                    final Class<?> serviceClass = findClass(e.getKey(), className);
                    if ((serviceClass != null) && allClasses.add(serviceClass) && acceptClass(e.getKey(), serviceClass)) {
                        if (classes == null) classes = new LinkedHashSet<Class<?>>(e.getValue().size());
                        classes.add(serviceClass);
                    }
                }
            }
            if (classes != null) map.put(e.getKey(), classes);
        }
        return map;
    }

    public Map<? extends ClassLoader, Set<String>> getClassNamesByClassLoader() throws IOException {
        final Map<? extends ClassLoader, Set<URL>> urlsByClassLoader = getClassNameListURLsByClassLoader();
        final LinkedHashMap<ClassLoader, Set<String>> map = new LinkedHashMap<ClassLoader, Set<String>>(urlsByClassLoader.size());
        final HashSet<String> allNames = new HashSet<String>(map.size());
        for (final Map.Entry<? extends ClassLoader, Set<URL>> e : urlsByClassLoader.entrySet()) {
            LinkedHashSet<String> names = null;
            for (final URL url : e.getValue()) {
                InputStream bin = null;
                try {
                    bin = url.openStream();
                    final LineNumberReader in = new LineNumberReader(new InputStreamReader(bin));
                    for (String line; (line = in.readLine()) != null; ) {
                        line = line.trim();
                        if ((line.length() > 0) && (line.charAt(0) != '#') && allNames.add(line) && acceptClassName(e.getKey(), url, line)) {
                            if (names == null) names = new LinkedHashSet<String>(e.getValue().size());
                            names.add(line);
                        }
                    }
                    in.close();
                    bin = null;
                } catch (IOException ex) {
                    handleIOException(e.getKey(), url, ex);
                } finally {
                    if (bin != null) {
                        try {
                            bin.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
            if (names != null) map.put(e.getKey(), names);
        }
        return map;
    }

    public Map<? extends ClassLoader, Set<URL>> getClassNameListURLsByClassLoader() throws IOException {
        final LinkedHashMap<ClassLoader, Set<URL>> map = new LinkedHashMap<ClassLoader, Set<URL>>(this.classLoaders.size());
        final HashSet<URL> allURLs = new HashSet<URL>(this.classLoaders.size());
        final String classNameListResourceName = getServiceListResourceName();
        for (final ClassLoader classloader : this.classLoaders) {
            LinkedHashSet<URL> urls = null;
            try {
                for (final Enumeration<URL> it = classloader.getResources(classNameListResourceName); it.hasMoreElements(); ) {
                    final URL url = it.nextElement();
                    if ((url != null) && allURLs.add(url) && acceptServiceListResourceURL(classloader, url)) {
                        if (urls == null) urls = new LinkedHashSet<URL>();
                        urls.add(url);
                    }
                }
            } catch (IOException ex) {
                handleIOException(classloader, null, ex);
            }
            if (urls != null) map.put(classloader, urls);
        }
        return map;
    }

    public String getServiceListResourceName() {
        return this.serviceListResourceName;
    }

    public Class<T> getServiceType() {
        return this.serviceType;
    }

    public boolean isAcceptAbstract() {
        return this.acceptAbstract;
    }

    public boolean isAcceptNonPublic() {
        return this.acceptNonPublic;
    }

    public boolean isInitClasses() {
        return this.initClasses;
    }

    public void setAcceptAbstract(final boolean v) {
        this.acceptAbstract = v;
    }

    public void setAcceptNonPublic(final boolean v) {
        this.acceptNonPublic = v;
    }

    public void setInitClasses(final boolean v) {
        this.initClasses = v;
    }

    public void setServiceListResourceName(final String name) {
        if (!name.trim().equals(name)) throw new IllegalArgumentException("Name begins or ends with whitespaces: '" + name + "'");
        this.serviceListResourceName = name;
    }
}
