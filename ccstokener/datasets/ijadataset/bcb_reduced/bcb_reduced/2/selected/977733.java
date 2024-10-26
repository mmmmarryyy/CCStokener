package org.allcolor.ywt.adapter.web;

import org.allcolor.eu.medsea.util.MimeUtil;
import org.allcolor.xml.parser.CShaniDomParser;
import org.allcolor.alc.filesystem.Directory;
import org.allcolor.alc.filesystem.File;
import org.allcolor.alc.filesystem.FileSystem;
import org.allcolor.alc.filesystem.FileSystemType;
import org.allcolor.ywt.filter.CContext;
import org.allcolor.ywt.i18n.CResourceBundle;
import org.allcolor.ywt.jdbc.pool.JDBCPooler;
import org.allcolor.ywt.jndi.CJNDIContextSetup;
import org.allcolor.ywt.utils.LOGGERHelper;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

/**
 * 
DOCUMENT ME!
 *
 * @author Quentin Anciaux
 * @version 0.1.0
 */
@SuppressWarnings("unchecked")
public final class CServletContextWrapper implements ServletContext {

    /** DOCUMENT ME! */
    private final FileSystem fs;

    /** DOCUMENT ME! */
    private final List<ServletContextListener> contextListener = new ArrayList<ServletContextListener>(0);

    /** DOCUMENT ME! */
    private final Logger log;

    /** DOCUMENT ME! */
    private final Map<String, Object> attributes = new HashMap<String, Object>(0);

    /** DOCUMENT ME! */
    private final Map<String, String> parameters = new HashMap<String, String>(0);

    /** DOCUMENT ME! */
    private final Map<String, CHttpServlet> servlets = new HashMap<String, CHttpServlet>(0);

    /** DOCUMENT ME! */
    private final String name;

    /** DOCUMENT ME! */
    private URL webxml = null;

    private static volatile boolean hasPrintedLog4JWarning = false;

    /**
   * Creates a new CServletContextWrapper object.
   * 
   * @param fs
   *            DOCUMENT ME!
   * @param contextName
   *            DOCUMENT ME!
   */
    public CServletContextWrapper(final FileSystem fs, final String contextName) {
        this.fs = fs;
        this.name = contextName;
        CContext.getInstance().init(this);
        try {
            URL url = this.getResource("/WEB-INF/classes/log4j.properties");
            boolean ok = false;
            InputStream in = null;
            try {
                in = url.openStream();
                ok = true;
            } catch (Throwable e) {
            } finally {
                try {
                    if (in != null) in.close();
                } catch (Exception ignore) {
                }
            }
            if (ok) {
                PropertyConfigurator.configure(url);
            }
        } catch (final Throwable e) {
            if (!hasPrintedLog4JWarning) {
                hasPrintedLog4JWarning = true;
                System.err.println("!!! WARNING: /WEB-INF/classes/log4j.properties missing.");
            }
        }
        this.init();
        this.loadServletContextListener();
        this.log = LOGGERHelper.getLogger(this.getClass());
        CJNDIContextSetup.init(this);
        JDBCPooler.init(this);
        CResourceBundle.registerBundles(this);
        this.fireInitEvent();
    }

    /**
	 * DOCUMENT ME!
	 */
    public void destroy() {
        this.fireDestroyEvent();
        this.destroyServlets();
        this.deleteScratchDir(null);
        this.attributes.clear();
        this.parameters.clear();
        CResourceBundle.destroy();
        CJNDIContextSetup.destroy();
        try {
            FileSystem.getFileSystem(this.getServletContextName() + "_MEMORY").umount();
        } catch (final IOException ignore) {
            ;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public Object getAttribute(final String arg0) {
        try {
            synchronized (this.attributes) {
                final Object value = this.attributes.get(arg0);
                if (value != null) {
                    return value;
                }
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public Enumeration getAttributeNames() {
        try {
            final Vector<String> vector = new Vector<String>();
            synchronized (this.attributes) {
                for (final Object element : this.attributes.keySet()) {
                    vector.add((String) element);
                }
            }
            return vector.elements();
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public ServletContext getContext(final String arg0) {
        return this;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public String getInitParameter(final String arg0) {
        try {
            synchronized (this.parameters) {
                final String value = (String) this.parameters.get(arg0);
                if (value != null) {
                    return value;
                }
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public Enumeration getInitParameterNames() {
        try {
            final Vector<String> vector = new Vector<String>();
            synchronized (this.parameters) {
                for (final Object element : this.parameters.keySet()) {
                    vector.add((String) element);
                }
            }
            return vector.elements();
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public int getMajorVersion() {
        return 0;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public String getMimeType(final String arg0) {
        final List<String> result = MimeUtil.getMimeType(this.getResourceAsStream(arg0), arg0);
        if (result.size() > 0) {
            return result.get(0);
        }
        return "application/octet-stream";
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public int getMinorVersion() {
        return 0;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public RequestDispatcher getNamedDispatcher(final String arg0) {
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public String getRealPath(final String arg0) {
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public RequestDispatcher getRequestDispatcher(final String arg0) {
        return new CRequestDispatcher(arg0);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws MalformedURLException DOCUMENT ME!
	 */
    public URL getResource(final String arg0) throws MalformedURLException {
        try {
            if (arg0.indexOf("WEB-INF/web.xml") != -1) {
                return this.getWebXml();
            }
            try {
                final File file = this.fs.file(arg0);
                if (file.exists()) {
                    return file.toURL();
                }
            } catch (final IOException e) {
                ;
            }
            try {
                final Directory dir = this.fs.directory(arg0);
                if (dir.exists()) {
                    return dir.toURL();
                }
            } catch (final IOException e) {
                ;
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public InputStream getResourceAsStream(final String arg0) {
        try {
            return this.getResource(arg0).openStream();
        } catch (final IOException e) {
            return null;
        } catch (final NullPointerException e) {
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public Set getResourcePaths(final String arg0) {
        try {
            final TreeSet<String> set = new TreeSet<String>();
            final Directory dir = this.fs.directory(arg0);
            for (final Directory d : dir.getDirectories()) {
                set.add(d.getPath() + "/");
            }
            for (final File file : dir.getFiles()) {
                set.add(file.getPath());
            }
            if (set.size() == 0) {
                return null;
            }
            return set;
        } catch (final IOException e) {
            ;
        }
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public String getServerInfo() {
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws ServletException DOCUMENT ME!
	 */
    public Servlet getServlet(final String arg0) throws ServletException {
        synchronized (this.servlets) {
            final CHttpServlet hs = ((CHttpServlet) this.servlets.get(arg0));
            if (hs == null) {
                return null;
            }
            return hs.getServlet();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public String getServletContextName() {
        return this.name;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param uri DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public CHttpServlet getServletForUri(final String uri) {
        synchronized (this.servlets) {
            for (final Object element : this.servlets.entrySet()) {
                final Map.Entry entry = (Map.Entry) element;
                final CHttpServlet hs = (CHttpServlet) entry.getValue();
                if (hs.getMatch().matcher(uri).matches()) {
                    return hs;
                }
            }
        }
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public Enumeration getServletNames() {
        final Vector<String> vector = new Vector<String>();
        synchronized (this.servlets) {
            for (final Object element : this.servlets.keySet()) {
                vector.add((String) element);
            }
        }
        return vector.elements();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public Enumeration getServlets() {
        final Vector<Servlet> vector = new Vector<Servlet>();
        synchronized (this.servlets) {
            for (final Object element : this.servlets.entrySet()) {
                final Map.Entry entry = (Map.Entry) element;
                final CHttpServlet hs = (CHttpServlet) entry.getValue();
                vector.add(hs.getServlet());
            }
        }
        return vector.elements();
    }

    /**
	 * DOCUMENT ME!
	 */
    public void initAll() {
        this.loadServlets();
        this.initServlets();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 * @param arg1 DOCUMENT ME!
	 */
    public void log(final Exception arg0, final String arg1) {
        if (this.log != null) {
            this.log.info(arg1, arg0);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 */
    public void log(final String arg0) {
        if (this.log != null) {
            this.log.info(arg0);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 * @param arg1 DOCUMENT ME!
	 */
    public void log(final String arg0, final Throwable arg1) {
        if (this.log != null) {
            this.log.info(arg0, arg1);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param listener DOCUMENT ME!
	 */
    public synchronized void registerContextListener(final ServletContextListener listener) {
        if (!this.contextListener.contains(listener)) {
            this.contextListener.add(listener);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 */
    public void removeAttribute(final String arg0) {
        try {
            synchronized (this.attributes) {
                this.attributes.remove(arg0);
            }
        } catch (final Exception e) {
            ;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param listener DOCUMENT ME!
	 */
    public synchronized void removeContextListener(final ServletContextListener listener) {
        if (this.contextListener.contains(listener)) {
            this.contextListener.remove(listener);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 * @param arg1 DOCUMENT ME!
	 */
    public void setAttribute(final String arg0, final Object arg1) {
        synchronized (this.attributes) {
            this.attributes.put(arg0, arg1);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws Exception DOCUMENT ME!
	 */
    private byte[] createWebXml() throws Exception {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        buffer.append("<!DOCTYPE web-app PUBLIC\n");
        buffer.append("	\"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\n");
        buffer.append("	\"http://java.sun.com/dtd/web-app_2_3.dtd\">\n");
        buffer.append("<web-app>\n");
        buffer.append("	<!-- Context param -->\n");
        final CShaniDomParser parser = new CShaniDomParser();
        final Document scdoc = parser.parse(this.getResource("/WEB-INF/config/servletcontext.xml"));
        final NodeList scparam = scdoc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/context", "param");
        for (int i = 0; i < scparam.getLength(); i++) {
            final Element param = (Element) scparam.item(i);
            final String name = param.getAttribute("name");
            final String value = param.getTextContent().trim();
            buffer.append("	<context-param>\n");
            buffer.append("		<param-name>");
            buffer.append(name);
            buffer.append("</param-name>\n");
            buffer.append("		<param-value>");
            buffer.append(value);
            buffer.append("</param-value>\n");
            buffer.append("	</context-param>\n");
        }
        buffer.append("	\n");
        final Document fldoc = parser.parse(this.getResource("/WEB-INF/config/filter.config.xml"));
        final NodeList filters = fldoc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/filter", "filter");
        buffer.append("	<!-- Filter -->\n");
        final List<String> precFilter = new ArrayList<String>();
        for (int i = 0; i < filters.getLength(); i++) {
            final Element filter = (Element) filters.item(i);
            if (precFilter.contains(filter.getAttribute("name").intern())) {
                continue;
            }
            precFilter.add(filter.getAttribute("name").intern());
            buffer.append("	<filter>\n");
            buffer.append("		<filter-name>");
            buffer.append(filter.getAttribute("name"));
            buffer.append("</filter-name>\n");
            buffer.append("		<filter-class>");
            buffer.append(filter.getAttribute("class"));
            buffer.append("</filter-class>\n");
            final NodeList params = filter.getElementsByTagNameNS("http://www.allcolor.org/xmlns/filter", "param");
            for (int j = 0; j < params.getLength(); j++) {
                final Element param = (Element) params.item(j);
                final String name = param.getAttribute("name");
                final String value = param.getTextContent().trim();
                buffer.append("		<init-param>\n");
                buffer.append("			<param-name>");
                buffer.append(name);
                buffer.append("</param-name>\n");
                buffer.append("			<param-value>");
                buffer.append(value);
                buffer.append("</param-value>\n");
                buffer.append("		</init-param>\n");
            }
            buffer.append("	</filter>\n");
        }
        buffer.append("	\n");
        buffer.append("	<!-- Filter Mapping -->\n");
        for (int i = 0; i < filters.getLength(); i++) {
            final Element filter = (Element) filters.item(i);
            final NodeList matchs = filter.getElementsByTagNameNS("http://www.allcolor.org/xmlns/filter", "match");
            if (matchs.getLength() == 0) {
                buffer.append("	<filter-mapping>\n");
                buffer.append("		<filter-name>");
                buffer.append(filter.getAttribute("name"));
                buffer.append("</filter-name>\n");
                buffer.append("		<url-pattern>");
                buffer.append("/*");
                buffer.append("</url-pattern>\n");
                buffer.append("	</filter-mapping>\n");
            } else {
                for (int j = 0; j < matchs.getLength(); j++) {
                    final Element match = (Element) matchs.item(j);
                    buffer.append("	<filter-mapping>\n");
                    buffer.append("		<filter-name>");
                    buffer.append(filter.getAttribute("name"));
                    buffer.append("</filter-name>\n");
                    buffer.append("		<url-pattern>");
                    buffer.append(match.getTextContent().trim());
                    buffer.append("</url-pattern>\n");
                    buffer.append("	</filter-mapping>\n");
                }
            }
        }
        buffer.append("	\n");
        final NodeList sclisten = scdoc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/context", "listener");
        buffer.append("	<!-- Context Listener -->\n");
        for (int i = 0; i < sclisten.getLength(); i++) {
            final Element listen = (Element) sclisten.item(i);
            buffer.append("	<listener>\n");
            buffer.append("		<listener-class>");
            buffer.append(listen.getAttribute("class"));
            buffer.append("</listener-class>\n");
            buffer.append("	</listener>\n");
        }
        buffer.append("	\n");
        final Document svdoc = parser.parse(this.getResource("/WEB-INF/config/servlet.config.xml"));
        final NodeList servlets = svdoc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/servlet", "servlet");
        buffer.append("	<!-- Servlet -->\n");
        final List<String> precServlet = new ArrayList<String>();
        for (int i = 0; i < servlets.getLength(); i++) {
            final Element servlet = (Element) servlets.item(i);
            if (precServlet.contains(servlet.getAttribute("name").intern())) {
                continue;
            }
            precServlet.add(servlet.getAttribute("name").intern());
            buffer.append("	<servlet>\n");
            buffer.append("		<servlet-name>");
            buffer.append(servlet.getAttribute("name"));
            buffer.append("</servlet-name>\n");
            buffer.append("		<servlet-class>");
            buffer.append(servlet.getAttribute("class"));
            buffer.append("</servlet-class>\n");
            final NodeList params = servlet.getElementsByTagNameNS("http://www.allcolor.org/xmlns/servlet", "param");
            for (int j = 0; j < params.getLength(); j++) {
                final Element param = (Element) params.item(j);
                final String name = param.getAttribute("name");
                final String value = param.getTextContent().trim();
                buffer.append("		<init-param>\n");
                buffer.append("			<param-name>");
                buffer.append(name);
                buffer.append("</param-name>\n");
                buffer.append("			<param-value>");
                buffer.append(value);
                buffer.append("</param-value>\n");
                buffer.append("		</init-param>\n");
            }
            buffer.append("	</servlet>\n");
        }
        buffer.append("	\n");
        buffer.append("	<!-- Servlet Mapping -->\n");
        for (int i = 0; i < servlets.getLength(); i++) {
            final Element servlet = (Element) servlets.item(i);
            final NodeList matchs = servlet.getElementsByTagNameNS("http://www.allcolor.org/xmlns/servlet", "match");
            if (matchs.getLength() == 0) {
                buffer.append("	<servlet-mapping>\n");
                buffer.append("		<servlet-name>");
                buffer.append(servlet.getAttribute("name"));
                buffer.append("</servlet-name>\n");
                buffer.append("		<url-pattern>");
                buffer.append("/*");
                buffer.append("</url-pattern>\n");
                buffer.append("	</servlet-mapping>\n");
            } else {
                for (int j = 0; j < matchs.getLength(); j++) {
                    final Element match = (Element) matchs.item(j);
                    buffer.append("	<servlet-mapping>\n");
                    buffer.append("		<servlet-name>");
                    buffer.append(servlet.getAttribute("name"));
                    buffer.append("</servlet-name>\n");
                    buffer.append("		<url-pattern>");
                    buffer.append(match.getTextContent().trim());
                    buffer.append("</url-pattern>\n");
                    buffer.append("	</servlet-mapping>\n");
                }
            }
        }
        buffer.append("</web-app>\n");
        return buffer.toString().getBytes();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param file DOCUMENT ME!
	 */
    private void deleteScratchDir(java.io.File file) {
        if (file == null) {
            try {
                file = java.io.File.createTempFile("directory", "dir");
                final java.io.File dir = new java.io.File(file.getParentFile().getAbsolutePath() + "/" + this.getServletContextName() + "/");
                try {
                    file.delete();
                } catch (final Exception ignore) {
                    ;
                }
                this.deleteScratchDir(dir);
            } catch (final Exception ignore) {
                ;
            }
        } else {
            try {
                if (file.isFile()) {
                    file.delete();
                } else {
                    final java.io.File list[] = file.listFiles();
                    if (list != null) {
                        for (final java.io.File f : list) {
                            this.deleteScratchDir(f);
                        }
                    }
                    file.delete();
                }
            } catch (final Exception ignore) {
                ;
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private void destroyServlets() {
        synchronized (this.servlets) {
            for (final Iterator it = this.servlets.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) it.next();
                final CHttpServlet hs = (CHttpServlet) entry.getValue();
                final Servlet servlet = hs.getServlet();
                try {
                    if (!hs.isCopy()) {
                        this.log("Destroy servlet : " + hs.getName());
                        servlet.destroy();
                        this.log("Destroy servlet : " + hs.getName() + " done.");
                    }
                } catch (final Throwable ignore) {
                    if (ignore.getClass() == ThreadDeath.class) {
                        throw (ThreadDeath) ignore;
                    }
                    Throwable cause = ignore.getCause();
                    while (cause != null) {
                        if (cause.getClass() == ThreadDeath.class) {
                            throw (ThreadDeath) cause;
                        }
                        cause = cause.getCause();
                    }
                } finally {
                    it.remove();
                }
            }
            this.servlets.clear();
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private void fireDestroyEvent() {
        for (final Object element : this.contextListener) {
            final ServletContextListener listener = (ServletContextListener) element;
            try {
                listener.contextDestroyed(new ServletContextEvent(this));
            } catch (final Throwable ignore) {
                if (ignore.getClass() == ThreadDeath.class) {
                    throw (ThreadDeath) ignore;
                }
                Throwable cause = ignore.getCause();
                while (cause != null) {
                    if (cause.getClass() == ThreadDeath.class) {
                        throw (ThreadDeath) cause;
                    }
                    cause = cause.getCause();
                }
            }
        }
        this.contextListener.clear();
    }

    /**
	 * DOCUMENT ME!
	 */
    private void fireInitEvent() {
        for (final Object element : this.contextListener) {
            final ServletContextListener listener = (ServletContextListener) element;
            try {
                listener.contextInitialized(new ServletContextEvent(this));
            } catch (final Throwable ignore) {
                if (ignore.getClass() == ThreadDeath.class) {
                    throw (ThreadDeath) ignore;
                }
                Throwable cause = ignore.getCause();
                while (cause != null) {
                    if (cause.getClass() == ThreadDeath.class) {
                        throw (ThreadDeath) cause;
                    }
                    cause = cause.getCause();
                }
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws Exception DOCUMENT ME!
	 */
    private URL getWebXml() throws Exception {
        if (this.webxml != null) {
            return this.webxml;
        }
        final FileSystem fs = FileSystem.mount(this.getServletContextName() + "_MEMORY", FileSystemType.MEMORY, null);
        final File webxml = fs.file("/WEB-INF/web.xml");
        final OutputStream out = webxml.getOutputStream();
        out.write(this.createWebXml());
        out.close();
        return (this.webxml = webxml.toURL());
    }

    /**
	 * DOCUMENT ME!
	 */
    private void init() {
        try {
            final CShaniDomParser parser = new CShaniDomParser();
            final Document doc = parser.parse(this.getResource("/WEB-INF/config/servletcontext.xml"));
            final NodeList nl = doc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/context", "param");
            synchronized (this.parameters) {
                for (int i = 0; i < nl.getLength(); i++) {
                    final Element param = (Element) nl.item(i);
                    param.normalize();
                    try {
                        this.parameters.put(param.getAttribute("name"), param.getTextContent().trim());
                    } catch (final Exception ignore) {
                        ;
                    }
                }
            }
        } catch (final Exception ignore) {
            ;
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private void initServlets() {
        synchronized (this.servlets) {
            this.log("Initializing servlets...");
            for (final Iterator it = this.servlets.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) it.next();
                final CHttpServlet hs = (CHttpServlet) entry.getValue();
                final Servlet servlet = hs.getServlet();
                try {
                    if (!hs.isCopy()) {
                        this.log("Init servlet : " + hs.getName());
                        servlet.init(hs.getConfig());
                        this.log("Init servlet : " + hs.getName() + " done.");
                    }
                } catch (final Throwable ignore) {
                    if (ignore.getClass() == ThreadDeath.class) {
                        throw (ThreadDeath) ignore;
                    }
                    Throwable cause = ignore.getCause();
                    while (cause != null) {
                        if (cause.getClass() == ThreadDeath.class) {
                            throw (ThreadDeath) cause;
                        }
                        cause = cause.getCause();
                    }
                    this.log.error(ignore);
                    it.remove();
                }
            }
            this.log("Servlets initialized.");
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private void loadServletContextListener() {
        try {
            final CShaniDomParser parser = new CShaniDomParser();
            final Document doc = parser.parse(this.getResource("/WEB-INF/config/servletcontext.xml"));
            final NodeList nl = doc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/context", "listener");
            for (int i = 0; i < nl.getLength(); i++) {
                final Element listener = (Element) nl.item(i);
                try {
                    final Class clazz = Class.forName(listener.getAttribute("class"));
                    this.registerContextListener((ServletContextListener) clazz.newInstance());
                } catch (final Exception ignore) {
                    System.err.println("Error while creating listener: " + listener.getAttribute("class"));
                    ignore.printStackTrace();
                }
            }
        } catch (final Exception ignore) {
            System.err.println("Error while loading ServletContextListeners.");
            ignore.printStackTrace();
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private void loadServlets() {
        try {
            final CShaniDomParser parser = new CShaniDomParser();
            final Document doc = parser.parse(this.getResource("/WEB-INF/config/servlet.config.xml"));
            final NodeList nl = doc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/servlet", "servlet");
            synchronized (this.servlets) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Loading servlets nbr: " + nl.getLength());
                }
                for (int i = 0; i < nl.getLength(); i++) {
                    final Element servlet = (Element) nl.item(i);
                    try {
                        final Class clazz = Class.forName(servlet.getAttribute("class"));
                        final String name = servlet.getAttribute("name");
                        if (this.log.isDebugEnabled()) {
                            this.log.debug("New servlet: " + name);
                        }
                        final NodeList nlMatch = servlet.getElementsByTagName("match");
                        final List<Pattern> pmatch = new ArrayList<Pattern>();
                        for (int j = 0; j < nlMatch.getLength(); j++) {
                            final Element em = (Element) nlMatch.item(j);
                            final Pattern match = Pattern.compile(em.getTextContent());
                            pmatch.add(match);
                        }
                        final ServletConfig config = new CServletConfig(name);
                        final Servlet httpServlet = (Servlet) clazz.newInstance();
                        for (int j = 0; j < pmatch.size(); j++) {
                            final Pattern match = pmatch.get(j);
                            if (this.log.isDebugEnabled()) {
                                this.log.debug("added servlet: " + name + " - " + j);
                            }
                            if (j == 0) {
                                this.servlets.put(name, new CHttpServlet(httpServlet, match, name, config, false));
                            } else {
                                this.servlets.put(name + j, new CHttpServlet(httpServlet, match, name, config, true));
                            }
                        }
                    } catch (final Exception ignore) {
                        this.log.error(ignore);
                    }
                }
            }
        } catch (final Exception ignore) {
            this.log.error(ignore);
        }
    }

    /**
	 * 
	DOCUMENT ME!
	 *
	 * @author Quentin Anciaux
	 * @version 0.1.0
	 */
    public static final class CHttpServlet {

        /** DOCUMENT ME! */
        private final Pattern match;

        /** DOCUMENT ME! */
        private final Servlet servlet;

        /** DOCUMENT ME! */
        private final ServletConfig config;

        /** DOCUMENT ME! */
        private final String name;

        /** DOCUMENT ME! */
        private final boolean isCopy;

        /**
     * Creates a new CHttpServlet object.
     * 
     * @param servlet
     *            DOCUMENT ME!
     * @param match
     *            DOCUMENT ME!
     * @param name
     *            DOCUMENT ME!
     * @param config
     *            DOCUMENT ME!
     * @param isCopy
     *            DOCUMENT ME!
     */
        public CHttpServlet(final Servlet servlet, final Pattern match, final String name, final ServletConfig config, final boolean isCopy) {
            this.servlet = servlet;
            this.match = match;
            this.name = name;
            this.config = config;
            this.isCopy = isCopy;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public ServletConfig getConfig() {
            return this.config;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public Pattern getMatch() {
            return this.match;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public String getName() {
            return this.name;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public Servlet getServlet() {
            return this.servlet;
        }

        /**
		 * DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public boolean isCopy() {
            return this.isCopy;
        }
    }

    @Override
    public String getContextPath() {
        return "/" + this.getServletContextName();
    }
}
