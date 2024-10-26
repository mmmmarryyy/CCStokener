package com.apelon.soap;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author jweis
 */
public class SoapMessageFactory {

    public static SoapMessage createSoapFaultMessage(String code, String reason, String version) {
        if (version.equalsIgnoreCase(SoapMessage.V1_1)) return new SoapFaultMessage1_1(code, reason); else return new SoapFaultMessage1_2(code, reason);
    }

    public static SoapMessage createSoapMessageWithBodyContent(String body, String version) {
        SoapMessage sm = new SoapMessage();
        sm.setVersion(version);
        sm.fSoapBodyContent = body;
        return sm;
    }

    public static SoapMessage createSoapMessage(InputStream is, int content_length, String encoding) throws IOException {
        byte[] bytes = new byte[content_length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        return new SoapMessage(new String(bytes, encoding));
    }

    public static SoapMessage sendSoapMessage(String serviceURL, SoapMessage sm) throws Exception {
        if (serviceURL.toLowerCase().indexOf("https") > -1) {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            } };
            HostnameVerifier hv = null;
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            hv = new HostnameVerifier() {

                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        }
        URL url = new URL(serviceURL);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset='UTF-8'");
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(sm.toString());
        wr.flush();
        InputStream inputStream = conn.getInputStream();
        int length = conn.getContentLength();
        byte[] bytes;
        if (length == -1) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int c;
            while (true) {
                c = inputStream.read();
                if (c == -1) break;
                baos.write(c);
            }
            bytes = baos.toByteArray();
        } else {
            bytes = new byte[length];
            inputStream.read(bytes);
        }
        SoapMessage return_soap = new SoapMessage(new String(bytes));
        String sm_text = return_soap.getSoapBodyContent();
        if (sm_text.indexOf(":Fault") > -1) {
            if (sm.getVersion().equalsIgnoreCase(SoapMessage.V1_1)) return_soap = new SoapFaultMessage1_1(return_soap); else return_soap = new SoapFaultMessage1_2(return_soap);
        }
        return return_soap;
    }
}
