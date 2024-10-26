package org.eclipse.help.internal.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.remote.PreferenceFileHandler;
import org.eclipse.help.internal.base.remote.RemoteContentLocator;
import org.eclipse.help.internal.base.remote.RemoteHelp;
import org.eclipse.help.internal.util.ResourceLocator;
import org.eclipse.help.internal.util.URLCoder;
import org.osgi.framework.Bundle;

/**
 * URLConnection to help documents in plug-ins
 */
public class HelpURLConnection extends URLConnection {

    private static final String PARAM_LANG = "lang";

    private static final String PRODUCT_PLUGIN = "PRODUCT_PLUGIN";

    public static final String PLUGINS_ROOT = "PLUGINS_ROOT/";

    private static final String PATH_RTOPIC = "/rtopic";

    protected static boolean cachingEnabled = true;

    static {
        String[] args = Platform.getCommandLineArgs();
        for (int i = 0; i < args.length; i++) {
            if ("-dev".equals(args[i])) {
                cachingEnabled = false;
                break;
            }
        }
    }

    protected String pluginAndFile;

    protected String query;

    protected HashMap arguments;

    protected Bundle plugin;

    protected String file;

    protected String locale;

    private static String appserverImplPluginId;

    /**
	 * Constructor for HelpURLConnection
	 */
    public HelpURLConnection(URL url) {
        super(url);
        String urlFile = url.getFile();
        int index = urlFile.indexOf(PLUGINS_ROOT);
        if (index != -1) urlFile = urlFile.substring(index + PLUGINS_ROOT.length());
        if (urlFile.startsWith("/")) urlFile = urlFile.substring(1);
        int indx = urlFile.indexOf("?");
        if (indx != -1) {
            query = urlFile.substring(indx + 1);
            urlFile = urlFile.substring(0, indx);
        }
        this.pluginAndFile = urlFile;
        parseQuery();
        setDefaultUseCaches(isCacheable());
    }

    /**
	 * @see URLConnection#connect()
	 */
    public void connect() throws IOException {
    }

    /**
	 * see URLConnection#getInputStream(); Note: this method can throw IOException, but should never
	 * return null
	 */
    public InputStream getInputStream() throws IOException {
        Bundle plugin = getPlugin();
        if (plugin != null && plugin.getSymbolicName().equals(getAppserverImplPluginId())) {
            throw new IOException("Resource not found.");
        }
        if (getFile() == null || "".equals(getFile())) {
            throw new IOException("Resource not found.");
        }
        InputStream in = null;
        if (plugin != null) {
            in = ResourceLocator.openFromProducer(plugin, query == null ? getFile() : getFile() + "?" + query, getLocale());
            if (in == null) {
                in = ResourceLocator.openFromZip(plugin, "doc.zip", getFile(), getLocale());
            }
            if (in == null) {
                in = ResourceLocator.openFromPlugin(plugin, getFile(), getLocale());
            }
        } else {
            in = openFromRemoteServer(getHref(), getLocale());
        }
        if (in == null) {
            throw new IOException("Resource not found.");
        }
        return in;
    }

    public long getExpiration() {
        return isCacheable() ? new Date().getTime() + 10000 : 0;
    }

    public static void parseQuery(String query, HashMap arguments) {
        StringTokenizer stok = new StringTokenizer(query, "&");
        while (stok.hasMoreTokens()) {
            String aQuery = stok.nextToken();
            int equalsPosition = aQuery.indexOf("=");
            if (equalsPosition > -1) {
                String arg = aQuery.substring(0, equalsPosition);
                String val = aQuery.substring(equalsPosition + 1);
                Object existing = arguments.get(arg);
                if (existing == null) arguments.put(arg, val); else if (existing instanceof Vector) {
                    ((Vector) existing).add(val);
                    arguments.put(arg, existing);
                } else {
                    Vector v = new Vector(2);
                    v.add(existing);
                    v.add(val);
                    arguments.put(arg, v);
                }
            }
        }
    }

    /**
	 * NOTE: need to add support for multi-valued parameters (like filtering) Multiple values are
	 * added as vectors
	 */
    protected void parseQuery() {
        if (query != null && !"".equals(query)) {
            if (arguments == null) {
                arguments = new HashMap(5);
            }
            parseQuery(query, arguments);
        }
    }

    public String getContentType() {
        String file = pluginAndFile.toLowerCase(Locale.US);
        if (file.endsWith(".html") || file.endsWith(".htm") || file.endsWith(".xhtml")) return "text/html"; else if (file.endsWith(".css")) return "text/css"; else if (file.endsWith(".gif")) return "image/gif"; else if (file.endsWith(".jpg")) return "image/jpeg"; else if (file.endsWith(".pdf")) return "application/pdf"; else if (file.endsWith(".xml")) return "application/xml"; else if (file.endsWith(".xsl")) return "application/xsl";
        return "text/plain";
    }

    /**
	 * 
	 */
    public Vector getMultiValue(String name) {
        if (arguments != null) {
            Object value = arguments.get(name);
            if (value instanceof Vector) return (Vector) value;
            return null;
        }
        return null;
    }

    /**
	 * 
	 */
    public String getValue(String name) {
        if (arguments == null) return null;
        Object value = arguments.get(name);
        String stringValue = null;
        if (value instanceof String) stringValue = (String) value; else if (value instanceof Vector) stringValue = (String) ((Vector) value).firstElement(); else return null;
        try {
            return URLCoder.decode(stringValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * Returns the locale specified by client.
	 */
    protected String getLocale() {
        if (locale == null) {
            locale = getValue(PARAM_LANG);
            if (locale == null) {
                locale = Platform.getNL();
            }
        }
        return locale;
    }

    protected String getFile() {
        if (file == null) {
            int start = pluginAndFile.indexOf("/") + 1;
            int end = pluginAndFile.indexOf("?");
            if (end == -1) end = pluginAndFile.indexOf("#");
            if (end == -1) end = pluginAndFile.length();
            file = pluginAndFile.substring(start, end);
            file = URLCoder.decode(file);
        }
        return file;
    }

    protected Bundle getPlugin() {
        if (plugin == null) {
            int i = pluginAndFile.indexOf('/');
            String pluginId = i == -1 ? "" : pluginAndFile.substring(0, i);
            pluginId = URLCoder.decode(pluginId);
            if (PRODUCT_PLUGIN.equals(pluginId)) {
                IProduct product = Platform.getProduct();
                if (product != null) {
                    plugin = product.getDefiningBundle();
                    return plugin;
                }
            }
            plugin = Platform.getBundle(pluginId);
        }
        return plugin;
    }

    private String getHref() {
        return '/' + pluginAndFile;
    }

    public boolean isCacheable() {
        if (getValue("resultof") != null) return false;
        return cachingEnabled;
    }

    public String toString() {
        return pluginAndFile;
    }

    /**
	 * Obtains ID of plugin that contributes appserver implementation. *
	 * 
	 * @return plug-in ID or null
	 */
    private static String getAppserverImplPluginId() {
        if (appserverImplPluginId == null) {
            IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
            IExtensionPoint point = pluginRegistry.getExtensionPoint("org.eclipse.help.appserver.server");
            if (point != null) {
                IExtension[] extensions = point.getExtensions();
                if (extensions.length != 0) {
                    IConfigurationElement[] elements = extensions[0].getConfigurationElements();
                    if (elements.length == 0) return null;
                    IConfigurationElement serverElement = null;
                    for (int i = 0; i < elements.length; i++) {
                        String defaultValue = elements[i].getAttribute("default");
                        if (defaultValue == null || defaultValue.equals("false")) {
                            serverElement = elements[i];
                            break;
                        }
                    }
                    if (serverElement == null) {
                        serverElement = elements[0];
                    }
                    appserverImplPluginId = serverElement.getContributor().getName();
                }
            }
        }
        return appserverImplPluginId;
    }

    private InputStream openFromRemoteServer(String href, String locale) {
        if (RemoteHelp.isEnabled()) {
            String pathSuffix = PATH_RTOPIC + href + '?' + PARAM_LANG + '=' + locale;
            int i = pluginAndFile.indexOf('/');
            String pluginId = i == -1 ? "" : pluginAndFile.substring(0, i);
            pluginId = URLCoder.decode(pluginId);
            String remoteURL = RemoteContentLocator.getUrlForContent(pluginId);
            InputStream in;
            if (remoteURL == null) {
                in = tryOpeningAllServers(pathSuffix);
            } else {
                in = openRemoteStream(remoteURL, pathSuffix);
            }
            return in;
        }
        return null;
    }

    private InputStream openRemoteStream(String remoteURL, String pathSuffix) {
        URL url;
        InputStream in = null;
        try {
            url = new URL(remoteURL + pathSuffix);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            in = connection.getInputStream();
        } catch (Exception e) {
        }
        return in;
    }

    private InputStream tryOpeningAllServers(String pathSuffix) {
        PreferenceFileHandler prefHandler = new PreferenceFileHandler();
        String host[] = prefHandler.getHostEntries();
        String port[] = prefHandler.getPortEntries();
        String path[] = prefHandler.getPathEntries();
        String isEnabled[] = prefHandler.isEnabled();
        int numICs = host.length;
        for (int i = 0; i < numICs; i++) {
            if (isEnabled[i].equalsIgnoreCase("true")) {
                String urlStr = "http://" + host[i] + ':' + port[i] + path[i];
                InputStream is = openRemoteStream(urlStr, pathSuffix);
                if (is != null) {
                    return is;
                }
            }
        }
        return null;
    }
}
