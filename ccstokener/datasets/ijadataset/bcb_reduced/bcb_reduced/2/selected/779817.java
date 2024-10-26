package com.jspx.upload;

import java.io.*;
import java.net.*;
import javax.servlet.*;

/** 
 * A collection of static utility methods useful to servlets.
 * Some methods require Servlet API 2.2.
 *
 * @author <b>Jason Hunter</b>, Copyright &#169; 1998-2000
 * @version 1.5, 2001/02/11, added getResource() ".." check
 * @version 1.4, 2000/09/27, finalized getResource() behavior
 * @version 1.3, 2000/08/15, improved getStackTraceAsString() to take Throwable
 * @version 1.2, 2000/03/10, added getResource() method
 * @version 1.1, 2000/02/13, added returnURL() methods
 * @version 1.0, 1098/09/18
 */
public class ServletUtils {

    /**
   * Sends the contents of the specified file to the output stream
   *
   * @param filename the file to send
   * @param out the output stream to write the file
   * @exception FileNotFoundException if the file does not exist
   * @exception IOException if an I/O error occurs
   */
    public static void returnFile(String filename, OutputStream out) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            byte[] buf = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = fis.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }
        } finally {
            if (fis != null) fis.close();
        }
    }

    /**
     * Sends the contents of the specified URL to the output stream
     * @param url   URL
     * @param out   OutputStream
     * @throws IOException  IOException
     */
    public static void returnURL(URL url, OutputStream out) throws IOException {
        InputStream in = url.openStream();
        byte[] buf = new byte[4 * 1024];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) {
            out.write(buf, 0, bytesRead);
        }
    }

    /**
   * Sends the contents of the specified URL to the Writer (commonly either a
   * PrintWriter or JspWriter)
   *
   * @param url whose contents are to be sent
   * @param out the Writer to write the contents
   * @exception IOException if an I/O error occurs
   */
    public static void returnURL(URL url, Writer out) throws IOException {
        URLConnection con = url.openConnection();
        con.connect();
        String encoding = con.getContentEncoding();
        BufferedReader in;
        if (encoding == null) {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(url.openStream(), encoding));
        }
        char[] buf = new char[4 * 1024];
        int charsRead;
        while ((charsRead = in.read(buf)) != -1) {
            out.write(buf, 0, charsRead);
        }
    }

    /**
     *  Gets an exception's stack trace as a String
     * @param t  Throwable
     * @return   String
     */
    public static String getStackTraceAsString(Throwable t) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bytes, true);
        t.printStackTrace(writer);
        return bytes.toString();
    }

    /**
   * Gets a reference to the named upload, attempting to load it
   * through an HTTP request if necessary.  Returns null if there's a problem.
   * This method behaves similarly to <tt>ServletContext.getServlet()</tt>
   * except, while that method may return null if the 
   * named upload wasn't already loaded, this method tries to load
   * the upload using a dummy HTTP request.  Only loads HTTP servlets.
   *
   * @param name the name of the upload
   * @param req the upload request
   * @param context the upload context
   * @return the named upload, or null if there was a problem
   */
    @SuppressWarnings({ "ResultOfMethodCallIgnored", "deprecation" })
    public static Servlet getServlet(String name, ServletRequest req, ServletContext context) {
        try {
            Servlet servlet = context.getServlet(name);
            if (servlet != null) return servlet;
            Socket socket = new Socket(req.getServerName(), req.getServerPort());
            socket.setSoTimeout(4000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("GET /upload/" + name + " HTTP/1.0");
            out.println();
            try {
                socket.getInputStream().read();
            } catch (InterruptedIOException e) {
            }
            out.close();
            return context.getServlet(name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
   * Gets a reference to the given resource within the given context,
   * making sure not to serve the contents of WEB-INF, META-INF, or to
   * display .jsp file source.
   * Throws an IOException if the resource can't be read.
   *
   * @param context the context containing the resource
   * @param resource the resource to be read
   * @return a URL reference to the resource
   * @exception IOException if there's any problem accessing the resource
   */
    public static URL getResource(ServletContext context, String resource) throws IOException {
        if (resource == null) {
            throw new FileNotFoundException("Requested resource was null (passed in null)");
        }
        if (resource.endsWith("/") || resource.endsWith("\\") || resource.endsWith(".")) {
            throw new MalformedURLException("Path may not end with a slash or dot");
        }
        if (resource.indexOf("..") != -1) {
            throw new MalformedURLException("Path may not contain double dots");
        }
        String upperResource = resource.toUpperCase();
        if (upperResource.startsWith("/WEB-INF") || upperResource.startsWith("/META-INF")) {
            throw new MalformedURLException("Path may not begin with /WEB-INF or /META-INF");
        }
        if (upperResource.endsWith(".JSP")) {
            throw new MalformedURLException("Path may not end with .jsp");
        }
        URL url = context.getResource(resource);
        if (url == null) {
            throw new FileNotFoundException("Requested resource was null (" + resource + ")");
        }
        return url;
    }
}
