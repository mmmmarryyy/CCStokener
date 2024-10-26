package org.dwgsoftware.raistlin.composition.model.impl;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.dwgsoftware.raistlin.composition.data.ClassLoaderDirective;
import org.dwgsoftware.raistlin.composition.data.ContainmentProfile;
import org.dwgsoftware.raistlin.composition.data.ExcludeDirective;
import org.dwgsoftware.raistlin.composition.data.FilesetDirective;
import org.dwgsoftware.raistlin.composition.data.IncludeDirective;
import org.dwgsoftware.raistlin.composition.model.ClassLoaderModel;
import org.dwgsoftware.raistlin.composition.model.ModelException;
import org.dwgsoftware.raistlin.composition.model.ServiceRepository;
import org.dwgsoftware.raistlin.composition.model.TypeRepository;
import org.dwgsoftware.raistlin.composition.provider.ClassLoaderContext;
import org.dwgsoftware.raistlin.composition.provider.SystemContext;
import org.dwgsoftware.raistlin.extension.Extension;
import org.dwgsoftware.raistlin.extension.manager.ExtensionManager;
import org.dwgsoftware.raistlin.extension.manager.OptionalPackage;
import org.dwgsoftware.raistlin.extension.manager.PackageManager;
import org.dwgsoftware.raistlin.extension.manager.impl.DefaultExtensionManager;
import org.dwgsoftware.raistlin.extension.manager.impl.DelegatingExtensionManager;
import org.dwgsoftware.raistlin.repository.Artifact;
import org.dwgsoftware.raistlin.repository.Repository;
import org.dwgsoftware.raistlin.util.i18n.ResourceManager;
import org.dwgsoftware.raistlin.util.i18n.Resources;

/**
 * <p>Implementation of a classloader model within which a 
 * repository, a base directory and a classloader directive 
 * are associated together enabling the creation of a fully 
 * qualified classpath.</p>
 *
 * <p>The classpath established by this model implementation
 * applies the following logic:</p>
 * <ul>
 *  <li>establish an extensions manager relative to the 
 *      &lt;library/&gt> directives</li>
 *  <li>build an uqualifed classpath relative to the  
 *      &lt;classpath/&gt> directives</li>
 *  <li>resolve any optional jar file extension jar file
 *      entries based on the manifest declarations of 
 *      the unqualified classpath, together with recursive
 *      resolution of resolved optional extensions</li>
 *  <li>consolidate the generated classpath relative to 
 *      the optional extensions established by any parent
 *      classloader models</li>
 * </ul>
 * <p>
 * Class dependecies include the Excalibur i18n, the assembly 
 * repository package, the avalon framework and meta packages,
 * and the extensions package.
 * </p>
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $ $Date: 2005/09/06 00:58:18 $
 */
public class DefaultClassLoaderModel extends AbstractLogEnabled implements ClassLoaderModel {

    private static final Resources REZ = ResourceManager.getPackageResources(DefaultClassLoaderModel.class);

    private final ClassLoaderContext m_context;

    private final ExtensionManager m_extension;

    private final PackageManager m_manager;

    private final String[] m_classpath;

    private final OptionalPackage[] m_packages;

    private final URL[] m_urls;

    private final URLClassLoader m_classLoader;

    private final DefaultTypeRepository m_types;

    private final DefaultServiceRepository m_services;

    private final Logger m_local;

    /**
    * Creation of a new classloader model.  The model associated a 
    * repository, a base directory and a classloader directive 
    * enabling the creation of a fully populated classpath.
    *
    * @param context the classloader context
    */
    public DefaultClassLoaderModel(ClassLoaderContext context) throws ModelException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        m_context = context;
        enableLogging(context.getLogger());
        m_local = getLogger().getChildLogger("classloader");
        if (getLogger().isDebugEnabled()) {
            getLocalLogger().debug("base: " + context.getSystemContext().toString(context.getBaseDirectory()));
        }
        File base = context.getBaseDirectory();
        Repository repository = context.getRepository();
        ClassLoaderDirective directive = context.getClassLoaderDirective();
        ExtensionManager manager = context.getExtensionManager();
        URL[] implicit = context.getImplicitURLs();
        try {
            if (manager != null) {
                DefaultExtensionManager local = new DefaultExtensionManager(directive.getLibrary().getOptionalExtensionDirectories(base));
                m_extension = new DelegatingExtensionManager(new ExtensionManager[] { manager, local });
            } else {
                m_extension = new DefaultExtensionManager(directive.getLibrary().getOptionalExtensionDirectories(base));
            }
            m_manager = new PackageManager(m_extension);
            m_classpath = createClassPath(base, repository, directive, implicit);
            if (getLocalLogger().isDebugEnabled()) {
                String str = "classpath: " + context.getSystemContext().toString(m_classpath);
                getLocalLogger().debug(str);
            }
            m_packages = buildOptionalPackages(m_classpath, context.getOptionalPackages());
            m_urls = buildQualifiedClassPath();
            m_classLoader = new URLClassLoader(m_urls, context.getClassLoader());
            ArrayList types = new ArrayList();
            ArrayList services = new ArrayList();
            Logger scannerLogger = getLocalLogger().getChildLogger("scanner");
            SystemContext system = context.getSystemContext();
            Scanner scanner = new Scanner(scannerLogger, system, m_classLoader);
            scanner.scan(m_urls, types, services);
            Logger typeLogger = getLocalLogger().getChildLogger("types");
            m_types = new DefaultTypeRepository(typeLogger, m_classLoader, context.getTypeRepository(), types);
            Logger serviceLogger = getLocalLogger().getChildLogger("services");
            m_services = new DefaultServiceRepository(serviceLogger, context.getServiceRepository(), services);
        } catch (Throwable e) {
            final String error = "Could not create classloader.";
            throw new ModelException(error, e);
        }
    }

    /**
    * Creation of a classloader model using this model as the 
    * relative parent.
    *
    * @param logger the loggiong channel
    * @param profile the profile directive
    * @param implied a sequence of implied urls
    * @return a new classloader context
    */
    public ClassLoaderModel createClassLoaderModel(Logger logger, ContainmentProfile profile, URL[] implied) throws ModelException {
        ClassLoaderContext context = createChildContext(logger, profile, implied);
        logger.debug("creating child classloader for: " + profile);
        return new DefaultClassLoaderModel(context);
    }

    /**
    * Return the type repository managed by this containment
    * context.
    *
    * @return the repository
    */
    public TypeRepository getTypeRepository() {
        return m_types;
    }

    /**
    * Return the classloader model service repository.
    *
    * @return the repository
    */
    public ServiceRepository getServiceRepository() {
        return m_services;
    }

    /**
    * Return the optional extensions manager.
    * @return the extension manager
    */
    public ExtensionManager getExtensionManager() {
        return m_extension;
    }

    /**
    * Return the set of local established optional packages.
    *
    * @return the local set of optional packages
    */
    public OptionalPackage[] getOptionalPackages() {
        return getOptionalPackages(false);
    }

    /**
    * Return the set of optional packages already established including
    * the optional packages established by any parent classloader model.
    *
    * @param policy if TRUE, return the local and all ancestor optional 
    *   package - if FALSE only return the local packages
    * @return the OptionalPackage instances
    */
    public OptionalPackage[] getOptionalPackages(boolean policy) {
        if (!policy) {
            return m_packages;
        }
        final ArrayList list = new ArrayList();
        OptionalPackage[] available = m_context.getOptionalPackages();
        for (int i = 0; i < available.length; i++) {
            list.add(available[i]);
        }
        for (int i = 0; i < m_packages.length; i++) {
            list.add(m_packages[i]);
        }
        return (OptionalPackage[]) list.toArray(new OptionalPackage[0]);
    }

    /**
    * Return the fully qualified classpath including extension jar files
    * resolved relative to the classpath directives in the meta-data
    * and any parent classloader models.
    *
    * WARNING: lots of updates needed to properly populate the returned
    * code source with certificates used to verify repository content which
    * means updating the extension utilities and the repository package.
    * Currently the code sources returned contain an empty certificates 
    * array.
    *
    * @return an array of URL representing the complete classpath 
    */
    public CodeSource[] getQualifiedClassPath() {
        CodeSource[] sources = new CodeSource[m_urls.length];
        for (int i = 0; i < m_urls.length; i++) {
            URL url = m_urls[i];
            sources[i] = new CodeSource(url, new Certificate[0]);
        }
        return sources;
    }

    /**
    * Return the classloader established by this classloader model.
    * @return the classloader
    */
    public ClassLoader getClassLoader() {
        return m_classLoader;
    }

    /**
    * Creation of a classloader context using this model as the 
    * relative parent.
    *
    * @param logger the loggiong channel
    * @param profile the profile directive
    * @param implied a sequence of implied urls
    * @return a new classloader context
    */
    private ClassLoaderContext createChildContext(Logger logger, ContainmentProfile profile, URL[] implied) {
        Repository repository = m_context.getRepository();
        SystemContext system = m_context.getSystemContext();
        File base = m_context.getBaseDirectory();
        OptionalPackage[] packages = getOptionalPackages();
        ClassLoaderDirective directive = profile.getClassLoaderDirective();
        return new DefaultClassLoaderContext(logger, system, m_classLoader, packages, m_extension, m_types, m_services, directive, implied);
    }

    private String[] getClassPath() {
        return m_classpath;
    }

    /**
    * Build the fully qulalified classpath including extension jar files
    * resolved relative to the classpath directives in the meta-data.
    *
    * @return an array of URL representing the complete classpath 
    */
    private URL[] buildQualifiedClassPath() throws Exception {
        final ArrayList list = new ArrayList();
        final String[] classpath = getClassPath();
        for (int i = 0; i < classpath.length; i++) {
            list.add(new URL(classpath[i]));
        }
        File[] extensions = OptionalPackage.toFiles(getOptionalPackages());
        for (int i = 0; i < extensions.length; i++) {
            list.add(extensions[i].toURL());
        }
        return (URL[]) list.toArray(new URL[0]);
    }

    private String[] createClassPath(File base, Repository repository, ClassLoaderDirective directive, URL[] implicit) throws Exception {
        ArrayList classpath = new ArrayList();
        if (implicit.length > 0) {
            if (getLogger().isDebugEnabled()) {
                getLocalLogger().debug("implicit entries: " + implicit.length);
            }
            for (int i = 0; i < implicit.length; i++) {
                classpath.add(implicit[i].toString());
            }
        }
        File[] files = expandFileSetDirectives(base, directive.getClasspathDirective().getFilesets());
        addToClassPath(classpath, files);
        if (files.length > 0) {
            if (getLocalLogger().isDebugEnabled()) {
                getLogger().debug("included entries: " + files.length);
            }
        }
        Artifact[] artifacts = directive.getClasspathDirective().getArtifacts();
        for (int i = 0; i < artifacts.length; i++) {
            Artifact artifact = artifacts[i];
            URL url = repository.getResource(artifact);
            classpath.add(url.toString());
        }
        return (String[]) classpath.toArray(new String[0]);
    }

    /**
     * Retrieve the files for the optional packages required by
     * the jars in ClassPath.
     *
     * @param classPath the Classpath array
     * @return the files that need to be added to ClassLoader
     * @exception Exception if a extension error occurs
     */
    private OptionalPackage[] buildOptionalPackages(final String[] classPath) throws Exception {
        return buildOptionalPackages(classPath, new OptionalPackage[0]);
    }

    /**
     * Retrieve the files for the optional packages required by
     * the jars in the ClassPath.
     *
     * @param classPath the Classpath array
     * @return the files that need to be added to ClassLoader
     * @exception Exception if a extension error occurs
     */
    private OptionalPackage[] buildOptionalPackages(final String[] classPath, final OptionalPackage[] established) throws Exception {
        final ArrayList unsatisfied = new ArrayList();
        final ArrayList dependencies = new ArrayList();
        for (int i = 0; i < established.length; i++) {
            dependencies.add(established[i]);
        }
        final Manifest[] manifests = getManifests(classPath);
        final Extension[] available = Extension.getAvailable(manifests);
        final Extension[] required = Extension.getRequired(manifests);
        m_manager.scanDependencies(required, available, dependencies, unsatisfied);
        if (0 != unsatisfied.size()) {
            final int size = unsatisfied.size();
            final String message = REZ.getString("classloader.unsatisfied-extensions.error", new Integer(size));
            StringBuffer buffer = new StringBuffer(message);
            for (int i = 0; i < size; i++) {
                final Extension extension = (Extension) unsatisfied.get(i);
                final Object[] params = new Object[] { extension.getExtensionName(), extension.getSpecificationVendor(), extension.getSpecificationVersion(), extension.getImplementationVendor(), extension.getImplementationVendorID(), extension.getImplementationVersion(), extension.getImplementationURL() };
                final String entry = REZ.format("classloader.missing.extension.error", params);
                buffer.append("\n" + entry);
            }
            throw new ModelException(buffer.toString());
        }
        final OptionalPackage[] packages = (OptionalPackage[]) dependencies.toArray(new OptionalPackage[0]);
        return consolidate(packages, established);
    }

    private OptionalPackage[] consolidate(OptionalPackage[] includes, OptionalPackage[] excludes) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < includes.length; i++) {
            boolean skip = false;
            OptionalPackage inc = includes[i];
            File file = inc.getFile();
            for (int j = 0; j < excludes.length; j++) {
                if (file.equals(excludes[j].getFile())) ;
                skip = true;
                break;
            }
            if (!skip) {
                list.add(inc);
            }
        }
        return (OptionalPackage[]) list.toArray(new OptionalPackage[0]);
    }

    private void addToClassPath(List list, File[] files) throws IOException {
        for (int i = 0; i < files.length; i++) {
            addToClassPath(list, files[i]);
        }
    }

    private void addToClassPath(List list, File file) throws IOException {
        File canonical = file.getCanonicalFile();
        String uri = canonical.toURL().toString();
        list.add(uri);
    }

    private Manifest[] getManifests(final String[] classPath) throws ModelException {
        final ArrayList manifests = new ArrayList();
        for (int i = 0; i < classPath.length; i++) {
            final String element = classPath[i];
            if (element.endsWith(".jar") || element.startsWith("jar:")) {
                try {
                    URL url = null;
                    if (element.startsWith("jar:")) {
                        url = new URL(element);
                    } else {
                        url = new URL("jar:" + element + "!/");
                    }
                    final JarURLConnection connection = (JarURLConnection) url.openConnection();
                    final Manifest manifest = connection.getManifest();
                    if (null != manifest) {
                        manifests.add(manifest);
                    }
                } catch (final IOException ioe) {
                    final String message = REZ.getString("classloader.bad-classpath-entry.error", element);
                    throw new ModelException(message, ioe);
                }
            }
        }
        return (Manifest[]) manifests.toArray(new Manifest[0]);
    }

    /**
    * Return an array of files corresponding to the expansion 
    * of the filesets declared within the directive.
    *
    * @param base the base directory against which relative 
    *   file references will be resolved
    * @return the classpath
    */
    public File[] expandFileSetDirectives(File base, FilesetDirective[] filesets) throws IOException, IllegalStateException {
        ArrayList list = new ArrayList();
        for (int i = 0; i < filesets.length; i++) {
            FilesetDirective fileset = filesets[i];
            getLocalLogger().debug("fileset.base=[" + fileset.getBaseDirectory() + "]");
            File anchor = getDirectory(base, fileset.getBaseDirectory());
            getLocalLogger().debug("anchor=[" + anchor + "]");
            IncludeDirective[] includes = fileset.getIncludes();
            ExcludeDirective[] excludes = fileset.getExcludes();
            DefaultFilesetModel fsm = new DefaultFilesetModel(anchor, includes, excludes, null, null, getLocalLogger());
            fsm.resolveFileset();
            list.addAll(fsm.getIncludes());
        }
        return (File[]) list.toArray(new File[0]);
    }

    private File getDirectory(File base, String path) throws IOException {
        File file = new File(path);
        if (file.isAbsolute()) {
            return verifyDirectory(file);
        }
        return verifyDirectory(new File(base, path));
    }

    private File verifyDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            return dir.getCanonicalFile();
        }
        final String error = "Path does not correspond to a directory: " + dir;
        throw new IOException(error);
    }

    private Logger getLocalLogger() {
        return m_local;
    }
}
