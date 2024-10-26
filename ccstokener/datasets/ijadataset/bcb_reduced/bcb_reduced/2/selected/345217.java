package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.abstractimpl.AbstractAccount;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 *
 * @author Dinesh
 */
public class LetitbitAccount extends AbstractAccount {

    private URL u;

    private HttpURLConnection uc;

    private static String tmp;

    private static String phpsessioncookie;

    private static String debugcookie;

    private BufferedReader br;

    private static String logcookie;

    private static String pascookie;

    private static String hostcookie;

    public LetitbitAccount() {
        KEY_USERNAME = "libusername";
        KEY_PASSWORD = "libpassword";
        HOSTNAME = "Letitbit.net";
    }

    public static String getDebugcookie() {
        return debugcookie;
    }

    public static String getHostcookie() {
        return hostcookie;
    }

    public static String getLogcookie() {
        return logcookie;
    }

    public static String getPascookie() {
        return pascookie;
    }

    public static String getPhpsessioncookie() {
        return phpsessioncookie;
    }

    public void disableLogin() {
        loginsuccessful = false;
        debugcookie = "";
        hostcookie = "";
        logcookie = "";
        pascookie = "";
        phpsessioncookie = "";
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void login() {
        try {
            initialize();
            loginLetitbit();
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: Error in Letitbit Login", getClass().getName());
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from letitbit.net");
        u = new URL("http://www.letitbit.net/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("PHPSESSID")) {
                    phpsessioncookie = tmp;
                }
                if (tmp.contains("debug_panel")) {
                    debugcookie = tmp;
                }
            }
        }
        phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
        debugcookie = debugcookie.substring(0, debugcookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "phpsessioncookie: {0}", phpsessioncookie);
        NULogger.getLogger().log(Level.INFO, "debugcookie : {0}", debugcookie);
    }

    public void loginLetitbit() throws Exception {
        loginsuccessful = false;
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        NULogger.getLogger().info("Trying to log in to letitbit.com");
        HttpPost httppost = new HttpPost("http://letitbit.net/");
        httppost.setHeader("Cookie", phpsessioncookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("act", "login"));
        formparams.add(new BasicNameValuePair("login", getUsername()));
        formparams.add(new BasicNameValuePair("password", getPassword()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpresponse = httpclient.execute(httppost);
        NULogger.getLogger().info("Getting cookies........");
        Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
        Cookie escookie = null;
        while (it.hasNext()) {
            escookie = it.next();
            if (escookie.getName().equalsIgnoreCase("log")) {
                logcookie = "log=" + escookie.getValue();
                NULogger.getLogger().info(logcookie);
                loginsuccessful = true;
            }
            if (escookie.getName().equalsIgnoreCase("pas")) {
                pascookie = "pas=" + escookie.getValue();
                NULogger.getLogger().info(pascookie);
            }
            if (escookie.getName().equalsIgnoreCase("host")) {
                hostcookie = "host=" + escookie.getValue();
                NULogger.getLogger().info(hostcookie);
            }
        }
        if (loginsuccessful) {
            loginsuccessful = true;
            username = getUsername();
            password = getPassword();
            NULogger.getLogger().info("Letitbit.net Login success :)");
        } else {
            NULogger.getLogger().info("Letitbit.net Login failed :(");
            loginsuccessful = false;
            username = "";
            password = "";
            JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
            AccountsManager.getInstance().setVisible(true);
        }
    }
}
