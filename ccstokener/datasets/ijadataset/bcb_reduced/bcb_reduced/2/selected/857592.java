package com.wei.weiAndroidTest1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.struts.upload.FormFile;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class HttpRequester {

    public static String post(String actionUrl, Map<String, String> params, FormFile[] files) {
        try {
            String BOUNDARY = "---------7d4a6d158c9";
            String MULTIPART_FORM_DATA = "multipart/form-data";
            URL url = new URL(actionUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", MULTIPART_FORM_DATA + "; boundary=" + BOUNDARY);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append("--");
                sb.append(BOUNDARY);
                sb.append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                sb.append(entry.getValue());
                sb.append("\r\n");
            }
            DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
            outStream.write(sb.toString().getBytes());
            for (FormFile file : files) {
                StringBuilder split = new StringBuilder();
                split.append("--");
                split.append(BOUNDARY);
                split.append("\r\n");
                split.append("Content-Disposition: form-data;name=\"" + file.getFileName() + "\";filename=\"" + file.getFileName() + "\"\r\n");
                split.append("Content-Type: " + file.getContentType() + "\r\n\r\n");
                outStream.write(split.toString().getBytes());
                outStream.write(file.getFileData(), 0, file.getFileData().length);
                outStream.write("\r\n".getBytes());
            }
            byte[] end_data = ("--" + BOUNDARY + "--\r\n").getBytes();
            outStream.write(end_data);
            outStream.flush();
            int cah = conn.getResponseCode();
            if (cah != 200) throw new RuntimeException("request URL failed");
            InputStream is = conn.getInputStream();
            int ch;
            StringBuilder b = new StringBuilder();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            Log.i("ItcastHttpPost", b.toString());
            outStream.close();
            conn.disconnect();
            return b.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String post(String actionUrl, Map<String, String> params, FormFile file) {
        return post(actionUrl, params, new FormFile[] { file });
    }

    public static String post(String actionUrl, Map<String, String> params) {
        HttpPost httpPost = new HttpPost(actionUrl);
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(list, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(httpResponse.getEntity());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
