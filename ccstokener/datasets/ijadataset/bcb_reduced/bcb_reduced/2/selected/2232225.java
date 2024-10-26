package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.HostsPanel;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.abstractimpl.AbstractAccount;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.utils.NULogger;
import neembuuuploader.utils.NeembuuUploaderProperties;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class BoxDotComAccount extends AbstractAccount {

    public static final String KEY_AUTH_TOKEN = "box_auth_token";

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String ticket;

    private String zcookie, visitorcookie;

    private String request_token;

    private String loginresponse = "";

    public BoxDotComAccount() {
        KEY_USERNAME = "boxusername";
        KEY_PASSWORD = "boxpassword";
        HOSTNAME = "Box.com";
    }

    public static String getAuth_token() {
        return NeembuuUploaderProperties.getEncryptedProperty(KEY_AUTH_TOKEN);
    }

    public void disableLogin() {
        NeembuuUploaderProperties.setEncryptedProperty(KEY_AUTH_TOKEN, "");
        loginsuccessful = false;
        username = "";
        password = "";
        HostsPanel.getInstance().boxDotComCheckBox.setEnabled(false);
        HostsPanel.getInstance().boxDotComCheckBox.setSelected(false);
        NeembuuUploader.getInstance().updateSelectedHostsLabel();
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void login() {
        if (!username.isEmpty()) {
            if (username.equals(getUsername()) && password.equals(getPassword())) {
                return;
            } else {
                loginsuccessful = false;
                NeembuuUploaderProperties.setEncryptedProperty(KEY_AUTH_TOKEN, "");
            }
        }
        if (!NeembuuUploaderProperties.getEncryptedProperty(KEY_AUTH_TOKEN).isEmpty()) {
            HostsPanel.getInstance().boxDotComCheckBox.setEnabled(true);
            loginsuccessful = true;
            username = getUsername();
            password = getPassword();
            return;
        }
        try {
            NULogger.getLogger().log(Level.INFO, "{0}Getting ticket value........", getClass());
            u = new URL("https://www.box.net/api/1.0/rest?action=get_ticket&api_key=vkf3k5dh0tg1ibvcikjcp8sx0f89d14u");
            uc = (HttpURLConnection) u.openConnection();
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String k = "", tmp;
            while ((tmp = br.readLine()) != null) {
                k += tmp;
            }
            ticket = CommonUploaderTasks.parseResponse(k, "<ticket>", "</ticket>");
            NULogger.getLogger().log(Level.INFO, "{0}Ticket : {1}", new Object[] { getClass(), ticket });
            uc.disconnect();
            NULogger.getLogger().log(Level.INFO, "{0}Getting cookies & request token value ..........", getClass());
            u = new URL("https://www.box.net/api/1.0/auth/" + ticket);
            uc = (HttpURLConnection) u.openConnection();
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            tmp = "";
            k = "";
            while ((tmp = br.readLine()) != null) {
                k += tmp;
            }
            request_token = CommonUploaderTasks.parseResponse(k, "request_token = '", "'");
            NULogger.getLogger().log(Level.INFO, "{0}Request token  : {1}", new Object[] { getClass(), request_token });
            Map<String, List<String>> headerFields = uc.getHeaderFields();
            if (headerFields.containsKey("Set-Cookie")) {
                List<String> header = headerFields.get("Set-Cookie");
                for (int i = 0; i < header.size(); i++) {
                    String t = header.get(i);
                    if (t.contains("z=")) {
                        zcookie = t;
                        zcookie = zcookie.substring(0, zcookie.indexOf(";"));
                    }
                    if (t.contains("box_visitor_id=")) {
                        visitorcookie = t;
                        visitorcookie = visitorcookie.substring(0, visitorcookie.indexOf(";"));
                    }
                }
                NULogger.getLogger().log(Level.INFO, "{0}zcookie : {1}", new Object[] { getClass(), zcookie });
                NULogger.getLogger().log(Level.INFO, "{0}visitorcookie : {1}", new Object[] { getClass(), visitorcookie });
            }
            uc.disconnect();
            loginBox();
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: Error in Box Login", getClass().getName());
        }
    }

    public void loginBox() throws Exception {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        NULogger.getLogger().log(Level.INFO, "{0}Trying to log in to box.com", getClass());
        HttpPost httppost = new HttpPost("https://www.box.net/api/1.0/auth/" + ticket);
        httppost.setHeader("Cookie", zcookie + ";" + visitorcookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("login", getUsername()));
        formparams.add(new BasicNameValuePair("password", getPassword()));
        formparams.add(new BasicNameValuePair("__login", "1"));
        formparams.add(new BasicNameValuePair("dologin", "1"));
        formparams.add(new BasicNameValuePair("reg_step", ""));
        formparams.add(new BasicNameValuePair("submit1", "1"));
        formparams.add(new BasicNameValuePair("folder", ""));
        formparams.add(new BasicNameValuePair("skip_framework_login", "1"));
        formparams.add(new BasicNameValuePair("login_or_register_mode", "login"));
        formparams.add(new BasicNameValuePair("new_login_or_register_mode", ""));
        formparams.add(new BasicNameValuePair("request_token", request_token));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpresponse = httpclient.execute(httppost);
        NULogger.getLogger().log(Level.INFO, "{0}Gonna print the response", getClass());
        loginresponse = EntityUtils.toString(httpresponse.getEntity());
        if (loginresponse.contains("Invalid username or password")) {
            NULogger.getLogger().log(Level.INFO, "{0}Box login failed", getClass());
            loginsuccessful = false;
            NeembuuUploaderProperties.setEncryptedProperty(KEY_AUTH_TOKEN, "");
            username = "";
            password = "";
            JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
            AccountsManager.getInstance().setVisible(true);
        } else {
            NULogger.getLogger().log(Level.INFO, "{0}Box login successful :)", getClass());
            getUserInfo();
            loginsuccessful = true;
            HostsPanel.getInstance().boxDotComCheckBox.setEnabled(true);
            username = getUsername();
            password = getPassword();
            NULogger.getLogger().info("Box Login success :)");
        }
    }

    private void getUserInfo() throws Exception {
        NULogger.getLogger().log(Level.INFO, "{0}Getting auth token value............", getClass());
        u = new URL("https://www.box.net/api/1.0/rest?action=get_auth_token&api_key=vkf3k5dh0tg1ibvcikjcp8sx0f89d14u&ticket=" + ticket);
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "", tmp;
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        String auth_token = CommonUploaderTasks.parseResponse(k, "<auth_token>", "</auth_token>");
        NULogger.getLogger().log(Level.INFO, "{0}Auth_token : {1}", new Object[] { getClass(), auth_token });
        NeembuuUploaderProperties.setEncryptedProperty(KEY_AUTH_TOKEN, auth_token);
        uc.disconnect();
    }
}
