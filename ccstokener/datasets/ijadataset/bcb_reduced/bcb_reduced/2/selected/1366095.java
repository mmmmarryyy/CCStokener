package net.sf.opentranquera.pagespy;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * Este filtro se encarga de espiar todo el HTML que pasa por el response dada un session id especificado.
 * Escribe lo que captura en el port indicado.
 * @author Guillermo Meyer
 */
public class PageSpyFilter implements Filter {

    public static final String PROPERTIES_PANEL = "PROPERTIES_PANEL.className";

    public static final String LOG_CAT = "OpenTranquera.PageSpy";

    private PageSpyServer spy;

    private Collection propertySetters = new ArrayList();

    private Collection ignoredPatterns = new ArrayList();

    private PageReplacerChain replacerChain = new PageReplacerChain();

    private String ignorePattern;

    private PagePanel propertiesPanel;

    private static Logger logger = Logger.getLogger(PageSpyFilter.LOG_CAT);

    private Logger getLogger() {
        return logger;
    }

    public void init(FilterConfig config) throws ServletException {
        try {
            String configFile = config.getInitParameter("config");
            this.loadConfig(config.getServletContext(), configFile);
            config.getServletContext().setAttribute(PROPERTIES_PANEL, this.propertiesPanel);
            PageSpyContext.getInstance().setServer(this.spy);
            this.getLogger().info("Starting server: " + this.spy.getClass().getName());
            this.spy.initServer(config.getServletContext());
            if (ignorePattern != null) {
                StringTokenizer st = new StringTokenizer(ignorePattern, ",");
                while (st.hasMoreTokens()) {
                    ignoredPatterns.add(st.nextToken());
                }
            }
            this.spy.startServer();
            this.getLogger().info("Server Started.");
        } catch (IOException e) {
            this.getLogger().error("Error staring PageSpyFilter.", e);
            throw new ServletException(e);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!this.spy.isSessionMonitored(((HttpServletRequest) request).getSession().getId()) || this.isRequestIgnored((HttpServletRequest) request)) {
            chain.doFilter(request, response);
        } else {
            PageSpyResponseWrapper resWrapper = new PageSpyResponseWrapper((HttpServletResponse) response);
            chain.doFilter(request, resWrapper);
            resWrapper.flushBuffer();
            byte[] resp = resWrapper.getOutputAsByteArray();
            resp = this.replacerChain.replace(request, response, resp);
            final Page pagina = new Page(((HttpServletRequest) request).getSession().getId(), resp);
            Iterator it = this.propertySetters.iterator();
            while (it.hasNext()) {
                PagePropertySetter pps = (PagePropertySetter) it.next();
                pps.addProperties(request, response, pagina.getProperties());
            }
            if (this.getLogger().isDebugEnabled()) {
                this.getLogger().debug("Sending notification. SessionId: " + pagina.getId());
            }
            this.spy.sendNotification(pagina);
        }
    }

    private boolean isRequestIgnored(HttpServletRequest request) {
        Iterator it = this.ignoredPatterns.iterator();
        boolean ret = false;
        while (it.hasNext() && !ret) {
            String ip = (String) it.next();
            ret = request.getRequestURL().indexOf(ip) >= 0;
        }
        return ret;
    }

    public void destroy() {
        try {
            this.spy.stopServer();
        } catch (IOException e) {
            this.getLogger().error("Error destroying PageSpyFilter.", e);
        } finally {
            this.spy.destroyServer();
        }
    }

    private void loadConfig(ServletContext ctx, String configFileName) {
        Digester digester = new Digester();
        digester.push(this);
        digester.addFactoryCreate("pagespy/server", new AbstractObjectCreationFactory() {

            public Object createObject(Attributes attrs) {
                String className = attrs.getValue("className");
                try {
                    return Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ClassCastException("Error al instanciar " + className);
                }
            }
        });
        digester.addSetProperty("pagespy/server/param", "name", "value");
        digester.addSetNext("pagespy/server", "setServer", PageSpyServer.class.getName());
        digester.addCallMethod("pagespy/ignored-patterns", "setIgnorePattern", 1);
        digester.addCallParam("pagespy/ignored-patterns", 0);
        digester.addFactoryCreate("pagespy/property-setters/setter", new AbstractObjectCreationFactory() {

            public Object createObject(Attributes attrs) {
                String className = attrs.getValue("className");
                try {
                    return Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ClassCastException("Error al instanciar " + className);
                }
            }
        });
        digester.addSetNext("pagespy/property-setters/setter", "addPropertySetter", PagePropertySetter.class.getName());
        digester.addFactoryCreate("pagespy/page-replacers/replacer", new AbstractObjectCreationFactory() {

            public Object createObject(Attributes attrs) {
                String className = attrs.getValue("className");
                try {
                    return Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ClassCastException("Error al instanciar " + className);
                }
            }
        });
        digester.addSetNext("pagespy/page-replacers/replacer", "addPageReplacer", PageReplacer.class.getName());
        digester.addFactoryCreate("pagespy/properties-panel", new AbstractObjectCreationFactory() {

            public Object createObject(Attributes attrs) {
                String className = attrs.getValue("className");
                try {
                    return Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ClassCastException("Error al instanciar " + className);
                }
            }
        });
        digester.addSetNext("pagespy/properties-panel", "setPropertiesPanel", PagePanel.class.getName());
        try {
            this.getLogger().info("Initializing " + configFileName);
            URL url = ctx.getResource(configFileName);
            if (url == null) {
                url = this.getClass().getResource(configFileName);
            }
            digester.parse(url.openStream());
        } catch (Exception e) {
            this.getLogger().error("Error parsing configuration file.", e);
            throw new RuntimeException(e);
        }
    }

    public void addPropertySetter(PagePropertySetter pps) {
        this.propertySetters.add(pps);
    }

    public void addPageReplacer(PageReplacer pr) {
        this.replacerChain.addReplacer(pr);
    }

    public void setServer(PageSpyServer server) {
        this.spy = server;
    }

    public void setPropertiesPanel(PagePanel panel) {
        this.propertiesPanel = panel;
    }

    public void setIgnorePattern(String s) {
        this.ignorePattern = s;
    }
}
