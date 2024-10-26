package bw.os;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import bw.net.*;
import bw.util.*;
import com.google.appengine.api.urlfetch.*;
import static com.google.appengine.api.urlfetch.FetchOptions.Builder.*;

public class HttpObjectWriter extends AsynchronousObjectWriter {

    public static final String REMOTE_STORE = "/store";

    public static final String REMOTE_ERASE = "/remove";

    private String _httpRoot = null;

    public HttpObjectWriter(ServletContext context) {
        super(context);
        _httpRoot = context.getInitParameter("objectStore.httpWriter.root");
        Log.getInstance().write("Using HttpObjectWriter at: " + _httpRoot);
    }

    public HttpObjectWriter(String httpRoot) {
        super();
        _httpRoot = httpRoot;
        Log.getInstance().write("Using HttpObjectWriter at: " + _httpRoot);
    }

    public void asynchronousWrite(String key, Object obj) throws Exception {
        String urlStr = _httpRoot + REMOTE_STORE + "/" + makeFilename(key);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        ObjectOutputStream oos = getObjectOutputStream(conn.getOutputStream());
        oos.writeObject(obj);
        oos.close();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Problem putting response to server! " + urlStr + " ==> " + conn.getResponseCode());
        }
    }

    public void lowlevelWrite(String key, Object obj) throws Exception {
        String urlStr = _httpRoot + REMOTE_STORE + "/" + makeFilename(key);
        URL url = new URL(urlStr);
        HTTPRequest request = new HTTPRequest(url, HTTPMethod.PUT, doNotFollowRedirects());
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        ObjectOutputStream oos = getObjectOutputStream(boas);
        oos.writeObject(obj);
        oos.close();
        request.setPayload(boas.toByteArray());
        HTTPResponse response = URLFetchServiceFactory.getURLFetchService().fetch(request);
        if (response.getResponseCode() != 200) {
            Log.getInstance().warn("Problem writing " + key + ": " + response.getResponseCode());
        }
    }

    public void asynchronousErase(String key) throws Exception {
        String filename = makeFilename(key);
        NetResource resource = new NetResource(_httpRoot + REMOTE_ERASE + "/" + filename);
        HttpURLConnection conn = resource.getURLConnection();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Problem erasing key: " + key + "\n" + resource.toString() + " ==> " + conn.getResponseCode());
        }
    }
}
