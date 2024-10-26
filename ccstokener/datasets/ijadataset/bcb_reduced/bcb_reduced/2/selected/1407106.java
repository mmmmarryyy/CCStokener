package org.jaffa.util;

import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.net.URI;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import org.jaffa.config.Config;

/** Utility class to manipulate URL's and provide some additional capability for using them.
 *
 * @author PaulE
 * @version 1.0
 */
public class URLHelper {

    private static Logger log = Logger.getLogger(URLHelper.class.getName());

    /** This contains the name of the URL protocol that implies the resouce can be located in the classpath */
    public static final String PROTOCOL_CLASSPATH = "classpath";

    /** This contains the name of the URL protocol that implies the resource can be found relative to the location of the web-root on the local file system,
     * This assumes that you are running this inside a servlet, on a web server
     */
    public static final String PROTOCOL_WEBROOT = "webroot";

    /** This is the default value for the initial page of the application */
    public static final String DEFAULT_PAGE = "index.html";

    /** Based on a HttpRequest, this routine will ingure out the URL that represents the root of
     * the web application. It derives the URL protocol, host, port and application.
     * An example of a returned string may be http://www.example.org/myApp/
     * or https://www.example.com:888/SecureApp/
     *
     * @param request A HttpRequest used to derive information about the root of the web applicatoin
     * @return  a string that represents the base URL for this web application
     */
    public static String getBase(HttpServletRequest request) {
        StringBuffer buf = new StringBuffer();
        buf.append(request.getScheme());
        buf.append("://");
        buf.append(request.getServerName());
        if ("http".equals(request.getScheme()) && (80 == request.getServerPort())) {
            ;
        } else if ("https".equals(request.getScheme()) && (443 == request.getServerPort())) {
            ;
        } else {
            buf.append(':');
            buf.append(request.getServerPort());
        }
        buf.append(request.getContextPath() + "/");
        return buf.toString();
    }

    /** Build up a valid URL string based on the supplied source URL string.
     * If the supplied string in null, use the string defined in DEFAULT_PAGE and
     * append this to web root ( using URLHelper.getBase() )
     * If the supplied string already is a valid url, leave it as is.
     * If it is currently invalid, see if it can be made valid by making it relative to
     * the web application base ( using URLHelper.getBase() ), if so return this value
     * If this still doesn't yield a valid url, assume it was null, and build it based on
     * DEFAULT_PAGE and getBase()
     *
     * @param url The URL to build and validate
     * @param req The httpRequest needed to derive the web app base
     * @return A fully expressed valid URL
     */
    public static String buildUrl(String url, HttpServletRequest req) {
        String oldUrl = url;
        if (url == null) {
            url = getBase(req) + DEFAULT_PAGE;
        } else {
            try {
                URL u = new URL(url);
            } catch (java.net.MalformedURLException e) {
                url = getBase(req) + (url.charAt(0) == '/' ? url.substring(1) : url);
                try {
                    URL u = new URL(url);
                } catch (java.net.MalformedURLException ex) {
                    log.info("Invalid URL : " + url);
                    url = getBase(req) + DEFAULT_PAGE;
                }
            }
        }
        log.debug("Converted URL " + oldUrl + " to " + url);
        return url;
    }

    /** Get a complete string based representation of a request's source URL include query parameters
     *
     * @param request HttpRequest containing the url to extracts
     * @return  string representation of URL
     */
    public static String getFullUrl(HttpServletRequest request) {
        return request.getRequestURL().toString() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    /** This method will try to load the input resource off the classpath.
     * If its not found, then it will look for it in the filesystem.
     * A null will be returned, if the resource could not be located.
     *
     * @param resourceName the resource to be located.
     * @throws IOException if any error occurs while opening the stream.
     * @return an input stream for reading from the resource.
     */
    public static InputStream getInputStream(String resourceName) throws IOException {
        InputStream stream = null;
        URL url = getUrl(resourceName);
        if (url != null) {
            stream = url.openStream();
        }
        return stream;
    }

    /** This method will try to load the input resource off the classpath.
     * If its not found, then it will look for it in the filesystem.
     * A null will be returned, if the resource could not be located.
     *
     * This method merely invokes the newExtendedURL() method, returning a null if any exception is raised.
     *
     * @param resourceName the resource to be located.
     * @return a URL for reading from the resource.
     * @deprecated Use the newExtendedURL() method.
     */
    public static URL getUrl(String resourceName) {
        URL url = null;
        try {
            url = newExtendedURL(resourceName);
        } catch (MalformedURLException e) {
        }
        return url;
    }

    /** Create a URL object from a string, this can handle the two new Custom URL
     * protocols, 'classpath:///' and 'webroot:///'. If either of these new ones are
     * used they will be converted into the appropriate 'file://' format.
     * If no protocol is specified, then it'll try to load the input resource off the classpath.
     * If its not found, then it will look for the input resource in the filesystem.
     *
     * @param url source URL that may use one of the new protocols
     * @throws MalformedURLException if the supplied URL is not valid, or can't be translated into something that is valid
     * @return valid URL object, as these two new protocols are not really supported by the java.net.URL object
     */
    public static URL newExtendedURL(String url) throws MalformedURLException {
        if (url == null) {
            throw new IllegalArgumentException("The input url cannot be null");
        }
        URL u = null;
        u = getUrlFromFilesystem(url);
        if (u == null) {
            URI uri = null;
            try {
                uri = new URI(url);
            } catch (java.net.URISyntaxException e) {
                throw new MalformedURLException(e.getMessage());
            }
            if (uri.getScheme() != null) {
                String path = uri.getPath().substring(uri.getPath().startsWith("/") ? 1 : 0);
                if (uri.getScheme().equalsIgnoreCase(PROTOCOL_CLASSPATH)) {
                    u = getUrlFromClasspath(path);
                    if (u == null) {
                        throw new MalformedURLException("Can't Locate Resource in Classpath - " + path);
                    }
                } else if (uri.getScheme().equalsIgnoreCase(PROTOCOL_WEBROOT)) {
                    String root = (String) Config.getProperty(Config.PROP_WEB_SERVER_ROOT, "file:///");
                    String separator = "/";
                    try {
                        if ((new URI(root)).getScheme().equalsIgnoreCase("file")) {
                            separator = File.separator;
                        }
                    } catch (java.net.URISyntaxException e) {
                    }
                    u = new URL(root + (root.endsWith(separator) ? "" : separator) + path);
                } else {
                    u = new URL(url);
                }
            } else {
                u = getUrlFromClasspath(url);
                if (u == null) {
                    throw new MalformedURLException("Can't Locate Resource in Classpath or the Filesystem - " + url);
                }
            }
        }
        return u;
    }

    /** Search for the input resource in the classpath */
    private static URL getUrlFromClasspath(String resourceName) {
        URL url = null;
        ClassLoader classLoader = URLHelper.class.getClassLoader();
        url = classLoader.getResource(resourceName);
        if (url == null) {
            url = ClassLoader.getSystemResource(resourceName);
        }
        return url;
    }

    /** Search for the input resource in the filesystem */
    private static URL getUrlFromFilesystem(String resourceName) {
        URL url = null;
        File f = new File(resourceName);
        try {
            if (f.exists() && f.isFile()) {
                url = f.toURI().toURL();
            }
        } catch (MalformedURLException e) {
            url = null;
        }
        return url;
    }

    /** Test rig
     * @param args none required
     */
    public static void main(String[] args) {
        try {
            String a = "classpath:///org/jaffa/config/framework.properties";
            System.out.println("URL: " + a);
            System.out.println("Real URL: " + newExtendedURL(a).toExternalForm());
            String b = "webroot:///index.html";
            System.out.println("URL: " + b);
            System.out.println("Real URL: " + newExtendedURL(b).toExternalForm());
            String c = "file:///index.html";
            System.out.println("URL: " + c);
            System.out.println("Real URL: " + newExtendedURL(c).toExternalForm());
            String d = "org/jaffa/config/framework.properties";
            System.out.println("URL: " + d);
            System.out.println("Real URL: " + newExtendedURL(d).toExternalForm());
            String e = "bin";
            System.out.println("URL: " + e);
            System.out.println("Real URL: " + newExtendedURL(e).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
