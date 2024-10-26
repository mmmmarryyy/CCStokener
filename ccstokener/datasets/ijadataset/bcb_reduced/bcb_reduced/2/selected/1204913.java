package org.com.cnc.common.android.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.com.cnc.common.android.parameter.RequestParameter;
import org.com.cnc.common.android.request.CommonRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import android.util.Log;

public class CommonService {

    public static final String MESSAGE = "Message";

    public static final String ERROR = "Error";

    public static final String STATUS = "Status";

    @SuppressWarnings("deprecation")
    public static String getStringByGet(CommonRequest request) {
        try {
            HttpParams httpParameters = new BasicHttpParams();
            int timeout = request.getTimeout();
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
            HttpConnectionParams.setSoTimeout(httpParameters, timeout);
            HttpClient httpclient = new DefaultHttpClient(httpParameters);
            String url = request.getUrl();
            for (int i = 0; i < request.sizeListParameter(); i++) {
                RequestParameter parameter = request.getRequestParameter(i);
                String key = parameter.getKey();
                String value = URLEncoder.encode(parameter.getValue());
                if (i == 0) {
                    url += "?" + key + "=" + value;
                } else {
                    url += "&" + key + "=" + value;
                }
            }
            HttpGet httppost = new HttpGet(url);
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            InputStreamReader input = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(input);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            return "{\"status\":\"false\", \"message\":\"Connect time out!\",\"error\":\"Connect time out!\"}";
        }
    }

    @SuppressWarnings("deprecation")
    public static String getStringByPOST(CommonRequest request) {
        try {
            int timeout = request.getTimeout();
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
            HttpConnectionParams.setSoTimeout(httpParameters, timeout);
            HttpClient httpclient = new DefaultHttpClient(httpParameters);
            HttpPost httppost = new HttpPost(request.getUrl());
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            for (int i = 0; i < request.sizeListParameter(); i++) {
                RequestParameter parameter = request.getRequestParameter(i);
                String key = parameter.getKey();
                String value = URLEncoder.encode(parameter.getValue());
                BasicNameValuePair pair = new BasicNameValuePair(key, value);
                nameValuePairs.add(pair);
            }
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            InputStreamReader input = new InputStreamReader(is, "iso-8859-1");
            BufferedReader reader = new BufferedReader(input, 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            return "{\"status\":\"false\", \"message\":\"Connect time out!\",\"error\":\"Connect time out!\"}";
        }
    }

    public static JSONObject getJSON(CommonRequest request) {
        String str = null;
        if (request.isGet()) {
            str = getStringByGet(request);
        } else {
            str = getStringByPOST(request);
        }
        Log.i("DATA", str);
        try {
            return new JSONObject(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONArray getJSONArray(CommonRequest request) {
        String str = null;
        if (request.isGet()) {
            str = getStringByGet(request);
        } else {
            str = getStringByPOST(request);
        }
        Log.i("DATA", str);
        try {
            return new JSONArray(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject getJSONObjectByKey(JSONObject jsonObject, String key) {
        try {
            return jsonObject.getJSONObject(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static final Document getDocument(CommonRequest request) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            String str = null;
            if (request.isGet()) {
                str = getStringByGet(request);
            } else {
                str = getStringByPOST(request);
            }
            is.setCharacterStream(new StringReader(str));
            return db.parse(is);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getString(JSONObject object, String key) {
        try {
            return object.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getValue(Element item, String str) {
        try {
            NodeList n = item.getElementsByTagName(str);
            return getElementValue(n.item(0));
        } catch (Exception e) {
            return "";
        }
    }

    public static final String getElementValue(Node elem) {
        Node kid;
        if (elem != null) {
            if (elem.hasChildNodes()) {
                for (kid = elem.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
                    if (kid.getNodeType() == Node.TEXT_NODE) {
                        return kid.getNodeValue();
                    }
                }
            }
        }
        return "";
    }

    public static void main(String[] args) {
    }
}
