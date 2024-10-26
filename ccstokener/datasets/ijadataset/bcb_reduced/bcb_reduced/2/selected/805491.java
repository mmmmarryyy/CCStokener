package org.tn5250j.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * This is a simplified backport of <tt>java.util.ServiceLoader</tt>.
 * <br><br>
 * (<i>Text copied from JavaDoc 1.6:</i>)<br>
 * <br>
 * <b>Example:</b>
 * Suppose we have a service type com.example.CodecSet which is intended
 * to represent sets of encoder/decoder pairs for some protocol.
 * In this case it is an abstract class with two abstract methods:
 * <ul>
 * <li>public abstract Encoder getEncoder(String encodingName);</li>
 * <li>public abstract Decoder getDecoder(String encodingName);</li>
 * </ul>
 * Each method returns an appropriate object or null if the provider does
 * not support the given encoding. Typical providers support more than
 * one encoding.<br>
 * <br>
 * If <tt>com.example.impl.StandardCodecs</tt> is an implementation of the
 * CodecSet service then its jar file also contains a file named
 * 
 * <pre>
 * META-INF/services/com.example.CodecSet
 * </pre>
 * 
 * This file contains the single line:
 * 
 * <pre>
 * com.example.impl.StandardCodecs    # Standard codecs
 * </pre>
 * 
 * The CodecSet class creates and saves a single service instance at
 * initialization:
 * 
 * <pre>
 * private static ServiceLoader&lt;CodecSet&gt; codecSetLoader
 * 		= ServiceLoader.load(CodecSet.class);
 * </pre>
 * 
 * To locate an encoder for a given encoding name it defines a static factory
 * method which iterates through the known and available providers,
 * returning only when it has located a suitable encoder or has run out of providers.
 * 
 * <pre>
 * public static Encoder getEncoder(String encodingName) {
 *       for (CodecSet cp : codecSetLoader) {
 *           Encoder enc = cp.getEncoder(encodingName);
 *           if (enc != null)
 *               return enc;
 *       }
 *       return null;
 *   }
 * </pre>
 * 
 * A getDecoder method is defined similarly. 
 * 
 * @author maki
 * @see http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider
 */
public class ServiceLoader<S> {

    private static final String PREFIX = "META-INF" + File.separatorChar + "services" + File.separatorChar;

    private final List<String> classNames;

    private final String serviceName;

    private final ClassLoader classLoader;

    /**
	 * @param servicename
	 * @param classLoader
	 * @param arrayList
	 */
    private ServiceLoader(String servicename, ClassLoader classLoader, ArrayList<String> arrayList) {
        assert servicename != null;
        assert arrayList != null;
        this.serviceName = servicename;
        this.classNames = arrayList;
        this.classLoader = classLoader;
    }

    /**
	 * Creates a new service loader for the given service type,
	 * using the current thread's context class loader.
	 * @param <S>
	 * @param service
	 * @return
	 */
    public static <S> ServiceLoader<S> load(Class<S> service) {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    /**
	 * Creates a new service loader for the given service type and class loader.
	 * @param <S>
	 * @param service
	 * @param loader
	 * @return
	 */
    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        try {
            Enumeration<URL> resources = loader.getResources(PREFIX + service.getCanonicalName());
            final ArrayList<String> cnames = new ArrayList<String>();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    line = line.trim();
                    final int startCommentIndex = line.indexOf(0x23) >= 0 ? line.indexOf(0x23) : line.length();
                    line = line.substring(0, startCommentIndex).trim();
                    if (line.length() > 0) {
                        cnames.add(line);
                    }
                }
            }
            return new ServiceLoader<S>(service.getCanonicalName(), loader, cnames);
        } catch (Exception transform) {
            throw new RuntimeException(transform);
        }
    }

    /**
	 * Clear this loader's provider cache so that all providers will be reloaded.<br>
	 * <br>
	 * After invoking this method, subsequent invocations of the {@link #iterator()} method 
	 * will lazily look up and instantiate providers from scratch, just as is
	 * done by a newly-created loader.<br>
	 * <br>
	 * This method is intended for use in situations in which new providers can
	 * be installed into a running Java virtual machine.<br>
	 *
	 */
    public void reload() {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    /**
	 * Lazily loads the available providers of this loader's service.
	 * 
	 * @return
	 */
    public Iterator<S> iterator() {
        Iterator<S> lazyclassloader = new Iterator<S>() {

            int pointer = 0;

            public boolean hasNext() {
                return pointer < classNames.size();
            }

            @SuppressWarnings("unchecked")
            public S next() {
                Object target = null;
                final String clazzname = classNames.get(pointer);
                try {
                    final Class<?> clazz = classLoader.loadClass(clazzname);
                    final Constructor[] constructors = clazz.getConstructors();
                    for (int i = 0; i < constructors.length && target == null; i++) {
                        Constructor c = constructors[i];
                        if (c.getGenericParameterTypes().length == 0) {
                            target = c.newInstance(new Object[] {});
                        }
                    }
                } catch (Exception transform) {
                    throw new RuntimeException(transform);
                } finally {
                    pointer++;
                }
                if (target == null) {
                    throw new IllegalStateException("Class " + clazzname + " does not have a public default constructor!");
                }
                return (S) target;
            }

            public void remove() {
                throw new UnsupportedOperationException("Method not implemented");
            }
        };
        return lazyclassloader;
    }

    @Override
    public String toString() {
        return "ServiceLoader<" + serviceName + ">";
    }
}
