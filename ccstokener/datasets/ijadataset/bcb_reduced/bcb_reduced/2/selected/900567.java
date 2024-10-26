package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import constants.CharacterConstants;

public abstract class WebHelper {

    private static final String USER_AGENT_NAME = "http.useragent";

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; pl-PL; rv:1.8.0.7) Gecko/20060921 Ubuntu/dapper-security Firefox/1.5.0.7";

    private enum Method {

        GET, POST
    }

    ;

    private static HttpMethod fillForm(String formUri, Map<String, String> parameters, String userAgent, Method method) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        HttpMethod httpMethod;
        NameValuePair[] pairs;
        if (parameters != null && parameters.size() > 0) {
            pairs = new NameValuePair[parameters.size()];
            Set<String> keys = parameters.keySet();
            int i = 0;
            for (String key : keys) {
                pairs[i++] = new NameValuePair(key, parameters.get(key));
            }
        } else {
            pairs = new NameValuePair[0];
        }
        if (method == Method.POST) {
            PostMethod postMethod = new PostMethod(formUri);
            postMethod.addParameters(pairs);
            httpMethod = postMethod;
        } else {
            GetMethod getMethod = new GetMethod(formUri);
            getMethod.setQueryString(pairs);
            httpMethod = getMethod;
        }
        httpMethod.getParams().setParameter(USER_AGENT_NAME, userAgent);
        int statusCode = client.executeMethod(httpMethod);
        if (statusCode != HttpStatus.SC_OK) {
            System.err.println("Method failed: " + httpMethod.getStatusLine());
        }
        return httpMethod;
    }

    private static String fillForm(String formUri, Map<String, String> parameters, String encoding, String userAgent, Method method) throws UnsupportedEncodingException, IOException {
        HttpMethod httpMethod = fillForm(formUri, parameters, userAgent, method);
        StringBuilder builder = new StringBuilder();
        byte[] bs = new byte[1024];
        InputStream inputStream = httpMethod.getResponseBodyAsStream();
        do {
            inputStream.read(bs);
            builder.append(new String(bs, encoding));
        } while (inputStream.read(bs) != -1);
        return builder.toString();
    }

    public static String getRedirectedQueryString(String formUri, Map<String, String> parameters) throws UnsupportedEncodingException, IOException {
        HttpMethod httpMethod = fillForm(formUri, parameters, DEFAULT_USER_AGENT, Method.GET);
        return httpMethod.getQueryString();
    }

    public static String fillFormPost(String formUrl, Map<String, String> parameters) throws UnsupportedEncodingException, IOException {
        return fillForm(formUrl, parameters, CharacterConstants.ENCODING_ISO8859_2, DEFAULT_USER_AGENT, Method.POST);
    }

    public static String fillFormPost(String formUrl, Map<String, String> parameters, String siteEncoding) throws UnsupportedEncodingException, IOException {
        return fillForm(formUrl, parameters, siteEncoding, DEFAULT_USER_AGENT, Method.POST);
    }

    public static String fillFormGet(String formUrl, Map<String, String> parameters) throws UnsupportedEncodingException, IOException {
        return fillForm(formUrl, parameters, CharacterConstants.ENCODING_ISO8859_2, DEFAULT_USER_AGENT, Method.GET);
    }

    public static String createQueryString(Map<String, String> parameters, String encoding) throws UnsupportedEncodingException {
        String path = null;
        for (String key : parameters.keySet()) {
            if (path == null) path = URLEncoder.encode(key, encoding) + "=" + URLEncoder.encode(parameters.get(key), encoding); else path += "&" + URLEncoder.encode(key, encoding) + "=" + URLEncoder.encode(parameters.get(key), encoding);
        }
        return path;
    }

    public static Map<String, List<String>> getHttpHeaderFields(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.connect();
        Map<String, List<String>> output = connection.getHeaderFields();
        return output;
    }

    public static Map<String, String> parseQueryString(String queryString) {
        String[] parameters = queryString.split("&");
        Map<String, String> output = new HashMap<String, String>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            String[] parameter = parameters[i].split("=");
            output.put(parameter[0], parameter[1]);
        }
        return output;
    }

    public static String loadSite(String spec) throws IOException {
        URL url = new URL(spec);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String output = "";
        String str;
        while ((str = in.readLine()) != null) {
            output += str + "\n";
        }
        in.close();
        return output;
    }
}
