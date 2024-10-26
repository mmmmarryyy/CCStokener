package org.epoline.jsf.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import org.epoline.jsf.client.LoadBalancingProxyInterface;
import org.epoline.jsf.services.core.JiniService;
import org.epoline.jsf.services.helper.LoadObject;

/** 
 * All utilities that are used in the jsf-core (service-level)
 * 11-Mar-2003 10:23:29
 * @author Patrick Balm
 * @version $Revision: 1.1.1.1 $
 */
public class Core {

    static org.apache.log4j.Logger logger = null;

    static {
        logger = Log4jManager.getLogger(Core.class.getName());
    }

    /**
	 *
	 * @param classname
	 * @return the basename of this class without packagename
	 */
    public static final String basename(String classname) {
        if (logger.isDebugEnabled()) logger.debug("basename()");
        return classname.substring(classname.lastIndexOf('.') + 1);
    }

    /**
	 * Get our proxy from the service host running on the (non-)default port.
	 * <br>
	 * You can specify the <code>port</code> by adding ':port' to the URL.
	 * <br>
	 * <br>
	 * <b>Does not perform loadbalancing</b>
	 *
	 * @param	url The url to our known host, which can be anywhere in the network
	 * @param	template Match the active services in the network with our template
	 * 			and return the corresponding proxies.
	 * @param	maxMatches Maximum of object to be returned
	 *
	 * @return	The proxies or null when not found or in case of error
	 *
	 * @throws 	java.net.MalformedURLException URL not correctly defined
	 * @throws 	ClassNotFoundException If the proxy could not obtained from the lookupserver (Reggie)
	 * @throws 	java.io.IOException
	 * @see		net.jini.core.lookup.ServiceTemplate
	 */
    public static final ServiceItem[] getProxies(final String url, final ServiceTemplate template, final int maxMatches) throws MalformedURLException, IOException, ClassNotFoundException {
        if (logger.isDebugEnabled()) logger.debug("+getProxies");
        if (null == url) return null;
        LookupLocator loc = new LookupLocator(url);
        if (logger.isInfoEnabled()) {
            logger.info("Performing direct-lookup to " + loc.getHost() + " port " + loc.getPort());
        }
        ServiceRegistrar lusvc = loc.getRegistrar();
        final ServiceMatches match = lusvc.lookup(template, JiniService.MAX_SERVICE_MATCHES);
        return match.items;
    }

    public static final Object getProxy(String url, final ServiceTemplate template, final int maxMatches) throws MalformedURLException, IOException, ClassNotFoundException {
        if (logger.isDebugEnabled()) logger.debug("+getProxy");
        Object remote = null;
        Object[] services;
        ArrayList roundRobin = new ArrayList();
        boolean canLoadBalance = true;
        if (!url.startsWith("jini://")) {
            url = "jini://" + url;
        }
        ServiceRegistrar lusvc = new LookupLocator(url).getRegistrar();
        try {
            final ServiceMatches match = lusvc.lookup(template, maxMatches);
            final ServiceItem[] items = match.items;
            services = new Object[items.length];
            if (logger.isInfoEnabled()) logger.info("Locating LUS...");
            for (int i = 0; i < items.length; i++) {
                services[i] = items[i].service;
                if (services[i] instanceof LoadBalancingProxyInterface) {
                    if (logger.isInfoEnabled()) logger.info("Located service: " + services[i].toString());
                    try {
                        roundRobin.add(new LoadObject(services[i], ((LoadBalancingProxyInterface) services[i]).getCurrentLoad(), items[i].attributeSets));
                    } catch (Throwable runtime) {
                        logger.error("Locating error: " + runtime.getMessage());
                    }
                } else {
                    if (logger.isInfoEnabled()) logger.info("Proxy does not support loadbalancing " + new Date());
                    canLoadBalance = false;
                    remote = services[i];
                    return remote;
                }
            }
            if (canLoadBalance) {
                Object proxy = null;
                if (roundRobin.size() > 0) {
                    try {
                        proxy = Collections.min(roundRobin);
                        proxy = ((LoadObject) proxy).getObject();
                        return proxy;
                    } catch (NoSuchElementException nsee) {
                        logger.error(nsee.getMessage());
                        return null;
                    }
                }
            }
        } catch (RemoteException ex) {
            logger.error("Error doing lookup: " + ex.getMessage());
            return null;
        }
        if (logger.isDebugEnabled()) logger.debug("-getProxy");
        return remote;
    }

    /**
	 * Check HTTP based codebase settings.
	 * @param p the properties used to init the JVM or setContext
	 * @return false if at least one cannot be (down)loaded
	 */
    public static boolean isCodebaseDownloadable(Properties p) {
        class CodebaseData {

            String file;

            boolean success = true;
        }
        String codebase = p.getProperty("java.rmi.server.codebase", null);
        if (null == codebase) {
            if (logger.isDebugEnabled()) logger.debug("java.rmi.server.codebase = null (return false)");
            return false;
        }
        try {
            URL cbUrl = new URL(codebase);
            String protocol = cbUrl.getProtocol();
            String filename = cbUrl.getFile();
            if (logger.isDebugEnabled()) {
                logger.debug("Verifying java.rmi.server.codebase setting(s)...");
                logger.debug("Codebase = " + cbUrl.toString());
            }
            if (protocol.equals("http")) {
                if (filename.indexOf("http") == -1) {
                    try {
                        int size = cbUrl.openConnection().getContentLength();
                        if (logger.isDebugEnabled()) logger.debug("Checking " + cbUrl + " : OK");
                        return true;
                    } catch (IOException e) {
                        if (logger.isDebugEnabled()) logger.debug("Checking " + cbUrl + " : FAIL");
                        return false;
                    } finally {
                        if (logger.isDebugEnabled()) logger.debug("Verifying java.rmi.server.codebase setting(s)... Done!");
                    }
                } else {
                    ArrayList files = new ArrayList();
                    StringTokenizer st = new StringTokenizer(codebase);
                    URL url = null;
                    String part = null;
                    CodebaseData data = null;
                    while (st.hasMoreTokens()) {
                        part = st.nextToken();
                        url = new URL(part);
                        data = new CodebaseData();
                        try {
                            int len = url.openConnection().getContentLength();
                            if (len == -1) {
                                data.success = false;
                                data.file = part;
                            } else {
                                data.file = part;
                            }
                        } catch (IOException e) {
                            data.success = false;
                        }
                        files.add(data);
                    }
                    String wrong = null;
                    CodebaseData codebaseData = null;
                    boolean allOK = true;
                    int errorFiles = 0;
                    for (int i = 0; i < files.size(); i++) {
                        codebaseData = (CodebaseData) files.get(i);
                        if (!codebaseData.success) {
                            wrong += " " + codebaseData.file;
                            ++errorFiles;
                            allOK = false;
                        }
                        if (logger.isDebugEnabled()) logger.debug((i + 1) + ". Checking " + codebaseData.file + " : " + (codebaseData.success ? "OK" : "FAIL"));
                    }
                    if (errorFiles == 0) {
                        if (logger.isDebugEnabled()) logger.debug("All entries can be downloaded successfully!");
                    } else {
                        logger.error(errorFiles + " of " + files.size() + " entries can *not* be downloaded successfully!");
                    }
                    if (logger.isDebugEnabled()) logger.debug("Verifying java.rmi.server.codebase setting(s)... Done!");
                    if (allOK) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } else if (protocol.equalsIgnoreCase("file")) {
                if (logger.isDebugEnabled()) logger.debug("'file' protocol not supported for JSF");
            }
        } catch (MalformedURLException e) {
            logger.debug(e.getMessage());
            return false;
        }
        return false;
    }

    /**
	 * Obtains the loadbalancing scheme for the ServiceProvider
	 * @param inherit
	 * @return c
	 * @see org.epoline.jsf.client.ServiceProvider
	 */
    public static LoadBalancingProxyInterface getLoadBalancingScheme(LoadBalancingProxyInterface inherit) {
        if (logger.isDebugEnabled()) logger.debug("+getLoadBalancingScheme");
        if (logger.isDebugEnabled()) logger.debug("-getLoadBalancingScheme");
        return null;
    }
}
