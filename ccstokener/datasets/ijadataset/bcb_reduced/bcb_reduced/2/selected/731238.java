package com.haliyoo.adhere.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import android.util.Log;

/**
 * HTTP通信封装
 * 
 * @author jiuhua.song
 * 
 */
public class HttpConnection {

    private static final String TAG = "HttpConnection";

    public static final Integer DEFAULT_GET_REQUEST_TIMEOUT = 20000;

    public static final Integer DEFAULT_POST_REQUEST_TIMEOUT = 30000;

    public static final int HTTP_TIMEOUT = 20000;

    private static final int MAX_BYTE = 10240;

    /**
	 * 发送HTTP get请求
	 * 
	 * @param url
	 * @param client
	 * @return String
	 * @throws IOException 
	 * @throws Exception 
	 * @throws ConnectionException
	 * @throws NetWorkException
	 */
    public static HttpData getRequest(HttpGet getMethod) throws SocketTimeoutException, SocketException, ClientProtocolException, IOException {
        String statusCode = null;
        HttpData data = new HttpData();
        try {
            getMethod.addHeader("Content-Type", "text/xml; charset=utf-8");
            getMethod.addHeader("User-Agent", "Openwave");
            printGetRequestHeader(getMethod);
            HttpClient client = new DefaultHttpClient(new BasicHttpParams());
            client.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, DEFAULT_GET_REQUEST_TIMEOUT);
            client.getParams().setIntParameter(HttpConnectionParams.SO_TIMEOUT, DEFAULT_GET_REQUEST_TIMEOUT);
            HttpResponse httpResponse = client.execute(getMethod);
            printResponseHeader(httpResponse, data);
            data.setStatusCode(statusCode);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                byte byteArray[] = retrieveInputStream(httpResponse.getEntity());
                if (byteArray != null) data.setHttpLength(Integer.toString(byteArray.length));
                data.setByteArray(byteArray);
                if (byteArray != null) {
                    Log.i(TAG, "Response Data:" + new String(byteArray, "utf-8"));
                } else {
                    Log.e(TAG, "None Response Data");
                }
            } else {
                Log.e(TAG, "httpResponse.getStatusLine().getStatusCode():" + httpResponse.getStatusLine().getStatusCode());
            }
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException();
        } catch (SocketException e) {
            throw new SocketException();
        } catch (ClientProtocolException e) {
            throw new ClientProtocolException();
        } catch (IOException e) {
            throw new IOException();
        } finally {
            getMethod.abort();
        }
        return data;
    }

    /**
	 * 输出Get请求的所有请求报文
	 * 
	 * @param getMethod
	 */
    public static void printGetRequestHeader(HttpGet getMethod) {
        URI url = getMethod.getURI();
        Log.i(TAG, "URL:" + url);
        Header header[] = getMethod.getAllHeaders();
        for (int i = 0; i < header.length; i++) {
            Log.i(TAG, header[i].getName() + " :  " + header[i].getValue());
        }
    }

    /**
	 * 输出Post请求的所有请求报文
	 * 
	 * @param postMethod
	 */
    public static void printPostRequestHeader(HttpPost postMethod) {
        URI url = postMethod.getURI();
        Log.i(TAG, "URL:" + url);
        Header header[] = postMethod.getAllHeaders();
        for (int i = 0; i < header.length; i++) {
            Log.i(TAG, header[i].getName() + " :  " + header[i].getValue());
        }
    }

    /**
	 * 输出响应报头
	 * 
	 * @param httpResponse
	 */
    public static void printResponseHeader(HttpResponse httpResponse, HttpData httpData) {
        Header header[] = httpResponse.getAllHeaders();
        for (int i = 0; i < header.length; i++) {
            String key = header[i].getName();
            String value = header[i].getValue();
            Log.i(TAG, key + " :  " + value);
            if ("DCMP-content-length".equals(key)) {
                httpData.setDcmpContentLength(value);
            } else if ("Text-content-length".equals(key)) {
                httpData.setTextContentLength(value);
            }
        }
    }

    /**
	 * 请求图片
	 * 
	 * @param getMethod
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws NetWorkException
	 */
    public static byte[] getImageResource(HttpGet getMethod) throws IllegalStateException, IOException {
        HttpResponse httpResponse = null;
        byte buffer[] = null;
        try {
            HttpConnectionParams.setConnectionTimeout(new BasicHttpParams(), HTTP_TIMEOUT);
            HttpClient client = new DefaultHttpClient(new BasicHttpParams());
            client.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, DEFAULT_POST_REQUEST_TIMEOUT);
            client.getParams().setIntParameter(HttpConnectionParams.SO_TIMEOUT, DEFAULT_POST_REQUEST_TIMEOUT);
            httpResponse = client.execute(getMethod);
            buffer = ImageInputStream(httpResponse.getEntity());
        } catch (SocketTimeoutException e) {
            throw e;
        } finally {
            getMethod.abort();
        }
        return buffer;
    }

    /**
	 *数据的读取
	 * 
	 * @param httpEntity
	 * @return String
	 */
    public static byte[] retrieveInputStream(HttpEntity httpEntity) {
        byte imgbyte[] = null;
        try {
            InputStream in = httpEntity.getContent();
            int leng = (int) httpEntity.getContentLength();
            if (leng == 0) {
                return null;
            }
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            byte buffer[] = new byte[leng];
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytestream.write(buffer, 0, count);
            }
            imgbyte = bytestream.toByteArray();
            in.close();
            bytestream.close();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return imgbyte;
    }

    protected static byte[] ImageInputStream(HttpEntity httpEntity) {
        byte imgbyte[] = null;
        try {
            InputStream in = httpEntity.getContent();
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            byte buffer[] = new byte[MAX_BYTE];
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytestream.write(buffer, 0, count);
            }
            imgbyte = bytestream.toByteArray();
            bytestream.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return imgbyte;
    }

    /**
	 * Parse the status code and throw appropriate exceptions when necessary.
	 * 
	 * @param code
	 * @param path
	 * @throws ConnectionException
	 */
    public static boolean parseStatusCode(HttpData httpdata, String code) {
        if ("0000".equals(code)) {
            httpdata.setStatusCode(code);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * 发送HTTP post请求
	 * 
	 * @param url
	 * @param client
	 * @return String
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws NetWorkException
	 * @throws NetWorkException
	 * @throws ConnectionException
	 */
    public static HttpData postRequest(HttpPost postMethod, String xml) throws ClientProtocolException, SocketException, IOException, SocketTimeoutException {
        HttpData data = new HttpData();
        try {
            postMethod.addHeader("Content-Type", "text/xml; charset=utf-8");
            postMethod.addHeader("Connection", "Keep-Alive");
            postMethod.addHeader("User-Agent", "Openwave");
            StringEntity se = new StringEntity(xml, HTTP.UTF_8);
            postMethod.setEntity(se);
            printPostRequestHeader(postMethod);
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT);
            HttpClient client = new DefaultHttpClient(httpParams);
            client.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, DEFAULT_POST_REQUEST_TIMEOUT);
            client.getParams().setIntParameter(HttpConnectionParams.SO_TIMEOUT, DEFAULT_POST_REQUEST_TIMEOUT);
            HttpResponse httpResponse = client.execute(postMethod);
            if (httpResponse == null) throw new SocketTimeoutException();
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                byte bytearray[] = ImageInputStream(httpResponse.getEntity());
                data.setByteArray(bytearray);
            } else {
                data.setStatusCode(httpResponse.getStatusLine().getStatusCode() + "");
            }
        } catch (SocketException e) {
            throw new SocketException();
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException();
        } catch (ClientProtocolException e) {
            throw new ClientProtocolException();
        } catch (IOException e) {
            throw new IOException();
        } finally {
            postMethod.abort();
        }
        return data;
    }
}
