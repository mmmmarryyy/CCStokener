package org.eclipse.equinox.http.servlet.internal;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.*;
import java.util.*;
import javax.servlet.*;
import org.osgi.service.http.HttpContext;

public class ServletContextAdaptor implements ServletContext {

    private ServletContext servletContext;

    HttpContext httpContext;

    private AccessControlContext acc;

    private ProxyContext proxyContext;

    public ServletContextAdaptor(ProxyContext proxyContext, ServletContext servletContext, HttpContext httpContext, AccessControlContext acc) {
        this.servletContext = servletContext;
        this.httpContext = httpContext;
        this.acc = acc;
        this.proxyContext = proxyContext;
    }

    /**
	 * 	@see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
	 * 
	 * This method was added in the Servlet 2.3 API however the OSGi HttpService currently does not provide
	 * support for this method in the HttpContext interface. To support "getResourcePaths(...) this
	 * implementation uses reflection to check for and then call the associated HttpContext.getResourcePaths(...)
	 * method opportunistically. Null is returned if the method is not present or fails.
	 */
    public Set getResourcePaths(String name) {
        if (name == null || !name.startsWith("/")) return null;
        try {
            Method getResourcePathsMethod = httpContext.getClass().getMethod("getResourcePaths", new Class[] { String.class });
            if (!getResourcePathsMethod.isAccessible()) getResourcePathsMethod.setAccessible(true);
            return (Set) getResourcePathsMethod.invoke(httpContext, new Object[] { name });
        } catch (Exception e) {
        }
        return null;
    }

    public Object getAttribute(String attributeName) {
        Dictionary attributes = proxyContext.getContextAttributes(httpContext);
        return attributes.get(attributeName);
    }

    public Enumeration getAttributeNames() {
        Dictionary attributes = proxyContext.getContextAttributes(httpContext);
        return attributes.keys();
    }

    public void setAttribute(String attributeName, Object attributeValue) {
        Dictionary attributes = proxyContext.getContextAttributes(httpContext);
        attributes.put(attributeName, attributeValue);
    }

    public void removeAttribute(String attributeName) {
        Dictionary attributes = proxyContext.getContextAttributes(httpContext);
        attributes.remove(attributeName);
    }

    public String getMimeType(String name) {
        String mimeType = httpContext.getMimeType(name);
        return (mimeType != null) ? mimeType : servletContext.getMimeType(name);
    }

    public URL getResource(final String name) {
        try {
            return (URL) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws Exception {
                    return httpContext.getResource(name);
                }
            }, acc);
        } catch (PrivilegedActionException e) {
            log(e.getException().getMessage(), e.getException());
        }
        return null;
    }

    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url != null) {
            try {
                return url.openStream();
            } catch (IOException e) {
                log("Error opening stream for resource '" + name + "'", e);
            }
        }
        return null;
    }

    public ServletContext getContext(String arg0) {
        return servletContext.getContext(arg0);
    }

    public String getInitParameter(String arg0) {
        return servletContext.getInitParameter(arg0);
    }

    public Enumeration getInitParameterNames() {
        return servletContext.getInitParameterNames();
    }

    public int getMajorVersion() {
        return servletContext.getMajorVersion();
    }

    public int getMinorVersion() {
        return servletContext.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String arg0) {
        return new RequestDispatcherAdaptor(servletContext.getNamedDispatcher(arg0));
    }

    public String getRealPath(String arg0) {
        return servletContext.getRealPath(arg0);
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        return new RequestDispatcherAdaptor(servletContext.getRequestDispatcher(proxyContext.getServletPath() + arg0));
    }

    public String getServerInfo() {
        return servletContext.getServerInfo();
    }

    /**@deprecated*/
    public Servlet getServlet(String arg0) throws ServletException {
        return servletContext.getServlet(arg0);
    }

    public String getServletContextName() {
        return servletContext.getServletContextName();
    }

    /**@deprecated*/
    public Enumeration getServletNames() {
        return servletContext.getServletNames();
    }

    /**@deprecated*/
    public Enumeration getServlets() {
        return servletContext.getServlets();
    }

    /**@deprecated*/
    public void log(Exception arg0, String arg1) {
        servletContext.log(arg0, arg1);
    }

    public void log(String arg0, Throwable arg1) {
        servletContext.log(arg0, arg1);
    }

    public void log(String arg0) {
        servletContext.log(arg0);
    }

    public String getContextPath() {
        try {
            Method getContextPathMethod = servletContext.getClass().getMethod("getContextPath", null);
            return (String) getContextPathMethod.invoke(servletContext, null) + proxyContext.getServletPath();
        } catch (Exception e) {
        }
        return null;
    }
}
