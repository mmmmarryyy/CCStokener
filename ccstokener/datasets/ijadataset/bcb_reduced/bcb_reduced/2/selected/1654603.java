package net.sf.jsfcomp.facelets.deploy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;
import net.sf.jsfcomp.facelets.deploy.DeploymentFinderFactory.FactoryType;
import net.sf.jsfcomp.facelets.deploy.annotations.FaceletComponent;
import net.sf.jsfcomp.facelets.deploy.annotations.FaceletConverter;
import net.sf.jsfcomp.facelets.deploy.annotations.FaceletElFunction;
import net.sf.jsfcomp.facelets.deploy.annotations.FaceletFunctionHolder;
import net.sf.jsfcomp.facelets.deploy.annotations.FaceletTagHandler;
import net.sf.jsfcomp.facelets.deploy.annotations.FaceletValidator;
import net.sf.jsfcomp.facelets.deploy.annotations.FacesRenderer;
import net.sf.jsfcomp.facelets.deploy.factory.AnnotationTagLibraryFactory;
import org.apache.commons.collections.iterators.EnumerationIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sun.facelets.FaceletFactory;
import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.impl.DefaultFaceletFactory;

/**
 * The parser that scans the classpath for annotated classes, creating tag libraries
 * and registers them with the facelets compiler.
 * 
 * @see #parse()
 * @see Compiler
 * @see AnnotationTagLibrary
 * @author Andrew Robinson (andrew)
 */
public class FaceletAnnotationParser {

    private static final Log log = LogFactory.getLog(FaceletAnnotationParser.class);

    public static final String MANIFEST_SECTION_NAME = "net/sf/jsf-comp/facelets/deployment/";

    private AnnotationTagLibraryFactory annotationTagLibraryFactory;

    private Map<String, AnnotationTagLibrary> tagLibraryMap;

    /**
   * Default constructor (required for ParserFactory)
   */
    public FaceletAnnotationParser() {
    }

    /**
   * The work-horse of the class. Parses the classpath to find
   * classes that contain facelets deployment annotations
   * @return Set of configured tag libraries
   * @throws ParserException thrown if there was an issue
   * during parsing
   */
    public Set<AnnotationTagLibrary> parse() throws ParserException {
        try {
            tagLibraryMap = new HashMap<String, AnnotationTagLibrary>();
            FacesContext context = FacesContext.getCurrentInstance();
            log.debug("Scanning classpath...");
            scanClasspath(context);
            return new HashSet<AnnotationTagLibrary>(tagLibraryMap.values());
        } catch (Exception ex) {
            log.error("Unable to scan for facelet components", ex);
            throw new ParserException(ex);
        } finally {
            tagLibraryMap = null;
        }
    }

    /**
   * Convenience method that gets the compiler from the facelet factory and
   * registers the tag libraries with it. Requires that
   * {@link FaceletFactory.getInstance()} returns an instance of
   * {@link DefaultFaceletFactory}.
   * <p>
   * Once the compiler is obtained, it calls
   * {@link #addToFacelets(Set, Compiler)}.
   * </p>
   * 
   * @see DefaultFaceletFactory#getCompiler()
   * @throws IllegalStateException thrown if the facelet factory instance is not
   *         of type <tt>DefaultFaceletFactory</tt>
   * @see DefaultFaceletFactory
   * @see FaceletFactory#getInstance()
   */
    public static void addToFacelets(Set<AnnotationTagLibrary> libraries) throws IllegalStateException {
        FaceletFactory factory = FaceletFactory.getInstance();
        if (!(factory instanceof DefaultFaceletFactory)) throw new IllegalStateException("Annotation deployer on works with DefaultFaceletFactory");
        addToFacelets(libraries, ((DefaultFaceletFactory) factory).getCompiler());
    }

    /**
   * Convenience method that adds the given libraries to the compiler given
   * 
   * @param libraries the tag libraries to add
   * @param c the compiler to add them to
   */
    public static void addToFacelets(Set<AnnotationTagLibrary> libraries, Compiler c) {
        for (AnnotationTagLibrary tagLibrary : libraries) c.addTagLibrary(tagLibrary);
    }

    /**
   * Scan the classes in a jar file on the classpath
   * 
   * @param context the faces context to use when registering JSF items
   * @param jarFile the jar file to scan
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws IOException opening and scanning the jar file errors
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void scanClassesInJar(FacesContext context, File jarFile, RegisterPriority defaultPriority) throws IOException, FactoryException {
        JarFile jar = new JarFile(jarFile);
        log.trace("Scanning jar file: " + jarFile);
        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            log.trace("Checking jar entry: " + entry.getName());
            String className = getClassName(entry.getName());
            if (className == null) {
                log.trace("Could not get class name");
                continue;
            }
            scanClass(context, className, defaultPriority);
        }
    }

    /**
   * Scan the given directory
   * 
   * @param context the faces context to use when registering JSF items
   * @param dir the directory
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   */
    protected void scanClassesInDirectory(FacesContext context, File dir, RegisterPriority defaultPriority) throws FactoryException {
        scanClassesInDirectory(context, dir, new StringBuilder(), defaultPriority);
    }

    /**
   * Called from
   * {@link #scanClassesInDirectory(FacesContext, File) scanClassesInDirectory}
   * and recursively calls itself looking for files with a .class extension in
   * sub-directories
   * 
   * @param context the faces context to use when registering JSF items
   * @param dir the current directory
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @param path the path to this directory starting after the directory in the
   *        classpath (so the folders in this path should mirror the package of
   *        the class file)
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void scanClassesInDirectory(FacesContext context, File dir, StringBuilder path, RegisterPriority defaultPriority) throws FactoryException {
        log.trace("Scanning directory: " + dir);
        int origLen = path.length();
        for (File file : dir.listFiles()) {
            int len = path.length();
            if (len > 0) path.append('/');
            path.append(file.getName());
            if (file.isDirectory()) scanClassesInDirectory(context, file, path, defaultPriority); else {
                log.trace("Checking path: " + path);
                String className = getClassName(path.toString());
                if (className == null) log.trace("Could not get class name from file: " + path); else scanClass(context, className, defaultPriority);
            }
            path.setLength(len);
        }
        path.setLength(origLen);
    }

    /**
   * Translate a file system path or a jar file path into a class name.
   * If the name does not appear to be a class or a class that supports
   * deployment (for example, an inner class), this method should return null.
   * @param name the file name and relative path from the directory or the
   * entry name from the jar file
   * @return The java-compatible class name or null if not supported
   */
    protected String getClassName(String name) {
        if (!name.endsWith(".class") || name.contains("$")) return null;
        name = name.substring(0, name.length() - 6);
        return name.replace('/', '.').replace('\\', '.');
    }

    /**
   * Loads the <tt>Class</tt> for the given name and then checks it for
   * facelet deployment annotations
   * 
   * @param context the faces context to use when registering JSF items
   * @param name the class name
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void scanClass(FacesContext context, String name, RegisterPriority defaultPriority) throws FactoryException {
        log.trace("Scanning class: " + name);
        Class<?> cls;
        try {
            cls = Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (Exception ex) {
            log.warn("Failed to load class", ex);
            return;
        }
        if (cls.isAnnotationPresent(FaceletFunctionHolder.class)) parseFunctions(cls);
        if (cls.isAnnotationPresent(FaceletConverter.class)) parseConverter(context, cls, defaultPriority);
        if (cls.isAnnotationPresent(FaceletValidator.class)) parseValidator(context, cls, defaultPriority);
        if (cls.isAnnotationPresent(FaceletComponent.class)) parseComponent(context, cls, defaultPriority);
        if (cls.isAnnotationPresent(FaceletTagHandler.class)) parseTagHandler(context, cls, defaultPriority);
        if (cls.isAnnotationPresent(FacesRenderer.class)) parseRenderer(context, cls, defaultPriority);
        postScan(context, cls, defaultPriority);
    }

    /**
   * Allows sub-classes to do any further work on the given class. Called from
   * {@link #scanClass(FacesContext, String), scanClass}.
   * 
   * @param context the faces context to use when registering JSF items
   * @param cls the class to scan
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void postScan(FacesContext context, Class<?> cls, RegisterPriority defaultPriority) throws FactoryException {
    }

    /**
   * Parse a class that has the {@link FaceletConverter} annotation
   * 
   * @param context the faces context to use when registering JSF items
   * @param cls the class
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void parseConverter(FacesContext context, Class<?> cls, RegisterPriority defaultPriority) throws FactoryException {
        FaceletConverter annotation = cls.getAnnotation(FaceletConverter.class);
        getTagLibrary(annotation.namespace()).add(annotation);
        RegisterPriority priority = getEffectivePriority(annotation.registerWithFaces(), defaultPriority);
        if (priority == RegisterPriority.NONE) {
            log.debug("Not registering converter class with faces: " + cls);
            return;
        }
        boolean skipClass = void.class.equals(annotation.forClass());
        boolean skipId = false;
        Application app = context.getApplication();
        if (priority == RegisterPriority.UNLESS_REGISTERED) {
            if (!skipId && iteratorContains(app.getConverterIds(), annotation.id())) skipId = true;
            if (!skipClass && iteratorContains(app.getConverterTypes(), annotation.forClass())) skipClass = true;
            if (skipId && skipClass) {
                log.debug("Not registering converter class with faces (already registered): " + cls);
                return;
            }
        }
        if (!skipId) {
            log.debug("Registering converter class with faces (with ID): " + cls);
            app.addConverter(annotation.id(), cls.getName());
        }
        if (!skipClass) {
            log.debug("Registering converter class with faces (with class): " + cls);
            app.addConverter(annotation.forClass(), cls.getName());
        }
    }

    /**
   * Parse a class that has the {@link FaceletValidator} annotation
   * 
   * @param context the faces context to use when registering JSF items
   * @param cls the class
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void parseValidator(FacesContext context, Class<?> cls, RegisterPriority defaultPriority) throws FactoryException {
        FaceletValidator annotation = cls.getAnnotation(FaceletValidator.class);
        getTagLibrary(annotation.namespace()).add(annotation);
        RegisterPriority priority = getEffectivePriority(annotation.registerWithFaces(), defaultPriority);
        if (priority == RegisterPriority.NONE) {
            log.debug("Not registering validator class with faces: " + cls);
            return;
        }
        Application app = context.getApplication();
        if (priority == RegisterPriority.UNLESS_REGISTERED) {
            if (iteratorContains(app.getValidatorIds(), annotation.id())) {
                log.debug("Not registering validator class with faces (already registered): " + cls);
                return;
            }
        }
        log.debug("Registering validator class with faces: " + cls);
        app.addValidator(annotation.id(), cls.getName());
    }

    /**
   * Parse a class that has the {@link FaceletTagHandler} annotation
   * 
   * @param context the faces context to use when registering JSF items
   * @param cls the class
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void parseTagHandler(FacesContext context, Class<?> cls, RegisterPriority defaultPriority) throws FactoryException {
        FaceletTagHandler annotation = cls.getAnnotation(FaceletTagHandler.class);
        getTagLibrary(annotation.namespace()).add(annotation, cls);
    }

    /**
   * Parse a class that has the {@link FaceletComponent} annotation
   * 
   * @param context the faces context to use when registering JSF items
   * @param cls the class
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void parseComponent(FacesContext context, Class<?> cls, RegisterPriority defaultPriority) throws FactoryException {
        FaceletComponent annotation = cls.getAnnotation(FaceletComponent.class);
        getTagLibrary(annotation.namespace()).add(annotation);
        RegisterPriority priority = getEffectivePriority(annotation.registerWithFaces(), defaultPriority);
        if (priority == RegisterPriority.NONE) {
            log.debug("Not registering component class with faces: " + cls);
            return;
        }
        Application app = context.getApplication();
        if (priority == RegisterPriority.UNLESS_REGISTERED) {
            if (iteratorContains(app.getComponentTypes(), annotation.type())) {
                log.debug("Not registering component class with faces (already registered): " + cls);
                return;
            }
        }
        log.debug("Registering component class with faces: " + cls);
        app.addComponent(annotation.type(), cls.getName());
    }

    /**
   * Parse a class for functions
   * 
   * @see FaceletFunctionHolder
   * @see FaceletElFunction
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void parseFunctions(Class<?> cls) throws FactoryException {
        FaceletFunctionHolder annotation = cls.getAnnotation(FaceletFunctionHolder.class);
        String defaultNs = annotation.defaultNamespace();
        boolean implicit = annotation.implicit();
        log.trace("Checking class for functions: " + cls);
        for (Method method : cls.getMethods()) {
            log.trace("Checking method: " + method);
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                log.trace("Method is not static");
                continue;
            }
            FaceletElFunction funcDef = method.getAnnotation(FaceletElFunction.class);
            if ((funcDef == null && !implicit) || (funcDef != null && implicit && !funcDef.visible())) {
                log.trace("Function is hidden");
                continue;
            }
            log.debug("Found function: " + method);
            String ns = funcDef != null ? funcDef.namespace() : null;
            getTagLibrary(ns == null || ns.length() == 0 ? defaultNs : ns).add(annotation, funcDef, method);
        }
    }

    /**
   * Scan the class path for annotated classes
   * 
   * @see #obtainLocationsToScan()
   * @param context the faces context to use when registering JSF items
   * @throws IOException if an IO exception occurs from the file system
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected void scanClasspath(FacesContext context) throws IOException, FactoryException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (ScanLocationInformation location : obtainLocationsToScan()) {
            if (location.defaultPriority == null || location.defaultPriority == RegisterPriority.DEFAULT) {
                log.warn(String.format("Default priority was incorrect for location " + "\"%s\". It cannot be null or DEFAULT. NONE will be used instead. " + "Value found: %s", location.location, location.defaultPriority));
                location.defaultPriority = RegisterPriority.NONE;
            }
            URL url = location.location;
            String urlPath = URLDecoder.decode(url.getFile(), "UTF-8");
            if (urlPath.startsWith("file:")) urlPath = urlPath.substring(5);
            int index = urlPath.indexOf('!');
            if (index > 0) urlPath = urlPath.substring(0, index); else if (urlPath.endsWith("MANIFEST.MF")) urlPath = new File(urlPath).getParentFile().getParent();
            log.trace("Scanning classpath entry: " + urlPath);
            File file = new File(urlPath);
            if (file.isDirectory()) scanClassesInDirectory(context, file, location.defaultPriority); else scanClassesInJar(context, file, location.defaultPriority);
        }
    }

    @SuppressWarnings("unchecked")
    protected Iterator<URL> getManifestsToScan() throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return new EnumerationIterator(cl.getResources("META-INF/MANIFEST.MF"));
    }

    /**
   * Obtain the URLs to scan for class files. This is protected to allow
   * sub-classes to override the current behavior of scanning the entire class
   * path for manifest files and then checking the contents of the manifest to
   * check and see if the path should be parsed.
   * 
   * @see #parseClassesInPath(URL, Manifest)
   * @return the locations to scan
   * @throws IOException opening and scanning the jar file errors
   */
    protected Iterable<ScanLocationInformation> obtainLocationsToScan() throws IOException {
        Set<ScanLocationInformation> locations = new HashSet<ScanLocationInformation>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (Iterator<URL> iter = getManifestsToScan(); iter.hasNext(); ) {
            URL url = iter.next();
            log.trace("Checking manifest at URL: " + url);
            InputStream is = url.openStream();
            Manifest mf = new Manifest();
            try {
                mf.read(is);
            } finally {
                is.close();
            }
            if (!parseClassesInPath(url, mf)) continue;
            ScanLocationInformation location = new ScanLocationInformation();
            location.location = url;
            String defaultLocation = getManifestValue(mf, MANIFEST_SECTION_NAME, "RegisterPriority", "NONE");
            try {
                location.defaultPriority = RegisterPriority.valueOf(defaultLocation.toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.error("Illeaal default priority \"%s\". NONE will be used instead", ex);
                location.defaultPriority = RegisterPriority.NONE;
            }
            locations.add(location);
        }
        return locations;
    }

    /**
   * Checks a manifest to see if the path should be parsed.
   * <p>
   * If this is not overridden, it checks for an attribute
   * <tt>Scan</tt> that is equal to <tt>true</tt> in
   * the named manifest section <tt>net/sf/jsf-comp/facelets</tt>.
   * </p>
   * 
   * @param manifestPath The URL to the manifest file
   * @param mf The manifest file data
   * @return true if the file should be parsed.
   */
    protected boolean parseClassesInPath(URL manifestPath, Manifest mf) {
        if (!"true".equals(getManifestValue(mf, MANIFEST_SECTION_NAME, "Scan", "false"))) {
            log.trace("Scan was not found " + "or was not true in this manifest");
            return false;
        } else return true;
    }

    /**
   * Parse a class that has the {@link FacesRenderer} annotation
   * 
   * @param context the faces context to use when registering JSF items
   * @param cls the class
   * @param defaultPriority The default priority to use when the annotation is
   *        set to {@link RegisterPriority#DEFAULT}
   */
    protected void parseRenderer(FacesContext context, Class<?> cls, RegisterPriority defaultPriority) {
        FacesRenderer annotation = cls.getAnnotation(FacesRenderer.class);
        RegisterPriority priority = getEffectivePriority(annotation.registerWithFaces(), defaultPriority);
        if (priority == RegisterPriority.NONE) {
            log.debug("Not registering renderer class with faces: " + cls);
            return;
        }
        String renderKitId = annotation.renderKitId();
        if ("".equals(renderKitId)) renderKitId = context.getApplication().getDefaultRenderKitId();
        RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        RenderKit renderKit = factory.getRenderKit(context, renderKitId);
        if (priority == RegisterPriority.UNLESS_REGISTERED && renderKit.getRenderer(annotation.componentFamily(), annotation.rendererType()) != null) {
            log.debug("Skipping renderer class (already registered): " + cls);
            return;
        }
        try {
            log.debug("Registering renderer class with faces: " + cls);
            renderKit.addRenderer(annotation.componentFamily(), annotation.rendererType(), (Renderer) cls.newInstance());
        } catch (Exception ex) {
            log.error("Failed to register renderer class " + cls, ex);
        }
    }

    /**
   * Get a tag library. If one doesn't already exist for the
   * given namespace, this method creates one.
   * 
   * @see #getTagLibraryFactory()
   * @param ns the namespace
   * @return the tag library for the namespace
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected final AnnotationTagLibrary getTagLibrary(String ns) throws FactoryException {
        AnnotationTagLibrary tagLibrary = tagLibraryMap.get(ns);
        if (tagLibrary == null) {
            log.debug("Creating new tag library for namespace: " + ns);
            tagLibrary = getTagLibraryFactory().createTagLibrary(ns);
            tagLibraryMap.put(ns, tagLibrary);
        }
        return tagLibrary;
    }

    /**
   * Utility function to see if a value is contained in an iterator
   */
    protected final boolean iteratorContains(Iterator<?> iter, Object value) {
        while (iter.hasNext()) {
            if (value.equals(iter.next())) return true;
        }
        return false;
    }

    /**
   * Get an instance of the tag library factory. If not overridden, this
   * method uses {@link DeploymentFinderFactory#createFactory(FactoryType)} to
   * obtain the factory that should be used.
   * 
   * @see DeploymentFinderFactory#createFactory(FactoryType)
   * @see FactoryType#TAGLIBRARY
   * @return the factory to use to create tag libraries
   * @throws FactoryException if there was an error getting the
   *         <tt>AnnotationTagLibraryFactory</tt>
   */
    protected AnnotationTagLibraryFactory getTagLibraryFactory() throws FactoryException {
        if (annotationTagLibraryFactory == null) annotationTagLibraryFactory = (AnnotationTagLibraryFactory) DeploymentFinderFactory.createFactory(FactoryType.TAGLIBRARY);
        return annotationTagLibraryFactory;
    }

    /**
   * Get a value from a named section in a manifest
   * 
   * @param mf the manifest
   * @param namedSection the section name
   * @param attrName the attribute name
   * @param defaultValueIfNotSet the value to return if the section or attribute
   *        were not found
   * @return the value or the default value
   */
    protected final String getManifestValue(Manifest mf, String namedSection, String attrName, String defaultValueIfNotSet) {
        Attributes attrs = mf.getAttributes(namedSection);
        if (attrs == null) return defaultValueIfNotSet;
        String value = attrs.getValue(attrName);
        return (value == null) ? defaultValueIfNotSet : value;
    }

    /**
   * Get the effective priority
   * @param value the value from the annotation
   * @param defaultValue the default value from the manifest
   * @return the priority to use
   */
    protected final RegisterPriority getEffectivePriority(RegisterPriority value, RegisterPriority defaultValue) {
        if (value == RegisterPriority.DEFAULT) return defaultValue; else return value;
    }
}
