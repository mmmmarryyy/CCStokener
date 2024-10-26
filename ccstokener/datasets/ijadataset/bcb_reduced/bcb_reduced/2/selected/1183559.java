package neembuuuploader.accounts;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.HostsPanel;
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
public class UploadedDotToAccount extends AbstractAccount {

    private URL u;

    private HttpURLConnection uc;

    private String tmp;

    private static String phpsessioncookie = "";

    private static String logincookie = "";

    private static String authcookie = "";

    public UploadedDotToAccount() {
        KEY_USERNAME = "udtusername";
        KEY_PASSWORD = "udtpassword";
        HOSTNAME = "Uploaded.to";
    }

    public void disableLogin() {
        loginsuccessful = false;
        phpsessioncookie = "";
        logincookie = "";
        authcookie = "";
        HostsPanel.getInstance().uploadedDotToCheckBox.setEnabled(false);
        HostsPanel.getInstance().uploadedDotToCheckBox.setSelected(false);
        NeembuuUploader.getInstance().updateSelectedHostsLabel();
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public static String getLogincookie() {
        return logincookie;
    }

    public static String getPhpsessioncookie() {
        return phpsessioncookie;
    }

    public static String getAuthcookie() {
        return authcookie;
    }

    public void login() {
        try {
            loginsuccessful = false;
            NULogger.getLogger().info("Getting startup cookie from uploaded.to");
            u = new URL("http://uploaded.to/");
            uc = (HttpURLConnection) u.openConnection();
            Map<String, List<String>> headerFields = uc.getHeaderFields();
            if (headerFields.containsKey("Set-Cookie")) {
                List<String> header = headerFields.get("Set-Cookie");
                for (int i = 0; i < header.size(); i++) {
                    tmp = header.get(i);
                    if (tmp.contains("PHPSESSID")) {
                        phpsessioncookie = tmp;
                    }
                }
            }
            phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
            NULogger.getLogger().log(Level.INFO, "phpsessioncookie: {0}", phpsessioncookie);
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to uploaded.to");
            HttpPost httppost = new HttpPost("http://uploaded.to/io/login");
            httppost.setHeader("Cookie", phpsessioncookie);
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("id", getUsername()));
            formparams.add(new BasicNameValuePair("pw", getPassword()));
            formparams.add(new BasicNameValuePair("_", ""));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info("Getting cookies........");
            Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
            Cookie escookie = null;
            while (it.hasNext()) {
                escookie = it.next();
                if (escookie.getName().equalsIgnoreCase("login")) {
                    logincookie = "login=" + escookie.getValue();
                    NULogger.getLogger().info(logincookie);
                    loginsuccessful = true;
                    HostsPanel.getInstance().uploadedDotToCheckBox.setEnabled(true);
                }
                if (escookie.getName().equalsIgnoreCase("auth")) {
                    authcookie = "auth=" + escookie.getValue();
                    NULogger.getLogger().info(authcookie);
                }
            }
            if (loginsuccessful) {
                username = getUsername();
                password = getPassword();
                NULogger.getLogger().info("Uploaded.to Login success :)");
            } else {
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
                NULogger.getLogger().info("Uploaded.to Login failed :(");
            }
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: Error in Uploaded.to Login", getClass().getName());
        }
    }
}
