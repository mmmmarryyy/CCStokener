package org.dwgsoftware.raistlin.composition.model.impl;

import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.dwgsoftware.raistlin.composition.data.builder.ProfilePackageBuilder;
import org.dwgsoftware.raistlin.composition.model.ModelException;
import org.dwgsoftware.raistlin.composition.provider.SystemContext;
import org.dwgsoftware.raistlin.meta.info.Service;
import org.dwgsoftware.raistlin.meta.info.ServiceDescriptor;
import org.dwgsoftware.raistlin.meta.info.Type;
import org.dwgsoftware.raistlin.meta.info.builder.ServiceBuilder;
import org.dwgsoftware.raistlin.meta.info.builder.TypeBuilder;
import org.dwgsoftware.raistlin.meta.info.verifier.TypeVerifier;
import org.dwgsoftware.raistlin.util.exception.ExceptionHelper;
import org.dwgsoftware.raistlin.util.i18n.ResourceManager;
import org.dwgsoftware.raistlin.util.i18n.Resources;

/**
 * A repository for services, types and profiles.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $ $Date: 2005/09/06 00:58:18 $
 */
class Scanner extends AbstractLogEnabled {

    private static final Resources REZ = ResourceManager.getPackageResources(Scanner.class);

    private static final String X_INFO = ".xinfo";

    private static final String X_TYPE = ".xtype";

    private static final String X_SERVICE = ".xservice";

    private static final String X_PROFILE = ".xprofile";

    /**
     * The type builder.
     */
    private static final TypeBuilder TYPE_BUILDER = new TypeBuilder();

    /**
     * The service builder.
     */
    private static final ServiceBuilder SERVICE_BUILDER = new ServiceBuilder();

    /**
     * The packaged profile builder.
     */
    private static final ProfilePackageBuilder PACKAGE_BUILDER = new ProfilePackageBuilder();

    /**
     * Parent repository.
     */
    private ClassLoader m_classloader;

    /**
     * System context.
     */
    private SystemContext m_system;

    /**
     * Creation of a new scanner using a supplied classloader.  
     * The scanner is responsible for scanning suppied URLs for 
     * service and types.
     *
     * @param logger the logging channel
     * @param classloader the classloader
     */
    public Scanner(Logger logger, SystemContext system, ClassLoader classloader) {
        if (classloader == null) {
            throw new NullPointerException("classloader");
        }
        if (system == null) {
            throw new NullPointerException("system");
        }
        m_classloader = classloader;
        m_system = system;
        enableLogging(logger);
    }

    /**
     * Scan the supplied url for Service and Type defintions.
     * @param urls the URL array to scan
     * @param types the map to populate with types as keys and 
     *   and packaged profiles as values
     * @param services a list to be populated with service descriptors
     */
    public void scan(URL[] urls, List types, List services) throws ModelException {
        for (int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            scanURL(url, types, services);
        }
    }

    /**
     * Add a URL to the classpath.
     * @param url the URL to add to the repository
     */
    private void scanURL(URL url, List types, List services) throws ModelException {
        if (getLogger().isDebugEnabled()) {
            final String message = REZ.getString("scanner.scanning", m_system.toString(url.toString()));
            getLogger().debug(message);
        }
        if (isDirectory(url)) {
            scanDirectory(url, types, services);
        } else if (url.getProtocol().equals("jar") || (url.getProtocol().equals("file") && url.toString().endsWith(".jar"))) {
            scanJarFileURL(url, types, services);
        } else {
            scanInputStream(url, types, services);
        }
    }

    private void scanDirectory(URL url, List types, List services) throws ModelException {
        try {
            File directory = getDirectory(url);
            scanDirectoryContent(directory, directory, types, services);
        } catch (Throwable e) {
            final String error = REZ.getString("scanner.dir-scan.error", url.toString());
            throw new ModelException(error, e);
        }
    }

    private void scanJarFileURL(URL url, List types, List services) throws ModelException {
        URL uri = url;
        try {
            if (!url.getProtocol().equals("jar")) {
                uri = getJarURL(url);
            }
            if (!uri.toString().endsWith("!/")) {
                final String error = REZ.getString("scanner.nested-jar-unsupported.error", url.toString());
                throw new ModelException(error);
            }
            final JarURLConnection jar = (JarURLConnection) uri.openConnection();
            final JarFile base = jar.getJarFile();
            scanJarFile(base, types, services);
        } catch (Throwable e) {
            final String error = REZ.getString("scanner.jar.error", url.toString());
            throw new ModelException(error, e);
        }
    }

    private void scanJarFile(JarFile base, List types, List services) throws Exception {
        Enumeration entries = base.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(X_TYPE) || name.endsWith(X_INFO)) {
                addType(types, name);
            } else if (name.endsWith(X_SERVICE)) {
                addService(services, name);
            }
        }
    }

    private void scanInputStream(URL url, List types, List services) {
        try {
            Object object = url.openStream();
            if (object != null) {
                JarInputStream stream = new JarInputStream((InputStream) object);
                scanJarInputStream(stream, types, services);
                return;
            } else {
                if (getLogger().isWarnEnabled()) {
                    final String warning = REZ.getString("scanner.stream.unrecognized-content.warning", url.toString());
                    getLogger().warn(warning);
                }
            }
        } catch (Throwable e) {
            if (getLogger().isWarnEnabled()) {
                final String error = REZ.getString("scanner.stream.content.error", url.toString());
                final String warning = ExceptionHelper.packException(error, e, getLogger().isDebugEnabled());
                getLogger().warn(warning);
            }
        }
    }

    private void scanJarInputStream(JarInputStream stream, List types, List services) throws Exception {
        ZipEntry entry = null;
        try {
            entry = stream.getNextEntry();
        } catch (Throwable e) {
            entry = null;
        }
        while (entry != null) {
            String name = entry.getName();
            if (name.endsWith(X_TYPE) || name.endsWith(X_INFO)) {
                addType(types, name);
            } else if (name.endsWith(X_SERVICE)) {
                addService(services, name);
            }
            try {
                entry = stream.getNextEntry();
            } catch (Throwable e) {
                entry = null;
            }
        }
    }

    private void scanDirectoryContent(File base, File dir, List types, List services) throws Exception {
        File[] files = dir.listFiles();
        String path = base.toString();
        int j = path.length();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                scanDirectoryContent(base, file, types, services);
            } else {
                scanFile(j, file, types, services);
            }
        }
    }

    private void scanFile(int j, File file, List types, List services) throws Exception {
        String filename = file.toString();
        String name = filename.substring(j, filename.length());
        if (name.endsWith(X_TYPE) || name.endsWith(X_INFO)) {
            addType(types, name);
        } else if (name.endsWith(X_SERVICE)) {
            addService(services, name);
        }
    }

    private void addType(List types, String name) throws Exception {
        String classname = parseResourceName(name);
        Class clazz = getComponentClass(classname);
        Type type = TYPE_BUILDER.buildType(clazz);
        try {
            verifyType(type, clazz);
            if (getLogger().isDebugEnabled()) {
                final String message = REZ.getString("scanner.type.addition", classname);
                getLogger().debug(message);
            }
            types.add(type);
        } catch (NoClassDefFoundError e) {
            if (getLogger().isWarnEnabled()) {
                final String error = REZ.getString("scanner.type.verification.ncdf.failure", classname, e.getMessage());
                getLogger().warn(error);
            }
        } catch (Throwable e) {
            if (getLogger().isWarnEnabled()) {
                final String error = REZ.getString("scanner.type.verification.failure", classname);
                getLogger().warn(ExceptionHelper.packException(error, e, getLogger().isDebugEnabled()));
            }
        }
    }

    private void addService(List list, String name) throws Exception {
        String classname = parseResourceName(name);
        Service service = SERVICE_BUILDER.build(classname, m_classloader);
        try {
            verifyService(service);
            if (getLogger().isDebugEnabled()) {
                final String message = REZ.getString("scanner.service.addition", classname);
                getLogger().debug(message);
            }
            list.add(service);
        } catch (Throwable e) {
            if (getLogger().isWarnEnabled()) {
                final String error = REZ.getString("scanner.service.verification.failure", classname);
                getLogger().warn(ExceptionHelper.packException(error, e, getLogger().isDebugEnabled()));
            }
        }
    }

    /**
     * Verify the intergrity of the supplied type.
     * @param type the type to verify
     * @param clazz the implementation class
     * @exception Exception if an verification failure occurs
     */
    private void verifyType(Type type, Class clazz) throws Exception {
        final String name = type.getInfo().getName();
        final Class[] classes = getServiceClasses(type);
        final TypeVerifier verifier = new TypeVerifier();
        verifier.verifyType(name, clazz, classes);
    }

    /**
     * Verify the intergrity of the supplied type.
     * @param type the type to verify
     * @exception Exception if an verification failure occurs
     */
    private void verifyService(Service service) throws Exception {
        String classname = service.getClassname();
        try {
            m_classloader.loadClass(classname);
        } catch (NoClassDefFoundError ncdf) {
            String ref = parseResourceName(ncdf.getMessage());
            final String error = REZ.getString("scanner.service.bad-class.error", classname, ref);
            throw new ModelException(error);
        } catch (ClassNotFoundException cnfe) {
            final String error = REZ.getString("scanner.service.missing-class.error", classname);
            throw new ModelException(error);
        }
    }

    /**
     * Return the set of interface classes for a given type that are declared
     * or default to the "native" service access protocol and where the
     * service access model is undefined (i.e. native implementation).
     * access mode.
     *
     * @param type the component type
     * @return an array of classes represnting the type's service interfaces
     */
    private Class[] getServiceClasses(Type type) throws ModelException {
        ArrayList list = new ArrayList();
        ServiceDescriptor[] services = type.getServices();
        for (int i = 0; i < services.length; i++) {
            ServiceDescriptor service = services[i];
            if ((service.getAttribute("urn:raistlin:service.protocol", "native").equals("native")) && (service.getAttribute("urn:raistlin:service.accessor", null) == null)) {
                list.add(getServiceClass(services[i]));
            }
        }
        return (Class[]) list.toArray(new Class[0]);
    }

    /**
     * Returns the component type implementation class.
     * @param type the component type descriptor
     * @return the class implementing the component type
     * @exception ModelException if a classloader error occurs
     */
    private Class getComponentClass(Type type) throws ModelException {
        if (null == type) {
            throw new NullPointerException("type");
        }
        return getComponentClass(type.getInfo().getClassname());
    }

    /**
     * Returns the component type implementation class.
     * @param classname the component type implementation classname
     * @exception ModelException if a classloader error occurs
     */
    private Class getComponentClass(String classname) throws ModelException {
        try {
            return m_classloader.loadClass(classname);
        } catch (NoClassDefFoundError ncdf) {
            String ref = parseResourceName(ncdf.getMessage());
            final String error = REZ.getString("scanner.type.bad-class.error", classname, ref);
            throw new ModelException(error);
        } catch (ClassNotFoundException cnfe) {
            final String error = REZ.getString("scanner.type.missing-class.error", classname);
            throw new ModelException(error);
        }
    }

    /**
     * Returns the service type implementation class.
     * @param service the service type descriptor
     * @return the class implementing the service type
     * @exception ModelException if a classloader error occurs
     */
    private Class getServiceClass(ServiceDescriptor service) throws ModelException {
        final String classname = service.getReference().getClassname();
        try {
            return m_classloader.loadClass(classname);
        } catch (NoClassDefFoundError ncdf) {
            String ref = parseResourceName(ncdf.getMessage());
            final String error = REZ.getString("scanner.service.bad-class.error", classname, ref);
            throw new ModelException(error);
        } catch (ClassNotFoundException cnfe) {
            final String error = REZ.getString("scanner.service.missing-class.error", classname);
            throw new ModelException(error);
        }
    }

    private boolean isDirectory(URL url) {
        if (url.getProtocol().equals("file")) {
            return getFile(url).isDirectory();
        }
        return false;
    }

    private File getDirectory(URL url) throws IllegalArgumentException {
        File file = getFile(url);
        if (file.isDirectory()) {
            return file;
        }
        final String error = REZ.getString("scanner.url-not-a-directory.error", url.toString());
        throw new IllegalArgumentException(error);
    }

    private File getFile(URL url) throws IllegalArgumentException {
        if (url.getProtocol().equals("file")) {
            return new File(url.toString().substring(5));
        }
        final String error = REZ.getString("scanner.not-file-protocol.error", url.toString());
        throw new IllegalArgumentException(error);
    }

    private String parseResourceName(String resource) {
        try {
            int i = resource.lastIndexOf(".");
            String name = resource.substring(0, i);
            String name2 = name.replace('/', '.');
            String name3 = name2.replace('\\', '.');
            if (name3.startsWith(".")) {
                return name3.substring(1, name3.length());
            } else {
                return name3;
            }
        } catch (Throwable e) {
            return resource;
        }
    }

    private URL getJarURL(URL url) throws MalformedURLException {
        if (url.getProtocol().equals("jar")) {
            return url;
        } else {
            return new URL("jar:" + url.toString() + "!/");
        }
    }
}
