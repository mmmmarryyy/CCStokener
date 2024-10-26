package org.apache.myfaces.custom.skin.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.renderkit.html.util.ResourceLoader;
import org.apache.myfaces.trinidad.util.URLUtils;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.XhtmlConstants;

/**
 * This class handle resource created on temp directory.
 * 
 * @author Leonardo Uribe
 *
 */
public class SkinResourceLoader implements ResourceLoader {

    private static final int _BUFFER_SIZE = 2048;

    /**
     * Context parameter for activating debug mode, which will disable
     * caching.
     */
    public static final String DEBUG_INIT_PARAM = "org.apache.myfaces.trinidad.resource.DEBUG";

    private Boolean _debug = null;

    public static final long ONE_YEAR_MILLIS = 31363200000L;

    public void serveResource(ServletContext context, HttpServletRequest request, HttpServletResponse response, String resourceUri) throws IOException {
        _initDebug(context);
        File tempdir = (File) context.getAttribute("javax.servlet.context.tempdir");
        URL url = findResource(tempdir, XhtmlConstants.STYLES_CACHE_DIRECTORY + resourceUri);
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(false);
        _setHeaders(context, connection, response);
        InputStream in = connection.getInputStream();
        OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[_BUFFER_SIZE];
        try {
            _pipeBytes(in, out, buffer);
        } finally {
            try {
                in.close();
            } finally {
                out.close();
            }
        }
    }

    protected URL findResource(File directory, String path) throws IOException {
        if (path.charAt(0) == '/') path = path.substring(1);
        File file = new File(directory, path).getCanonicalFile();
        boolean isContained = file.getCanonicalPath().startsWith(directory.getCanonicalPath());
        return (isContained && file.exists()) ? file.toURI().toURL() : null;
    }

    /**
     * Initialize whether resource debug mode is enabled.
     */
    private void _initDebug(ServletContext context) {
        if (_debug == null) {
            String debug = context.getInitParameter(DEBUG_INIT_PARAM);
            _debug = "true".equalsIgnoreCase(debug);
        }
    }

    /**
     * Reads the specified input stream into the provided byte array storage and
     * writes it to the output stream.
     */
    private static void _pipeBytes(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int length;
        while ((length = (in.read(buffer))) >= 0) {
            out.write(buffer, 0, length);
        }
    }

    /**
     * Sets HTTP headers on the response which tell
     * the browser to cache the resource indefinitely.
     */
    private void _setHeaders(ServletContext context, URLConnection connection, HttpServletResponse response) {
        String contentType = connection.getContentType();
        if (contentType == null || "content/unknown".equals(contentType)) {
            URL url = connection.getURL();
            String resourcePath = url.getPath();
            if (resourcePath.endsWith(".css")) contentType = "text/css"; else if (resourcePath.endsWith(".js")) contentType = "application/x-javascript"; else contentType = context.getMimeType(resourcePath);
        }
        response.setContentType(contentType);
        int contentLength = connection.getContentLength();
        if (contentLength >= 0) response.setContentLength(contentLength);
        long lastModified;
        try {
            lastModified = URLUtils.getLastModified(connection);
        } catch (IOException exception) {
            lastModified = -1;
        }
        if (lastModified >= 0) response.setDateHeader("Last-Modified", lastModified);
        if (!_debug) {
            response.setHeader("Cache-Control", "Public");
            long currentTime = System.currentTimeMillis();
            response.setDateHeader("Expires", currentTime + ONE_YEAR_MILLIS);
        }
    }
}
