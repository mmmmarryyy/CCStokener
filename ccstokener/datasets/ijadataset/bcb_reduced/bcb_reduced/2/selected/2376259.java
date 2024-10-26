package neembuuuploader.accounts;

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
 * @author dinesh
 */
public class NetLoadAccount extends AbstractAccount {

    private URL u;

    private HttpURLConnection uc;

    private static String phpsessioncookie;

    private static String usercookie;

    public NetLoadAccount() {
        KEY_USERNAME = "nlusername";
        KEY_PASSWORD = "nlpassword";
        HOSTNAME = "Netload.in";
    }

    public static String getPhpsessioncookie() {
        return phpsessioncookie;
    }

    public static String getUsercookie() {
        return usercookie;
    }

    public void disableLogin() {
        loginsuccessful = false;
        phpsessioncookie = "";
        usercookie = "";
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    private void initialize() throws Exception {
        u = new URL("http://netload.in/");
        uc = (HttpURLConnection) u.openConnection();
        NULogger.getLogger().info("gettig netload post url.......");
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                String tmp = header.get(i);
                if (tmp.contains("PHPSESSID")) {
                    phpsessioncookie = tmp;
                    phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
                }
            }
        }
        NULogger.getLogger().log(Level.INFO, "PHP session cookie : {0}", phpsessioncookie);
    }

    public void login() {
        loginsuccessful = false;
        try {
            initialize();
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to netload.in");
            HttpPost httppost = new HttpPost("http://netload.in/index.php");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("txtuser", getUsername()));
            formparams.add(new BasicNameValuePair("txtpass", getPassword()));
            formparams.add(new BasicNameValuePair("txtcheck", "login"));
            formparams.add(new BasicNameValuePair("txtlogin", "Login"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info("Getting cookies........");
            Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
            Cookie escookie = null;
            while (it.hasNext()) {
                escookie = it.next();
                if (escookie.getName().equalsIgnoreCase("cookie_user")) {
                    usercookie = "cookie_user=" + escookie.getValue();
                    NULogger.getLogger().info(usercookie);
                    loginsuccessful = true;
                    username = getUsername();
                    password = getPassword();
                    NULogger.getLogger().info("Netload login successful :)");
                }
            }
            if (!loginsuccessful) {
                NULogger.getLogger().info("Netload Login failed :(");
                loginsuccessful = false;
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
            }
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: {1}", new Object[] { getClass().getName(), e.toString() });
        }
    }
}
