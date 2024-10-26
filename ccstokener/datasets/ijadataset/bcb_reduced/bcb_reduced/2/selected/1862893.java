package org.coos.messaging;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import org.coos.util.macro.MacroSubstituteReader;
import org.coos.messaging.COContainer;
import org.coos.messaging.COOS;
import org.coos.messaging.COOSFactory;
import org.coos.messaging.Channel;
import org.coos.messaging.ChannelServer;
import org.coos.messaging.Plugin;
import org.coos.messaging.PluginFactory;
import org.coos.messaging.impl.DefaultChannel;
import org.coos.messaging.plugin.PluginChannel;
import org.coos.messaging.transport.JvmTransport;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Knut Eilif Husa, Tellu AS
 */
public class COOSActivator implements BundleActivator, COContainer {

    private static final String ARGUMENT_CONFIG_DIR = "configDir";

    private static final String COOS_CONFIG_FILE = "/coos.xml";

    private static final String COOS_CONFIG_PATH = "/org/coos/config";

    private static final String COOS_CONFIG_DIR = "./coosConfig";

    private BundleContext bc;

    private COOS coos;

    private String configDir;

    private ArrayList<Plugin> plugins = new ArrayList<Plugin>();

    private static final Log logger = LogFactory.getLog(COOSActivator.class);

    public void start(BundleContext bundleContext) throws Exception {
        configDir = System.getProperty(ARGUMENT_CONFIG_DIR, COOS_CONFIG_DIR);
        logger.info("Coos OSGI starting");
        this.bc = bundleContext;
        InputStream is = null;
        try {
            is = new FileInputStream(configDir + COOS_CONFIG_FILE);
            is = substitute(is);
        } catch (Exception e) {
        }
        if (is != null) {
            logger.info("Using provided coos config");
        } else {
            URL url = bc.getBundle().getResource(COOS_CONFIG_PATH + COOS_CONFIG_FILE);
            if (url == null) {
                logger.warn("This coos bundle has no Coos configuration!");
                return;
            }
            is = url.openStream();
            is = substitute(is);
            logger.info("Using included coos config");
        }
        coos = COOSFactory.createCOOS(is);
        coos.start();
        ChannelServer channelServer = coos.getChannelServer("default");
        bc.registerService(ChannelServer.class.getName(), channelServer, new Hashtable());
        logger.info("ChannelServer registered");
        Enumeration<URL> urls = bc.getBundle().findEntries(COOS_CONFIG_PATH, "plugin*.xml", false);
        if (urls != null && urls.hasMoreElements()) {
            logger.info("Starting included plugins");
        } else {
            return;
        }
        while (urls.hasMoreElements()) {
            URL url = (URL) urls.nextElement();
            is = null;
            String fileName = null;
            try {
                fileName = url.getFile();
                fileName = fileName.substring(fileName.lastIndexOf("/"));
                logger.info("Looking for plugin configurations in: " + configDir + fileName);
                is = new FileInputStream(configDir + fileName);
                is = substitute(is);
            } catch (Exception e) {
            }
            if (is == null) {
                is = url.openStream();
                is = substitute(is);
                logger.info("Starting plugins defined in: " + url.getFile());
            } else {
                logger.info("Starting plugins defined in: " + configDir + fileName);
            }
            Plugin[] plg = PluginFactory.createPlugins(is, this);
            for (int i = 0; i < plg.length; i++) {
                Plugin plugin = plg[i];
                for (Iterator iterator = plugin.getChannels().iterator(); iterator.hasNext(); ) {
                    Channel channel = (Channel) iterator.next();
                    if (channel instanceof PluginChannel && ((DefaultChannel) channel).getTransport() instanceof JvmTransport) {
                        ((JvmTransport) ((DefaultChannel) channel).getTransport()).setChannelServer(channelServer);
                    }
                }
                plugin.connect();
            }
            for (int i = 0; i < plg.length; i++) {
                plugins.add(plg[i]);
            }
        }
    }

    private InputStream substitute(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        MacroSubstituteReader msr = new MacroSubstituteReader(isr);
        String substituted = msr.substituteMacros();
        is = new ByteArrayInputStream(substituted.getBytes());
        return is;
    }

    public void stop(BundleContext bundleContext) throws Exception {
        if (coos != null) {
            coos.stop();
            logger.info("Coos OSGI stopped");
        }
        for (Plugin plugin : plugins) {
            plugin.disconnect();
        }
        logger.info("Plugin OSGI stopped");
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        try {
            return bc.getBundle().loadClass(className);
        } catch (IllegalStateException e) {
            return Class.forName(className);
        }
    }

    public InputStream getResource(String resourceName) throws IOException {
        if (!resourceName.startsWith("/")) {
            resourceName += "/";
        }
        URL url = bc.getBundle().getResource(COOS_CONFIG_PATH + resourceName);
        InputStream is = null;
        try {
            FileInputStream fis = new FileInputStream(configDir + resourceName);
            is = substitute(fis);
        } catch (Exception e) {
        }
        if (is == null) {
            is = url.openStream();
            is = substitute(is);
        }
        return is;
    }

    public Object getObject(String name) {
        if (name.equals(BUNDLE_CONTEXT)) {
            return bc;
        }
        return null;
    }
}
