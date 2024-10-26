package bw.net;

import java.net.*;
import java.io.*;
import java.util.*;
import bw.os.*;
import bw.util.*;

public class NetResource {

    public static final int NETWORK_DOWN = 0;

    public static final int HTTP_NOT_OK = 1;

    private static final String CONDITIONAL_GET_TABLE_KEY = "WD_COND_GET_TABLE";

    private URL _url = null;

    private String _username = null;

    private String _password = null;

    private int _error = -1;

    private int _httpResponseCode = -1;

    private boolean _doConditionalGET = false;

    private boolean _shouldGET = true;

    public NetResource(String url) throws MalformedURLException {
        _url = new URL(url);
    }

    public NetResource(String url, String username, String password) throws MalformedURLException {
        _url = new URL(url);
        _username = username;
        _password = password;
    }

    public void doConditionalGET() {
        _doConditionalGET = true;
    }

    public boolean shouldGET() {
        return _shouldGET;
    }

    public int getError() {
        return _error;
    }

    public HttpURLConnection getURLConnection() throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) _url.openConnection();
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", "WD-2.0");
            if (_doConditionalGET) {
                ResourceInfo ri = (ResourceInfo) conditionalGetTable().get(_url.toString());
                if (ri != null) {
                    if (ri.lastModified != null) {
                        conn.setRequestProperty("If-Modified-Since", ri.lastModified);
                    }
                    if (ri.etag != null) {
                        conn.setRequestProperty("If-None-Match", ri.etag);
                    }
                }
            }
            if (_username != null && _password != null) {
                String authenticationStr = _username + ":" + _password;
                String encodedAuthStr = Base64.encodeBytes(authenticationStr.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encodedAuthStr);
            }
            _httpResponseCode = conn.getResponseCode();
            if (_httpResponseCode == HttpURLConnection.HTTP_OK) {
                if (_doConditionalGET) {
                    ResourceInfo ri = new ResourceInfo();
                    ri.lastModified = conn.getHeaderField("Last-Modified");
                    ri.etag = conn.getHeaderField("ETag");
                    Hashtable table = conditionalGetTable();
                    table.put(_url.toString(), ri);
                    storeConditionalGetTable(table);
                }
            } else if (_httpResponseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                _shouldGET = false;
            } else {
                Log.getInstance().write("Error getting url: " + _url + "\n" + "Error code: " + _httpResponseCode);
                _error = HTTP_NOT_OK;
                conn.disconnect();
                conn = null;
            }
        } catch (SocketException ex) {
            conn.disconnect();
            conn = null;
            _error = NETWORK_DOWN;
        }
        return conn;
    }

    public InputStream getInputStream() throws IOException {
        InputStream in = null;
        HttpURLConnection conn = getURLConnection();
        if (conn != null) {
            in = conn.getInputStream();
        }
        return in;
    }

    public int getResponseCode() {
        return _httpResponseCode;
    }

    private Hashtable conditionalGetTable() {
        Hashtable cgt = null;
        try {
            MemoryCache cache = ObjectStore.getInstance().getMemoryCache();
            cgt = (Hashtable) cache.get(CONDITIONAL_GET_TABLE_KEY);
        } catch (Exception ex) {
            Log.getInstance().write("Problem getting CONDITIONAL GET table", ex);
        }
        if (cgt == null) {
            cgt = new Hashtable();
        }
        return cgt;
    }

    private void storeConditionalGetTable(Hashtable table) {
        try {
            MemoryCache cache = ObjectStore.getInstance().getMemoryCache();
            cache.put(CONDITIONAL_GET_TABLE_KEY, table);
        } catch (Exception ex) {
            Log.getInstance().write("Problem putting CONDITIONAL GET table", ex);
        }
    }

    public String toString() {
        return _url.toString();
    }
}
