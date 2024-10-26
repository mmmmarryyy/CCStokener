package com.outertrack.jspeedstreamer.http;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.outertrack.jspeedstreamer.utils.MultiLogger;

/**
 * This class reads from an input stream and parses a HTTP request.
 * 
 * It can then execute that request by using the 'execute' method
 * 
 * @author conorhunt
 *
 */
public class HttpRequest {

    private static MultiLogger log = MultiLogger.getLogger(HttpRequest.class);

    private HashMap<String, String> headers = new HashMap<String, String>();

    private String firstReqLine = null;

    private byte[] requestBody = null;

    private String requestType = null;

    private String host = null;

    private int hostPort = 80;

    private long originalStart = 0;

    private HashMap cachedSockets = new HashMap();

    /**
     * After a request has been made this object saves the headers and request and then that request
     * can be re-executed by calling this method.
     * 
     * @param startPosition
     * @param endPosition
     * @return
     * @throws IOException
     */
    public HttpRequest(InputStream input) throws IOException {
        parseHttpRequest(input);
    }

    /**
     * Read a http request and parse out the headers and the GET/POST line
     * 
     * TODO: this doesn't currently save the body of the request, only the headers!
     * 
     * @param in
     * @throws IOException
     */
    private void parseHttpRequest(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        firstReqLine = readLine(in, out);
        requestType = firstReqLine.substring(0, firstReqLine.indexOf(' '));
        log.debug(firstReqLine);
        String line = null;
        while ((line = readLine(in, out)).length() > 0) {
            int colonIndex = line.indexOf(':');
            if (colonIndex >= 0) headers.put(line.substring(0, colonIndex).toLowerCase().trim(), line.substring(colonIndex + 1).trim());
        }
        if (headers.get("content-length") != null) {
            int contentLength = Integer.parseInt(headers.get("content-length"));
            int b = -2;
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            for (int counter = 0; counter < contentLength && (b = in.read()) > 0; counter++) {
                out.write(b);
                body.write(b);
            }
            requestBody = body.toByteArray();
        }
        host = headers.get("host");
        if (firstReqLine.indexOf("jeturl=") >= 0) {
            int url_start = firstReqLine.indexOf("jeturl=");
            int url_end = firstReqLine.indexOf("/", url_start + 7);
            host = firstReqLine.substring(url_start + 7, url_end);
            String req = firstReqLine.substring(url_end, firstReqLine.indexOf(" ", url_end + 1));
            if (firstReqLine.indexOf("HTTP/1.1") > 0) firstReqLine = requestType + " " + req + " HTTP/1.1"; else firstReqLine = requestType + " " + req + " HTTP/1.0";
            headers.put("host", host);
        } else if (host == null) {
            int doubleSlashIndex = firstReqLine.indexOf("://");
            int nextSlashIndex = firstReqLine.indexOf("/", doubleSlashIndex + 3);
            host = firstReqLine.substring(doubleSlashIndex + 3, nextSlashIndex);
        }
        int colonIndex = host.indexOf(":");
        if (colonIndex > 0) {
            this.hostPort = Integer.parseInt(host.substring(colonIndex + 1));
            host = host.substring(0, colonIndex);
        }
        String range = headers.get("range");
        if (range != null) {
            originalStart = Long.parseLong(range.substring(range.indexOf("=") + 1, range.indexOf("-")));
        }
    }

    public String getRequestType() {
        return requestType;
    }

    /**
     * After a request has been made this object saves the headers and request and then that request
     * can be re-executed by calling this method.
     * 
     * This method also adds a Range header to the request from the startPosition to the
     * endPosition.
     * 
     * @param startPosition
     * @param endPosition
     * @return
     * @throws IOException
     */
    public HttpResponse execute(long startPosition, long endPosition) throws IOException {
        return execute(startPosition, endPosition, 5000);
    }

    public HttpResponse execute(long startPosition, long endPosition, int timeout) throws IOException {
        ArrayList socketList = (ArrayList) cachedSockets.get(host + hostPort);
        Socket server = null;
        if (socketList != null && socketList.size() > 0) server = (Socket) socketList.remove(0); else {
            server = new Socket(host, hostPort);
            server.setSoTimeout(timeout);
        }
        BufferedOutputStream serverOut = new BufferedOutputStream(server.getOutputStream());
        PrintWriter serverPrintOut = new PrintWriter(serverOut);
        serverPrintOut.write(firstReqLine + "\r\n");
        HashMap<String, String> newHeaders = (HashMap<String, String>) headers.clone();
        if (startPosition > 0 || endPosition > 0) {
            newHeaders.put("range", "bytes=" + (originalStart + startPosition) + "-" + (originalStart + endPosition));
        }
        Iterator it = newHeaders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            serverPrintOut.write(key + ": " + value + "\r\n");
        }
        serverPrintOut.write("\r\n");
        serverPrintOut.flush();
        if (requestBody != null) {
            serverOut.write(requestBody);
            serverOut.flush();
        }
        HttpResponse response = new HttpResponse(server);
        return response;
    }

    public HttpResponse execute() throws IOException {
        HttpResponse response = execute(0, 0);
        return response;
    }

    /**
     * Read a single line from an input stream and write out the data including line endings to an
     * output stream.
     * 
     * @param in
     * @param out
     * @return
     * @throws IOException
     */
    private String readLine(InputStream in, OutputStream out) throws IOException {
        StringBuffer buf = new StringBuffer(128);
        int b = 0;
        while ((b = in.read()) >= 0) {
            out.write(b);
            if (b == '\r') {
                b = in.read();
                out.write(b);
                break;
            }
            buf.append((char) b);
        }
        String bufString = buf.toString();
        return bufString;
    }
}
