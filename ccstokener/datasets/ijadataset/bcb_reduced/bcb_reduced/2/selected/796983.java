package net.sf.sail.jetty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.util.IO;

/**
 * EXPERIMENTAL Proxy servlet.
 * @author gregw
 *
 */
public class WebstartProxyServlet implements Servlet {

    private static final int METHOD_UNKNOWN = -1;

    private static final int METHOD_GET = 0;

    private static final int METHOD_HEAD = 1;

    private static final int METHOD_POST = 2;

    private int _tunnelTimeoutMs = 300000;

    protected HashSet _DontProxyHeaders = new HashSet();

    {
        _DontProxyHeaders.add("proxy-connection");
        _DontProxyHeaders.add("connection");
        _DontProxyHeaders.add("keep-alive");
        _DontProxyHeaders.add("transfer-encoding");
        _DontProxyHeaders.add("te");
        _DontProxyHeaders.add("trailer");
        _DontProxyHeaders.add("proxy-authorization");
        _DontProxyHeaders.add("proxy-authenticate");
        _DontProxyHeaders.add("upgrade");
    }

    private ServletConfig config;

    private ServletContext context;

    public void init(ServletConfig config) throws ServletException {
        this.config = config;
        this.context = config.getServletContext();
        context.log("init: proxyHost: " + System.getProperty("http.proxyHost"));
    }

    public ServletConfig getServletConfig() {
        return config;
    }

    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if ("CONNECT".equalsIgnoreCase(request.getMethod())) {
            handleConnect(request, response);
        } else {
            String uri = request.getRequestURI();
            if (request.getQueryString() != null) {
                uri += "?" + request.getQueryString();
            }
            URL url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), uri);
            context.log("URL=" + url);
            context.log("  method: " + request.getMethod());
            if ("1.1 (jetty)".equals(request.getHeader("Via"))) {
                throw new FileNotFoundException("Can't proxy local url: " + url);
            }
            if (handleJnlpCacheFile(request, response)) {
                return;
            }
            URLConnection remoteConn = url.openConnection();
            remoteConn.setAllowUserInteraction(false);
            HttpURLConnection remoteHttpConn = null;
            if (remoteConn instanceof HttpURLConnection) {
                remoteHttpConn = (HttpURLConnection) remoteConn;
                remoteHttpConn.setRequestMethod(request.getMethod());
                remoteHttpConn.setInstanceFollowRedirects(false);
            }
            String reqHdrConnection = request.getHeader("Connection");
            if (reqHdrConnection != null) {
                reqHdrConnection = reqHdrConnection.toLowerCase();
                if (reqHdrConnection.equals("keep-alive") || reqHdrConnection.equals("close")) {
                    reqHdrConnection = null;
                }
            }
            boolean xForwardedFor = false;
            boolean hasContent = false;
            Enumeration reqHdrNames = request.getHeaderNames();
            while (reqHdrNames.hasMoreElements()) {
                String reqHdrName = (String) reqHdrNames.nextElement();
                String lReqHdrName = reqHdrName.toLowerCase();
                String reqHdrStrVal = request.getHeader(reqHdrName);
                context.log("   " + reqHdrName + ": " + reqHdrStrVal);
                if (_DontProxyHeaders.contains(lReqHdrName)) {
                    continue;
                }
                if (reqHdrConnection != null && reqHdrConnection.indexOf(lReqHdrName) >= 0) {
                    continue;
                }
                if ("x-forwarded-for".equals(lReqHdrName)) {
                    xForwardedFor = true;
                }
                if ("content-type".equals(lReqHdrName)) {
                    hasContent = true;
                }
                Enumeration reqHdrVals = request.getHeaders(reqHdrName);
                while (reqHdrVals.hasMoreElements()) {
                    String reqHdrVal = (String) reqHdrVals.nextElement();
                    if (reqHdrVal != null) {
                        remoteConn.addRequestProperty(reqHdrName, reqHdrVal);
                    }
                }
            }
            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                hasContent = false;
            }
            remoteConn.setRequestProperty("Via", "1.1 (jetty)");
            if (!xForwardedFor) {
                remoteConn.addRequestProperty("X-Forwarded-For", request.getRemoteAddr());
            }
            String cache_control = request.getHeader("Cache-Control");
            if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0)) remoteConn.setUseCaches(false);
            try {
                if (hasContent) {
                    remoteConn.setDoInput(true);
                    InputStream requestIn = request.getInputStream();
                    remoteConn.setDoOutput(true);
                    IO.copy(requestIn, remoteConn.getOutputStream());
                }
                remoteConn.connect();
            } catch (Exception e) {
                context.log("proxy", e);
            }
            InputStream remoteIn = null;
            int code = 500;
            if (remoteHttpConn != null) {
                remoteIn = remoteHttpConn.getErrorStream();
                code = remoteHttpConn.getResponseCode();
                response.setStatus(code, remoteHttpConn.getResponseMessage());
                context.log("response = " + remoteHttpConn.getResponseCode());
            }
            if (remoteIn == null) {
                try {
                    remoteIn = remoteConn.getInputStream();
                } catch (Exception e) {
                    context.log("stream", e);
                    remoteIn = remoteHttpConn.getErrorStream();
                }
            }
            response.setHeader("Date", null);
            response.setHeader("Server", null);
            context.log("response -----------");
            int remoteHdrIndex = 0;
            String remoteHdrName = remoteConn.getHeaderFieldKey(remoteHdrIndex);
            String val = remoteConn.getHeaderField(remoteHdrIndex);
            while (remoteHdrName != null || val != null) {
                String lhdr = remoteHdrName != null ? remoteHdrName.toLowerCase() : null;
                if (remoteHdrName != null && val != null && !_DontProxyHeaders.contains(lhdr)) response.addHeader(remoteHdrName, val);
                context.log("  " + remoteHdrName + ": " + val);
                remoteHdrIndex++;
                remoteHdrName = remoteConn.getHeaderFieldKey(remoteHdrIndex);
                val = remoteConn.getHeaderField(remoteHdrIndex);
            }
            response.addHeader("Via", "1.1 (jetty)");
            if (remoteIn != null) IO.copy(remoteIn, response.getOutputStream());
        }
    }

    /**
     * 
     * @param request
     * @param response
     * @return false if the cache file is not handled, true if the 
     * 		request was found in the local cache and has been handled.
     * @throws IOException 
     */
    private boolean handleJnlpCacheFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String scheme = request.getScheme();
        String userHome = System.getProperty("user.home");
        File parent = new File(userHome + "/Library/Caches/java/cache/javaws");
        parent = new File(parent, scheme);
        if (!parent.isDirectory()) {
            context.log(" can't find local path: " + parent.getPath());
            return false;
        }
        parent = new File(parent, "D" + request.getServerName());
        if (!parent.isDirectory()) {
            context.log(" can't find local path: " + parent.getPath());
            return false;
        }
        parent = new File(parent, "P" + request.getServerPort());
        if (!parent.isDirectory()) {
            context.log(" can't find local path: " + parent.getPath());
            return false;
        }
        String versionId = request.getParameter("version-id");
        if (versionId != null) {
            parent = new File(parent, "V" + versionId);
            if (!parent.isDirectory()) {
                context.log(" can't find local path: " + parent.getPath());
                return false;
            }
        }
        String requestURI = request.getRequestURI();
        String[] pathElements = requestURI.split("/");
        if (pathElements == null || pathElements.length < 2) {
            return false;
        }
        for (int i = 1; i < pathElements.length - 1; i++) {
            if (pathElements[i].length() == 0) {
                context.log(" found empty string in path: " + requestURI);
                continue;
            }
            parent = new File(parent, "DM" + pathElements[i]);
            if (!parent.isDirectory()) {
                context.log(" can't find local path: " + parent.getPath());
                return false;
            }
        }
        String fileName = pathElements[pathElements.length - 1];
        String prefix = null;
        String contentType = null;
        if (fileName.endsWith(".jar")) {
            prefix = "R";
            contentType = "application/x-java-archive";
        } else if (fileName.endsWith(".jnlp")) {
            prefix = "A";
            contentType = "application/x-java-jnlp-file";
        } else {
            context.log("can't handle file ending: " + fileName);
            return false;
        }
        File requestedFile = new File(parent, prefix + "M" + fileName);
        if (!requestedFile.isFile()) {
            context.log(" can't find local file: " + requestedFile.getPath());
            return false;
        }
        File requestedFileServerDate = new File(parent, prefix + "T" + fileName);
        BufferedReader dateReader = new BufferedReader(new FileReader(requestedFileServerDate));
        String modifiedTimeStr = dateReader.readLine();
        long modifiedTime = Long.parseLong(modifiedTimeStr);
        int requestMethod = METHOD_UNKNOWN;
        String uMethod = request.getMethod().toUpperCase();
        if ("GET".equals(uMethod)) {
            requestMethod = METHOD_GET;
        } else if ("HEAD".equals(uMethod)) {
            requestMethod = METHOD_HEAD;
        }
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (requestMethod == METHOD_GET && modifiedTime <= ifModifiedSince) {
            context.log("Not Modified");
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }
        response.setDateHeader("Last-Modified", modifiedTime);
        response.setHeader("Content-Length", "" + requestedFile.length());
        response.setHeader("Content-Type", contentType);
        if (versionId != null) {
            response.setHeader("x-java-jnlp-version-id", versionId);
        }
        if (requestMethod == METHOD_HEAD) {
            return true;
        }
        if (requestMethod != METHOD_GET) {
            context.log("can't handle request method: " + request.getMethod());
            return false;
        }
        context.log("sending local file: " + requestedFile.getPath());
        FileInputStream fileInput = new FileInputStream(requestedFile);
        IO.copy(fileInput, response.getOutputStream());
        context.log("  finished sending");
        return true;
    }

    public void handleConnect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        context.log("CONNECT: " + uri);
        String port = "";
        String host = "";
        int c = uri.indexOf(':');
        if (c >= 0) {
            port = uri.substring(c + 1);
            host = uri.substring(0, c);
            if (host.indexOf('/') > 0) host = host.substring(host.indexOf('/') + 1);
        }
        InetSocketAddress inetAddress = new InetSocketAddress(host, Integer.parseInt(port));
        {
            InputStream in = request.getInputStream();
            OutputStream out = response.getOutputStream();
            Socket socket = new Socket(inetAddress.getAddress(), inetAddress.getPort());
            context.log("Socket: " + socket);
            response.setStatus(200);
            response.setHeader("Connection", "close");
            response.flushBuffer();
            context.log("out<-in");
            IO.copyThread(socket.getInputStream(), out);
            context.log("in->out");
            IO.copy(in, socket.getOutputStream());
        }
    }

    public String getServletInfo() {
        return "Proxy Servlet";
    }

    public void destroy() {
    }
}
