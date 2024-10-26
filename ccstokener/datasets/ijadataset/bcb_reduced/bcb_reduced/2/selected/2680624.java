package org.spockframework.runtime;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.spockframework.runtime.extension.ExtensionException;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.util.IoUtil;

/**
 * Scans class path for extension descriptors and loads the extension classes specified therein.
 */
public class ExtensionClassesLoader {

    public static final String EXTENSION_DESCRIPTOR_PATH = "META-INF/services/" + IGlobalExtension.class.getName();

    public List<Class<?>> loadClassesFromDefaultLocation() {
        return loadClasses(EXTENSION_DESCRIPTOR_PATH);
    }

    public List<Class<?>> loadClasses(String descriptorPath) {
        List<Class<?>> extClasses = new ArrayList<Class<?>>();
        for (URL url : locateDescriptors(descriptorPath)) for (String className : readDescriptor(url)) extClasses.add(loadExtensionClass(className));
        return extClasses;
    }

    private List<URL> locateDescriptors(String descriptorPath) {
        try {
            return Collections.list(RunContext.class.getClassLoader().getResources(descriptorPath));
        } catch (Exception e) {
            throw new ExtensionException("Failed to locate extension descriptors", e);
        }
    }

    private List<String> readDescriptor(URL url) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            List<String> lines = new ArrayList<String>();
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) lines.add(line);
                line = reader.readLine();
            }
            return lines;
        } catch (IOException e) {
            throw new ExtensionException("Failed to read extension descriptor '%s'", e).withArgs(url);
        } finally {
            IoUtil.closeQuietly(reader);
        }
    }

    private Class<?> loadExtensionClass(String className) {
        try {
            return RunContext.class.getClassLoader().loadClass(className);
        } catch (Exception e) {
            throw new ExtensionException("Failed to load extension class '%s'", e).withArgs(className);
        }
    }
}
