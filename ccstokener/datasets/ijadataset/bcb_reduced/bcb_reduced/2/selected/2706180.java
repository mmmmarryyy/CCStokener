package neembuuuploader.accounts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.abstractimpl.AbstractAccount;
import neembuuuploader.utils.NULogger;
import neembuuuploader.utils.NeembuuUploaderProperties;

/**
 *
 * @author dinesh
 */
public class FileFactoryAccount extends AbstractAccount {

    private URL url;

    private HttpURLConnection uc;

    private PrintWriter pw;

    private BufferedReader br;

    private static String membershipcookie = "";

    private URLConnection conn;

    private static String filecookie = "";

    private String content = "";

    private URL u;

    public FileFactoryAccount() {
        KEY_USERNAME = "ffusername";
        KEY_PASSWORD = "ffpassword";
        HOSTNAME = "FileFactory.com";
    }

    public void login() {
        try {
            initilize();
        } catch (Exception ex) {
            Logger.getLogger(FileSonicAccount.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void disableLogin() {
        loginsuccessful = false;
        membershipcookie = "";
        filecookie = "";
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void initilize() throws Exception {
        url = new URL("http://filefactory.com/");
        conn = url.openConnection();
        filecookie = conn.getHeaderField("Set-Cookie");
        NULogger.getLogger().info(filecookie);
        content = "redirect=%2F&email=" + URLEncoder.encode(getUsername(), "UTF-8") + "&password=" + getPassword();
        postData(content, "http://filefactory.com/member/login.php");
    }

    public void setHttpHeader(String cookie) throws Exception {
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "filefactory.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://filefactory.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        uc.setRequestProperty("Cookie", cookie);
        uc.setRequestMethod("POST");
        uc.setInstanceFollowRedirects(false);
    }

    public void writeHttpContent(String content) throws Exception {
        loginsuccessful = false;
        pw = new PrintWriter(new OutputStreamWriter(uc.getOutputStream()), true);
        pw.print(content);
        pw.flush();
        pw.close();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String httpResp = br.readLine();
        NULogger.getLogger().log(Level.INFO, "Http Response value :{0}", httpResp);
        membershipcookie = uc.getHeaderField("Set-Cookie");
        NULogger.getLogger().log(Level.INFO, "FileFactory cookie : {0}", membershipcookie);
        if (membershipcookie == null) {
            NULogger.getLogger().info("FileFactory login failed");
            loginsuccessful = false;
            JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
            AccountsManager.getInstance().setVisible(true);
        } else {
            NULogger.getLogger().info("Filefactory login success");
            loginsuccessful = true;
            username = getUsername();
            password = getPassword();
        }
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        u = null;
        uc = null;
        return k;
    }

    public void postData(String content, String posturl) throws Exception {
        NULogger.getLogger().info("Login to FileFactory....");
        u = new URL(posturl);
        setHttpHeader(filecookie);
        writeHttpContent(content);
        u = null;
        uc = null;
    }

    public static String getMembershipcookie() {
        return membershipcookie;
    }

    public static String getFilecookie() {
        return filecookie;
    }
}
