package org.shaitu.easyphoto.action;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Logger;
import org.shaitu.easyphoto.AppConstants;
import org.shaitu.easyphoto.util.FileUtil;

/**
 * handler update event
 * @author whx
 */
public class UpdateAction {

    /**
     * log instance
     */
    private static final Logger logger = Logger.getLogger(UpdateAction.class.getName());

    /**
     * latest version No
     */
    private String latestVersion;

    /**
     * latest internal version
     */
    private String latestInternalVersion;

    /**
     * autoupdate internal verision
     */
    private String autoUpdateInternalVersion;

    /**
     * main app jar file url
     */
    private String jarFileURL;

    /**
     * publish info url
     */
    private String publishURL;

    /**
     * singleton UpdateAction instance 
     */
    private static UpdateAction updateAction;

    /**
     * auto update done flag
     */
    private boolean updateDone;

    /**
     * check upate success;
     */
    private boolean checkDone;

    /**
     * get singleton instance
     * @return UpdateAction singleton instance
     */
    public static UpdateAction getInstance() {
        if (updateAction == null) {
            updateAction = new UpdateAction();
        }
        return updateAction;
    }

    private UpdateAction() {
    }

    ;

    /**
     * check if update exist
     */
    public void checkUpdate() {
        Properties prop = new Properties();
        try {
            URL checkUpdateURL = new URL(AppConstants.UPDATE_CHECK_URL);
            URLConnection conn = checkUpdateURL.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            prop.load(conn.getInputStream());
            if (prop.isEmpty()) {
                checkDone = false;
                return;
            }
            latestVersion = prop.getProperty("latest_version");
            latestInternalVersion = prop.getProperty("inter_version_no");
            autoUpdateInternalVersion = prop.getProperty("autoupdate_inter_version");
            jarFileURL = prop.getProperty("jar_file");
            publishURL = prop.getProperty("publish_url");
            checkDone = true;
        } catch (Exception e) {
            e.printStackTrace();
            checkDone = false;
        }
    }

    /**
     * auto install update
     */
    public void autoUpdate() {
        Thread autoUpdateThread = new Thread() {

            public void run() {
                BufferedInputStream bis = null;
                BufferedOutputStream bos = null;
                try {
                    String installDir = FileUtil.getWorkingDir();
                    String updateJarFile = installDir + "EasyPhoto.jar.update";
                    URL url = new URL(jarFileURL);
                    bis = new BufferedInputStream(url.openStream());
                    bos = new BufferedOutputStream(new FileOutputStream(updateJarFile));
                    byte[] buffArr = new byte[1024 * 1024];
                    int n = 0;
                    while ((n = bis.read(buffArr)) != -1) {
                        bos.write(buffArr, 0, n);
                    }
                    updateDone = true;
                    logger.fine("downloaded new jar file for update");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bis != null) {
                            bis.close();
                        }
                        if (bos != null) {
                            bos.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        autoUpdateThread.start();
    }

    /**
     * if valid update exist
     * @return true has new update, else false
     */
    public boolean isUpdateValid() {
        if (AppConstants.CURRENT_INTERNAL_VERSION.compareTo(latestInternalVersion) < 0) {
            return true;
        }
        return false;
    }

    /**
     * if re-install needed
     * @return true re-install needed, else false
     */
    public boolean isAutoUpdateValid() {
        if (isUpdateValid() && AppConstants.CURRENT_INTERNAL_VERSION.compareTo(autoUpdateInternalVersion) >= 0) {
            return true;
        }
        return false;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public String getJarFileURL() {
        return jarFileURL;
    }

    public void setJarFileURL(String jarFileURL) {
        this.jarFileURL = jarFileURL;
    }

    public String getPublishURL() {
        return publishURL;
    }

    public void setPublishURL(String publishURL) {
        this.publishURL = publishURL;
    }

    public boolean isUpdateDone() {
        return updateDone;
    }

    public void setUpdateDone(boolean updateDone) {
        this.updateDone = updateDone;
    }

    public boolean isCheckDone() {
        return checkDone;
    }
}
