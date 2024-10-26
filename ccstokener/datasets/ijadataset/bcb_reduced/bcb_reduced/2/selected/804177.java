package org.decomo.constants.eclipse.registry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import org.decomo.constants.eclipse.logging.PluginLogManager;
import org.decomo.constants.eclipse.registry.editors.ConstantsListEditor;
import org.decomo.constants.eclipse.registry.util.RegistryConstants;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    private ArrayList logManagers = new ArrayList();

    public ConstantsListEditor defaultDatatypeEditor;

    public static final String PLUGIN_ID = "org.decomo.constants.eclipse.registry";

    private static final String LOG_PROPERTIES_FILE = "logger.properties";

    private PluginLogManager logManager;

    private static Activator plugin;

    /**
	 * The constructor
	 */
    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        configure();
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
        if (this.logManager != null) {
            this.logManager.shutdown();
            this.logManager = null;
        }
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
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public File getDatatypesRegistryFilename() {
        IPath rootFolder = Activator.getDefault().getStateLocation();
        rootFolder = rootFolder.append(RegistryConstants.DATAREGISTRY_FILENAME);
        return rootFolder.toFile();
    }

    public File getDatatypesRegistry() {
        IPath rootFolder = Activator.getDefault().getStateLocation();
        rootFolder = rootFolder.append(RegistryConstants.DATAREGISTRY_FILENAME);
        return rootFolder.toFile();
    }

    public File createDatatypesRegistry() {
        IPath rootFolder = Activator.getDefault().getStateLocation();
        rootFolder = rootFolder.append(RegistryConstants.DATAREGISTRY_FILENAME);
        File registryFile = null;
        if (rootFolder.toFile().exists()) registryFile = rootFolder.toFile(); else {
            try {
                rootFolder.toFile().createNewFile();
                registryFile = rootFolder.toFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File created first time " + rootFolder.getFileExtension());
            IStatus status = new Status(IStatus.ERROR, getDefault().getBundle().getSymbolicName(), IStatus.ERROR, "Registry File created first time : " + rootFolder.getFileExtension(), new FileNotFoundException());
            getLog().log(status);
        }
        IStatus status = new Status(IStatus.INFO, getDefault().getBundle().getSymbolicName(), "Successfully located the registry file: " + rootFolder.getFileExtension());
        getLog().log(status);
        return registryFile;
    }

    public ConstantsListEditor getDefaultDatatypeEditor() {
        return defaultDatatypeEditor;
    }

    public void setDefaultDatatypeEditor(ConstantsListEditor defaultDatatypeEditor) {
        this.defaultDatatypeEditor = defaultDatatypeEditor;
    }

    public static PluginLogManager getLogManager() {
        return getDefault().logManager;
    }

    private void configure() {
        try {
            URL url = getBundle().getEntry("/" + LOG_PROPERTIES_FILE);
            InputStream propertiesInputStream = url.openStream();
            if (propertiesInputStream != null) {
                Properties props = new Properties();
                props.load(propertiesInputStream);
                propertiesInputStream.close();
                this.logManager = new PluginLogManager(this, props);
                this.logManager.hookPlugin(Activator.getDefault().getBundle().getSymbolicName(), Activator.getDefault().getLog());
            }
        } catch (Exception e) {
            String message = "Error while initializing log properties." + e.getMessage();
            IStatus status = new Status(IStatus.ERROR, getDefault().getBundle().getSymbolicName(), IStatus.ERROR, message, e);
            getLog().log(status);
            throw new RuntimeException("Error while initializing log properties.", e);
        }
    }

    /**
	 * Adds a log manager object to the list of active log managers
	 */
    public void addLogManager(PluginLogManager logManager) {
        synchronized (this.logManagers) {
            if (logManager != null) this.logManagers.add(logManager);
        }
    }

    /**
	 * Removes a log manager object from the list of active log managers
	 */
    public void removeLogManager(PluginLogManager logManager) {
        synchronized (this.logManagers) {
            if (logManager != null) this.logManagers.remove(logManager);
        }
    }
}
