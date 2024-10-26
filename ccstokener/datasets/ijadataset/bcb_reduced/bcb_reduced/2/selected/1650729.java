package org.springframework.security.facebook;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: porter
 * Date: Jul 22, 2010
 * Time: 1:45:40 AM
 */
public class HttpUtil {

    public static Map<String, List<String>> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            String key = pair[0];
            String value = pair[1];
            List<String> values = params.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                params.put(key, values);
            }
            values.add(value);
        }
        return params;
    }

    public static String postRequest(String requestUrl) {
        return processRequest(requestUrl, HttpMethod.POST);
    }

    public static String getRequest(String requestUrl) {
        return processRequest(requestUrl, HttpMethod.GET);
    }

    private static String processRequest(String request, HttpMethod method) {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        URI uri = null;
        try {
            uri = new URI(request);
            ClientHttpRequest clientHttpRequest = simpleClientHttpRequestFactory.createRequest(uri, method);
            ClientHttpResponse response = clientHttpRequest.execute();
            InputStream bodyInputStream = response.getBody();
            String body = org.apache.commons.io.IOUtils.toString(bodyInputStream);
            return body;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateUrl(String url, Map<String, String> mParams) {
        URLCodec codec = new URLCodec();
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        boolean firstParam = true;
        for (String param : mParams.keySet()) {
            if (firstParam) {
                firstParam = false;
                sb.append('?');
            } else {
                sb.append('&');
            }
            try {
                sb.append(codec.encode(param)).append('=').append(codec.encode(mParams.get(param)));
            } catch (EncoderException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private static URLCodec codec = new URLCodec();

    public static String deocde(String encoded) {
        try {
            return codec.decode(encoded);
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return null;
    }
}
