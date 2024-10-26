package org.nbn.ontodas.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.nbn.ontodas.datafactories.CombinableDataObjectsFactory;

/**
 * Servlet acting as a DAS proxy for Dasty2 to get around the same origin problem.
 * The DAS service is sent, and the results (including headers) forwarded.
 * DAS exceptions are not yet forwarded correctly.
 * @author kieran
 *
 */
public class DastyProxyServlet extends HttpServlet {

    Logger logger = null;

    public DastyProxyServlet() {
        this.logger = Logger.getLogger(CombinableDataObjectsFactory.class);
    }

    /**
	 * Construct the DAS request using the server, method, id given by Dasty.
	 * (Different methods require slightly different URL patterns).
	 * @param server
	 * @param method
	 * @param id
	 * @return
	 */
    private String generateUrl(String server, String method, String id) throws ServletException {
        if (server.charAt(server.length() - 1) != '/') {
            server = server + "/";
        }
        String reqUrl;
        if (method.equals("features") || method.equals("sequence")) {
            reqUrl = server + method + "?segment=" + id;
        } else {
            if (method.equals("types") || method.equals("stylesheet")) {
                reqUrl = server + method;
            } else {
                if (method.equals("registry")) {
                    if (id == "") {
                        reqUrl = server;
                    } else {
                        reqUrl = server + "?label=" + id;
                    }
                } else {
                    throw new ServletException("Method not allowed: " + method);
                }
            }
        }
        return reqUrl;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, String[]> parMap = request.getParameterMap();
        String method = parMap.get("m")[0];
        String server = parMap.get("s")[0];
        String id;
        if (parMap.containsKey("q")) {
            id = parMap.get("q")[0];
        } else {
            id = "";
        }
        Integer timeout;
        if (parMap.containsKey("t")) {
            timeout = Integer.valueOf(parMap.get("t")[0]);
        } else {
            timeout = 5;
        }
        String reqUrl = generateUrl(server, method, id);
        URL url = new URL(reqUrl);
        try {
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            if (connect.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try {
                    InputStream iStream = connect.getInputStream();
                    OutputStream oStream = response.getOutputStream();
                    byte[] data = new byte[4096];
                    int readLen = -1;
                    while ((readLen = iStream.read(data)) > 0) {
                        oStream.write(data, 0, readLen);
                    }
                    iStream.close();
                    oStream.close();
                } catch (IOException e) {
                    throw new ServletException("Error reading from DAS server: " + e.getMessage());
                }
            } else {
                response.sendError(connect.getResponseCode());
            }
        } catch (ConnectException c) {
            response.sendError(404);
        }
    }
}
