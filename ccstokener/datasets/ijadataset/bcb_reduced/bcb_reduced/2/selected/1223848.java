package org.knopflerfish.bundle.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.http.HttpContext;

public class ServletContextImpl implements ServletContext {

    private final HttpContext httpContext;

    private final String realPath;

    private final HttpConfig httpConfig;

    private final LogRef log;

    private final Registrations registrations;

    private final Attributes attributes = new Attributes();

    ServletContextImpl(final HttpContext httpContext, final String realPath, final HttpConfig httpConfig, final LogRef log, final Registrations registrations) {
        this.httpContext = httpContext;
        this.realPath = realPath;
        this.httpConfig = httpConfig;
        this.log = log;
        this.registrations = registrations;
    }

    public ServletContext getContext(final String uri) {
        return null;
    }

    public String getContextPath() {
        System.err.println("***NYI." + getClass().getName());
        throw new RuntimeException("NYI");
    }

    public String getServletContextName() {
        System.err.println("***NYI." + getClass().getName());
        throw new RuntimeException("NYI");
    }

    public Set getResourcePaths(String path) {
        System.err.println("***NYI." + getClass().getName());
        throw new RuntimeException("NYI");
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 5;
    }

    public String getMimeType(final String file) {
        String mimeType = httpContext.getMimeType(file);
        if (mimeType == null) mimeType = httpConfig.getMimeType(file);
        return mimeType;
    }

    public URL getResource(final String path) {
        return httpContext.getResource(realPath + path);
    }

    public InputStream getResourceAsStream(final String path) {
        final URL url = getResource(path);
        if (url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException ioe) {
            return null;
        }
    }

    public RequestDispatcher getRequestDispatcher(final String uri) {
        return registrations.getRequestDispatcher(uri);
    }

    public RequestDispatcher getNamedDispatcher(final String name) {
        return null;
    }

    public Servlet getServlet(final String name) {
        return null;
    }

    public Enumeration getServlets() {
        return HttpUtil.EMPTY_ENUMERATION;
    }

    public Enumeration getServletNames() {
        return HttpUtil.EMPTY_ENUMERATION;
    }

    public void log(final String message) {
        if (log.doInfo()) log.info(message);
    }

    public void log(final Exception exception, final String message) {
        log(message, exception);
    }

    public void log(final String message, final Throwable throwable) {
        if (log.doWarn()) log.warn(message, throwable);
    }

    public String getRealPath(final String path) {
        return null;
    }

    public String getServerInfo() {
        return httpConfig.getServerInfo();
    }

    public String getInitParameter(final String name) {
        return null;
    }

    public Enumeration getInitParameterNames() {
        return HttpUtil.EMPTY_ENUMERATION;
    }

    public Object getAttribute(final String name) {
        return attributes.getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return attributes.getAttributeNames();
    }

    public void setAttribute(final String name, final Object value) {
        attributes.setAttribute(name, value);
    }

    public void removeAttribute(final String name) {
        attributes.removeAttribute(name);
    }
}
