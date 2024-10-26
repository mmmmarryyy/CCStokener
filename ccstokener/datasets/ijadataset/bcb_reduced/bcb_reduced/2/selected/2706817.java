package net.sourceforge.fluxion.pussycat;

import net.sourceforge.fluxion.pussycat.util.PussycatUtils;
import net.sourceforge.fluxion.pussycat.util.exceptions.PussycatException;
import net.sourceforge.fluxion.pussycat.util.exceptions.PussycatHTMLException;
import org.semanticweb.owl.model.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * Javadocs go here.
 *
 * @author Rob Davey / Tony Burdett
 * @version 1.0
 * @date 28-Nov-2006
 */
public class FluxionServiceClient {

    Set<URI> cachedDatasources;

    Set<OWLOntology> cachedSchemas;

    Set<URL> serviceURLs = new HashSet<URL>();

    Set<URL> upServiceURLs = new HashSet<URL>();

    Set<URL> downServiceURLs = new HashSet<URL>();

    String httpProxy = "";

    private int httpProxyPort = -1;

    private String httpNonProxyHosts = "";

    public FluxionServiceClient() {
    }

    public FluxionServiceClient(URL serviceURL) throws PussycatException {
        try {
            if (isUp(proxiedURLConnection(serviceURL))) {
                upServiceURLs.add(serviceURL);
            } else {
                downServiceURLs.add(serviceURL);
            }
        } catch (IOException ioe) {
            throw new PussycatHTMLException("Could not create FluxionServiceClient from URL: " + serviceURL, ioe.getMessage());
        }
    }

    public FluxionServiceClient(Set<URL> serviceURLs) {
        this.serviceURLs = serviceURLs;
        for (URL serviceURL : serviceURLs) {
            try {
                if (isUp(proxiedURLConnection(serviceURL))) {
                    upServiceURLs.add(serviceURL);
                } else {
                    downServiceURLs.add(serviceURL);
                }
            } catch (IOException ioe) {
                throw new PussycatHTMLException("Could not create FluxionServiceClient from URL: " + serviceURL, ioe.getMessage());
            }
        }
    }

    public void destroy() {
        upServiceURLs = null;
        downServiceURLs = null;
        cachedDatasources = null;
        cachedSchemas = null;
        serviceURLs = null;
    }

    public Set<URL> getAllServiceURLs() {
        return this.serviceURLs;
    }

    public Set<URL> getUpServiceURLs() {
        return this.upServiceURLs;
    }

    public Set<URL> getDownServiceURLs() {
        return this.downServiceURLs;
    }

    public void addService(URL serviceURL) {
        if (!this.upServiceURLs.contains(serviceURL) && !this.downServiceURLs.contains(serviceURL)) {
            try {
                if (isUp(proxiedURLConnection(serviceURL))) {
                    upServiceURLs.add(serviceURL);
                } else {
                    downServiceURLs.add(serviceURL);
                }
            } catch (IOException ioe) {
                throw new PussycatHTMLException("Could not create FluxionServiceClient from URL: " + serviceURL, ioe.getMessage());
            }
        }
    }

    public Set<URI> getDataSources(URL serviceURL) {
        return null;
    }

    public Set<URI> getCachedDataSources() {
        return this.cachedDatasources;
    }

    public Set<URI> getCachedDataSources(URL serviceURL) {
        return this.cachedDatasources;
    }

    public Set<String> getDataSourcesNames(URL serviceURL) {
        return null;
    }

    public Set<OWLOntology> getCachedSchemas() {
        return this.cachedSchemas;
    }

    public Set<URI> getSchemaURIs(URL serviceURL) {
        return null;
    }

    public OWLOntology getSchema(URL serviceURL, URI uri) {
        return null;
    }

    public void setSessionProxy(String proxyHost, int proxyPort, String nonProxyHosts) {
        this.httpProxy = proxyHost;
        this.httpProxyPort = proxyPort;
        this.httpNonProxyHosts = nonProxyHosts;
    }

    public HttpURLConnection proxiedURLConnection(URL url) throws IOException, PussycatException {
        HttpURLConnection uc = null;
        if (this.httpProxy == null || this.httpProxy.equals("") || PussycatUtils.isLocalURL(url.toString())) {
            System.getProperties().put("proxySet", "false");
        } else {
            System.getProperties().put("proxySet", "true");
        }
        if (System.getProperties().getProperty("proxySet").equals("true")) {
            uc = (java.net.HttpURLConnection) url.openConnection(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress(this.httpProxy, this.httpProxyPort)));
        } else {
            uc = (java.net.HttpURLConnection) url.openConnection();
        }
        return uc;
    }

    public boolean isUp(HttpURLConnection uc) throws IOException {
        if (uc != null) {
            uc.connect();
            if (uc.getResponseCode() == 200) {
                return true;
            }
        }
        return false;
    }
}
