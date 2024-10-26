package net.hakulaite.maverick;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import net.hakulaite.maverick.model.MavElement;
import net.hakulaite.maverick.model.MaverickModelFactory;
import net.hakulaite.maverick.model.MaverickParser;
import net.hakulaite.maverick.model.TransformElement;
import net.hakulaite.maverick.model.ViewElement;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.core.internal.events.ResourceChangeListenerList;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class MaverickPlugin extends AbstractUIPlugin {

    private static Logger log = Logger.getLogger(MaverickPlugin.class);

    private static MaverickPlugin plugin;

    private ResourceBundle resourceBundle;

    private IResourceChangeListener listener;

    /**
   * The constructor.
   */
    public MaverickPlugin() {
        super();
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        String pluginlogfile = "";
        plugin = this;
        try {
            URL url = this.getClass().getResource("/net/hakulaite/maverick/log4j.properties");
            Properties properties = new Properties();
            properties.load(url.openStream());
            pluginlogfile = Platform.getPluginStateLocation(this) + "/" + properties.getProperty("log4j.appender.R.File");
            if (Platform.inDebugMode()) {
                System.out.println("Opening plugin log file: " + pluginlogfile);
            }
            properties.setProperty("log4j.appender.R.File", pluginlogfile);
            PropertyConfigurator.configure(properties);
        } catch (Exception e) {
            System.err.println("Could not initialize logger, beacuse " + e);
        }
        log.info("Initializing maverick plugin");
        IContainer root = getWorkspace().getRoot();
        log.debug("Searching for maverick.xml");
        find(root, "maverick.xml");
        log.debug("Search completed");
        ImageRegistry ireg = getImageRegistry();
        try {
            resourceBundle = ResourceBundle.getBundle("net.hakulaite.maverick.resources");
        } catch (MissingResourceException x) {
            log.error("Cannot load resources 'net.hakulaite.maverick.resources'");
            resourceBundle = null;
        }
        String imgprefix = getResourceString("plugin.images.prefix");
        String imgpostfix = getResourceString("plugin.images.url.postfix");
        String preloadimages = getResourceString("plugin.preloadimages");
        StringTokenizer st = new StringTokenizer(preloadimages, ",");
        while (st.hasMoreElements()) {
            String imagename = (String) st.nextElement();
            String imageurl = getResourceString(imgprefix + "." + imagename + "." + imgpostfix);
            if (imagename != null && imageurl != null) addImageToRegistry(imagename, imageurl, ireg);
        }
        checkClasspathVariable();
        getWorkspace().addResourceChangeListener(new MaverickResourceChangeReporter(), IResourceChangeEvent.POST_CHANGE);
        log.info("Initialization complete.");
    }

    /**
   * Adds image to registry
   * 
   * @param key
   * @param image
   * @param reg
   */
    public static void addImageToRegistry(String key, String image, ImageRegistry reg) {
        try {
            log.debug("Adding image to registry:" + image);
            ImageDescriptor imaged = AbstractUIPlugin.imageDescriptorFromPlugin(IMaverickConstants.PLUGIN_ID, image);
            addImageToRegistry(key, imaged, reg);
        } catch (Exception e) {
            log.error("Could not add image 'to registry'" + image + "' to registry");
        }
    }

    /**
   * Adds image to registry
   * 
   * @param key
   * @param image
   * @param reg
   */
    public static void addImageToRegistry(String key, ImageDescriptor image, ImageRegistry reg) {
        reg.put(key, image);
    }

    /**
   * Find resource (file) under container (folder)
   * 
   * @param container
   *          container to look under
   * @param name
   *          file to look for
   */
    public static void find(IContainer container, String name) {
        IResource resources[];
        try {
            resources = container.members();
            for (int i = 0; i < resources.length; i++) {
                if (resources[i] instanceof IFile && name.equals(resources[i].getName())) {
                    log.debug("found " + name + ":" + resources[i].toString());
                    IProject project = resources[i].getProject();
                    log.info("projects property:" + getProjectProperty(project, "maverick.xml"));
                    log.info("setting property");
                    setProjectProperty(project, "maverick.xml", resources[i].getProjectRelativePath().toString());
                    log.info("projects property now:" + getProjectProperty(project, "maverick.xml"));
                }
                if (resources[i] instanceof IProject && resources[i].getProject().isAccessible()) {
                    if (!((IProject) resources[i]).hasNature(IMaverickConstants.MAVERICK_NATURE_ID)) {
                        log.debug("recurse project " + resources[i].getName());
                        find((IContainer) resources[i], name);
                    } else {
                        log.debug("project " + resources[i].getName() + " allready has maverick nature!");
                    }
                } else if (resources[i] instanceof IFolder) {
                    log.debug("recurse folder " + resources[i].getName());
                    find((IContainer) resources[i], name);
                }
            }
        } catch (CoreException e) {
            log.error("Error in identifying project.", e);
        }
    }

    /**
   * Returns the shared instance.
   */
    public static MaverickPlugin getDefault() {
        return plugin;
    }

    /**
   * Returns the workspace instance.
   */
    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   */
    public static String getResourceString(String key) {
        ResourceBundle bundle = MaverickPlugin.getDefault().getResourceBundle();
        if (bundle != null) {
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                log.error("could not find value for key '" + key + "'");
                return null;
            }
        } else {
            log.error("bundle was null, could not find value for key '" + key + "'");
            return null;
        }
    }

    /**
   * Returns the plugin's resource bundle,
   */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
   * Locate source directories of this project.
   * 
   * @param project
   *          target project
   * @return
   */
    public static Vector getSrcDirs(IProject project) {
        Vector srcpaths = new Vector();
        IResource classpath = project.findMember(".classpath");
        if (classpath instanceof IFile) {
            try {
                Document doc = parseDoc(((IFile) classpath).getContents());
                if (doc != null) {
                    Iterator i = doc.getRootElement().getChildren("classpathentry").iterator();
                    while (i.hasNext()) {
                        Element e = (Element) i.next();
                        if (e.getAttributeValue("kind") != null) {
                            srcpaths.add(e.getAttributeValue("path"));
                        }
                    }
                }
            } catch (CoreException e) {
                log.error(e, e);
            }
        }
        return srcpaths;
    }

    /**
   * Parse xml document
   * 
   * @param inputstream
   * @return parsed document
   */
    public static Document parseDoc(InputStream inputstream) {
        SAXBuilder builder = new SAXBuilder();
        try {
            return builder.build(inputstream);
        } catch (JDOMException e) {
            log.error(e, e);
        } catch (IOException e) {
            log.error(e, e);
        }
        return null;
    }

    public static String getWorkspaceRootDir() {
        return MaverickPlugin.getWorkspace().getRoot().getRawLocation().toString();
    }

    /**
   * returns a projects property
   */
    public static String getProjectProperty(IProject project, String name) {
        String property = null;
        if (project != null && project.exists()) {
            try {
                property = (String) project.getPersistentProperty(new QualifiedName("maveric", name));
            } catch (CoreException e) {
                log.error(e, e);
            }
        }
        return property;
    }

    public static void setProjectProperty(IProject project, String name, String value) {
        try {
            project.setPersistentProperty(new QualifiedName("maveric", name), value);
        } catch (CoreException e) {
            log.error(e, e);
        }
    }

    /**
   * returns a projects property or default
   */
    public static String getProjectProperty(IProject project, String name, String default_value) {
        String property = default_value;
        try {
            property = (String) project.getPersistentProperty(new QualifiedName("maveric", name));
        } catch (CoreException e) {
            log.error(e, e);
        }
        return property;
    }

    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        getWorkspace().removeResourceChangeListener(listener);
    }

    /**
   * 
   * @param file
   * @return
   */
    public boolean isController(IFile file) {
        final Vector what = new Vector();
        what.add("org.infohazard.maverick.ctl.ThrowawayBean2");
        try {
            IJavaProject javaproject = JavaCore.create(file.getProject());
            boolean isOnClasspath = javaproject.isOnClasspath(file);
            if (!isOnClasspath) {
                log.info("not on classpath, ignoring");
                return false;
            }
            IJavaElement element = JavaCore.create(file);
            if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
                ICompilationUnit cu = (ICompilationUnit) element;
                return superContains(cu, what);
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        return false;
    }

    public boolean superContains(ICompilationUnit cu, Vector what) {
        try {
            String superclassname = cu.findPrimaryType().getSuperclassName();
            if (superclassname == null) return false;
            String fqn = getfullyQualifiedName(superclassname, cu);
            if (fqn.equals(superclassname)) return false;
            if (what.contains(fqn)) return true;
            IType parenttype = cu.getJavaProject().findType(fqn);
            ICompilationUnit supercu = parenttype.getCompilationUnit();
            if (supercu == null) return false;
            return superContains(supercu, what);
        } catch (JavaModelException e) {
            log.error(e, e);
        }
        return false;
    }

    public String getfullyQualifiedName(String name, ICompilationUnit cu) {
        String fqn = "";
        try {
            if (cu.getJavaProject().findType(name) != null) {
                return name;
            }
        } catch (JavaModelException e) {
            log.error(e, e);
        }
        try {
            fqn = cu.getPackageDeclarations()[0].getElementName() + "." + name;
            if (cu.getJavaProject().findType(fqn) != null) {
                return fqn;
            }
        } catch (JavaModelException e2) {
            log.error(e2, e2);
        }
        IImportDeclaration[] imports;
        try {
            imports = cu.getImports();
            for (int i = 0; i < imports.length; i++) {
                String ename = imports[i].getElementName();
                if (ename.indexOf('*') != -1) {
                    fqn = ename.substring(0, ename.length() - 1) + name;
                    if (cu.getJavaProject().findType(fqn) != null) {
                        return fqn;
                    }
                } else if (ename.endsWith("." + name)) {
                    fqn = imports[i].getElementName();
                    if (cu.getJavaProject().findType(fqn) != null) {
                        return fqn;
                    }
                }
            }
        } catch (JavaModelException e1) {
            log.error(e1, e1);
        }
        return name;
    }

    /**
   * Returns true if folder is webroot folder
   * 
   * @param file
   * @return
   */
    public boolean isWebRoot(IFolder folder) {
        IProject project = folder.getProject();
        String webroot = getWebRoot(project);
        String folderpath = folder.getProjectRelativePath().toString();
        log.debug("weboot is " + webroot);
        log.debug("folder is " + folderpath);
        log.debug("will return:" + folderpath.equals(webroot));
        return folderpath.equals(webroot);
    }

    /**
   * Get webroot of project
   * 
   * @param project
   * @return
   */
    public String getWebRoot(IProject project) {
        String webroot = getProjectProperty(project, getResourceString("properties.webroot.property.name"));
        if (webroot == null) webroot = getResourceString("properties.webroot.default.value");
        return webroot;
    }

    /**
   * Check if file is a view defined in maverick.xml
   * 
   * @param file
   * @return
   */
    public boolean isView(IFile file) {
        IProject proj = file.getProject();
        String maverickxml = getProjectProperty(proj, "maverick.xml");
        String webroot = getWebRoot(proj);
        if (maverickxml == null) {
            return false;
        }
        IFile maverickxmlfile = proj.getFile(maverickxml);
        MavElement me[] = MaverickModelFactory.getInstance().getElements(maverickxmlfile);
        if (file.getProjectRelativePath().toString().length() > webroot.length()) {
            String fileName = file.getProjectRelativePath().toString().substring(webroot.length() + 1);
            for (int i = 0; i < me.length; i++) {
                if (me[i] instanceof ViewElement) {
                    ViewElement ve = (ViewElement) me[i];
                    if (fileName.equals(ve.getElement().getAttributeValue(MaverickParser.ATTR_VIEW_PATH))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
   * Check if file is a transform defined in maverick.xml
   * 
   * @param file
   * @return
   */
    public boolean isTransform(IFile file) {
        IProject proj = file.getProject();
        String maverickxml = getProjectProperty(proj, "maverick.xml");
        String webroot = getWebRoot(proj);
        if (maverickxml == null) {
            return false;
        }
        IFile maverickxmlfile = proj.getFile(maverickxml);
        if (file.getProjectRelativePath().toString().length() > webroot.length()) {
            String fileName = file.getProjectRelativePath().toString().substring(webroot.length() + 1);
            MavElement me[] = MaverickModelFactory.getInstance().getElements(maverickxmlfile);
            for (int i = 0; i < me.length; i++) {
                if (me[i] instanceof TransformElement) {
                    TransformElement te = (TransformElement) me[i];
                    if (fileName.equals(te.getElement().getAttributeValue(MaverickParser.ATTR_TRANSFORM_PATH))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
   * 
   */
    public void checkClasspathVariable() {
        if ("devel".equals(MaverickPlugin.getResourceString("plugin.mode"))) {
            return;
        }
        String pluginversionstring = MaverickPlugin.getResourceString("plugin.version.string");
        IPath path = JavaCore.getClasspathVariable(MaverickPlugin.getResourceString("home.variable.mavplug.name"));
        if (path != null && path.toString().indexOf(pluginversionstring) == -1) {
            log.info("version mismatch?, removing variable");
            try {
                path = null;
                JavaCore.removeClasspathVariable(MaverickPlugin.getResourceString("home.variable.mavplug.name"), null);
            } catch (Exception e) {
                log.error("Could not remove variable.");
            }
        }
        if (path == null) {
            IPath ECLIPSE_HOME = JavaCore.getClasspathVariable("ECLIPSE_HOME");
            if (ECLIPSE_HOME != null) {
                try {
                    JavaCore.setClasspathVariable(MaverickPlugin.getResourceString("home.variable.mavplug.name"), ECLIPSE_HOME.append("plugins/net.hakulaite.maverick_" + pluginversionstring + "/resources/"), null);
                } catch (JavaModelException e) {
                    log.error(e);
                }
            } else {
                log.error("Variable ECLIPSE_HOME is null, cannot proceed!");
            }
        }
    }

    public static boolean isMaverickProject(IProject p) {
        return getProjectProperty(p, "maverick.xml") != null;
    }
}
