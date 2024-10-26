package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.abstractimpl.AbstractAccount;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
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
public class ImageShackAccount extends AbstractAccount {

    private String tmp;

    private String langcookie;

    private String latestcookie;

    private String uncookie;

    private String imgshckcookie;

    private String phpsessioncookie;

    private String newcookie;

    private String myidcookie;

    private static String myimagescookie;

    private static String usercookie;

    private URL u;

    private HttpURLConnection uc;

    private String uploadresponse = "";

    private BufferedReader br;

    private static String upload_key;

    public ImageShackAccount() {
        KEY_USERNAME = "isusername";
        KEY_PASSWORD = "ispassword";
        HOSTNAME = "ImageShack.us";
    }

    public static String getUsercookie() {
        return usercookie;
    }

    public static String getMyimagescookie() {
        return myimagescookie;
    }

    public static String getUpload_key() {
        return upload_key;
    }

    public void disableLogin() {
        loginsuccessful = false;
        langcookie = "";
        latestcookie = "";
        uncookie = "";
        imgshckcookie = "";
        phpsessioncookie = "";
        newcookie = "";
        myidcookie = "";
        myimagescookie = "";
        usercookie = "";
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from imageshack.us");
        u = new URL("http://imageshack.us/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("lang")) {
                    langcookie = tmp;
                }
                if (tmp.contains("latest")) {
                    latestcookie = tmp;
                }
                if (tmp.contains("un_")) {
                    uncookie = tmp;
                }
                if (tmp.contains("imgshck")) {
                    imgshckcookie = tmp;
                }
                if (tmp.contains("PHPSESSID")) {
                    phpsessioncookie = tmp;
                }
                if (tmp.contains("new_")) {
                    newcookie = tmp;
                }
            }
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        upload_key = CommonUploaderTasks.parseResponse(k, "name=\"key\" value=\"", "\"");
        NULogger.getLogger().log(Level.INFO, "upload_key : {0}", upload_key);
        langcookie = langcookie.substring(0, langcookie.indexOf(";"));
        latestcookie = latestcookie.substring(0, latestcookie.indexOf(";"));
        uncookie = uncookie.substring(0, uncookie.indexOf(";"));
        imgshckcookie = imgshckcookie.substring(0, imgshckcookie.indexOf(";"));
        phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
        newcookie = newcookie.substring(0, newcookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "langcookie : {0}", langcookie);
        NULogger.getLogger().log(Level.INFO, "latestcookie : {0}", latestcookie);
        NULogger.getLogger().log(Level.INFO, "uncookie : {0}", uncookie);
        NULogger.getLogger().log(Level.INFO, "imgshckcookie : {0}", imgshckcookie);
        NULogger.getLogger().log(Level.INFO, "phpsessioncookie : {0}", phpsessioncookie);
        NULogger.getLogger().log(Level.INFO, "newcookie : {0}", newcookie);
    }

    private void loginImageShack() throws Exception {
        loginsuccessful = false;
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        NULogger.getLogger().info("Trying to log in to imageshack.us");
        HttpPost httppost = new HttpPost("http://imageshack.us/auth.php");
        httppost.setHeader("Referer", "http://www.uploading.com/");
        httppost.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        httppost.setHeader("Cookie", newcookie + ";" + phpsessioncookie + ";" + imgshckcookie + ";" + uncookie + ";" + latestcookie + ";" + langcookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("username", getUsername()));
        formparams.add(new BasicNameValuePair("password", getPassword()));
        formparams.add(new BasicNameValuePair("stay_logged_in", ""));
        formparams.add(new BasicNameValuePair("format", "json"));
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
            if (escookie.getName().equalsIgnoreCase("myid")) {
                myidcookie = escookie.getValue();
                NULogger.getLogger().info(myidcookie);
                loginsuccessful = true;
            }
            if (escookie.getName().equalsIgnoreCase("myimages")) {
                myimagescookie = escookie.getValue();
                NULogger.getLogger().info(myimagescookie);
            }
            if (escookie.getName().equalsIgnoreCase("isUSER")) {
                usercookie = escookie.getValue();
                NULogger.getLogger().info(usercookie);
            }
        }
        if (loginsuccessful) {
            NULogger.getLogger().info("ImageShack Login Success");
            loginsuccessful = true;
            username = getUsername();
            password = getPassword();
        } else {
            NULogger.getLogger().info("ImageShack Login failed");
            loginsuccessful = false;
            username = "";
            password = "";
            JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
            AccountsManager.getInstance().setVisible(true);
        }
    }

    public void login() {
        try {
            initialize();
            loginImageShack();
        } catch (Exception ex) {
            Logger.getLogger(FileSonicAccount.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
