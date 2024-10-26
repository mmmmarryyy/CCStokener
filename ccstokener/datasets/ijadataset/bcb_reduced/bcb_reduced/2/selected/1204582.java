package org.kwantu.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kwantu.m2.KwantuFaultException;
import org.kwantu.m2.model.AbstractApplicationController;
import org.kwantu.m2.model.KwantuClass;
import org.openide.util.Exceptions;

/**
 *
 * @author chris
 */
public class KwantuJarClassLoader extends URLClassLoader {

    private static final Log LOG = LogFactory.getLog(KwantuJarClassLoader.class);

    private AbstractApplicationController controller;

    private String artifactId, javaArtifactId, version;

    private URL jarURL;

    public KwantuJarClassLoader(AbstractApplicationController controller, String artifactId, String version, ClassLoader parent) {
        super(new URL[] {}, parent);
        this.controller = controller;
        this.artifactId = artifactId;
        this.version = version;
        initJarURL();
        addURL(jarURL);
    }

    private URL getMavenRepoURL() throws MalformedURLException {
        return new URL("file:///" + System.getProperty("user.home") + "/.m2/repository/");
    }

    private URL getKwantuJarURLInMavenRepo(String artifactId, String version) throws MalformedURLException {
        URL url = new URL(getMavenRepoURL(), "./org/kwantu/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
        LOG.info("url=" + url);
        return url;
    }

    private void initJarURL() {
        try {
            URL url = getKwantuJarURLInMavenRepo(artifactId, version);
            File tempJarFile = File.createTempFile(artifactId + "-" + version, ".jar");
            OutputStream out = new FileOutputStream(tempJarFile);
            InputStream in = url.openStream();
            int length = 0;
            byte[] bytes = new byte[2048];
            while ((length = in.read(bytes)) > 0) {
                out.write(bytes, 0, length);
            }
            in.close();
            out.close();
            jarURL = tempJarFile.toURI().toURL();
        } catch (IOException ex) {
            throw new KwantuFaultException(ex);
        }
    }

    public Class getJavaClass(KwantuClass c) {
        String name = c.getJavaQualifiedName("org.kwantu", javaArtifactId);
        try {
            return loadClass(name);
        } catch (ClassNotFoundException ex) {
            throw new KwantuFaultException("KwantuClass " + name + " not found", ex);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> c = super.loadClass(name);
        try {
            Method controllerSetter = c.getDeclaredMethod("setController", AbstractApplicationController.class);
            controllerSetter.invoke(null, controller);
        } catch (IllegalAccessException ex) {
            throw new KwantuFaultException(ex);
        } catch (IllegalArgumentException ex) {
            throw new KwantuFaultException(ex);
        } catch (InvocationTargetException ex) {
            throw new KwantuFaultException(ex);
        } catch (NoSuchMethodException ex) {
        } catch (SecurityException ex) {
            throw new KwantuFaultException(ex);
        }
        return c;
    }

    public String getJavaArtifactId() {
        return javaArtifactId;
    }

    public void setJavaArtifactId(String javaArtifactId) {
        this.javaArtifactId = javaArtifactId;
    }
}
