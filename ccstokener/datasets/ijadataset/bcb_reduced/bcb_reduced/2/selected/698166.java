package org.openliberty.wsc.test.old;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * This class loads some prebuilt xml files and shoves them down the pipe
 * to Conor's AS.
 * 
 * @author asa
 *
 */
public class TestAuthConor {

    private static X509TrustManager xtm = new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    private static HostnameVerifier hnv = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static void main(String[] args) throws Exception {
        TestAuthConor ps = new TestAuthConor();
    }

    public TestAuthConor() {
        try {
            initAndGo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAndGo() throws Exception {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            X509TrustManager[] xtmArray = new X509TrustManager[] { xtm };
            sslContext.init(null, xtmArray, new java.security.SecureRandom());
        } catch (GeneralSecurityException gse) {
            gse.printStackTrace();
        }
        if (sslContext != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }
        HttpsURLConnection.setDefaultHostnameVerifier(hnv);
        String strURL = "https://i-idp.liberty-iop.org:8481/axis/services/LibertyAS2";
        String response1 = sendSOAPRequestViaHttpURLConnection(strURL, "cahill_auth1.xml");
        String response2 = sendSOAPRequestViaHttpURLConnection(strURL, "cahill_auth2.xml");
        System.out.println(response2);
    }

    public String sendSOAPRequestViaHttpURLConnection(String urlString, String inputFileName) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("SOAPAction", "");
        conn.setInstanceFollowRedirects(false);
        conn.setUseCaches(false);
        DataOutputStream printout = new DataOutputStream(conn.getOutputStream());
        StringBuffer xmlBuffer = new StringBuffer();
        {
            BufferedReader xmlReader = new BufferedReader(new FileReader(inputFileName));
            String tmp;
            while (null != ((tmp = xmlReader.readLine()))) {
                xmlBuffer.append(tmp);
            }
            xmlReader.close();
        }
        printout.writeBytes(xmlBuffer.toString());
        printout.flush();
        printout.close();
        BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer buff = new StringBuffer();
        String str2;
        while (null != ((str2 = input.readLine()))) {
            buff.append(str2);
        }
        input.close();
        return buff.toString();
    }
}
