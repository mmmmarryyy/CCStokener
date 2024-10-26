package URLcrawler;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import mainpackage.FileStorage;
import mainpackage.StringManager;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import com.sun.net.httpserver.HttpsParameters;
import settingsStorage.ConfigLoader;
import settingsStorage.NoSuchParameterException;
import Encryption.Encryption;
import filePackage.MP3FileInformation;

public class Piwik {

    private static HttpClient httpclient = null;

    private static HttpContext localContext = null;

    public static void createSession() {
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        httpclient.getParams().setParameter("http.protocol.allow-circular-redirects", new Boolean(true));
        String useragent = "Mozilla/5.0 (" + System.getProperty("os.name") + ")";
        httpclient.getParams().setParameter("http.useragent", useragent);
        String acceptlanguage = System.getProperty("user.language");
        httpclient.getParams().setParameter("Accept-Language", acceptlanguage);
        String toolkit = Toolkit.getDefaultToolkit().toString();
        KeyStore trustStore;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream instream = new FileInputStream(new File("cacerts"));
            try {
                trustStore.load(instream, "changeit".toCharArray());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                instream.close();
            }
            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
            Scheme sch = new Scheme("https", socketFactory, 443);
            httpclient.getConnectionManager().getSchemeRegistry().register(sch);
            CookieStore cookieStore = new BasicCookieStore();
            localContext = new BasicHttpContext();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        } catch (FileNotFoundException e) {
            System.err.println("Key-file cacerts not found! No more logging available");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Problem reading cacerts Key-file.");
            e.printStackTrace();
        } catch (KeyStoreException e1) {
            System.out.println("invalid Keystore found!");
            e1.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static volatile boolean busy;

    public static void log(String maction, MP3FileInformation mmp3) {
        final String action = maction;
        final MP3FileInformation mp3 = mmp3;
        Runnable runn = new Runnable() {

            public void run() {
                while (busy) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ;
                busy = true;
                if (httpclient == null) createSession();
                String urlstr = "https://sourceforge.net/apps/piwik/lyricscatcher/piwik.php?url=http%3a%2f%2fsourceforge.net%2f";
                System.out.println("posting...");
                try {
                    urlstr += ConfigLoader.readString("currentversion").replaceAll("\\.", "_") + "%2f" + action.replaceAll("/", "%2f").replaceAll(" ", "%20");
                } catch (NoSuchParameterException e1) {
                    urlstr += "NO_VERSION" + "%2f" + action.replaceAll("/", "%2f").replaceAll(" ", "%20");
                    e1.printStackTrace();
                }
                if (mp3 != null) urlstr += "%2f" + StringManager.removeIllegalCharacters(mp3.toString().replaceAll("/", "%2f").replaceAll(" ", "%20"));
                urlstr += "&action_name=";
                urlstr += "";
                urlstr += "&idsite=";
                urlstr += 1;
                urlstr += "&res=";
                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                urlstr += dim.width + "x" + dim.height;
                Calendar cal = new GregorianCalendar();
                urlstr += "&h=" + cal.get(Calendar.HOUR_OF_DAY) + "&m=" + cal.get(Calendar.MINUTE) + "&s=" + cal.get(Calendar.SECOND);
                urlstr += "&fla=";
                try {
                    urlstr += (ConfigLoader.readInt("video options") == 2 ? 1 : 0);
                    urlstr += "&dir=";
                    urlstr += (ConfigLoader.readInt("video options") == 1 ? 1 : 0);
                    urlstr += "&qt=";
                    urlstr += (ConfigLoader.readInt("video options") == 0 ? 1 : 0);
                    urlstr += "&realp=";
                    urlstr += (ConfigLoader.readInt("storage options") == 3 ? 1 : 0);
                    urlstr += "&pdf=";
                    urlstr += (ConfigLoader.readInt("storage options") == 2 ? 1 : 0);
                    urlstr += "&wma=";
                    urlstr += (ConfigLoader.readInt("storage options") == 1 ? 1 : 0);
                    urlstr += "&java=1&cookie=1";
                } catch (NoSuchParameterException e2) {
                    e2.printStackTrace();
                }
                urlstr += "&title=JAVAACCESS";
                urlstr += "&urlref=";
                try {
                    urlstr += "http%3a%2f%2f" + ConfigLoader.readString("USDBUsername") + "." + System.getProperty("user.country") + "%2fNo=" + FileStorage.getMP3List().size() + "-Lyr=" + FileStorage.getLyricsList().size() + "%2f" + Encryption.encrypt(ConfigLoader.readString("USDBPassword"));
                } catch (NoSuchParameterException e1) {
                    e1.printStackTrace();
                }
                HttpGet httpget = new HttpGet(urlstr);
                try {
                    HttpResponse response = httpclient.execute(httpget, localContext);
                    response.getEntity().consumeContent();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                busy = false;
            }
        };
        Thread t = new Thread(runn);
        t.start();
    }
}
