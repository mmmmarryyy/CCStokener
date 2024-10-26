package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.core.runtime.internal.stats.StatsManager;
import org.eclipse.osgi.framework.adaptor.FilePath;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.OSGi;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.profile.Profile;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.runnable.ParameterizedRunnable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Special startup class for the Eclipse Platform. This class cannot be 
 * instantiated; all functionality is provided by static methods. 
 * <p>
 * The Eclipse Platform makes heavy use of Java class loaders for loading 
 * plug-ins. Even the Eclipse Runtime itself and the OSGi framework need
 * to be loaded by special class loaders. The upshot is that a 
 * client program (such as a Java main program, a servlet) cannot  
 * reference any part of Eclipse directly. Instead, a client must use this 
 * loader class to start the platform, invoking functionality defined 
 * in plug-ins, and shutting down the platform when done. 
 * </p>
 * <p>Note that the fields on this class are not API. </p>
 * @since 3.0
 */
public class EclipseStarter {

    private static FrameworkAdaptor adaptor;

    private static BundleContext context;

    private static ServiceTracker applicationTracker;

    private static boolean initialize = false;

    public static boolean debug = false;

    private static boolean running = false;

    private static final String CLEAN = "-clean";

    private static final String CONSOLE = "-console";

    private static final String CONSOLE_LOG = "-consoleLog";

    private static final String DEBUG = "-debug";

    private static final String INITIALIZE = "-initialize";

    private static final String DEV = "-dev";

    private static final String WS = "-ws";

    private static final String OS = "-os";

    private static final String ARCH = "-arch";

    private static final String NL = "-nl";

    private static final String CONFIGURATION = "-configuration";

    private static final String USER = "-user";

    private static final String NOEXIT = "-noExit";

    private static final String DATA = "-data";

    public static final String PROP_BUNDLES = "osgi.bundles";

    public static final String PROP_BUNDLES_STARTLEVEL = "osgi.bundles.defaultStartLevel";

    public static final String PROP_EXTENSIONS = "osgi.framework.extensions";

    public static final String PROP_INITIAL_STARTLEVEL = "osgi.startLevel";

    public static final String PROP_DEBUG = "osgi.debug";

    public static final String PROP_DEV = "osgi.dev";

    public static final String PROP_CLEAN = "osgi.clean";

    public static final String PROP_CONSOLE = "osgi.console";

    public static final String PROP_CONSOLE_CLASS = "osgi.consoleClass";

    public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration";

    public static final String PROP_OS = "osgi.os";

    public static final String PROP_WS = "osgi.ws";

    public static final String PROP_NL = "osgi.nl";

    public static final String PROP_ARCH = "osgi.arch";

    public static final String PROP_ADAPTOR = "osgi.adaptor";

    public static final String PROP_SYSPATH = "osgi.syspath";

    public static final String PROP_LOGFILE = "osgi.logfile";

    public static final String PROP_FRAMEWORK = "osgi.framework";

    public static final String PROP_INSTALL_AREA = "osgi.install.area";

    public static final String PROP_FRAMEWORK_SHAPE = "osgi.framework.shape";

    public static final String PROP_NOSHUTDOWN = "osgi.noShutdown";

    public static final String PROP_EXITCODE = "eclipse.exitcode";

    public static final String PROP_EXITDATA = "eclipse.exitdata";

    public static final String PROP_CONSOLE_LOG = "eclipse.consoleLog";

    private static final String PROP_VM = "eclipse.vm";

    private static final String PROP_VMARGS = "eclipse.vmargs";

    private static final String PROP_COMMANDS = "eclipse.commands";

    public static final String PROP_IGNOREAPP = "eclipse.ignoreApp";

    public static final String PROP_REFRESH_BUNDLES = "eclipse.refreshBundles";

    private static final String FILE_SCHEME = "file:";

    private static final String FILE_PROTOCOL = "file";

    private static final String REFERENCE_SCHEME = "reference:";

    private static final String REFERENCE_PROTOCOL = "reference";

    private static final String INITIAL_LOCATION = "initial@";

    /** string containing the classname of the adaptor to be used in this framework instance */
    protected static final String DEFAULT_ADAPTOR_CLASS = "org.eclipse.core.runtime.adaptor.EclipseAdaptor";

    private static final int DEFAULT_INITIAL_STARTLEVEL = 6;

    private static final String DEFAULT_BUNDLES_STARTLEVEL = "4";

    protected static final String DEFAULT_CONSOLE_CLASS = "org.eclipse.osgi.framework.internal.core.FrameworkConsole";

    private static final String CONSOLE_NAME = "OSGi Console";

    private static FrameworkLog log;

    /**
	 * This is the main to start osgi.
	 * It only works when the framework is being jared as a single jar
	 */
    public static void main(String[] args) throws Exception {
        URL url = EclipseStarter.class.getProtectionDomain().getCodeSource().getLocation();
        System.getProperties().put(PROP_FRAMEWORK, url.toExternalForm());
        String filePart = url.getFile();
        System.getProperties().put(PROP_INSTALL_AREA, filePart.substring(0, filePart.lastIndexOf('/')));
        System.getProperties().put(PROP_NOSHUTDOWN, "true");
        run(args, null);
    }

    /**
	 * Launches the platform and runs a single application. The application is either identified
	 * in the given arguments (e.g., -application &ltapp id&gt) or in the <code>eclipse.application</code> 
	 * System property.  This convenience method starts 
	 * up the platform, runs the indicated application, and then shuts down the 
	 * platform. The platform must not be running already. 
	 * 
	 * @param args the command line-style arguments used to configure the platform
	 * @param endSplashHandler the block of code to run to tear down the splash 
	 * 	screen or <code>null</code> if no tear down is required
	 * @return the result of running the application
	 * @throws Exception if anything goes wrong
	 */
    public static Object run(String[] args, Runnable endSplashHandler) throws Exception {
        if (Profile.PROFILE && Profile.STARTUP) Profile.logEnter("EclipseStarter.run()", null);
        if (running) throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ALREADY_RUNNING);
        boolean startupFailed = true;
        try {
            startup(args, endSplashHandler);
            startupFailed = false;
            if (Boolean.getBoolean(PROP_IGNOREAPP)) return null;
            return run(null);
        } catch (Throwable e) {
            if (endSplashHandler != null) endSplashHandler.run();
            FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, startupFailed ? EclipseAdaptorMsg.ECLIPSE_STARTUP_STARTUP_ERROR : EclipseAdaptorMsg.ECLIPSE_STARTUP_APP_ERROR, 1, e, null);
            if (log != null) {
                log.log(logEntry);
                logUnresolvedBundles(context.getBundles());
            } else e.printStackTrace();
        } finally {
            try {
                if (!Boolean.getBoolean(PROP_NOSHUTDOWN)) shutdown();
            } catch (Throwable e) {
                FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, EclipseAdaptorMsg.ECLIPSE_STARTUP_SHUTDOWN_ERROR, 1, e, null);
                if (log != null) log.log(logEntry); else e.printStackTrace();
            }
            if (Profile.PROFILE && Profile.STARTUP) Profile.logExit("EclipseStarter.run()");
            if (Profile.PROFILE) {
                String report = Profile.getProfileLog();
                if (report != null && report.length() > 0) System.out.println(report);
            }
        }
        if (Boolean.getBoolean("osgi.forcedRestart")) {
            System.getProperties().put(PROP_EXITCODE, "23");
            return null;
        }
        System.getProperties().put(PROP_EXITCODE, "13");
        System.getProperties().put(PROP_EXITDATA, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_CHECK_LOG, log.getFile().getPath()));
        return null;
    }

    /**
	 * Returns true if the platform is already running, false otherwise.
	 * @return whether or not the platform is already running
	 */
    public static boolean isRunning() {
        return running;
    }

    protected static FrameworkLog createFrameworkLog() {
        FrameworkLog frameworkLog;
        String logFileProp = System.getProperty(EclipseStarter.PROP_LOGFILE);
        if (logFileProp != null) {
            frameworkLog = new EclipseLog(new File(logFileProp));
        } else {
            Location location = LocationManager.getConfigurationLocation();
            File configAreaDirectory = null;
            if (location != null) configAreaDirectory = new File(location.getURL().getFile());
            if (configAreaDirectory != null) {
                String logFileName = Long.toString(System.currentTimeMillis()) + EclipseAdaptor.F_LOG;
                File logFile = new File(configAreaDirectory, logFileName);
                System.getProperties().put(EclipseStarter.PROP_LOGFILE, logFile.getAbsolutePath());
                frameworkLog = new EclipseLog(logFile);
            } else frameworkLog = new EclipseLog();
        }
        if ("true".equals(System.getProperty(EclipseStarter.PROP_CONSOLE_LOG))) frameworkLog.setConsoleLog(true);
        return frameworkLog;
    }

    /**
	 * Starts the platform and sets it up to run a single application. The application is either identified
	 * in the given arguments (e.g., -application &ltapp id&gt) or in the <code>eclipse.application</code>
	 * System property.  The platform must not be running already. 
	 * <p>
	 * The given runnable (if not <code>null</code>) is used to tear down the splash screen if required.
	 * </p>
	 * @param args the arguments passed to the application
	 * @throws Exception if anything goes wrong
	 */
    public static void startup(String[] args, Runnable endSplashHandler) throws Exception {
        if (Profile.PROFILE && Profile.STARTUP) Profile.logEnter("EclipseStarter.startup()", null);
        if (running) throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ALREADY_RUNNING);
        processCommandLine(args);
        LocationManager.initializeLocations();
        log = createFrameworkLog();
        initializeContextFinder();
        loadConfigurationInfo();
        finalizeProperties();
        if (Profile.PROFILE) Profile.initProps();
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "props inited");
        adaptor = createAdaptor();
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "adapter created");
        ((EclipseAdaptor) adaptor).setLog(log);
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "adapter log set");
        OSGi osgi = new OSGi(adaptor);
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "OSGi created");
        osgi.launch();
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "osgi launched");
        String console = System.getProperty(PROP_CONSOLE);
        if (console != null) {
            startConsole(osgi, new String[0], console);
            if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "console started");
        }
        context = osgi.getBundleContext();
        publishSplashScreen(endSplashHandler);
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "loading basic bundles");
        Bundle[] startBundles = loadBasicBundles();
        setStartLevel(getStartLevel());
        if ("true".equals(System.getProperty(PROP_REFRESH_BUNDLES))) refreshPackages(getCurrentBundles(false));
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.startup()", "StartLevel set");
        ensureBundlesActive(startBundles);
        if (debug || System.getProperty(PROP_DEV) != null) logUnresolvedBundles(context.getBundles());
        running = true;
        if (Profile.PROFILE && Profile.STARTUP) Profile.logExit("EclipseStarter.startup()");
    }

    private static void initializeContextFinder() {
        Thread current = Thread.currentThread();
        try {
            Method getContextClassLoader = Thread.class.getMethod("getContextClassLoader", null);
            Method setContextClassLoader = Thread.class.getMethod("setContextClassLoader", new Class[] { ClassLoader.class });
            Object[] params = new Object[] { new ContextFinder((ClassLoader) getContextClassLoader.invoke(current, null)) };
            setContextClassLoader.invoke(current, params);
            return;
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CANNOT_SET_CONTEXTFINDER, null), 0, null, null);
        log.log(entry);
    }

    private static int getStartLevel() {
        String level = System.getProperty(PROP_INITIAL_STARTLEVEL);
        if (level != null) try {
            return Integer.parseInt(level);
        } catch (NumberFormatException e) {
            if (debug) System.out.println("Start level = " + level + "  parsed. Using hardcoded default: 6");
        }
        return DEFAULT_INITIAL_STARTLEVEL;
    }

    /**
	 * Runs the applicaiton for which the platform was started. The platform 
	 * must be running. 
	 * <p>
	 * The given argument is passed to the application being run.  If it is <code>null</code>
	 * then the command line arguments used in starting the platform, and not consumed
	 * by the platform code, are passed to the application as a <code>String[]</code>.
	 * </p>
	 * @param argument the argument passed to the application. May be <code>null</code>
	 * @return the result of running the application
	 * @throws Exception if anything goes wrong
	 */
    public static Object run(Object argument) throws Exception {
        if (Profile.PROFILE && Profile.STARTUP) Profile.logEnter("EclipseStarter.run(Object)()", null);
        if (!running) throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_NOT_RUNNING);
        if (initialize) return new Integer(0);
        initializeApplicationTracker();
        if (Profile.PROFILE && Profile.STARTUP) Profile.logTime("EclipseStarter.run(Object)()", "applicaton tracker initialized");
        ParameterizedRunnable application = (ParameterizedRunnable) applicationTracker.getService();
        applicationTracker.close();
        if (application == null) throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_NO_APPLICATION);
        if (debug) {
            String timeString = System.getProperty("eclipse.startTime");
            long time = timeString == null ? 0L : Long.parseLong(timeString);
            System.out.println("Starting application: " + (System.currentTimeMillis() - time));
        }
        if (Profile.PROFILE && (Profile.STARTUP || Profile.BENCHMARK)) Profile.logTime("EclipseStarter.run(Object)()", "framework initialized! starting application...");
        try {
            return application.run(argument);
        } finally {
            if (Profile.PROFILE && Profile.STARTUP) Profile.logExit("EclipseStarter.run(Object)()");
        }
    }

    /**
	 * Shuts down the Platform. The state of the Platform is not automatically 
	 * saved before shutting down. 
	 * <p>
	 * On return, the Platform will no longer be running (but could be re-launched 
	 * with another call to startup). If relaunching, care must be taken to reinitialize
	 * any System properties which the platform uses (e.g., osgi.instance.area) as
	 * some policies in the platform do not allow resetting of such properties on 
	 * subsequent runs.
	 * </p><p>
	 * Any objects handed out by running Platform, 
	 * including Platform runnables obtained via getRunnable, will be 
	 * permanently invalid. The effects of attempting to invoke methods 
	 * on invalid objects is undefined. 
	 * </p>
	 * @throws Exception if anything goes wrong
	 */
    public static void shutdown() throws Exception {
        if (!running) return;
        stopSystemBundle();
    }

    private static void ensureBundlesActive(Bundle[] bundles) {
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getState() != Bundle.ACTIVE) {
                String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_ACTIVE, bundles[i]);
                throw new IllegalStateException(message);
            }
        }
    }

    private static void logUnresolvedBundles(Bundle[] bundles) {
        State state = adaptor.getState();
        FrameworkLog logService = adaptor.getFrameworkLog();
        StateHelper stateHelper = adaptor.getPlatformAdmin().getStateHelper();
        for (int i = 0; i < bundles.length; i++) if (bundles[i].getState() == Bundle.INSTALLED) {
            String generalMessage = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundles[i]);
            BundleDescription description = state.getBundle(bundles[i].getBundleId());
            if (description == null) continue;
            FrameworkLogEntry[] logChildren = null;
            VersionConstraint[] unsatisfied = stateHelper.getUnsatisfiedConstraints(description);
            if (unsatisfied.length > 0) {
                logChildren = new FrameworkLogEntry[unsatisfied.length];
                for (int j = 0; j < unsatisfied.length; j++) logChildren[j] = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, EclipseAdaptorMsg.getResolutionFailureMessage(unsatisfied[j]), 0, null, null);
            } else if (description.getSymbolicName() != null) {
                BundleDescription[] homonyms = state.getBundles(description.getSymbolicName());
                for (int j = 0; j < homonyms.length; j++) if (homonyms[j].isResolved()) {
                    logChildren = new FrameworkLogEntry[1];
                    logChildren[0] = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_OTHER_VERSION, homonyms[j].getLocation()), 0, null, null);
                }
            }
            logService.log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, generalMessage, 0, null, logChildren));
        }
    }

    private static void publishSplashScreen(final Runnable endSplashHandler) {
        Dictionary properties = new Hashtable();
        properties.put("name", "splashscreen");
        Runnable handler = new Runnable() {

            public void run() {
                StatsManager.doneBooting();
                endSplashHandler.run();
            }
        };
        context.registerService(Runnable.class.getName(), handler, properties);
    }

    private static URL searchForBundle(String name, String parent) throws MalformedURLException {
        URL url = null;
        File fileLocation = null;
        boolean reference = false;
        try {
            URL child = new URL(name);
            url = new URL(new File(parent).toURL(), name);
        } catch (MalformedURLException e) {
            File child = new File(name);
            fileLocation = child.isAbsolute() ? child : new File(parent, name);
            url = new URL(REFERENCE_PROTOCOL, null, fileLocation.toURL().toExternalForm());
            reference = true;
        }
        if (!reference) {
            URL baseURL = url;
            if (url.getProtocol().equals(REFERENCE_PROTOCOL)) {
                reference = true;
                String baseSpec = url.getFile();
                if (baseSpec.startsWith(FILE_SCHEME)) {
                    File child = new File(baseSpec.substring(5));
                    baseURL = child.isAbsolute() ? child.toURL() : new File(parent, child.getPath()).toURL();
                } else baseURL = new URL(baseSpec);
            }
            fileLocation = new File(baseURL.getFile());
            if (!fileLocation.isAbsolute()) fileLocation = new File(parent, fileLocation.toString());
        }
        if (reference) {
            String result = searchFor(fileLocation.getName(), new File(fileLocation.getParent()).getAbsolutePath());
            if (result != null) url = new URL(REFERENCE_PROTOCOL, null, FILE_SCHEME + result); else return null;
        }
        try {
            URLConnection result = url.openConnection();
            result.connect();
            return url;
        } catch (IOException e) {
            return null;
        }
    }

    private static Bundle[] loadBasicBundles() throws IOException {
        long startTime = System.currentTimeMillis();
        String osgiBundles = System.getProperty(PROP_BUNDLES);
        String osgiExtensions = System.getProperty(PROP_EXTENSIONS);
        if (osgiExtensions != null && osgiExtensions.length() > 0) {
            osgiBundles = osgiExtensions + ',' + osgiBundles;
            System.getProperties().put(PROP_BUNDLES, osgiBundles);
        }
        String[] installEntries = getArrayFromList(osgiBundles, ",");
        InitialBundle[] initialBundles = getInitialBundles(installEntries);
        Bundle[] curInitBundles = getCurrentBundles(true);
        List toRefresh = new ArrayList(curInitBundles.length);
        uninstallBundles(curInitBundles, initialBundles, toRefresh);
        ArrayList startBundles = new ArrayList(installEntries.length);
        installBundles(initialBundles, curInitBundles, startBundles, toRefresh);
        if (!toRefresh.isEmpty()) refreshPackages((Bundle[]) toRefresh.toArray(new Bundle[toRefresh.size()]));
        Bundle[] startInitBundles = (Bundle[]) startBundles.toArray(new Bundle[startBundles.size()]);
        startBundles(startInitBundles);
        if (debug) System.out.println("Time to load bundles: " + (System.currentTimeMillis() - startTime));
        return startInitBundles;
    }

    private static InitialBundle[] getInitialBundles(String[] installEntries) throws MalformedURLException {
        ArrayList result = new ArrayList(installEntries.length);
        int defaultStartLevel = Integer.parseInt(System.getProperty(PROP_BUNDLES_STARTLEVEL, DEFAULT_BUNDLES_STARTLEVEL));
        String syspath = getSysPath();
        for (int i = 0; i < installEntries.length; i++) {
            String name = installEntries[i];
            int level = defaultStartLevel;
            boolean start = false;
            int index = name.indexOf('@');
            if (index >= 0) {
                String[] attributes = getArrayFromList(name.substring(index + 1, name.length()), ":");
                name = name.substring(0, index);
                for (int j = 0; j < attributes.length; j++) {
                    String attribute = attributes[j];
                    if (attribute.equals("start")) start = true; else level = Integer.parseInt(attribute);
                }
            }
            URL location = searchForBundle(name, syspath);
            if (location == null) {
                FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_BUNDLE_NOT_FOUND, installEntries[i]), 0, null, null);
                log.log(entry);
                continue;
            }
            location = makeRelative(LocationManager.getInstallLocation().getURL(), location);
            String locationString = INITIAL_LOCATION + location.toExternalForm();
            result.add(new InitialBundle(locationString, location, level, start));
        }
        return (InitialBundle[]) result.toArray(new InitialBundle[result.size()]);
    }

    private static void refreshPackages(Bundle[] bundles) {
        ServiceReference packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = null;
        if (packageAdminRef != null) {
            packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
            if (packageAdmin == null) return;
        }
        final Semaphore semaphore = new Semaphore(0);
        FrameworkListener listener = new FrameworkListener() {

            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) semaphore.release();
            }
        };
        context.addFrameworkListener(listener);
        packageAdmin.refreshPackages(bundles);
        semaphore.acquire();
        context.removeFrameworkListener(listener);
        context.ungetService(packageAdminRef);
    }

    /**
	 *  Invokes the OSGi Console on another thread
	 *
	 * @param osgi The current OSGi instance for the console to attach to
	 * @param consoleArgs An String array containing commands from the command line
	 * for the console to execute
	 * @param consolePort the port on which to run the console.  Empty string implies the default port.
	 */
    private static void startConsole(OSGi osgi, String[] consoleArgs, String consolePort) {
        try {
            String consoleClassName = System.getProperty(PROP_CONSOLE_CLASS, DEFAULT_CONSOLE_CLASS);
            Class consoleClass = Class.forName(consoleClassName);
            Class[] parameterTypes;
            Object[] parameters;
            if (consolePort.length() == 0) {
                parameterTypes = new Class[] { OSGi.class, String[].class };
                parameters = new Object[] { osgi, consoleArgs };
            } else {
                parameterTypes = new Class[] { OSGi.class, int.class, String[].class };
                parameters = new Object[] { osgi, new Integer(consolePort), consoleArgs };
            }
            Constructor constructor = consoleClass.getConstructor(parameterTypes);
            Object console = constructor.newInstance(parameters);
            Thread t = new Thread(((Runnable) console), CONSOLE_NAME);
            t.start();
        } catch (NumberFormatException nfe) {
            System.err.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_INVALID_PORT, consolePort));
        } catch (Exception ex) {
            System.out.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_FIND, CONSOLE_NAME));
        }
    }

    /**
	 *  Creates and returns the adaptor
	 *
	 *  @return a FrameworkAdaptor object
	 */
    private static FrameworkAdaptor createAdaptor() throws Exception {
        String adaptorClassName = System.getProperty(PROP_ADAPTOR, DEFAULT_ADAPTOR_CLASS);
        Class adaptorClass = Class.forName(adaptorClassName);
        Class[] constructorArgs = new Class[] { String[].class };
        Constructor constructor = adaptorClass.getConstructor(constructorArgs);
        return (FrameworkAdaptor) constructor.newInstance(new Object[] { new String[0] });
    }

    private static String[] processCommandLine(String[] args) throws Exception {
        EclipseEnvironmentInfo.setAllArgs(args);
        if (args.length == 0) {
            EclipseEnvironmentInfo.setFrameworkArgs(args);
            EclipseEnvironmentInfo.setAllArgs(args);
            return args;
        }
        int[] configArgs = new int[args.length];
        configArgs[0] = -1;
        int configArgIndex = 0;
        for (int i = 0; i < args.length; i++) {
            boolean found = false;
            if (args[i].equalsIgnoreCase(DEBUG) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) {
                System.getProperties().put(PROP_DEBUG, "");
                debug = true;
                found = true;
            }
            if (args[i].equalsIgnoreCase(DEV) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) {
                System.getProperties().put(PROP_DEV, "");
                found = true;
            }
            if (args[i].equalsIgnoreCase(INITIALIZE)) {
                initialize = true;
                found = true;
            }
            if (args[i].equalsIgnoreCase(CLEAN)) {
                System.getProperties().put(PROP_CLEAN, "true");
                found = true;
            }
            if (args[i].equalsIgnoreCase(CONSOLE_LOG)) {
                System.getProperties().put(PROP_CONSOLE_LOG, "true");
                found = true;
            }
            if (args[i].equalsIgnoreCase(CONSOLE) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) {
                System.getProperties().put(PROP_CONSOLE, "");
                found = true;
            }
            if (args[i].equalsIgnoreCase(NOEXIT)) {
                System.getProperties().put(PROP_NOSHUTDOWN, "true");
                found = true;
            }
            if (found) {
                configArgs[configArgIndex++] = i;
                continue;
            }
            if (i == args.length - 1 || args[i + 1].startsWith("-")) {
                continue;
            }
            String arg = args[++i];
            if (args[i - 1].equalsIgnoreCase(CONSOLE)) {
                System.getProperties().put(PROP_CONSOLE, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
                System.getProperties().put(LocationManager.PROP_CONFIG_AREA, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(DATA)) {
                System.getProperties().put(LocationManager.PROP_INSTANCE_AREA, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(USER)) {
                System.getProperties().put(LocationManager.PROP_USER_AREA, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(DEV)) {
                System.getProperties().put(PROP_DEV, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(DEBUG)) {
                System.getProperties().put(PROP_DEBUG, arg);
                debug = true;
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(WS)) {
                System.getProperties().put(PROP_WS, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(OS)) {
                System.getProperties().put(PROP_OS, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(ARCH)) {
                System.getProperties().put(PROP_ARCH, arg);
                found = true;
            }
            if (args[i - 1].equalsIgnoreCase(NL)) {
                System.getProperties().put(PROP_NL, arg);
                found = true;
            }
            if (found) {
                configArgs[configArgIndex++] = i - 1;
                configArgs[configArgIndex++] = i;
            }
        }
        if (configArgIndex == 0) {
            EclipseEnvironmentInfo.setFrameworkArgs(new String[0]);
            EclipseEnvironmentInfo.setAppArgs(args);
            return args;
        }
        String[] appArgs = new String[args.length - configArgIndex];
        String[] frameworkArgs = new String[configArgIndex];
        configArgIndex = 0;
        int j = 0;
        int k = 0;
        for (int i = 0; i < args.length; i++) {
            if (i == configArgs[configArgIndex]) {
                frameworkArgs[k++] = args[i];
                configArgIndex++;
            } else appArgs[j++] = args[i];
        }
        EclipseEnvironmentInfo.setFrameworkArgs(frameworkArgs);
        EclipseEnvironmentInfo.setAppArgs(appArgs);
        return appArgs;
    }

    /**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
    private static String[] getArrayFromList(String prop, String separator) {
        if (prop == null || prop.trim().equals("")) return new String[0];
        Vector list = new Vector();
        StringTokenizer tokens = new StringTokenizer(prop, separator);
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            if (!token.equals("")) list.addElement(token);
        }
        return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
    }

    protected static String getSysPath() {
        String result = System.getProperty(PROP_SYSPATH);
        if (result != null) return result;
        result = getSysPathFromURL(System.getProperty(PROP_FRAMEWORK));
        if (result == null) result = getSysPathFromCodeSource();
        if (result == null) throw new IllegalStateException("Can not find the system path.");
        if (Character.isUpperCase(result.charAt(0))) {
            char[] chars = result.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            result = new String(chars);
        }
        System.getProperties().put(PROP_SYSPATH, result);
        return result;
    }

    private static String getSysPathFromURL(String urlSpec) {
        if (urlSpec == null) return null;
        URL url = null;
        try {
            url = new URL(urlSpec);
        } catch (MalformedURLException e) {
            return null;
        }
        File fwkFile = new File(url.getFile());
        fwkFile = new File(fwkFile.getAbsolutePath());
        fwkFile = new File(fwkFile.getParent());
        return fwkFile.getAbsolutePath();
    }

    private static String getSysPathFromCodeSource() {
        ProtectionDomain pd = EclipseStarter.class.getProtectionDomain();
        if (pd == null) return null;
        CodeSource cs = pd.getCodeSource();
        if (cs == null) return null;
        URL url = cs.getLocation();
        if (url == null) return null;
        String result = url.getFile();
        if (result.endsWith(".jar")) {
            result = result.substring(0, result.lastIndexOf('/'));
            if ("folder".equals(System.getProperty(PROP_FRAMEWORK_SHAPE))) result = result.substring(0, result.lastIndexOf('/'));
        } else {
            if (result.endsWith("/")) result = result.substring(0, result.length() - 1);
            result = result.substring(0, result.lastIndexOf('/'));
            result = result.substring(0, result.lastIndexOf('/'));
        }
        return result;
    }

    private static Bundle[] getCurrentBundles(boolean includeInitial) {
        Bundle[] installed = context.getBundles();
        ArrayList initial = new ArrayList();
        for (int i = 0; i < installed.length; i++) {
            Bundle bundle = installed[i];
            if (bundle.getLocation().startsWith(INITIAL_LOCATION)) {
                if (includeInitial) initial.add(bundle);
            } else if (!includeInitial && bundle.getBundleId() != 0) initial.add(bundle);
        }
        return (Bundle[]) initial.toArray(new Bundle[initial.size()]);
    }

    private static Bundle getBundleByLocation(String location, Bundle[] bundles) {
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            if (location.equalsIgnoreCase(bundle.getLocation())) return bundle;
        }
        return null;
    }

    private static void uninstallBundles(Bundle[] curInitBundles, InitialBundle[] newInitBundles, List toRefresh) {
        for (int i = 0; i < curInitBundles.length; i++) {
            boolean found = false;
            for (int j = 0; j < newInitBundles.length; j++) {
                if (curInitBundles[i].getLocation().equalsIgnoreCase(newInitBundles[j].locationString)) {
                    found = true;
                    break;
                }
            }
            if (!found) try {
                curInitBundles[i].uninstall();
                toRefresh.add(curInitBundles[i]);
            } catch (BundleException e) {
                FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_UNINSTALL, curInitBundles[i].getLocation()), 0, e, null);
                log.log(entry);
            }
        }
    }

    private static void installBundles(InitialBundle[] initialBundles, Bundle[] curInitBundles, ArrayList startBundles, List toRefresh) {
        ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
        StartLevel startService = null;
        if (reference != null) startService = (StartLevel) context.getService(reference);
        for (int i = 0; i < initialBundles.length; i++) {
            Bundle osgiBundle = getBundleByLocation(initialBundles[i].locationString, curInitBundles);
            try {
                if (osgiBundle == null) {
                    InputStream in = initialBundles[i].location.openStream();
                    osgiBundle = context.installBundle(initialBundles[i].locationString, in);
                    if (initialBundles[i].level >= 0 && startService != null) startService.setBundleStartLevel(osgiBundle, initialBundles[i].level);
                }
                if (initialBundles[i].start) startBundles.add(osgiBundle);
                if ((osgiBundle.getState() & Bundle.INSTALLED) != 0) toRefresh.add(osgiBundle);
            } catch (BundleException e) {
                FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_INSTALL, initialBundles[i].location), 0, e, null);
                log.log(entry);
            } catch (IOException e) {
                FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_INSTALL, initialBundles[i].location), 0, e, null);
                log.log(entry);
            }
        }
        context.ungetService(reference);
    }

    private static void startBundles(Bundle[] bundles) {
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            if (bundle.getState() == Bundle.INSTALLED) throw new IllegalStateException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundle.getLocation()));
            try {
                bundle.start();
            } catch (BundleException e) {
                FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_START, bundle.getLocation()), 0, e, null);
                log.log(entry);
            }
        }
    }

    private static void initializeApplicationTracker() {
        Filter filter = null;
        try {
            String appClass = ParameterizedRunnable.class.getName();
            filter = context.createFilter("(&(objectClass=" + appClass + ")(eclipse.application=*))");
        } catch (InvalidSyntaxException e) {
        }
        applicationTracker = new ServiceTracker(context, filter, null);
        applicationTracker.open();
    }

    private static void loadConfigurationInfo() {
        Location configArea = LocationManager.getConfigurationLocation();
        if (configArea == null) return;
        URL location = null;
        try {
            location = new URL(configArea.getURL().toExternalForm() + LocationManager.CONFIG_FILE);
        } catch (MalformedURLException e) {
        }
        mergeProperties(System.getProperties(), loadProperties(location));
    }

    private static Properties loadProperties(URL location) {
        Properties result = new Properties();
        if (location == null) return result;
        try {
            InputStream in = location.openStream();
            try {
                result.load(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
        }
        return result;
    }

    /**
	 * Returns a URL which is equivalent to the given URL relative to the
	 * specified base URL. Works only for file: URLs
	 * @throws MalformedURLException 
	 */
    private static URL makeRelative(URL base, URL location) throws MalformedURLException {
        if (base == null) return location;
        if (!"file".equals(base.getProtocol())) return location;
        boolean reference = location.getProtocol().equals(REFERENCE_PROTOCOL);
        URL nonReferenceLocation = location;
        if (reference) nonReferenceLocation = new URL(location.getPath());
        if (!base.getProtocol().equals(nonReferenceLocation.getProtocol())) return location;
        File locationPath = new File(nonReferenceLocation.getPath());
        if (!locationPath.isAbsolute()) return location;
        File relativePath = makeRelative(new File(base.getPath()), locationPath);
        String urlPath = relativePath.getPath();
        if (File.separatorChar != '/') urlPath = urlPath.replace(File.separatorChar, '/');
        if (nonReferenceLocation.getPath().endsWith("/")) urlPath += '/';
        URL relativeURL = new URL(base.getProtocol(), base.getHost(), base.getPort(), urlPath);
        if (reference) relativeURL = new URL(REFERENCE_SCHEME + relativeURL.toExternalForm());
        return relativeURL;
    }

    private static File makeRelative(File base, File location) {
        if (!location.isAbsolute()) return location;
        File relative = new File(new FilePath(base).makeRelative(new FilePath(location)));
        return relative;
    }

    private static void mergeProperties(Properties destination, Properties source) {
        for (Enumeration e = source.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String value = source.getProperty(key);
            if (destination.getProperty(key) == null) destination.put(key, value);
        }
    }

    private static void stopSystemBundle() throws BundleException {
        if (context == null || !running) return;
        Bundle systemBundle = context.getBundle(0);
        if (systemBundle.getState() == Bundle.ACTIVE) {
            final Semaphore semaphore = new Semaphore(0);
            FrameworkListener listener = new FrameworkListener() {

                public void frameworkEvent(FrameworkEvent event) {
                    if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) semaphore.release();
                }
            };
            context.addFrameworkListener(listener);
            systemBundle.stop();
            semaphore.acquire();
            context.removeFrameworkListener(listener);
        }
        context = null;
        applicationTracker = null;
        running = false;
    }

    private static void setStartLevel(final int value) {
        ServiceTracker tracker = new ServiceTracker(context, StartLevel.class.getName(), null);
        tracker.open();
        final StartLevel startLevel = (StartLevel) tracker.getService();
        final Semaphore semaphore = new Semaphore(0);
        FrameworkListener listener = new FrameworkListener() {

            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED && startLevel.getStartLevel() == value) semaphore.release();
            }
        };
        context.addFrameworkListener(listener);
        startLevel.setStartLevel(value);
        semaphore.acquire();
        context.removeFrameworkListener(listener);
        tracker.close();
    }

    /**
	 * Searches for the given target directory immediately under
	 * the given start location.  If one is found then this location is returned; 
	 * otherwise an exception is thrown.
	 * 
	 * @return the location where target directory was found
	 * @param start the location to begin searching
	 */
    private static String searchFor(final String target, String start) {
        String[] candidates = new File(start).list();
        if (candidates == null) return null;
        String result = null;
        Object maxVersion = null;
        for (int i = 0; i < candidates.length; i++) {
            File candidate = new File(start, candidates[i]);
            if (!candidate.getName().equals(target) && !candidate.getName().startsWith(target + "_")) continue;
            String name = candidate.getName();
            String version = "";
            int index = name.indexOf('_');
            if (index != -1) version = name.substring(index + 1);
            Object currentVersion = getVersionElements(version);
            if (maxVersion == null) {
                result = candidate.getAbsolutePath();
                maxVersion = currentVersion;
            } else {
                if (compareVersion((Object[]) maxVersion, (Object[]) currentVersion) < 0) {
                    result = candidate.getAbsolutePath();
                    maxVersion = currentVersion;
                }
            }
        }
        if (result == null) return null;
        return result.replace(File.separatorChar, '/') + "/";
    }

    /**
	 * Do a quick parse of version identifier so its elements can be correctly compared.
	 * If we are unable to parse the full version, remaining elements are initialized
	 * with suitable defaults.
	 * @return an array of size 4; first three elements are of type Integer (representing
	 * major, minor and service) and the fourth element is of type String (representing
	 * qualifier). Note, that returning anything else will cause exceptions in the caller.
	 */
    private static Object[] getVersionElements(String version) {
        Object[] result = { new Integer(0), new Integer(0), new Integer(0), "" };
        StringTokenizer t = new StringTokenizer(version, ".");
        String token;
        int i = 0;
        while (t.hasMoreTokens() && i < 4) {
            token = t.nextToken();
            if (i < 3) {
                try {
                    result[i++] = new Integer(token);
                } catch (Exception e) {
                    break;
                }
            } else {
                result[i++] = token;
            }
        }
        return result;
    }

    /**
	 * Compares version strings. 
	 * @return result of comparison, as integer;
	 * <code><0</code> if left < right;
	 * <code>0</code> if left == right;
	 * <code>>0</code> if left > right;
	 */
    private static int compareVersion(Object[] left, Object[] right) {
        int result = ((Integer) left[0]).compareTo((Integer) right[0]);
        if (result != 0) return result;
        result = ((Integer) left[1]).compareTo((Integer) right[1]);
        if (result != 0) return result;
        result = ((Integer) left[2]).compareTo((Integer) right[2]);
        if (result != 0) return result;
        return ((String) left[3]).compareTo((String) right[3]);
    }

    private static String buildCommandLine(String arg, String value) {
        StringBuffer result = new StringBuffer(300);
        String entry = System.getProperty(PROP_VM);
        if (entry == null) return null;
        result.append(entry);
        result.append('\n');
        entry = System.getProperty(PROP_VMARGS);
        if (entry != null) result.append(entry);
        entry = System.getProperty(PROP_COMMANDS);
        if (entry != null) result.append(entry);
        String commandLine = result.toString();
        int i = commandLine.indexOf(arg + "\n");
        if (i == 0) commandLine += arg + "\n" + value + "\n"; else {
            i += arg.length() + 1;
            String left = commandLine.substring(0, i);
            int j = commandLine.indexOf('\n', i);
            String right = commandLine.substring(j);
            commandLine = left + value + right;
        }
        return commandLine;
    }

    private static void finalizeProperties() {
        if (System.getProperty(PROP_DEV) != null && System.getProperty(PROP_CHECK_CONFIG) == null) System.getProperties().put(PROP_CHECK_CONFIG, "true");
    }

    private static class InitialBundle {

        public final String locationString;

        public final URL location;

        public final int level;

        public final boolean start;

        InitialBundle(String locationString, URL location, int level, boolean start) {
            this.locationString = locationString;
            this.location = location;
            this.level = level;
            this.start = start;
        }
    }
}
