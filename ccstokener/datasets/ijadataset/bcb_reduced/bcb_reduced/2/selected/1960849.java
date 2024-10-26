package org.mayo.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;

/**
 * Checks the request for real files and intercepts the controller
 * where needed.
 * @author Chris Corbyn <chris@w3style.co.uk>
 */
public class FileFilter implements Filter {

    /** Filter configuration */
    protected FilterConfig filterConfig;

    /**
   * Required init method to get config.
   * @param FilterConfig filterConfig
   */
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
   * Runs this filter.
   * @param ServletRequest request
   * @param ServletResponse response
   * @param FilterChain filterChain
   * @throws ServletException
   * @throws IOException
   */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = getRelativePath(httpRequest);
        if (isFile(path)) {
            serveFile(httpRequest, httpResponse);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
   * Get the real requested path from the request.
   * @param HttpServletRequest request
   * @return String
   * @throws IOException
   * @throws ServletException
   */
    public String getRelativePath(HttpServletRequest request) throws IOException, ServletException {
        String ctxPath = request.getContextPath();
        String path = request.getRequestURI().substring(ctxPath.length());
        return path;
    }

    /**
   * Checks if the servlet context is able to get a resource at this path.
   * @param String path
   * @return boolean
   * @throws IOException
   */
    private boolean isFile(String path) throws IOException {
        if (path.replaceAll("/", "").equals("")) {
            return false;
        }
        URL url = null;
        ServletContext context = filterConfig.getServletContext();
        try {
            url = context.getResource(path);
        } catch (MalformedURLException e) {
            return false;
        }
        if (url == null) {
            return false;
        }
        return true;
    }

    /**
   * Serves the file if possible.
   * If an error occurs, either a IOException will be raised, or nothing will
   * be streamed depending upon the cause of the error.
   * @param HttpServletRequest request
   * @param HttpServletResponse response
   * @throws IOException
   * @throws ServletException
   */
    private void serveFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = getRelativePath(request);
        if (path.matches(".*\\.(?i)(?:jsp|jstl|jspx)$")) {
            ServletContext context = filterConfig.getServletContext();
            RequestDispatcher disp = context.getRequestDispatcher(path);
            disp.forward(request, response);
            return;
        }
        URL url = null;
        File file;
        ServletContext context = filterConfig.getServletContext();
        try {
            url = context.getResource(path);
            String realpath = context.getRealPath(path);
            if (realpath == null) {
                return;
            }
            file = new File(realpath);
            if (!file.canRead()) {
                return;
            }
        } catch (MalformedURLException e) {
            return;
        }
        if (url == null) {
            return;
        }
        Date lastModified = new Date(file.lastModified());
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String lastModifiedStr = df.format(lastModified);
        String eTag = "\"" + lastModifiedStr.replaceAll(" ", "") + "\"";
        response.setHeader("Last-Modified", lastModifiedStr);
        response.setHeader("ETag", eTag);
        String requestedIfModified = request.getHeader("If-Modified-Since");
        if ((requestedIfModified != null) && requestedIfModified.equals(lastModifiedStr)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        String requestedNoneMatch = request.getHeader("If-None-Match");
        if ((requestedNoneMatch != null) && requestedNoneMatch.equals(eTag)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        String mimeType = context.getMimeType(path);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        ServletOutputStream os = null;
        InputStream is = null;
        try {
            os = response.getOutputStream();
            is = url.openStream();
            response.setContentType(mimeType);
            response.setContentLength(is.available());
            byte b[] = new byte[8192];
            int c;
            while ((c = is.read(b)) > -1) {
                os.write(b);
                os.flush();
            }
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    /**
   * Required destroy() method.
   */
    public void destroy() {
        filterConfig = null;
    }
}
