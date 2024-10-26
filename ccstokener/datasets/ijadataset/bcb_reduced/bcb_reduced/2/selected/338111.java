package javacommon.util.http.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 模拟 HTTP PET POST 连接.
 * @author bzq
 *
 */
public class HttpPostConnect {

    private static Log log = LogFactory.getLog(HttpPostConnect.class);

    /**
	 * 
	 * @param paraMap<String, String> 要通过POST发送的参数集合
	 * @param urlStr 请求的HTTP URL
	 * @return 
	 * @throws IOException
	 */
    public static String urlPost(Map<String, String> paraMap, String urlStr) throws IOException {
        String strParam = "";
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            strParam = strParam + (entry.getKey() + "=" + entry.getValue()) + "&";
        }
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "utf-8");
        out.write(strParam);
        out.flush();
        out.close();
        String sCurrentLine;
        String sTotalString;
        sCurrentLine = "";
        sTotalString = "";
        InputStream l_urlStream;
        l_urlStream = connection.getInputStream();
        BufferedReader l_reader = new BufferedReader(new InputStreamReader(l_urlStream));
        while ((sCurrentLine = l_reader.readLine()) != null) {
            sTotalString += sCurrentLine + "\r\n";
        }
        System.out.println(sTotalString);
        return sTotalString;
    }

    /** 
     * 执行一个HTTP POST请求，返回请求响应的HTML 
     * 
     * @param url         请求的URL地址 
     * @param params    请求的查询参数,可以为null 
     * @param charset 字符集 
     * @param pretty    是否美化 
     * @return 返回请求响应的HTML 
     */
    public static String httpMethodPost(String url, Map<String, String> params, String charset, boolean pretty) {
        StringBuffer response = new StringBuffer();
        HttpClient client = new HttpClient();
        HttpMethod method = new PostMethod(url);
        if (params != null) {
            HttpMethodParams p = new HttpMethodParams();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                p.setParameter(entry.getKey(), entry.getValue());
            }
            method.setParams(p);
        }
        try {
            client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (pretty) response.append(line).append(System.getProperty("line.separator")); else response.append(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            log.error("执行HTTP Post请求" + url + "时，发生异常！", e);
        } finally {
            method.releaseConnection();
        }
        return response.toString();
    }

    private static final String CHARACTER_ENCODING = "UTF-8";

    private static final String PATH_SIGN = "/";

    private static final String METHOD_POST = "POST";

    private static final String METHOD_GET = "GET";

    private static final String CONTENT_TYPE = "Content-Type";

    /**
	 * 以POST方式向指定地址发送数据包请求,并取得返回的数据包
	 * 
	 * @param urlString
	 * @param requestData
	 * @return 返回数据包
	 * @throws Exception
	 */
    public static byte[] requestPost(String urlString, byte[] requestData) throws Exception {
        Properties requestProperties = new Properties();
        requestProperties.setProperty(CONTENT_TYPE, "application/octet-stream; charset=utf-8");
        return requestPost(urlString, requestData, requestProperties);
    }

    /**
	 * 以POST方式向指定地址发送数据包请求,并取得返回的数据包
	 * 
	 * @param urlString
	 * @param requestData
	 * @param requestProperties
	 * @return 返回数据包
	 * @throws Exception
	 */
    public static byte[] requestPost(String urlString, byte[] requestData, Properties requestProperties) throws Exception {
        byte[] responseData = null;
        HttpURLConnection con = null;
        try {
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();
            if ((requestProperties != null) && (requestProperties.size() > 0)) {
                for (Map.Entry<Object, Object> entry : requestProperties.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    String value = String.valueOf(entry.getValue());
                    con.setRequestProperty(key, value);
                }
                con.setRequestProperty("keyxx", "valuexx");
            }
            con.setRequestMethod(METHOD_POST);
            con.setDoInput(true);
            con.setDoOutput(true);
            if (requestData != null) {
                DataOutputStream dos = new DataOutputStream(con.getOutputStream());
                dos.write(requestData);
                dos.flush();
                dos.close();
            }
            int length = con.getContentLength();
            if (length != -1) {
                DataInputStream dis = new DataInputStream(con.getInputStream());
                responseData = new byte[length];
                dis.readFully(responseData);
                dis.close();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (con != null) {
                con.disconnect();
                con = null;
            }
        }
        return responseData;
    }

    /**
	 * 以POST方式向指定地址提交表单<br>
	 * arg0=urlencode(value0)&arg1=urlencode(value1)
	 * 
	 * @param urlString
	 * @param formProperties
	 * @return 返回数据包
	 * @throws Exception
	 */
    public static byte[] requestPostForm(String urlString, Properties formProperties) throws Exception {
        return requestPostForm(urlString, formProperties, null);
    }

    /**
	 * 以POST方式向指定地址提交表单<br>
	 * arg0=urlencode(value0)&arg1=urlencode(value1)
	 * 
	 * @param urlString
	 * @param formProperties
	 * @param requestProperties
	 * @return 返回数据包
	 * @throws Exception
	 */
    public static byte[] requestPostForm(String urlString, Properties formProperties, Properties requestProperties) throws Exception {
        requestProperties.setProperty(CHARACTER_ENCODING, "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        if ((formProperties != null) && (formProperties.size() > 0)) {
            for (Map.Entry<Object, Object> entry : formProperties.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String value = String.valueOf(entry.getValue());
                sb.append(key);
                sb.append("=");
                sb.append(encode(value));
                sb.append("&");
            }
        }
        String str = sb.toString();
        str = str.substring(0, (str.length() - 1));
        return requestPost(urlString, str.getBytes(CHARACTER_ENCODING), requestProperties);
    }

    /**
	 * url解码
	 * 
	 * @param str
	 * @return 解码后的字符串,当异常时返回原始字符串。
	 */
    private static String decode(String url) {
        try {
            return URLDecoder.decode(url, CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            return url;
        }
    }

    /**
	 * url编码
	 * 
	 * @param str
	 * @return 编码后的字符串,当异常时返回原始字符串。
	 */
    private static String encode(String url) {
        try {
            return URLEncoder.encode(url, CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            return url;
        }
    }

    public String simplePost(String address, Map<String, String> paraMap) throws IOException {
        HttpClient client = new HttpClient();
        client.getHostConfiguration().setHost("www.imobile.com.cn", 80, "http");
        HttpMethod method = getPostMethod(address, paraMap);
        client.executeMethod(method);
        String response = new String(method.getResponseBodyAsString().getBytes("ISO8859-1"));
        method.releaseConnection();
        return response;
    }

    /**
	 * 
	 * 使用POST方式提交数据
	 * 
	 * @return
	 * 
	 */
    private static HttpMethod getPostMethod(String address, Map<String, String> paraMap) {
        PostMethod post = new PostMethod(address);
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            NameValuePair simcard = new NameValuePair(entry.getKey(), entry.getValue());
            post.setRequestBody(new NameValuePair[] { simcard });
        }
        return post;
    }
}
