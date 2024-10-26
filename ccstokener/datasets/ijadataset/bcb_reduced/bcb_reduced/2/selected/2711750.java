package org.apache.myfaces.trinidad.webapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.trinidad.config.Configurator;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;
import org.apache.myfaces.trinidad.resource.CachingResourceLoader;
import org.apache.myfaces.trinidad.resource.DirectoryResourceLoader;
import org.apache.myfaces.trinidad.resource.ResourceLoader;
import org.apache.myfaces.trinidad.resource.ServletContextResourceLoader;
import org.apache.myfaces.trinidad.util.URLUtils;

public class ResourceServlet extends HttpServlet {

    /**
   * 
   */
    private static final long serialVersionUID = 4547362994406585148L;

    /**
   * Override of Servlet.destroy();
   */
    @Override
    public void destroy() {
        _loaders = null;
        _facesContextFactory = null;
        _lifecycle = null;
        super.destroy();
    }

    /**
   * Override of Servlet.init();
   */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            _facesContextFactory = (FacesContextFactory) FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        } catch (FacesException e) {
            Throwable rootCause = e.getCause();
            if (rootCause == null) {
                throw e;
            } else {
                throw new ServletException(e.getMessage(), rootCause);
            }
        }
        _lifecycle = new _ResourceLifecycle();
        _initDebug(config);
        _loaders = new HashMap<String, ResourceLoader>();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        boolean hasFacesContext = false;
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            hasFacesContext = true;
        } else {
            Configurator.disableConfiguratorServices(request);
            context = _facesContextFactory.getFacesContext(getServletContext(), request, response, _lifecycle);
        }
        try {
            super.service(request, response);
        } catch (ServletException e) {
            _LOG.severe(e);
            throw e;
        } catch (IOException e) {
            if (!_canIgnore(e)) _LOG.severe(e);
            throw e;
        } finally {
            if (!hasFacesContext) context.release();
        }
    }

    /**
   * Override of HttpServlet.doGet()
   */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ResourceLoader loader = _getResourceLoader(request);
        String resourcePath = getResourcePath(request);
        URL url = loader.getResource(resourcePath);
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        _setHeaders(connection, response);
        InputStream in = connection.getInputStream();
        OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[_BUFFER_SIZE];
        try {
            _pipeBytes(in, out, buffer);
        } finally {
            try {
                in.close();
            } finally {
                out.close();
            }
        }
    }

    /**
   * Override of HttpServlet.getLastModified()
   */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        try {
            ResourceLoader loader = _getResourceLoader(request);
            String resourcePath = getResourcePath(request);
            URL url = loader.getResource(resourcePath);
            if (url == null) return super.getLastModified(request);
            return URLUtils.getLastModified(url);
        } catch (IOException e) {
            return super.getLastModified(request);
        }
    }

    /**
   * Returns the resource path from the http servlet request.
   *
   * @param request  the http servlet request
   *
   * @return the resource path
   */
    protected String getResourcePath(HttpServletRequest request) {
        return request.getServletPath() + request.getPathInfo();
    }

    /**
   * Returns the resource loader for the requested servlet path.
   */
    private ResourceLoader _getResourceLoader(HttpServletRequest request) {
        final String servletPath = request.getServletPath();
        ResourceLoader loader = _loaders.get(servletPath);
        if (loader == null) {
            try {
                String key = "META-INF/servlets/resources" + servletPath + ".resources";
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                URL url = cl.getResource(key);
                if (url != null) {
                    Reader r = new InputStreamReader(url.openStream());
                    BufferedReader br = new BufferedReader(r);
                    try {
                        String className = br.readLine();
                        if (className != null) {
                            className = className.trim();
                            Class<?> clazz = cl.loadClass(className);
                            try {
                                Constructor<?> decorator = clazz.getConstructor(_DECORATOR_SIGNATURE);
                                ServletContext context = getServletContext();
                                File tempdir = (File) context.getAttribute("javax.servlet.context.tempdir");
                                ResourceLoader delegate = new DirectoryResourceLoader(tempdir);
                                loader = (ResourceLoader) decorator.newInstance(new Object[] { delegate });
                            } catch (InvocationTargetException e) {
                                loader = (ResourceLoader) clazz.newInstance();
                            } catch (NoSuchMethodException e) {
                                loader = (ResourceLoader) clazz.newInstance();
                            }
                        }
                    } finally {
                        br.close();
                    }
                } else {
                    _LOG.warning("Unable to find ResourceLoader for ResourceServlet" + " at servlet path:{0}" + "\nCause: Could not find resource:{1}", new Object[] { servletPath, key });
                    loader = new ServletContextResourceLoader(getServletContext()) {

                        @Override
                        public URL getResource(String path) throws IOException {
                            return super.getResource(path);
                        }
                    };
                }
                if (!_debug) loader = new CachingResourceLoader(loader);
            } catch (IllegalAccessException e) {
                loader = ResourceLoader.getNullResourceLoader();
            } catch (InstantiationException e) {
                loader = ResourceLoader.getNullResourceLoader();
            } catch (ClassNotFoundException e) {
                loader = ResourceLoader.getNullResourceLoader();
            } catch (IOException e) {
                loader = ResourceLoader.getNullResourceLoader();
            }
            _loaders.put(servletPath, loader);
        }
        return loader;
    }

    /**
   * Reads the specified input stream into the provided byte array storage and
   * writes it to the output stream.
   */
    private static void _pipeBytes(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int length;
        while ((length = (in.read(buffer))) >= 0) {
            out.write(buffer, 0, length);
        }
    }

    /**
   * Initialize whether resource debug mode is enabled.
   */
    private void _initDebug(ServletConfig config) {
        String debug = config.getInitParameter(DEBUG_INIT_PARAM);
        if (debug == null) {
            debug = config.getServletContext().getInitParameter(DEBUG_INIT_PARAM);
        }
        _debug = "true".equalsIgnoreCase(debug);
        if (_debug) {
            _LOG.info("RESOURCESERVLET_IN_DEBUG_MODE", DEBUG_INIT_PARAM);
        }
    }

    /**
   * Sets HTTP headers on the response which tell
   * the browser to cache the resource indefinitely.
   */
    private void _setHeaders(URLConnection connection, HttpServletResponse response) {
        String contentType = connection.getContentType();
        if (contentType == null || "content/unknown".equals(contentType)) {
            URL url = connection.getURL();
            String resourcePath = url.getPath();
            if (resourcePath.endsWith(".css")) contentType = "text/css"; else if (resourcePath.endsWith(".js")) contentType = "application/x-javascript"; else contentType = getServletContext().getMimeType(resourcePath);
        }
        response.setContentType(contentType);
        int contentLength = connection.getContentLength();
        if (contentLength >= 0) response.setContentLength(contentLength);
        long lastModified;
        try {
            lastModified = URLUtils.getLastModified(connection);
        } catch (IOException exception) {
            lastModified = -1;
        }
        if (lastModified >= 0) response.setDateHeader("Last-Modified", lastModified);
        if (!_debug) {
            response.setHeader("Cache-Control", "Public");
            long currentTime = System.currentTimeMillis();
            response.setDateHeader("Expires", currentTime + ONE_YEAR_MILLIS);
        }
    }

    private static boolean _canIgnore(Throwable t) {
        if (t instanceof InterruptedIOException) {
            return true;
        } else if (t instanceof SocketException) {
            return true;
        } else if (t instanceof IOException) {
            String message = t.getMessage();
            if ((message != null) && ((message.indexOf("Broken pipe") >= 0) || (message.indexOf("abort") >= 0))) return true;
        }
        return false;
    }

    private static class _ResourceLifecycle extends Lifecycle {

        @Override
        public void execute(FacesContext p0) throws FacesException {
        }

        @Override
        public PhaseListener[] getPhaseListeners() {
            return null;
        }

        @Override
        public void removePhaseListener(PhaseListener p0) {
        }

        @Override
        public void render(FacesContext p0) throws FacesException {
        }

        @Override
        public void addPhaseListener(PhaseListener p0) {
        }
    }

    /**
   * Context parameter for activating debug mode, which will disable
   * caching.
   */
    public static final String DEBUG_INIT_PARAM = "org.apache.myfaces.trinidad.resource.DEBUG";

    public static final long ONE_YEAR_MILLIS = 31363200000L;

    private static final Class[] _DECORATOR_SIGNATURE = new Class[] { ResourceLoader.class };

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(ResourceServlet.class);

    private static final int _BUFFER_SIZE = 2048;

    private boolean _debug;

    private Map<String, ResourceLoader> _loaders;

    private FacesContextFactory _facesContextFactory;

    private Lifecycle _lifecycle;
}
