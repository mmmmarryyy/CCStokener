package gawky.comm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class HTTPClient {

    public HTTPClient() {
        setTrustAll();
    }

    public void setTrustAll() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier hv = new HostnameVerifier() {

                public boolean verify(String urlHostName, SSLSession session) {
                    System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) throws Exception {
        HTTPClient client = new HTTPClient();
        String res = client.sendPOST("https://www.netbank.de", "test");
        System.out.println(res);
    }

    public String sendGET(String url) {
        String result = "";
        return result;
    }

    public String sendPOST(String link, String data) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(link);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    public static void upper() throws Exception {
        File input = new File("dateiname");
        PostMethod post = new PostMethod("url");
        post.setRequestBody(new FileInputStream(input));
        if (input.length() < Integer.MAX_VALUE) post.setRequestContentLength((int) input.length()); else post.setRequestContentLength(EntityEnclosingMethod.CONTENT_LENGTH_CHUNKED);
        post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859�1");
        HttpClient httpclient = new HttpClient();
        httpclient.executeMethod(post);
        post.releaseConnection();
        URL url = new URL("https://www.amazon.de/");
        URLConnection conn = url.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
        rd.close();
    }
}
