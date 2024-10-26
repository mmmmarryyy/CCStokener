package org.orbeon.faces.servlet;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * This class implements a simple Servlet filter that applies a series of
 * stylesheets.
 *
 * NOTE: This is an example indended to show a functional filter. There have
 * not been many considerations of performance or memory optimization.
 */
public class Filter implements javax.servlet.Filter {

    public static final boolean DEBUG = true;

    private FilterConfig filterConfig;

    private SAXTransformerFactory saxTransformerFactory;

    private Map templatesCache = new HashMap();

    private List stylesheetNames = new ArrayList();

    /**
     * Initialize the filter.
     */
    public void init(FilterConfig config) throws ServletException {
        filterConfig = config;
        for (int i = 1; ; i++) {
            String param = config.getInitParameter("stylesheet" + i);
            if (param == null) break;
            stylesheetNames.add("/WEB-INF/" + param);
        }
    }

    /**
     * Add this for WebLogic 6.1 that supports a draft version of the
     * Servlet API.
     */
    public void setFilterConfig(FilterConfig config) {
        try {
            init(config);
        } catch (Exception e) {
            config.getServletContext().log("Exception while setting config", e);
            throw new RuntimeException("Exception while setting config");
        }
    }

    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    /**
     * Execute the filter.
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletResponseWrapper servletResponseWrapper = new ServletResponseWrapper(filterConfig.getServletContext(), (HttpServletResponse) response);
        chain.doFilter(request, servletResponseWrapper);
        int index = 0;
        ContentHandler initialContentHandler = null;
        TransformerHandler priorTransformerHandler = null;
        for (Iterator i = stylesheetNames.iterator(); i.hasNext(); index++) {
            String stylesheetName = (String) i.next();
            TransformerHandler transformerHandler = getTransformerHandler(stylesheetName);
            if (index == 0) {
                initialContentHandler = transformerHandler;
            }
            if (index == (stylesheetNames.size() - 1)) {
                setHTMLOutputProperties(transformerHandler.getTransformer());
                transformerHandler.setResult(new StreamResult(response.getWriter()));
            }
            if (index > 0 && index < stylesheetNames.size()) {
                priorTransformerHandler.setResult(new SAXResult(transformerHandler));
            }
            priorTransformerHandler = transformerHandler;
        }
        response.setContentType("text/html");
        servletResponseWrapper.parse(initialContentHandler);
    }

    /**
     * Destroy the filter.
     */
    public void destroy() {
    }

    /**
     * Create a TransformerHandler and make sure the stylesheet is up to date.
     */
    private synchronized TransformerHandler getTransformerHandler(String stylesheetName) throws ServletException {
        try {
            if (saxTransformerFactory == null) {
                saxTransformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                saxTransformerFactory.setAttribute("http://xml.apache.org/xalan/features/incremental", Boolean.FALSE);
                saxTransformerFactory.setURIResolver(new URIResolver() {

                    public Source resolve(String href, String base) throws TransformerException {
                        try {
                            URL url = filterConfig.getServletContext().getResource("/WEB-INF/" + href);
                            URLConnection connection = url.openConnection();
                            return new SAXSource(new InputSource(connection.getInputStream()));
                        } catch (IOException e) {
                            filterConfig.getServletContext().log("Exception while resolving URL", e);
                            throw new RuntimeException(e.getMessage());
                        }
                    }
                });
            }
            URL url = filterConfig.getServletContext().getResource(stylesheetName);
            URLConnection connection = url.openConnection();
            try {
                long lastModified = connection.getLastModified();
                TemplatesInfo templatesInfo = (TemplatesInfo) templatesCache.get(stylesheetName);
                if (templatesInfo == null || lastModified > templatesInfo.getLastModified()) {
                    SAXSource source = new SAXSource(new InputSource(connection.getInputStream()));
                    source.setSystemId(stylesheetName);
                    templatesInfo = new TemplatesInfo(lastModified, saxTransformerFactory.newTemplates(source));
                    templatesCache.put(stylesheetName, templatesInfo);
                }
                TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler(templatesInfo.getTemplates());
                return transformerHandler;
            } finally {
                if (connection != null) connection.getInputStream().close();
            }
        } catch (Exception e) {
            throw new ServletException("Exception caught while getting SAX transformer", e);
        }
    }

    private void setHTMLOutputProperties(Transformer transformer) {
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.VERSION, "4.0");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.0 Transitional//EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/html4/loose.dtd");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "iso-8859-1");
        transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/html");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    }

    private static class TemplatesInfo {

        private Templates templates;

        private long lastModified;

        public TemplatesInfo(long lastModified, Templates templates) {
            this.lastModified = lastModified;
            this.templates = templates;
        }

        public long getLastModified() {
            return lastModified;
        }

        public Templates getTemplates() {
            return templates;
        }
    }
}
