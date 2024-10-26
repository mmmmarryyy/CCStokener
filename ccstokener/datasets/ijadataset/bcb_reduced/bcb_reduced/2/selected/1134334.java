package fi.hip.gb.disk.transport.webdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Vector;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.webdav.lib.WebdavResource;
import fi.hip.gb.disk.transport.Transport;

/**
 * Transport implementation of WebDAV protocol with support for basic 
 * HTTP communication. First the operation is tested with
 * WebDAV methods, if they fail we fallback to basic http put/get methods.
 * <p>
 * The WebDAV works with any WebDAV server with no specific requirements.
 * <p>
 * The HTTP part is fully functional only when communicating with 
 * {@link fi.hip.gb.disk.conf.InitService} service.
 * Supported methods for all servers are http GET 
 * ({@link fi.hip.gb.disk.transport.Transport#get(String, File)} method)
 * and http PUT with multipart request ({@link fi.hip.gb.disk.transport.Transport#put(String, File)} method).
 * Queries of existence ({@link fi.hip.gb.disk.transport.Transport#exists(String)}) and
 * deleting ({@link fi.hip.gb.disk.transport.Transport#delete(String)}) are not
 * functional unless {@link fi.hip.gb.disk.conf.InitService} service is used. 
 * <p>
 * Jakarta slide is used for WebDAV and Commons http client for http and multipart messages. 
 * <p>
 * This implementation has all the same functionalities as 
 * <code>fi.hip.gb.disk.transport.http.HttpTransport</code> class (now deprecated).
 * 
 * @author Juho Karppinen
 */
public class WebDavTransport implements Transport {

    /** The WebDAV resource. */
    private WebdavResource webdavResource = null;

    /** The http URL on the client connection. */
    private HttpURL httpURL;

    private Log log = LogFactory.getLog(WebDavTransport.class);

    /**
     * Connects to the WebDAV/http resource.
     * 
     * @param endpointURL
     *            Endpoint for client connection
     */
    public WebDavTransport(String endpointURL) {
        try {
            this.httpURL = new HttpURL(endpointURL);
            if (webdavResource == null) {
                webdavResource = new WebdavResource(httpURL);
                webdavResource.setDebug(0);
            } else {
                webdavResource.close();
                webdavResource.setHttpURL(httpURL);
            }
            log.debug("Connected to WebDAV server " + this.httpURL);
            String pathInfo = this.httpURL.getPath();
            if (pathInfo.endsWith("/") == false) pathInfo += "/";
            webdavResource.setPath(pathInfo);
        } catch (HttpException he) {
            log.error("HttpException.getReasonCode(): " + he.getReasonCode());
            if (he.getReasonCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                log.error("Error: Not WebDAV-enabled? " + he.getMessage());
            } else if (he.getReasonCode() == HttpStatus.SC_UNAUTHORIZED) {
                log.error("Error: Unauthorized " + he.getMessage());
            } else {
                log.error("Error: " + he.getMessage());
            }
            httpURL = null;
            webdavResource = null;
        } catch (IOException ioe) {
            log.error("Error: " + ioe.getMessage());
            httpURL = null;
            webdavResource = null;
        }
    }

    public void put(String path, File file) throws IOException {
        String status = "";
        log.info("file " + file.getName() + " into " + this.webdavResource.getPath() + new String(path.getBytes("UTF-8"), "UTF-8"));
        if (file.isDirectory()) {
            if (exists(path) == 0 && webdavResource.mkcolMethod(this.webdavResource.getPath() + path) == false) {
                log.error("failed to create directory " + path);
            }
            if (path.endsWith("/") == false) path += "/";
            for (File f : file.listFiles()) {
                put(file.getName() + "/" + f.getName(), f);
            }
            return;
        } else if (webdavResource.putMethod(this.webdavResource.getPath() + path, file)) {
            log.info(file.getPath() + " uploaded to WebDAV " + this.httpURL.toString() + path);
            return;
        } else if (webdavResource.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
            log.info("WebDAV not supported on " + this.httpURL.toString() + ", using http instead");
            HttpClient client = new HttpClient();
            MultipartPostMethod mPost = new MultipartPostMethod(this.httpURL.toString());
            client.setConnectionTimeout(8000);
            mPost.addParameter(path, file);
            client.executeMethod(mPost);
            mPost.releaseConnection();
            if (mPost.getStatusCode() != HttpStatus.SC_OK) {
                status = mPost.getStatusLine().toString();
            } else {
                log.info(file.getPath() + " uploaded to HTTP servlet " + this.httpURL.toString() + " : " + mPost.getStatusLine());
            }
        } else {
            status = webdavResource.getStatusMessage();
        }
        throw new IOException("Uploading " + file.getPath() + " to " + this.httpURL.toString() + " failed: " + status);
    }

    /**
     * Lists or downloads all files recursively. 
     * @param path path to travel
     * @param dir local directory for files, if null only list files without downloading
     * @throws IOException
     */
    private void mget(String path, File dir) throws IOException {
        String originalPath = this.httpURL.getPath();
        try {
            if (httpURL.getPath().endsWith(path) == false) {
                webdavResource.setPath(this.httpURL.getPath() + path);
            }
            System.out.println(decode(this.webdavResource.getPath()));
            if (dir != null) {
                dir = new File(dir.getParentFile(), decode(dir.getName()));
                dir.mkdirs();
            }
            WebdavResource[] files = webdavResource.listWebdavResources();
            if (files != null) {
                for (WebdavResource file : files) {
                    File target = new File(dir, file.getName());
                    if (file.getName().equals(dir.getName())) {
                    } else if (file.isCollection()) {
                        mget(file.getName() + "/", target);
                    } else {
                        System.out.println(decode(this.webdavResource.getPath()) + file.getName());
                        webdavResource.getMethod(this.webdavResource.getPath() + "/" + file.getName(), target);
                    }
                }
            }
        } catch (IOException ioe) {
            log.error("Failed to process directory " + path);
            throw ioe;
        } finally {
            webdavResource.setPath(originalPath);
        }
    }

    public void get(String path, File file) throws IOException {
        if (existsDir(path) == -1) {
            if (path.endsWith("/") == false) path += "/";
            mget(path, file);
        } else if (webdavResource.getMethod(this.webdavResource.getPath() + path, file)) {
            log.info(path + " downloaded from " + this.httpURL.toString());
        } else {
            throw new IOException("Downloading to " + file.getPath() + " from " + this.httpURL.toString() + " failed : " + webdavResource.getStatusMessage());
        }
    }

    /**
     * Additional Transport method for listing directory.
     * @return list of files in the remove folder, folders end with slash
     */
    public String[] list() throws IOException {
        Vector<String> list = new Vector<String>();
        WebdavResource[] files = webdavResource.listWebdavResources();
        if (files != null) {
            for (WebdavResource file : files) {
                String name = encode(file.getName());
                if (name.equals(webdavResource.getName())) {
                    continue;
                } else if (file.isCollection()) {
                    name += "/";
                }
                list.add(name);
            }
        } else {
            throw new IOException("Listing of " + this.httpURL.toString() + " failed: " + webdavResource.getStatusMessage());
        }
        return list.toArray(new String[0]);
    }

    public void delete(String path) throws IOException {
        if (webdavResource.deleteMethod(webdavResource.getPath() + "/" + path)) {
            log.info(path + " deleted from WebDAV " + this.httpURL.toString());
        } else if (webdavResource.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
            URL url = new URL(this.httpURL.toString() + path + "?delete");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            if (connection.getResponseCode() == 200) {
                log.info(path + " deleted from HTTP servlet " + this.httpURL.toString());
                log.debug(getResponse(connection.getInputStream()));
            } else {
                throw new IOException(path + " could not be deleted from HTTP servlet " + this.httpURL.toString() + " returned " + getResponse(connection.getInputStream()));
            }
        } else {
            throw new IOException("Deleting of " + path + " from " + this.httpURL.toString() + " failed: " + webdavResource.getStatusMessage());
        }
    }

    public int exists(String path) throws IOException {
        return Math.abs(existsDir(path));
    }

    /**
     * Gets the existence status and type of item.
     * @param path path to discover
     * @return -1 if directory, 0 if not found, 1 if found as file
     * @throws IOException
     */
    private int existsDir(String path) throws IOException {
        WebdavResource[] files = webdavResource.listWebdavResources();
        if (files != null) {
            for (WebdavResource file : files) {
                String name = encode(file.getName());
                if (name.equals(path)) {
                    log.info(path + " exists on WebDAV " + httpURL.toString() + " as " + (file.isCollection() ? " directory" : " file"));
                    return (file.isCollection() ? -1 : 1);
                } else {
                }
            }
        } else if (webdavResource.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
            URL url = new URL(httpURL.toString() + path + "?exists");
            log.debug("WebDAV check failed from " + httpURL.toString() + ", " + webdavResource.getStatusMessage() + ", using HTTP Servlet instead");
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            InputStream in = null;
            try {
                in = connection.getInputStream();
                if (in.read() == 1) {
                    log.info(path + " exists on HTTP " + httpURL.toString());
                    return 1;
                }
            } finally {
                if (in != null) in.close();
            }
        } else {
            throw new IOException("Exists check of " + path + " from WebDAV " + httpURL.toString() + " failed: " + webdavResource.getStatusMessage());
        }
        return 0;
    }

    /**
     * Gets response from the operation
     * @param connection
     * @return
     * @throws IOException 
     */
    private String getResponse(InputStream in) throws IOException {
        if (in == null) return null;
        StringBuffer result = new StringBuffer();
        try {
            byte[] line = new byte[16384];
            int bytes = -1;
            while ((bytes = in.read(line)) != -1) {
                result.append(new String(line, 0, bytes));
            }
        } finally {
            if (in != null) in.close();
        }
        return result.toString();
    }

    /**
     * Encode the name for UTF-8 used by WebDAV.
     * @param name filename with unsafe characters
     * @return UTF-8 encoded file name
     */
    private String encode(String name) {
        try {
            name = URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode uri " + name);
        }
        name = name.replaceAll("\\+", "%20");
        return name;
    }

    /**
     * Decode the name from UTF-8 used by WebDAV.
     * @param name UTF-8 coded filename
     * @return system filename
     */
    private String decode(String name) {
        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to decode uri " + name);
        }
        return name;
    }
}
