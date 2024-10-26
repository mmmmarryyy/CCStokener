package org.easygen.ui.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * @author eveno
 * Created on 9 mars 07
 *
 */
public class ModuleManagerImpl implements ModuleManager {

    private static final Logger logger = Logger.getLogger(ModuleManagerImpl.class);

    private static final Pattern nonCommentPattern = Pattern.compile("^([^#]+)");

    protected Map<String, Map<String, Module>> moduleKinds = new Hashtable<String, Map<String, Module>>();

    /**
	 *
	 */
    public ModuleManagerImpl() {
        initModules();
    }

    /**
	 * @throws ModuleException 
     *
     */
    protected void addModule(Module module) {
        String kind = module.getKind();
        logger.info("Loading module: " + module.getClass().getName() + ", kind: " + kind);
        if (module == null) return;
        if (moduleKinds.containsKey(kind) == false) {
            moduleKinds.put(kind, new Hashtable<String, Module>());
        }
        Map<String, Module> modules = moduleKinds.get(kind);
        if (modules.containsKey(module.getNature())) {
            Logger.getLogger(getClass()).warn("A module for this nature has already been registered: " + module.getNature() + " (" + modules.get(module.getNature()).getClass().getName() + ")");
        }
        modules.put(module.getNature(), module);
    }

    /**
	 * @throws ModuleNotFoundException
	 * @see org.easygen.ui.modules.ModuleManager#getModule(java.lang.String)
	 */
    public Module getModule(String kind, String pName) throws ModuleNotFoundException {
        if (kind == null || pName == null || moduleKinds.containsKey(kind) == false) throw new ModuleNotFoundException(pName + " for kind " + kind);
        Map<String, Module> modules = moduleKinds.get(kind);
        if (modules.containsKey(pName)) {
            return modules.get(pName);
        }
        throw new ModuleNotFoundException(pName);
    }

    /**
	 * @see org.easygen.ui.modules.ModuleManager#getModuleNatures()
	 */
    public String[] getModuleNatures(String kind) {
        Map<String, Module> modules = internalGetModules(kind);
        return (String[]) modules.keySet().toArray(new String[modules.size()]);
    }

    /**
	 * @see org.easygen.ui.modules.ModuleManager#getModules()
	 */
    public Module[] getModules(String kind) {
        Map<String, Module> modules = internalGetModules(kind);
        return (Module[]) modules.values().toArray(new Module[modules.size()]);
    }

    /**
	 * @see org.easygen.ui.modules.ModuleManager#getModules()
	 */
    protected Map<String, Module> internalGetModules(String kind) {
        if (kind == null || moduleKinds.containsKey(kind) == false) {
            return Collections.emptyMap();
        }
        Map<String, Module> modules = moduleKinds.get(kind);
        return modules;
    }

    /**
	 * Add the various modules from any kind 
	 */
    protected void initModules() {
        List<Module> modules = findAllModules();
        for (Module module : modules) {
            addModule(module);
        }
    }

    /**
	 * Looks for all modules contained in classpath by looking for the file "META-INF/services/org.easygen.ui.modules.Module"
	 */
    protected List<Module> findAllModules() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources;
        try {
            resources = loader.getResources("META-INF/services/" + Module.class.getName());
        } catch (IOException e) {
            logger.error("Can't look for modules", e);
            return new LinkedList<Module>();
        }
        Set<String> moduleClassNames = new HashSet<String>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                moduleClassNames.addAll(moduleNamesFromReader(url));
            } catch (IOException e) {
                logger.warn("Can't load module list from: " + url, e);
            }
        }
        List<Module> foundModules = new LinkedList<Module>();
        for (String moduleClassName : moduleClassNames) {
            try {
                Module newModule = (Module) loader.loadClass(moduleClassName).newInstance();
                foundModules.add(newModule);
            } catch (Exception e) {
                logger.debug("Can't load module class: " + moduleClassName, e);
            }
        }
        return foundModules;
    }

    protected Set<String> moduleNamesFromReader(URL url) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        Set<String> names = new HashSet<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m = nonCommentPattern.matcher(line);
            if (m.find()) {
                names.add(m.group().trim());
            }
        }
        return names;
    }
}
