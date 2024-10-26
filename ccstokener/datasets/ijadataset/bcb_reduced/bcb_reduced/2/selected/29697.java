package com.finchsync.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.finchsync.helper.XMLReader;
import com.finchsync.helper.XMLWriter;
import com.finchsync.http.HttpServer;
import com.finchsync.http.HttpServlet;
import com.finchsync.sync.StatusServlet;
import com.finchsync.sync.SyncClientConfig;
import com.finchsync.sync.SyncConfig;
import com.finchsync.sync.SyncServlet;
import com.finchsync.sync.SyncSourceConfig;
import com.finchsync.sync.SyncSourceConfigFactory;

/**
 * This class starts the FinchSync server.
 * <p>
 * The HTTP-Server is started and the GUI initialized.
 * <p>
 * $Author: $
 * <p>
 * $Revision: $
 */
public class SyncMain implements SyncConfig {

    /** Logger for this class */
    private static org.apache.log4j.Logger mylog = org.apache.log4j.Logger.getLogger(SyncMain.class);

    public static final int OS_UNKNOWN = 0;

    public static final int OS_WINDOWS = 1;

    public static final int OS_OSX = 2;

    public static final int OS_LINUX = 3;

    private static int osid = OS_UNKNOWN;

    /** List of SyncSourceConfig objects */
    private List<SyncSourceConfig> ssc_list;

    /** List of SyncClientConfig objects */
    private List<SyncClientConfig> client_list;

    private String rootpath;

    private SyncMainWindow syncmainwindow;

    /**
	 * Set, if server was started with gui.
	 */
    @SuppressWarnings("unused")
    private boolean gui;

    /**
	 * The singleton instance of this class.
	 */
    private static SyncMain syncmain;

    /**
	 * Contains the server settings.<br>
	 * Populates keys are:<br>
	 * #
	 *<table>
	 *<tr>
	 * <th>key</th>
	 * <th>contents</th>
	 * <th>default</th>
	 * </tr>
	 *<tr>
	 * porthttp
	 * <td></td>
	 * <td>Port for unencrypted connections</td>
	 * <td>8080</td>
	 * </tr>
	 *<tr>
	 * porthttp_enable
	 * <td></td>
	 * <td>Accept connections on unencrypted http-port</td>
	 * <td>true</td>
	 * </tr>
	 *<tr>
	 * portssl
	 * <td></td>
	 * <td>Port for encrypted connections</td>
	 * <td>443</td>
	 * </tr>
	 *<tr>
	 * portssl_enable
	 * <td></td>
	 * <td>Accept connections on encrypted ssl-port</td>
	 * <td>true</td>
	 * </tr>
	 *<tr>
	 * loglevel
	 * <td></td>
	 * <td>logging level, on of 'off','debug','info','warn','error','fatal'</td>
	 * <td>off</td>
	 * </tr>
	 *</table>
	 */
    private Map<String, String> settings;

    public static final String S_PORTHTTP = "porthttp";

    public static final String S_PORTHTTP_ENABLED = "porthttp_enabled";

    public static final String S_PORTSSL = "portssl";

    public static final String S_PORTSSL_ENABLED = "portssl_enabled";

    public static final String S_LOGLEVEL = "loglevel";

    public static final String S_ADMINLOGIN = "adminlogin";

    public static final String S_ADMINPASSWORD = "adminpassword";

    public static final String S_ENABLESTATUSPAGE = "enablestatuspage";

    private HttpServer httpserver;

    private SyncServlet syncservlet;

    private String logfilename;

    public static SyncMain getSyncMain() {
        if (syncmain == null) throw new IllegalStateException("No instance!");
        return syncmain;
    }

    /**
	 * Returns the setting with the specified name.
	 * 
	 * @param name
	 *            name of the setting.
	 * @return the setting value or null.
	 */
    public String getSetting(String name) {
        return settings.get(name);
    }

    /**
	 * Returns a Map with all settings.
	 * 
	 * @return map with settings.
	 */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
	 * Returns the name of the logfile.
	 * 
	 * @return the name of the logfile.
	 */
    public String getLogFileName() {
        return logfilename;
    }

    /**
	 * Returns the path to the 'finchconfig' directory.
	 * 
	 * @return path to 'finchconfig' directory.
	 */
    public String getRootPath() {
        return rootpath;
    }

    /** Creates new form SyncMain */
    private SyncMain(boolean gui) {
        if (syncmain != null) throw new IllegalStateException("Instance already created!");
        syncmain = this;
        this.gui = gui;
        String os = System.getProperty("os.name");
        if (os.toLowerCase().indexOf("windows") > -1) osid = OS_WINDOWS;
        rootpath = new File("finchconfig").getAbsolutePath();
        Logger root = Logger.getRootLogger();
        PatternLayout pl = new PatternLayout("[slf5s.start]%d{DATE}[slf5s.DATE]%n%p[slf5s.PRIORITY]%n%x[slf5s.NDC]%n%t[slf5s.THREAD]%n%c[slf5s.CATEGORY]%n%l[slf5s.LOCATION]%n%m[slf5s.MESSAGE]%n%n");
        logfilename = rootpath + "/finch.log";
        try {
            RollingFileAppender rfa = new RollingFileAppender(pl, logfilename);
            rfa.setMaxFileSize("5MB");
            root.addAppender(rfa);
            root.setLevel(Level.OFF);
            Logger.getLogger("net").setLevel(Level.OFF);
        } catch (IOException ioe) {
            String msg = "Can't open Logfile '" + logfilename + "'. Logging disabled.";
            if (gui) JOptionPane.showMessageDialog(null, msg); else System.out.println(msg);
        }
        if (checkDirs(rootpath) == false) {
            String msg = "Faild to create configuration directory at '" + rootpath + "'!";
            mylog.fatal(msg);
            if (gui) JOptionPane.showMessageDialog(null, msg);
            System.exit(0);
        }
        ssc_list = new Vector<SyncSourceConfig>();
        client_list = new Vector<SyncClientConfig>();
        settings = new HashMap<String, String>();
        settings.put(S_PORTHTTP, "8080");
        settings.put(S_PORTHTTP_ENABLED, "true");
        settings.put(S_PORTSSL, "443");
        settings.put(S_PORTSSL_ENABLED, "true");
        settings.put(S_LOGLEVEL, "off");
        File f = new File(rootpath + "/serversettings.xml");
        if (!f.exists()) {
            String msg = "Can't find configuration file '" + f.getAbsolutePath() + "'. Using default settings.";
            mylog.warn(msg);
            if (gui) JOptionPane.showMessageDialog(null, msg);
        } else if (readConfig(f) == false) {
            String msg = "Error reading configuration from file '" + f.getAbsolutePath() + "'.";
            mylog.error(msg);
            if (gui) JOptionPane.showMessageDialog(null, msg);
            System.exit(0);
        }
        String loglevel = settings.get(S_LOGLEVEL);
        if (loglevel == null) loglevel = "off";
        setLogLevel(loglevel);
        mylog.info("Server started.");
        httpserver = new HttpServer();
        httpserver.setHttpPort(Integer.parseInt(settings.get(S_PORTHTTP)));
        if (gui) syncmainwindow = new SyncMainWindow(this);
        syncservlet = new SyncServlet(this, syncmainwindow);
        httpserver.registerServlet("/sync", syncservlet);
        if ("true".equals(settings.get(S_ENABLESTATUSPAGE))) {
            if (settings.get(S_ADMINLOGIN) == null) {
                mylog.error("Admin account not configured !");
                if (gui) JOptionPane.showMessageDialog(null, "Please configure the admin account! (Menu 'File/Server configuration')"); else System.exit(0);
            }
            HttpServlet statusservlet = new StatusServlet();
            httpserver.registerServlet("/status", statusservlet);
        }
        if (startServer() == false) {
            if (httpserver.getErrorMessage().startsWith("java.net.BindException")) {
                StringBuffer sb = new StringBuffer("Error starting server:'" + httpserver.getErrorMessage() + "'\n\n");
                sb.append("Problem: Port '" + settings.get(S_PORTHTTP) + "' could not be opened.\n\n");
                sb.append("Possible Reasons:\n\n1) Port may be already in use by another application.");
                sb.append("Select 'File/Server Configuration' and choose an other port.\n(Try numbers between 8080 and 8087 for example.)");
                sb.append("\n\n2) FinchSync was blocked by your firewall. FinchSync (java/javaw) must not be blocked.");
                mylog.error(sb.toString());
                if (gui) JOptionPane.showMessageDialog(null, sb.toString());
            } else {
                String msg = "Error starting server:'" + httpserver.getErrorMessage() + "'";
                mylog.error(msg);
                if (gui) JOptionPane.showMessageDialog(null, msg);
            }
        }
    }

    /**
	 * Sets the log-level of the log4j system.
	 * 
	 * @param level
	 *            one of 'off','debug','info','warn',error','fatal'.
	 */
    private void setLogLevel(String level) {
        Logger root = Logger.getRootLogger();
        if ("debug".equals(level)) root.setLevel(Level.DEBUG); else if ("info".equals(level)) root.setLevel(Level.INFO); else if ("warn".equals(level)) root.setLevel(Level.WARN); else if ("error".equals(level)) root.setLevel(Level.ERROR); else if ("fatal".equals(level)) root.setLevel(Level.FATAL); else root.setLevel(Level.OFF);
    }

    public List<SyncSourceConfig> getSyncSourceConfigurations() {
        return ssc_list;
    }

    public List<SyncClientConfig> getSyncClientConfigurations() {
        return client_list;
    }

    /**
	 * Starts the server
	 * 
	 * @return true, if successful.
	 */
    private boolean startServer() {
        if (httpserver == null) throw new RuntimeException("Missing http-server.");
        if (httpserver.isRunning()) return true;
        httpserver.startServer();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        return httpserver.isRunning();
    }

    /**
	 * Stops the server. May take a few secondes.
	 * 
	 * @return true, if the server was stopped.
	 */
    public boolean stopServer() {
        if (httpserver == null) throw new RuntimeException("Missing http-server.");
        if (httpserver.isRunning()) {
            httpserver.stopServer();
            int i = 50;
            while (i-- > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                if (!httpserver.isRunning()) break;
            }
            if (i == 0) mylog.warn("Could not stop server!"); else mylog.info("Server stopped.");
        }
        return httpserver.isRunning() == false;
    }

    /**
	 * Returns the OS the server is running on.
	 * 
	 * @return OS id, one of OS_UNKNOWN,OS_WINDOWS,OS_OSX,OS_LINUX
	 */
    public static int getOsId() {
        return osid;
    }

    /**
	 * Checks, if the the specified path exists. This is the directory, where
	 * all settings will be stored. <br>
	 * If the directory does not exists, it will be created and an empty
	 * configuration file will be written.
	 * 
	 * @param path
	 *            path to check.
	 * @return false, if the path could not be created.
	 */
    private boolean checkDirs(String path) {
        File dir = new File(path);
        try {
            if (!dir.isDirectory()) {
                dir.mkdirs();
                File f = new File(path + "/serversettings.xml");
                mylog.debug("Creating new configuration file '" + f.getPath() + "'.");
                if (writeConfig(f) == false) JOptionPane.showMessageDialog(null, "Can't initialize configuration-file '" + f.getAbsolutePath() + "'");
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
	 * Starts the server.
	 * <p>
	 * Supported command line arguments:
	 * <p>
	 * -nogui: Start server without user interface.
	 * <p>
	 * -stopserver: Stops a running server.
	 * 
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String args[]) {
        boolean gui = true;
        if (args.length > 0) {
            if ("-nogui".equals(args[0])) gui = false;
            if ("-stopserver".equals(args[0])) {
                sendStop();
                System.exit(0);
            }
        }
        if (gui) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Throwable th) {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                } catch (Exception e) {
                }
            }
        }
        new SyncMain(gui);
    }

    /**
	 * Sends a stop signal to the running server.
	 */
    private static void sendStop() {
        try {
            File f = new File(new File("finchconfig").getAbsolutePath() + "/serversettings.xml");
            if (!f.exists()) {
                String msg = "Can't find configuration file '" + f.getAbsolutePath() + "'.";
                System.out.println(msg);
            }
            Document doc = XMLReader.readXMLDocument(f, null, false, null);
            if (doc == null) throw new Exception("Can't read config file!");
            Element root = doc.getDocumentElement();
            Element el_settings = XMLReader.findSubElement(root, "serversettings");
            HashMap<String, String> settings = new HashMap<String, String>();
            XMLReader.readChildsToMap(el_settings, settings, true);
            if (!"true".equals(settings.get(SyncMain.S_ENABLESTATUSPAGE))) {
                System.out.println("Server'S status page must be enabled to stop the server from the commandline!\nServer not stopped!");
                return;
            }
            String login = settings.get(SyncMain.S_ADMINLOGIN);
            String password = settings.get(SyncMain.S_ADMINPASSWORD);
            String port = settings.get(SyncMain.S_PORTHTTP);
            String u = "http://localhost:" + port + "/status?login=" + login + "&password=" + password + "&action=stopserver";
            URL url = new URL(u);
            HttpURLConnection hc = (HttpURLConnection) url.openConnection();
            hc.setUseCaches(false);
            if (hc.getResponseCode() == 200) System.out.println("Server stopped!");
        } catch (Exception e) {
            System.out.println("Error :" + e.getMessage());
        }
    }

    /**
	 * Write the server configuration to the specified file.
	 * 
	 * @param file
	 *            file to write the configuration to.
	 * @return true, if successful.
	 */
    public boolean writeConfig(File file) {
        DocumentBuilderFactory factory = null;
        Document doc = null;
        factory = DocumentBuilderFactory.newInstance();
        try {
            doc = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
        }
        Element root = doc.createElement("finchserver");
        root.setAttribute("version", SyncServlet.SOFTWARE_VERSION);
        doc.appendChild(root);
        Element parent = doc.createElement("serversettings");
        root.appendChild(parent);
        if (settings != null) XMLWriter.writeChildsFromMap(parent, settings);
        parent = doc.createElement("syncsources");
        root.appendChild(parent);
        if (ssc_list != null) for (SyncSourceConfig ssc : ssc_list) {
            Element el = doc.createElement("syncsourceconfig");
            parent.appendChild(el);
            XMLWriter.writeChildsFromMap(el, ssc.getConfig());
        }
        parent = doc.createElement("syncclients");
        root.appendChild(parent);
        if (client_list != null) for (SyncClientConfig scc : client_list) {
            Element el = doc.createElement("syncclientconfig");
            parent.appendChild(el);
            Element settings = doc.createElement("settings");
            el.appendChild(settings);
            XMLWriter.writeChildsFromMap(settings, scc.getConfig());
            List<Map<String, String>> cssd = scc.getSyncSourceList();
            for (Map<String, String> p : cssd) {
                Element sub = doc.createElement("syncsourcedescr");
                el.appendChild(sub);
                XMLWriter.writeChildsFromMap(sub, p);
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            XMLWriter.writeXmlFile(doc, fos, "iso-8859-1");
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
            }
        }
        return true;
    }

    /**
	 * Reads the server-configuration from the specified file.
	 * 
	 * @param f
	 *            file to read the configuration from.
	 * @return true, if configuration could be read.
	 */
    private boolean readConfig(File f) {
        try {
            Document doc = XMLReader.readXMLDocument(f, null, false, null);
            if (doc == null) return false;
            Element root = doc.getDocumentElement();
            Element el_settings = XMLReader.findSubElement(root, "serversettings");
            XMLReader.readChildsToMap(el_settings, settings, true);
            Element el = XMLReader.findSubElement(root, "syncsources");
            if (el != null) {
                Element sscnode = XMLReader.findSubElement(el, "syncsourceconfig");
                while (sscnode != null) {
                    Map<String, String> m = new Hashtable<String, String>();
                    XMLReader.readChildsToMap(sscnode, m, true);
                    try {
                        int id = Integer.parseInt(m.get("source_type_id"));
                        SyncSourceConfig ssc = SyncSourceConfigFactory.getSyncSourceConfig(id, m);
                        ssc_list.add(ssc);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Error reading SyncSource - configuration for '" + m.get("name") + "'");
                    }
                    sscnode = XMLReader.findNextElement(sscnode, "syncsourceconfig");
                }
            }
            el = XMLReader.findSubElement(root, "syncclients");
            if (el != null) {
                Element sccnode = XMLReader.findSubElement(el, "syncclientconfig");
                while (sccnode != null) {
                    Map<String, String> m = new Hashtable<String, String>();
                    XMLReader.readChildsToMap(XMLReader.findSubElement(sccnode, "settings"), m, true);
                    SyncClientConfig scc = new SyncClientConfig(new LinkedHashMap<String, String>(m));
                    Element cssd = XMLReader.findSubElement(sccnode, "syncsourcedescr");
                    while (cssd != null) {
                        m.clear();
                        XMLReader.readChildsToMap(cssd, m, true);
                        scc.addSyncSourceDescription(new LinkedHashMap<String, String>(m));
                        cssd = XMLReader.findNextElement(cssd, "syncsourcedescr");
                    }
                    client_list.add(scc);
                    sccnode = XMLReader.findNextElement(sccnode, "syncclientconfig");
                }
            }
        } catch (Exception e) {
            mylog.error("Error reading configuration!", e);
            return false;
        }
        return true;
    }

    public File getSyncInfoFile(SyncClientConfig scc) {
        if (scc == null) throw new IllegalArgumentException();
        return new File(rootpath + "/" + scc.getName() + ".sinfo");
    }
}
