package org.simpledm.core;

import static org.simpledm.core.utils.Assert._assert;
import static org.simpledm.core.utils.Log.trace;
import groovy.lang.GroovyClassLoader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import org.simpledm.core.dsl.Configuration;
import org.simpledm.core.utils.Log;
import org.simpledm.core.utils.Utils;

/**
 * Module class loader
 * 
 * @author Alexandre P. Clement
 * 
 *  
 * 
 */
@SuppressWarnings("unchecked")
public class ModuleClassLoader extends GroovyClassLoader {

    ModuleManager moduleManager;

    /**
	 * module dependency list a module dependency is represented by a map
	 */
    Collection<Map> moduleDeps;

    /**
	 * module dependency that this classloader is handling
	 * 
	 */
    Map moduleDep;

    /**
	 * loaded classes
	 */
    Collection<Class> loadedClasses = new LinkedHashSet<Class>();

    /**
	 * Module URLs
	 */
    Collection<URL> moduleUrls;

    /**
	 * Dependency URIs
	 */
    Collection<URI> uris;

    /**
	 * Classloader used to find resources
	 */
    URLClassLoader ucl;

    Map resolveArgs;

    /**
	 * Module started
	 */
    boolean moduleStarted;

    Date startingDate;

    Configuration configuration;

    ModuleObserver observer;

    boolean developmentStage;

    Collection<Map> loadedDeps = new LinkedHashSet<Map>();

    Collection<Map> startedDeps = new LinkedHashSet<Map>();

    /**
	 * Constructor; initializes the loader with an empty list of delegates.
	 */
    public ModuleClassLoader(ClassLoader parent, Map dep) throws Exception {
        super(parent);
        this.moduleDep = dep;
    }

    public void init(ModuleDescriptor descriptor) throws Exception {
        try {
            uris = new LinkedHashSet<URI>(descriptor.getUris());
            moduleDeps = new LinkedHashSet<Map>(descriptor.getModuleDeps());
            moduleUrls = new LinkedHashSet<URL>(descriptor.getModuleUrls());
            developmentStage = descriptor.isDevelopmentStage();
            for (URL url : moduleUrls) {
                addURL(url);
            }
            ucl = new URLClassLoader(Utils.toURLs(uris));
        } finally {
        }
    }

    protected void finalize() {
        for (URL url : moduleUrls) {
            try {
                url.openConnection().setDefaultUseCaches(false);
            } catch (Exception e) {
                trace(e.getMessage());
            }
        }
    }

    public void start() {
        moduleStarted = true;
        startingDate = new Date();
        if (developmentStage) {
            observer = new ModuleObserver(this, moduleManager);
            observer.start();
        }
    }

    public void stop() {
        if (observer != null) {
            observer.setStop(true);
        }
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        Class result = null;
        try {
            Log.depth++;
            if (Log.depth == 1) trace("\n");
            trace("START:   MCL[" + moduleDep + "]: Start to find class " + name);
            Collection<Map> modules = moduleManager.resolveModule(name, moduleDeps);
            if (modules.isEmpty()) {
                modules = moduleManager.resolveModule(name, getContextClassLoader().moduleDeps);
            }
            _assert(!modules.isEmpty(), "MCL cannot resolve module");
            result = findClassInDependencies(name, modules);
            if (result == null) {
                trace("FAILURE: MCL[" + moduleDep + "]: Class not found: " + name);
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            trace("FAILURE: MCL[" + moduleDep + "]: Runtime Failure: Class can not be load: " + name);
            throw new ClassNotFoundException("Runtime Failure: Class can not be load: " + name + "; " + e.getMessage(), e);
        } finally {
            Log.depth--;
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    protected Class findClassInDependencies(String name, Collection<Map> deps) throws Exception {
        Class result = null;
        for (Map module : deps) {
            try {
                if (module.equals(moduleDep)) {
                    try {
                        result = super.findClass(name);
                    } catch (ClassNotFoundException e) {
                        String path = name.replace('.', '/') + ".groovy";
                        URL url = super.findResource(path);
                        if (url == null) throw e;
                        try {
                            result = parseClass(url.openStream());
                        } finally {
                            url.openConnection().setDefaultUseCaches(false);
                        }
                    }
                    loadedClasses.add(result);
                } else {
                    ModuleClassLoader mcl = moduleManager.getMcl(module);
                    if (mcl == null) {
                        moduleManager.assureModuleStarted(module);
                        mcl = moduleManager.getMcl(module);
                        startedDeps.add(module);
                    }
                    result = mcl.loadClass(name);
                    loadedDeps.add(module);
                }
                trace("SUCCESS: MCL[" + module + "]: Successfuly load class: " + name);
                break;
            } catch (ClassNotFoundException e) {
            }
        }
        return result;
    }

    @Override
    public URL findResource(String name) {
        trace("RESOURCE: MCL[" + moduleDep + "]:" + name);
        ModuleClassLoader ccl = getContextClassLoader();
        trace("RESOURCE: delegating to context CL: MCL[" + ccl.moduleDep + "]:" + name);
        URL result = ccl.getUcl().findResource(name);
        return result;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        ModuleClassLoader ccl = getContextClassLoader();
        return ccl.getUcl().findResources(name);
    }

    public URL getModuleResource(String name) {
        return null;
    }

    private ModuleClassLoader getContextClassLoader() {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        return (ModuleClassLoader) (result != null && result instanceof ModuleClassLoader ? result : this);
    }

    /**
	 * Add a dependency dynamically
	 * @param md the ModuleDescriptor
	 */
    public void addDependency(ModuleDescriptor md) {
        moduleDeps.addAll(md.getModuleDeps());
        uris.addAll(md.getUris());
        ucl = new URLClassLoader(Utils.toURLs(uris));
    }

    public Collection<Map> getModuleDeps() {
        return moduleDeps;
    }

    public void setModuleDeps(Collection<Map> moduleDeps) {
        this.moduleDeps = moduleDeps;
    }

    /**
	 * Rename to getLoadedClassesAsCollection() to avoid clash with super.getLoadedClasses()
	 */
    public Collection<Class> getLoadedClassesAsCollection() {
        return loadedClasses;
    }

    public URLClassLoader getUcl() {
        return ucl;
    }

    public boolean isModuleStarted() {
        return moduleStarted;
    }

    public void setModuleStarted(boolean moduleStarted) {
        this.moduleStarted = moduleStarted;
    }

    public Date getStartingDate() {
        return startingDate;
    }

    public void setStartingDate(Date startingDate) {
        this.startingDate = startingDate;
    }

    public ModuleObserver getObserver() {
        return observer;
    }

    public void setObserver(ModuleObserver observer) {
        this.observer = observer;
    }

    public boolean isDevelopmentStage() {
        return developmentStage;
    }

    public void setDevelopmentStage(boolean developmentStage) {
        this.developmentStage = developmentStage;
    }

    public Map getModuleDep() {
        return moduleDep;
    }

    public void setModuleDep(Map moduleDep) {
        this.moduleDep = moduleDep;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public void setModuleManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Collection<Map> getLoadedDeps() {
        return loadedDeps;
    }

    public Collection<Map> getStartedDeps() {
        return startedDeps;
    }

    public Collection<Class> getAllLoadedClasses() {
        return loadedClasses;
    }
}
