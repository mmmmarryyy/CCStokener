package com.nerevar.andricq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

/***
 * Наследуемая сущность, содержит методы для работы с сетью
 */
public class NetworkEntity {

    public static final String SERVER = "http://andricq.nerevar.com/api.php";

    /**
	 * Отправляет HTTP POST сообщение на удаленный сервер
	 * @param host - удаленный сервер
	 * @param nameValuePairs - POST переменные в формате имя-значение
	 */
    protected static String postData(String host, List<NameValuePair> nameValuePairs) throws ClientProtocolException, IOException {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 3000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 5000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        HttpClient httpclient = new DefaultHttpClient(httpParameters);
        HttpPost httppost = new HttpPost(host);
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        String resp = EntityUtils.toString(resEntity, HTTP.UTF_8);
        String respEncoded = new String(resp.getBytes("Cp1251"), HTTP.UTF_8);
        return respEncoded;
    }

    /**
	 * Парсит входящую строку с сервера
	 * @param response - строка json
	 * @return - результирующий ассоциативный массив или null
	 */
    protected static HashMap<String, String> parseJSON(String response) throws JSONException {
        HashMap<String, String> out = null;
        JSONObject json = new JSONObject(response);
        out = new HashMap<String, String>();
        Iterator jk = json.keys();
        while (jk.hasNext()) {
            String key = (String) jk.next();
            out.put(key, json.getString(key));
        }
        return out;
    }
}
