package org.eclipse.help.internal.webapp.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.protocols.HelpURLConnection;
import org.eclipse.help.internal.protocols.HelpURLStreamHandler;
import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.internal.webapp.data.ServletResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;

/**
 * Performs transfer of data from eclipse to a jsp/servlet
 */
public class EclipseConnector {

    private static final String errorPageBegin = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" + "<html><head>\n" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" + "</head>\n" + "<body><p>\n";

    private static final String errorPageEnd = "</p></body></html>";

    private static final IFilter allFilters[] = new IFilter[] { new HighlightFilter(), new FramesetFilter(), new InjectionFilter(), new DynamicXHTMLFilter(), new BreadcrumbsFilter(), new ShowInTocFilter() };

    private static final IFilter errorPageFilters[] = new IFilter[] { new FramesetFilter(), new InjectionFilter(), new DynamicXHTMLFilter() };

    private ServletContext context;

    public EclipseConnector(ServletContext context) {
        this.context = context;
    }

    public void transfer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String url = getURL(req);
            if (url == null) return;
            int index = url.lastIndexOf(HelpURLConnection.PLUGINS_ROOT);
            if (index != -1 && url.indexOf("content/" + HelpURLConnection.PLUGINS_ROOT) == -1) {
                StringBuffer redirectURL = new StringBuffer();
                redirectURL.append(req.getContextPath());
                redirectURL.append(req.getServletPath());
                redirectURL.append("/");
                redirectURL.append(url.substring(index + HelpURLConnection.PLUGINS_ROOT.length()));
                resp.sendRedirect(redirectURL.toString());
                return;
            }
            if (url.toLowerCase(Locale.ENGLISH).startsWith("file:") || url.toLowerCase(Locale.ENGLISH).startsWith("jar:") || url.toLowerCase(Locale.ENGLISH).startsWith("platform:")) {
                int i = url.indexOf('?');
                if (i != -1) url = url.substring(0, i);
                if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER || !UrlUtil.isLocalRequest(req)) {
                    return;
                }
            } else {
                url = "help:" + url;
            }
            URLConnection con = createConnection(req, resp, url);
            InputStream is;
            boolean pageNotFound = false;
            try {
                is = con.getInputStream();
            } catch (IOException ioe) {
                pageNotFound = true;
                if (url.toLowerCase(Locale.ENGLISH).endsWith("htm") || url.toLowerCase(Locale.ENGLISH).endsWith("html")) {
                    Preferences prefs = HelpBasePlugin.getDefault().getPluginPreferences();
                    String errorPage = prefs.getString("page_not_found");
                    if (errorPage != null && errorPage.length() > 0) {
                        con = createConnection(req, resp, "help:" + errorPage);
                        try {
                            is = con.getInputStream();
                        } catch (IOException ioe2) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        }
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            } catch (Exception e) {
                Throwable t = e;
                if (t instanceof UndeclaredThrowableException && t.getCause() != null) {
                    t = t.getCause();
                }
                StringBuffer message = new StringBuffer();
                message.append(errorPageBegin);
                message.append("<p>");
                message.append(ServletResources.getString("contentProducerException", req));
                message.append("</p>");
                message.append("<pre>");
                Writer writer = new StringWriter();
                t.printStackTrace(new PrintWriter(writer));
                message.append(writer.toString());
                message.append("</pre>");
                message.append(errorPageEnd);
                is = new ByteArrayInputStream(message.toString().getBytes("UTF8"));
            }
            OutputStream out = resp.getOutputStream();
            IFilter filters[] = pageNotFound ? errorPageFilters : allFilters;
            for (int i = 0; i < filters.length; i++) {
                out = filters[i].filter(req, out);
            }
            transferContent(is, out);
            out.close();
            is.close();
        } catch (Exception e) {
            String msg = "Error processing help request " + getURL(req);
            HelpWebappPlugin.logError(msg, e);
        }
    }

    private URLConnection createConnection(HttpServletRequest req, HttpServletResponse resp, String url) throws Exception {
        URLConnection con;
        con = openConnection(url, req, resp);
        String contentType;
        String pathInfo = req.getPathInfo();
        String mimeType = context.getMimeType(pathInfo);
        if (mimeType != null && !mimeType.equals("application/xhtml+xml")) {
            contentType = mimeType;
        } else {
            contentType = con.getContentType();
        }
        resp.setContentType(contentType);
        long maxAge = 0;
        try {
            long expiration = con.getExpiration();
            maxAge = (expiration - System.currentTimeMillis()) / 1000;
            if (maxAge < 0) maxAge = 0;
        } catch (Exception e) {
        }
        resp.setHeader("Cache-Control", "max-age=" + maxAge);
        return con;
    }

    /**
	 * Write the body to the response
	 */
    private void transferContent(InputStream inputStream, OutputStream out) throws IOException {
        try {
            BufferedInputStream dataStream = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[4096];
            int len = 0;
            while (true) {
                len = dataStream.read(buffer);
                if (len == -1) break;
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
        }
    }

    /**
	 * Gets content from the named url (this could be and eclipse defined url)
	 */
    private URLConnection openConnection(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        URLConnection con = null;
        if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER) {
            String locale = UrlUtil.getLocale(request, response);
            if (url.indexOf('?') >= 0) {
                url = url + "&lang=" + locale;
            } else {
                url = url + "?lang=" + locale;
            }
        }
        URL helpURL;
        if (url.startsWith("help:")) {
            helpURL = new URL("help", null, -1, url.substring("help:".length()), HelpURLStreamHandler.getDefault());
        } else {
            if (url.startsWith("jar:")) {
                int excl = url.indexOf("!/");
                String jar = url.substring(0, excl);
                String path = url.length() > excl + 2 ? url.substring(excl + 2) : "";
                url = jar.replaceAll("!", "%21") + "!/" + path.replaceAll("!", "%21");
            }
            helpURL = new URL(url);
        }
        String protocol = helpURL.getProtocol();
        if (!("help".equals(protocol) || "file".equals(protocol) || "platform".equals(protocol) || "jar".equals(protocol))) {
            throw new IOException();
        }
        con = helpURL.openConnection();
        con.setAllowUserInteraction(false);
        con.setDoInput(true);
        con.connect();
        return con;
    }

    /**
	 * Extracts the url from a request
	 */
    private String getURL(HttpServletRequest req) {
        String query = "";
        boolean firstParam = true;
        for (Enumeration params = req.getParameterNames(); params.hasMoreElements(); ) {
            String param = (String) params.nextElement();
            String[] values = req.getParameterValues(param);
            if (values == null) continue;
            for (int i = 0; i < values.length; i++) {
                if (firstParam) {
                    query += "?" + param + "=" + values[i];
                    firstParam = false;
                } else query += "&" + param + "=" + values[i];
            }
        }
        String url = req.getPathInfo() + query;
        if (url.startsWith("/")) url = url.substring(1);
        return url;
    }
}
