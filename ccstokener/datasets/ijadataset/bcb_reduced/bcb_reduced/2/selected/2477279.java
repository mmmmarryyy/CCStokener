package org.igeek.atomqq.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.http.util.ByteArrayBuffer;
import org.igeek.atomqq.net.Response.Response_TYPE;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author 作者 E-mail:hangxin1940@gmail.com
 * @version 创建时间：2012-1-9 下午08:59:09
 * 全局网络连接
 */
public class HttpConnection {

    private static CookieManager cookieManager = CookieManager.getInstance();

    public enum Request_TYPE {

        GET, POST
    }

    private Request_TYPE type;

    private URL url;

    private byte[] bytes;

    private Response response;

    private HttpURLConnection conn;

    private String referer;

    /**
	 * 
	 * @param url 提交类型
	 * @param params 提交地址
	 * @param type 提交参数
	 */
    public HttpConnection(URL url, Request_TYPE type) {
        this.type = type;
        this.url = url;
    }

    /**
	 * 设置跳转来的地址
	 * @param referer
	 */
    public void setReferer(String referer) {
        this.referer = referer;
    }

    /**
	 * 连接
	 * @throws IOException
	 * @throws JSONException 
	 */
    public void connect() throws IOException, JSONException {
        String params = "";
        if (Request_TYPE.POST == type) {
            String surl = url.toString();
            int flag = surl.indexOf("?");
            String method = surl.substring(0, flag);
            params = surl.substring(flag + 1, surl.length());
            url = new URL(method);
        }
        conn = (HttpURLConnection) url.openConnection();
        if (null != referer) conn.setRequestProperty("Referer", referer);
        conn.setRequestProperty("Host", url.getHost());
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cookie", cookieManager.getCookies());
        conn.setConnectTimeout(3000);
        switch(type) {
            case GET:
                conn.setRequestMethod("GET");
                conn.connect();
                break;
            case POST:
                if (!"".equals(params)) {
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(params);
                    osw.flush();
                    osw.close();
                    os.close();
                }
                break;
        }
        List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
        cookieManager.setCookies(cookies);
        response = new Response();
        Map<String, List<String>> fields = conn.getHeaderFields();
        List<String> types = fields.get("Content-Type");
        if (null == types) throw new IOException();
        String scontentType = types.get(0);
        if (scontentType.indexOf("plain") != -1 || scontentType.indexOf("json") != -1) {
            InputStream ins = conn.getInputStream();
            BufferedInputStream instream = new BufferedInputStream(ins);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = instream.read()) != -1) {
                baf.append((byte) current);
            }
            bytes = baf.toByteArray();
            instream.close();
            ins.close();
            response.setRType(Response_TYPE.JSON);
            String s = new String(bytes);
            JSONObject json = new JSONObject(s);
            response.setJsonObj(json);
            response.setText(s);
        } else if (scontentType.indexOf("image") != -1) {
            response.setRType(Response_TYPE.STREAM);
            response.setStream(conn.getInputStream());
        } else if (scontentType.indexOf("html") != -1 || scontentType.indexOf("javascript") != -1) {
            InputStream ins = conn.getInputStream();
            BufferedInputStream instream = new BufferedInputStream(ins);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = instream.read()) != -1) {
                baf.append((byte) current);
            }
            bytes = baf.toByteArray();
            instream.close();
            ins.close();
            response.setRType(Response_TYPE.TEXT);
            String s = new String(bytes);
            response.setText(s);
        }
    }

    /**
	 * 获取返回类型
	 * @return
	 */
    public Response getResponse() {
        return response;
    }

    /**
	 * 获取返回字节
	 * @return
	 */
    public byte[] getBytes() {
        return bytes;
    }

    public void disconnect() {
        if (conn != null) conn.disconnect();
    }

    public static CookieManager getCookieManager() {
        return cookieManager;
    }
}
