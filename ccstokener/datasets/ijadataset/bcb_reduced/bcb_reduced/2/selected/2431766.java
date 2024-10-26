package org.qedeq.base.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.qedeq.base.trace.Trace;
import org.qedeq.base.utility.YodaUtility;

/**
 * A collection of useful static methods for URL s.
 *
 * @author  Michael Meyling
 */
public final class UrlUtility {

    /** This class, for debugging purpose. */
    private static final Class CLASS = UrlUtility.class;

    /**
     * Constructor, should never be called.
     */
    private UrlUtility() {
    }

    /**
     * Convert file in URL.
     *
     * @param   file    File.
     * @return  URL.
     */
    public static URL toUrl(final File file) {
        try {
            return file.getAbsoluteFile().toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert URL path in file path.
     *
     * @param   url    Convert this URL path.
     * @return  File path.
     */
    public static File transformURLPathToFilePath(final URL url) {
        try {
            return new File(URLDecoder.decode(url.getFile(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create relative address from <code>origin</code> to <code>next</code>.
     * The resulting file path has "/" as directory name separator.
     * If the resulting file path is the same as origin specifies, we return "".
     * Otherwise the result will always have an "/" as last character.
     *
     * @param   origin  This is the original location. Must be a directory.
     * @param   next    This should be the next location. Must also be a directory.
     * @return  Relative (or if necessary absolute) file path.
     */
    public static final String createRelativePath(final File origin, final File next) {
        if (origin.equals(next)) {
            return "";
        }
        final Path org = new Path(origin.getPath().replace(File.separatorChar, '/'), "");
        final Path ne = new Path(next.getPath().replace(File.separatorChar, '/'), "");
        return org.createRelative(ne.toString()).toString();
    }

    /**
     * Simplify file URL by returning a file path.
     *
     * @param   url     URL to simplify.
     * @return  File path (if protocol is "file"). Otherwise just return <code>url</code>.
     */
    public static String easyUrl(final String url) {
        String result = url;
        try {
            final URL u = new URL(url);
            if (u.getProtocol().equalsIgnoreCase("file")) {
                return transformURLPathToFilePath(u).getCanonicalPath();
            }
        } catch (RuntimeException e) {
        } catch (IOException e) {
        }
        return result;
    }

    /**
     * Make local copy of an URL.
     *
     * @param   url             Save this URL.
     * @param   f               Save into this file. An existing file is overwritten.
     * @param   proxyHost       Use this proxy host.
     * @param   proxyPort       Use this port at proxy host.
     * @param   nonProxyHosts   This are hosts not to be proxied.
     * @param   connectTimeout  Connection timeout.
     * @param   readTimeout     Read timeout.
     * @param   listener        Here completion events are fired.
     * @throws  IOException     Saving failed.
     */
    public static void saveUrlToFile(final String url, final File f, final String proxyHost, final String proxyPort, final String nonProxyHosts, final int connectTimeout, final int readTimeout, final LoadingListener listener) throws IOException {
        final String method = "saveUrlToFile()";
        Trace.begin(CLASS, method);
        if (!isSetConnectionTimeOutSupported() && !IoUtility.isWebStarted()) {
            saveQedeqFromWebToBufferApache(url, f, proxyHost, proxyPort, nonProxyHosts, connectTimeout, readTimeout, listener);
            Trace.end(CLASS, method);
            return;
        }
        if (!IoUtility.isWebStarted()) {
            if (proxyHost != null) {
                System.setProperty("http.proxyHost", proxyHost);
            }
            if (proxyPort != null) {
                System.setProperty("http.proxyPort", proxyPort);
            }
            if (nonProxyHosts != null) {
                System.setProperty("http.nonProxyHosts", nonProxyHosts);
            }
        }
        FileOutputStream out = null;
        InputStream in = null;
        try {
            final URLConnection connection = new URL(url).openConnection();
            if (connection instanceof HttpURLConnection) {
                final HttpURLConnection httpConnection = (HttpURLConnection) connection;
                if (isSetConnectionTimeOutSupported()) {
                    try {
                        YodaUtility.executeMethod(httpConnection, "setConnectTimeout", new Class[] { Integer.TYPE }, new Object[] { new Integer(connectTimeout) });
                    } catch (NoSuchMethodException e) {
                        Trace.fatal(CLASS, method, "URLConnection.setConnectTimeout was previously found", e);
                    } catch (InvocationTargetException e) {
                        Trace.fatal(CLASS, method, "URLConnection.setConnectTimeout throwed an error", e);
                    }
                }
                if (isSetReadTimeoutSupported()) {
                    try {
                        YodaUtility.executeMethod(httpConnection, "setReadTimeout", new Class[] { Integer.TYPE }, new Object[] { new Integer(readTimeout) });
                    } catch (NoSuchMethodException e) {
                        Trace.fatal(CLASS, method, "URLConnection.setReadTimeout was previously found", e);
                    } catch (InvocationTargetException e) {
                        Trace.fatal(CLASS, method, "URLConnection.setReadTimeout throwed an error", e);
                    }
                }
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == 200) {
                    in = httpConnection.getInputStream();
                } else {
                    in = httpConnection.getErrorStream();
                    final String errorText = IoUtility.loadStreamWithoutException(in, 1000);
                    throw new IOException("Response code from HTTP server was " + responseCode + (errorText.length() > 0 ? "\nResponse  text from HTTP server was:\n" + errorText : ""));
                }
            } else {
                Trace.paramInfo(CLASS, method, "connection.getClass", connection.getClass().toString());
                in = connection.getInputStream();
            }
            if (!url.equals(connection.getURL().toString())) {
                throw new FileNotFoundException("\"" + url + "\" was substituted by " + "\"" + connection.getURL() + "\" from server");
            }
            final double maximum = connection.getContentLength();
            IoUtility.createNecessaryDirectories(f);
            out = new FileOutputStream(f);
            final byte[] buffer = new byte[4096];
            int bytesRead;
            int position = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                position += bytesRead;
                out.write(buffer, 0, bytesRead);
                if (maximum > 0) {
                    double completeness = position / maximum;
                    if (completeness < 0) {
                        completeness = 0;
                    }
                    if (completeness > 100) {
                        completeness = 1;
                    }
                    listener.loadingCompletenessChanged(completeness);
                }
            }
            listener.loadingCompletenessChanged(1);
        } finally {
            IoUtility.close(out);
            out = null;
            IoUtility.close(in);
            in = null;
            Trace.end(CLASS, method);
        }
    }

    /**
     * Make local copy of a http accessable URL. This method uses apaches HttpClient,
     * but it dosn't work under webstart with proxy configuration. If we don't use this
     * method, the apache commons-httpclient library can be removed
     *
     * @param   url             Save this URL.
     * @param   f               Save into this file. An existing file is overwritten.
     * @param   proxyHost       Use this proxy host.
     * @param   proxyPort       Use this port at proxy host.
     * @param   nonProxyHosts   This are hosts not to be proxied.
     * @param   connectTimeout  Connection timeout.
     * @param   readTimeout     Read timeout.
     * @param   listener        Here completion events are fired.
     * @throws  IOException     Saving failed.
     */
    private static void saveQedeqFromWebToBufferApache(final String url, final File f, final String proxyHost, final String proxyPort, final String nonProxyHosts, final int connectTimeout, final int readTimeout, final LoadingListener listener) throws IOException {
        final String method = "saveQedeqFromWebToBufferApache()";
        Trace.begin(CLASS, method);
        HttpClient client = new HttpClient();
        if (!IoUtility.isWebStarted() && proxyHost != null && proxyHost.length() > 0) {
            final String pHost = proxyHost;
            int pPort = 80;
            if (proxyPort != null) {
                try {
                    pPort = Integer.parseInt(proxyPort);
                } catch (RuntimeException e) {
                    Trace.fatal(CLASS, method, "proxy port not numeric: " + proxyPort, e);
                }
            }
            if (pHost.length() > 0) {
                client.getHostConfiguration().setProxy(pHost, pPort);
            }
        }
        GetMethod httpMethod = new GetMethod(url);
        try {
            httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
            httpMethod.getParams().setSoTimeout(connectTimeout);
            int statusCode = client.executeMethod(httpMethod);
            if (statusCode != HttpStatus.SC_OK) {
                throw new FileNotFoundException("Problems loading: " + url + "\n" + httpMethod.getStatusLine());
            }
            byte[] responseBody = httpMethod.getResponseBody();
            IoUtility.createNecessaryDirectories(f);
            IoUtility.saveFileBinary(f, responseBody);
            listener.loadingCompletenessChanged(1);
        } finally {
            httpMethod.releaseConnection();
            Trace.end(CLASS, method);
        }
    }

    /**
     * This class ist just for solving the lazy loading problem thread save.
     * see <a href="http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom">
     * Initialization_on_demand_holder_idiom</a>.
     */
    private static final class LazyHolderTimeoutMethods {

        /** Lazy initialized constant that knows about the existence of the method
         * <code>URLConnection.setConnectTimeout</code>. This depends on the currently running
         * JVM. */
        private static final boolean IS_SET_CONNECTION_TIMEOUT_SUPPORTED = YodaUtility.existsMethod(URLConnection.class, "setConnectTimeout", new Class[] { Integer.TYPE });

        /** Lazy initialized constant that knows about the existence of the method
         * <code>URLConnection.setReadTimeout</code>. This depends on the currently running
         * JVM. */
        private static final boolean IS_SET_READ_TIMEOUT_SUSPPORTED = YodaUtility.existsMethod(URLConnection.class, "setReadTimeout", new Class[] { Integer.TYPE });

        /**
         * Hidden constructor.
         */
        private LazyHolderTimeoutMethods() {
        }
    }

    /**
     * Is setting of connection timeout supported in current environment?
     *
     * @return  Setting connection timeout supported?
     */
    public static boolean isSetConnectionTimeOutSupported() {
        return LazyHolderTimeoutMethods.IS_SET_CONNECTION_TIMEOUT_SUPPORTED;
    }

    /**
     * Is setting of read timeout supported in current environment?
     *
     * @return  Setting read timeout supported?
     */
    public static boolean isSetReadTimeoutSupported() {
        return LazyHolderTimeoutMethods.IS_SET_READ_TIMEOUT_SUSPPORTED;
    }
}
