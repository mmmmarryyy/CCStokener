package jgloss.www;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.EDict;
import jgloss.dictionary.KanjiDic;
import jgloss.parser.KanjiParser;
import jgloss.parser.Parser;
import jgloss.util.CharacterEncodingDetector;
import jgloss.util.UTF8ResourceBundleControl;
import javax.servlet.http.*;
import javax.servlet.*;

/**
 * Servlet which annotates an HTML page with the dictionary entries for
 * Japanese words. The servlet takes an URL, loads the specified page, annotates it
 * and forwards it to the client. The annotations will be displayed via JavaScript.
 *
 * @author Michael Koch
 */
public class JGlossServlet extends HttpServlet {

    /**
     * Path to the messages ressource.
     */
    public static final String MESSAGES = "messages-www";

    /**
     * Initialization parameter name.
     */
    public static final String DICTIONARIES = "dictionaries";

    /**
     * Initialization parameter name.
     */
    public static final String ALLOWED_PROTOCOLS = "allowed_protocols";

    /**
     * Initialization parameter name.
     */
    public static final String SECURE_ALLOWED_PROTOCOLS = "secure_allowed_protocols";

    /**
     * Initialization parameter name.
     */
    public static final String REWRITTEN_TYPES = "rewritten_types";

    /**
     * Initialization parameter name.
     */
    public static final String ENABLE_COOKIE_FORWARDING = "enable_cookie_forwarding";

    /**
     * Initialization parameter name.
     */
    public static final String ENABLE_SECURE_INSECURE_COOKIE_FORWARDING = "enable_secure-to-insecure_cookie_forwarding";

    /**
     * Initialization parameter name.
     */
    public static final String ENABLE_FORM_DATA_FORWARDING = "enable_form_data_forwarding";

    /**
     * Initialization parameter name.
     */
    public static final String ENABLE_SECURE_INSECURE_FORM_DATA_FORWARDING = "enable_secure-to-insecure_form_data_forwarding";

    /**
     * Initialization parameter name.
     */
    public static final String RESPONSE_BUFFER_SIZE = "response-buffer-size";

    /**
     * Initialization parameter name.
     */
    public static final String ENABLE_COMPRESSION = "enable_compression";

    /**
     * CGI parameter name.
     */
    public static final String REMOTE_URL = "jgurl";

    /**
     * CGI parameter name.
     */
    public static final String ALLOW_COOKIE_FORWARDING = "jgforwardcookies";

    /**
     * CGI parameter name.
     */
    public static final String ALLOW_FORM_DATA_FORWARDING = "jgforwardforms";

    private jgloss.dictionary.Dictionary[] dictionaries;

    /**
     * Set of protocols allowed in remote urls.
     */
    private Set allowedProtocols;

    /**
     * Set of protocols allowed in remote urls used when the Client-Servlet connection is
     * secure.
     */
    private Set secureAllowedProtocols;

    /**
     * List of MIME types which will be annotated.
     */
    private String[] rewrittenContentTypes;

    /**
     * Set of request and response header keys which will not be forwarded.
     */
    private Set noForwardHeaders;

    /**
     * Flag if cookie forwarding should be enabled globally. If enabled, it can still
     * be disabled on a per-request basis.
     */
    private boolean enableCookieForwarding;

    /**
     * Flag if cookies should be forwarded from a secure to an insecure connection.
     * Regardless of the setting of this flag, a cookie with the 'secure' attribute set will
     * never be forwarded over an insecure connection.
     */
    private boolean enableCookieSecureInsecureForwarding;

    /**
     * Flag if form data forwarding should be enabled globally. If enabled, it can still
     * be disabled on a per-request basis.
     */
    private boolean enableFormDataForwarding;

    /**
     * Flag if form data should be forwarded from a secure to an insecure connection.
     */
    private boolean enableFormDataSecureInsecureForwarding;

    /**
     * Size of the write buffer of HttpServletResponses in bytes, or -1 to use
     * the default size.
     */
    private int responseBufferSize;

    /**
     * Flag if compression content encodings should be enabled.
     */
    private boolean enableCompression;

    public JGlossServlet() {
    }

    /**
     * Reads the initialization parameters and loads the dictionaries.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        HttpURLConnection.setFollowRedirects(false);
        List diclist = new LinkedList();
        String d = config.getInitParameter(DICTIONARIES);
        if (d == null || d.length() == 0) throw new ServletException(MessageFormat.format(ResourceBundle.getBundle(MESSAGES).getString("error.nodictionary"), new Object[] { DICTIONARIES }));
        for (Iterator i = split(d, ' ').iterator(); i.hasNext(); ) {
            d = (String) i.next();
            jgloss.dictionary.Dictionary dic = null;
            try {
                dic = DictionaryFactory.createDictionary(d);
            } catch (DictionaryFactory.InstantiationException ex) {
                throw new ServletException(MessageFormat.format(ResourceBundle.getBundle(MESSAGES).getString("error.loaddictionary"), new Object[] { d }), ex);
            } catch (DictionaryFactory.NotSupportedException ex) {
                throw new ServletException(MessageFormat.format(ResourceBundle.getBundle(MESSAGES).getString("error.unknowndictionary"), new Object[] { d }));
            }
            diclist.add(dic);
        }
        dictionaries = new jgloss.dictionary.Dictionary[diclist.size()];
        dictionaries = (jgloss.dictionary.Dictionary[]) diclist.toArray(dictionaries);
        Parser parser = new KanjiParser(dictionaries, null);
        parser.setIgnoreNewlines(true);
        try {
            HTMLAnnotator annotator = new HTMLAnnotator(parser);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
        allowedProtocols = new HashSet(5);
        String p = config.getInitParameter(ALLOWED_PROTOCOLS);
        if (p == null || p.length() == 0) throw new ServletException(MessageFormat.format(ResourceBundle.getBundle(MESSAGES).getString("error.noprotocols"), new Object[] { ALLOWED_PROTOCOLS }));
        allowedProtocols.addAll(split(p, ','));
        p = "";
        for (Iterator i = allowedProtocols.iterator(); i.hasNext(); ) {
            if (p.length() > 0) p += ", ";
            p += (String) i.next();
        }
        if (p.length() == 0) p = "none";
        getServletContext().log("allowed protocols: " + p);
        secureAllowedProtocols = new HashSet(5);
        p = config.getInitParameter(SECURE_ALLOWED_PROTOCOLS);
        if (p != null) secureAllowedProtocols.addAll(split(p, ','));
        p = "";
        for (Iterator i = secureAllowedProtocols.iterator(); i.hasNext(); ) {
            if (p.length() > 0) p += ", ";
            p += (String) i.next();
        }
        if (p.length() == 0) p = "none";
        getServletContext().log("secure allowed protocols: " + p);
        noForwardHeaders = new HashSet(51);
        noForwardHeaders.add("connection");
        noForwardHeaders.add("upgrade");
        noForwardHeaders.add("via");
        noForwardHeaders.add("transfer-encoding");
        noForwardHeaders.add("keep-alive");
        noForwardHeaders.add("range");
        noForwardHeaders.add("if-range");
        noForwardHeaders.add("accept-encoding");
        noForwardHeaders.add("authorization");
        noForwardHeaders.add("referer");
        noForwardHeaders.add("te");
        noForwardHeaders.add("trailers");
        noForwardHeaders.add("host");
        noForwardHeaders.add("accept-ranges");
        noForwardHeaders.add("location");
        noForwardHeaders.add("content-encoding");
        noForwardHeaders.add("content-length");
        noForwardHeaders.add("content-md5");
        noForwardHeaders.add("content-type");
        noForwardHeaders.add("set-cookie");
        noForwardHeaders.add("set-cookie2");
        noForwardHeaders.add("cookie");
        noForwardHeaders.add("cookie2");
        noForwardHeaders.add("proxy-connection");
        noForwardHeaders.add("refresh");
        rewrittenContentTypes = new String[0];
        p = config.getInitParameter(REWRITTEN_TYPES);
        if (p != null) {
            rewrittenContentTypes = (String[]) split(p, ',').toArray(rewrittenContentTypes);
        }
        if (rewrittenContentTypes.length == 0) {
            rewrittenContentTypes = new String[1];
            rewrittenContentTypes[0] = "text/html";
        }
        p = "";
        for (int i = 0; i < rewrittenContentTypes.length; i++) {
            if (p.length() > 0) p += ", ";
            p += rewrittenContentTypes[i];
        }
        if (p.length() == 0) p = "none";
        getServletContext().log("rewritten content types: " + p);
        p = config.getInitParameter(ENABLE_COOKIE_FORWARDING);
        enableCookieForwarding = "true".equals(p);
        getServletContext().log("cookie forwarding " + (enableCookieForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter(ENABLE_SECURE_INSECURE_COOKIE_FORWARDING);
        enableCookieSecureInsecureForwarding = "true".equals(p);
        getServletContext().log("secure-to-insecure cookie forwarding " + (enableCookieSecureInsecureForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter(ENABLE_FORM_DATA_FORWARDING);
        enableFormDataForwarding = "true".equals(p);
        getServletContext().log("form data forwarding " + (enableFormDataForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter(ENABLE_SECURE_INSECURE_FORM_DATA_FORWARDING);
        enableFormDataSecureInsecureForwarding = "true".equals(p);
        getServletContext().log("secure-to-insecure form data forwarding " + (enableFormDataSecureInsecureForwarding ? "enabled" : "disabled"));
        try {
            responseBufferSize = Integer.parseInt(config.getInitParameter(RESPONSE_BUFFER_SIZE));
            getServletContext().log("response buffer size set to " + responseBufferSize + " bytes");
        } catch (Exception ex) {
            responseBufferSize = -1;
            getServletContext().log("response buffer size not set, using default size");
        }
        enableCompression = "true".equals(config.getInitParameter(ENABLE_COMPRESSION));
        getServletContext().log("content compression " + (enableCompression ? "enabled" : "disabled"));
    }

    public void destroy() {
        super.destroy();
        for (int i = 0; i < dictionaries.length; i++) dictionaries[i].dispose();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGetPost(req, resp, false);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGetPost(req, resp, true);
    }

    protected void doGetPost(HttpServletRequest req, HttpServletResponse resp, boolean post) throws ServletException, IOException {
        if (responseBufferSize > 0) resp.setBufferSize(responseBufferSize);
        String pathinfo = req.getPathInfo();
        if (pathinfo == null) {
            String urlstring = req.getParameter(REMOTE_URL);
            if (urlstring == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ResourceBundle.getBundle(MESSAGES, req.getLocale(), new UTF8ResourceBundleControl()).getString("error.nourl"));
                return;
            }
            boolean allowCookieForwarding = "true".equals(req.getParameter(ALLOW_COOKIE_FORWARDING));
            boolean allowFormDataForwarding = "true".equals(req.getParameter(ALLOW_FORM_DATA_FORWARDING));
            String target = new JGlossURLRewriter(req.getContextPath() + req.getServletPath(), new URL(HttpUtils.getRequestURL(req).toString()), null, allowCookieForwarding, allowFormDataForwarding).rewrite(urlstring, true);
            resp.sendRedirect(target);
            return;
        }
        Set connectionAllowedProtocols;
        if (req.isSecure()) connectionAllowedProtocols = secureAllowedProtocols; else connectionAllowedProtocols = allowedProtocols;
        Object[] oa = JGlossURLRewriter.parseEncodedPath(pathinfo);
        if (oa == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, MessageFormat.format(ResourceBundle.getBundle(MESSAGES, req.getLocale(), new UTF8ResourceBundleControl()).getString("error.malformedrequest"), new Object[] { pathinfo }));
            return;
        }
        boolean allowCookieForwarding = ((Boolean) oa[0]).booleanValue();
        boolean allowFormDataForwarding = ((Boolean) oa[1]).booleanValue();
        String urlstring = (String) oa[2];
        getServletContext().log("received request for " + urlstring);
        if (urlstring.toLowerCase().indexOf(req.getServletPath().toLowerCase()) != -1) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, MessageFormat.format(ResourceBundle.getBundle(MESSAGES, req.getLocale()).getString("error.addressnotallowed"), new Object[] { urlstring }));
            return;
        }
        if (urlstring.indexOf(':') == -1) {
            if (req.isSecure()) {
                if (secureAllowedProtocols.contains("https")) urlstring = "https://" + urlstring;
            } else {
                if (allowedProtocols.contains("http")) urlstring = "http://" + urlstring;
            }
        }
        URL url;
        try {
            url = new URL(urlstring);
        } catch (MalformedURLException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, MessageFormat.format(ResourceBundle.getBundle(MESSAGES, req.getLocale()).getString("error.malformedurl"), new Object[] { urlstring }));
            return;
        }
        String protocol = url.getProtocol();
        if (!connectionAllowedProtocols.contains(protocol)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, MessageFormat.format(ResourceBundle.getBundle(MESSAGES, req.getLocale()).getString("error.protocolnotallowed"), new Object[] { protocol }));
            getServletContext().log("protocol not allowed accessing " + url.toString());
            return;
        }
        boolean remoteIsHttp = protocol.equals("http") || protocol.equals("https");
        boolean forwardCookies = remoteIsHttp && enableCookieForwarding && allowCookieForwarding;
        boolean forwardFormData = remoteIsHttp && enableFormDataForwarding && allowFormDataForwarding && (enableFormDataSecureInsecureForwarding || !req.isSecure() || url.getProtocol().equals("https"));
        if (forwardFormData) {
            String query = req.getQueryString();
            if (query != null && query.length() > 0) {
                if (url.getQuery() == null || url.getQuery().length() == 0) url = new URL(url.toExternalForm() + "?" + query); else url = new URL(url.toExternalForm() + "&" + query);
            }
        }
        JGlossURLRewriter rewriter = new JGlossURLRewriter(new URL(req.getScheme(), req.getServerName(), req.getServerPort(), req.getContextPath() + req.getServletPath()).toExternalForm(), url, connectionAllowedProtocols, allowCookieForwarding, allowFormDataForwarding);
        URLConnection connection = url.openConnection();
        if (forwardFormData && post && remoteIsHttp) {
            getServletContext().log("using POST");
            try {
                ((HttpURLConnection) connection).setRequestMethod("POST");
            } catch (ClassCastException ex) {
                getServletContext().log("failed to set method POST: " + ex.getMessage());
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
        }
        String acceptEncoding = buildAcceptEncoding(req.getHeader("accept-encoding"));
        getServletContext().log("accept-encoding: " + acceptEncoding);
        if (acceptEncoding != null) connection.setRequestProperty("Accept-Encoding", acceptEncoding);
        forwardRequestHeaders(connection, req);
        if (forwardCookies && (enableCookieSecureInsecureForwarding || !req.isSecure() || url.getProtocol().equals("https"))) CookieTools.addRequestCookies(connection, req.getCookies(), getServletContext());
        try {
            connection.connect();
        } catch (UnknownHostException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, MessageFormat.format(ResourceBundle.getBundle(MESSAGES, req.getLocale()).getString("error.unknownhost"), new Object[] { url.toExternalForm(), url.getHost() }));
            return;
        } catch (IOException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, MessageFormat.format(ResourceBundle.getBundle(MESSAGES, req.getLocale()).getString("error.connect"), new Object[] { url.toExternalForm(), ex.getClass().getName(), ex.getMessage() }));
            return;
        }
        if (forwardFormData && post && remoteIsHttp) {
            InputStream is = req.getInputStream();
            OutputStream os = connection.getOutputStream();
            byte[] buf = new byte[512];
            int len;
            while ((len = is.read(buf)) != -1) os.write(buf, 0, len);
            is.close();
            os.close();
        }
        forwardResponseHeaders(connection, req, resp, rewriter);
        if (forwardCookies && (enableCookieSecureInsecureForwarding || req.isSecure() || !url.getProtocol().equals("https"))) CookieTools.addResponseCookies(connection, resp, req.getServerName(), req.getContextPath() + req.getServletPath(), req.isSecure(), getServletContext());
        if (remoteIsHttp) {
            try {
                int response = ((HttpURLConnection) connection).getResponseCode();
                getServletContext().log("response code " + response);
                resp.setStatus(response);
                if (response == 304) return;
            } catch (ClassCastException ex) {
                getServletContext().log("failed to read response code: " + ex.getMessage());
            }
        }
        String type = connection.getContentType();
        getServletContext().log("content type " + type + " url " + connection.getURL().toString());
        boolean supported = false;
        if (type != null) {
            for (int i = 0; i < rewrittenContentTypes.length; i++) if (type.startsWith(rewrittenContentTypes[i])) {
                supported = true;
                break;
            }
        }
        if (supported) {
            String encoding = connection.getContentEncoding();
            supported = encoding == null || encoding.endsWith("gzip") || encoding.endsWith("deflate") || encoding.equals("identity");
        }
        if (supported) rewrite(connection, req, resp, rewriter); else tunnel(connection, req, resp);
    }

    protected void tunnel(URLConnection connection, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        byte[] buf = new byte[1024];
        if (connection.getContentType() != null) resp.setContentType(connection.getContentType());
        if (connection.getContentLength() > 0) resp.setContentLength(connection.getContentLength());
        if (connection.getContentEncoding() != null) resp.setHeader("Content-Encoding", connection.getContentEncoding());
        InputStream in = connection.getInputStream();
        OutputStream out = resp.getOutputStream();
        try {
            int len = in.read(buf);
            while (len != -1) {
                out.write(buf, 0, len);
                len = in.read(buf);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    protected void rewrite(URLConnection connection, HttpServletRequest req, HttpServletResponse resp, URLRewriter rewriter) throws ServletException, IOException {
        InputStream in = new BufferedInputStream(connection.getInputStream());
        String encoding = connection.getContentEncoding();
        if (encoding != null) {
            if (encoding.endsWith("gzip")) in = new GZIPInputStream(in); else if (encoding.endsWith("deflate")) in = new InflaterInputStream(in);
        }
        InputStreamReader reader = CharacterEncodingDetector.getReader(in, null, 5000);
        try {
            resp.setContentType("text/html; charset=" + reader.getEncoding());
            Parser parser = new KanjiParser(dictionaries, null);
            parser.setIgnoreNewlines(true);
            HTMLAnnotator annotator = new HTMLAnnotator(parser);
            annotator.annotate(rewriter.getDocumentBase(), reader, resp.getWriter(), rewriter);
        } finally {
            in.close();
        }
    }

    protected void forwardRequestHeaders(URLConnection connection, HttpServletRequest req) {
        String via = req.getHeader("Via");
        if (via == null) via = ""; else via += ", ";
        via += req.getProtocol() + " " + req.getServerName() + ":" + req.getServerPort();
        connection.setRequestProperty("Via", via);
        if (connection.getURL().getProtocol().startsWith("http")) {
            String host = connection.getURL().getHost();
            if (host != null) {
                connection.setRequestProperty("Host", host);
                getServletContext().log("host header: " + host);
            }
            String referer = req.getHeader("referer");
            if (referer != null) {
                int index = referer.lastIndexOf(req.getServletPath() + "/");
                if (index != -1) {
                    Object[] out = JGlossURLRewriter.parseEncodedPath(referer.substring(index + req.getServletPath().length()));
                    if (out != null) referer = (String) out[2];
                }
                connection.setRequestProperty("referer", referer);
                getServletContext().log("referer: " + referer + " (" + req.getHeader("referer") + ")");
            } else getServletContext().log("no referrer");
            for (Enumeration names = req.getHeaderNames(); names != null && names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                if (!noForwardHeaders.contains(name.toLowerCase())) {
                    StringBuffer value = new StringBuffer();
                    for (Enumeration values = req.getHeaders(name); values.hasMoreElements(); ) {
                        if (value.length() > 0) value.append(',');
                        value.append((String) values.nextElement());
                    }
                    connection.setRequestProperty(name, value.toString());
                    getServletContext().log("request header " + name + ": " + value.toString());
                }
            }
        }
    }

    protected void forwardResponseHeaders(URLConnection connection, HttpServletRequest req, HttpServletResponse resp, URLRewriter rewriter) {
        String via = connection.getHeaderField("Via");
        if (via == null) via = ""; else via += ", ";
        via += req.getProtocol() + " " + req.getServerName() + ":" + req.getServerPort();
        resp.setHeader("Via", via);
        String refresh = connection.getHeaderField("refresh");
        if (refresh != null) {
            getServletContext().log("new refresh header: " + refresh);
            int urlstart = refresh.toLowerCase().indexOf("URL=");
            int urlend = refresh.indexOf(';', urlstart);
            if (urlend == -1) urlend = refresh.length();
            try {
                refresh = refresh.substring(0, urlstart) + rewriter.rewrite(refresh.substring(urlstart, urlend)) + refresh.substring(urlend);
            } catch (MalformedURLException ex) {
                getServletContext().log(ex.getMessage());
            }
            resp.setHeader("Refresh", refresh);
            getServletContext().log("new refresh header: " + refresh);
        }
        if (connection.getURL().getProtocol().startsWith("http")) {
            String location = connection.getHeaderField("location");
            if (location != null) {
                getServletContext().log("location: " + location);
                try {
                    location = rewriter.rewrite(location);
                } catch (MalformedURLException ex) {
                    getServletContext().log("malformed location");
                }
                resp.setHeader("Location", location);
                getServletContext().log("new location: " + location);
            }
            int i = 1;
            String name;
            while ((name = connection.getHeaderFieldKey(i)) != null) {
                if (!noForwardHeaders.contains(name.toLowerCase())) {
                    resp.addHeader(name, connection.getHeaderField(i));
                    getServletContext().log("response header " + name + ": " + connection.getHeaderField(i));
                }
                i++;
            }
        }
    }

    /**
     * Splits a string in a list of strings. Whitespace at the beginning and end of
     * an element in the list is removed.
     * 
     * @param s String containing a list of items.
     * @param separator Character which separates the items. The special character ' ' will
     *        split on any whitespace.
     * @return List of strings with whitespace at beginning and end removed.
     */
    protected List split(String s, char separator) {
        List out = new LinkedList();
        boolean inword = false;
        StringBuffer word = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!inword) {
                if (c <= 32) continue;
                if (c == separator) continue;
                inword = true;
            } else if (c == separator || separator == ' ' && c <= 32) {
                inword = false;
                out.add(word.toString().trim());
                word.delete(0, word.length());
            }
            if (inword) word.append(c);
        }
        if (inword) out.add(word.toString().trim());
        return out;
    }

    /**
     * Build the Accept-Encoding header sent to the remote server. The accepted encodings are
     * a subset of the encodings understood by the client and the servlet. Currently the "q" attribute
     * is not supported.
     */
    protected String buildAcceptEncoding(String acceptEncoding) {
        if (!enableCompression) return "identity";
        if (acceptEncoding == null) return null;
        boolean usegzip;
        boolean usedeflate;
        if (acceptEncoding.indexOf('*') != -1) {
            usegzip = true;
            usedeflate = true;
        } else {
            usegzip = (acceptEncoding.indexOf("gzip") != -1);
            usedeflate = (acceptEncoding.indexOf("deflate") != -1);
        }
        StringBuffer out = new StringBuffer(30);
        if (usegzip) out.append("gzip,");
        if (usedeflate) out.append("deflate,");
        out.append("identity");
        return out.toString();
    }
}
