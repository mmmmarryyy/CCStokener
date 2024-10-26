package de.mpiwg.vspace.metamodel.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import de.mpiwg.vspace.extension.ExceptionHandlingService;

/**
 * This is the central singleton for the Mediastation edit plugin. <!--
 * begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */
public final class MediastationEditPlugin extends EMFPlugin {

    /**
	 * Keep track of the singleton. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
    public static final MediastationEditPlugin INSTANCE = new MediastationEditPlugin();

    /**
	 * Keep track of the singleton. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
    private static Implementation plugin;

    /**
	 * Create the instance. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public MediastationEditPlugin() {
        super(new ResourceLocator[] {});
    }

    /**
	 * Returns the singleton instance of the Eclipse plugin. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return the singleton instance.
	 * @generated
	 */
    @Override
    public ResourceLocator getPluginResourceLocator() {
        return plugin;
    }

    /**
	 * Returns the singleton instance of the Eclipse plugin. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return the singleton instance.
	 * @generated
	 */
    public static Implementation getPlugin() {
        return plugin;
    }

    /**
	 * The actual implementation of the Eclipse <b>Plugin</b>. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public static class Implementation extends EclipsePlugin {

        /**
		 * Creates an instance. <!-- begin-user-doc --> <!-- end-user-doc -->
		 * 
		 * @generated
		 */
        public Implementation() {
            super();
            plugin = this;
        }
    }

    @Override
    public Object getImage(String key) {
        if (key.indexOf("exhibition/") != -1) {
            InputStream inputStream = null;
            try {
                URL url = new URL(getBaseURL() + "icons/" + key + ".png");
                inputStream = url.openStream();
                return url;
            } catch (Exception e) {
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        ExceptionHandlingService.INSTANCE.handleException(e);
                    }
                }
            }
        }
        return super.getImage(key);
    }
}
