package com.technoetic.dof.transport.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Category;
import com.technoetic.dof.transform.ObjectDeserializer;
import com.technoetic.dof.transform.ObjectSerializer;
import com.technoetic.dof.transform.ObjectTransformerChain;
import com.technoetic.dof.transport.Request;
import com.technoetic.dof.transport.Response;
import com.technoetic.dof.transport.ResponseCallback;
import com.technoetic.dof.transport.Transport;
import com.technoetic.dof.util.AsynchronousCallAdapter;

public class HttpTransport implements Transport {

    private final Category log = Category.getInstance(getClass());

    private URL url;

    private String endPoint;

    private boolean keepAlive;

    private HttpURLConnection connection;

    private ObjectTransformerChain inputTransforms = new ObjectTransformerChain();

    private ObjectTransformerChain outputTransforms = new ObjectTransformerChain();

    private AsynchronousCallAdapter asyncAdapter;

    private Method sendSynchronousMethod;

    public HttpTransport(String endPoint) throws MalformedURLException {
        this(endPoint, false);
    }

    /** @todo Add keep alive headers for server side */
    public HttpTransport(String endPoint, boolean keepAlive) throws MalformedURLException {
        this.endPoint = endPoint;
        this.url = new URL(endPoint);
        this.keepAlive = keepAlive;
        inputTransforms.addTransform(new ObjectDeserializer());
        outputTransforms.addTransform(new ObjectSerializer());
        initializeSSL();
    }

    public synchronized Response sendSynchronousRequest(Request request) throws Throwable {
        if (!keepAlive || (keepAlive && connection == null)) {
            connection = createConnection();
        }
        byte[] dataOut = (byte[]) outputTransforms.applyTransforms(request);
        connection.setRequestMethod("POST");
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-type", "application/binary");
        connection.setRequestProperty("Content-length", Integer.toString(dataOut.length));
        connection.setDoOutput(true);
        System.setProperty("http.strictPostRedirect", Boolean.TRUE.toString());
        OutputStream out = connection.getOutputStream();
        out.write(dataOut);
        out.close();
        byte[] data = readData(connection.getInputStream());
        if (!keepAlive) {
            connection.disconnect();
            connection = null;
        }
        Response response = (Response) inputTransforms.applyTransforms(data);
        if (response.isException()) {
            throw (Throwable) response.getResult();
        }
        return response;
    }

    protected HttpURLConnection createConnection() throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private static byte[] readData(InputStream inputStream) throws IOException {
        byte[] inputBuffer = new byte[1024];
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int n = inputStream.read(inputBuffer);
        while (n > 0) {
            byteStream.write(inputBuffer, 0, n);
            n = inputStream.read(inputBuffer);
        }
        return byteStream.toByteArray();
    }

    private static class CallbackAdapter implements ResponseCallback {

        private ResponseCallback delegate;

        public CallbackAdapter(ResponseCallback delegate) {
            this.delegate = delegate;
        }

        public void handleResponse(Response response) {
            delegate.handleResponse((Response) response.getResult());
        }
    }

    public synchronized void sendAsynchronousRequest(Request request, ResponseCallback callback) {
        if (asyncAdapter == null) {
            asyncAdapter = new AsynchronousCallAdapter();
            try {
                sendSynchronousMethod = getClass().getDeclaredMethod("sendSynchronousRequest", new Class[] { Request.class });
            } catch (Exception ex) {
                log.error("error initializing async call", ex);
            }
        }
        asyncAdapter.call(this, sendSynchronousMethod, new Object[] { request }, new CallbackAdapter(callback));
    }

    public Object getEndPoint() {
        return endPoint;
    }

    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    } };

    private boolean isSSLInitialized;

    private void initializeSSL() {
        if (!isSSLInitialized) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                });
            } catch (Exception e) {
                log.error("SSL initialization error", e);
            }
            isSSLInitialized = true;
        }
    }
}
