package gov.llnl.text.util.html;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import gov.llnl.text.util.FileUtils;

/**
 * Description: Retrieve files via HTTP Also, convert HTML files into DOM trees
 * Copyright (c) 2001
 * 
 * @author David Buttler
 * @created July 19, 2001
 * @updated August 30, 2001
 * @version 1.0
 */
public class HTTPUtils {

    /**
 * retrieve the contents of a url
 * 
 * @param url
 *           Description of Parameter
 * @return Description of the Returned Value
 * @exception MalformedURLException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static String get(String url) throws MalformedURLException, IOException {
        StringBuffer buf = new StringBuffer();
        get(url, buf);
        return buf.toString();
    }

    /**
 * Description of the Method
 * 
 * @param url
 *           Description of Parameter
 * @param buf
 *           Description of Parameter
 * @exception MalformedURLException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static void get(String url, StringBuffer buf) throws MalformedURLException, IOException {
        String filePrefix = "file://";
        if (url.toLowerCase().startsWith(filePrefix)) {
            String filename = url.substring(filePrefix.length());
            FileUtils.readFile(filename, buf);
        } else {
            get(new URL(url), buf);
        }
    }

    /**
 * retrieve the contents of a url
 * 
 * @param url
 *           Description of Parameter
 * @return Description of the Returned Value
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static String get(URL url) throws FileNotFoundException, IOException {
        StringBuffer buf = new StringBuffer();
        get(url, buf);
        return buf.toString();
    }

    /**
 * retrieve the contents of a url
 * 
 * @param url
 *           Description of Parameter
 * @param buf
 *           Description of Parameter
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static void get(URL url, StringBuffer buf) throws FileNotFoundException, IOException {
        InputStreamReader dis = null;
        BufferedReader in = null;
        try {
            dis = new InputStreamReader(getStream(url));
            in = new BufferedReader(dis);
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                buf.append(line);
                buf.append('\n');
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (dis != null) {
                dis.close();
            }
        }
    }

    /**
 * retrieve the contents of a url directly to a file
 * 
 * @param urlStr
 *           Description of Parameter
 * @param outWriter
 *           Description of Parameter
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static void get(String urlStr, Writer outWriter) throws FileNotFoundException, IOException {
        URL url = new URL(urlStr);
        get(url, outWriter);
    }

    /**
 * Description of the Method
 * 
 * @param url
 *           Description of Parameter
 * @param outWriter
 *           Description of Parameter
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static void get(URL url, Writer outWriter) throws FileNotFoundException, IOException {
        PrintWriter out = null;
        if (outWriter instanceof PrintWriter) {
            out = (PrintWriter) outWriter;
        } else {
            out = new PrintWriter(outWriter, true);
        }
        InputStreamReader dis = null;
        BufferedReader in = null;
        try {
            dis = new InputStreamReader(getStream(url));
            in = new BufferedReader(dis);
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                out.println(line);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (dis != null) {
                dis.close();
            }
        }
    }

    /**
 * retrieve the contents of a url
 * 
 * @param url
 *           Description of Parameter
 * @return The stream value
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static InputStream getStream(URL url) throws FileNotFoundException, IOException {
        InputStream result = null;
        try {
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("User-Agent", "HTTP Client");
            uc.setRequestProperty("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, image/png, */*");
            uc.setRequestProperty("Accept-Encoding", "gzip");
            uc.setRequestProperty("Accept-Language", "en");
            uc.setRequestProperty("Accept-Charset", "iso-8859-1,*,utf-8");
            uc.setDoOutput(true);
            uc.setDoInput(true);
            uc.setUseCaches(false);
            uc.setAllowUserInteraction(true);
            result = uc.getInputStream();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        return result;
    }

    /**
 * retrieve the contents of a url
 * 
 * @param url
 *           Description of Parameter
 * @return The stream value
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static Reader getReader(URL url) throws FileNotFoundException, IOException {
        Reader result = null;
        try {
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("User-Agent", "HTTP Client");
            uc.setRequestProperty("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, image/png, */*");
            uc.setRequestProperty("Accept-Encoding", "gzip");
            uc.setRequestProperty("Accept-Language", "en");
            uc.setRequestProperty("Accept-Charset", "iso-8859-1,*,utf-8");
            uc.setDoOutput(true);
            uc.setDoInput(true);
            uc.setUseCaches(false);
            uc.setAllowUserInteraction(true);
            result = new InputStreamReader(uc.getInputStream());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        return result;
    }

    /**
 * retrieve the contents of a url form The form fields must be put in the
 * content string as this method only writes the content string to the specified
 * url
 * 
 * @param urlStr
 *           Description of Parameter
 * @param content
 *           Description of Parameter
 * @return Description of the Returned Value
 * @exception MalformedURLException
 *               Description of Exception
 */
    public static String post(String urlStr, String content) throws MalformedURLException {
        StringBuffer buf = new StringBuffer();
        post(urlStr, content, buf);
        return buf.toString();
    }

    /**
 * Description of the Method
 * 
 * @param urlStr
 *           Description of Parameter
 * @param content
 *           Description of Parameter
 * @param resultBuf
 *           Description of Parameter
 * @exception MalformedURLException
 *               Description of Exception
 */
    public static void post(String urlStr, String content, StringBuffer resultBuf) throws MalformedURLException {
        String urlString = urlStr.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        StringBuffer buf = new StringBuffer(urlString);
        for (int i = 0; i < buf.length(); i++) {
            if (buf.charAt(i) < '\n') {
                buf.setCharAt(i, ' ');
            }
        }
        urlString = buf.toString().trim();
        try {
            URL u = new URL(urlString);
            post(u, content, resultBuf);
        } catch (MalformedURLException e) {
            throw new MalformedURLException("[HTTPClient]Error in retrieving post results: " + e);
        } catch (Exception e) {
            throw new RuntimeException("[HTTPClient]Error in retrieving post results: " + e);
        }
    }

    /**
 * retrieve the contents of a url form The form fields must be put in the
 * content string as this method only writes the content string to the specified
 * url
 * 
 * @param url
 *           Description of Parameter
 * @param content
 *           Description of Parameter
 * @return Description of the Returned Value
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static String post(URL url, String content) throws FileNotFoundException, IOException {
        StringBuffer result = new StringBuffer();
        post(url, content, result);
        return result.toString();
    }

    /**
 * Description of the Method
 * 
 * @param url
 *           url to post to
 * @param content
 *           post content
 * @param result
 *           String buffer to append results to
 * @exception FileNotFoundException
 *               if URL not found
 * @exception IOException
 *               other unexpected error from post method
 */
    public static void post(URL url, String content, StringBuffer result) throws FileNotFoundException, IOException {
        InputStreamReader dis = null;
        BufferedReader in = null;
        try {
            dis = new InputStreamReader(postStream(url, content));
            in = new BufferedReader(dis);
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                result.append(line);
                result.append('\n');
            }
            in.close();
            dis.close();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (dis != null) {
                dis.close();
            }
        }
    }

    /**
 * retrieve the contents of a url form The form fields must be put in the
 * content string as this method only writes the content string to the specified
 * url
 * 
 * @param url
 *           Description of Parameter
 * @param content
 *           Description of Parameter
 * @return Description of the Returned Value
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static InputStream postStream(URL url, String content) throws FileNotFoundException, IOException {
        content = content.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        content = content.trim();
        StringBuffer buf = new StringBuffer(content);
        for (int i = 0; i < buf.length(); i++) {
            if (buf.charAt(i) < '\n') {
                buf.setCharAt(i, ' ');
            }
        }
        content = buf.toString().trim();
        try {
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("USER_AGENT", "HTTP Client");
            uc.setDoOutput(true);
            uc.setDoInput(true);
            uc.setUseCaches(false);
            uc.setAllowUserInteraction(true);
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream dos = new DataOutputStream(uc.getOutputStream());
            dos.writeBytes(content);
            dos.close();
            return uc.getInputStream();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
 * retrieve the contents of a url form The form fields must be put in the
 * content string as this method only writes the content string to the specified
 * url
 * 
 * @param url
 *           Description of Parameter
 * @param content
 *           Description of Parameter
 * @return Description of the Returned Value
 * @exception FileNotFoundException
 *               Description of Exception
 * @exception IOException
 *               Description of Exception
 */
    public static Reader postReader(URL url, String content) throws FileNotFoundException, IOException {
        content = content.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        content = content.trim();
        StringBuffer buf = new StringBuffer(content);
        for (int i = 0; i < buf.length(); i++) {
            if (buf.charAt(i) < '\n') {
                buf.setCharAt(i, ' ');
            }
        }
        content = buf.toString().trim();
        try {
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("USER_AGENT", "HTTP Client");
            uc.setDoOutput(true);
            uc.setDoInput(true);
            uc.setUseCaches(false);
            uc.setAllowUserInteraction(true);
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream dos = new DataOutputStream(uc.getOutputStream());
            dos.writeBytes(content);
            dos.close();
            return new InputStreamReader(uc.getInputStream());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
 * Get an attribput given a node
 * 
 * @param n
 *           org.w3c.dom.Node
 * @param attr
 * @return java.lang.String
 */
    public static String getAttribute(Node n, String attr) {
        NamedNodeMap nnm = n.getAttributes();
        Node attribute = nnm.getNamedItem(attr);
        return attribute.getNodeValue().trim();
    }

    /**
 * put an attributes value into a buffer, given its name and the node to extract
 * it from
 * 
 * @param n
 *           org.w3c.dom.Node
 * @param attr
 * @param buf
 */
    public static void getAttribute(Node n, String attr, StringBuffer buf) {
        NamedNodeMap nnm = n.getAttributes();
        Node attribute = nnm.getNamedItem(attr);
        buf.append(attribute.getNodeValue().trim());
    }

    /**
 * Return the base ref of this page. Creation date: (12/13/99 1:58:01 PM)
 * 
 * @param page
 * @return URL for the page given in the header portion
 * @throws MalformedURLException
 */
    public static URL getBase(Node page) throws MalformedURLException {
        String name = page.getNodeName();
        if (name != null) {
            name = name.trim().toLowerCase();
        }
        if ("base".equals(name)) {
            String href = getAttribute(page, "href");
            if (href != null) {
                href = href.trim();
                if (href.length() > 0) {
                    URL base = new URL(href);
                    return base;
                }
            }
        }
        NodeList nl = page.getChildNodes();
        for (int i = 0; nl != null && i < nl.getLength(); i++) {
            Node child = nl.item(i);
            URL base = getBase(child);
            if (base != null) {
                return base;
            }
        }
        return null;
    }

    /**
 * Return the base url for a given page, defaulting to a given URL
 * 
 * @param page
 * @param url
 *           java.lang.String
 * @return String
 * @throws MalformedURLException
 */
    public static String getBase(Node page, URL url) throws MalformedURLException {
        String defaultURL = url.toExternalForm();
        int index = defaultURL.lastIndexOf('/');
        if (index > 0) {
            defaultURL = defaultURL.substring(0, index + 1);
        }
        URL base = getBase(page);
        if (base != null) {
            defaultURL = base.toExternalForm();
        }
        return defaultURL;
    }

    /**
 * Insert the method's description here. Creation date: (2/1/01 11:56:54 AM)
 * Author: David Buttler Revisions: <date> <description>
 * 
 * @return java.lang.String
 * @param n
 *           org.w3c.dom.Node
 */
    public static String getContent(Node n) {
        StringBuffer buf = new StringBuffer();
        getContent(n, buf);
        return buf.toString();
    }

    /**
 * Get the content of a given node, including all descendants. Ignore comments,
 * and attributes
 * 
 * @param n
 *           org.w3c.dom.Node
 * @param buf
 */
    public static void getContent(Node n, StringBuffer buf) {
        if (n.getNodeType() == Node.TEXT_NODE) {
            buf.append(n.getNodeValue());
            buf.append(' ');
        } else if (n.getNodeType() == Node.COMMENT_NODE) {
            return;
        } else if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
            return;
        }
        NodeList nl = n.getChildNodes();
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node c = nl.item(i);
                getContent(c, buf);
            }
        }
    }

    /**
 * Locate the title of the HTML document that this node is a part of Creation
 * date: (2/1/01 12:05:04 PM) Author: David Buttler Revisions: <date>
 * <description>
 * 
 * @return java.lang.String
 * @param n
 *           org.w3c.dom.Node
 */
    public static String getTitle(Node n) {
        Node p = n;
        Node a = n.getParentNode();
        while (p != null && a != null && a != p) {
            p = a;
            a = p.getParentNode();
        }
        String title = titleSearch(p);
        return title;
    }

    /**
 * Starting at given node, check descendants for a title node Creation date:
 * (2/1/01 12:10:04 PM) Author: David Buttler Revisions: <date> <description>
 * 
 * @return java.lang.String
 * @param n
 *           org.w3c.dom.Node
 */
    public static String titleSearch(Node n) {
        String name = n.getNodeName();
        if (name != null) {
            name = name.trim().toLowerCase();
            if (name.equals("title")) {
                String t = getContent(n);
                return t;
            }
        }
        NodeList nl = n.getChildNodes();
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node c = nl.item(i);
                String result = titleSearch(c);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
 * @param page
 * @return content of the given HTML page (minus the tags and comments)
 */
    public static String extractText(String page) {
        return HTMLContentCallbackHandler.getContent(page);
    }
}
