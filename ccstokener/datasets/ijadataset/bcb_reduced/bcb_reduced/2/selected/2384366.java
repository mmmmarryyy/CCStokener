package org.peaseplate.domain.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

public class ServiceConfiguration {

    private final Class<?> serviceClass;

    private final ClassLoader[] classLoaders;

    private final Collection<Class<?>> contributions;

    public ServiceConfiguration(Class<?> serviceClass, Collection<ClassLoader> classLoaders) throws IOException {
        this(serviceClass, classLoaders.toArray(new ClassLoader[classLoaders.size()]));
    }

    public ServiceConfiguration(Class<?> serviceClass, ClassLoader... classLoaders) throws IOException {
        super();
        this.serviceClass = serviceClass;
        this.classLoaders = classLoaders;
        contributions = new ArrayList<Class<?>>();
        load();
    }

    public Collection<Class<?>> getContributions() {
        return Collections.unmodifiableCollection(contributions);
    }

    public boolean containsContribution(Class<?> contribution) {
        return contributions.contains(contribution);
    }

    protected void load() throws IOException {
        for (ClassLoader classLoader : classLoaders) {
            Enumeration<URL> en = classLoader.getResources("META-INF/services/" + serviceClass.getName());
            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                InputStream in = url.openStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    try {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("#")) {
                                line = line.trim();
                                if (line.length() > 0) contributions.add(resolveClass(url, line));
                            }
                        }
                    } finally {
                        reader.close();
                    }
                } finally {
                    in.close();
                }
            }
        }
    }

    protected Class<?> resolveClass(URL resource, String classname) throws IOException {
        Class<?> result = null;
        for (int i = classLoaders.length - 1; i >= 0; i -= 1) {
            try {
                result = classLoaders[i].loadClass(classname);
            } catch (ClassNotFoundException e) {
            }
        }
        if (result == null) throw new IOException("Could not find class " + classname + " as referenced in service configuration " + resource);
        return result;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getName());
        builder.append(" [\n");
        for (Class<?> contribution : contributions) builder.append("\t").append(contribution).append("\n");
        builder.append("]");
        return builder.toString();
    }
}
