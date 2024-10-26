package org.radrails.rails.core.railsplugins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.radrails.rails.core.RailsLog;
import org.radrails.rails.internal.core.RailsPlugin;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Singleton class that manages access to a list of available Rails plugins. The
 * list is cached in the rails.ui plug-in state location, and can be refreshed
 * on-demand from the Rails Plugins web service.
 * 
 * @author mkent
 * 
 */
public class RailsPluginsManager {

    public class RailsPluginException extends Exception {

        public RailsPluginException(Exception e) {
            super(e);
        }
    }

    private static final String SERVICE_URL = "http://agilewebdevelopment.com/plugins/script_list";

    private static final String PLUGINS_FILE = "rails_plugins.xml";

    private static RailsPluginsManager fInstance;

    private List fPlugins;

    private RailsPluginsManager() {
    }

    public static RailsPluginsManager getInstance() {
        if (fInstance == null) {
            fInstance = new RailsPluginsManager();
        }
        return fInstance;
    }

    /**
	 * Gets a list of currently available Rails plugins. The list contains Maps,
	 * each of which holds information about a plugin in String to String
	 * mappings.
	 * 
	 * @return a list of plugins
	 * @throws Exception
	 */
    public List getPlugins() throws RailsPluginException {
        if (fPlugins == null) {
            fPlugins = loadPlugins();
            if (fPlugins.isEmpty()) {
                fPlugins = updatePlugins(new NullProgressMonitor());
            }
        }
        return fPlugins;
    }

    /**
	 * Updates the current list of Rails plugins from the web service.
	 * @throws RailsPluginException 
	 * 
	 * @throws Exception
	 *             if an error occurs
	 */
    public List updatePlugins(IProgressMonitor monitor) throws RailsPluginException {
        List plugins = new ArrayList();
        monitor.beginTask("Updating plugin list", 3);
        monitor.subTask("Accessing plugin directory");
        HttpURLConnection conn;
        try {
            conn = getConnection();
        } catch (MalformedURLException e1) {
            throw new RailsPluginException(e1);
        } catch (ProtocolException e1) {
            throw new RailsPluginException(e1);
        } catch (IOException e1) {
            throw new RailsPluginException(e1);
        }
        monitor.worked(1);
        monitor.subTask("Downloading plugin list");
        String pxml = null;
        try {
            pxml = getPluginXMLFeed(monitor, conn);
        } catch (IOException e) {
            monitor.subTask("Error downloading plugin list.  See log for details.");
            RailsLog.log(e);
            monitor.done();
            return plugins;
        }
        monitor.worked(1);
        monitor.subTask("Processing plugin information");
        StringReader strIn = new StringReader(pxml);
        try {
            plugins = parsePluginsXML(strIn);
        } catch (SAXException e) {
            throw new RailsPluginException(e);
        } catch (ParserConfigurationException e) {
            throw new RailsPluginException(e);
        } catch (IOException e) {
            throw new RailsPluginException(e);
        }
        writeOutPluginsXML(pxml);
        monitor.worked(1);
        monitor.done();
        return plugins;
    }

    private HttpURLConnection getConnection() throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(SERVICE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return conn;
    }

    private String getPluginXMLFeed(IProgressMonitor monitor, HttpURLConnection conn) throws IOException {
        BufferedReader bufIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer pxml = new StringBuffer();
        String l = null;
        while ((l = bufIn.readLine()) != null) {
            pxml.append(l);
        }
        return pxml.toString();
    }

    private void writeOutPluginsXML(String pxml) {
        PrintWriter out = null;
        try {
            File f = new File(getLocalPluginXMLCache().toOSString());
            out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
            out.write(pxml);
            out.flush();
        } catch (IllegalStateException e) {
            RailsLog.log(e);
        } catch (IOException e) {
            RailsLog.log(e);
        } finally {
            if (out != null) out.close();
        }
    }

    /**
	 * Loads the list of plugins from the state file.
	 */
    private List loadPlugins() {
        File f = new File(getLocalPluginXMLCache().toOSString());
        if (f.exists()) {
            try {
                FileReader fis = new FileReader(f);
                return parsePluginsXML(fis);
            } catch (SAXException e) {
                RailsLog.logError("Error parsing Rails plugins XML", e);
            } catch (ParserConfigurationException e) {
                RailsLog.logError("Error parsing Rails plugins XML: parser misconfigured.", e);
            } catch (IOException e) {
                RailsLog.logError("Error parsing Rails plugins XML: I/O problems.", e);
            }
        }
        return new ArrayList();
    }

    private IPath getLocalPluginXMLCache() {
        return RailsPlugin.getInstance().getStateLocation().append(PLUGINS_FILE);
    }

    /**
	 * Parses an XML file describing a list of Rails plugins and returns a List
	 * of Map objects, containing the plugin attributes mapped to their
	 * corresponding values.
	 * 
	 * @param rdr
	 *            the XML source
	 * @return a list of plugin Maps
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
    private List parsePluginsXML(Reader rdr) throws SAXException, ParserConfigurationException, IOException {
        XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        RailsPluginsContentHandler handler = new RailsPluginsContentHandler();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(rdr));
        return handler.getRailsPlugins();
    }
}
