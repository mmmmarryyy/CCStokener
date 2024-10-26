package net.sourceforge.javautil.web.server.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import net.sourceforge.javautil.common.StringUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualDirectory;
import net.sourceforge.javautil.web.server.IWebServer;

/**
 * A servlet context that will use the {@link IWebApplicationResourceResolver}
 * for resolving resources. {@link IWebServer} implementations must use this
 * to wrap any technology specific servlet contexts exposed.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: WebApplicationServletContext.java 2475 2010-10-24 13:30:13Z ponderator $
 */
@SuppressWarnings("deprecation")
public class WebApplicationServletContext implements ServletContext {

    protected final ServletContext original;

    protected final IWebApplication application;

    protected final IWebApplicationResourceResolver resolver;

    public WebApplicationServletContext(ServletContext original, IWebApplication application) {
        this.original = original;
        this.application = application;
        this.resolver = application.getResourceResolver();
    }

    public ServletContext getContext(String path) {
        return original.getContext(path);
    }

    public String getContextPath() {
        return original.getContextPath();
    }

    public String getInitParameter(String name) {
        return original.getInitParameter(name);
    }

    public Enumeration getInitParameterNames() {
        return original.getInitParameterNames();
    }

    public int getMajorVersion() {
        return original.getMajorVersion();
    }

    public String getMimeType(String filename) {
        return original.getMimeType(filename);
    }

    public int getMinorVersion() {
        return original.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return original.getNamedDispatcher(name);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return original.getRequestDispatcher(path);
    }

    public String getRealPath(String path) {
        File file = this.resolver.getFile(path);
        if (file != null) return file.getAbsolutePath();
        String op = original.getRealPath(path);
        return op == null ? null : op;
    }

    public URL getResource(String path) throws MalformedURLException {
        return this.getResource(path, true);
    }

    public URL getResource(String path, boolean tryOriginal) throws MalformedURLException {
        if ("/WEB-INF/web.xml".equals(path)) return this.createWebXmlURL();
        URL url = tryOriginal ? original.getResource(path) : null;
        if (url == null) url = resolver.getResource(path);
        return url;
    }

    public InputStream getResourceAsStream(String path) {
        try {
            URL url = this.getResource(path, false);
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    public Set getResourcePaths(String path) {
        Set set = new HashSet();
        Set ors = original.getResourcePaths(path);
        if (ors != null) set.addAll(ors);
        List<String> rrs = this.resolver.getResourcePaths(path);
        if (rrs != null) set.addAll(rrs);
        return set;
    }

    public String getServerInfo() {
        return original.getServerInfo();
    }

    public Servlet getServlet(String name) throws ServletException {
        return original.getServlet(name);
    }

    public String getServletContextName() {
        return original.getServletContextName();
    }

    public Enumeration getServletNames() {
        return original.getServletNames();
    }

    public Enumeration getServlets() {
        return original.getServlets();
    }

    public void log(Exception e, String message) {
        original.log(e, message);
    }

    public void log(String message, Throwable e) {
        original.log(message, e);
    }

    public void log(String message) {
        original.log(message);
    }

    public void removeAttribute(String name) {
        original.removeAttribute(name);
    }

    public void setAttribute(String name, Object value) {
        original.setAttribute(name, value);
    }

    public Object getAttribute(String name) {
        return original.getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return original.getAttributeNames();
    }

    public URL createWebXmlURL() {
        try {
            return new URL("internal", null, 0, "web.xml", new WebXmlHandler());
        } catch (MalformedURLException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    public class WebXmlHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new WebXmlConnection(url);
        }
    }

    public class WebXmlConnection extends URLConnection {

        public WebXmlConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(application.getWebXmlAsXml().getBytes());
        }
    }
}
