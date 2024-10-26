package com.ideo.sweetdevria.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ideo.sweetdevria.config.PropertiesManager;
import com.ideo.sweetdevria.page.Page;

public class URLUtils {

    private static final Log LOG = LogFactory.getLog(URLUtils.class);

    private static Boolean useServerLocalAdress = null;

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String SERVER_HTTP_CALL_SCHEME = "sweetdev-ria.serverHttpCall.localScheme";

    private static final String SERVER_HTTP_CALL_NAME = "sweetdev-ria.serverHttpCall.localName";

    private static final String SERVER_HTTP_CALL_PORT = "sweetdev-ria.serverHttpCall.localPort";

    private static final String SERVER_HTTP_CALL_CONTEXT = "sweetdev-ria.serverHttpCall.localContext";

    private static final String SERVER_URL_PARAMETER = "sweetdev-ria.serverHttpCall.localURL";

    private static final String FLAG_USE_LOCAL_INFO = "sweetdev-ria.serverHttpCall.useServerLocalAdress";

    private static final String JNDI_URL_KEY = "jndi:";

    private static String prefixedScheme = null;

    private static String prefixedServerName = null;

    private static String prefixedServerPort = null;

    private static String prefixedContext = null;

    private static URL prefixedURL = null;

    /**
	 * Open an input stream to the specified URL.
	 * Suffix the URL with the JSESSION ID if specified.
	 * @param url URL link to the file	
	 * @param sessionId  Session ID to specify in the URL (could be null).
	 * @return An opened Input stream (should be closed).
	 * @throws IOException
	 */
    public static InputStream getFileContent(URL url, String sessionId) throws IOException {
        url = new URL(escapeDoubleSlash(url.toString()));
        URLConnection conn = url.openConnection();
        if (sessionId != null) {
            conn.addRequestProperty("Cookie", "JSESSIONID=" + sessionId);
        }
        conn.connect();
        return conn.getInputStream();
    }

    /**
	 * Return the content of an url
	 * @param url the url to call
	 * @param request the original request processing the url demand  
	 * @return a String containing the call result
	 * @throws IOException
	 */
    public static String getURLContent(URL url, HttpServletRequest request, String inputs) throws IOException {
        String formatUrl = escapeDoubleSlash(url.toString());
        StringBuffer buffer = new StringBuffer();
        formatUrl = addParamToUrl(formatUrl, inputs, null);
        URLConnection conn = null;
        InputStreamReader inStream = null;
        try {
            conn = new URL(formatUrl).openConnection();
            propagateCookies(request, conn);
            tryPropagate(conn, request, "Accept");
            tryPropagate(conn, request, "Accept-Language");
            conn.addRequestProperty("Content-Type", "text/html;charset=UTF-8");
            conn.connect();
            String encoding = null;
            inStream = getInputStreamReaderFromUrl(conn, encoding);
            BufferedReader in = new BufferedReader(inStream);
            String line;
            while ((line = in.readLine()) != null) buffer.append(line).append("\n");
        } finally {
            if (inStream != null) {
                inStream.close();
            } else if (conn != null && conn.getInputStream() != null) {
                conn.getInputStream().close();
            }
        }
        return buffer.toString();
    }

    /**
	 * Retour un InputStreamReader branche sur une URL Connexion avec l'encoding passe en parametre
	 */
    private static InputStreamReader getInputStreamReaderFromUrl(URLConnection conn, String encoding) throws IOException {
        InputStreamReader inStream = null;
        String contentType = conn.getContentType();
        if (contentType != null && contentType.lastIndexOf('=') >= 0) {
            encoding = contentType.substring(conn.getContentType().lastIndexOf('=') + 1, contentType.length());
            try {
                inStream = new InputStreamReader(conn.getInputStream(), encoding);
            } catch (UnsupportedEncodingException uee) {
                LOG.warn("UnsupportedEncodingException - Encoding not valid :" + uee.getMessage());
                if (inStream != null) inStream.close();
                inStream = new InputStreamReader(conn.getInputStream());
            }
        } else {
            inStream = new InputStreamReader(conn.getInputStream());
        }
        return inStream;
    }

    /**
	 * Propage un entete de la requete courrante vers la connexion url 
	 */
    private static void tryPropagate(URLConnection conn, HttpServletRequest request, String property) {
        if (request.getHeader(property) != null) {
            conn.addRequestProperty(property, request.getHeader(property));
        }
    }

    private static String escapeDoubleSlash(String url) throws MalformedURLException {
        return url.replaceAll("///", "/").replaceAll("://", ":////").replaceAll("//", "/");
    }

    private static String appendPageId(String url, String pageId) {
        if (pageId == null) return url;
        return addParamToUrl(url, Page.REQ_PAGE_ID, pageId);
    }

    /**
	 * Builds an URL object from the url sent
	 * 
	 * @param request
	 * @param url the url to format
	 * @param pageId used to propagate the current page configuration, and use the statefull zones
	 * @return an URL formatted from a request coming from a SweetDEV RIA Ajax call
	 * @throws MalformedURLException
	 */
    public static URL formatAjaxUrl(HttpServletRequest request, String url, String pageId) throws MalformedURLException {
        if (url == null) url = request.getHeader("referer");
        return formatAjaxUrl(appendPageId(url, pageId), request);
    }

    /**
	 * FIX : Portlet compatibility. This function is used by RIA Component which load URL content from server.
	 * Builds an URL object from the url sent.
	 * IMPORTANT : this URL should be used by the server.
	 * 
	 * @param request
	 * @param url the url to format
	 * @param pageId used to propagate the current page configuration, and use the statefull zones
	 * @return an URL formatted from a request coming from a SweetDEV RIA Ajax call
	 * @throws MalformedURLException
	 */
    public static URL formatAjaxUrlForServer(HttpServletRequest request, String url, String pageId) throws MalformedURLException {
        if (url == null) url = request.getHeader("referer");
        return formatUrlForServer(appendPageId(url, pageId), request, true);
    }

    /**
	 * Build an url corresponding to the string.
	 * Convert either absolutes urls (http://...) , absolutes ones (/mypage...) or relatives ones (url).  
	 * 
	 * @param url the url to build
	 * @param pageId used to propagate the current page configuration, and use the statefull zones
	 * @return a full URL corresponding to the string given in parameter
	 * @throws MalformedURLException 
	 */
    public static URL formatUrl(String url, HttpServletRequest request, String pageId) throws MalformedURLException {
        return formatUrl(appendPageId(url, pageId), request);
    }

    /**
	 * FIX : Portlet compatibility. This function is used by RIA Component which load URL content from server.
	 * Build an url corresponding to the string. 
	 * Convert either absolutes urls (http://...) , absolutes ones (/mypage...) or relatives ones (url).  
	 * IMPORTANT : this URL should be used by the server.
	 * 
	 * @param url the url to build
	 * @param pageId used to propagate the current page configuration, and use the statefull zones
	 * @return a full URL corresponding to the string given in parameter
	 * @throws MalformedURLException 
	 */
    public static URL formatUrlForServer(String url, HttpServletRequest request, String pageId) throws MalformedURLException {
        return formatUrlForServer(appendPageId(url, pageId), request, false);
    }

    /**
	 * Format an url with the correct host, port, and context path
	 * @param url
	 * @param request
	 * @return
	 * @throws MalformedURLException
	 */
    private static URL formatUrl(String url, HttpServletRequest request) throws MalformedURLException {
        if (url.startsWith("http://") || url.startsWith("https://")) return new URL(url);
        if (url.startsWith("/")) {
            String filePath = request.getContextPath();
            filePath = concatUrlWithSlaches(filePath, url);
            return new URL(request.getScheme(), request.getServerName(), request.getServerPort(), filePath);
        }
        String file = getFileFromUrl(request.getServletPath().toString());
        String filePath = concatUrlWithSlaches(request.getContextPath(), concatUrlWithSlaches(file, url));
        return new URL(request.getScheme(), request.getServerName(), request.getServerPort(), filePath);
    }

    /**
	 * Format an url with the correct host, port, and context path
	 * @param url
	 * @param request
	 * @return
	 * @throws MalformedURLException
	 */
    private static URL formatUrlForServer(String url, HttpServletRequest request, boolean fromAjax) throws MalformedURLException {
        if (url.startsWith("http://") || url.startsWith("https://")) return new URL(url);
        if (fromAjax && !url.startsWith("/")) {
            String p = request.getHeader("Referer");
            if (p == null) {
                return formatUrl(url, request);
            } else {
                String path = getFileFromUrl(p);
                return new URL(concatUrlWithSlaches(path, url));
            }
        }
        String path = getFileFromUrl(request.getServletPath().toString());
        String scheme = getServerScheme(request);
        String serverName = getServerName(request);
        String serverPort = getServerPort(request);
        String context = getServerContext(request);
        String filePath = "";
        if (url.startsWith("/")) {
            filePath = concatUrlWithSlaches(request.getContextPath(), url);
        } else {
            filePath = concatUrlWithSlaches(context, concatUrlWithSlaches(path, url));
        }
        int iServerPort = (serverPort != null) ? Integer.parseInt(serverPort) : -1;
        return new URL(scheme, serverName, iServerPort, filePath);
    }

    /**
	 * Return true if property 'sweetdev-ria.serverHttpCall.useServerLocalAdress' is set to TRUE.
	 * This property force URLUtils to use :
	 * - request.getLocalName() instead of request.getServerName()
	 * - request.getLocalPort() instead of request.getServerPort()
	 * @return
	 */
    private static boolean isLocalAdressInUse() {
        Boolean useLocalAdress = useServerLocalAdress;
        if (useLocalAdress == null) {
            String config = PropertiesManager.getOptionalProperty(FLAG_USE_LOCAL_INFO);
            if (config != null) {
                useLocalAdress = Boolean.valueOf(config);
            }
        }
        if (useLocalAdress == null) useLocalAdress = Boolean.FALSE;
        return useLocalAdress.booleanValue();
    }

    /**
	 * return Server Scheme
	 * @param request
	 * @return
	 */
    public static String getServerScheme(HttpServletRequest request) {
        if (StringUtils.isEmpty(prefixedScheme)) {
            URL serverURL = retrieveURLProperty();
            if (serverURL != null) {
                prefixedScheme = serverURL.getProtocol();
            } else {
                prefixedScheme = PropertiesManager.getOptionalProperty(SERVER_HTTP_CALL_SCHEME);
            }
        }
        String scheme = prefixedScheme;
        if (scheme == null) {
            scheme = request.getScheme();
        }
        return scheme;
    }

    /**
	 * return Server Name
	 * @param request
	 * @return
	 */
    public static String getServerName(HttpServletRequest request) {
        if (StringUtils.isEmpty(prefixedServerName)) {
            URL serverURL = retrieveURLProperty();
            if (serverURL != null) {
                prefixedServerName = serverURL.getHost();
            } else {
                prefixedServerName = PropertiesManager.getOptionalProperty(SERVER_HTTP_CALL_NAME);
            }
        }
        String serverName = prefixedServerName;
        if (serverName == null) {
            serverName = (isLocalAdressInUse()) ? request.getLocalName() : request.getServerName();
        }
        return serverName;
    }

    /**
	 * return Server port
	 * @param request
	 * @return
	 */
    public static String getServerPort(HttpServletRequest request) {
        if (StringUtils.isEmpty(prefixedServerPort)) {
            URL serverURL = retrieveURLProperty();
            if (serverURL != null) {
                prefixedServerPort = Integer.toString(serverURL.getPort());
            } else {
                prefixedServerPort = PropertiesManager.getOptionalProperty(SERVER_HTTP_CALL_PORT);
            }
        }
        String serverPort = prefixedServerPort;
        if (serverPort == null) {
            int iServerPort = (isLocalAdressInUse()) ? request.getLocalPort() : request.getServerPort();
            serverPort = Integer.toString(iServerPort);
        }
        if ("-1".equals(serverPort)) {
            serverPort = null;
        }
        return serverPort;
    }

    private static int getDefaultPort(String scheme) {
        if (StringUtils.isEmpty(scheme)) {
            scheme = "HTTP";
        }
        if ("HTTP".equals(scheme.toUpperCase())) {
            return 80;
        }
        if ("HTTPS".equals(scheme.toUpperCase())) {
            return 443;
        }
        return 80;
    }

    /**
	 * return Server port
	 * @param request
	 * @return
	 */
    public static String getServerContext(HttpServletRequest request) {
        if (StringUtils.isEmpty(prefixedContext)) {
            URL serverURL = retrieveURLProperty();
            if (serverURL != null) {
                prefixedContext = getContextFromURL(serverURL);
            } else {
                prefixedContext = PropertiesManager.getOptionalProperty(SERVER_HTTP_CALL_CONTEXT);
            }
        }
        String context = prefixedContext;
        if (context == null) {
            context = request.getContextPath();
        }
        return concatUrlWithSlaches(context, "");
    }

    /**
	 * 
	 * @param propertyName
	 * @return
	 * @throws MalformedURLException 
	 */
    private static URL retrieveURLProperty() {
        String attributeValue = PropertiesManager.getOptionalProperty(SERVER_URL_PARAMETER);
        if (prefixedURL == null) {
            if (attributeValue != null && attributeValue.indexOf(JNDI_URL_KEY) >= 0) {
                String jndiName = null;
                try {
                    InitialContext ctx = new InitialContext();
                    jndiName = attributeValue.substring(attributeValue.indexOf(JNDI_URL_KEY) + JNDI_URL_KEY.length());
                    prefixedURL = (URL) ctx.lookup(jndiName);
                    if (prefixedURL == null) {
                        LOG.error("Property '" + SERVER_URL_PARAMETER + "' contains an undeclared JNDI Name : " + jndiName);
                    }
                } catch (NamingException e) {
                    LOG.error("Property '" + SERVER_URL_PARAMETER + "' isn't a valid JNDI Name : " + jndiName, e);
                }
            } else {
                try {
                    if (attributeValue != null) {
                        prefixedURL = new URL(attributeValue);
                    }
                } catch (MalformedURLException e) {
                    LOG.error("Property '" + SERVER_URL_PARAMETER + "' isn't a valid URL : " + attributeValue);
                }
            }
        }
        return prefixedURL;
    }

    /**
	 * 
	 * @param url
	 * @param request
	 * @return
	 * @throws MalformedURLException
	 */
    private static URL formatAjaxUrl(String url, HttpServletRequest request) throws MalformedURLException {
        if (url.startsWith("http://") || url.startsWith("https://")) return new URL(url);
        if (url.startsWith("/")) {
            String filePath = request.getRequestURL().toString().replaceAll("RiaController", "");
            return new URL(concatUrlWithSlaches(filePath, url));
        }
        String referer = request.getHeader("Referer").toString();
        if (referer != null) {
            String filePath = getFileFromUrl(referer);
            return new URL(concatUrlWithSlaches(filePath, url));
        }
        throw new MalformedURLException("Url is relative and referer Header information is null.");
    }

    /**
	 * Propagate the whole informations transmitted into the request cookie
	 * @param request the request given in parameter.
	 * @param connection the connection that will propagate the cookies
	 * @throws {@link IllegalStateException}
	 */
    public static void propagateCookies(HttpServletRequest request, URLConnection connection) {
        String cookie = null;
        if (request.getHeader("Cookie") == null) {
            if (request.getSession() != null && request.getSession().getId() != null) {
                cookie = "JSESSIONID=" + request.getSession().getId();
            } else {
                LOG.info("No information about Cookies found in the requesting request.");
            }
        } else {
            cookie = request.getHeader("Cookie");
        }
        if (cookie != null) {
            connection.addRequestProperty("Cookie", cookie);
        }
    }

    /** SWTRIA-971
	 * Test if the url is local in the conext of the referent request.
	 * @param url the Url from which the path will be extracted
	 * @param request Reference request
	 * @return 'true' is the url is local, else 'false'.
	 * @throws MalformedURLException
	 */
    public static boolean isAdressLocal(URL url, HttpServletRequest request) throws MalformedURLException {
        String serverPort = getServerPort(request);
        int iServerPort = (serverPort == null) ? getDefaultPort(getServerScheme(request)) : Integer.parseInt(serverPort);
        int iUrlPort = (url.getPort() < 0) ? getDefaultPort(url.getProtocol()) : url.getPort();
        boolean localAdress = url.getProtocol().equals(getServerScheme(request)) && url.getHost().equals(getServerName(request)) && iUrlPort == iServerPort && url.getPath().indexOf(getServerContext(request)) == 0;
        return localAdress;
    }

    /** SWTRIA-971
	 * Extract from an url the path relative to the context of the reference request.
	 * If the url is no local (@see isAdressLocal(URL url, HttpServletRequest request))
	 * then returns null.
	 * @param url the Url from which the path will be extracted
	 * @param request Reference request
	 * @return the relative path 
	 * @throws MalformedURLException
	 */
    public static String getPathFromUrl(URL url, HttpServletRequest request) throws MalformedURLException {
        if (!isAdressLocal(url, request)) {
            return null;
        }
        return url.getPath().substring(request.getContextPath().length());
    }

    /**
	 * Return the context part of the URL of an JEE resource.
	 * @param url (URL)
	 * @return (String) the context of the JEE resource.
	 */
    public static String getContextFromURL(URL url) {
        String context = "";
        String path = url.getPath();
        if (StringUtils.isNotEmpty(path) && (path.indexOf("/") == 0)) {
            int endContext = path.indexOf("/", 1);
            endContext = (endContext < 0) ? path.length() : endContext;
            context = path.substring(0, endContext);
        }
        return context;
    }

    /**
	 * 
	 * @param url
	 * @return
	 */
    public static String getFileFromUrl(String url) {
        int cutIndex = url.lastIndexOf('/');
        if (cutIndex == -1) cutIndex = url.length() - 1;
        return url.substring(0, cutIndex);
    }

    /**
	 * prepare params and transform them into URL GET params
	 * @param params
	 * @return
	 */
    public static String encodeParams(String params) {
        String encodedParams = null;
        try {
            encodedParams = URLEncoder.encode(params, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            LOG.error("encodeParams failue!, e");
            encodedParams = params;
        }
        encodedParams = encodedParams.replaceAll("%21%3D%21", "=");
        encodedParams = encodedParams.replaceAll("%21%26%21", "&");
        return encodedParams;
    }

    /**
	 * Concat two part of an url into one.
	 * Check that this two part are separated with "/".
	 * Check that there no double "/" separating the two part
	 * <ul>
	 * <li>"firstpart/" + "/secondpart" -> "firstpart/secondpart"
	 * <li>"firstpart" + "/secondpart" -> "firstpart/secondpart"
	 * <li>"firstpart/" + "secondpart" -> "firstpart/secondpart"
	 * <li>"firstpart" + "secondpart" -> "firstpart/secondpart"
	 * </ul>
	 * @param firstPart
	 * @param secondPart
	 * @return 
	 */
    public static String concatUrlWithSlaches(String firstPart, String secondPart) {
        if (firstPart == null) firstPart = "";
        if (secondPart == null) secondPart = "";
        if (!firstPart.endsWith("/") && !secondPart.startsWith("/")) {
            return firstPart + "/" + secondPart;
        } else if (firstPart.endsWith("/") && secondPart.startsWith("/")) {
            return firstPart + secondPart.substring(1);
        }
        return firstPart + secondPart;
    }

    /**
	 * Add a GET parameter to the url. Check if parameters already exists.
	 * This function do not encode paramName or paramValue. This has to been done before if necessary.
	 * <ul>
	 * <li> return "url?paramName=paramValue", if url has no previous paramteers
	 * <li> return "url?[previous parameters]&amp;paramName=paramValue", if url has previous parameters
	 * <li> return "url?paramName=paramValue", if url has no previous parameters
	 * <li> return "url?[previous parameters]&amp;paramName=paramValue", if url has previous parameters
	 * <li> return "url", if paramName is null.
	 * @param url
	 * @param paramName Name of the parameter. Could not be null.
	 * @param paramValue
	 * @return
	 */
    public static String addParamToUrl(String url, String paramName, Object paramValue) {
        if (paramName == null || "".equals(paramName)) return url;
        if (paramValue == null) {
            if (url.indexOf("?") != -1) return url + "&" + paramName; else return url + "?" + paramName;
        }
        if (url.indexOf("?") != -1) return url + "&" + paramName + "=" + paramValue.toString(); else return url + "?" + paramName + "=" + paramValue.toString();
    }

    /**
	 * Add a GET parameter to the url. Check if parameters already exists.
	 * This function do not encode paramName or paramValue. This has to been done before if necessary.
	 * <ul>
	 * <li> return "url?paramName=paramValue", if url has no previous paramteers
	 * <li> return "url?[previous parameters]&amp;paramName=paramValue", if url has previous parameters
	 * <li> return "url?paramName=paramValue", if url has no previous parameters
	 * <li> return "url?[previous parameters]&amp;paramName=paramValue", if url has previous parameters
	 * <li> return "url", if paramName is null.
	 * @param url
	 * @param paramName Name of the parameter. Could not be null.
	 * @param paramValue
	 * @return
	 */
    public static String addParamToUrl(String url, String paramName, String paramValue) {
        return addParamToUrl(url, paramName, (Object) paramValue);
    }
}
