package org.xaware.tracer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;
import org.xaware.api.XABizView;
import org.xaware.ide.shared.BuildXAwareHomeDesigner;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.tracer.util.IconFactory;

/**
 * The main plugin class to be used in the desktop.
 */
public class TracerPlugin extends AbstractUIPlugin {

    public static final String ID = "org.xaware.tracer";

    private static TracerPlugin plugin;

    /** Context ClassLoader for this thread prior to starting plugin */
    private static ClassLoader previousClassLoader = null;

    private IEclipsePreferences configPrefs;

    /** xaware.home string */
    private final String xahomeStr = "xaware.home";

    /**
     * The constructor.
     */
    public TracerPlugin() {
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        org.xaware.shared.util.logging.LogConfigUtil.getInstance().setupGlobalLog();
        XABizView.initialize(XABizView.MODE_DESIGNER);
        UserPrefs.initSystemProps();
        setupXAwareHome();
        final Preferences configPrefs = getConfigPrefs();
        final String configKey = "myKey";
        configPrefs.put(configKey, "foo");
        configPrefs.flush();
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        IconFactory.dispose();
        if (configPrefs != null) {
            configPrefs.flush();
            configPrefs = null;
        }
        plugin = null;
    }

    /**
     * Returns the shared instance.
     */
    public static TracerPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(final String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("org.xaware.tracer", path);
    }

    /**
     * Answer the configuration directory for this plug-in that is shared by all workspaces of this installation.
     */
    public File getConfigDir() {
        final Location location = Platform.getConfigurationLocation();
        if (location != null) {
            final URL configURL = location.getURL();
            if (configURL != null && configURL.getProtocol().startsWith("file")) {
                return new File(configURL.getFile(), ID);
            }
        }
        return getStateLocation().toFile();
    }

    /**
     * Answer the configuration preferences for this plug-in that are shared by all workspaces of this installation
     */
    public Preferences getConfigPrefs() {
        if (configPrefs == null) {
            configPrefs = new ConfigurationScope().getNode(ID);
        }
        return configPrefs;
    }

    public static boolean isEarlyStartupDisabled() {
        final String plugins = PlatformUI.getPreferenceStore().getString("PLUGINS_NOT_ACTIVATED_ON_STARTUP");
        return plugins.indexOf(TracerPlugin.ID) != -1;
    }

    /**
     * check preference store for xaware.home if not found allow the user to set it. then set the system property
     * xaware.home
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void setupXAwareHome() throws FileNotFoundException, IOException {
        String xawareHomeDir = System.getProperty(xahomeStr);
        if (xawareHomeDir == null || xawareHomeDir.length() <= 0) {
            final ScopedPreferenceStore prefStore = (ScopedPreferenceStore) XA_Designer_Plugin.getDefault().getPreferenceStore();
            xawareHomeDir = prefStore.getString(xahomeStr);
            if (xawareHomeDir == null || xawareHomeDir.length() <= 0) {
                xawareHomeDir = getXAwareHome();
                final boolean overwrite = false;
                installDesignerHome(xawareHomeDir, overwrite);
                prefStore.setValue(xahomeStr, xawareHomeDir);
                System.out.println("set " + xahomeStr + " = " + xawareHomeDir);
                prefStore.save();
            }
            System.setProperty(xahomeStr, xawareHomeDir);
        }
    }

    /**
     * Opens the dialog for finding a directory for XAwareHome
     */
    protected String getXAwareHome() {
        final DirectoryDialog dialog = new DirectoryDialog(XA_Designer_Plugin.getShell());
        dialog.setText("Browse to select XAware Tracer home");
        dialog.setMessage("XAware Tracer home not found. Select a directory to assign the Tracer home now. ");
        final String dirName = Platform.getInstallLocation().getURL().getPath();
        ;
        dialog.setFilterPath(new Path(dirName).toOSString());
        return dialog.open();
    }

    /**
     * Use designerhome.jar to build the directories required by designer and extract the jars from the plugin required
     * for external scripts to run.
     */
    private void installDesignerHome(final String xawareHome, final boolean overwrite) {
        try {
            final String symbolicName = "org.xaware.designer";
            final Bundle bundle = Platform.getBundle(symbolicName);
            final URL url = bundle.getEntry("/home/designerhome.jar");
            final BuildXAwareHomeDesigner xaHomeBuilder = new BuildXAwareHomeDesigner();
            xaHomeBuilder.buildHome(xawareHome, url.openStream(), overwrite);
            xaHomeBuilder.extractJarsFromResources(xawareHome);
        } catch (final Exception e) {
            System.out.println("FAILED TO INSTALL DESIGNER HOME: " + e);
        }
    }
}
