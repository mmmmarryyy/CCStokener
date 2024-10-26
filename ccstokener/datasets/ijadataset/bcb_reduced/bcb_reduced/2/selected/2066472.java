package net.sf.xsltbuddy.xslt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.xsltbuddy.XSLTBuddy;
import org.apache.log4j.Category;
import org.apache.xml.utils.WrappedRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xbill.DNS.Address;
import org.xml.sax.InputSource;
import HTTPClient.CookieModule;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

/**
 * This class is used to provide http helper routines
 */
public class HTTPUtil {

    private XSLTBuddy buddy;

    private static final Category logger = Category.getInstance(HTTPUtil.class.getName());

    private static ServletContext context;

    private static Context jndicontext;

    public static final String REQUEST_SCOPE = "request";

    public static final String SESSION_SCOPE = "session";

    public static final String APPLICATION_SCOPE = "application";

    /** Main constructor
   *
   * @throws Exception
   */
    public HTTPUtil(XSLTBuddy buddy) {
        this.buddy = buddy;
    }

    /**
   *
   */
    public void init() throws Exception {
        jndicontext = new InitialContext();
    }

    /** Get session id
   *
   * @param request
   * @return session id
   */
    public String getSessionID(HttpServletRequest request) {
        return request.getSession().getId();
    }

    /** Return session
   *
   * @param request
   * @return Current HTTP Session
   */
    public static HttpSession getSession(Object request) {
        HttpServletRequest req = (HttpServletRequest) request;
        return req.getSession();
    }

    /** Return session
   *
   * @param request
   * @return context path
   */
    public static String getContextPath(Object request) {
        HttpServletRequest req = (HttpServletRequest) request;
        return req.getContextPath();
    }

    /** Get request attribute
   *
   * @param request
   * @param key
   * @return request attribute
   */
    public Object getRequestAttribute(HttpServletRequest request, String key) {
        return request.getAttribute(key);
    }

    /** Set request attribute
   *
   * @param request
   * @param key
   * @param value
   */
    public void setRequestAttribute(HttpServletRequest request, String key, Object value) {
        request.setAttribute(key, value);
    }

    /** Find attribute
   *
   * @param request
   * @param key
   * @return request attribute
   */
    public Object findRequestAttribute(HttpServletRequest request, String key) {
        Object attribute = this.getRequestAttribute(request, key);
        if (attribute == null) {
            if ((request.getSession() != null)) {
                attribute = request.getSession().getAttribute(key);
                if (attribute == null) {
                    attribute = request.getSession().getServletContext().getAttribute(key);
                }
            }
        }
        return attribute;
    }

    /** Find attribute
   *
   * @param request
   * @param key
   * @return request attribute
   */
    public Object findRequestAttribute(HttpServletRequest request, String key, String scope) {
        if (scope != null) {
            if (scope.equals(REQUEST_SCOPE)) {
                return this.getRequestAttribute(request, key);
            } else if (scope.equals(SESSION_SCOPE)) {
                return request.getSession().getAttribute(key);
            } else if (scope.equals(APPLICATION_SCOPE)) {
                return request.getSession().getServletContext().getAttribute(key);
            }
        } else {
            return this.findRequestAttribute(request, key);
        }
        return null;
    }

    /** Get request parameter
   *
   * @param request
   * @param key
   * @return request parameter
   */
    public String getRequestParameter(HttpServletRequest request, String key) {
        return request.getParameter(key);
    }

    /** Get request header
   *
   * @param request
   * @param key
   * @return request header
   */
    public String getRequestHeader(HttpServletRequest request, String key) {
        return request.getHeader(key);
    }

    /** Get request header names
   *
   * @param request
   * @return request header name list
   */
    public Enumeration getRequestHeaderNames(HttpServletRequest request) {
        return request.getHeaderNames();
    }

    /** Get request parameter names
   *
   * @param request
   * @return request parameter list
   */
    public Enumeration getRequestParameterNames(HttpServletRequest request) {
        return request.getParameterNames();
    }

    /** Get request cookies
   *
   * @param request
   * @return request cookies
   */
    public javax.servlet.http.Cookie[] getRequestCookies(HttpServletRequest request) {
        return request.getCookies();
    }

    /** Create empty node
   * 
   */
    public Node createEmptyNode() {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            return doc.getDocumentElement();
        } catch (Exception ex) {
            throw new WrappedRuntimeException(ex);
        }
    }

    /** Creates a formatted-date node with the given locale.
   * 
   * @param xml
   * @return Node
   */
    public Node toNode(Object xml) {
        try {
            logger.info("XML Class: " + xml.getClass().getName());
            String xmlContent = null;
            if (xml instanceof String) {
                xmlContent = (String) xml;
            } else if (xml instanceof Node) {
                xmlContent = ((Node) xml).toString();
            } else {
                xmlContent = xml.toString();
            }
            logger.info("Converting xml into node:\n --- " + xmlContent + "\n --- \n");
            InputSource isource = new InputSource(new StringReader(xmlContent));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(isource);
            logger.info("Converted xml into node successfully");
            return doc.getDocumentElement();
        } catch (Exception ex) {
            throw new WrappedRuntimeException(ex);
        }
    }

    /** Add resource to jndi
   *
   * @param key
   * @param value
   * @throws Exception
   */
    public void addJNDIResource(String key, Object value) throws Exception {
        jndicontext.bind(key, value);
    }

    /** Get resource from jndi
   *
   * @param key
   * @return JNDI Resource
   * @throws Exception
   */
    public Object getJNDIResource(String key) throws Exception {
        return jndicontext.lookup(key);
    }

    /** Get client host name
   *
   * @param request
   * @return Client Host name
   * @throws Exception
   */
    public String getClientHostName(HttpServletRequest request) throws Exception {
        return this.getHostName(this.getClientIPAddress(request));
    }

    /** Get client host name
   *
   * @param ipAddress
   * @return Host name
   * @throws Exception
   */
    public String getHostName(String ipAddress) throws Exception {
        return InetAddress.getByName(ipAddress).getHostName();
    }

    /** Work out server name from request
   *
   * @param request
   * @return Host name
   * @throws Exception
   */
    public String getServerHostName(HttpServletRequest request) throws Exception {
        URL url = null;
        try {
            url = new URL(request.getRequestURL().toString());
        } catch (MalformedURLException ex) {
            logger.error("Resolving url " + request.getRequestURL().toString() + " failed - " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        if (url != null) {
            String host = url.getHost();
            if (!host.equalsIgnoreCase("localhost")) {
                logger.info("Resolving host: " + host);
                try {
                    InetAddress addr = InetAddress.getByName(host);
                    host = Address.getHostName(addr);
                    if (host.endsWith(".")) {
                        host = host.substring(0, host.length() - 1);
                    }
                } catch (UnknownHostException e) {
                    logger.warn("Unknown host: " + host);
                }
            }
            return host;
        }
        throw new Exception("Could not work out server host name from " + request.getRequestURL().toString());
    }

    /** Caculate client ip address from servlet request
   *
   * @param req
   * @return Client IP Address
   */
    public String getClientIPAddress(HttpServletRequest request) {
        String ipAddress = null;
        if ((request.getHeader("x-forwarded-for") != null) && (request.getHeader("x-forwarded-for").length() > 0)) {
            ipAddress = request.getHeader("x-forwarded-for");
        } else if ((request.getHeader("Client-IP") != null) && (request.getHeader("Client-IP").length() > 0)) {
            StringTokenizer tokenizer = new StringTokenizer(request.getHeader("Client-IP"), ".");
            if (tokenizer.countTokens() == 4) {
                StringBuffer buffer = new StringBuffer();
                String octet[] = new String[4];
                for (int i = 0; i < 4; i++) {
                    octet[i] = tokenizer.nextToken();
                }
                for (int i = 3; i > -1; i--) {
                    buffer.append(octet[i]);
                    if (i > 0) buffer.append(".");
                }
                ipAddress = buffer.toString();
            }
        } else {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    /** Get cookie names
   *
   * @param request
   * @return Cookie Names
   * @throws Exception
   */
    public String[] getCookieNames(HttpServletRequest request) throws Exception {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new String[0];
        }
        String[] cookieNames = new String[cookies.length];
        for (int i = 0; i < cookies.length; i++) {
            cookieNames[i] = cookies[i].getName();
        }
        return cookieNames;
    }

    /** Get XML Feed
   *
   * @param key
   * @param value
   * @throws Exception
   */
    public String getFeedAsString(String source) throws Exception {
        return this.getURLStreamAsString(source, 3000, null, null, null);
    }

    /** Get URL as input stream
   *
   * @param source
   * @param username
   * @param password
   * @return URL Contents
   * @throws Exception
   */
    public String getURLStreamAsString(String source, int timeout, String username, String password, String realm) throws Exception {
        BufferedReader r = (BufferedReader) this.getURLStream(source, timeout, username, password, realm);
        StringBuffer sb = new StringBuffer();
        String line = "";
        while ((line = r.readLine()) != null) {
            sb.append(line + System.getProperty("line.separator"));
        }
        r.close();
        String urlResponse = sb.toString();
        logger.debug("URL Response: " + urlResponse);
        return urlResponse;
    }

    /** Get URL as input stream
   *
   * @param source
   * @param username
   * @param password
   * @return URL Stream
   * @throws Exception
   */
    public Reader getURLStream(String source) throws Exception {
        return this.getURLStream(source, 3000, null, null, null);
    }

    /** Get URL as input stream
   *
   * @param source
   * @param username
   * @param password
   * @return URL Stream
   * @throws Exception
   */
    public Reader getURLStream(String source, int timeout, String username, String password, String realm) throws Exception {
        long start = System.currentTimeMillis();
        logger.info("Get URL Stream: " + source);
        BufferedReader in = null;
        logger.info("New HTTP Connection: " + source);
        HTTPConnection con = null;
        HTTPResponse rsp = null;
        try {
            if (source == null) {
                throw new Exception("Source is null");
            }
            URL url = null;
            if (source.startsWith("/")) {
                url = this.getContext().getResource(source);
                in = new BufferedReader(new InputStreamReader(url.openStream()));
            } else {
                url = new URL(source);
                con = new HTTPConnection(url);
                CookieModule.setCookiePolicyHandler(null);
                con.setDefaultHeaders(new NVPair[] { new NVPair("Connection", "close") });
                if (timeout > 0) {
                    logger.info("Timeout Set To: " + timeout + " milliseconds");
                    con.setTimeout(timeout);
                }
                if ((username != null) && (username.trim().length() > 0)) {
                    logger.info("Adding basic authentication: " + username + "(realm: " + realm + ")");
                    con.addBasicAuthorization(realm, username, password);
                }
                String request = url.getPath();
                if (url.getQuery() != null) {
                    request += "?" + url.getQuery();
                }
                logger.info("Perform GET Http request " + request);
                rsp = con.Get(request);
                logger.info("Start reading response after " + (System.currentTimeMillis() - start) + " millis");
                if (rsp.getStatusCode() >= 300) {
                    logger.error("Received Error: " + rsp.getReasonLine());
                    logger.error(rsp.getText());
                    throw new Exception("Received HTTP Error Status " + rsp.getStatusCode() + ": " + rsp.getReasonLine() + " - " + rsp.getText());
                }
                in = new BufferedReader(new InputStreamReader(rsp.getInputStream()));
            }
        } catch (Exception ex) {
            logger.error("Failed to get URL Stream for source: " + source + " - " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            if (con != null) {
                logger.info("Stopping HTTP Connection");
                con.stop();
            }
            throw new Exception("Exception: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        logger.info("Got URL Stream in " + (System.currentTimeMillis() - start) + " millis");
        return in;
    }

    /** Test connection for given url location
   *
   * @param urlLocation
   * @return connection success flag
   */
    public boolean testConnection(String urlLocation) {
        try {
            logger.info("Testing connection to: " + urlLocation);
            URL url = new URL(urlLocation);
            HTTPClient.HttpURLConnection con = new HTTPClient.HttpURLConnection(url);
            con.connect();
            logger.info("Succesful connection made to: " + urlLocation);
            return true;
        } catch (IOException ex) {
            logger.info("Connection failed to: " + urlLocation);
        }
        logger.info("Attempted connection failed to: " + urlLocation);
        return false;
    }

    /** Resolve resource into a stream
   *
   * @param resource
   * @return resource stream
   * @throws Exception
   */
    public static Reader getResource(String resource) throws Exception {
        return new InputStreamReader(HTTPUtil.getResource(resource, context));
    }

    /** Resolve resource into a stream
   *
   * @param resource
   * @param context
   * @return resource stream
   * @throws Exception
   */
    public static InputStream getResource(String resource, ServletContext context) throws Exception {
        if (resource == null) {
            return null;
        }
        InputStream stream = null;
        if (resource.trim().startsWith("/")) {
            logger.info("Get Local Resource: " + resource.trim());
            URL xslURL = context.getResource(resource.trim());
            String resourceURI = xslURL.toString();
            logger.info("Resolved Local Resource: " + resourceURI);
            stream = xslURL.openStream();
        } else {
            logger.info("Get External XSL Resource: " + resource.trim());
            URL url = new URL(resource.trim());
            String resourceURI = url.toString();
            logger.info("Resolved External XSL Resource: " + resourceURI);
            stream = url.openStream();
        }
        logger.info("Got Resource Stream successfully ");
        return stream;
    }

    public ServletContext getContext() {
        return context;
    }

    public static void setContext(ServletContext context) {
        HTTPUtil.context = context;
    }
}
