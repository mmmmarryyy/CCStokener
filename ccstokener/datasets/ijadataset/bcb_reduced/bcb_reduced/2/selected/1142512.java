package com.jmircwordgames;

import java.io.*;
import org.apache.http.*;
import java.util.Vector;
import java.net.*;

public class HttpIrc extends IrcConnection {

    private Vector inqueue;

    private String encoding, identifier, gwhost, gwpasswd, outbuf;

    private boolean utf8detect, utf8output;

    private int gwport;

    private boolean connected, closeconn;

    private int bytein;

    private int byteout;

    public HttpIrc(String gwhost, int gwport, String gwpasswd, String charset) {
        this.gwhost = gwhost;
        this.gwport = gwport;
        this.gwpasswd = gwpasswd;
        encoding = charset;
        inqueue = new Vector();
        outbuf = null;
        utf8detect = false;
        utf8output = false;
        bytein = 0;
        byteout = 0;
        connected = closeconn = false;
        identifier = "";
    }

    public String connect(String host, int port, String init) {
        HttpURLConnection c = null;
        String ret = "";
        int response;
        URL url = null;
        try {
            url = new URL("http://" + gwhost + ":" + gwport + "/connect?host=" + Utils.URLEncode(host.getBytes()) + "&port=" + Utils.URLEncode(("" + port).getBytes()) + "&passwd=" + Utils.URLEncode(gwpasswd.getBytes()) + "&data=" + Utils.URLEncode(stringToByteArray(init, encoding, utf8output)));
        } catch (MalformedURLException me) {
            ret += "Exception: " + me.getMessage();
        }
        try {
            c = (HttpURLConnection) url.openConnection();
            response = c.getResponseCode();
            identifier = c.getHeaderField("X-Identifier");
            if (c != null) c.disconnect();
        } catch (Exception e) {
            ret += "Error trying to connect to HTTP proxy server, aborting... ";
            ret += "Exception: " + e.getMessage();
            return ret;
        }
        if (response != HttpStatus.SC_OK) {
            ret += "Error trying to connect to IRC server, reason: ";
            switch(response) {
                case HttpStatus.SC_FORBIDDEN:
                    ret += "Wrong password";
                    break;
                case HttpStatus.SC_BAD_GATEWAY:
                    ret += "Bad gateway";
                    break;
                case HttpStatus.SC_NOT_FOUND:
                    ret += "IRC connection not found";
                    break;
                default:
                    ret += "HTTP response code: " + response;
                    break;
            }
            return ret;
        } else {
            connected = true;
            return null;
        }
    }

    public void disconnect() {
        if (connected) closeconn = true;
    }

    public String readLine() {
        String ret;
        if (inqueue.size() > 0) {
            ret = (String) inqueue.firstElement();
            inqueue.removeElementAt(0);
        } else ret = "";
        return ret;
    }

    public String updateConnection() {
        String url, ret;
        url = "http://" + gwhost + ":" + gwport + "/" + identifier;
        if (outbuf != null) {
            url += "?data=" + Utils.URLEncode(stringToByteArray(outbuf, encoding, utf8output));
            byteout += url.getBytes().length;
            outbuf = null;
        }
        ret = handleRequest(url, true);
        if (closeconn) connected = false;
        return ret;
    }

    public String writeData(String data) {
        if (outbuf == null) outbuf = data; else outbuf += data;
        return null;
    }

    private String handleRequest(String url, boolean get) {
        HttpURLConnection c = null;
        InputStream is = null;
        ByteArrayInputStream bais;
        byte[] buf;
        String temp, ret = "";
        int response, len, i;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            if (get) c.setRequestMethod("GET"); else c.setRequestMethod("HEAD");
            response = c.getResponseCode();
            if (get) {
                is = c.getInputStream();
                len = (int) c.getContentLength();
                if (len > 0) {
                    byte[] data = new byte[len];
                    for (i = 0; i < len; i++) {
                        data[i] = (byte) is.read();
                    }
                    bytein += data.length;
                    bais = new ByteArrayInputStream(data);
                    while (bais.available() > 0) {
                        buf = Utils.readLine(bais);
                        if (buf != null) {
                            temp = byteArrayToString(buf, encoding, utf8detect);
                            inqueue.addElement(temp);
                        }
                    }
                }
            }
            if (is != null) is.close();
            if (c != null) c.disconnect();
        } catch (Exception e) {
            ret += "Request failed, continuing...";
            return ret;
        }
        if (response != HttpStatus.SC_OK) {
            if (response != HttpStatus.SC_NOT_FOUND) {
                ret += "Error in connection to IRC server, aborting... ";
                ret += "Error: HTTP response code: " + response;
            }
            connected = false;
            return ret;
        } else return null;
    }

    public boolean hasDataInBuffer() {
        if (inqueue.size() == 0) return false; else return true;
    }

    public void setUnicodeMode(boolean utf8detect, boolean utf8output) {
        this.utf8detect = utf8detect;
        this.utf8output = utf8output;
    }

    public boolean isConnected() {
        return connected;
    }

    public int getBytesIn() {
        return bytein;
    }

    public int getBytesOut() {
        return byteout;
    }
}
