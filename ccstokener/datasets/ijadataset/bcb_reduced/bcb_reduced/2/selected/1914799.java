package org.skyfree.ghyll.tcard;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import com.tools.logging.PluginLogManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.skyfree.ghyll.tcard";

    private static Activator plugin;

    private static final String LOG_PROPERTIES_FILE = "logger.properties";

    private PluginLogManager logManager = null;

    /**
	 * The constructor
	 */
    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        this.configure();
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
    public static Activator getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    private void configure() {
        try {
            URL url = getBundle().getEntry("/" + LOG_PROPERTIES_FILE);
            InputStream propertiesInputStream = url.openStream();
            if (propertiesInputStream != null) {
                Properties props = new Properties();
                props.load(propertiesInputStream);
                propertiesInputStream.close();
                this.logManager = new PluginLogManager(this.getBundle(), props);
            }
        } catch (Exception e) {
            String message = "Error while initializing log properties." + e.getMessage();
            IStatus status = new Status(IStatus.ERROR, getDefault().getBundle().getSymbolicName(), IStatus.ERROR, message, e);
            getLog().log(status);
            throw new RuntimeException("Error while initializing log properties.", e);
        }
    }

    public static PluginLogManager getLogManager() {
        return plugin.logManager;
    }
}
