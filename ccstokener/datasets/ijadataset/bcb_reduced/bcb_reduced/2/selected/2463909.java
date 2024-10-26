package it.javalinux.lms.httpForward.service;

import it.javalinux.lms.httpForward.valueObject.Request;
import it.javalinux.lms.httpForward.valueObject.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * 
 * @author Stefano Maestri, stefano.maestri@javalinux.it
 * @author Alessio Soldano, alessio.soldano@javalinux.it
 */
public class HttpServiceProvider {

    public static final String CHARSET_ENC_ISO_LATIN1 = "ISO-8859-1";

    public static final String CHARSET_ENC_UNICODE8 = "UTF-8";

    public static final int DEFAULT_INPUT_BUFFER_SIZE = 100000;

    public static final String DEFAULT_HTTP_PROTO_VERSION = "HTTP/1.0";

    private String charset;

    private String httpProtoVersion;

    private int inputBufferSize;

    public HttpServiceProvider() {
        charset = CHARSET_ENC_ISO_LATIN1;
        httpProtoVersion = DEFAULT_HTTP_PROTO_VERSION;
        inputBufferSize = DEFAULT_INPUT_BUFFER_SIZE;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getHttpProtoVersion() {
        return httpProtoVersion;
    }

    public void setHttpProtoVersion(String httpProtoVersion) {
        this.httpProtoVersion = httpProtoVersion;
    }

    public int getInputBufferSize() {
        return inputBufferSize;
    }

    public void setInputBufferSize(int inputBufferSize) {
        this.inputBufferSize = inputBufferSize;
    }

    /**
     * Performs a HTTP GET connection to the specified remote host and try
     * to retrieve the incoming information associated to the request
     * 
     * @param httpURL
     *                The target URL with the encoded GET parameters
     * @param user
     *                The username used in the HTTP authentication
     * @param password
     *                The password used in the HTTP authentication
     * @return A string containing the result
     * @throws HttpAccessException
     */
    public Response executeGET(Request rq) throws Exception {
        Response result = new Response();
        HttpURLConnection connection = null;
        BufferedReader inputBufferReader = null;
        String fullAuthHeader = rq.getHeader("Authorization");
        String user = rq.getUsername();
        String password = rq.getPassword();
        String httpURL = rq.getUrl();
        try {
            Logger.getLogger(this.getClass()).debug("HTTP REQUEST DATA:\nURL: " + httpURL + "\nAUTH: " + fullAuthHeader + "\nUSER: " + user + "\nPASSWD: " + password);
            URL url = new URL(httpURL);
            connection = ((HttpURLConnection) url.openConnection());
            if (fullAuthHeader != null && !fullAuthHeader.equalsIgnoreCase("")) {
            } else {
                if (((user != null) && (password != null)) && ((!user.equals("")) && (!password.equals("")))) {
                    String userpasswd = URLEncoder.encode(user, charset) + ":" + URLEncoder.encode(password, charset);
                    String userpasswd64 = new sun.misc.BASE64Encoder().encode(userpasswd.getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + userpasswd64);
                }
            }
            for (Iterator it = rq.getHeaders().keySet().iterator(); it.hasNext(); ) {
                String k = (String) it.next();
                connection.setRequestProperty(k, rq.getHeader(k));
                Logger.getLogger(this.getClass()).debug("Setting header #" + k + "# -> #" + rq.getHeader(k) + "#");
            }
            connection.setRequestMethod("GET");
            connection.setRequestProperty("connection", "close");
            connection.setRequestProperty("connection-token", "close");
            connection.setRequestProperty("HTTP-Version", httpProtoVersion);
            connection.connect();
            Logger.getLogger(this.getClass()).debug("\nCONNECTED\nREADING...");
            result.setHeaders(connection.getHeaderFields());
            InputStream resInStream = null;
            if (connection.getResponseCode() == 200) {
                resInStream = connection.getInputStream();
            } else {
                Logger.getLogger(this.getClass()).debug("ERROR CODE RECEIVED: " + connection.getResponseCode());
                resInStream = connection.getErrorStream();
            }
            inputBufferReader = new BufferedReader(new InputStreamReader(resInStream, charset), inputBufferSize);
            String line = null;
            StringBuffer lineBuffer = new StringBuffer();
            while ((line = inputBufferReader.readLine()) != null) {
                lineBuffer.append(line);
                lineBuffer.append("\n");
            }
            inputBufferReader.close();
            Logger.getLogger(this.getClass()).debug("READ COMPLETED, DISCONNECTING...");
            connection.disconnect();
            Logger.getLogger(this.getClass()).debug("DISCONNECTED");
            result.setBody(lineBuffer.toString());
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error("\nHTTP REQUEST FAILED. \nERROR MESSAGE: " + e.getMessage() + "\nURL: " + httpURL);
            throw new Exception("HttpServiceProvider.executeGET: " + e.getMessage());
        } finally {
            try {
                inputBufferReader.close();
            } catch (Exception e) {
            }
            try {
                connection.disconnect();
            } catch (Exception e) {
            }
        }
        return result;
    }

    public Response executePOST(Request rq) throws Exception {
        Response result = new Response();
        HttpURLConnection connection = null;
        BufferedReader inputBufferReader = null;
        BufferedWriter outputBufferWriter = null;
        String fullAuthHeader = rq.getHeader("Authorization");
        String user = rq.getUsername();
        String password = rq.getPassword();
        String httpURL = rq.getUrl();
        byte[] postData = rq.getPostData();
        try {
            Logger.getLogger(this.getClass()).debug("HTTP REQUEST DATA:\nURL: " + httpURL + "\nAUTH: " + fullAuthHeader + "\nUSER: " + user + "\nPASSWD: " + password);
            URL url = new URL(httpURL);
            connection = ((HttpURLConnection) url.openConnection());
            if (fullAuthHeader != null && !fullAuthHeader.equalsIgnoreCase("")) {
            } else {
                if (((user != null) && (password != null)) && ((!user.equals("")) && (!password.equals("")))) {
                    String userpasswd = URLEncoder.encode(user, charset) + ":" + URLEncoder.encode(password, charset);
                    String userpasswd64 = new sun.misc.BASE64Encoder().encode(userpasswd.getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + userpasswd64);
                }
            }
            for (Iterator it = rq.getHeaders().keySet().iterator(); it.hasNext(); ) {
                String k = (String) it.next();
                connection.setRequestProperty(k, rq.getHeader(k));
                Logger.getLogger(this.getClass()).debug("Setting header #" + k + "# -> #" + rq.getHeader(k) + "#");
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("connection", "close");
            connection.setRequestProperty("connection-token", "close");
            connection.setRequestProperty("HTTP-Version", httpProtoVersion);
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(postData);
            Logger.getLogger(this.getClass()).debug("\nPOST DOCUMENT WRITTEN..\nCONNECTING");
            connection.connect();
            Logger.getLogger(this.getClass()).debug("\nCONNECTED\nREADING...");
            result.setHeaders(connection.getHeaderFields());
            InputStream resInStream = null;
            if (connection.getResponseCode() == 200) {
                resInStream = connection.getInputStream();
            } else {
                Logger.getLogger(this.getClass()).debug("ERROR CODE RECEIVED: " + connection.getResponseCode());
                resInStream = connection.getErrorStream();
            }
            inputBufferReader = new BufferedReader(new InputStreamReader(resInStream, charset), inputBufferSize);
            String line = null;
            StringBuffer lineBuffer = new StringBuffer();
            while ((line = inputBufferReader.readLine()) != null) {
                lineBuffer.append(line);
                lineBuffer.append("\n");
            }
            inputBufferReader.close();
            Logger.getLogger(this.getClass()).debug("READ COMPLETED, DISCONNECTING...");
            connection.disconnect();
            Logger.getLogger(this.getClass()).debug("DISCONNECTED");
            result.setBody(lineBuffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger(this.getClass()).error("\nHTTP REQUEST FAILED. \nERROR MESSAGE: " + e.getMessage() + "\nURL: " + httpURL);
            throw new Exception("HttpServiceProvider.executeGET: " + e.getMessage());
        } finally {
            try {
                outputBufferWriter.close();
            } catch (Exception e) {
            }
            try {
                inputBufferReader.close();
            } catch (Exception e) {
            }
            try {
                connection.disconnect();
            } catch (Exception e) {
            }
        }
        return result;
    }

    /**
     * Creates a HTTP URL compliant with the GET parameters encoding
     * 
     * @param httpURLBase
     *                The fixed part of the URL
     * @param parameters
     *                The parameters to append to the URL
     * @return A HTTP URL containing both the destination server address and
     *         the parameters needed to perform a GET request
     * @throws HttpAccessException
     */
    public String createHttpGETUrl(String httpURLBase, Map parameters) throws Exception {
        String result = null;
        StringBuffer paramBuffer = new StringBuffer();
        if ((parameters != null) && (parameters.size() > 0)) {
            try {
                paramBuffer.append("?");
                for (Iterator iter = parameters.keySet().iterator(); iter.hasNext(); ) {
                    String paramName = (String) iter.next();
                    String pn = URLEncoder.encode(paramName, charset);
                    paramBuffer.append(pn);
                    if (!("wsdl".equalsIgnoreCase(pn) && parameters.size() == 1)) {
                        paramBuffer.append("=");
                        paramBuffer.append(URLEncoder.encode((String) parameters.get(paramName), charset));
                    }
                    if (iter.hasNext()) {
                        paramBuffer.append("&");
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Logger.getLogger(this.getClass()).info("appendEncodedURLParameters failed with error message: " + uee.getMessage());
                uee.printStackTrace();
                throw new Exception("HttpSPAccess.appendEncodedURLParameters: " + uee.getMessage());
            }
        }
        result = httpURLBase + paramBuffer.toString();
        Logger.getLogger(this.getClass()).debug("HTTP GET URL: " + result);
        return result;
    }
}
