package com.volantis.osgi.j2ee.bridge.http.service;

import org.osgi.service.http.HttpContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * The servlet context that wraps a {@link HttpContext} and is given to any
 * registered servlets.
 */
public class ServletContextImpl implements InternalServletContext {

    /**
     * The servlet context from the outermost container.
     */
    private final ServletContext containerContext;

    /**
     * The context supplied when registered.
     */
    private final HttpContext httpContext;

    /**
     * The set of attributes associated with this servlet context.
     */
    private final Hashtable attributes;

    /**
     * The count of the number of usages.
     */
    private int useCount;

    /**
     * Initialise.
     *
     * @param containerContext The servlet context from the outermost container.
     * @param httpContext      The context supplied when registered.
     */
    public ServletContextImpl(ServletContext containerContext, HttpContext httpContext) {
        this.containerContext = containerContext;
        this.httpContext = httpContext;
        attributes = new Hashtable();
    }

    public ServletContext getContext(String name) {
        throw new UnsupportedOperationException();
    }

    public int getMajorVersion() {
        throw new UnsupportedOperationException();
    }

    public int getMinorVersion() {
        throw new UnsupportedOperationException();
    }

    public String getMimeType(String name) {
        String mimeType = httpContext.getMimeType(name);
        if (mimeType == null) {
            mimeType = containerContext.getMimeType(name);
        }
        return mimeType;
    }

    public Set getResourcePaths(String name) {
        throw new UnsupportedOperationException();
    }

    public URL getResource(String name) throws MalformedURLException {
        return httpContext.getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        InputStream is = null;
        try {
            URL url = getResource(name);
            if (url != null) {
                is = url.openStream();
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return is;
    }

    public RequestDispatcher getRequestDispatcher(String name) {
        throw new UnsupportedOperationException();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException();
    }

    public Servlet getServlet(String name) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public Enumeration getServlets() {
        throw new UnsupportedOperationException();
    }

    public Enumeration getServletNames() {
        throw new UnsupportedOperationException();
    }

    public void log(String message) {
    }

    public void log(Exception exception, String message) {
    }

    public void log(String message, Throwable throwable) {
    }

    public String getRealPath(String name) {
        return null;
    }

    public String getServerInfo() {
        throw new UnsupportedOperationException();
    }

    public String getInitParameter(String name) {
        throw new UnsupportedOperationException();
    }

    public Enumeration getInitParameterNames() {
        throw new UnsupportedOperationException();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        return attributes.keys();
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getServletContextName() {
        throw new UnsupportedOperationException();
    }

    public synchronized void incrementUseCount() {
        useCount += 1;
    }

    public synchronized boolean decrementUseCount() {
        useCount -= 1;
        return useCount == 0;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }
}
