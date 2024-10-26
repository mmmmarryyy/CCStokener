package quorto.parser;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.Sets;
import lombok.Lombok;

/**
 * The java core libraries have a SPI discovery system, but it works only in Java 1.6 and up. For at least Eclipse,
 * lombok actually works in java 1.5, so we've rolled our own SPI discovery system.
 * 
 * It is not API compatible with {@code ServiceLoader}.
 * 
 * @see java.util.ServiceLoader
 */
public class ServicesUtil {

    private ServicesUtil() {
    }

    /**
	 * Method that conveniently turn the {@code Iterable}s returned by the other methods in this class to a
	 * {@code List}.
	 * 
	 * @see #findServices(Class)
	 * @see #findServices(Class, ClassLoader)
	 */
    public static <T> List<T> readAllFromIterator(Iterable<T> findServices) {
        List<T> list = new ArrayList<T>();
        for (T t : findServices) list.add(t);
        return list;
    }

    /**
	 * Returns an iterator of instances that, at least according to the spi discovery file, are implementations
	 * of the stated class.
	 * 
	 * Like ServiceLoader, each listed class is turned into an instance by calling the public no-args constructor.
	 * 
	 * Convenience method that calls the more elaborate {@link #findServices(Class, ClassLoader)} method with
	 * this {@link java.lang.Thread}'s context class loader as {@code ClassLoader}.
	 * 
	 * @param target class to find implementations for.
	 */
    public static <C> Iterable<C> findServices(Class<C> target) {
        return findServices(target, Thread.currentThread().getContextClassLoader());
    }

    /**
	 * Returns an iterator of class objects that, at least according to the spi discovery file, are implementations
	 * of the stated class.
	 * 
	 * Like ServiceLoader, each listed class is turned into an instance by calling the public no-args constructor.
	 * 
	 * @param target class to find implementations for.
	 * @param loader The classloader object to use to both the spi discovery files, as well as the loader to use
	 * to make the returned instances.
	 */
    public static <C> Iterable<C> findServices(final Class<C> target, ClassLoader loader) {
        try {
            if (loader == null) loader = ClassLoader.getSystemClassLoader();
            Enumeration<URL> resources = loader.getResources("META-INF/services/" + target.getName());
            final Set<String> entries = new LinkedHashSet<String>();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                readServicesFromUrl(entries, url);
            }
            final Iterator<String> names = entries.iterator();
            final ClassLoader fLoader = loader;
            return new Iterable<C>() {

                @Override
                public Iterator<C> iterator() {
                    return new Iterator<C>() {

                        @Override
                        public boolean hasNext() {
                            return names.hasNext();
                        }

                        @Override
                        public C next() {
                            try {
                                return target.cast(Class.forName(names.next(), true, fLoader).newInstance());
                            } catch (Throwable t) {
                                throw Lombok.sneakyThrow(t);
                            }
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static void readServicesFromUrl(Collection<String> list, URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            while (true) {
                String line = r.readLine();
                if (line == null) break;
                int idx = line.indexOf('#');
                if (idx != -1) line = line.substring(0, idx);
                line = line.trim();
                if (line.length() == 0) continue;
                list.add(line);
            }
        } finally {
            try {
                if (in != null) in.close();
            } catch (Throwable ignore) {
            }
        }
    }

    /**
	 * This method will find the @{code T} in {@code public class Foo extends BaseType<T>}.
	 * 
	 * It returns an annotation type because it is used exclusively to figure out which annotations are
	 * being handled by {@link lombok.eclipse.EclipseAnnotationHandler} and {@link lombok.javac.JavacAnnotationHandler}.
	 */
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> findAnnotationClass(Class<?> c, Class<?> base) {
        if (c == Object.class || c == null) return null;
        for (Type iface : c.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType) {
                ParameterizedType p = (ParameterizedType) iface;
                if (!base.equals(p.getRawType())) continue;
                Type target = p.getActualTypeArguments()[0];
                if (target instanceof Class<?>) {
                    if (Annotation.class.isAssignableFrom((Class<?>) target)) {
                        return (Class<? extends Annotation>) target;
                    }
                }
                throw new ClassCastException("Not an annotation type: " + target);
            }
        }
        Class<? extends Annotation> potential = findAnnotationClass(c.getSuperclass(), base);
        if (potential != null) return potential;
        for (Class<?> iface : c.getInterfaces()) {
            potential = findAnnotationClass(iface, base);
            if (potential != null) return potential;
        }
        return null;
    }

    /**
	 * Solves a conflict between different services that all claim priority on the same token by checking their {@code comesBefore} and {@code comesAfter} lists.
	 * If an unambiguous service exists, it is returned. If not, a {@code ServiceConflictException} is thrown.<br />
	 * A service wins a conflict if: <ul>
	 * <li>For each other service, that service lists the class of the winning service in its {@code comesAfter} list,
	 *     <i>and</i>/<i>or</i> the winning service lists the other's class in its {@code comesBefore} list.
	 * <li>For each other service, that service <i>does not</i> list the class of the winning service in its {@code comesBefore} list,
	 *     <i>and</i>the winning service <i>does not</i> list the other's class in its {@code comesAfter} list.
	 * </ul>
	 * 
	 * null is returned if the input is 0 services.
	 */
    public static <S extends Service> S solveConflict(Iterable<S> services) {
        Set<Class<?>> losers = Sets.newHashSet();
        Set<Class<?>> classes = Sets.newHashSet();
        for (Service candidate : services) {
            for (Class<?> noCandidate : candidate.comesBefore()) losers.add(noCandidate);
            classes.add(candidate.getClass());
        }
        if (classes.isEmpty()) return null;
        S winner = null;
        top: for (S candidate : services) {
            if (losers.contains(candidate.getClass())) continue;
            for (Class<?> priority : candidate.comesAfter()) {
                if (classes.contains(priority)) continue top;
            }
            if (winner != null) {
                winner = null;
                break;
            }
            winner = candidate;
        }
        if (winner != null) return winner;
        throw new ServiceConflictException(classes);
    }
}
