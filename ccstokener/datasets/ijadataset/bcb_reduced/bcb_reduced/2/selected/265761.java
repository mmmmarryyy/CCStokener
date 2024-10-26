package org.astrogrid.clustertool;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.httpd.UtilServer;

/**
 * Provides HTTP server functionality for ClusterTool.
 * This includes a web server for dynamically generated content and an
 * XML-RPC server for use with SAMP.
 * This class is a singleton.
 *
 * @author   Mark Taylor
 * @author Paul Harrison
 */
public class ClusterToolServer {

    private final UtilServer utilServer_;

    private final ClientProfile profile_;

    private final ResourceHandler resourceHandler_;

    private static ClusterToolServer instance_;

    private static final int BUFSIZ = 16 * 1024;

    /** logger for this class */
    private static final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(ClusterToolServer.class);

    /**
     * Private constructor constructs sole instance.
     */
    private ClusterToolServer() throws IOException {
        utilServer_ = UtilServer.getInstance();
        profile_ = DefaultClientProfile.getProfile();
        HttpServer httpServer = utilServer_.getServer();
        resourceHandler_ = new ResourceHandler(httpServer, utilServer_.getBasePath("/dynamic"));
        httpServer.addHandler(resourceHandler_);
    }

    /**
     * Returns a SAMP client profile.
     *
     * @return   profile
     */
    public ClientProfile getProfile() {
        return profile_;
    }

    /**
     * Makes a resource available for retrieving from this internal HTTP server.
     * A <code>name</code> may be supplied which will appear at the end of
     * the URL, but this is just for cosmetic purposes.  The URL at which
     * the resource is available will provided as the return value.
     *
     * @param   name   filename identifying the resource
     * @param   resource   resource to make available
     * @return    URL at which <code>resource</code> can be found
     */
    public URL addResource(String name, ServerResource resource) {
        return resourceHandler_.addResource(name == null ? "" : name, resource);
    }

    /**
     * Removes a resource from this server.
     *
     * @param  url  URL returned by a previous addResource call
     */
    public void removeResource(URL url) {
        resourceHandler_.removeResource(url);
    }

    /**
     * Indicates whether this server can serve the resource with a given URL.
     *
     * @param   url  URL to enquire about
     * @return   true if a request for <code>url</code> will complete with
     *           non-error status
     */
    public boolean isFound(URL url) {
        try {
            URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection hconn = (HttpURLConnection) connection;
                hconn.setRequestMethod("HEAD");
                hconn.setDoOutput(false);
                hconn.connect();
                InputStream in = connection.getInputStream();
                byte[] buf = new byte[BUFSIZ];
                while (in.read(buf) >= 0) {
                }
                in.close();
                return hconn.getResponseCode() == 200;
            } else {
                connection.connect();
                InputStream in = connection.getInputStream();
                byte[] buf = new byte[BUFSIZ];
                while (in.read(buf) >= 0) {
                }
                in.close();
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static ClusterToolServer getInstance() {
        if (instance_ == null) {
            synchronized (ClusterToolServer.class) {
                if (instance_ == null) {
                    try {
                        instance_ = new ClusterToolServer();
                    } catch (IOException e) {
                        logger.error("cannot create SAMP server", e);
                    }
                }
            }
        }
        return instance_;
    }
}
