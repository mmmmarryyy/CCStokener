package net.kodeninja.jem.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.kodeninja.io.JARFilenameFilter;
import net.kodeninja.jem.server.console.ConsoleInterface;
import net.kodeninja.jem.server.console.LocalConsole;
import net.kodeninja.jem.server.content.CustomMediaCollection;
import net.kodeninja.jem.server.content.transcoding.Transcoder;
import net.kodeninja.jem.server.storage.MediaItem;
import net.kodeninja.jem.server.storage.MemoryStorageModule;
import net.kodeninja.jem.server.storage.Metadata;
import net.kodeninja.jem.server.storage.MetadataType;
import net.kodeninja.jem.server.storage.StorageModule;
import net.kodeninja.jem.server.userinterface.Command;
import net.kodeninja.jem.server.userinterface.Group;
import net.kodeninja.jem.server.userinterface.Section;
import net.kodeninja.scheduling.FIFOScheduler;
import net.kodeninja.scheduling.Scheduler;
import net.kodeninja.util.KNModule;
import net.kodeninja.util.KNModuleInitException;
import net.kodeninja.util.KNRunnableModule;
import net.kodeninja.util.KNServiceModule;
import net.kodeninja.util.KNXMLModule;
import net.kodeninja.util.MimeType;
import net.kodeninja.util.logging.FileLogger;
import net.kodeninja.util.logging.LoggerCollection;
import net.kodeninja.util.logging.LoggerHook;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public final class JemServer extends LoggerCollection {

    private class StatusCommand implements Command {

        public void activate(LoggerHook output, String[] args) {
            Map<KNRunnableModule, Boolean> modules = JemServer.command().getModuleStatus();
            output.addLog("Status:");
            for (KNRunnableModule mod : modules.keySet()) output.addLog("  " + mod.getName() + " [" + (modules.get(mod) ? "Running" : "Stopped") + "]");
        }

        public String getDescription() {
            return "Returns a list of the running modules.";
        }

        public String getTitle() {
            return "Status";
        }
    }

    public final class CommandClass {

        JemServer owner;

        private CommandClass(JemServer Owner) {
            owner = Owner;
        }

        public void shutdown() {
            owner.setStatus(Statuses.Exiting);
            owner.sched.stop();
        }

        public void exception() {
            owner.setStatus(Statuses.Exiting);
            owner.errorFlag = true;
        }

        public Map<KNRunnableModule, Boolean> getModuleStatus() {
            Map<KNRunnableModule, Boolean> retVal = new HashMap<KNRunnableModule, Boolean>();
            Iterator<KNRunnableModule> it = owner.runnables.iterator();
            while (it.hasNext()) {
                KNRunnableModule tmpJob = it.next();
                retVal.put(tmpJob, new Boolean(tmpJob.isStarted()));
            }
            return retVal;
        }
    }

    public static enum Statuses {

        Starting, Running, Exiting
    }

    private static final int HELPER_THREAD_COUNT = 4;

    private Map<String, KNModule> loadedModules = Collections.synchronizedMap(new HashMap<String, KNModule>());

    private Set<InterfaceHook> interfaces = Collections.synchronizedSet(new HashSet<InterfaceHook>());

    private Set<KNRunnableModule> runnables = Collections.synchronizedSet(new HashSet<KNRunnableModule>());

    private Set<KNServiceModule> services = Collections.synchronizedSet(new LinkedHashSet<KNServiceModule>());

    private Set<Transcoder> transcoders = Collections.synchronizedSet(new HashSet<Transcoder>());

    private Set<Group> uiGroups = Collections.synchronizedSet(new LinkedHashSet<Group>());

    protected static JemServer instance;

    protected Scheduler sched = new FIFOScheduler(HELPER_THREAD_COUNT);

    protected StorageModule storage;

    protected CommandClass commands = new CommandClass(this);

    protected URLClassLoader classLoader = null;

    private Statuses Status = Statuses.Starting;

    private boolean errorFlag = false;

    private JemServer() {
        instance = this;
        File[] pluginsHome = (new File(System.getProperty("user.home") + "/.jems/plugins/")).listFiles(new JARFilenameFilter());
        File[] pluginsWorking = (new File(System.getProperty("user.dir") + "/plugins/")).listFiles(new JARFilenameFilter());
        Vector<URL> urlVector = new Vector<URL>();
        URL[] pluginsURLs;
        if (pluginsWorking != null) {
            for (File pluginPath : pluginsWorking) {
                try {
                    urlVector.add(pluginPath.toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        if (pluginsHome != null) {
            for (File pluginPath : pluginsHome) {
                try {
                    urlVector.add(pluginPath.toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        pluginsURLs = new URL[urlVector.size()];
        urlVector.toArray(pluginsURLs);
        classLoader = new URLClassLoader(pluginsURLs, ClassLoader.getSystemClassLoader());
        storage = new MemoryStorageModule();
    }

    public static JemServer getInstance() {
        if (instance == null) new JemServer();
        return instance;
    }

    public static StorageModule getMediaStorage() {
        return getInstance().storage;
    }

    public static String getMediaName(MediaItem mi) {
        String result = "";
        String artist = null;
        String title = null;
        for (Metadata md : mi.getMetadataList()) {
            if ((title == null) && (md.getType().equals(MetadataType.Title))) title = md.getValue(); else if ((artist == null) && (md.getType().equals(MetadataType.Artist))) artist = md.getValue();
            if ((artist != null) && (title != null)) break;
        }
        if (title != null) {
            result = title;
            if (artist != null) result = artist + " - " + title;
        }
        if (result.equals("")) {
            result = mi.getURI().toString();
            result = result.substring(result.lastIndexOf("/"));
        }
        return result;
    }

    public static CommandClass command() {
        return instance.commands;
    }

    public static URL getResource(String name) {
        File res = new File("override" + File.separatorChar + name);
        if (res.exists()) {
            try {
                return res.toURI().toURL();
            } catch (MalformedURLException e) {
            }
        }
        return getInstance().classLoader.getResource(name);
    }

    public static InputStream getResourceAsStream(String name) throws FileNotFoundException {
        URL url = getResource(name);
        if (url == null) throw new FileNotFoundException(name);
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new FileNotFoundException(name + " (Error occured while opening file)");
        }
    }

    private boolean start() {
        if (getStatus().equals(Statuses.Exiting)) return false;
        sched.start(true);
        return true;
    }

    public static Scheduler getScheduler() {
        return instance.sched;
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    public synchronized Statuses getStatus() {
        if (errorFlag) return Statuses.Exiting; else return Status;
    }

    private synchronized void setStatus(Statuses NewStatus) {
        if (!errorFlag) Status = NewStatus;
    }

    public KNModule getModule(String Name) {
        return loadedModules.get(Name);
    }

    public void addInterface(String name, InterfaceHook Interface) {
        interfaces.add(Interface);
        runnables.add(Interface);
        Interface.start();
        loadedModules.put(name, Interface);
    }

    public synchronized void addUIGroup(Group g) {
        uiGroups.add(g);
    }

    public synchronized void removeUIGroup(Group g) {
        uiGroups.remove(g);
    }

    public synchronized Set<Group> getUIGroups() {
        Set<Group> result = new LinkedHashSet<Group>();
        result.addAll(uiGroups);
        return result;
    }

    public InputStream requestTranscode(MimeType from, MimeType to, InputStream src) throws IOException {
        InputStream retVal = null;
        for (Transcoder t : transcoders) if ((retVal = t.transcode(from, to, src)) != null) break;
        return retVal;
    }

    public boolean hasError() {
        return errorFlag;
    }

    protected void parseConfiguration(Node root) throws JemMalformedConfigurationException {
        for (Node sectionNode = root.getFirstChild(); sectionNode != null; sectionNode = sectionNode.getNextSibling()) {
            if (sectionNode.getNodeType() != Node.ELEMENT_NODE) continue;
            String nodeName = sectionNode.getNodeName();
            if (nodeName.equals("modules")) for (Node moduleNode = sectionNode.getFirstChild(); moduleNode != null; moduleNode = moduleNode.getNextSibling()) {
                if (moduleNode.getNodeType() != Node.ELEMENT_NODE) continue;
                if (moduleNode.getNodeName().equals("module")) try {
                    initModule(moduleNode);
                } catch (JemMalformedConfigurationException e) {
                    System.err.println("Error while reading config: " + e.toString());
                } else throw new JemMalformedConfigurationException("Invalid config file. Malformed module entry: " + moduleNode.getNodeName());
            } else if (nodeName.equals("services")) for (Node serviceNode = sectionNode.getFirstChild(); serviceNode != null; serviceNode = serviceNode.getNextSibling()) {
                if (serviceNode.getNodeType() != Node.ELEMENT_NODE) continue;
                if (serviceNode.getNodeName().equals("service")) try {
                    initService(serviceNode);
                } catch (JemMalformedConfigurationException e) {
                    System.err.println("Error while reading config: " + e.toString());
                } else throw new JemMalformedConfigurationException("Invalid config file. Malformed service entry: " + serviceNode.getNodeName());
            } else if (nodeName.equals("loggers")) for (Node loggerNode = sectionNode.getFirstChild(); loggerNode != null; loggerNode = loggerNode.getNextSibling()) {
                if (loggerNode.getNodeType() != Node.ELEMENT_NODE) continue;
                if (loggerNode.getNodeName().equals("logger")) try {
                    initLogger(loggerNode);
                } catch (JemMalformedConfigurationException e) {
                    System.err.println("Error while reading config: " + e.toString());
                } else throw new JemMalformedConfigurationException("Invalid config file. Malformed logger entry: " + loggerNode.getNodeName());
            } else if (nodeName.equals("interfaces")) for (Node interfaceNode = sectionNode.getFirstChild(); interfaceNode != null; interfaceNode = interfaceNode.getNextSibling()) {
                if (interfaceNode.getNodeType() != Node.ELEMENT_NODE) continue;
                if (interfaceNode.getNodeName().equals("interface")) try {
                    initInterface(interfaceNode);
                } catch (JemMalformedConfigurationException e) {
                    System.err.println("Error while reading config: " + e.toString());
                } else throw new JemMalformedConfigurationException("Invalid config file. Malformed interface entry: " + interfaceNode.getNodeName());
            } else if (nodeName.equals("transcoders")) for (Node transcoderNode = sectionNode.getFirstChild(); transcoderNode != null; transcoderNode = transcoderNode.getNextSibling()) {
                if (transcoderNode.getNodeType() != Node.ELEMENT_NODE) continue;
                if (transcoderNode.getNodeName().equals("transcoder")) try {
                    initTranscoder(transcoderNode);
                } catch (JemMalformedConfigurationException e) {
                    System.err.println("Error while reading config: " + e.toString());
                } else throw new JemMalformedConfigurationException("Invalid config file. Malformed transcoder entry: " + transcoderNode.getNodeName());
            } else if (nodeName.equals("media")) for (Node moduleNode = sectionNode.getFirstChild(); moduleNode != null; moduleNode = moduleNode.getNextSibling()) {
                if (moduleNode.getNodeType() != Node.ELEMENT_NODE) continue;
                if (moduleNode.getNodeName().equals("collection")) try {
                    initCollection(moduleNode);
                } catch (JemMalformedConfigurationException e) {
                    System.err.println("Error while reading config: " + e.toString());
                } else throw new JemMalformedConfigurationException("Invalid config file. Malformed media entry: " + moduleNode.getNodeName());
            } else System.out.println("Invalid config file. Unreconized key: " + nodeName);
        }
    }

    protected void initModule(Node module) throws JemMalformedConfigurationException {
        Node nameAttr = module.getAttributes().getNamedItem("name");
        if ((nameAttr == null) || (nameAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("Unnamed module found! All modules must have names.");
        String modName = nameAttr.getNodeValue();
        Node classAttr = module.getAttributes().getNamedItem("class");
        if ((classAttr == null) || (classAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("No class is associated with module: " + modName);
        String className = classAttr.getNodeValue();
        Object o;
        KNXMLModule mod;
        try {
            o = classLoader.loadClass(className).newInstance();
        } catch (ClassNotFoundException e) {
            throw new JemMalformedConfigurationException("Unable to location module: " + modName);
        } catch (IllegalAccessException e) {
            throw new JemMalformedConfigurationException("Permission denied attempting to load module: " + modName);
        } catch (InstantiationException e) {
            throw new JemMalformedConfigurationException("An error occured while attempting to load the module: " + modName);
        }
        if (!(o instanceof KNXMLModule)) throw new JemMalformedConfigurationException("'" + modName + "' is not a valid module."); else mod = (KNXMLModule) o;
        if (mod instanceof KNServiceModule) {
            KNServiceModule s = (KNServiceModule) o;
            services.add(s);
        }
        if (loadedModules.containsKey(modName)) throw new JemMalformedConfigurationException("A module with the name '" + modName + "' allready exists.");
        try {
            mod.xmlInit(module);
        } catch (KNModuleInitException e) {
            throw new JemMalformedConfigurationException(e.getMessage());
        }
        loadedModules.put(modName, mod);
        addLog("Loaded module: " + mod.getName() + " - " + mod.getVersionMajor() + "." + mod.getVersionMinor() + "." + mod.getVersionRevision());
    }

    protected void initService(Node service) throws JemMalformedConfigurationException {
        Node modAttr = service.getAttributes().getNamedItem("module");
        if ((modAttr == null) || (modAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("Service found without module reference found! All services must reference a module name.");
        String modName = modAttr.getNodeValue();
        KNModule tmpModule = loadedModules.get(modName);
        if (tmpModule == null) throw new JemMalformedConfigurationException("Service references unloaded module: " + modName);
        if (tmpModule instanceof KNRunnableModule) ((KNRunnableModule) tmpModule).start(); else throw new JemMalformedConfigurationException("'" + modName + "' is not a valid service.");
        runnables.add(((KNRunnableModule) tmpModule));
    }

    protected void initLogger(Node logger) throws JemMalformedConfigurationException {
        Node modAttr = logger.getAttributes().getNamedItem("module");
        if ((modAttr == null) || (modAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("Logger found without module reference found! All loggers must reference a module name.");
        String modName = modAttr.getNodeValue();
        KNModule tmpModule = loadedModules.get(modName);
        if (tmpModule == null) throw new JemMalformedConfigurationException("Logger references unloaded module: " + modName);
        if (tmpModule instanceof LoggerHook) loggers.add((LoggerHook) tmpModule); else throw new JemMalformedConfigurationException("'" + modName + "' is not a valid logger.");
    }

    protected void initInterface(Node userInterface) throws JemMalformedConfigurationException {
        Node modAttr = userInterface.getAttributes().getNamedItem("module");
        if ((modAttr == null) || (modAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("Interface found without module reference found! All interfaces must reference a module name.");
        String modName = modAttr.getNodeValue();
        KNModule tmpModule = loadedModules.get(modName);
        if (tmpModule == null) throw new JemMalformedConfigurationException("Interface references unloaded module: " + modName);
        if (tmpModule instanceof InterfaceHook) {
            interfaces.add((InterfaceHook) tmpModule);
            runnables.add((InterfaceHook) tmpModule);
            ((InterfaceHook) tmpModule).start();
        } else throw new JemMalformedConfigurationException("'" + modName + "' is not a valid interface.");
    }

    protected void initTranscoder(Node transcoder) throws JemMalformedConfigurationException {
        Node modAttr = transcoder.getAttributes().getNamedItem("module");
        if ((modAttr == null) || (modAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("Logger found without module reference found! All loggers must reference a module name.");
        String modName = modAttr.getNodeValue();
        KNModule tmpModule = loadedModules.get(modName);
        if (tmpModule == null) throw new JemMalformedConfigurationException("Transcoder references unloaded module: " + modName);
        if (tmpModule instanceof Transcoder) transcoders.add((Transcoder) tmpModule); else throw new JemMalformedConfigurationException("'" + modName + "' is not a valid transcoder.");
    }

    protected void initCollection(Node collection) throws JemMalformedConfigurationException {
        Node nameAttr = collection.getAttributes().getNamedItem("name");
        if ((nameAttr == null) || (nameAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("Unnamed module found! All modules must have names.");
        String collectionName = nameAttr.getNodeValue();
        Node classAttr = collection.getAttributes().getNamedItem("class");
        if ((classAttr == null) || (classAttr.getNodeValue().trim().equals(""))) throw new JemMalformedConfigurationException("No class is associated with collection: " + collectionName);
        String className = classAttr.getNodeValue();
        Object o;
        CustomMediaCollection mediaCollection;
        try {
            o = classLoader.loadClass(className).newInstance();
        } catch (ClassNotFoundException e) {
            throw new JemMalformedConfigurationException("Unable to location collection module: " + collectionName);
        } catch (IllegalAccessException e) {
            throw new JemMalformedConfigurationException("Permission denied attempting to load collection module: " + collectionName);
        } catch (InstantiationException e) {
            throw new JemMalformedConfigurationException("Unable to create collection (Invalid construction signature): " + collectionName);
        }
        if (!(o instanceof CustomMediaCollection)) throw new JemMalformedConfigurationException("'" + collectionName + "' is not a valid collection module."); else mediaCollection = (CustomMediaCollection) o;
        try {
            mediaCollection.xmlInit(collection);
        } catch (KNModuleInitException e) {
            throw new JemMalformedConfigurationException(e.getMessage());
        }
        addLog("[" + mediaCollection.getName() + "] Loaded collection: " + mediaCollection.getCollectionName());
        JemServer.getMediaStorage().addCollection(mediaCollection);
    }

    private void initUI() {
        Group serverGroup = new Group("Main", "JEMS Configuration");
        Section serverSection = new Section("Main", "JEMS Configuration");
        serverGroup.addSection(serverSection);
        serverSection.addCommand(new StatusCommand());
        addUIGroup(serverGroup);
    }

    protected boolean initAll() {
        addLog("Server Started - " + getVersionMajor() + "." + getVersionMinor() + "." + getVersionRevision());
        addLog("----------------------");
        Document configFile;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File configPath = new File(System.getProperty("user.home") + "/.jems/JemsConfig.xml");
            if (configPath.exists() == false) configPath = new File(System.getProperty("user.dir") + "/JemsConfig.xml");
            configFile = builder.parse(configPath);
            if (configFile.getDocumentElement().getNodeName().equals("jems") != true) throw new Exception("Invalid config file. Root node should be 'jems', got '" + configFile.getDocumentElement().getNodeName() + "'.");
            parseConfiguration(configFile.getDocumentElement());
        } catch (Throwable e) {
            e.printStackTrace();
            commands.exception();
        }
        setStatus(Statuses.Running);
        if (!errorFlag) {
            for (KNServiceModule s : services) {
                try {
                    s.init();
                } catch (KNModuleInitException e) {
                    e.printStackTrace();
                }
            }
            initUI();
            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run() {
                    JemServer.getInstance().deinit();
                }
            });
        }
        return !errorFlag;
    }

    private void deinit() {
        commands.shutdown();
        for (KNRunnableModule service : runnables) service.stop();
        for (KNServiceModule s : services) {
            try {
                s.deinit();
            } catch (KNModuleInitException e) {
                e.printStackTrace();
            }
        }
        addLog("Server Stopped");
        if (errorFlag == true) addLog("Server exited due to error");
        addLog("Server exited normally\n");
    }

    public int getVersionMajor() {
        return 0;
    }

    public int getVersionMinor() {
        return 2;
    }

    public int getVersionRevision() {
        return 0;
    }

    public String getName() {
        return "Java Extendable Media Server";
    }

    public static void main(String[] args) {
        JemServer app = JemServer.getInstance();
        ConsoleInterface localConsole = new LocalConsole();
        app.addLogger(new FileLogger(new File("JemServer.log"), false));
        app.addLogger(localConsole);
        app.addInterface("Built-in Console", localConsole);
        app.initAll();
        if (app.start() == false) app.addLog("Error occured during startup.");
        if (app.hasError()) System.exit(-1); else System.exit(0);
    }
}
