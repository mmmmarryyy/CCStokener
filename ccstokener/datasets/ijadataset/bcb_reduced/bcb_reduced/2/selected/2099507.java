package hk.hku.cs.msc.ules;

import hk.hku.cs.msc.ules.dto.RequestData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class RequestSender extends Thread {

    public static final String TAG = "RequestSender";

    private Context context;

    private Handler handler;

    RequestSender(Context context) {
        this.context = context;
        handler = new RequestSenderHandler(this);
    }

    public Handler getHandler() {
        return this.handler;
    }

    public Context getContext() {
        return this.context;
    }

    protected String requestRandomKey(RequestData data) {
        return requestRandomKey(data.getUrl(), data.getUsername(), data.getPassword());
    }

    protected String requestMountKey(RequestData data) {
        return requestMountKey(data.getUrl(), data.getUsername(), data.getRandomKey());
    }

    private String requestRandomKey() {
        String url = "http://192.168.1.16:8080/ufle/getmountkey.jsp?username=kevin&sms=374513";
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        String line = null;
        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                line = readResponseFromRandomKey(httpResponse);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Could not establish a HTTP connection to the server or could not get a response properly from the server.", e);
            e.printStackTrace();
        }
        return line;
    }

    private String requestRandomKey(String url, String username, String password) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("from", "mobile"));
        String line = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                line = readResponseFromRandomKey(httpResponse);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Could not establish a HTTP connection to the server or could not get a response properly from the server.", e);
            e.printStackTrace();
        } finally {
        }
        return line;
    }

    private String requestMountKey(String url, String username, String randomKey) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("sms", randomKey));
        String line = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                line = readResponseFromMountKey(httpResponse);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Could not establish a HTTP connection to the server or could not get a response properly from the server.", e);
            e.printStackTrace();
        }
        String mountkey = line;
        return mountkey;
    }

    private String readResponseFromRandomKey(HttpResponse httpResponse) {
        String line = null;
        try {
            InputStream content = httpResponse.getEntity().getContent();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));
            line = in.readLine();
            in.close();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    private String readResponseFromMountKey(HttpResponse httpResponse) {
        String line = null;
        try {
            InputStream content = httpResponse.getEntity().getContent();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));
            String[] str = in.readLine().split("=");
            if (str.length > 1) {
                line = str[1];
            }
            in.close();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }
}
