package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.FileFactoryAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class FileFactory extends AbstractUploader {

    FileFactoryAccount fileFactoryAccount = (FileFactoryAccount) AccountsManager.getAccount("FileFactory.com");

    private boolean login = false;

    private URL u;

    private HttpURLConnection uc;

    private String filecookie = "";

    private BufferedReader br;

    private String downloadlink = "";

    public FileFactory(File file) {
        super(file);
        host = "FileFactory.com";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        if (fileFactoryAccount.loginsuccessful) {
            login = true;
            host = fileFactoryAccount.username + " | FileFactory.com";
        }
    }

    public void run() {
        try {
            if (file.length() > 2147483648l) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            if (fileFactoryAccount.loginsuccessful) {
                login = true;
                host = fileFactoryAccount.username + " | FileFactory.com";
            } else {
                login = false;
                host = "FileFactory.com";
            }
            status = UploadStatus.INITIALISING;
            if (!login) {
                status = UploadStatus.INITIALISING;
                u = new URL("http://filefactory.com/");
                uc = (HttpURLConnection) u.openConnection();
                filecookie = uc.getHeaderField("Set-Cookie");
                NULogger.getLogger().log(Level.INFO, "FileFactory Cookie : {0}", filecookie);
            }
            fileupload();
        } catch (Exception e) {
            Logger.getLogger(FileFactory.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
            uploadFailed();
        }
    }

    private void fileupload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://upload.filefactory.com/upload.php");
        if (login) {
            httppost.setHeader("Cookie", fileFactoryAccount.getFilecookie() + ";" + fileFactoryAccount.getMembershipcookie());
        } else {
            httppost.setHeader("Cookie", filecookie);
        }
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
        reqEntity.addPart("upload", new StringBody("Submit Query"));
        httppost.setEntity(reqEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into filefactory.com. Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        String page = "";
        if (resEntity != null) {
            page = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "Page URL value :{0}", page);
        }
        status = UploadStatus.GETTINGLINK;
        downloadlink = getData("http://www.filefactory.com/mupc/" + page);
        downloadlink = downloadlink.substring(downloadlink.indexOf("<div class=\"metadata\">"));
        downloadlink = downloadlink.replace("<div class=\"metadata\">", "");
        downloadlink = downloadlink.substring(0, downloadlink.indexOf("<"));
        downloadlink = downloadlink.trim();
        NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
        downURL = downloadlink;
        uploadFinished();
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
}
