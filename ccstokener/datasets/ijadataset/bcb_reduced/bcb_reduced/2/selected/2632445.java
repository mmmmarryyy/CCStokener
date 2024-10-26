package org.escapek.i18n;

import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Manage access to properties files used for translations.
 * @author nicolasjouanin
 *
 */
public class LocaleService {

    public static final String registryMessages = "$nl$/registry.properties";

    public static final String securityMessages = "$nl$/security.properties";

    public static final String uiMessages = "$nl$/UImessages.properties";

    public static final String repositoryCreationMessages = "$nl$/repositoryCreation.properties";

    public static final String serviceTesterMessages = "$nl$/serviceTester.properties";

    public static final String monitoringMessages = "$nl$/monitoring.properties";

    public static final String commonMessages = "$nl$/commonMessages.properties";

    public static final String CIDefinitions = "$nl$/CIDefinitions.properties";

    public static final String errorMessages = "$nl$/errorMessages.properties";

    private static Hashtable<String, MessageService> serviceCache = null;

    private static void init() {
        serviceCache = new Hashtable<String, MessageService>();
    }

    /**
	 * Look for the requested file into the plugin fragments and return a MessageService object
	 * initialized with the translation read.
	 * @param fileId the file to read
	 * @return the message service loaded.
	 */
    public static MessageService getMessageService(String fileId) {
        MessageService ms = null;
        if (serviceCache == null) init();
        if (serviceCache.containsKey(fileId)) return serviceCache.get(fileId);
        Properties p = new Properties();
        try {
            URL url = I18nPlugin.getFileURL(fileId);
            p.load(url.openStream());
            ms = new MessageService(p);
        } catch (Exception e) {
            ms = new MessageService();
        }
        serviceCache.put(fileId, ms);
        return ms;
    }
}
