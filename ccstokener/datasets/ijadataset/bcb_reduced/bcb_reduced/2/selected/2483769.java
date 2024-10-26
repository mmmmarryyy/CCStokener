package bpmn.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.MissingResourceException;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.ResourceLocator;

/**
 * This is the central singleton for the Bpmn edit plugin.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public final class BpmnEditPlugin extends EMFPlugin {

    /**
	 * Keep track of the singleton.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final BpmnEditPlugin INSTANCE = new BpmnEditPlugin();

    /**
	 * Keep track of the singleton.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private static Implementation plugin;

    /**
	 * Create the instance.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public BpmnEditPlugin() {
        super(new ResourceLocator[] {});
    }

    /**
	 * Returns the singleton instance of the Eclipse plugin.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the singleton instance.
	 * @generated
	 */
    public ResourceLocator getPluginResourceLocator() {
        return plugin;
    }

    /**
	 * Returns the singleton instance of the Eclipse plugin.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the singleton instance.
	 * @generated
	 */
    public static Implementation getPlugin() {
        return plugin;
    }

    /**
	 * The actual implementation of the Eclipse <b>Plugin</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static class Implementation extends EclipsePlugin {

        /**
		 * Creates an instance.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public Implementation() {
            super();
            plugin = this;
        }

        @Override
        protected Object doGetImage(String key) throws IOException {
            URL url;
            if (key.endsWith(".png")) {
                url = new URL(getBaseURL() + "icons/" + key);
            } else url = new URL(getBaseURL() + "icons/" + key + ".gif");
            InputStream inputStream = url.openStream();
            inputStream.close();
            return url;
        }
    }
}
