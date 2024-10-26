package com.migniot.streamy.proxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import com.migniot.streamy.core.Category;
import com.migniot.streamy.core.CheckListener;
import com.migniot.streamy.core.DownloadListener;
import com.migniot.streamy.core.MediaListener;
import com.migniot.streamy.core.MediaReference;
import com.migniot.streamy.core.Metadata;
import com.migniot.streamy.core.ProcessingListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class ProxyPlugin extends AbstractUIPlugin implements ProcessingListener {

    /**
	 * The logger.
	 */
    private static Logger LOGGER = Logger.getLogger(ProxyPlugin.class);

    /**
	 * The plug-in ID
	 */
    public static final String PLUGIN_ID = "com.migniot.streamy.Proxy";

    /**
	 * The shared instance
	 */
    private static ProxyPlugin plugin;

    /**
	 * The proxy server.
	 */
    private Server proxy;

    /**
	 * The media listeners
	 */
    private List<MediaListener> mediaListeners;

    /**
	 * The download listeners
	 */
    private List<DownloadListener> downloadListeners;

    /**
	 * The constructor
	 */
    public ProxyPlugin() {
        proxy = null;
        mediaListeners = new ArrayList<MediaListener>();
        downloadListeners = new ArrayList<DownloadListener>();
    }

    /**
	 * {@inheritDoc}
	 */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /**
	 * {@inheritDoc}
	 */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
    public static ProxyPlugin getDefault() {
        return plugin;
    }

    /**
	 * Perform checks
	 */
    public void check(CheckListener listener) {
        checkNetworkBind(listener);
        checkContainerFeatures(listener);
        checkProxyFeatures(listener);
    }

    /**
	 * Check binding to local host.
	 */
    private void checkNetworkBind(CheckListener listener) {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
        } catch (IOException ioe) {
            String message = "Failed to bind to network interface on ANY free port";
            LOGGER.error(message, ioe);
            listener.failed(Category.NETWORK, message, ioe);
            return;
        }
        listener.checked(Category.NETWORK, "Success binding to local host");
        final ServerSocket server = socket;
        Runnable acceptor = new Runnable() {

            public void run() {
                try {
                    doRun();
                } catch (IOException ioe) {
                    LOGGER.error("Server socket test failed with I/O", ioe);
                } catch (InterruptedException ie) {
                    LOGGER.error("Server socket test interrupted", ie);
                }
            }

            public void doRun() throws IOException, InterruptedException {
                byte[] buffer = new byte[2048];
                Socket client = server.accept();
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();
                while (in.available() <= 0) {
                    Thread.sleep(100);
                }
                int count = in.read(buffer);
                out.write(new StringBuffer("Hello ").append(new String(buffer, 0, count)).toString().getBytes());
            }
        };
        Thread thread = new Thread(acceptor);
        thread.start();
        Socket request = null;
        try {
            request = new Socket(socket.getInetAddress(), socket.getLocalPort());
        } catch (IOException ioe) {
            String message = "Client socket local connection test failed";
            LOGGER.error(message, ioe);
            listener.failed(Category.NETWORK, message, ioe);
            return;
        }
        listener.checked(Category.NETWORK, "Success requesting local server");
        OutputStream sent = null;
        InputStream received = null;
        byte[] buffer = new byte[2048];
        String response = null;
        try {
            sent = request.getOutputStream();
            received = request.getInputStream();
            sent.write("World".getBytes());
            int count = received.read(buffer);
            response = new String(buffer, 0, count);
        } catch (IOException ioe) {
            String message = "Client socket byte communication test failed";
            LOGGER.error(message, ioe);
            listener.failed(Category.NETWORK, message, ioe);
            return;
        }
        if ("Hello World".equals(response)) {
            listener.checked(Category.NETWORK, "Success reading local server response");
        } else {
            LOGGER.error(MessageFormat.format("Got erroneous response = [{0}]", response));
            listener.failed(Category.NETWORK, "Erroneous response read from local server", null);
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.error("Failed to shutdown local server", e);
        }
        socket = null;
    }

    /**
	 * Check container features.
	 *
	 * @param listener
	 *            The listener
	 * @return A server serving {@link HelloServlet#MESSAGE} at "/hello" URI
	 */
    private void checkContainerFeatures(CheckListener listener) {
        StreamyContextHandler handler = new StreamyContextHandler();
        handler.addServlet(HelloServlet.class, "/hello");
        Server server = new Server(0);
        server.setHandler(handler);
        try {
            server.start();
        } catch (Exception e) {
            String message = "Starting servlet container failed";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        }
        listener.checked(Category.PROXY, "Success starting servlet container");
        URL url = null;
        byte[] buffer = new byte[512];
        int count = -1;
        try {
            url = new URL("http://127.0.0.1:" + server.getConnectors()[0].getLocalPort() + "/hello");
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            count = stream.read(buffer);
            stream.close();
        } catch (MalformedURLException e) {
            String message = "Localhost 127.0.0.1 url is malformed by default";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        } catch (IOException e) {
            String message = "Servlet container did not communicate properly";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        }
        String response = new String(buffer, 0, count);
        if (!HelloServlet.MESSAGE.equals(response)) {
            String message = "Calling servlet container returns erroneous response";
            LOGGER.error(MessageFormat.format("Calling servlet container " + "returns erroneous response = [{0}]", message));
            listener.failed(Category.PROXY, message, null);
            return;
        }
        listener.checked(Category.PROXY, "Success calling servlet container");
        try {
            server.stop();
        } catch (Exception e) {
            LOGGER.error("Server.stop() failure", e);
        }
    }

    /**
	 * Check proxy features.
	 *
	 * @param server
	 *            The trivial hello server
	 * @param listener
	 *            The listener
	 */
    private void checkProxyFeatures(CheckListener listener) {
        Server server = null;
        try {
            server = newServletServer(0, WorldServlet.class, "/", false);
        } catch (Exception e) {
            String message = "Failed to start servlet trivial server";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        }
        int serverPort = server.getConnectors()[0].getLocalPort();
        Server proxyServer = null;
        try {
            proxyServer = newServletServer(0, ProxyServlet.class, "/", false);
        } catch (Exception e) {
            String message = "Failed to start servlet proxy server";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        }
        int proxyPort = proxyServer.getConnectors()[0].getLocalPort();
        listener.checked(Category.PROXY, "Success starting servlet proxy server");
        URL url = null;
        byte[] buffer = new byte[512];
        int count = -1;
        try {
            url = new URL("http://127.0.0.1:" + serverPort + "/world");
            URLConnection connection = url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort)));
            InputStream stream = connection.getInputStream();
            count = stream.read(buffer);
            stream.close();
        } catch (MalformedURLException e) {
            String message = "Localhost 127.0.0.1 url is malformed by default";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        } catch (IOException e) {
            String message = "Proxy servlet container did not communicate properly";
            LOGGER.error(message, e);
            listener.failed(Category.PROXY, message, e);
            return;
        }
        String response = new String(buffer, 0, count);
        if (!WorldServlet.MESSAGE.equals(response)) {
            LOGGER.error(MessageFormat.format("Calling WorldServlet " + "container returns " + "erroneous response = [{0}]", response));
            String message = "Calling WorldServlet " + "container returns erroneous response";
            listener.failed(Category.PROXY, message, null);
            return;
        }
        listener.checked(Category.PROXY, "Success routing through servlet proxy server");
        try {
            server.stop();
        } catch (Exception e) {
            LOGGER.error("Server.stop() failure", e);
        }
        try {
            proxyServer.stop();
        } catch (Exception e) {
            LOGGER.error("Proxy server.stop() failure", e);
        }
    }

    /**
	 * Create a new single servlet server.
	 *
	 * @param port
	 *            The port, use 0 to automatically select a free port
	 * @param klass
	 *            The servlet class
	 * @param path
	 *            The path specification
	 * @param useExtensions
	 * 			  If true, use servlet extensions
	 * @return The started server
	 * @throws Exception
	 *             Upon {@link Server#start()} failure
	 */
    private Server newServletServer(int port, Class<? extends Servlet> klass, String path, boolean useExtensions) throws Exception {
        StreamyContextHandler handler = new StreamyContextHandler();
        if (useExtensions) {
            IConfigurationElement[] configuration = Platform.getExtensionRegistry().getConfigurationElementsFor("com.migniot.streamy.filters");
            for (IConfigurationElement element : configuration) {
                Object extension = null;
                String filterPath = null;
                try {
                    extension = element.createExecutableExtension("class");
                    filterPath = element.getAttribute("path");
                    LOGGER.info(MessageFormat.format("Registered {0} filter at path = [{1}]", element.getContributor().getName(), filterPath));
                } catch (CoreException e) {
                    LOGGER.error(MessageFormat.format("Faulty plugin filter class = [{0}]", element.getAttribute("class")), e);
                }
                if (extension instanceof Filter) {
                    handler.addFilter(new FilterHolder((Filter) extension), filterPath, 0);
                }
            }
        }
        handler.addServlet(klass, path);
        Server server = new Server(port);
        server.setHandler(handler);
        server.setThreadPool(new QueuedThreadPool(getPluginPreferences().getInt(PreferenceInitializer.MAX_THREADS)));
        server.start();
        return server;
    }

    /**
	 * Return the running production proxy, starting it if required.
	 *
	 * @return The running production proxy, starting it if required.
	 */
    public Server getProxy() {
        if (this.proxy == null) {
            synchronized (this) {
                if (this.proxy == null) {
                    int port = getPort();
                    try {
                        this.proxy = newServletServer(port, ProductionProxyServlet.class, "/", true);
                    } catch (Exception e) {
                        LOGGER.error(MessageFormat.format("Failed to start PRODUCTION ", "proxy server on port = [{0}]", port), e);
                    }
                }
            }
        }
        return this.proxy;
    }

    /**
	 * Return the proxy port.
	 *
	 * @return The proxy port
	 */
    public int getPort() {
        return getPluginPreferences().getInt(PreferenceInitializer.PROXY_PORT);
    }

    /**
	 * Add a media listener.
	 *
	 * @param listener
	 *            The listener
	 */
    public void addMediaListener(MediaListener listener) {
        this.mediaListeners.add(listener);
    }

    /**
	 * Add a media download listener.
	 *
	 * @param listener
	 *            The listener
	 */
    public void addDownloadListener(DownloadListener listener) {
        this.downloadListeners.add(listener);
    }

    /**
	 * Handle media candidate start.
	 *
	 * Find a classifier for the media file, store a reference for delayed
	 * classification, leave it untouched or remove it.
	 *
	 * @param request
	 *            The client request
	 * @param response
	 *            The client response
	 * @param proxyRequest
	 *            The server request
	 * @param proxyResponse
	 *            The server response
	 * @param backup
	 *            The unpacked saved media download
	 */
    public void acknowledgeStart(HttpServletRequest request, HttpServletResponse response, HttpRequest proxyRequest, HttpResponse proxyResponse, File backup) {
        if (!mediaListeners.isEmpty()) {
            Metadata metadata = new DownloadMetadata(request, response, proxyRequest, proxyResponse);
            for (MediaListener mediaListener : mediaListeners) {
                Metadata streamMetadata = mediaListener.acknowledgeStart(metadata, backup);
                if ((streamMetadata != null) && !downloadListeners.isEmpty()) {
                    MediaReference reference = new MediaReference(streamMetadata, backup);
                    for (DownloadListener downloadListener : downloadListeners) {
                        downloadListener.notifyStart(reference);
                    }
                }
            }
        }
    }

    /**
	 * Handle media candidate.
	 *
	 * Find a classifier for the media file, store a reference for delayed
	 * classification, leave it untouched or remove it.
	 *
	 * @param request
	 *            The client request
	 * @param response
	 *            The client response
	 * @param proxyRequest
	 *            The server request
	 * @param proxyResponse
	 *            The server response
	 * @param backup
	 *            The unpacked saved media download
	 */
    void acknowledge(HttpServletRequest request, HttpServletResponse response, HttpRequest proxyRequest, HttpResponse proxyResponse, File backup) {
        if (!mediaListeners.isEmpty()) {
            Metadata metadata = new DownloadMetadata(request, response, proxyRequest, proxyResponse);
            for (MediaListener listener : mediaListeners) {
                MediaReference reference = listener.acknowledge(metadata, backup, this);
                if ((reference != null) && !downloadListeners.isEmpty()) {
                    for (DownloadListener downloadListener : downloadListeners) {
                        downloadListener.notifyEnd(reference);
                    }
                }
            }
        }
    }

    /**
	 * Handle metadata candidate.
	 *
	 * Create a classification when relevant and run a delayed classification
	 * pass on that case.
	 *
	 * @param request
	 *            The client request
	 * @param response
	 *            The client response
	 * @param proxyRequest
	 *            The server request
	 * @param proxyResponse
	 *            The server response
	 * @param memoryStream
	 *            The unpacked content
	 */
    void classify(HttpServletRequest request, HttpServletResponse response, HttpRequest proxyRequest, HttpResponse proxyResponse, ByteArrayOutputStream memoryStream) {
        if (!mediaListeners.isEmpty()) {
            Metadata metadata = new DownloadMetadata(request, response, proxyRequest, proxyResponse);
            for (MediaListener listener : mediaListeners) {
                listener.classify(metadata, memoryStream);
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void notifyConversion(MediaReference reference) {
        for (DownloadListener downloadListener : downloadListeners) {
            downloadListener.notifyConversion(reference);
        }
    }
}
