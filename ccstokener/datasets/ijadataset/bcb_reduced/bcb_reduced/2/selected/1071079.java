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
import neembuuuploader.utils.NULogger;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 *
 * @author dinesh
 */
public class SlingFileAccount extends AbstractAccount {

    private HttpURLConnection uc;

    private URL u;

    private static StringBuilder slingfilecookie = null;

    private BufferedReader br;

    public SlingFileAccount() {
        KEY_USERNAME = "sfusername";
        KEY_PASSWORD = "sfpassword";
        HOSTNAME = "SlingFile.com";
    }

    public static StringBuilder getSlingfilecookie() {
        return slingfilecookie;
    }

    public void disableLogin() {
        loginsuccessful = false;
        if (slingfilecookie != null) {
            slingfilecookie.setLength(0);
            slingfilecookie.append("");
        }
        HostsPanel.getInstance().slingFileCheckBox.setEnabled(false);
        HostsPanel.getInstance().slingFileCheckBox.setSelected(false);
        NeembuuUploader.getInstance().updateSelectedHostsLabel();
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void login() {
        loginsuccessful = false;
        try {
            initialize();
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to slingfile.com");
            HttpPost httppost = new HttpPost("http://www.slingfile.com/login");
            httppost.setHeader("Cookie", slingfilecookie.toString() + ";signupreferrerurl=http%3A%2F%2Fwww.slingfile.com%2F;");
            httppost.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("f_user", getUsername()));
            formparams.add(new BasicNameValuePair("f_password", getPassword()));
            formparams.add(new BasicNameValuePair("submit", "Login »"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info(httpresponse.getStatusLine().toString());
            Header lastHeader = httpresponse.getLastHeader("Location");
            if (lastHeader != null && lastHeader.getValue().contains("dashboard")) {
                loginsuccessful = true;
                slingfilecookie.append(";signupreferrerurl=http%3A%2F%2Fwww.slingfile.com%2F;");
                username = getUsername();
                password = getPassword();
                HostsPanel.getInstance().slingFileCheckBox.setEnabled(true);
            } else {
                loginsuccessful = false;
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
            }
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: {1}", new Object[] { getClass().getName(), e.toString() });
            System.err.println(e);
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookies & link from slingfile.com");
        u = new URL("http://www.slingfile.com/");
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        slingfilecookie = new StringBuilder();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                slingfilecookie.append(header.get(i)).append(";");
            }
            NULogger.getLogger().info(slingfilecookie.toString());
        }
    }
}
