import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 
 * @author <a href="mailto:kalda.kaido@gmail.com">Kaido Kalda</a>
 */
public class SSLTest {

    private static final Pattern cnPattern = Pattern.compile("(,|^)CN=([^,]+)(,|$)");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java SSLTest <host_to_test> <ssl_port>");
            return;
        }
        System.out.println("DNS cache policy: " + toText(sun.net.InetAddressCachePolicy.get()));
        initSSL();
        String host = args[0];
        String port = args[1];
        System.out.println("\n--> Collecting information about host: " + host);
        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (int i = 0; i < addresses.length; i++) {
            InetAddress inetAddress = addresses[i];
            System.out.println("  > Resolved address (" + (i + 1) + " of " + addresses.length + "): " + inetAddress);
            try {
                collectInfo(new URL("https://" + inetAddress.getHostAddress() + ":" + port + "/"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("\n--> Making connection via host name");
        collectInfo(new URL("https://" + host + ":" + port + "/"));
        System.out.println("done!");
    }

    private static void collectInfo(URL url) throws IOException {
        System.out.println("\tComposed url>\t" + url);
        URLConnection connection = url.openConnection();
        System.out.println("\tConection>\t" + connection);
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
            httpsURLConnection.setHostnameVerifier(standardVerifier);
            InputStream inputStream = null;
            try {
                inputStream = httpsURLConnection.getInputStream();
                byte[] b = new byte[30];
                int len = inputStream.read(b);
                System.out.println("\tresult>\t\t\t\tOK - initial read returned: " + new String(b, 0, len).replaceAll("\n", "<n>"));
            } catch (Exception e) {
                System.out.println("\tresult>\t\t\t\t(" + e.getClass().getName() + ") - " + e.getMessage());
            }
        }
    }

    private static void initSSL() throws Exception {
        System.out.print("Initializing SSL: ");
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                System.out.println("\t\t: AcceptedIssuers: accept all");
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                System.out.println("\t\t: ClientTrusted (" + authType + "): trust all");
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                System.out.println("\t\t: ServerTrusted(" + authType + "): trust all");
            }
        } };
        SSLContext httpsContext = SSLContext.getInstance("SSL");
        httpsContext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(new DelecateSSLSocketFactory(httpsContext));
        System.out.println("HTTPS initialized.");
    }

    private static String toText(int i) {
        switch(i) {
            case -1:
                return "forever";
            default:
                return String.valueOf(i) + " seconds";
        }
    }

    static class DelecateSSLSocketFactory extends SSLSocketFactory {

        private SSLSocketFactory context;

        public DelecateSSLSocketFactory(SSLContext httpsContext) {
            context = httpsContext.getSocketFactory();
        }

        @Override
        public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
            System.out.println("\t\t: " + socket + ", " + s + ", " + i + ", " + flag);
            return context.createSocket(socket, s, i, flag);
        }

        @Override
        public String[] getDefaultCipherSuites() {
            String[] cipherSuites = context.getDefaultCipherSuites();
            System.out.print("\t\t: ");
            for (String string : cipherSuites) {
                System.out.print(string + ", ");
            }
            System.out.println();
            return cipherSuites;
        }

        @Override
        public String[] getSupportedCipherSuites() {
            String[] cipherSuites = context.getSupportedCipherSuites();
            System.out.print("\t\t: ");
            for (String string : cipherSuites) {
                System.out.print(string + ", ");
            }
            System.out.println();
            return cipherSuites;
        }

        @Override
        public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
            System.out.println("\t\t: " + ", " + s + ", " + i);
            return context.createSocket(s, i);
        }

        @Override
        public Socket createSocket(InetAddress inetaddress, int i) throws IOException {
            System.out.println("\t\t: " + inetaddress.toString() + ", " + i);
            return context.createSocket(inetaddress, i);
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress inetaddress, int j) throws IOException, UnknownHostException {
            System.out.println("\t\t: " + s + ", " + i + ", " + inetaddress + ", " + j);
            return context.createSocket(s, i, inetaddress, j);
        }

        @Override
        public Socket createSocket(InetAddress inetaddress, int i, InetAddress inetaddress1, int j) throws IOException {
            System.out.println("\t\t: " + inetaddress.toString() + ", " + i + ", " + inetaddress1 + ", " + j);
            return context.createSocket(inetaddress, i, inetaddress1, j);
        }
    }

    public static final HostnameVerifier standardVerifier = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            System.out.println("\t\t: Verifying for hostname: " + hostname);
            try {
                String s = ((X509Certificate) session.getPeerCertificates()[0]).getSubjectX500Principal().getName();
                System.out.println("\t\t: Certificate subject principal: " + s);
                Matcher m = cnPattern.matcher(s);
                if (m.find()) {
                    String cn = m.group(2);
                    System.out.println("\t\t: Issued to " + cn);
                    boolean result = false;
                    if (cn.charAt(0) == '*') {
                        result = hostname.endsWith(cn.substring(1));
                        System.out.println("\t\t: Validating wildcarded certificate - " + hostname + " vs. " + cn + ". Valid: " + result);
                    } else {
                        result = cn.equals(hostname);
                        System.out.println("\t\t: Validating exact certificate - " + hostname + " vs. " + cn + ". Valid: " + result);
                    }
                    if (!result) {
                        result = hostname.equals(session.getPeerHost());
                        System.out.println("\t\t: Validating hostname with session - " + hostname + " vs. " + session + "@" + session.getPeerHost() + ". Valid: " + result);
                    }
                    return result;
                } else return false;
            } catch (Exception e) {
                System.out.println("HostnameVerifier:verify got error: " + e.getMessage());
                return false;
            }
        }
    };
}
