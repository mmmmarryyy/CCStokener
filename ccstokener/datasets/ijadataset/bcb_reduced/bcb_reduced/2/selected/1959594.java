package neembuuuploader.accounts;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class BayFilesAccount extends AbstractAccount {

    private URL u;

    private HttpURLConnection uc;

    private String tmp;

    private static String sessioncookie = "";

    public BayFilesAccount() {
        KEY_USERNAME = "bfusername";
        KEY_PASSWORD = "bfpassword";
        HOSTNAME = "BayFiles.com";
    }

    public void disableLogin() {
        loginsuccessful = false;
        sessioncookie = "";
    }

    public static String getSessioncookie() {
        return sessioncookie;
    }

    public void login() {
        try {
            loginsuccessful = false;
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to bayfiles.com");
            HttpPost httppost = new HttpPost("http://bayfiles.com/ajax_login");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("action", "login"));
            formparams.add(new BasicNameValuePair("username", getUsername()));
            formparams.add(new BasicNameValuePair("password", getPassword()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info("Getting cookies........");
            Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
            Cookie escookie = null;
            while (it.hasNext()) {
                escookie = it.next();
                if (escookie.getName().equalsIgnoreCase("SESSID")) {
                    sessioncookie = "SESSID=" + escookie.getValue();
                    NULogger.getLogger().info(sessioncookie);
                    loginsuccessful = true;
                }
            }
            if (loginsuccessful) {
                NULogger.getLogger().info("BayFiles.com Login success :)");
                username = getUsername();
                password = getPassword();
            } else {
                NULogger.getLogger().info("UploadBox Login failed");
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
            }
        } catch (Exception e) {
            Logger.getLogger(BayFilesAccount.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
