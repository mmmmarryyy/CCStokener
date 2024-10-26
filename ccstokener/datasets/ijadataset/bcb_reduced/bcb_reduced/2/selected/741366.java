package edu.psu.its.lionshare.peerserver.settings;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 *
 * Class will handle the configuration properties of this peerserver
 * if any new configuration properties need to be implemented support for
 * persisting them should be added here.
 *
 * @author Lorin Metzger
 *
 */
public class PeerserverProperties {

    private static PeerserverProperties instance = null;

    private Properties properties = null;

    private static final String property_file_path = "PeerserverProperties.properties";

    private static final String STORAGE_QUOTA_KEY = "quota";

    private static final String KERB_KDC_KEY = "kerbkdc";

    private static final String KERB_REALM_KEY = "kerbrealm";

    private static final String JAAS_CONFIG_KEY = "jaasconfig";

    private static final String GNUTELLA_PORT = "gnu_port";

    private static final String PEERSERVER_DESCRIPTION = "peerserver_description";

    private static final String PEERSERVER_NAME = "peerserver_name";

    private static final String PEERSERVER_HOST_ADDR = "peerserver_host_address";

    private static final String PEERSERVER_NON_SECURE_PORT = "non_secure_port";

    private static final String PEERSERVER_SECURE_PORT = "secure_port";

    private static final String PEERSERVER_MAX_THREADS = "max_threads";

    private static final String PEERSERVER_MAX_USER_VDS = "max_virtual_directories_per_user";

    private static final String PEERSERVER_FILE_STORAGE_DIR = "file_storage_dir";

    private static final String UDDI_HOST = "uddi_host";

    private static final String UDDI_USERNAME = "uddi_username";

    private static final String UDDI_PASSWORD = "uddi_password";

    private PeerserverProperties() {
        properties = new Properties();
        loadProperties();
    }

    public static void initialize() {
        System.setProperty("java.security.krb5.realm", PeerserverProperties.getInstance().getKerberosRealm());
        System.setProperty("java.security.krb5.kdc", PeerserverProperties.getInstance().getKerberosKdc());
        System.setProperty("java.security.auth.login.config", PeerserverProperties.getInstance().getJaasConfigFile());
    }

    /**
   *
   * Load the persisted properties from the properties file at 
   * property_file_path.
   *
   */
    public void loadProperties() {
        try {
            InputStream pstream = null;
            if (new File(property_file_path).exists()) pstream = new FileInputStream(property_file_path); else {
                URL url = Thread.currentThread().getContextClassLoader().getResource("PeerserverProperties.properties");
                if (url != null) pstream = url.openStream();
            }
            properties.load(pstream);
        } catch (Exception e) {
        }
    }

    /**
   *
   * Persist the properties associate with this 
   * <code>java.util.Properties</code> object to disk.
   * 
   */
    public void storeProperties() {
        try {
            if (!new File(property_file_path).exists()) new File(property_file_path).createNewFile();
            FileOutputStream postream = new FileOutputStream(property_file_path);
            properties.store(postream, "Peerserver Properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   *
   * Return a single instance of this PeerserverProperties class.
   *
   * @return PeerserverProperties - singleton.
   *
   */
    public static PeerserverProperties getInstance() {
        if (instance == null) {
            instance = new PeerserverProperties();
        }
        return instance;
    }

    /**
   *
   * Get the amount of disk storage space each user will be allocated to
   * store files onto the peerserver.
   *
   * @return int - The amount of space in MegaBytes.
   *
   */
    public int getUserQuotaValue() {
        if (properties != null) {
            String size = properties.getProperty(STORAGE_QUOTA_KEY);
            try {
                return new Integer(size).intValue();
            } catch (Exception e) {
                System.out.println("Could no load user quota value, using 1024MB");
            }
        }
        return 1024;
    }

    /**
   *
   * Set the amount of disk storage space each user is allocated.
   *
   * @param int quota - Amount of disk space in MegaBytes.
   *
   */
    public void setUserQuotaValue(int quota) {
        try {
            properties.setProperty(STORAGE_QUOTA_KEY, new Integer(quota).toString());
            storeProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setKerberosRealm(String host) {
        try {
            properties.setProperty(KERB_REALM_KEY, host);
            storeProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getKerberosRealm() {
        String realm = properties.getProperty(KERB_REALM_KEY);
        if (realm != null) {
            return realm;
        }
        return "dce.psu.edu";
    }

    public void setKerberosKdc(String host) {
        try {
            properties.setProperty(KERB_KDC_KEY, host);
            storeProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getKerberosKdc() {
        String kdc = properties.getProperty(KERB_KDC_KEY);
        if (kdc != null) {
            return kdc;
        }
        return "fido.aset.psu.edu";
    }

    public void setJaasConfigFile(String path) {
        try {
            properties.setProperty(JAAS_CONFIG_KEY, path);
            storeProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getJaasConfigFile() {
        String jaas_conf = properties.getProperty(JAAS_CONFIG_KEY);
        if (jaas_conf != null) {
            return jaas_conf;
        }
        return ".." + File.separator + "jaas.conf";
    }

    public String getGnutellaPort() {
        String port = properties.getProperty(GNUTELLA_PORT);
        if (port != null) {
            return port;
        }
        return "6346";
    }

    public String getName() {
        String name = properties.getProperty(PEERSERVER_NAME);
        if (name != null) {
            return name;
        }
        return "Local Host";
    }

    public String getPeerserverDescription() {
        String description = properties.getProperty(PEERSERVER_DESCRIPTION);
        if (description != null) {
            return description;
        }
        return "Generic nServer Description";
    }

    public String getPeerserverHostAddress() {
        String host_addr = properties.getProperty(PEERSERVER_HOST_ADDR);
        if (host_addr != null) {
            return host_addr;
        }
        return "localhost";
    }

    public int getPeerserverSecurePort() {
        String secure = properties.getProperty(PEERSERVER_SECURE_PORT);
        if (secure != null) {
            return new Integer(secure).intValue();
        }
        return 8443;
    }

    public int getNonSecurePeerserverPort() {
        String non_secure = properties.getProperty(PEERSERVER_NON_SECURE_PORT);
        if (non_secure != null) {
            return new Integer(non_secure).intValue();
        }
        return 8080;
    }

    public int getPeerserverMaxThreads() {
        String max_threads = properties.getProperty(PEERSERVER_MAX_THREADS);
        if (max_threads != null) {
            return new Integer(max_threads).intValue();
        }
        return 50;
    }

    public int getMaximumVirtualDirectoriesPerUser() {
        String max = properties.getProperty(PEERSERVER_MAX_USER_VDS);
        if (max != null) {
            return new Integer(max).intValue();
        }
        return 10;
    }

    public String getFileStorageDir() {
        String dir = properties.getProperty(PEERSERVER_FILE_STORAGE_DIR);
        if (dir != null) {
            return dir;
        }
        return "../UserFiles";
    }

    public String getUddiHost() {
        String host = properties.getProperty(UDDI_HOST);
        if (host != null) {
            return host;
        }
        return "http://lionshare.its.psu.edu:1891/juddi/publish";
    }

    public String getUddiUsername() {
        String username = properties.getProperty(UDDI_USERNAME);
        if (username != null) {
            return username;
        }
        return "lmetzger";
    }

    public String getUddiPassword() {
        String password = properties.getProperty(UDDI_PASSWORD);
        if (password != null) {
            return password;
        }
        return "password";
    }
}
