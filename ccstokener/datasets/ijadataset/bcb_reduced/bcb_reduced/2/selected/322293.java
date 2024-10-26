package cn.edu.zucc.leyi.util.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map.Entry;
import cn.edu.zucc.leyi.util.Environment;
import cn.edu.zucc.leyi.util.StringHelper;
import android.util.Log;

public class HttpClient {

    private HttpRequest request;

    private HttpResponse response;

    private HttpURLConnection conn;

    private String params;

    private int connectTimeout;

    private int readTimeout;

    public HttpResponse execute(HttpRequest request) throws IOException {
        this.request = request;
        buildParams();
        String l = request.getUrl();
        if (request instanceof HttpGet) {
            l = l + "?" + params;
        }
        URL url = new URL(l);
        conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        buildHeader();
        if (request instanceof HttpPost) {
            sendRequest();
        }
        readResponse();
        return this.response;
    }

    private void buildHeader() {
        Iterator<Entry<String, String>> itr = request.getHeaders().entrySet().iterator();
        while (itr.hasNext()) {
            Entry<String, String> entry = itr.next();
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private void buildParams() {
        params = StringHelper.join("&", StringHelper.join("=", request.getParams()));
    }

    private void sendRequest() throws IOException {
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        PrintWriter out = new PrintWriter(conn.getOutputStream());
        Log.i("params", params);
        out.write(params);
        out.flush();
    }

    private void readResponse() throws IOException {
        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
        response = new HttpResponse();
        response.setResponseCode(conn.getResponseCode());
        response.setResponseMessage(conn.getResponseMessage());
        String encoding = "GBK";
        if (conn.getContentEncoding() != null) {
            encoding = conn.getContentEncoding();
        }
        StringBuffer sb = new StringBuffer();
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf, 0, buf.length)) != -1) {
            sb.append(new String(buf, 0, len, encoding));
        }
        response.setContent(sb.toString());
        in.close();
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public static void main(String[] args) {
        String url = "http://10.66.5.119";
        HttpPost post = new HttpPost(url);
        HttpClient hc = new HttpClient();
        hc.setConnectTimeout(Environment.HTTP_TIMEOUT);
        hc.setReadTimeout(Environment.HTTP_TIMEOUT);
        try {
            String content = hc.execute(post).getContent();
            Log.i("tag", content);
            System.out.println(content);
        } catch (Exception e) {
        }
    }
}
