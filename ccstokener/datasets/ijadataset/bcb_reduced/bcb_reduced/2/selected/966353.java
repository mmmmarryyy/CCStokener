package socksviahttp.client.net;

import java.io.*;
import java.net.*;
import java.util.*;
import socksviahttp.core.util.*;

/**
 * A class to simplify HTTP applet-server communication.  It abstracts
 * the communication into messages, which can be either GET or POST.
 * <p>
 * It can be used like this:
 * <blockquote><pre>
 * URL url = new URL(getCodeBase(), "/servlet/ServletName");
 * &nbsp;
 * HttpMessage msg = new HttpMessage(url);
 * &nbsp;
 * // Parameters may optionally be set using java.util.Properties
 * Properties props = new Properties();
 * props.put("name", "value");
 * &nbsp;
 * // Headers, cookies, and authorization may be set as well
 * msg.setHeader("Accept", "image/png");             // optional
 * msg.setCookie("JSESSIONID", "9585155923883872");  // optional
 * msg.setAuthorization("guest", "try2gueSS");       // optional
 * &nbsp;
 * InputStream in = msg.sendGetMessage(props);
 * </pre></blockquote>
 * <p>
 */
public class HttpMessage {

    URL servlet = null;

    Hashtable headers = null;

    /**
   * Constructs a new HttpMessage that can be used to communicate with the
   * servlet at the specified URL.
   *
   * @param servlet the server resource (typically a servlet) with which
   * to communicate
   */
    public HttpMessage(URL servlet) {
        this.servlet = servlet;
    }

    /**
   * Performs a GET request to the servlet, with no query string.
   *
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
    public InputStream sendGetMessage() throws IOException {
        return sendGetMessage(null);
    }

    /**
   * Performs a GET request to the servlet, building
   * a query string from the supplied properties list.
   *
   * @param args the properties list from which to build a query string
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
    public InputStream sendGetMessage(Properties args) throws IOException {
        String argString = "";
        if (args != null) {
            argString = "?" + toEncodedString(args);
        }
        URL url = new URL(servlet.toExternalForm() + argString);
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        sendHeaders(con);
        return con.getInputStream();
    }

    /**
   * Performs a POST request to the servlet, with no query string.
   *
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
    public InputStream sendPostMessage() throws IOException {
        return sendPostMessage(null);
    }

    /**
   * Performs a POST request to the servlet, building
   * post data from the supplied properties list.
   *
   * @param args the properties list from which to build the post data
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
    public InputStream sendPostMessage(Properties args) throws IOException {
        String argString = "";
        if (args != null) {
            argString = toEncodedString(args);
        }
        URLConnection con = servlet.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        sendHeaders(con);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(argString);
        out.flush();
        out.close();
        return con.getInputStream();
    }

    /**
   * Performs a POST request to the servlet, uploading a serialized object.
   * <p>
   * The servlet can receive the object in its <tt>doPost()</tt> method
   * like this:
   * <pre>
   *     ObjectInputStream objin =
   *       new ObjectInputStream(req.getInputStream());
   *     Object obj = objin.readObject();
   * </pre>
   * The type of the uploaded object can be determined through introspection.
   *
   * @param obj the serializable object to upload
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
    public InputStream sendPostMessage(Serializable obj) throws IOException {
        URLConnection con = servlet.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
        sendHeaders(con);
        ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());
        out.writeObject(obj);
        out.flush();
        out.close();
        return con.getInputStream();
    }

    public InputStream sendGZippedPostMessage(Serializable obj) throws IOException {
        URLConnection con = servlet.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
        sendHeaders(con);
        java.util.zip.GZIPOutputStream zos = new java.util.zip.GZIPOutputStream(con.getOutputStream());
        ObjectOutputStream out = new ObjectOutputStream(zos);
        out.writeObject(obj);
        out.flush();
        out.close();
        return con.getInputStream();
    }

    public InputStream sendByteArrayInPostMessage(byte[] array) throws IOException {
        return (sendByteArrayInPostMessage(array, 0, array.length));
    }

    public InputStream sendByteArrayInPostMessage(byte[] array, int off, int len) throws IOException {
        URLConnection con = servlet.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/octet-stream");
        con.setRequestProperty("Content-Length", Integer.toString(len));
        sendHeaders(con);
        OutputStream out = con.getOutputStream();
        out.write(array, off, len);
        out.flush();
        out.close();
        return con.getInputStream();
    }

    /**
   * Sets a request header with the given name and value.  The header
   * persists across multiple requests.  The caller is responsible for
   * ensuring there are no illegal characters in the name and value.
   *
   * @param name the header name
   * @param value the header value
   */
    public void setHeader(String name, String value) {
        if (headers == null) {
            headers = new Hashtable();
        }
        headers.put(name, value);
    }

    private void sendHeaders(URLConnection con) {
        if (headers != null) {
            Enumeration enum1 = headers.keys();
            while (enum1.hasMoreElements()) {
                String name = (String) enum1.nextElement();
                String value = (String) headers.get(name);
                con.setRequestProperty(name, value);
            }
        }
    }

    /**
   * Sets a request cookie with the given name and value.  The cookie
   * persists across multiple requests.  The caller is responsible for
   * ensuring there are no illegal characters in the name and value.
   *
   * @param name the header name
   * @param value the header value
   */
    public void setCookie(String name, String value) {
        if (headers == null) {
            headers = new Hashtable();
        }
        String existingCookies = (String) headers.get("Cookie");
        if (existingCookies == null) {
            setHeader("Cookie", name + "=" + value);
        } else {
            setHeader("Cookie", existingCookies + "; " + name + "=" + value);
        }
    }

    /**
   * Sets the authorization information for the request (using BASIC
   * authentication via the HTTP Authorization header).  The authorization
   * persists across multiple requests.
   *
   * @param name the user name
   * @param name the user password
   */
    public void setAuthorization(String name, String password) {
        String authorization = Base64Encoder.encode(name + ":" + password);
        setHeader("Authorization", "Basic " + authorization);
    }

    public void setProxyAuthorization(String name, String password) {
        String authorization = Base64Encoder.encode(name + ":" + password);
        setHeader("Proxy-Authorization", "Basic " + authorization);
    }

    private String toEncodedString(Properties args) {
        StringBuffer buf = new StringBuffer();
        Enumeration names = args.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = args.getProperty(name);
            buf.append(URLEncoder.encode(name) + "=" + URLEncoder.encode(value));
            if (names.hasMoreElements()) buf.append("&");
        }
        return buf.toString();
    }
}
