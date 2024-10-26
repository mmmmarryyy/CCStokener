package tristero.node;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import tristero.*;
import java.net.*;
import java.util.*;
import tristero.*;

public class Request extends HttpServlet {

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            System.out.println("init: " + name);
        }
        Config.init();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path != null) if (path.startsWith("/")) path = path.substring(1);
        System.out.println("path: " + path);
        String key = req.getParameter("key");
        if (key == null) key = path;
        key = URLDecoder.decode(key);
        String keyType = req.getParameter("keyType");
        if (keyType == null) keyType = "text";
        System.out.println("key: " + key);
        System.out.println("keyType: " + keyType);
        if (key == null) {
            printError(resp, resp.SC_NOT_FOUND, "Emtpy key.");
            return;
        }
        String address = req.getParameter("address");
        int htl;
        String htlStr = req.getParameter("htl");
        if (htlStr == null) htl = 0; else {
            try {
                htl = Integer.parseInt(htlStr);
            } catch (NumberFormatException e) {
                htl = 0;
            }
        }
        storeReference(address, key);
        InputStream is = findData(resp, key, htl);
        if (is != null) {
            resp.setContentType(guessContentType(key));
            Conduit.pump(is, resp.getOutputStream());
        }
    }

    protected String guessContentType(String key) {
        if (key == null) return "application/unknown";
        if (key.endsWith(".html")) return "text/html";
        if (key.endsWith(".jpg")) return "image/jpeg";
        if (key.endsWith(".jpeg")) return "image/jpeg";
        return "application/unknown";
    }

    protected void storeReference(String address, String key) {
        if (address != null && key != null) Config.refStore.addReference(address, key);
    }

    public InputStream findData(HttpServletResponse resp, String key, int htl) {
        InputStream is = getData(key);
        if (is != null) return is;
        Iterator iterator = Config.refStore.getAddressesForKey(key);
        while (iterator.hasNext()) {
            String address = (String) iterator.next();
            System.out.println("REF: " + address);
            try {
                return askNodeForKey(resp, address, key, htl);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        System.out.println("No references left.");
        printError(resp, resp.SC_NOT_FOUND, "No references left.");
        return null;
    }

    public InputStream askNodeForKey(HttpServletResponse resp, String address, String path, int htl) throws IOException, MalformedURLException {
        System.out.println("Asking " + address + " for " + path);
        Config.mapper.request(Config.address, address, path);
        URL url = makeURL(address, path, htl);
        System.out.println("url: " + url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        System.out.println("conn: " + conn);
        int code = getResponseCode(conn);
        if (code == conn.HTTP_OK) {
            System.out.println("Key " + path + " found on " + address);
            return conn.getInputStream();
        } else {
            System.out.println("File could not be found in the network");
            printError(resp, code, "Not found.");
            return null;
        }
    }

    public URL makeURL(String address, String key, int htl) throws MalformedURLException {
        String htlStr = new Integer(htl).toString();
        String urlStr = address + "/www/" + key;
        return new URL(urlStr);
    }

    public int getResponseCode(HttpURLConnection conn) {
        int code = 0;
        try {
            code = conn.getResponseCode();
        } catch (Exception f) {
            code = conn.HTTP_NOT_FOUND;
        }
        System.out.println("code: " + code);
        return code;
    }

    public InputStream getData(String key) {
        DataStore store = Config.dataStore;
        if (store == null) return null;
        try {
            return store.get(key);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public void printError(HttpServletResponse resp, int code, String message) {
        try {
            resp.sendError(code, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
