package org.xfeep.asura.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import org.xfeep.asura.core.ComponentDefinition;
import org.xfeep.asura.core.ComponentManager;

/**
 * PackageBundleComponentLoader use {@link XmlComponentLoader} to load all component definitations from all
 * classes or xml files specified by the Asura-Component header in the manifest file which is included in a jar file or
 * a unzipped jar dir. In the load process, classes are loaded by the ClassLoader instance passed to meothod
 * getStaticLoadComponents(ClassLoader)
 * @see #getStaticLoadComponents(ClassLoader)
 * @author zhang yuexiang
 *
 */
public class PackageBundleComponentLoader implements ComponentLoader {

    URLClassLoader fileLoader;

    public PackageBundleComponentLoader() {
        this(".");
    }

    public PackageBundleComponentLoader(String jarOrDirPath) {
        this(new File(jarOrDirPath));
    }

    public PackageBundleComponentLoader(File jarOrDir) {
        try {
            fileLoader = new URLClassLoader(new URL[] { jarOrDir.toURI().toURL() });
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public List<ComponentDefinition> getStaticLoadComponents(ClassLoader classLoader) throws IOException {
        URL url = fileLoader.findResource("META-INF/MANIFEST.MF");
        if (url == null) {
            return Collections.EMPTY_LIST;
        }
        InputStream manifestStream = url.openStream();
        Manifest manifest = new Manifest(manifestStream);
        String acstring = manifest.getMainAttributes().getValue("Asura-Component");
        if (acstring == null || acstring.trim().length() == 0) {
            return Collections.EMPTY_LIST;
        }
        List<ComponentDefinition> rt = new ArrayList<ComponentDefinition>();
        for (String ac : acstring.split("\\,")) {
            ac = ac.trim();
            if (ac.endsWith(".xml") || ac.endsWith(".XML")) {
                rt.addAll(new XmlComponentLoader(fileLoader.findResource(ac)).getStaticLoadComponents(classLoader));
            } else {
                try {
                    rt.add(ComponentDefinition.create(classLoader.loadClass(ac)));
                } catch (ClassNotFoundException e) {
                    throw new IOException("can not load component " + ac + " from " + fileLoader.getURLs()[0], e);
                }
            }
        }
        manifestStream.close();
        return rt;
    }

    public void initialize(ComponentManager componentManager) {
    }

    public void setupComponentDynamicLoadListener(ComponentDynamicLoadListener listener) {
    }

    @Override
    public String toString() {
        return fileLoader == null ? super.toString() : fileLoader.getURLs()[0].toString();
    }
}
