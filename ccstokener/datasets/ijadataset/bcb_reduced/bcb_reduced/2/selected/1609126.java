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
public class SendSpaceAccount extends AbstractAccount {

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private static String tmp;

    private static String sidcookie = "";

    private static String ssuicookie = "", ssalcookie = "";

    public SendSpaceAccount() {
        KEY_USERNAME = "ssusername";
        KEY_PASSWORD = "sspassword";
        HOSTNAME = "SendSpace.com";
    }

    public void disableLogin() {
        loginsuccessful = false;
        sidcookie = "";
        ssalcookie = "";
        ssuicookie = "";
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from sendspace.com");
        u = new URL("http://www.sendspace.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("SID")) {
                    sidcookie = tmp;
                }
                if (tmp.contains("ssui")) {
                    ssuicookie = tmp;
                }
            }
        }
        sidcookie = sidcookie.substring(0, sidcookie.indexOf(";"));
        ssuicookie = ssuicookie.substring(0, ssuicookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "sidcookie: {0}", sidcookie);
        NULogger.getLogger().log(Level.INFO, "ssuicookie: {0}", ssuicookie);
    }

    public void loginSendSpace() throws Exception {
        loginsuccessful = false;
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        NULogger.getLogger().info("Trying to log in to sendspace");
        HttpPost httppost = new HttpPost("http://www.sendspace.com/login.html");
        httppost.setHeader("Cookie", sidcookie + ";" + ssuicookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("action", "login"));
        formparams.add(new BasicNameValuePair("submit", "login"));
        formparams.add(new BasicNameValuePair("target", "%252F"));
        formparams.add(new BasicNameValuePair("action_type", "login"));
        formparams.add(new BasicNameValuePair("remember", "1"));
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
            if (escookie.getName().equalsIgnoreCase("ssal")) {
                ssalcookie = escookie.getName() + "=" + escookie.getValue();
                NULogger.getLogger().info(ssalcookie);
                loginsuccessful = true;
            }
        }
        if (loginsuccessful) {
            username = getUsername();
            password = getPassword();
            NULogger.getLogger().info("SendSpace login success :)");
        } else {
            NULogger.getLogger().info("SendSpace login failed :(");
            loginsuccessful = false;
            username = "";
            password = "";
            JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
            AccountsManager.getInstance().setVisible(true);
        }
    }

    public static String getSidcookie() {
        return sidcookie;
    }

    public static String getSsalcookie() {
        return ssalcookie;
    }

    public static String getSsuicookie() {
        return ssuicookie;
    }

    public void login() {
        try {
            initialize();
            loginSendSpace();
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: Error in SendSpace Login", getClass().getName());
        }
    }
}
