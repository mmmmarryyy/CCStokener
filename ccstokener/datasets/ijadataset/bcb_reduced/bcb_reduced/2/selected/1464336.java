package org.eclipse.core.internal.preferences;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.runtime.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * @since 3.0
 */
public class DefaultPreferences extends EclipsePreferences {

    private static Set loadedNodes = new HashSet();

    private static final String ELEMENT_INITIALIZER = "initializer";

    private static final String ATTRIBUTE_CLASS = "class";

    private static final String KEY_PREFIX = "%";

    private static final String KEY_DOUBLE_PREFIX = "%%";

    private static final IPath NL_DIR = new Path("$nl$");

    public static final String PRODUCT_KEY = "preferenceCustomization";

    private static final String LEGACY_PRODUCT_CUSTOMIZATION_FILENAME = "plugin_customization.ini";

    private static final String PROPERTIES_FILE_EXTENSION = "properties";

    private static Properties productCustomization;

    private static Properties productTranslation;

    private static Properties commandLineCustomization;

    private EclipsePreferences loadLevel;

    private String qualifier;

    private int segmentCount;

    private Plugin plugin;

    /**
	 * Default constructor for this class.
	 */
    public DefaultPreferences() {
        this(null, null);
    }

    private DefaultPreferences(EclipsePreferences parent, String name, Plugin context) {
        this(parent, name);
        this.plugin = context;
    }

    private DefaultPreferences(EclipsePreferences parent, String name) {
        super(parent, name);
        if (parent instanceof DefaultPreferences) this.plugin = ((DefaultPreferences) parent).plugin;
        String path = absolutePath();
        segmentCount = getSegmentCount(path);
        if (segmentCount < 2) return;
        qualifier = getSegment(path, 1);
    }

    private void applyBundleDefaults() {
        Bundle bundle = Platform.getBundle(name());
        if (bundle == null) return;
        URL url = Platform.find(bundle, new Path(Plugin.PREFERENCES_DEFAULT_OVERRIDE_FILE_NAME));
        if (url == null) {
            if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Preference default override file not found for bundle: " + bundle.getSymbolicName());
            return;
        }
        URL transURL = Platform.find(bundle, NL_DIR.append(Plugin.PREFERENCES_DEFAULT_OVERRIDE_BASE_NAME).addFileExtension(PROPERTIES_FILE_EXTENSION));
        if (transURL == null && InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Preference translation file not found for bundle: " + bundle.getSymbolicName());
        applyDefaults(name(), loadProperties(url), loadProperties(transURL));
    }

    private void applyCommandLineDefaults() {
        if (commandLineCustomization == null) {
            String filename = InternalPlatform.pluginCustomizationFile;
            if (filename == null) {
                if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Command-line preferences customization file not specified.");
                return;
            }
            if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Using command-line preference customization file: " + filename);
            commandLineCustomization = loadProperties(filename);
        }
        applyDefaults(null, commandLineCustomization, null);
    }

    private void applyDefaults(String id, Properties defaultValues, Properties translations) {
        for (Enumeration e = defaultValues.keys(); e.hasMoreElements(); ) {
            String fullKey = (String) e.nextElement();
            String value = defaultValues.getProperty(fullKey);
            if (value == null) continue;
            IPath childPath = new Path(fullKey);
            String key = childPath.lastSegment();
            childPath = childPath.removeLastSegments(1);
            String localQualifier = id;
            if (id == null) {
                localQualifier = childPath.segment(0);
                childPath = childPath.removeFirstSegments(1);
            }
            if (name().equals(localQualifier)) {
                value = translatePreference(value, translations);
                if (InternalPlatform.DEBUG_PREFERENCE_SET) Policy.debug("Setting default preference: " + (new Path(absolutePath()).append(childPath).append(key)) + '=' + value);
                ((EclipsePreferences) internalNode(childPath.toString(), false, null)).internalPut(key, value);
            }
        }
    }

    private void runInitializer(IConfigurationElement element) {
        AbstractPreferenceInitializer initializer = null;
        try {
            initializer = (AbstractPreferenceInitializer) element.createExecutableExtension(ATTRIBUTE_CLASS);
            initializer.initializeDefaultPreferences();
        } catch (ClassCastException e) {
            IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, IStatus.ERROR, Messages.preferences_invalidExtensionSuperclass, e);
            log(status);
        } catch (CoreException e) {
            log(e.getStatus());
        }
    }

    public IEclipsePreferences node(String childName, Plugin context) {
        return internalNode(childName, true, context);
    }

    private void applyRuntimeDefaults() {
        IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(Platform.PI_RUNTIME, Platform.PT_PREFERENCES);
        if (point == null) {
            if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("No extensions for " + Platform.PI_RUNTIME + '.' + Platform.PT_PREFERENCES + " extension point. Skipping runtime default preference customization.");
            return;
        }
        IExtension[] extensions = point.getExtensions();
        boolean foundInitializer = false;
        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0; j < elements.length; j++) if (ELEMENT_INITIALIZER.equals(elements[j].getName())) {
                if (name().equals(elements[j].getNamespace())) {
                    if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Running default preference customization as defined by: " + elements[j].getDeclaringExtension().getDeclaringPluginDescriptor());
                    runInitializer(elements[j]);
                    foundInitializer = true;
                }
            }
        }
        if (foundInitializer) return;
        if (plugin == null && InternalPlatform.getDefault().getBundle(CompatibilityHelper.PI_RUNTIME_COMPATIBILITY) != null) plugin = Platform.getPlugin(name());
        if (plugin == null) {
            if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("No plug-in object available to set plug-in default preference overrides for:" + name());
            return;
        }
        if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Applying plug-in default preference overrides for plug-in: " + plugin.getDescriptor().getUniqueIdentifier());
        plugin.internalInitializeDefaultPluginPreferences();
    }

    private void applyProductDefaults() {
        if (productCustomization == null) {
            IProduct product = Platform.getProduct();
            if (product == null) {
                if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Product not available to set product default preference overrides.");
                return;
            }
            String id = product.getId();
            if (id == null) {
                if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Product ID not available to apply product-level preference defaults.");
                return;
            }
            Bundle bundle = product.getDefiningBundle();
            if (bundle == null) {
                if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Bundle not available to apply product-level preference defaults for product id: " + id);
                return;
            }
            String value = product.getProperty(PRODUCT_KEY);
            URL url = null;
            URL transURL = null;
            if (value == null) {
                if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Product : " + id + " does not define preference customization file. Using legacy file: plugin_customization.ini");
                value = LEGACY_PRODUCT_CUSTOMIZATION_FILENAME;
                url = Platform.find(bundle, new Path(LEGACY_PRODUCT_CUSTOMIZATION_FILENAME));
                transURL = Platform.find(bundle, NL_DIR.append(value).removeFileExtension().addFileExtension(PROPERTIES_FILE_EXTENSION));
            } else {
                try {
                    url = new URL(value);
                } catch (MalformedURLException e) {
                    url = Platform.find(bundle, new Path(value));
                    if (url != null) transURL = Platform.find(bundle, NL_DIR.append(value).removeFileExtension().addFileExtension(PROPERTIES_FILE_EXTENSION));
                }
            }
            if (url == null) {
                if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Product preference customization file: " + value + " not found for bundle: " + id);
                return;
            }
            if (transURL == null && InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("No preference translations found for product/file: " + bundle.getSymbolicName() + '/' + value);
            productCustomization = loadProperties(url);
            productTranslation = loadProperties(transURL);
        }
        applyDefaults(null, productCustomization, productTranslation);
    }

    public void flush() {
    }

    protected IEclipsePreferences getLoadLevel() {
        if (loadLevel == null) {
            if (qualifier == null) return null;
            EclipsePreferences node = this;
            for (int i = 2; i < segmentCount; i++) node = (EclipsePreferences) node.parent();
            loadLevel = node;
        }
        return loadLevel;
    }

    protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Plugin context) {
        return new DefaultPreferences(nodeParent, nodeName, context);
    }

    protected boolean isAlreadyLoaded(IEclipsePreferences node) {
        return loadedNodes.contains(node.name());
    }

    protected void load() {
        loadDefaults();
    }

    private void loadDefaults() {
        applyRuntimeDefaults();
        applyBundleDefaults();
        applyProductDefaults();
        applyCommandLineDefaults();
    }

    private Properties loadProperties(URL url) {
        Properties result = new Properties();
        if (url == null) return result;
        InputStream input = null;
        try {
            input = url.openStream();
            result.load(input);
        } catch (IOException e) {
            if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) {
                Policy.debug("Problem opening stream to preference customization file: " + url);
                e.printStackTrace();
            }
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    private Properties loadProperties(String filename) {
        Properties result = new Properties();
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(filename));
            result.load(input);
        } catch (FileNotFoundException e) {
            if (InternalPlatform.DEBUG_PREFERENCE_GENERAL) Policy.debug("Preference customization file not found: " + filename);
        } catch (IOException e) {
            String message = NLS.bind(Messages.preferences_loadException, filename);
            IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, IStatus.ERROR, message, e);
            InternalPlatform.getDefault().log(status);
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    protected void loaded() {
        loadedNodes.add(name());
    }

    public void sync() {
    }

    /**
	 * Takes a preference value and a related resource bundle and
	 * returns the translated version of this value (if one exists).
	 */
    private String translatePreference(String value, Properties props) {
        value = value.trim();
        if (props == null || value.startsWith(KEY_DOUBLE_PREFIX)) return value;
        if (value.startsWith(KEY_PREFIX)) {
            int ix = value.indexOf(" ");
            String key = ix == -1 ? value.substring(1) : value.substring(1, ix);
            String dflt = ix == -1 ? value : value.substring(ix + 1);
            return props.getProperty(key, dflt);
        }
        return value;
    }
}
