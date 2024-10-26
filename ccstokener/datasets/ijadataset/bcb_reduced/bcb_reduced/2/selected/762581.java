package org.netuno.proteu;

import org.apache.log4j.Logger;
import org.netuno.psamata.Values;
import org.netuno.psamata.script.BasicFuncs;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import org.netuno.psamata.io.OutputStream;

/**
 * Netuno content all objects of Requests and Responses to
 * comunication between server and client, and others resources.
 * @author eduveks
 */
public class Proteu {

    /**
     * Logger.
     */
    private static Logger logger = Logger.getLogger(Proteu.class);

    /**
     * All http content of client
     */
    private StringBuilder clientHttp;

    /**
     * Values of configurations
     */
    private Values config = new Values();

    /**
     * Request Http Head
     */
    private Values requestHead = new Values();

    /**
     * Request Http Post
     */
    private Values requestAll = new Values();

    /**
     * Request Http Post
     */
    private Values requestPost = new Values();

    /**
     * Request Http Post Cache, all content of forms submitted
     */
    private Values requestPostCache = new Values();

    /**
     * Request Http Get
     */
    private Values requestGet = new Values();

    /**
     * Request Http Cookie
     */
    private Values requestCookie = new Values();

    /**
     * Response Http Head
     */
    private Values responseHead = new Values();

    /**
     * Response Http Cookie
     */
    private Values responseCookie = new Values();

    /**
     * Http Session
     */
    private Values session = new Values();

    /**
     * Output bytes to client
     */
    private org.netuno.psamata.io.OutputStream out;

    /**
     * Output bytes to client
     */
    private boolean httpEnterprise = false;

    /**
     * Output bytes to client
     */
    private HttpServlet httpServlet = null;

    /**
     * Output bytes to client
     */
    private HttpServletRequest httpServletRequest = null;

    /**
     * Output bytes to client
     */
    private HttpServletResponse httpServletResponse = null;

    private Script script = null;

    /**
     * Uploads
     */
    private List<String> httpUploads = new ArrayList<String>();

    /**
     * Proteu for Enterprise.
     * @param servlet Servlet
     * @param request Servlet Request
     * @param response Servlet Response
     * @param out Output
     * @throws org.netuno.proteu.ProteuException Exception
     */
    public Proteu(HttpServlet servlet, HttpServletRequest request, HttpServletResponse response, org.netuno.psamata.io.OutputStream out) throws ProteuException {
        try {
            httpEnterprise = true;
            httpServlet = servlet;
            httpServletRequest = request;
            httpServletResponse = response;
            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement().toString();
                requestHead.set(name, request.getHeader(name));
            }
            requestHead.set("Method", request.getMethod());
            requestHead.set("URL", request.getRequestURL().substring(request.getRequestURL().indexOf(requestHead.getString("Host")) + requestHead.getString("Host").length()));
            if (request.getQueryString() != null && !request.getQueryString().equals("")) {
                requestGet = new Values(Http.buildForm(request.getQueryString()), "&", "=", Http.getCharset(requestHead));
            }
            Enumeration parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement().toString();
                String value = "";
                if (request.getParameterValues(name) != null && request.getParameterValues(name).length > 1) {
                    StringBuilder v = new StringBuilder(value);
                    String[] values = request.getParameterValues(name);
                    for (int x = 0; x < values.length; x++) {
                        if (x > 0) {
                            v.append(",");
                        }
                        v.append(values[x]);
                    }
                    value = v.toString();
                } else {
                    value = request.getParameter(name);
                }
                requestPost.set(name, value);
            }
            if (request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/form-data")) {
                httpUploads = Http.buildPostMultipart(new org.netuno.psamata.io.InputStream(request.getInputStream()), requestHead, requestPost);
            }
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    requestCookie.set(cookie.getName(), cookie.getValue());
                }
            }
            loadSession();
            this.out = out;
            init();
        } catch (Exception e) {
            throw new ProteuException(e);
        }
    }

    /**
     * Proteu for Netuno
     * @param clientHttp All http content of client
     * @param requestHead Request Http Head
     * @param requestPost Request Http Post
     * @param requestGet Request Http Get
     * @param requestCookie Request Http Cookie
     * @param responseHead Response Http Head
     * @param responseCookie Response Http Cookie
     * @param out Output bytes to client
     */
    public Proteu(StringBuilder clientHttp, Values requestHead, Values requestPost, Values requestGet, Values requestCookie, Values responseHead, Values responseCookie, org.netuno.psamata.io.OutputStream out) {
        this.clientHttp = clientHttp;
        this.config = new Values();
        this.requestHead = requestHead;
        this.requestPost = requestPost;
        this.requestGet = requestGet;
        this.requestCookie = requestCookie;
        this.responseHead = responseHead;
        this.responseCookie = responseCookie;
        this.out = out;
        init();
    }

    private void init() {
        config.set("Public-Dir", Config.getPublic());
        if (!isEnterprise() && org.netuno.proteu.Config.isStarting()) {
            if (Config.isRebuild()) {
                Compile.clear();
                Compile.makeBuildFolders(new java.io.File(Config.getPublic() + Config.getRebuildRestrict()), new java.io.File(Config.getBuild() + Config.getRebuildRestrict()));
                Compile.compile(getOutput(), new java.io.File(Config.getPublic() + Config.getRebuildRestrict()), new java.io.File(Config.getBuild() + Config.getRebuildRestrict()));
            }
        }
        if (Config.isSessionActive() && !isEnterprise()) {
            if (requestCookie.getString("proteu_session").equals("")) {
                resetSession();
            }
            if (!loadSession()) {
                resetSession();
            }
        }
        if (Config.isSessionActive() || isEnterprise()) {
            String postCacheKey = "proteu_postcache_" + requestHead.getString("URL") + "?" + getRequestGet().getInFormat("&", "=", true);
            if (!getSession().getString(postCacheKey).equals("")) {
                requestPostCache = (Values) session.get(postCacheKey);
            }
            if (getRequestHead().getString("Method").equalsIgnoreCase("post")) {
                getSession().set(postCacheKey, getRequestPost());
                requestPostCache = getRequestPost();
            }
        }
        Enumeration postKeys = requestPost.getKeys();
        while (postKeys.hasMoreElements()) {
            String name = postKeys.nextElement().toString();
            requestAll.set(name, requestPost.get(name));
        }
        Enumeration getKeys = requestGet.getKeys();
        while (getKeys.hasMoreElements()) {
            String name = getKeys.nextElement().toString();
            requestAll.set(name, requestGet.get(name));
        }
        Enumeration headKeys = requestHead.getKeys();
        while (headKeys.hasMoreElements()) {
            String name = headKeys.nextElement().toString();
            requestAll.set(name, requestHead.get(name));
        }
        out.setNotify(new OutputNotify(this));
        script = new Script(this);
        logger.info("Proteu initialized");
    }

    /**
     * Get client http header.
     * @return Client http
     */
    public StringBuilder getClientHttp() {
        return clientHttp;
    }

    /**
     * Get config.
     * @return Config
     */
    public Values getConfig() {
        return config;
    }

    /**
     * Get output.
     * @return Output
     */
    public OutputStream getOutput() {
        return out;
    }

    /**
     * Get request cookie.
     * @return Cookies
     */
    public Values getRequestCookie() {
        return requestCookie;
    }

    /**
     * Get request GET parammeters.
     * @return GET parameters
     */
    public Values getRequestGet() {
        return requestGet;
    }

    /**
     * Get request http headers parameters.
     * @return Http headers parameters
     */
    public Values getRequestHead() {
        return requestHead;
    }

    /**
     * Get request POST parameters.
     * @return POST parameters.
     */
    public Values getRequestPost() {
        return requestPost;
    }

    /**
     * Get request POST Cache parameters.
     * @return POST Cache parameters
     */
    public Values getRequestPostCache() {
        return requestPostCache;
    }

    /**
     * Get request all parameters.
     * @return All parameters
     */
    public Values getRequestAll() {
        return requestAll;
    }

    /**
     * Get response cookies.
     * @return Cookies
     */
    public Values getResponseCookie() {
        return responseCookie;
    }

    /**
     * Get response http headers parameters.
     * @return Http headers parameters
     */
    public Values getResponseHead() {
        return responseHead;
    }

    /**
     * Get sessions.
     * @return Sessions
     */
    public Values getSession() {
        return session;
    }

    /**
     * Is enterprise mode.
     * @return Enterprise mode
     */
    public boolean isEnterprise() {
        return httpEnterprise;
    }

    /**
     * Get Servlet.
     * @return Servlet
     */
    public HttpServlet getServlet() {
        return httpServlet;
    }

    /**
     * Get servlet request.
     * @return Servlet request
     */
    public HttpServletRequest getServletRequest() {
        return httpServletRequest;
    }

    /**
     * Get servlet response.
     * @return Servlet response
     */
    public HttpServletResponse getServletResponse() {
        return httpServletResponse;
    }

    public Script getScript() {
        return script;
    }

    /**
     * Load session.
     */
    private boolean loadSession() {
        if (isEnterprise()) {
            HttpSession httpSession = getServletRequest().getSession();
            Enumeration httpSessionAttributeNames = httpSession.getAttributeNames();
            while (httpSessionAttributeNames.hasMoreElements()) {
                String name = httpSessionAttributeNames.nextElement().toString();
                session.set(name, httpSession.getAttribute(name));
            }
            return true;
        } else {
            Object obj = Config.getSessions().get(requestCookie.getString("proteu_session"));
            if (obj == null) {
                return false;
            }
            session = (Values) obj;
            return true;
        }
    }

    /**
     * Save session.
     */
    public void saveSession() {
        if (isEnterprise()) {
            Enumeration sessionEnum = session.getKeys();
            while (sessionEnum.hasMoreElements()) {
                String name = sessionEnum.nextElement().toString();
                getServletRequest().getSession().setAttribute(name, session.get(name));
            }
        } else {
            if (requestCookie.getString("proteu_session").equals("")) {
                resetSession();
            }
            session.set("proteu_session_time", "" + System.currentTimeMillis());
            session.set("proteu_session_id", "" + requestCookie.getString("proteu_session"));
            Config.getSessions().set(requestCookie.getString("proteu_session"), session);
        }
    }

    /**
     * Destroy actual session and create a new session.
     */
    public void resetSession() {
        if (isEnterprise()) {
            saveSession();
            loadSession();
        } else {
            long time = (new java.util.Date()).getTime();
            String id = (1000000 * Math.random()) + "" + time + "" + (1000000 * Math.random());
            try {
                responseCookie.set("proteu_session", org.netuno.psamata.security.MD5.crypt(id));
            } catch (Exception e) {
                logger.error("Reset session", e);
                throw new Error(e);
            }
            requestCookie.set("proteu_session", responseCookie.getString("proteu_session"));
            session.set("proteu_session_id", requestCookie.getString("proteu_session"));
            session.set("proteu_session_time", "" + time);
            saveSession();
            loadSession();
        }
    }

    /**
     * Save Post Cache in session.
     */
    public void savePostCache() {
        session.set("postCache_" + requestHead.getString("URL"), requestPostCache.getInFormat("&", "="));
        saveSession();
    }

    /**
     * Start communication with client, send Http Head and ready for transmission of more data.
     */
    public void start() {
        try {
            if (isEnterprise()) {
                saveSession();
                getServletResponse().setContentType(responseHead.getString("Content-Type"));
                Enumeration cookie = responseCookie.getKeys();
                while (cookie.hasMoreElements()) {
                    String name = cookie.nextElement().toString();
                    getServletResponse().addCookie(new Cookie(name, responseCookie.getString(name)));
                }
                Enumeration head = responseHead.getKeys();
                while (head.hasMoreElements()) {
                    String name = head.nextElement().toString();
                    getServletResponse().addHeader(name, responseHead.getString(name));
                }
            } else {
                saveSession();
                out.println(responseHead.getString("HTTP"));
                responseHead.remove("HTTP");
                out.print(responseHead.getInFormat("\n", ": ").equals("") ? "" : responseHead.getInFormat("\n", ": ") + "\n");
                if (!responseCookie.getInFormat("; ", "=").equals("")) {
                    out.println("Set-Cookie: " + responseCookie.getInFormat("\nSet-Cookie: ", "="));
                }
                out.println();
            }
        } catch (java.io.IOException e) {
            logger.warn("HTTP header wasn't sent", e);
            throw new Error(e);
        }
        logger.info("HTTP header sent");
    }

    /**
     * Close connection with client.
     */
    public void close() {
        try {
            out.flush();
            out.close();
            logger.info("Connection closed");
        } catch (Exception e) {
            logger.error("Close connection", e);
            throw new Error(e);
        }
    }

    /**
     * Redirect client to other url.
     * @param href Cliento go to this url
     */
    public void redirect(String href) {
        if (isEnterprise()) {
            try {
                this.start();
                this.getServletResponse().sendRedirect(href);
            } catch (Exception e) {
                logger.error("Redirect", e);
                throw new Error(e);
            }
        } else {
            try {
                this.responseHead.set("HTTP", "HTTP/1.1 302 Object Moved");
                this.responseHead.set("Connection", "close");
                this.responseHead.set("Location", href);
                out.println("<head><title>Document Moved</title></head>");
                out.println("<body><h1>Object Moved</h1>This document may be found <a HREF=\"" + href + "\">here</a></body>");
                out.flush();
                out.close();
                logger.info("Client redirect to " + href);
            } catch (Exception e) {
                logger.error("Redirect", e);
                throw new Error(e);
            }
        }
    }

    /**
     * Proxy, bridge between client and other server.
     * @param server Server
     * @param port Port
     */
    public synchronized void proxy(String server, int port) {
        try {
            String _clientHttp = BasicFuncs.replace(clientHttp.toString(), ":85", "") + "\n\n";
            java.net.Socket client = new java.net.Socket(server, port);
            org.netuno.psamata.io.OutputStream output = new org.netuno.psamata.io.OutputStream(client.getOutputStream());
            org.netuno.psamata.io.InputStream input = new org.netuno.psamata.io.InputStream(client.getInputStream());
            this.out.setStart(false);
            output.write(_clientHttp.getBytes());
            while (true) {
                int _byte = input.read();
                if (_byte == -1) {
                    break;
                }
                this.out.write(_byte);
            }
            output.close();
            input.close();
            client.close();
        } catch (Exception e) {
            logger.error("Proxy", e);
            throw new Error(e);
        }
    }

    /**
     * Include another html content in html sending to client.
     * @param href Address of html for including
     */
    public void include(String href) throws ProteuException {
        try {
            if (href.toLowerCase().startsWith("http://")) {
                java.net.URLConnection urlConn = (new java.net.URL(href)).openConnection();
                Download.sendInputStream(this, urlConn.getInputStream());
            } else {
                requestHead.set("JCN_URL_INCLUDE", href);
                Url.build(this);
            }
        } catch (ProteuException pe) {
            throw pe;
        } catch (Throwable t) {
            logger.error("Include", t);
            throw new ProteuException(t.getMessage(), t);
        }
    }

    /**
     * HTML Encoder.
     * @param text Text to encode
     * @return Text encoded in Html
     */
    public String HTMLencoder(String text) {
        char[] _char = text.toCharArray();
        String _final = "";
        for (int x = 0; x < _char.length; x++) {
            _final += "&#" + ((int) _char[x]) + ";";
        }
        return _final;
    }

    /**
     * URL Decoder.
     * @param text Text to decode
     * @return Text decoded
     */
    public static String URLdecoder(String text) {
        return URLdecoder(text, Config.getCharacterEncoding());
    }

    /**
     * URL Decoder.
     * @param text Text to decode
     * @param charSet Character Encoding
     * @return Text decoded
     */
    public static String URLdecoder(String text, String charSet) {
        try {
            return java.net.URLDecoder.decode(text, charSet);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * URL Encoder.
     * @param text Text to encode
     * @return Text encoded
     */
    public static String URLencoder(String text) {
        return URLencoder(text, Config.getCharacterEncoding());
    }

    /**
     * URL Encoder.
     * @param text Text to encode
     * @param charSet Character Encoding
     * @return Text encoded
     */
    public static String URLencoder(String text, String charSet) {
        try {
            return java.net.URLEncoder.encode(text, charSet);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public void clear() {
        if (isEnterprise()) {
            if (httpUploads != null) {
                for (String file : httpUploads) {
                    org.netuno.psamata.io.File.delete(file);
                }
                httpUploads.clear();
            }
        }
        if (config != null) {
            config.removeAll();
        }
        if (requestHead != null) {
            requestHead.removeAll();
        }
        if (requestPost != null) {
            requestPost.removeAll();
        }
        if (requestPostCache != null) {
            requestPostCache.removeAll();
        }
        if (requestGet != null) {
            requestGet.removeAll();
        }
        if (requestCookie != null) {
            requestCookie.removeAll();
        }
        if (requestAll != null) {
            requestAll.removeAll();
        }
        if (responseHead != null) {
            responseHead.removeAll();
        }
        if (responseCookie != null) {
            responseCookie.removeAll();
        }
        if (session != null) {
            session.removeAll();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        config = null;
        clientHttp = null;
        requestHead = null;
        requestPost = null;
        requestPostCache = null;
        requestGet = null;
        requestCookie = null;
        requestAll = null;
        responseHead = null;
        responseCookie = null;
        session = null;
        out = null;
        clientHttp = null;
    }

    class OutputNotify implements org.netuno.psamata.io.OutputStreamNotify {

        private Proteu proteu = null;

        public OutputNotify(Proteu p) {
            proteu = p;
        }

        public void start() {
            proteu.start();
        }

        public void finish() {
        }
    }
}
