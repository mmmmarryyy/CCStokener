package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
public class GigaSizeAccount extends AbstractAccount {

    private static String formtoken;

    private static StringBuilder gigasizecookies = null;

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    public GigaSizeAccount() {
        KEY_USERNAME = "gsusername";
        KEY_PASSWORD = "gspassword";
        HOSTNAME = "GigaSize.com";
    }

    public static StringBuilder getGigasizecookies() {
        return gigasizecookies;
    }

    public void disableLogin() {
        loginsuccessful = false;
        if (gigasizecookies != null) {
            gigasizecookies.setLength(0);
        }
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void login() {
        try {
            initialize();
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to Giga Size");
            HttpPost httppost = new HttpPost("http://www.gigasize.com/signin");
            httppost.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("func", ""));
            formparams.add(new BasicNameValuePair("token", formtoken));
            formparams.add(new BasicNameValuePair("signRem", "1"));
            formparams.add(new BasicNameValuePair("email", getUsername()));
            formparams.add(new BasicNameValuePair("password", getPassword()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info("Getting cookies........");
            Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
            Cookie escookie = null;
            gigasizecookies.setLength(0);
            while (it.hasNext()) {
                escookie = it.next();
                gigasizecookies.append(escookie.getName()).append("=").append(escookie.getValue()).append(";");
            }
            NULogger.getLogger().info(gigasizecookies.toString());
            if (gigasizecookies.toString().contains("MIIS_GIGASIZE_AUTH")) {
                loginsuccessful = true;
                username = getUsername();
                password = getPassword();
                NULogger.getLogger().info("GigaSize Login Success");
            } else {
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
                NULogger.getLogger().info("GigaSize Login failed");
            }
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: {1}", new Object[] { getClass().getName(), e.toString() });
            System.err.println(e);
        }
    }

    private void initialize() throws IOException {
        gigasizecookies = new StringBuilder();
        NULogger.getLogger().info("Getting startup cookies from gigasize.com");
        u = new URL("http://www.gigasize.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                String tmp = header.get(i);
                gigasizecookies.append(tmp);
            }
            NULogger.getLogger().info(gigasizecookies.toString());
        }
        formtoken = getData("http://www.gigasize.com/formtoken");
        NULogger.getLogger().info(formtoken);
    }

    public String getData(String url) {
        try {
            u = new URL(url);
            uc = (HttpURLConnection) u.openConnection();
            uc.setDoOutput(true);
            uc.setRequestProperty("Host", "http://www.gigasize.com");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setRequestProperty("Referer", "http://gigasize.com/");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
            uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uc.setRequestProperty("Accept-Encoding", "html");
            uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.setRequestProperty("Cookie", gigasizecookies.toString());
            uc.setRequestMethod("GET");
            uc.setInstanceFollowRedirects(false);
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                k += temp;
            }
            return k;
        } catch (Exception e) {
            NULogger.getLogger().info("exception : " + e.toString());
            return "";
        }
    }
}
