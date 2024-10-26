package net.sf.epfe.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.LogManager;
import net.sf.epfe.core.io.II18NBundleIOFactory;
import net.sf.epfe.core.io.serializer.II18NBundleDeserializer;
import net.sf.epfe.core.io.serializer.II18NBundleSerializer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This is the central singleton for the I18NMsgBundles model plugin. <!-- begin-user-doc --> <!--
 * end-user-doc -->
 * 
 * @generated
 */
public final class EPFECoreActivator extends EMFPlugin {

    /**
	 * The actual implementation of the Eclipse <b>Plugin</b>. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
    public static class Implementation extends EclipsePlugin {

        private static final String LOG_PROPERTIES_FILE = "logging.properties";

        private ServiceRegistration fRegSerializer;

        private ServiceRegistration fRegDeserializer;

        EPFEResourceChangeDelegateServiceTracker fResListenerTracker;

        /**
		 * Creates an instance. <!-- begin-user-doc --> <!-- end-user-doc -->
		 * 
		 * @generated
		 */
        public Implementation() {
            super();
            plugin = this;
        }

        @Override
        public void start(BundleContext aContext) throws Exception {
            super.start(aContext);
            try {
                URL url = getBundle().getEntry("/" + LOG_PROPERTIES_FILE);
                if (url != null) {
                    InputStream propertiesInputStream = url.openStream();
                    LogManager.getLogManager().readConfiguration(propertiesInputStream);
                    propertiesInputStream.close();
                }
            } catch (IOException lEx) {
                EPFECoreActivator.log(IStatus.WARNING, "Couldn't load a resource file " + LOG_PROPERTIES_FILE + ".", lEx);
            }
            fResListenerTracker = new EPFEResourceChangeDelegateServiceTracker(aContext);
            registerServices(aContext);
        }

        @Override
        public void stop(BundleContext aContext) throws Exception {
            super.stop(aContext);
            if (fResListenerTracker != null) fResListenerTracker.close();
            deregisterServices(aContext);
        }

        private void deregisterServices(BundleContext aContext) {
            if (fRegDeserializer != null) {
                fRegDeserializer.unregister();
                fRegDeserializer = null;
            }
            if (fRegSerializer != null) {
                fRegSerializer.unregister();
                fRegSerializer = null;
            }
        }

        private void registerServices(BundleContext aContext) {
            Dictionary<String, String> lDescr = new Hashtable<String, String>();
            lDescr.put(II18NBundleSerializer.SERV_TYPE, II18NBundleSerializer.SERV_PROPVALUE_TYPE_PROPERTIES);
            lDescr.put(II18NBundleSerializer.SERV_PROP_OUTPUT, II18NBundleSerializer.SERV_PROPVALUE_OUTPUT_STREAM);
            fRegSerializer = aContext.registerService(II18NBundleSerializer.class.getName(), II18NBundleIOFactory.INSTANCE.createBundleSerializerProperties(), lDescr);
            lDescr = new Hashtable<String, String>();
            lDescr.put(II18NBundleDeserializer.SERV_TYPE, II18NBundleDeserializer.SERV_PROPVALUE_TYPE_PROPERTIES);
            lDescr.put(II18NBundleDeserializer.SERV_PROP_INPUT, II18NBundleDeserializer.SERV_PROPVALUE_INPUT_STREAM);
            fRegDeserializer = aContext.registerService(II18NBundleDeserializer.class.getName(), II18NBundleIOFactory.INSTANCE.createBundleDeserializerProperties(), lDescr);
        }
    }

    /**
	 * Keep track of the singleton. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public static final EPFECoreActivator INSTANCE = new EPFECoreActivator();

    public static final String PLUGIN_ID = "net.sf.epfe.core";

    /**
	 * Keep track of the singleton. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    private static Implementation plugin;

    public static IStatus createStatus(int aSeverity, String aMessage) {
        return new Status(aSeverity, PLUGIN_ID, aMessage);
    }

    public static IStatus createStatus(int aSeverity, String aMessage, Throwable aEx) {
        return new Status(aSeverity, PLUGIN_ID, aMessage, aEx);
    }

    public static BundleContext getBundleContext() {
        return getPlugin().getBundle().getBundleContext();
    }

    public static Plugin getDefault() {
        return getPlugin();
    }

    /**
	 * Returns the singleton instance of the Eclipse plugin. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return the singleton instance.
	 * @generated
	 */
    public static Implementation getPlugin() {
        return plugin;
    }

    public static String getVersion() {
        return getPlugin().getBundle().getHeaders().get("Bundle-Version").toString();
    }

    public static void log(int aError, String aString, Throwable aEx) {
        getDefault().getLog().log(new Status(aError, PLUGIN_ID, aString, aEx));
    }

    /**
	 * Create the instance. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public EPFECoreActivator() {
        super(new ResourceLocator[] {});
    }

    /**
	 * Returns the singleton instance of the Eclipse plugin. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return the singleton instance.
	 * @generated
	 */
    @Override
    public ResourceLocator getPluginResourceLocator() {
        return plugin;
    }
}
