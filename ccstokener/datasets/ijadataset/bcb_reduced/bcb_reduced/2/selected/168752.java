package org.wsether;

import org.apache.commons.lang.StringUtils;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Iterator;

/**
 * @author Jonathan Meeks
 * copyright 2008 Jonathan Meeks
 */
public class ForwardingFilter implements Filter {

    public static final String URL = "http://localhost:9800/";

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String body = ServletRequestUtils.getInstance().extractBody(servletRequest);
        Map parameterMap = servletRequest.getParameterMap();
        String method = ((HttpServletRequest) servletRequest).getMethod();
        String pathInfo = ((HttpServletRequest) servletRequest).getPathInfo();
        URL url = new URL(new URL(URL), pathInfo);
        HttpURLConnection httpConn = forwardRequest(url, body, parameterMap, method);
        String responseBody = handleResponse(httpConn);
        ServletResponseUtils.getInstance().populateResponse(responseBody, servletResponse);
    }

    protected String handleResponse(HttpURLConnection httpConn) throws IOException {
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        StringBuffer response = new StringBuffer("");
        String nextLine = br.readLine();
        while (nextLine != null) {
            response.append(nextLine);
            if ((nextLine = br.readLine()) != null) {
                response.append("\n");
            }
        }
        br.close();
        return response.toString();
    }

    protected HttpURLConnection forwardRequest(URL url, String body, Map parameterMap, String requestMethod) throws IOException {
        byte[] bodyBytes = body.getBytes();
        HttpURLConnection httpConn = handleRequestMethod(requestMethod, url, parameterMap);
        httpConn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", "");
        httpConn.setRequestMethod(requestMethod);
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);
        OutputStream outStream = httpConn.getOutputStream();
        outStream.write(bodyBytes);
        outStream.close();
        return httpConn;
    }

    protected HttpURLConnection handleRequestMethod(String requestMethod, URL url, Map parameterMap) throws IOException {
        HttpURLConnection httpConn = null;
        if (requestMethod.equalsIgnoreCase("GET")) {
            StringBuffer getSection = new StringBuffer("?");
            for (Iterator it = parameterMap.keySet().iterator(); it.hasNext(); ) {
                Object param = it.next();
                Object value = parameterMap.get(param);
                getSection.append(URLEncoder.encode(param.toString(), "UTF-8"));
                if (StringUtils.isEmpty(value.toString())) {
                    getSection.append("=");
                    getSection.append(URLEncoder.encode(parameterMap.get(param).toString(), "UTF-8"));
                }
                if (it.hasNext()) {
                    getSection.append("&");
                }
            }
            httpConn = (HttpURLConnection) new URL(url.toString() + getSection.toString()).openConnection();
        } else if (requestMethod.equalsIgnoreCase("POST")) {
            if (parameterMap.size() > 0) {
                throw new UnsupportedOperationException("Cannot handle post variables!");
            }
            httpConn = (HttpURLConnection) url.openConnection();
        } else {
            throw new UnsupportedOperationException("Cannot handle '" + requestMethod + "' method!");
        }
        return httpConn;
    }

    public void destroy() {
    }
}
