package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public class EclipseBundleData extends AbstractBundleData {

    /** bundle manifest type unknown */
    public static final byte MANIFEST_TYPE_UNKNOWN = 0x00;

    /** bundle manifest type bundle (META-INF/MANIFEST.MF) */
    public static final byte MANIFEST_TYPE_BUNDLE = 0x01;

    /** bundle manifest type plugin (plugin.xml) */
    public static final byte MANIFEST_TYPE_PLUGIN = 0x02;

    /** bundle manifest type fragment (fragment.xml) */
    public static final byte MANIFEST_TYPE_FRAGMENT = 0x04;

    /** bundle manifest type jared bundle */
    public static final byte MANIFEST_TYPE_JAR = 0x08;

    private static String[] libraryVariants = null;

    /** data to detect modification made in the manifest */
    private long manifestTimeStamp = 0;

    private byte manifestType = MANIFEST_TYPE_UNKNOWN;

    /** The platform protocol */
    public static final String PROTOCOL = "platform";

    /** The file protocol */
    public static final String FILE = "file";

    private static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration";

    /** the Plugin-Class header */
    protected String pluginClass = null;

    /**  Eclipse-AutoStart header */
    private boolean autoStart;

    private String[] autoStartExceptions;

    /** shortcut to know if a bundle has a buddy */
    protected String buddyList;

    /** shortcut to know if a bundle is a registrant to a registered policy */
    protected String registeredBuddyList;

    /** shortcut to know if the bundle manifest has package info */
    protected boolean hasPackageInfo;

    private static String[] buildLibraryVariants() {
        ArrayList result = new ArrayList();
        EclipseEnvironmentInfo info = EclipseEnvironmentInfo.getDefault();
        result.add("ws/" + info.getWS() + "/");
        result.add("os/" + info.getOS() + "/" + info.getOSArch() + "/");
        result.add("os/" + info.getOS() + "/");
        String nl = info.getNL();
        nl = nl.replace('_', '/');
        while (nl.length() > 0) {
            result.add("nl/" + nl + "/");
            int i = nl.lastIndexOf('/');
            nl = (i < 0) ? "" : nl.substring(0, i);
        }
        result.add("");
        return (String[]) result.toArray(new String[result.size()]);
    }

    /**
	 * Constructor for EclipseBundleData.
	 * @param adaptor The adaptor for this bundle data
	 * @param id The bundle id for this bundle data
	 */
    public EclipseBundleData(AbstractFrameworkAdaptor adaptor, long id) {
        super(adaptor, id);
    }

    /**
	 * Initialize an existing bundle
	 * @throws IOException if any error occurs loading the existing bundle
	 */
    public void initializeExistingBundle() throws IOException {
        createBaseBundleFile();
        if (!checkManifestTimeStamp()) {
            if (getBundleStoreDir().exists()) {
                FileOutputStream out = new FileOutputStream(new File(getBundleStoreDir(), ".delete"));
                out.close();
            }
            throw new IOException();
        }
    }

    private boolean checkManifestTimeStamp() {
        if (!"true".equalsIgnoreCase(System.getProperty(PROP_CHECK_CONFIG))) return true;
        if (PluginConverterImpl.getTimeStamp(getBaseFile(), getManifestType()) == getManifestTimeStamp()) {
            if ((getManifestType() & (MANIFEST_TYPE_JAR | MANIFEST_TYPE_BUNDLE)) != 0) return true;
            String cacheLocation = System.getProperty(LocationManager.PROP_MANIFEST_CACHE);
            Location parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation();
            if (parentConfiguration != null) {
                try {
                    return checkManifestAndParent(cacheLocation, getSymbolicName(), getVersion().toString(), getManifestType()) != null;
                } catch (BundleException e) {
                    return false;
                }
            }
            File cacheFile = new File(cacheLocation, getSymbolicName() + '_' + getVersion() + ".MF");
            if (cacheFile.isFile()) return true;
        }
        return false;
    }

    /**
	 * Returns the absolute path name of a native library. The VM invokes this
	 * method to locate the native libraries that belong to classes loaded with
	 * this class loader. If this method returns <code>null</code>, the VM
	 * searches the library along the path specified as the <code>java.library.path</code>
	 * property.
	 * 
	 * @param libName
	 *                   the library name
	 * @return the absolute path of the native library
	 */
    public String findLibrary(String libName) {
        String result = super.findLibrary(libName);
        if (result != null) return result;
        if (libraryVariants == null) libraryVariants = buildLibraryVariants();
        if (libName.length() == 0) return null;
        if (libName.charAt(0) == '/' || libName.charAt(0) == '\\') libName = libName.substring(1);
        libName = System.mapLibraryName(libName);
        return searchVariants(libraryVariants, libName);
    }

    private String searchVariants(String[] variants, String path) {
        for (int i = 0; i < variants.length; i++) {
            BundleEntry libEntry = baseBundleFile.getEntry(variants[i] + path);
            if (libEntry == null) {
            } else {
                File libFile = baseBundleFile.getFile(variants[i] + path);
                if (libFile == null) return null;
                if (org.eclipse.osgi.service.environment.Constants.OS_HPUX.equals(EclipseEnvironmentInfo.getDefault().getOS())) {
                    try {
                        Runtime.getRuntime().exec(new String[] { "chmod", "755", libFile.getAbsolutePath() }).waitFor();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return libFile.getAbsolutePath();
            }
        }
        return null;
    }

    private URL[] getSearchURLs(URL target) {
        return new URL[] { target };
    }

    public synchronized Dictionary getManifest() throws BundleException {
        return getManifest(false);
    }

    /**
	 * Gets the manifest for this bundle.  If this is the first time the manifest is
	 * ever accessed then the manifest is loaded from the manifest file in the bundle;
	 * otherwise the manifest is loaded from cached data.
	 * @param first set to true if this is the first time the manifest is accessed 
	 * for this bundle
	 * @return the manifest for this bundle
	 * @throws BundleException if any error occurs loading the manifest
	 */
    public synchronized Dictionary getManifest(boolean first) throws BundleException {
        if (manifest == null) manifest = first ? loadManifest() : new CachedManifest(this);
        return manifest;
    }

    private boolean isComplete(Dictionary manifest) {
        if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) != null) return true;
        return getEntry(PluginConverterImpl.PLUGIN_MANIFEST) == null && getEntry(PluginConverterImpl.FRAGMENT_MANIFEST) == null;
    }

    /**
	 * Loads the bundle manifest from the bundle.
	 * @return the bundle manifest
	 * @throws BundleException if an error occurs loading the bundle manifest
	 */
    public synchronized Dictionary loadManifest() throws BundleException {
        URL url = getEntry(Constants.OSGI_BUNDLE_MANIFEST);
        if (url != null) {
            Dictionary builtIn = loadManifestFrom(url);
            if (!isComplete(builtIn)) {
                Dictionary generatedManifest = generateManifest(builtIn);
                if (generatedManifest != null) return generatedManifest;
            }
            manifestType = MANIFEST_TYPE_BUNDLE;
            if (getBaseFile().isFile()) {
                manifestTimeStamp = getBaseFile().lastModified();
                manifestType |= MANIFEST_TYPE_JAR;
            } else manifestTimeStamp = getBaseBundleFile().getEntry(Constants.OSGI_BUNDLE_MANIFEST).getTime();
            return builtIn;
        }
        Dictionary result = generateManifest(null);
        if (result == null) throw new BundleException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_DATA_MANIFEST_NOT_FOUND, getLocation()));
        return result;
    }

    private Headers basicCheckManifest(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
        File currentFile = new File(cacheLocation, symbolicName + '_' + version + ".MF");
        if (PluginConverterImpl.upToDate(currentFile, getBaseFile(), inputType)) {
            try {
                return Headers.parseManifest(new FileInputStream(currentFile));
            } catch (FileNotFoundException e) {
            }
        }
        return null;
    }

    private Headers checkManifestAndParent(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
        Headers result = basicCheckManifest(cacheLocation, symbolicName, version, inputType);
        if (result != null) return result;
        Location parentConfiguration = null;
        if ((parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation()) != null) {
            result = basicCheckManifest(new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + '/' + LocationManager.MANIFESTS_DIR).toString(), symbolicName, version, inputType);
        }
        return result;
    }

    private Dictionary generateManifest(Dictionary originalManifest) throws BundleException {
        String cacheLocation = System.getProperty(LocationManager.PROP_MANIFEST_CACHE);
        if (getSymbolicName() != null) {
            Headers existingHeaders = checkManifestAndParent(cacheLocation, getSymbolicName(), getVersion().toString(), manifestType);
            if (existingHeaders != null) return existingHeaders;
        }
        PluginConverterImpl converter = PluginConverterImpl.getDefault();
        Dictionary generatedManifest;
        try {
            generatedManifest = converter.convertManifest(getBaseFile(), true, null, true, null);
        } catch (PluginConversionException pce) {
            String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_ERROR_CONVERTING, getBaseFile());
            throw new BundleException(message, pce);
        }
        Version version = Version.parseVersion((String) generatedManifest.get(Constants.BUNDLE_VERSION));
        String symbolicName = ManifestElement.parseHeader(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, (String) generatedManifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME))[0].getValue();
        ManifestElement generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, (String) generatedManifest.get(PluginConverterImpl.GENERATED_FROM))[0];
        Headers existingHeaders = checkManifestAndParent(cacheLocation, symbolicName, version.toString(), Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE)));
        setManifestTimeStamp(Long.parseLong(generatedFrom.getValue()));
        setManifestType(Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE)));
        if (!adaptor.canWrite() || existingHeaders != null) return existingHeaders;
        if (originalManifest != null) {
            Enumeration keysEnum = originalManifest.keys();
            while (keysEnum.hasMoreElements()) {
                Object key = keysEnum.nextElement();
                generatedManifest.put(key, originalManifest.get(key));
            }
        }
        File bundleManifestLocation = new File(cacheLocation, symbolicName + '_' + version.toString() + ".MF");
        try {
            converter.writeManifest(bundleManifestLocation, generatedManifest, true);
        } catch (Exception e) {
        }
        return generatedManifest;
    }

    private Dictionary loadManifestFrom(URL manifestURL) throws BundleException {
        try {
            return Headers.parseManifest(manifestURL.openStream());
        } catch (IOException e) {
            throw new BundleException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_DATA_ERROR_READING_MANIFEST, getLocation()), e);
        }
    }

    protected void loadFromManifest() throws BundleException {
        getManifest(true);
        super.loadFromManifest();
        if (manifest instanceof CachedManifest) throw new IllegalStateException();
        pluginClass = (String) manifest.get(EclipseAdaptor.PLUGIN_CLASS);
        parseAutoStart((String) manifest.get(EclipseAdaptor.ECLIPSE_AUTOSTART));
        buddyList = (String) manifest.get(Constants.BUDDY_LOADER);
        registeredBuddyList = (String) manifest.get(Constants.REGISTERED_POLICY);
        hasPackageInfo = hasPackageInfo(getEntry(Constants.OSGI_BUNDLE_MANIFEST));
    }

    private boolean hasPackageInfo(URL url) {
        if (url == null) return false;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Specification-Title: ") || line.startsWith("Specification-Version: ") || line.startsWith("Specification-Vendor: ") || line.startsWith("Implementation-Title: ") || line.startsWith("Implementation-Version: ") || line.startsWith("Implementation-Vendor: ")) return true;
            }
        } catch (IOException ioe) {
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
            }
        }
        return false;
    }

    /**
	 * Returns the plugin class
	 * @return the plugin class
	 */
    public String getPluginClass() {
        return pluginClass;
    }

    /**
	 * Returns the buddy list for this bundle
	 * @return the buddy list for this bundle
	 */
    public String getBuddyList() {
        return buddyList;
    }

    /**
	 * Returns the registered buddy list for this bundle
	 * @return the registered buddy list for this bundle
	 */
    public String getRegisteredBuddyList() {
        return registeredBuddyList;
    }

    /**
	 * Sets the plugin class
	 * @param value the plugin class
	 */
    public void setPluginClass(String value) {
        pluginClass = value;
    }

    /**
	 * Returns the timestamp for the manifest file
	 * @return the timestamp for the manifest file
	 */
    public long getManifestTimeStamp() {
        return manifestTimeStamp;
    }

    /**
	 * Sets the manifest timestamp for this bundle
	 * @param stamp the manifest timestamp
	 */
    public void setManifestTimeStamp(long stamp) {
        manifestTimeStamp = stamp;
    }

    /**
	 * Returns the manifest type
	 * @return the manifest type
	 */
    public byte getManifestType() {
        return manifestType;
    }

    /**
	 * Sets the manifest type
	 * @param manifestType the manifest type
	 */
    public void setManifestType(byte manifestType) {
        this.manifestType = manifestType;
    }

    /**
	 * Sets the auto start flag
	 * @param value the auto start flag
	 */
    public void setAutoStart(boolean value) {
        autoStart = value;
    }

    /**
	 * Checks whether this bundle is auto started for all resource/class loads
	 * @return true if the bundle is auto started; false otherwise
	 */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
	 * Returns the status of this bundle which is persisted on shutdown.  For bundles
	 * which are auto started the started state is removed to prevent the bundle from
	 * being started on the next startup.
	 * @return the status of this bundle which is persisted on shutdown
	 */
    public int getPersistentStatus() {
        return isAutoStartable() ? (~Constants.BUNDLE_STARTED) & getStatus() : getStatus();
    }

    /**
	 * Sets the list of auto start exceptions for this bundle
	 * @param autoStartExceptions the list of auto start exceptions
	 */
    public void setAutoStartExceptions(String[] autoStartExceptions) {
        this.autoStartExceptions = autoStartExceptions;
    }

    /**
	 * Returns the auto start exception packages
	 * @return the auto start exception packages
	 */
    public String[] getAutoStartExceptions() {
        return autoStartExceptions;
    }

    private void parseAutoStart(String headerValue) {
        autoStart = false;
        autoStartExceptions = null;
        ManifestElement[] allElements = null;
        try {
            allElements = ManifestElement.parseHeader(EclipseAdaptor.ECLIPSE_AUTOSTART, headerValue);
        } catch (BundleException e) {
            String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS, getLocation());
            EclipseAdaptor.getDefault().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, e, null));
        }
        if (allElements == null) return;
        autoStart = "true".equalsIgnoreCase(allElements[0].getValue());
        String exceptionsValue = allElements[0].getAttribute(EclipseAdaptor.ECLIPSE_AUTOSTART_EXCEPTIONS);
        if (exceptionsValue == null) return;
        StringTokenizer tokenizer = new StringTokenizer(exceptionsValue, ",");
        int numberOfTokens = tokenizer.countTokens();
        autoStartExceptions = new String[numberOfTokens];
        for (int i = 0; i < numberOfTokens; i++) autoStartExceptions[i] = tokenizer.nextToken().trim();
    }

    /**
	 * Checks whether this bundle is auto started for all resource/class loads or only for a
	 * subset of resource/classloads 
	 * @return true if the bundle is auto started; false otherwise
	 */
    public boolean isAutoStartable() {
        return autoStart || (autoStartExceptions != null && autoStartExceptions.length > 0);
    }

    /**
	 * Save the bundle data in the data file.
	 *
	 * @throws IOException if a write error occurs.
	 */
    public synchronized void save() throws IOException {
        if (adaptor.canWrite()) ((EclipseAdaptor) adaptor).saveMetaDataFor(this);
    }

    public String toString() {
        return "BundleData for " + getSymbolicName() + " (" + id + ")";
    }

    public File getParentGenerationDir() {
        Location parentConfiguration = null;
        Location currentConfiguration = LocationManager.getConfigurationLocation();
        if (currentConfiguration != null && (parentConfiguration = currentConfiguration.getParentLocation()) != null) return new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + '/' + LocationManager.BUNDLES_DIR + '/' + getBundleID() + '/' + getGeneration());
        return null;
    }
}
