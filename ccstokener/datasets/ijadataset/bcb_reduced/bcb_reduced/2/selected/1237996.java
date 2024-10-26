package net.sf.epfe.core.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.LogManager;
import net.sf.epfe.core.preferences.IEPFEPreferences2;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EPFECoreUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "net.sf.epfe.core.ui";

    private static final String LOG_PROPERTIES_FILE = "logging.properties";

    private static EPFECoreUIActivator plugin;

    private ColorRegistry fColorRegistry;

    private IPreferenceStore preferenceStore;

    private ResourceBundle resourceBundle;

    public static IStatus createStatus(int aSeverity, String aMessage) {
        return new Status(aSeverity, PLUGIN_ID, aMessage);
    }

    public static IStatus createStatus(int aSeverity, String aMessage, Throwable aEx) {
        return new Status(aSeverity, PLUGIN_ID, aMessage, aEx);
    }

    public static BundleContext getBundleContext() {
        return getDefault().getBundle().getBundleContext();
    }

    public static ColorRegistry getColorRegistry() {
        if (getDefault().fColorRegistry == null) {
            getDefault().fColorRegistry = new ColorRegistry();
        }
        return getDefault().fColorRegistry;
    }

    /**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
    public static EPFECoreUIActivator getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *          the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 */
    public static String getResourceString(String key) {
        ResourceBundle bundle = EPFECoreUIActivator.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public static void log(CoreException aEx) {
        getDefault().getLog().log(aEx.getStatus());
    }

    public static void log(int aSeverity, String aString, Throwable aEx) {
        getDefault().getLog().log(new Status(aSeverity, PLUGIN_ID, aString, aEx));
    }

    /**
	 * The constructor
	 */
    public EPFECoreUIActivator() {
    }

    @Override
    public IPreferenceStore getPreferenceStore() {
        if (preferenceStore == null) {
            preferenceStore = new ScopedPreferenceStore(IEPFEPreferences2.INSTANCE.getScope(), IEPFEPreferences2.INSTANCE.getScopeQualifier());
        }
        return preferenceStore;
    }

    /**
	 * Returns the plugin's resource bundle,
	 */
    public ResourceBundle getResourceBundle() {
        try {
            if (resourceBundle == null) resourceBundle = ResourceBundle.getBundle("com.ibm.zrh.eclipse.nls.NlsPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        return resourceBundle;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        try {
            URL url = getBundle().getEntry("/" + LOG_PROPERTIES_FILE);
            if (url != null) {
                InputStream propertiesInputStream = url.openStream();
                LogManager.getLogManager().readConfiguration(propertiesInputStream);
                propertiesInputStream.close();
            }
        } catch (IOException lEx) {
            log(IStatus.WARNING, "Couldn't load a resource file " + LOG_PROPERTIES_FILE + ".", lEx);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        resourceBundle = null;
        super.stop(context);
    }
}
