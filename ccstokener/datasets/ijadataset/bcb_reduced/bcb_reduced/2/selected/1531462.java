package net.sf.openrds.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import net.sf.openrds.INode;
import net.sf.openrds.ISystemProperties;
import net.sf.openrds.NodeEventAdaptor;
import net.sf.openrds.NodeFactory;
import net.sf.openrds.ProcessNode;
import net.sf.openrds.RegistryHandler;

/**
 * This class implements a generic process node to be easily
 * used and managed in a cluster environment.<BR>
 * This node provides some interactive console-based options.
 * Press 'H' at any time during it's execution for the full list of options.
 * @author Rodrigo Rosauro
 * @since OpenRDS 1.1-beta
 */
public class ClusterNode {

    /** Application exit code during a normal shutdown (0)*/
    public static final int EXIT_NORMAL_SHUTDOWN = 0;

    /** Application exit code during a normal restart (171)*/
    public static final int EXIT_NORMAL_RESTART = 171;

    /** Application exit code during a fatal error (172)*/
    public static final int EXIT_FATAL_ERROR = 172;

    /** Name of the properties file that this node uses ("clusternode.properties") */
    public static final String PROPERTIES_FILE = "clusternode.properties";

    /** Name of the property that defines the Main Node ip ("openrds.server.host") */
    public static final String PROPERTY_SERVER = "openrds.server.host";

    /** Name of the property that defines the remote HTTP port ("openrds.http.port") */
    public static final String PROPERTY_HTTP_PORT = "openrds.http.port";

    /** Name of the property that defines the remote registry port ("openrds.registry.port") */
    public static final String PROPERTY_REGISTRY_PORT = "openrds.registry.port";

    /** Name of the file that will be generated after downloading an updated version */
    public static final String UPDT_FILE = "openrds.updated.jar";

    /** Base URL for web updates */
    public static final String URL_BASE = "http://openrds.sourceforge.net/webupdate/";

    /** URL for checking the latest OpenRDS version on the web */
    public static final String URL_UPDT_VERSION = URL_BASE + "latestversion.php";

    private final File propFile = new File(PROPERTIES_FILE);

    private final BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in), 128);

    private Properties properties;

    private ProcessNode startedNode;

    /**
	 * Application main.
	 * @param args none
	 */
    public static void main(String[] args) {
        try {
            new ClusterNode().start(args);
        } catch (Throwable t) {
            System.out.println("[FATAL] - Exception trapped. Please report this bug on http://sf.net/openrds/");
            t.printStackTrace();
            System.exit(EXIT_FATAL_ERROR);
        }
    }

    /**
	 * Starts the cluster node execution.
	 * @param args process arguments (currently ignored)
	 */
    public void start(String[] args) {
        args.getClass();
        System.setProperty("java.library.path", "." + File.pathSeparator + System.getProperty("java.library.path"));
        loadProperties();
        String host = properties.getProperty(PROPERTY_SERVER);
        if (host == null) {
            host = requestHost();
            appendPropertyToFile(PROPERTY_SERVER, host);
        }
        String registry = properties.getProperty(PROPERTY_REGISTRY_PORT);
        if (!isInteger(registry)) {
            registry = requestRegistryPort();
            appendPropertyToFile(PROPERTY_REGISTRY_PORT, registry);
        }
        String http = properties.getProperty(PROPERTY_HTTP_PORT);
        if (!isInteger(http)) {
            http = requestHttpPort();
            appendPropertyToFile(PROPERTY_HTTP_PORT, http);
        }
        String baseIp = properties.getProperty(ISystemProperties.BASE_IP);
        if (baseIp == null) {
            baseIp = requestBaseIp();
        }
        if (!"none".equals(baseIp)) {
            System.setProperty(ISystemProperties.BASE_IP, baseIp);
        }
        startNode(host, Integer.parseInt(registry), Integer.parseInt(http));
    }

    /**
	 * Requests the base IP to user
	 * @return base ip, or "none" if it should not be set
	 */
    private String requestBaseIp() {
        if (confirm("Do you want to define a base-ip for OpenRDS?\n(This is only needed if you have more than one IP.)")) {
            System.out.println("The system has detected the following IPs:");
            try {
                for (Enumeration e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                    final NetworkInterface itf = (NetworkInterface) e.nextElement();
                    for (Enumeration ips = itf.getInetAddresses(); ips.hasMoreElements(); ) {
                        final InetAddress ip = (InetAddress) ips.nextElement();
                        if (ip.getHostAddress().indexOf(':') == -1) {
                            System.out.println(ip.getHostAddress());
                        }
                    }
                }
            } catch (SocketException e) {
                System.out.println("(Could not list all interfaces.)");
            }
            String ip = "";
            do {
                ip = getInput("Please enter the desired base ip to use").trim();
            } while (ip.length() == 0);
            appendPropertyToFile(ISystemProperties.BASE_IP, ip);
            return ip;
        } else {
            appendPropertyToFile(ISystemProperties.BASE_IP, "none");
            return "none";
        }
    }

    /**
	 * Starts the cluster node execution with the given information
	 * @param host main node host
	 * @param registryPort registry port
	 * @param httpPort http port
	 */
    private void startNode(String host, int registryPort, int httpPort) {
        try {
            System.out.println("Using network address '" + RegistryHandler.getInstance().getInetAddress().getHostAddress() + "'.");
            RegistryHandler.getInstance().initialize(host, registryPort, httpPort);
            this.startedNode = NodeFactory.getInstance().createProcessNode();
            this.startedNode.addNodeEventListener(new Listener());
            showHelp();
            this.startedNode.start();
            System.out.println("Node has been successfully started - Trying to connect to main node.");
            inputLoop();
        } catch (Exception e) {
            System.out.println("[FATAL] - Error starting node.");
            e.printStackTrace();
            System.exit(EXIT_FATAL_ERROR);
            return;
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[FATAL] - Could not load OpenRDS library. Please check it.");
            System.out.println("Stack trace:");
            e.printStackTrace();
            System.exit(EXIT_FATAL_ERROR);
            return;
        }
    }

    /**
	 * Enters in an infinite loop, reading user commands and executing them.
	 */
    private void inputLoop() {
        for (; ; ) {
            final String command = readLine().trim().toLowerCase();
            if ("exit".equals(command) || "quit".equals(command)) {
                commandExit();
            } else if ("restart".equals(command)) {
                commandRestart();
            } else if ("h".equals(command) || "help".equals(command)) {
                showHelp();
            } else if ("update".equals(command)) {
                commandUpdate();
            } else if ("config".equals(command)) {
                commandConfig();
            } else if ("clear".equals(command)) {
                System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            } else {
                System.out.println("Unknown command. Type 'help' for commands list.");
            }
        }
    }

    /**
	 * Executes the "update" command
	 */
    private void commandUpdate() {
        if (!confirm("Are you sure you want to check for updates on the web?")) {
            System.out.println("Operation canceled.");
            return;
        }
        try {
            System.out.println("Checking for the latest version. Please wait...");
            final byte data[] = readURL(new URL(URL_UPDT_VERSION + "?version=" + Version.getVersion()));
            final Properties prop = new Properties();
            prop.load(new ByteArrayInputStream(data));
            if (!prop.containsKey("requestor-version")) {
                throw new Exception("Got unexpected data.");
            }
            final String version = prop.getProperty("latest-version");
            final String jarUrl = prop.getProperty("latest-jar");
            final String message = prop.getProperty("message");
            if (message != null && message.length() > 0) {
                System.out.println(message);
                if (version == null || version.length() == 0) {
                    return;
                }
            } else if (version == null || version.length() == 0 || Version.getVersion().equals(version)) {
                System.out.println("The version is up to date!");
                return;
            }
            if (jarUrl == null || jarUrl.length() == 0) {
                throw new Exception("Server didn't return property 'latest-jar'.");
            }
            updateVersion(version, jarUrl);
        } catch (Exception e) {
            final String message = "" + "It was not possible to check for updates on the web.\n" + "Error: " + e.getMessage() + "\n" + "\n" + "Please note that you need a direct internet connection.\n" + "Proxy servers are not supported because modifing java proxy properties at runtime could cause undesired behaviours on the cluster node.";
            System.out.println(message);
        }
    }

    /**
	 * Updates the current software version.
	 * @param version latest version
	 * @param jarUrl url to download latest version
	 */
    private void updateVersion(String version, String jarUrl) {
        System.out.println("There is a new version of OpenRDS available: " + version);
        if (confirm("Do you want to update this node? (Will restart the application)")) {
            System.out.println("Connecting to update server...");
            InputStream in = null;
            OutputStream out = null;
            try {
                final URL url = new URL(URL_BASE + jarUrl);
                final URLConnection con = url.openConnection();
                in = new BufferedInputStream(con.getInputStream());
                System.out.println("Downloading... (" + (con.getContentLength() / 1024) + " KB)");
                final byte[] data = readInput(in);
                System.out.println("Writing file '" + UPDT_FILE + "'.");
                out = new FileOutputStream(UPDT_FILE);
                out.write(data);
                out.close();
                System.out.println("Update process finished!");
                commandRestart();
            } catch (Exception e) {
                final String message = "" + "An error happened while performing the update process.\n" + "Error message: " + e.getMessage() + "\n" + "Error class: " + e.getClass().getName();
                System.out.println(message);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            System.out.println("Operation canceled.");
        }
    }

    /**
	 * Reads an URL data
	 * @param url url
	 * @return data
	 * @throws IOException on any error
	 */
    private byte[] readURL(URL url) throws IOException {
        return readInput(new BufferedInputStream(url.openStream()));
    }

    /**
	 * Reads data from a stream.
	 * @param in stream
	 * @return data
	 * @throws IOException on any error
	 */
    private byte[] readInput(InputStream in) throws IOException {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream(2048);
            int b = 0;
            while ((b = in.read()) != -1) {
                bout.write(b);
            }
            return bout.toByteArray();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
	 * Executes the "config" command.
	 */
    private void commandConfig() {
        if (confirm("This will clear current configuration, are you sure?")) {
            System.out.println("Deleting file '" + PROPERTIES_FILE + "'.");
            propFile.delete();
            if (propFile.isFile()) {
                System.out.println("[ERROR] - Cannot delete file. Please check if it is not in use.");
                return;
            }
            System.out.println("Restarting process node...");
            System.exit(EXIT_NORMAL_RESTART);
        } else {
            System.out.println("Operation canceled.");
        }
    }

    /**
	 * Executes the "exit" command
	 */
    private void commandExit() {
        new Thread("commandExit timeout thread") {

            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                System.out.println("Timed-out while stopping node. Exiting anyway...");
                System.exit(EXIT_NORMAL_SHUTDOWN);
            }
        }.start();
        try {
            System.out.println("Stopping process node...");
            startedNode.finish();
        } catch (Exception ignored) {
        } finally {
            System.out.println("Exiting...");
            System.exit(EXIT_NORMAL_SHUTDOWN);
        }
    }

    /**
	 * Executes the "restart" command
	 */
    private void commandRestart() {
        new Thread("commandRestart timeout thread") {

            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                System.out.println("Timed-out while stopping node. Restarting anyway...");
                System.exit(EXIT_NORMAL_RESTART);
            }
        }.start();
        try {
            System.out.println("Stopping process node...");
            startedNode.finish();
        } catch (Exception ignored) {
        } finally {
            System.out.println("Restarting...");
            System.exit(EXIT_NORMAL_RESTART);
        }
    }

    /**
	 * Displays help information on console
	 */
    private void showHelp() {
        final String s = "" + "# OpenRDS " + Version.getVersion() + " (Built on " + Version.getBuildDate() + ")\n" + "# During the execution of the node, type any of the following\n" + "# commands and press [ENTER] to execute it.\n" + "# \n" + "# exit    - Stops the process node execution.\n" + "# quit    - Same as 'exit'.\n" + "# restart - Restarts the process node, unloading any loaded code.\n" + "# update  - Checks for online updates (requires internet).\n" + "# config  - Change node's configuration (will cause a restart).\n" + "# clear   - Clears the console, by printing 40 '\\n' chars.\n" + "# help    - Displays this message again.\n" + "# h       - Same as 'help'.\n";
        System.out.println(s);
    }

    /**
	 * Returns true if a string is an integer
	 * @param s string
	 * @return boolean
	 */
    private boolean isInteger(String s) {
        try {
            if (s != null) {
                Integer.parseInt(s);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
	 * Appends a property to the properties file
	 * @param name property name
	 * @param value property value
	 */
    private void appendPropertyToFile(String name, String value) {
        try {
            final boolean isNew = !propFile.exists();
            FileWriter w = new FileWriter(propFile, true);
            if (isNew) {
                w.write("#\n# Do not edit this file, it has been automatically generated.\n#");
            }
            w.write("\n" + name + ": " + value);
            w.close();
        } catch (IOException e) {
            System.out.println("[FATAL] - Could not write to file '." + PROPERTIES_FILE + "'. (" + e.getMessage() + ")");
            System.exit(EXIT_FATAL_ERROR);
        }
    }

    /**
	 * Requests the registry port to user
	 * @return registry port
	 */
    private String requestRegistryPort() {
        if (confirm("Do you want to use the default registry port?")) {
            return RegistryHandler.REGISTRY_PORT + "";
        }
        String value = null;
        do {
            value = getInput("Please type the registry port that you want to use");
            if (!isInteger(value)) {
                System.out.println("Invalid port number");
                value = null;
            }
        } while (value == null);
        return value;
    }

    /**
	 * Requests the http port to user
	 * @return http port
	 */
    private String requestHttpPort() {
        if (confirm("Do you want to use the default HTTP port?")) {
            return RegistryHandler.HTTP_PORT + "";
        }
        String value = null;
        do {
            value = getInput("Please type the HTTP port that you want to use");
            if (!isInteger(value)) {
                System.out.println("Invalid port number");
                value = null;
            }
        } while (value == null);
        return value;
    }

    /**
	 * Requests the main node ip to user
	 * @return main node ip
	 */
    private String requestHost() {
        String value = null;
        do {
            value = getInput("Please type the IP or host-name of the Main Node");
            try {
                InetAddress.getByName(value);
            } catch (Exception e) {
                if (!confirm("The specified host does not seen to exist or is invalid, are you sure it is correct?")) {
                    value = null;
                }
            }
        } while (value == null);
        return value;
    }

    /**
	 * Gets user input, displaying a message first
	 * @param message message to display
	 * @return user input
	 */
    private String getInput(String message) {
        System.out.print(message + ": ");
        return readLine();
    }

    /**
	 * Dysplays a confirmation message (y/n) to user and returns user option
	 * @param message message to display
	 * @return true if user confirmed
	 */
    private boolean confirm(String message) {
        for (; ; ) {
            System.out.print(message + " (y/n): ");
            final String s = readLine().toLowerCase();
            if ("y".equals(s) || "yes".equals(s)) {
                return true;
            }
            if ("n".equals(s) || "no".equals(s)) {
                return false;
            }
        }
    }

    /**
	 * Reads a text entered on standard input
	 * @return input text
	 */
    private String readLine() {
        try {
            final String s = sysin.readLine();
            if (s == null) {
                System.out.println("\nReceived signal to stop application. Exiting");
                System.exit(EXIT_NORMAL_SHUTDOWN);
            }
            return s;
        } catch (IOException e) {
            System.out.println();
            System.out.println("[WARNING] - Error reading console input. (" + e.getMessage() + ")");
            System.out.println("            All commands will be ignored.");
            System.out.println("            Running in non-interactive mode.");
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e1) {
                    System.out.println("Thread has been interrupted while sleeping.");
                }
            }
            return null;
        }
    }

    /**
	 * Loads the properties file.
	 */
    private void loadProperties() {
        properties = new Properties();
        if (propFile.isFile()) {
            try {
                final FileInputStream in = new FileInputStream(propFile);
                properties.load(in);
                in.close();
            } catch (Exception e) {
                System.out.println("[FATAL] - Error loading file '" + PROPERTIES_FILE + "'. (" + e.getMessage() + ")");
                System.exit(EXIT_FATAL_ERROR);
            }
        }
    }

    /**
	 * Node events listener
	 */
    private static final class Listener extends NodeEventAdaptor {

        private boolean anyConn = false;

        /** {@inheritDoc} */
        public void nodeLostConnection(INode node) {
            if (anyConn) {
                System.out.println("Lost connection with main node.");
            }
        }

        /** {@inheritDoc} */
        public void nodeRestoredConnection(INode node) {
            if (anyConn) {
                System.out.println("Restored connection with main node.");
            } else {
                anyConn = true;
                System.out.println("Connected to main node.");
            }
        }

        /** {@inheritDoc} */
        public void nodeFinished(INode node) {
            System.out.println("Node has been finished.");
        }
    }
}
