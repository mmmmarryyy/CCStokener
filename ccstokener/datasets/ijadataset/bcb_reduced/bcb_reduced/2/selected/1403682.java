package ar.com.ironsoft.javaopenauth.url;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import ar.com.ironsoft.javaopenauth.exceptions.JavaOpenAuthException;
import ar.com.ironsoft.javaopenauth.utils.ContentType;
import ar.com.ironsoft.javaopenauth.utils.HttpMethod;
import ar.com.ironsoft.javaopenauth.utils.MapUtils;
import ar.com.ironsoft.javaopenauth.utils.ValidatorUtils;

public class URLService {

    public Map<String, String> fetchUrl(String url) {
        return fetchUrl(url, null);
    }

    public Map<String, String> fetchUrl(String url, ContentType contentType) {
        ValidatorUtils.validateNullOrEmpty(url);
        Map<String, String> responseParams = null;
        try {
            URL _url = new URL(url);
            URLConnection conn = _url.openConnection();
            responseParams = parseResponse(conn, contentType);
        } catch (Exception e) {
            throw new JavaOpenAuthException("Error when fetching url", e);
        }
        return responseParams;
    }

    public Map<String, String> fetchUrl(String url, HttpMethod method, Map<String, String> params, ContentType contentType, Map<String, String> headers) {
        ValidatorUtils.validateNullOrEmpty(url, method);
        Map<String, String> responseParams = null;
        try {
            String queryString = null;
            if (params != null) queryString = MapUtils.mapToQueryStringEncoded(params);
            if (method == HttpMethod.GET && queryString != null) url += "?" + queryString;
            URL _url = new URL(url);
            URLConnection conn = _url.openConnection();
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType.getContentType());
                conn.setRequestProperty("Accept", contentType.getContentType());
            }
            if (headers != null) {
                for (Entry<String, String> e : headers.entrySet()) {
                    conn.setRequestProperty(e.getKey(), e.getValue());
                }
            }
            if (method == HttpMethod.POST && queryString != null) {
                setParamsByPost(conn, queryString);
                conn.setDoOutput(true);
            }
            responseParams = parseResponse(conn, contentType);
        } catch (Exception e) {
            throw new JavaOpenAuthException("Error when fetching url", e);
        }
        return responseParams;
    }

    public Map<String, String> fetchUrl(String url, HttpMethod method, Map<String, String> params) {
        return fetchUrl(url, method, params, null, null);
    }

    public String fetchJsonUrl(String url, HttpMethod method, Map<String, String> params) {
        return fetchJsonUrl(url, method, params, null);
    }

    public String fetchJsonUrl(String url, HttpMethod method, Map<String, String> params, Map<String, String> headers) {
        Map<String, String> response = fetchUrl(url, method, params, ContentType.JSON, headers);
        if (response == null) return "";
        return response.get("json");
    }

    private void setParamsByPost(URLConnection conn, String queryString) throws Exception {
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(queryString);
        wr.flush();
    }

    private Map<String, String> parseResponse(URLConnection conn, ContentType contentType) throws IOException {
        Map<String, String> responseParams = new HashMap<String, String>();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        String response = "";
        while ((line = rd.readLine()) != null) {
            if (contentType == null) responseParams = MapUtils.queryStringToMap(line); else if (contentType.equals(ContentType.JSON)) {
                response += line;
            }
        }
        if (contentType != null && contentType.equals(ContentType.JSON)) {
            responseParams = new HashMap<String, String>();
            responseParams.put("json", response);
        }
        rd.close();
        return responseParams;
    }
}
