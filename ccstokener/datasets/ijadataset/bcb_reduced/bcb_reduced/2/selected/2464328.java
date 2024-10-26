package edu.indiana.extreme.gfac.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * @author sudeivas
 *
 */
public class HTTPSClient {

    private String url;

    private String username;

    private String password;

    /**
	 * Create an anonymous class to trust all certificates.
	 */
    private X509TrustManager xtm = new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    /**
	 * Create an class to trust all hosts
	 */
    private HostnameVerifier hnv = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
	 * In this constructor we configure our system with a less stringent hostname 
	 * verifier and X509 trust manager.  This code is 
	 * executed once, and calls the static methods of HttpsURLConnection
	 */
    public HTTPSClient() {
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
    }

    /**
	 * 
	 * This function is called periodically, the important thing 
	 * to note here is that there is no special code that needs to 
	 * be added to deal with a "HTTPS" URL.  All of the trust 
	 * management, verification, is handled by the HttpsURLConnection.
	 * @param url
	 * @param username
	 * @param password
	 * @return
	 * @throws Exception
	 */
    public String getTextFromURL(String url, String username, String password) throws Exception {
        this.url = url;
        this.username = username;
        this.password = password;
        String content = "";
        try {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getUsername(), getPassword().toCharArray());
                }
            });
            URLConnection urlCon = (new URL(url)).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                content += line + "\n";
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
	 * 
	 * @return url
	 */
    public String getUrl() {
        return url;
    }

    /**
	 * 
	 * @param url
	 */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
	 * 
	 * @return username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * 
	 * @param username
	 */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
	 * 
	 * @return password
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * 
	 * @param password
	 */
    public void setPassword(String password) {
        this.password = password;
    }
}
