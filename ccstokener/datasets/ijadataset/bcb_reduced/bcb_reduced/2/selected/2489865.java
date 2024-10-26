package soht.client.java.configuration;

import org.apache.log4j.PropertyConfigurator;
import java.util.List;
import java.util.Properties;
import java.util.Enumeration;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ssl.*;

/**
 * Handles the configuration data for the SOHT Proxy client.
 *
 * @author Eric Daugherty
 */
public class ConfigurationManager {

    public static final int MODE_STATEFUL = 0;

    public static final int MODE_STATELESS = 1;

    private Properties properties;

    private String propertiesFile;

    private String serverURL;

    private boolean serverLoginRequired;

    private String serverUsername;

    private String serverPassword;

    private boolean useStatelessConnection;

    private boolean useHTTPProxy;

    private String proxyHost;

    private String proxyPort;

    private String proxyLogin;

    private String proxyPassword;

    private boolean socksServerEnabled;

    private int socksServerPort;

    private List hosts;

    static {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }
    }

    /**
     * Initializes a new ConfigurationManager instance to load/save
     * configuration information to/from the specified properties file.
     *
     * @param propertiesFile the path/filename to the soht properties file.
     * @throws ConfigurationException thrown if there is an error loading the file.
     */
    public ConfigurationManager(String propertiesFile) throws ConfigurationException {
        this.propertiesFile = propertiesFile;
        loadProperties();
        initializeLogger();
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public boolean isServerLoginRequired() {
        return serverLoginRequired;
    }

    /**
	 * @return Returns the socksServerPort.
	 */
    public int getSocksServerPort() {
        return socksServerPort;
    }

    /**
	 * @param socksServerPort The socksServerPort to set.
	 */
    public void setSocksServerPort(int socksServerPort) {
        this.socksServerPort = socksServerPort;
    }

    /**
	 * @return Returns the socksServerEnabled.
	 */
    public boolean isSocksServerEnabled() {
        return socksServerEnabled;
    }

    /**
	 * @param socksServerEnabled The socksServerEnabled to set.
	 */
    public void setSocksServerEnabled(boolean socksServerEnabled) {
        this.socksServerEnabled = socksServerEnabled;
    }

    public void setServerLoginRequired(boolean serverLoginRequired) {
        this.serverLoginRequired = serverLoginRequired;
    }

    public String getServerUsername() {
        return serverUsername;
    }

    public void setServerUsername(String serverUsername) {
        this.serverUsername = serverUsername;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    public boolean isUseStatelessConnection() {
        return useStatelessConnection;
    }

    public void setUseStatelessConnection(boolean useStatelessConnection) {
        this.useStatelessConnection = useStatelessConnection;
    }

    public boolean isUseHTTPProxy() {
        return useHTTPProxy;
    }

    public void setUseHTTPProxy(boolean useHTTPProxy) {
        this.useHTTPProxy = useHTTPProxy;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public List getHosts() {
        return hosts;
    }

    public void setHosts(List hosts) {
        this.hosts = hosts;
    }

    public String getProxyLogin() {
        return proxyLogin;
    }

    public void setProxyLogin(String proxyLogin) {
        this.proxyLogin = proxyLogin;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    /**
     * Initializes and configures a HttpURLConnection for use.  This includes
     * setting the server URL and any proxy configuration, if neccessary.
     *
     * @return a configured HttpURLConnection
     * @throws IOException thrown if unable to connect to URL
     */
    public HttpURLConnection getURLConnection() throws IOException {
        String url_str = getServerURL();
        URL url = new URL(url_str);
        HttpURLConnection urlConnection;
        if (url_str.toLowerCase().startsWith("https")) {
            HttpsURLConnection urlSConnection = (HttpsURLConnection) url.openConnection();
            urlSConnection.setHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            urlConnection = urlSConnection;
        } else urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        if (useHTTPProxy && getProxyLogin() != null) {
            String authString = getProxyLogin() + ":" + getProxyPassword();
            String auth = "Basic " + new sun.misc.BASE64Encoder().encode(authString.getBytes());
            urlConnection.setRequestProperty("Proxy-Authorization", auth);
        }
        urlConnection.setDoOutput(true);
        if (useHTTPProxy) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", proxyHost);
            System.getProperties().put("proxyPort", String.valueOf(proxyPort));
        }
        return urlConnection;
    }

    /**
     * Loads configuration information from the configured properties file.
     *
     * @throws ConfigurationException thrown if there is an error loading the file.
     */
    private void loadProperties() throws ConfigurationException {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (Throwable throwable) {
            throw new ConfigurationException("Unable to load configuration file: " + propertiesFile + " - " + throwable.toString());
        }
        serverURL = getRequiredProperty("server.url");
        String serverLoginRequiredString = properties.getProperty("server.loginrequired", "false");
        serverLoginRequired = Boolean.valueOf(serverLoginRequiredString).booleanValue();
        if (serverLoginRequired) {
            serverUsername = getRequiredProperty("server.username");
            serverPassword = getRequiredProperty("server.password");
        }
        String connectionMode = properties.getProperty("server.stateless", "false");
        useStatelessConnection = Boolean.valueOf(connectionMode).booleanValue();
        String useProxyString = properties.getProperty("proxy.useproxy", "false");
        useHTTPProxy = Boolean.valueOf(useProxyString).booleanValue();
        if (useHTTPProxy) {
            proxyHost = getRequiredProperty("proxy.host");
            proxyPort = getRequiredProperty("proxy.port");
            proxyLogin = properties.getProperty("proxy.login");
            proxyPassword = properties.getProperty("proxy.password");
        }
        String socksServerEnabledString = properties.getProperty("socks.server.enabled", "false");
        socksServerEnabled = Boolean.valueOf(socksServerEnabledString).booleanValue();
        if (socksServerEnabled) {
            String socksServerPortString = properties.getProperty("socks.server.port", "1080");
            socksServerPort = Integer.parseInt(socksServerPortString);
        }
        hosts = new ArrayList();
        Enumeration propertyKeys = properties.keys();
        String keyName;
        String keyValue;
        int delimiterIndex;
        String localPort;
        String remoteHost;
        String remotePort;
        while (propertyKeys.hasMoreElements()) {
            keyName = (String) propertyKeys.nextElement();
            if (keyName.startsWith("port.")) {
                localPort = keyName.substring(5);
                keyValue = properties.getProperty(keyName);
                delimiterIndex = keyValue.indexOf(":");
                if (delimiterIndex == -1) {
                    throw new ConfigurationException("Mapping for local port: " + localPort + " invalid.  Please specify value as <host>:<port>.");
                }
                remoteHost = keyValue.substring(0, delimiterIndex);
                remotePort = keyValue.substring(delimiterIndex + 1);
                hosts.add(new Host(localPort, remoteHost, remotePort));
            }
        }
    }

    /**
     * Loads the specified property, and throws a ConfigurationException
     * if the property does not exist.
     *
     * @param propertyName the property key to load.
     * @return the property value.  This will never be null.
     * @throws ConfigurationException thrown if the property is null.
     */
    private String getRequiredProperty(String propertyName) throws ConfigurationException {
        String property = properties.getProperty(propertyName);
        if (property == null) {
            throw new ConfigurationException("Missing required property: " + propertyName);
        }
        return property;
    }

    /**
     * Configure Logging framework.
     */
    private void initializeLogger() {
        PropertyConfigurator.configureAndWatch(propertiesFile);
    }
}
