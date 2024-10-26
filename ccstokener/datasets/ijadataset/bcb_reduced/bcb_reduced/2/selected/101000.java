package net.tygerstar.android.negocio;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import android.util.Log;

@SuppressWarnings("rawtypes")
public class ClienteHttp {

    public static String downloadTexto(String url) {
        InputStream is = download(url);
        String arquivo = Funcoes.readString(is);
        return arquivo;
    }

    public static InputStream downloadArquivo(String url) {
        return download(url);
    }

    public static String doPost(String url, Map mapa) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            List<NameValuePair> params = getParams(mapa);
            httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpresponse = httpclient.execute(httpPost);
            HttpEntity httpentity = httpresponse.getEntity();
            if (httpentity != null) {
                InputStream is = httpentity.getContent();
                return Funcoes.readString(is);
            }
        } catch (IOException e) {
            Log.e("HttpClientImpl.doPost", e.getMessage());
        }
        return url;
    }

    private static List<NameValuePair> getParams(Map mapa) throws IOException {
        if (mapa == null || mapa.size() == 0) {
            return null;
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        Iterator it = (Iterator) mapa.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            params.add(new BasicNameValuePair(name, String.valueOf(mapa.get(name))));
        }
        return params;
    }

    private static InputStream download(String url) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse httpresponse = httpclient.execute(httpget);
            HttpEntity httpentity = httpresponse.getEntity();
            if (httpentity != null) {
                return httpentity.getContent();
            }
        } catch (Exception e) {
            Log.e("Android", e.getMessage());
        }
        return null;
    }
}
