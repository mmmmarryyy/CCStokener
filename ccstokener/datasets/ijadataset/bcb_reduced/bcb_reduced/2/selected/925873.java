package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.HostsPanel;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.abstractimpl.AbstractAccount;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
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
 * @author Dinesh
 */
public class UploadingDotComAccount extends AbstractAccount {

    private URL u;

    private HttpURLConnection uc;

    private static String tmp = "", sidcookie = "", timecookie = "", cachecookie = "", ucookie = "";

    private BufferedReader br;

    private String uploadresponse = "";

    private static String startuppage;

    public UploadingDotComAccount() {
        KEY_USERNAME = "udcusername";
        KEY_PASSWORD = "udcpassword";
        HOSTNAME = "Uploading.com";
    }

    public void disableLogin() {
        loginsuccessful = false;
        sidcookie = "";
        cachecookie = "";
        timecookie = "";
        HostsPanel.getInstance().uploadingDotComCheckBox.setEnabled(false);
        HostsPanel.getInstance().uploadingDotComCheckBox.setSelected(false);
        NeembuuUploader.getInstance().updateSelectedHostsLabel();
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    private void loginUploadingdotcom() throws Exception {
        loginsuccessful = false;
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        NULogger.getLogger().info("Trying to log in to uploading.com");
        HttpPost httppost = new HttpPost("http://uploading.com/general/login_form/?SID=" + sidcookie.replace("SID=", "") + "&JsHttpRequest=" + new Date().getTime() + "0-xml");
        httppost.setHeader("Referer", "http://www.uploading.com/");
        httppost.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        httppost.setHeader("Cookie", sidcookie + ";" + cachecookie + ";" + timecookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("email", getUsername()));
        formparams.add(new BasicNameValuePair("password", getPassword()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpresponse = httpclient.execute(httppost);
        HttpEntity en = httpresponse.getEntity();
        uploadresponse = EntityUtils.toString(en);
        NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
        NULogger.getLogger().info("Getting cookies........");
        Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
        Cookie escookie = null;
        while (it.hasNext()) {
            escookie = it.next();
            if (escookie.getName().equalsIgnoreCase("u")) {
                ucookie = escookie.getName() + "=" + escookie.getValue();
                NULogger.getLogger().info(ucookie);
                loginsuccessful = true;
            }
            if (escookie.getName().equalsIgnoreCase("cache")) {
                cachecookie = escookie.getName() + "=" + escookie.getValue();
                NULogger.getLogger().info(cachecookie);
            }
            if (escookie.getName().equalsIgnoreCase("time")) {
                timecookie = escookie.getName() + "=" + escookie.getValue();
                NULogger.getLogger().info(timecookie);
            }
        }
        if (loginsuccessful) {
            NULogger.getLogger().info("Uploading.com Login successful. :)");
            loginsuccessful = true;
            username = getUsername();
            password = getPassword();
            HostsPanel.getInstance().uploadingDotComCheckBox.setEnabled(true);
        } else {
            NULogger.getLogger().info("Uploading.com Login failed :(");
            loginsuccessful = false;
            username = "";
            password = "";
            JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
            AccountsManager.getInstance().setVisible(true);
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from uploading.com");
        u = new URL("http://uploading.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("time")) {
                    timecookie = tmp;
                }
                if (tmp.contains("cache")) {
                    cachecookie = tmp;
                }
                if (tmp.contains("SID")) {
                    sidcookie = tmp;
                }
            }
        }
        sidcookie = sidcookie.substring(0, sidcookie.indexOf(";"));
        cachecookie = cachecookie.substring(0, cachecookie.indexOf(";"));
        timecookie = timecookie.substring(0, timecookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "sidcookie : {0}", sidcookie);
        NULogger.getLogger().log(Level.INFO, "timecookie : {0}", timecookie);
        NULogger.getLogger().log(Level.INFO, "cachecookie : {0}", cachecookie);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        startuppage = "";
        while ((tmp = br.readLine()) != null) {
            startuppage += tmp;
        }
    }

    public static String getCachecookie() {
        return cachecookie;
    }

    public static String getSidcookie() {
        return sidcookie;
    }

    public static String getTimecookie() {
        return timecookie;
    }

    public static String getStartuppage() {
        return startuppage;
    }

    public static String getUcookie() {
        return ucookie;
    }

    private String getData() throws Exception {
        u = new URL("http://www.uploading.com");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", sidcookie + ";" + ucookie + ";" + cachecookie + ";" + timecookie);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        return k;
    }

    public void login() {
        try {
            initialize();
            loginUploadingdotcom();
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: Error in Uploading.com Login", getClass().getName());
        }
    }
}
