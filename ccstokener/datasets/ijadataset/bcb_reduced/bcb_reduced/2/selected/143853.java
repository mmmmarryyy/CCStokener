package net.sourceforge.blogentis.plugins.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sourceforge.blogentis.om.Blog;
import net.sourceforge.blogentis.plugins.BlogPluginService;
import net.sourceforge.blogentis.plugins.IBlogExtensionPoint;
import net.sourceforge.blogentis.plugins.IExtension;
import net.sourceforge.blogentis.plugins.IExtensionPoint;
import net.sourceforge.blogentis.plugins.IPlugin;
import net.sourceforge.blogentis.plugins.IPluginService;
import net.sourceforge.blogentis.plugins.IPrefs;
import net.sourceforge.blogentis.utils.MappedConfiguration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.Turbine;
import org.apache.turbine.services.InitializationException;
import org.apache.turbine.services.TurbineBaseService;
import org.apache.turbine.services.factory.TurbineFactory;

/**
 * @author abas
 */
public class BlogPluginServiceImpl extends TurbineBaseService implements IPluginService {

    private static final String BLOG_EXTENSION_POINTS = "blog.extension.points";

    public static final String BLOG_PLUGIN_DEFERRED = "blogPluginDeferred";

    private static final Log log = LogFactory.getLog(BlogPluginServiceImpl.class);

    private IPlugin[] plugins = null;

    private ExtensionPointList globalExtensionPoints = new ExtensionPointList();

    private static final String PLUGIN_DESCRIPTOR = "plugins.resourceName";

    private static final String PLUGIN_DESCRIPTOR_DEFAULT = "/plugins.list";

    private static final String PLUGIN_CONFIG = "plugins.configFile";

    private static final String PLUGIN_CONFIG_DEFAULT = "/WEB-INF/conf/plugins.list";

    /**
     * Get the URIs of all files named "/plugins.list"
     * 
     * @return a List of the URIs of all files names "/plugin/list"
     * @throws IOException
     *             an exception propagated from the ClassLoader.
     */
    private List getPluginFileLists() throws IOException {
        Enumeration e = this.getClass().getClassLoader().getResources(getConfiguration().getString(PLUGIN_DESCRIPTOR, PLUGIN_DESCRIPTOR_DEFAULT));
        ArrayList l = new ArrayList();
        while (e.hasMoreElements()) l.add(e.nextElement());
        l.add(new File(Turbine.getRealPath(getConfiguration().getString(PLUGIN_CONFIG, PLUGIN_CONFIG_DEFAULT))).toURL());
        return l;
    }

    /**
     * Reads the classpath URIs in the pluginFileList and returns all plugin
     * classes defined by these URIs
     * 
     * The current implementation expects the URIs to point to files that
     * contain one-per-line class names.
     * 
     * @param pluginFileList
     *            the list of files to search for plugins
     * @return a list of class names.
     * @throws IOException
     */
    private List getPluginClassList(List pluginFileList) {
        ArrayList l = new ArrayList();
        for (Iterator i = pluginFileList.iterator(); i.hasNext(); ) {
            URL url = (URL) i.next();
            log.debug("Trying file " + url.toString());
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0 || line.charAt(0) == '#') continue;
                    l.add(line);
                }
            } catch (Exception e) {
                log.warn("Could not load " + url, e);
            }
        }
        return l;
    }

    /**
     * 
     * @param pluginClassnameList
     * @return
     */
    private List getPluginClasses(List pluginClassnameList) {
        ArrayList l = new ArrayList();
        for (Iterator i = pluginClassnameList.iterator(); i.hasNext(); ) {
            String className = (String) i.next();
            log.debug("Trying class " + className);
            try {
                Object plugin = TurbineFactory.getInstance(className);
                if (!(plugin instanceof IPlugin)) {
                    log.warn("Not a plugin: " + className);
                    continue;
                }
                l.add(plugin);
            } catch (Exception e1) {
                log.warn("Class not found " + className, e1);
                continue;
            } catch (LinkageError e) {
                log.error("Class not found " + className, e);
                continue;
            }
        }
        return l;
    }

    /**
     * Load the plugin classes. Searches in the classpath for all instances of
     * /plugin.list, which contains a list of classes, one per line. Empty lines
     * and lines starting with # will be ignored. Each class so found will be
     * loaded and checked on the implementation of the IPlugin interface.
     * 
     * @throws Exception
     */
    private void initPlugins() throws Exception {
        List tmp;
        tmp = getPluginFileLists();
        tmp = getPluginClassList(tmp);
        tmp = getPluginClasses(tmp);
        plugins = (IPlugin[]) tmp.toArray(new IPlugin[] {});
    }

    public void init() {
        log.debug("Loading plugins...");
        try {
            initPlugins();
        } catch (Exception e1) {
            log.error("Could not get the list of plugins", e1);
            plugins = new IPlugin[] {};
        }
        setInit(true);
        for (int i = 0; i < plugins.length; i++) {
            log.debug("Loading " + plugins[i].getName());
            try {
                plugins[i].startPlugin();
            } catch (InitializationException e) {
                log.error("Could not initialize plugin " + plugins[i].getName(), e);
            }
        }
    }

    public void shutdown() {
        for (int i = plugins.length - 1; i >= 0; i--) {
            plugins[i].stopPlugin();
        }
        setInit(false);
    }

    /**
     * List the plugins in the order of their definition.
     * 
     * @return an iterator that returns IPlugin
     */
    public Iterator getPlugins() {
        return Arrays.asList(plugins).iterator();
    }

    /**
     * Register a global extension point.
     * 
     * @param point
     *            the global extension point to register.
     */
    public synchronized void registerExtensionPoint(IExtensionPoint point) {
        globalExtensionPoints.registerExtensionPoint(point);
    }

    /**
     * Remove a global extension point.
     * 
     * @param point
     *            the extension point to remove.
     */
    public synchronized void deregisterExtensionPoint(IExtensionPoint point) {
        globalExtensionPoints.deregisterExtensionPoint(point);
    }

    /**
     * Register a blog-specific extension point.
     * 
     * @param blog
     *            the blog that the extension point will apply.
     * @param point
     *            the extension point that will be registered.
     */
    public void registerExtensionPoint(Blog blog, IBlogExtensionPoint point) {
        synchronized (blog) {
            ExtensionPointList epl = (ExtensionPointList) blog.getTemp(BLOG_EXTENSION_POINTS);
            if (epl == null) {
                epl = new ExtensionPointList();
                blog.setTemp(BLOG_EXTENSION_POINTS, epl);
            }
            epl.registerExtensionPoint(point);
            Map m = (Map) blog.getTemp(BLOG_PLUGIN_DEFERRED);
            if (m != null) {
                List toRemove = new ArrayList();
                for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
                    Class c = (Class) i.next();
                    if (c.isAssignableFrom(point.getClass())) {
                        List l = (List) m.get(c);
                        toRemove.add(c);
                        for (Iterator j = l.iterator(); j.hasNext(); ) {
                            IExtension ext = (IExtension) j.next();
                            ext.setPlugin(point.getPlugin());
                            point.addExtension(ext);
                        }
                    }
                }
                for (Iterator i = toRemove.iterator(); i.hasNext(); ) {
                    m.remove(i.next());
                }
            }
        }
    }

    /**
     * Remove a blog-specific extension point
     * 
     * @param blog
     *            the blog that should contain the extension point.
     * @param point
     *            the extension point to remove.
     */
    public void deregisterExtensionPoint(Blog blog, IBlogExtensionPoint point) {
        synchronized (blog) {
            ExtensionPointList epl = (ExtensionPointList) blog.getTemp(BLOG_EXTENSION_POINTS);
            if (epl != null) epl.deregisterExtensionPoint(point);
        }
    }

    public IExtensionPoint locateExtensionPoint(Class className) {
        return globalExtensionPoints.locateExtensionPoint(className);
    }

    public IBlogExtensionPoint locateExtensionPoint(Blog blog, Class className) {
        synchronized (blog) {
            ExtensionPointList epl = getBlogExtensionList(blog);
            return (IBlogExtensionPoint) epl.locateExtensionPoint(className);
        }
    }

    public void reloadExtensionPointsForBlog(Blog blog) {
        synchronized (blog) {
            if (log.isDebugEnabled()) log.debug("Reloading extension points for " + blog.getName());
            blog.setTemp(BLOG_EXTENSION_POINTS, null);
            getBlogExtensionList(blog);
        }
    }

    private List getValidPluginNames(Blog blog) {
        boolean doSave = false;
        MappedConfiguration config = blog.getConfiguration();
        List pluginNames = config.getList(BlogPluginService.PLUGIN_LIST, Collections.EMPTY_LIST);
        List unknown = new ArrayList(pluginNames);
        for (int j = 0; j < plugins.length; j++) unknown.remove(plugins[j].getClass().getName());
        pluginNames.removeAll(unknown);
        if (pluginNames.size() == 0) {
            pluginNames = new ArrayList(plugins.length);
            for (int i = 0; i < plugins.length; i++) pluginNames.add(plugins[i].getClass().getName());
            doSave = true;
        }
        if (doSave || unknown.size() > 0) {
            config.clearProperty(BlogPluginService.PLUGIN_LIST);
            config.addProperty(BlogPluginService.PLUGIN_LIST, pluginNames);
            try {
                config.save();
            } catch (Exception e) {
                log.error("Could not save configuration for " + blog.getName(), e);
            }
        }
        return pluginNames;
    }

    private ExtensionPointList getBlogExtensionList(Blog blog) {
        synchronized (blog) {
            ExtensionPointList epl = (ExtensionPointList) blog.getTemp(BLOG_EXTENSION_POINTS);
            if (epl != null) return epl;
            epl = new ExtensionPointList();
            blog.setTemp(BLOG_EXTENSION_POINTS, epl);
            List names = getValidPluginNames(blog);
            String[] enabled = (String[]) names.toArray(new String[names.size()]);
            for (int i = 0; i < enabled.length; i++) {
                for (int j = 0; j < plugins.length; j++) {
                    if (plugins[j].getClass().getName().equals(enabled[i])) {
                        plugins[j].registerInBlog(blog);
                    }
                }
            }
            return epl;
        }
    }

    public Iterator getExtensionPoints(Blog blog) {
        synchronized (blog) {
            ExtensionPointList epl = (ExtensionPointList) blog.getTemp(BLOG_EXTENSION_POINTS);
            LinkedList l = new LinkedList();
            CollectionUtils.addAll(l, epl.iterator());
            return l.iterator();
        }
    }

    public IPrefs getPreferencesFor(Blog blog, Class extensionClass) {
        return new PrefsImpl(blog, getClass().getName());
    }
}
