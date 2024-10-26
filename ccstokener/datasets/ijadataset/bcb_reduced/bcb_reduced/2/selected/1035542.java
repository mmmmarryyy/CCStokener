package org.monet.backmobile.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.monet.backmobile.exception.ConnectionException;
import org.monet.backmobile.exception.NetworkException;
import org.monet.backmobile.exception.ServiceErrorException;
import org.monet.backmobile.exception.StorageException;
import org.monet.backmobile.exception.UnexpectedException;
import org.monet.backmobile.exceptions.ActionException;
import org.monet.backmobile.service.requests.LoginRequest;
import org.monet.backmobile.util.LocalStorage;
import org.monet.backmobile.util.StreamHelper;
import org.simpleframework.xml.core.Persister;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ServiceProxy {

    private static ConcurrentHashMap<String, ServiceProxy> instance = new ConcurrentHashMap<String, ServiceProxy>();

    private static String SERVICE_URL_SUFIX = "/servlet/backmobile";

    private String portId;

    private DefaultHttpClient httpClient;

    private HttpContext localContext;

    private String serviceUrl = "http://192.168.0.195:8080/monet";

    private ServiceProxy() {
        this.httpClient = setupConnection();
    }

    private ServiceProxy(String portId) {
        this.portId = portId;
        this.httpClient = setupConnection();
        CookieStore cookieStore = new BasicCookieStore();
        this.localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        this.refresh();
    }

    private DefaultHttpClient setupConnection() {
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(httpParameters, false);
        HttpConnectionParams.setConnectionTimeout(httpParameters, 4000000);
        HttpConnectionParams.setSoTimeout(httpParameters, 10000000);
        HttpConnectionParams.setSocketBufferSize(httpParameters, 8192);
        HttpClientParams.setRedirecting(httpParameters, false);
        HttpProtocolParams.setVersion(httpParameters, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParameters, "UTF-8");
        HttpProtocolParams.setUserAgent(httpParameters, "Monet Mobile Client/1.0");
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager manager = new ThreadSafeClientConnManager(httpParameters, schemeRegistry);
        return new DefaultHttpClient(manager, httpParameters);
    }

    public static synchronized ServiceProxy getInstance(String portId) {
        ServiceProxy proxy = instance.get(portId);
        if (proxy == null) {
            proxy = new ServiceProxy(portId);
            instance.put(portId, proxy);
        }
        return proxy;
    }

    public static synchronized void registerInstance(String portId, ServiceProxy proxy) {
        instance.put(portId, proxy);
        proxy.portId = portId;
    }

    public void refresh() {
    }

    public void destroy() {
        instance.remove(this.portId);
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    public HttpResponse execute(HttpGet get) throws IOException {
        return this.httpClient.execute(get);
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(Request request) throws ConnectionException {
        try {
            BasicNameValuePair actionParam = new BasicNameValuePair("op", request.getAction().name());
            HttpPost httpPost = new HttpPost(this.serviceUrl + "?" + actionParam.toString());
            ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream zipStream = new GZIPOutputStream(requestOutputStream);
            Persister persister = new Persister();
            persister.write(request, zipStream, "UTF-8");
            StreamHelper.close(zipStream);
            StreamHelper.close(requestOutputStream);
            ByteArrayInputStream instream = new ByteArrayInputStream(requestOutputStream.toByteArray());
            httpPost.setHeader("Content-Encoding", "gzip");
            httpPost.setEntity(new InputStreamEntity(instream, instream.available()));
            HttpResponse httpResponse = this.httpClient.execute(httpPost, this.localContext);
            HttpEntity entity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String reason = httpResponse.getStatusLine().getReasonPhrase();
            if (statusCode >= 200 && statusCode < 300 && entity.getContentEncoding().getValue().equals("gzip")) {
                GZIPInputStream responseStream = new GZIPInputStream(entity.getContent());
                try {
                    Response response = persister.read(Response.class, responseStream);
                    if (response.isError()) {
                        throw new ActionException(response.getError());
                    }
                    return (T) response.getResult();
                } finally {
                    StreamHelper.close(responseStream);
                }
            } else {
                throw new ConnectionException(statusCode + " " + reason);
            }
        } catch (Exception e) {
            Log.e("MonetMobile", "Error:\n\n" + e.getMessage());
            throw new ConnectionException(e.getMessage(), e);
        }
    }

    public File loadModel() throws ServiceErrorException, UnexpectedException, NetworkException, StorageException {
        File destination = null;
        FileOutputStream outputStream = null;
        InputStream contentStream = null;
        try {
            destination = LocalStorage.createTempFile();
            HttpGet httpGet = new HttpGet(this.serviceUrl);
            BasicHttpParams params = new BasicHttpParams();
            params.setParameter("op", ActionCode.LoadModel);
            httpGet.setParams(params);
            HttpResponse response = this.httpClient.execute(httpGet, this.localContext);
            HttpEntity entity = response.getEntity();
            String contentType = entity.getContentType().getValue();
            contentStream = entity.getContent();
            if (!contentType.equals("application/vnd.monet.model")) throw new UnexpectedException("Unexpected content mimeType received from server.");
            outputStream = new FileOutputStream(destination);
            copyStream(contentStream, outputStream);
        } catch (ClientProtocolException e) {
            throw new NetworkException(e.getMessage(), e);
        } catch (IOException e) {
            throw new NetworkException(e.getMessage(), e);
        } finally {
            StreamHelper.close(outputStream);
        }
        return destination;
    }

    public static ServiceProxy checkBusinessUnit(String url, String nickname, String password) throws ConnectionException {
        LoginRequest request = new LoginRequest();
        request.setUsername(nickname);
        request.setPassword(password);
        ServiceProxy proxy = new ServiceProxy();
        proxy.serviceUrl = url + SERVICE_URL_SUFIX;
        proxy.execute(request);
        return proxy;
    }

    private static void copyStream(InputStream inputStream, FileOutputStream outputStream) throws IOException, StorageException {
        byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
            try {
                outputStream.write(buffer, 0, read);
            } catch (Exception e) {
                throw new StorageException(e.getMessage(), e);
            }
        }
    }
}
