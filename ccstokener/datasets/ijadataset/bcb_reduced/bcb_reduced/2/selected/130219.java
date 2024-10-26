package com.jawise.serviceadapter.convey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;

public class HttpMessageConveyer implements MessageConveyer {

    private static final Logger logger = Logger.getLogger(HttpMessageConveyer.class);

    private long connectionTimeout = 15000;

    private long readTimeout = 15000;

    private boolean checkCertificates = true;

    private String contentType = "application/x-www-form-urlencoded";

    private SSLContext sslContext;

    private HostnameVerifier hostnameVerifier;

    @Override
    public Object convey(URL url, Object msg) throws MessageConveyingException {
        try {
            setupTrustAllManager();
            createHostNameVerifier();
            String responsemsg = doConvey(url, (String) msg);
            return responsemsg;
        } catch (Exception e) {
            throw new MessageConveyingException("1017", e.getMessage());
        }
    }

    private void setupTrustAllManager() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                return;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                return;
            }
        } };
        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
    }

    private void createHostNameVerifier() {
        hostnameVerifier = new HostnameVerifier() {

            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                    logger.debug("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
    }

    private String doConvey(URL url, String msg) throws IOException {
        BufferedReader rd = null;
        OutputStreamWriter wr = null;
        URLConnection uc = null;
        try {
            if (url == null) {
                return "";
            }
            logger.debug("destination url : " + url);
            logger.debug("conveying request  : " + msg);
            uc = url.openConnection();
            setupHostNameVerifier(url, uc);
            setupTimeouts(uc);
            uc.setDoInput(true);
            uc.setDoOutput(true);
            uc.setUseCaches(false);
            uc.setRequestProperty("Content-Type", contentType);
            wr = new OutputStreamWriter(uc.getOutputStream());
            wr.write(msg.toString());
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            StringBuffer res = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                logger.debug("response : " + line);
                if (res.length() > 0) {
                    res.append("\r\n");
                }
                res.append(line);
            }
            wr.close();
            rd.close();
            return res.toString();
        } catch (IOException e) {
            logger.error("failed to convey message to " + url + " : " + e.getMessage());
            if (wr != null) {
                wr.close();
            }
            if (rd != null) {
                rd.close();
            }
            if (uc instanceof HttpURLConnection) {
                ((HttpURLConnection) uc).disconnect();
            }
            throw e;
        }
    }

    private void setupHostNameVerifier(URL destinationurl, URLConnection urlConn) {
        if (!getCheckCertificates()) {
            HttpsURLConnection httpsConnection = null;
            if (destinationurl.getProtocol().equals("https")) {
                httpsConnection = (HttpsURLConnection) urlConn;
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConnection.setHostnameVerifier(hostnameVerifier);
            }
        }
    }

    private void setupTimeouts(URLConnection urlConn) {
        if (connectionTimeout > 0) {
            urlConn.setConnectTimeout((int) connectionTimeout);
        }
        if (readTimeout > 0) {
            urlConn.setReadTimeout((int) readTimeout);
        }
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean getCheckCertificates() {
        return checkCertificates;
    }

    public void setCheckCertificates(boolean checkCertificates) {
        this.checkCertificates = checkCertificates;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
