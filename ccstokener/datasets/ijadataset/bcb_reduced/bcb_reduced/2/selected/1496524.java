package com.carey.renren;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import com.carey.renren.utils.RenRenHttpUtil;

public class RenRenHttpClient {

    private static final int CONNECTION_TIMEOUT = 20000;

    public RenRenHttpClient() {
    }

    /**
	 * Using GET method.
	 * 
	 * @param url
	 *            The remote URL.
	 * @param queryString
	 *            The query string containing parameters
	 * @return Response string.
	 * @throws Exception
	 */
    public String httpGet(String url, String queryString) throws Exception {
        String responseData = null;
        if (queryString != null && !queryString.equals("")) {
            url += "?" + queryString;
        }
        HttpClient httpClient = new HttpClient();
        GetMethod httpGet = new GetMethod(url);
        httpGet.getParams().setParameter("http.socket.timeout", new Integer(CONNECTION_TIMEOUT));
        try {
            int statusCode = httpClient.executeMethod(httpGet);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("HttpGet Method failed: " + httpGet.getStatusLine());
            }
            responseData = httpGet.getResponseBodyAsString();
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            httpGet.releaseConnection();
            httpClient = null;
        }
        return responseData;
    }

    /**
	 * Using POST method.
	 * 
	 * @param url
	 *            The remote URL.
	 * @param queryString
	 *            The query string containing parameters
	 * @return Response string.
	 * @throws Exception
	 */
    public String httpPost(String url, String queryString) throws Exception {
        String responseData = null;
        HttpClient httpClient = new HttpClient();
        PostMethod httpPost = new PostMethod(url);
        httpPost.addParameter("Content-Type", "application/x-www-form-urlencoded");
        httpPost.getParams().setParameter("http.socket.timeout", new Integer(CONNECTION_TIMEOUT));
        if (queryString != null && !queryString.equals("")) {
            httpPost.setRequestEntity(new ByteArrayRequestEntity(queryString.getBytes()));
        }
        try {
            int statusCode = httpClient.executeMethod(httpPost);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("HttpPost Method failed: " + httpPost.getStatusLine());
            }
            responseData = httpPost.getResponseBodyAsString();
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            httpPost.releaseConnection();
            httpClient = null;
        }
        return responseData;
    }

    /**
	 * Using POST method with multiParts.
	 * 
	 * @param url
	 *            The remote URL.
	 * @param queryString
	 *            The query string containing parameters
	 * @param files
	 *            The list of image files
	 * @return Response string.
	 * @throws Exception
	 */
    public String httpPostWithFile(String url, String queryString, List<RenRenParameter> files) throws Exception {
        String responseData = null;
        url += '?' + queryString;
        HttpClient httpClient = new HttpClient();
        PostMethod httpPost = new PostMethod(url);
        try {
            List<RenRenParameter> listParams = RenRenHttpUtil.getQueryParameters(queryString);
            int length = listParams.size() + (files == null ? 0 : files.size());
            Part[] parts = new Part[length];
            int i = 0;
            for (RenRenParameter param : listParams) {
                parts[i++] = new StringPart(param.mName, RenRenHttpUtil.formParamDecode(param.mValue), "UTF-8");
            }
            for (RenRenParameter param : files) {
                File file = new File(param.mValue);
                parts[i++] = new FilePart(param.mName, file.getName(), file, RenRenHttpUtil.getContentType(file), "UTF-8");
            }
            httpPost.setRequestEntity(new MultipartRequestEntity(parts, httpPost.getParams()));
            int statusCode = httpClient.executeMethod(httpPost);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("HttpPost Method failed: " + httpPost.getStatusLine());
            }
            responseData = httpPost.getResponseBodyAsString();
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            httpPost.releaseConnection();
            httpClient = null;
        }
        return responseData;
    }

    public static String doPost(String reqUrl, Map<String, String> parameters) {
        HttpURLConnection urlConn = null;
        try {
            urlConn = sendPost(reqUrl, parameters);
            String responseContent = getContent(urlConn);
            return responseContent.trim();
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
                urlConn = null;
            }
        }
    }

    private static String getContent(HttpURLConnection urlConn) {
        try {
            String responseContent = null;
            InputStream in = urlConn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer tempStr = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                tempStr.append(tempLine);
                tempStr.append(crlf);
                tempLine = rd.readLine();
            }
            responseContent = tempStr.toString();
            rd.close();
            in.close();
            return responseContent;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static HttpURLConnection sendPost(String reqUrl, Map<String, String> parameters) {
        HttpURLConnection urlConn = null;
        try {
            String params = generatorParamString(parameters);
            URL url = new URL(reqUrl);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestMethod("POST");
            urlConn.setConnectTimeout(5000);
            urlConn.setReadTimeout(5000);
            urlConn.setDoOutput(true);
            byte[] b = params.getBytes();
            urlConn.getOutputStream().write(b, 0, b.length);
            urlConn.getOutputStream().flush();
            urlConn.getOutputStream().close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return urlConn;
    }

    public static String generatorParamString(Map<String, String> parameters) {
        StringBuffer params = new StringBuffer();
        if (parameters != null) {
            for (Iterator<String> iter = parameters.keySet().iterator(); iter.hasNext(); ) {
                String name = iter.next();
                String value = parameters.get(name);
                params.append(name + "=");
                try {
                    params.append(URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e.getMessage(), e);
                } catch (Exception e) {
                    String message = String.format("'%s'='%s'", name, value);
                    throw new RuntimeException(message, e);
                }
                if (iter.hasNext()) {
                    params.append("&");
                }
            }
        }
        return params.toString();
    }
}
