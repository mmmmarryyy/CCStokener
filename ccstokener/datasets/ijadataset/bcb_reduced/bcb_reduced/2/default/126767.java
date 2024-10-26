import java.net.URL;
import java.security.GeneralSecurityException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SecurityManagerTest {

    /**
	 * @param args
	 * @throws Exception 
	 */
    public static void main(String[] args) throws Exception {
        System.setProperty("greeting", "hello world!");
        System.out.println(System.getProperty("greeting"));
        URL url0 = new URL("http://www.google.com");
        url0.openStream();
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (GeneralSecurityException gse) {
            throw new IllegalStateException(gse.getMessage());
        }
        URL url = new URL("https://engage.ac.uk");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.getInputStream();
    }
}
