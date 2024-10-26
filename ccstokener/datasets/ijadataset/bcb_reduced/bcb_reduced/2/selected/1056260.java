package de.fhg.igd.semoa.webservice;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import de.fhg.igd.logging.LogLevel;
import de.fhg.igd.logging.Logger;
import de.fhg.igd.logging.LoggerFactory;
import de.fhg.igd.semoa.server.Environment;
import de.fhg.igd.semoa.server.EnvironmentCallback;
import de.fhg.igd.semoa.util.Lookup;
import de.fhg.igd.semoa.webservice.uddi.UddiService;
import de.fhg.igd.util.ArgsParser;
import de.fhg.igd.util.ArgsParserException;
import de.fhg.igd.util.CanonicalPath;
import de.fhg.igd.util.NoSuchObjectException;
import de.fhg.igd.util.ObjectExistsException;
import de.fhg.igd.util.VariableSubstitution;
import de.fhg.igd.util.WhatIs;

/**
 * The purpose of this tool is to automatically startup and shutdown
 * webservices according to user-defined checkpoints within the
 * {@link Environment}. The checkpoints are to be defined by appropriate
 * entries in a configuration file being handed over via the command line
 * interface on <tt>startup</tt>. Detailed information about the configuration
 * syntax is provided via the sample configuration file.
 * <p>More information about command line parameters is available via
 * <p><nobreak><tt>java WebserviceManager -help</tt></nobreak>
 *
 * @author Matthias Pressfreund
 * @version "$Id: WebserviceManager.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class WebserviceManager implements EnvironmentCallback {

    /**
     * The <code>Logger</code> instance for this class
     */
    static Logger log_ = LoggerFactory.getLogger("webservice");

    /**
     * The local synchronization object
     */
    private final Object lock_ = new Object();

    /**
     * The <tt>-start</tt> command line parameter
     */
    protected static final String CMDLINE_START_ = "start";

    /**
     * The <tt>-q</tt> command line parameter
     */
    protected static final String CMDLINE_QUIET_ = "q";

    /**
     * The <tt>-stop</tt> command line parameter
     */
    protected static final String CMDLINE_STOP_ = "stop";

    /**
     * The <tt>-status</tt> command line parameter
     */
    protected static final String CMDLINE_STATUS_ = "status";

    /**
     * The <tt>-help</tt> command line parameter
     */
    protected static final String CMDLINE_HELP_ = "help";

    /**
     * Status ID during normal operation
     */
    public static final int STATUS_RUNNING = 1;

    /**
     * Status ID when halted
     */
    public static final int STATUS_HALTED = 0;

    /**
     * Status ID when inoperative due to missing <code>WhatIs</code> entries
     * or <code>Environment</code> lookup failures
     */
    public static final int STATUS_INOPERATIVE = -1;

    /**
     * The {@link ArgsParser} command line parameter descriptor
     */
    protected static final String DESCR_ = CMDLINE_START_ + ":s," + CMDLINE_QUIET_ + ":!," + CMDLINE_STOP_ + ":!," + CMDLINE_STATUS_ + ":!," + CMDLINE_HELP_ + ":!";

    /**
     * The {@link WhatIs} value of the {@link WebserviceService}
     */
    protected static final String WHATIS_WSS_ = WhatIs.stringValue(WebserviceService.WHATIS);

    /**
     * The {@link WhatIs} value of the {@link UddiService}
     */
    protected static final String WHATIS_UDDIS_ = WhatIs.stringValue(UddiService.WHATIS);

    /**
     * The set of {@link ConfigurationEntry} objects corresponding to
     * the currently loaded configuration file
     */
    protected Set config_;

    /**
     * The flag for omitted output
     */
    protected boolean quiet_;

    /**
     * The <code>WebserviceService</code> instance
     */
    protected WebserviceService wss_;

    /**
     * The <code>UddiService</code> instance
     */
    protected UddiService uddis_;

    /**
     * The singleton instance
     */
    protected static WebserviceManager instance_ = new WebserviceManager();

    /**
     * Hidden construction.
     */
    private WebserviceManager() {
        config_ = new HashSet();
        quiet_ = false;
        wss_ = null;
        uddis_ = null;
        log_.debug("Instance successfully created");
    }

    /**
     * Initialize the <code>WebserviceManager</code> by loading the
     * specified configuration file and
     * {@link Environment#registerCallback(EnvironmentCallback) register}
     * the callback.
     * <b>Notice</b>: This method will implicitly {@link #shutdown() shutdown}
     * the previously loaded configuration, if existent.
     *
     * @param cfgfile The configuration file
     * @param quiet Enable/Disable command line output
     * @throws IllegalArgumentException
     *   <ul>
     *   <li>if <code>cfgfile</code> is <code>null</code> or not readable
     *   <li>if <code>cfgfile</code> contains an unresolvable class name
     *   </ul>
     * @throws IllegalStateException
     *   if the {@link #status() status} is
     *   {@link #STATUS_INOPERATIVE inoperative}
     * @throws ObjectExistsException
     *   in case there is another {@link EnvironmentCallback} instance
     *   already registered
     */
    public void startup(String cfgfile, boolean quiet) throws IllegalArgumentException, IllegalStateException, ObjectExistsException {
        Map.Entry entry;
        Properties prp;
        String msg;
        Iterator i;
        if (cfgfile != null) {
            try {
                prp = VariableSubstitution.parseConfigFile(cfgfile, VariableSubstitution.SYSTEM_PROPERTIES | VariableSubstitution.WHATIS_VARIABLES);
            } catch (Exception e) {
                msg = "Cannot read configuration file '" + cfgfile + "': " + e.getMessage();
                log_.caught(LogLevel.ERROR, msg, e);
                throw new IllegalArgumentException(msg);
            }
        } else {
            log_.error(msg = "Missing configuration file");
            throw new IllegalArgumentException(msg);
        }
        synchronized (lock_) {
            shutdown();
            Environment.getEnvironment().registerCallback(this);
            log_.debug("Successfully registered callback");
            try {
                wss_ = (WebserviceService) Lookup.environmentWhatIs(WebserviceService.WHATIS, WebserviceService.class);
            } catch (Exception e) {
                wss_ = null;
            }
            try {
                uddis_ = (UddiService) Lookup.environmentWhatIs(UddiService.WHATIS, UddiService.class);
            } catch (Exception e) {
                uddis_ = null;
            }
            if (status() == STATUS_INOPERATIVE) {
                log_.error(msg = "Inoperative status");
                throw new IllegalStateException(msg + " (Details: 'WebserviceManager -status')");
            }
            for (i = prp.entrySet().iterator(); i.hasNext(); ) {
                entry = (Map.Entry) i.next();
                try {
                    config_.add(new ConfigurationEntry(entry.getKey().toString(), entry.getValue().toString()));
                } catch (Exception e) {
                    System.err.println("[WebserviceManager] Error: " + e.getMessage());
                }
            }
            quiet_ = quiet;
            log_.info("The WebserviceManager has been started successfully");
        }
    }

    /**
     * Remove all {@link ConfigurationEntry} objects and
     * {@link Environment#deregisterCallback(EnvironmentCallback) deregister}
     * the callback.
     */
    public void shutdown() {
        synchronized (lock_) {
            if (status() == STATUS_HALTED) {
                return;
            }
            try {
                Environment.getEnvironment().deregisterCallback(this);
                log_.debug("Successfully deregistered callback");
            } catch (NoSuchObjectException e) {
                log_.caught(LogLevel.ERROR, "Deregistering callback failed", e);
                System.err.println("[WebserviceManager] Error: " + e.getMessage());
            }
            config_.clear();
            log_.info("The WebserviceManager has been successfully halted");
        }
    }

    /**
     * Get the current status.
     *
     * @return Either {@link #STATUS_RUNNING} or {@link #STATUS_HALTED}
     *   or {@link #STATUS_INOPERATIVE}
     */
    public int status() {
        synchronized (lock_) {
            if (config_.isEmpty()) {
                return STATUS_HALTED;
            }
            if (wss_ == null || uddis_ == null) {
                return STATUS_INOPERATIVE;
            }
            return STATUS_RUNNING;
        }
    }

    /**
     * Create a verbose {@link #status() status} report.
     *
     * @return The report for the current status
     */
    protected String createReport() {
        StringBuffer report;
        Iterator i;
        report = new StringBuffer("The WebserviceManager is ");
        switch(status()) {
            case STATUS_RUNNING:
                report.append("up and running.").append("\n\nConfiguration:\n");
                for (i = config_.iterator(); i.hasNext(); ) {
                    report.append(i.next());
                }
                report.append("\nVerbose output is ").append(quiet_ ? "disabled." : "enabled.");
                break;
            case STATUS_HALTED:
                report.append("halted.");
                break;
            case STATUS_INOPERATIVE:
            default:
                report.append("inoperative.\n");
                if (WHATIS_WSS_ == null) {
                    report.append("\n\t'WhatIs:").append(WebserviceService.WHATIS).append("' is not defined.");
                } else if (wss_ == null) {
                    report.append("\n\t'Environment:").append(WHATIS_WSS_).append("' must contain a WebserviceService.");
                }
                if (WHATIS_UDDIS_ == null) {
                    report.append("\n\t'WhatIs:").append(UddiService.WHATIS).append("' is not defined.");
                } else if (uddis_ == null) {
                    report.append("\n\t'Environment:").append(WHATIS_UDDIS_).append("' must contain a UddiService.");
                }
        }
        return report.toString();
    }

    /**
     * In case there is a matching {@link ConfigurationEntry} for the given
     * <code>key</code>, an appropriate <code>Webservice</code>
     * {@link ClientProxyFactory#getClientProxy(Webservice.Description) client}
     * will be created.
     *
     * @return The <code>Webservice</code> client (which will be returned
     *   by {@link Environment#lookup(String)} instead of <code>null</code>),
     *   or <code>null</code> if no matching <code>ConfigurationEntry</code>
     *   could be found
     */
    public Object onLookupEmpty(String key) {
        Webservice.Description[] wsdescs;
        ConfigurationEntry entry;
        Object client;
        String wsname;
        Iterator i;
        Map ifconf;
        int j;
        if (key == null) {
            log_.warning("Key is null");
            return null;
        }
        synchronized (lock_) {
            if (status() != STATUS_RUNNING) {
                return null;
            }
            wsname = key.substring(1);
            for (i = findEntries(key).iterator(); i.hasNext(); ) {
                entry = (ConfigurationEntry) i.next();
                try {
                    wsdescs = uddis_.requestWebserviceDescriptions(wsname);
                    for (j = 0; j < wsdescs.length; j++) {
                        if (wsdescs[j] != null && !wsdescs[j].isGeneric()) {
                            ifconf = entry.getInterfaces();
                            if (ifconf == null || Arrays.asList(wsdescs[j].getInterfaces()).containsAll(ifconf.keySet())) {
                                log_.debug("Detected configuration match for " + wsdescs[j] + " in Environment:" + key);
                                if (online(wsdescs[j])) {
                                    log_.trace("Auto-creating '" + wsname + "' client...");
                                    client = ClientProxyFactory.getClientProxy(wsdescs[j]);
                                    log_.trace("Sucessfully auto-created client " + client);
                                    return client;
                                } else {
                                    log_.info("Webservice not online - skipping ...");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log_.caught(e);
                    System.err.println("[WebserviceManager] Error: " + e.getMessage());
                }
            }
            return null;
        }
    }

    /**
     * In case there is a matching {@link ConfigurationEntry} for the given
     * <code>key</code> and <code>object</code> class,
     * the given object will be
     * {@link WebserviceService#deployWebservice(String,Object,Class[]) deployed}
     * as <code>Webservice</code> named '<code>key</code>'.
     */
    public void onPublication(String key, Object object) {
        Class[] interfaces;
        String wsname;
        String msg;
        int status;
        if (key == null) {
            log_.warning("Key is null");
            return;
        }
        if (object == null) {
            log_.warning("Object is null");
            return;
        }
        synchronized (lock_) {
            if ((status = status()) == STATUS_HALTED) {
                return;
            } else if (status == STATUS_INOPERATIVE) {
                if (key.equals(WHATIS_WSS_)) {
                    if (object instanceof WebserviceService) {
                        wss_ = (WebserviceService) object;
                        log_.info("Detected WebserviceService publication");
                    }
                } else if (key.equals(WHATIS_UDDIS_)) {
                    if (object instanceof UddiService) {
                        uddis_ = (UddiService) object;
                        log_.info("Detected UddiService publication");
                    }
                }
                if (status() != STATUS_RUNNING) {
                    return;
                }
                log_.info("The WebserviceManager has been resumed");
            }
            if (handleObject(object)) {
                interfaces = matchInterfaces(findEntries(key), object.getClass());
            } else {
                log_.warning("The given object is a Webservice client stub: ignore!");
                interfaces = null;
            }
            if (interfaces != null) {
                log_.debug("Detected configuration match for " + object + " in Environment:" + key);
                wsname = key.substring(1);
                log_.trace("Auto-deploying Webservice '" + wsname + "'...");
                try {
                    uddis_.registerWebserviceDescription(wss_.deployWebservice(wsname, object, interfaces).getDescription());
                    msg = "Webservice '" + wsname + "' has been auto-deployed";
                    log_.trace(msg);
                    if (!quiet_) {
                        System.out.println("[WebserviceManager] " + msg);
                    }
                } catch (Exception e) {
                    log_.caught(e);
                    System.err.println("[WebserviceManager] Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * In case there is a matching {@link ConfigurationEntry} for the given
     * <code>key</code> and <code>object</code> class,
     * the corresponding <code>Webservice</code> object will be
     * {@link WebserviceService#undeployWebservice(String) undeployed}.
     */
    public void onRetraction(String key, Object object) {
        Class[] interfaces;
        String wsname;
        String msg;
        int status;
        if (key == null) {
            log_.warning("Key is null");
            return;
        }
        if (object == null) {
            log_.warning("Object is null");
            return;
        }
        synchronized (lock_) {
            if ((status = status()) == STATUS_HALTED) {
                return;
            }
            if (key.equals(WHATIS_WSS_)) {
                wss_ = null;
                log_.info("Detected WebserviceService retraction");
            } else if (key.equals(WHATIS_UDDIS_)) {
                uddis_ = null;
                log_.info("Detected UddiService retraction");
            }
            if (wss_ == null || uddis_ == null) {
                if (status == STATUS_RUNNING) {
                    log_.info("The WebserviceManager has become inoperative");
                }
                return;
            }
            if (handleObject(object)) {
                interfaces = matchInterfaces(findEntries(key), object.getClass());
            } else {
                log_.warning("The given object is a Webservice client stub: ignore!");
                interfaces = null;
            }
            if (interfaces != null) {
                log_.debug("Detected configuration match for " + object + " in Environment:" + key);
                wsname = key.substring(1);
                log_.trace("Auto-undeploying Webservice '" + wsname + "'...");
                try {
                    uddis_.revokeWebserviceDescription(wss_.undeployWebservice(wsname).getDescription());
                    msg = "Webservice '" + wsname + "' has been auto-undeployed";
                    log_.trace(msg);
                    if (!quiet_) {
                        System.out.println("[WebserviceManager] " + msg);
                    }
                } catch (Exception e) {
                    log_.caught(e);
                    System.err.println("[WebserviceManager] Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Find all {@link ConfigurationEntry} objects that imply the given
     * <code>path</code>.
     *
     * @param path The path to be found
     * @return The <code>Set</code> of matching
     *   <code>ConfigurationEntry</code> objects
     */
    protected Set findEntries(String path) {
        ConfigurationEntry entry;
        CanonicalPath cpath;
        Set entries;
        Iterator i;
        entries = new HashSet();
        cpath = path.length() > 0 ? new CanonicalPath(path, '/') : new CanonicalPath('/');
        for (i = config_.iterator(); i.hasNext(); ) {
            entry = (ConfigurationEntry) i.next();
            if (entry.getPath().implies(cpath)) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Search the given {@link ConfigurationEntry} <code>Set</code> for
     * interface definitions matching the given <code>Class</code>.
     *
     * @param entries The configuration entries to be checked
     * @param clazz The class to match the configured interfaces against
     * @return The matching interfaces, or <code>null</code> if no
     *   matching <code>ConfigurationEntry</code> could be found
     */
    protected static Class[] matchInterfaces(Set entries, Class clazz) {
        ConfigurationEntry entry;
        Collection ifclasses;
        Class[] impl;
        Iterator i;
        Map ifconf;
        for (i = entries.iterator(); i.hasNext(); ) {
            entry = (ConfigurationEntry) i.next();
            ifconf = entry.getInterfaces();
            impl = clazz.getInterfaces();
            if (ifconf != null) {
                ifclasses = ifconf.values();
                if (!Arrays.asList(impl).containsAll(ifclasses)) {
                    continue;
                }
            } else {
                ifclasses = Arrays.asList(impl);
            }
            return (Class[]) ifclasses.toArray(new Class[0]);
        }
        return null;
    }

    /**
     * Checks if the Web service with the given 
     * <code>Webservice.Description</code> is online, 
     * by trying to connect its WSDL URL
     *  
     * @param wsdesc The Web service description
     * @return <code>true</code>, if WSDL URL derived 
     *   by <code>wsdesc</code> is well-formed, and 
     *   a connection to this URL can be established
     *   successfully, <code>false</code> otherwise.
     */
    protected boolean online(Webservice.Description wsdesc) {
        URLConnection conn;
        URL url;
        conn = null;
        try {
            url = new URL(wsdesc.getWsdlURL());
            conn = url.openConnection();
            conn.connect();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                conn.getOutputStream().close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * This method analyzes the given object <code>obj</code>.
     * 
     * If the given object is a dynamic proxy encapsulating
     * a <code>WebserviceInvocationHandler</code>, thus is
     * a Webservice client stub generated by this Webservice 
     * Manager, the object should be ignored and this method
     * returns <code>false</code>. 
     * 
     * @param obj The object to analyze.
     * @return <code>true<code> if the given object shall be handled
     *   by the Webservice Manager, <code>false</code> otherwise.
     */
    protected boolean handleObject(Object obj) {
        try {
            if (Proxy.getInvocationHandler(obj) instanceof WebserviceInvocationHandler) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * The command line interface implementation. For more information run
     * <p><nobreak><tt>java WebserviceManager -help</tt></nobreak>
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        ArgsParser ap;
        ap = new ArgsParser(DESCR_);
        try {
            ap.parse(args);
            if (ap.isDefined(CMDLINE_HELP_)) {
                System.out.println(usage());
                return;
            }
            if (ap.isDefined(CMDLINE_START_)) {
                instance_.startup(ap.stringValue(CMDLINE_START_), ap.isDefined(CMDLINE_QUIET_));
            } else if (ap.isDefined(CMDLINE_STOP_)) {
                instance_.shutdown();
            }
            if (ap.isDefined(CMDLINE_STATUS_)) {
                System.out.println(instance_.createReport());
            }
        } catch (Exception e) {
            log_.caught(e);
            System.err.println("[WebserviceManager] Error: " + e.getMessage());
            if (e instanceof ArgsParserException) {
                System.err.println("\n" + usage());
            }
        }
    }

    /**
     * @return Some usage info.
     */
    protected static String usage() {
        return ("Usage: java WebserviceManager\n" + "\t-" + CMDLINE_HELP_ + "\n" + "\t-" + CMDLINE_START_ + " <config-file>" + " [-" + CMDLINE_QUIET_ + "]\n" + "\t-" + CMDLINE_STOP_ + "\n" + "\t-" + CMDLINE_STATUS_ + "\n" + "\nwhere:\n" + "\n-" + CMDLINE_HELP_ + "\n" + "\tShows this text.\n" + "\n-" + CMDLINE_START_ + " <config-file>" + " [-" + CMDLINE_QUIET_ + "]\n" + "\tStarts the WebserviceManager and loads the specified\n" + "\tconfiguration file. '-" + CMDLINE_QUIET_ + "' may be added to omit console output.\n" + "\n-" + CMDLINE_STOP_ + "\n" + "\tStops the WebserviceManager.\n" + "\n-" + CMDLINE_STATUS_ + "\n" + "\tPrints a status report to the console.\n" + "\nNOTICE: The WebserviceLauncher requires a previously " + "published\n\t" + WebserviceService.class.getName() + "\n\tFor more information, please concern\n\t" + de.fhg.igd.semoa.webservice.WebserviceServiceImpl.class.getName());
    }

    /**
     * This class represents a single configuration entry.
     */
    protected static class ConfigurationEntry {

        /**
         * The canonical path
         */
        protected CanonicalPath cpath_;

        /**
         * The interfaces to check for, or <code>null</code>
         * if all interfaces shall be considered;
         * if not <code>null</code>,
         * class names are used as keys,
         * the corresponding <code>Class</code> objects as values
         */
        protected Map interfaces_;

        /**
         * Create a <code>ConfigurationEntry</code>.
         */
        public ConfigurationEntry(String cpath, String interfaces) throws IllegalArgumentException, NullPointerException {
            StringTokenizer toki;
            String cname;
            String msg;
            if (cpath == null) {
                throw new NullPointerException("cpath");
            }
            if (interfaces == null) {
                throw new NullPointerException("interfaces");
            }
            cpath_ = cpath.length() > 0 ? new CanonicalPath(cpath, '/') : new CanonicalPath('/');
            if (interfaces.equals(WebserviceService.WILDCARD)) {
                interfaces_ = null;
            } else {
                interfaces_ = new HashMap();
                toki = new StringTokenizer(interfaces, ",");
                while (toki.hasMoreTokens()) {
                    cname = toki.nextToken().trim();
                    try {
                        interfaces_.put(cname, Class.forName(cname));
                    } catch (ClassNotFoundException e) {
                        msg = "Unresolvable interface class: " + cname;
                        log_.caught(LogLevel.ERROR, msg, e);
                        throw new IllegalArgumentException(msg);
                    }
                }
            }
            log_.debug("Instance (" + cpath_ + "," + (interfaces_ != null ? interfaces_.toString() : WebserviceService.WILDCARD) + ") successfully created");
        }

        public CanonicalPath getPath() {
            return cpath_;
        }

        public Map getInterfaces() {
            return interfaces_;
        }

        public String toString() {
            StringBuffer str;
            Iterator i;
            str = new StringBuffer();
            str.append("\n").append(cpath_).append(":\n");
            if (interfaces_ != null) {
                for (i = interfaces_.keySet().iterator(); i.hasNext(); ) {
                    str.append("\t").append(i.next()).append("\n");
                }
            } else {
                str.append("\t").append(WebserviceService.WILDCARD).append("\n");
            }
            return str.toString();
        }
    }
}
