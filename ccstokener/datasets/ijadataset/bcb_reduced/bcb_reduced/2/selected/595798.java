package domain.connection;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Vector;

public class HTTPS {

    static Vector<String> cookieVector = new Vector<String>();

    private static SSLSocketFactory sslSocketFactory;

    public boolean useLogin;

    public String loginHost;

    public String loginData;

    private static String readInput(HttpsURLConnection conn) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        String input = "";
        readCookies(conn);
        while ((line = rd.readLine()) != null) {
            input += line + "\n";
        }
        rd.close();
        return input;
    }

    public static void clearCookie() {
        cookieVector = new Vector<String>();
    }

    private static String sendOutput(HttpsURLConnection conn, String data) throws IOException {
        conn.setDoOutput(true);
        conn.setDoInput(true);
        writeCookies(conn);
        conn.setSSLSocketFactory(sslSocketFactory);
        conn.connect();
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();
        return readInput(conn);
    }

    private static void readCookies(HttpsURLConnection conn) {
        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase("Set-Cookie")) {
                String fullCookie = conn.getHeaderField(i).split("; ")[0];
                addCookie(fullCookie);
            }
        }
    }

    private static void writeCookies(HttpsURLConnection conn) {
        String strCookies = "";
        if (cookieVector.size() != 0) {
            for (String aCookie : cookieVector) {
                strCookies += aCookie + "; ";
            }
            conn.setRequestProperty("Cookie", strCookies);
        }
    }

    public Vector<String> getCookie() {
        return cookieVector;
    }

    private static int cookieExist(String cookie) {
        String[] cookieSplit = cookie.split("=");
        int i = 0;
        for (String aCookie : cookieVector) {
            if (aCookie.startsWith(cookieSplit[0])) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static String doRequest(String strUrl, String request) throws SocketTimeoutException {
        String respons = "";
        if (sslSocketFactory == null) {
            sslSocketFactory = initSSL();
        }
        try {
            URL url = new URL(strUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            respons = sendOutput(conn, request);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return respons;
    }

    public static SSLSocketFactory initSSL() {
        SSLSocketFactory sslsf = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            } };
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            sslsf = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslsf;
    }

    public static void addCookie(String cookieValue) {
        int i;
        if ((i = cookieExist(cookieValue)) == -1) {
            cookieVector.add(cookieValue);
        } else {
            cookieVector.set(i, cookieValue);
        }
    }

    public static void setProxy(String host, int port) {
        proxyDaten(host, port);
    }

    public static void setProxy(String host, int port, String username, String passwort) {
        proxyDaten(host, port);
        Authenticator.setDefault(new ProxyAuth(username, passwort));
    }

    private static void proxyDaten(String host, int port) {
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", String.valueOf(port));
    }

    private static class ProxyAuth extends Authenticator {

        private String username;

        private String passwort;

        public ProxyAuth(String username, String passwort) {
            this.username = username;
            this.passwort = passwort;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication(this.username, this.passwort.toCharArray()));
        }
    }

    public static void clearProxy() {
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
    }
}
