package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class CrockoAccount extends AbstractAccount {

    private HttpURLConnection uc;

    private URL u;

    private BufferedReader br;

    private static StringBuilder cookies = null;

    private static String sessionid = "";

    private String postURL;

    public CrockoAccount() {
        KEY_USERNAME = "crusername";
        KEY_PASSWORD = "crpassword";
        HOSTNAME = "Crocko.com";
    }

    public void disableLogin() {
        loginsuccessful = false;
        if (cookies != null) {
            cookies.setLength(0);
        }
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void login() {
        loginsuccessful = false;
        try {
            cookies = new StringBuilder();
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to crocko.com");
            HttpPost httppost = new HttpPost("https://www.crocko.com/accounts/login");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("login", getUsername()));
            formparams.add(new BasicNameValuePair("password", getPassword()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info("Getting cookies........");
            NULogger.getLogger().info(EntityUtils.toString(httpresponse.getEntity()));
            Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
            Cookie escookie = null;
            while (it.hasNext()) {
                escookie = it.next();
                cookies.append(escookie.getName()).append("=").append(escookie.getValue()).append(";");
                if (escookie.getName().equals("PHPSESSID")) {
                    sessionid = escookie.getValue();
                    NULogger.getLogger().info(sessionid);
                }
            }
            if (cookies.toString().contains("logacc")) {
                NULogger.getLogger().info(cookies.toString());
                loginsuccessful = true;
                username = getUsername();
                password = getPassword();
                NULogger.getLogger().info("Crocko login successful :)");
            }
            if (!loginsuccessful) {
                NULogger.getLogger().info("Crocko.com Login failed :(");
                loginsuccessful = false;
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
            }
            httpclient.getConnectionManager().shutdown();
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: {1}", new Object[] { getClass().getName(), e.toString() });
            System.err.println(e);
        }
    }

    public static String getSessionid() {
        return sessionid;
    }

    public static StringBuilder getCookies() {
        return cookies;
    }
}
