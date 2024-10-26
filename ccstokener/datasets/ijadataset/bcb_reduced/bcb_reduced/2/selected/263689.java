package nl.bsoft.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class WebProxy extends HttpServlet implements LoggingComposite {

    private static Logger logger = Logger.getLogger(WebProxy.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ServletContext servletContext;

    private String outputdir = null;

    private boolean loggingSwitch = true;

    private boolean outputSwitch = true;

    private boolean switchToSecure = false;

    private boolean verifyHostName = false;

    private int timeout = 0;

    private static Configuration configuration = null;

    private LoggingDelegate loggingDelegate = new LoggingDelegate(this);

    public void init(final ServletConfig servletConfig) throws ServletException {
        logger.debug("Started, in init");
        servletContext = servletConfig.getServletContext();
        if (configuration == null) {
            configuration = new Configuration();
            configuration.setConfigFile("web-proxy-config.xml");
        }
        outputdir = configuration.getString("web-proxy.outputdir");
        loggingSwitch = new Boolean(configuration.getString("web-proxy.loggingswitch")).booleanValue();
        outputSwitch = new Boolean(configuration.getString("web-proxy.outputswitch")).booleanValue();
        switchToSecure = new Boolean(configuration.getString("web-proxy.switchtosecure")).booleanValue();
        verifyHostName = new Boolean(configuration.getString("web-proxy.verifyhostname")).booleanValue();
        timeout = configuration.getInt("web-proxy.timeout");
    }

    public void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        doProcess(request, response);
    }

    public void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        doProcess(request, response);
    }

    private void doProcess(final HttpServletRequest request, final HttpServletResponse response) {
        boolean isSecure = false;
        try {
            String uniekId = java.util.UUID.randomUUID().toString();
            String requestString = request.toString();
            if (loggingSwitch) {
                loggingDelegate.logRequest(requestString, uniekId);
                loggingDelegate.logParameter("web-proxy.outputdir", outputdir, uniekId);
                loggingDelegate.logParameter("web-proxy.loggingswitch", Boolean.toString(loggingSwitch), uniekId);
                loggingDelegate.logParameter("web-proxy.outputswitch", Boolean.toString(outputSwitch), uniekId);
                loggingDelegate.logParameter("kweb-proxy.switchtosecure", Boolean.toString(switchToSecure), uniekId);
                loggingDelegate.logParameter("web-proxy.verifyhostname", Boolean.toString(verifyHostName), uniekId);
                loggingDelegate.logParameter("web-proxy.timeout", Integer.toString(timeout), uniekId);
            }
            BufferedWriter outputRequest = null;
            outputRequest = openOutputStream(uniekId, "-request.xml");
            writeToFile(outputRequest, requestString, uniekId);
            String urlString = request.getRequestURL().toString();
            if (loggingSwitch) {
                loggingDelegate.logUrlString(urlString, uniekId);
            }
            String queryString = request.getQueryString();
            urlString += queryString == null ? "" : "?" + queryString;
            URL url = new URL(urlString);
            if (url.getProtocol().equalsIgnoreCase("http")) {
                logger.debug("doProcess - received - not secure");
                isSecure = false;
            }
            if (url.getProtocol().equalsIgnoreCase("https")) {
                logger.debug("doProcess - received - secure");
                isSecure = true;
            }
            if (!isSecure) {
                logger.debug("doProcess - start - not secure");
                if (switchToSecure) {
                    logger.debug("doProcess - start - not secure - switch to secure");
                    isSecure = true;
                    String protocol = "https";
                    String host = url.getHost();
                    int port = url.getPort();
                    String secureUrl = protocol;
                    secureUrl += "://";
                    secureUrl += host;
                    if (port > 0) {
                        secureUrl += ":" + Integer.toString(port);
                    }
                    secureUrl += url.getPath();
                    secureUrl += queryString == null ? "" : "?" + queryString;
                    url = new URL(secureUrl);
                    if (loggingSwitch) {
                        loggingDelegate.logUrlString("Switched to url: " + secureUrl, uniekId);
                    }
                }
            }
            if (loggingSwitch) {
                loggingDelegate.logFetchingURL(url.toString(), uniekId);
            }
            if (isSecure) {
                procesSecure(request, response, uniekId, url);
            } else {
                procesNonSecure(request, response, uniekId, url);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
        }
    }

    private void procesNonSecure(final HttpServletRequest request, final HttpServletResponse response, String uniekId, URL url) throws IOException, ProtocolException {
        BufferedInputStream webToProxyBuf;
        BufferedOutputStream proxyToClientBuf;
        HttpURLConnection con;
        int statusCode;
        int oneByte;
        String methodName;
        BufferedWriter outputRequest = null;
        String requestString = request.toString();
        outputRequest = openOutputStream(uniekId, "-request.xml");
        writeToFile(outputRequest, requestString, uniekId);
        con = (HttpURLConnection) url.openConnection();
        methodName = request.getMethod();
        con.setRequestMethod(methodName);
        con.setDoOutput(true);
        con.setDoInput(true);
        HttpURLConnection.setFollowRedirects(false);
        con.setUseCaches(true);
        if (timeout > 0) {
            con.setConnectTimeout(timeout);
        }
        for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
            String headerName = e.nextElement().toString();
            String headerValue = request.getHeader(headerName);
            con.setRequestProperty(headerName, headerValue);
            writeToFile(outputRequest, headerName + " : " + headerValue + "\n", uniekId);
        }
        con.connect();
        if (methodName.equals("POST")) {
            BufferedInputStream clientToProxyBuf = new BufferedInputStream(request.getInputStream());
            BufferedOutputStream proxyToWebBuf = new BufferedOutputStream(con.getOutputStream());
            writeToFile(outputRequest, "ProxyToWebBuf: \n", uniekId);
            while ((oneByte = clientToProxyBuf.read()) != -1) {
                proxyToWebBuf.write(oneByte);
                outputRequest.write(oneByte);
            }
            proxyToWebBuf.flush();
            outputRequest.flush();
            proxyToWebBuf.close();
            clientToProxyBuf.close();
        }
        closeStream(outputRequest, uniekId);
        statusCode = con.getResponseCode();
        response.setStatus(statusCode);
        BufferedWriter outputResponse = openOutputStream(uniekId, "-response.xml");
        writeToFile(outputResponse, "Response-Statuscode: " + Integer.toString(statusCode) + "\nHTTP Headers:\n", uniekId);
        for (Iterator i = con.getHeaderFields().entrySet().iterator(); i.hasNext(); ) {
            Map.Entry mapEntry = (Map.Entry) i.next();
            if (mapEntry.getKey() != null) {
                String key = mapEntry.getKey().toString();
                String value = ((List) mapEntry.getValue()).get(0).toString();
                writeToFile(outputResponse, key + " : " + value + "\n", uniekId);
                response.setHeader(key, value);
            }
        }
        webToProxyBuf = new BufferedInputStream(con.getInputStream());
        proxyToClientBuf = new BufferedOutputStream(response.getOutputStream());
        writeToFile(outputResponse, "Response:  \n", uniekId);
        while ((oneByte = webToProxyBuf.read()) != -1) {
            proxyToClientBuf.write(oneByte);
            outputResponse.write(oneByte);
        }
        proxyToClientBuf.flush();
        outputResponse.flush();
        closeStream(outputResponse, uniekId);
        proxyToClientBuf.close();
        webToProxyBuf.close();
        con.disconnect();
    }

    private void procesSecure(final HttpServletRequest request, final HttpServletResponse response, String uniekId, URL url) throws IOException, ProtocolException {
        BufferedInputStream webToProxyBuf = null;
        BufferedOutputStream proxyToClientBuf = null;
        HttpsURLConnection cons = null;
        int statusCode = 0;
        int oneByte = 0;
        String methodName = null;
        MyVerifier myVerifier = null;
        BufferedWriter outputRequest = null;
        String requestString = request.toString();
        outputRequest = openOutputStream(uniekId, "-request.xml");
        writeToFile(outputRequest, requestString, uniekId);
        cons = (HttpsURLConnection) url.openConnection();
        methodName = request.getMethod();
        cons.setRequestMethod(methodName);
        cons.setDoOutput(true);
        cons.setDoInput(true);
        HttpsURLConnection.setFollowRedirects(false);
        cons.setUseCaches(true);
        if (timeout > 0) {
            cons.setConnectTimeout(timeout);
        }
        if (verifyHostName) {
            myVerifier = new MyVerifier();
            cons.setHostnameVerifier(myVerifier);
        }
        for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
            String headerName = e.nextElement().toString();
            String headerValue = request.getHeader(headerName);
            cons.setRequestProperty(headerName, headerValue);
            writeToFile(outputRequest, headerName + " : " + headerValue + "\n", uniekId);
        }
        cons.connect();
        if (methodName.equals("POST")) {
            BufferedInputStream clientToProxyBuf = new BufferedInputStream(request.getInputStream());
            BufferedOutputStream proxyToWebBuf = new BufferedOutputStream(cons.getOutputStream());
            writeToFile(outputRequest, "ProxyToWebBuf: \n", uniekId);
            while ((oneByte = clientToProxyBuf.read()) != -1) {
                proxyToWebBuf.write(oneByte);
                outputRequest.write(oneByte);
            }
            proxyToWebBuf.flush();
            outputRequest.flush();
            proxyToWebBuf.close();
            clientToProxyBuf.close();
        }
        closeStream(outputRequest, uniekId);
        statusCode = cons.getResponseCode();
        response.setStatus(statusCode);
        BufferedWriter outputResponse = openOutputStream(uniekId, "-response.xml");
        writeToFile(outputResponse, "Response-Statuscode: " + Integer.toString(statusCode) + "\nHTTP Headers:\n", uniekId);
        for (Iterator i = cons.getHeaderFields().entrySet().iterator(); i.hasNext(); ) {
            Map.Entry mapEntry = (Map.Entry) i.next();
            if (mapEntry.getKey() != null) {
                String key = mapEntry.getKey().toString();
                String value = ((List) mapEntry.getValue()).get(0).toString();
                writeToFile(outputResponse, key + " : " + value + "\n", uniekId);
                response.setHeader(key, value);
            }
        }
        webToProxyBuf = new BufferedInputStream(cons.getInputStream());
        proxyToClientBuf = new BufferedOutputStream(response.getOutputStream());
        writeToFile(outputResponse, "Response:  \n", uniekId);
        while ((oneByte = webToProxyBuf.read()) != -1) {
            proxyToClientBuf.write(oneByte);
            outputResponse.write(oneByte);
        }
        proxyToClientBuf.flush();
        outputResponse.flush();
        closeStream(outputResponse, uniekId);
        proxyToClientBuf.close();
        webToProxyBuf.close();
        cons.disconnect();
    }

    private void closeStream(BufferedWriter outputWriter, String uniekId) {
        try {
            if (outputSwitch) {
                outputWriter.flush();
                outputWriter.close();
            }
        } catch (IOException ioException) {
            loggingDelegate.logIOException(ioException, uniekId);
        }
    }

    private BufferedWriter openOutputStream(final String uniekId, final String postFix) {
        BufferedWriter writer = null;
        try {
            if (outputSwitch) {
                FileWriter fileStreamRequest = new FileWriter(outputdir + "/" + uniekId + postFix);
                writer = new BufferedWriter(fileStreamRequest);
            }
        } catch (IOException ioException) {
            writer = null;
            loggingDelegate.logIOException(ioException, uniekId);
        }
        return writer;
    }

    private void writeToFile(final BufferedWriter response, final String line, String uniekId) {
        try {
            if (outputSwitch) {
                response.write(line);
            }
        } catch (IOException ioException) {
            loggingDelegate.logIOException(ioException, uniekId);
        }
    }

    private static class LoggingDelegate extends AbstractLoggingDelegate {

        public LoggingDelegate(final LoggingComposite lc) {
            super(lc);
        }

        public void logParameter(final String parameterName, final String parameterValue, final String id) {
            logger.debug(id + " Parameter " + parameterName + ": " + parameterValue);
        }

        public void logUrlString(final String urlString, final String id) {
            logger.debug(id + " urlString: " + urlString);
        }

        public void logRequest(final String requestString, final String id) {
            logger.debug(id + " request: " + requestString);
        }

        public void logFetchingURL(final String urlString, final String id) {
            logger.info(id + " fetching: " + urlString);
        }

        public void logIOException(final IOException ioException, final String id) {
            logger.error(id + " ioException: ", ioException);
        }
    }
}
