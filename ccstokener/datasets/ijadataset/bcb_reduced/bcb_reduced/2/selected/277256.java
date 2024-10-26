package net.dromard.movies;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import net.dromard.common.url.HttpURLConnectionViaProxy;
import net.dromard.movies.service.ServiceLocator;

public final class AppContext implements AppConstants {

    private static AppContext singleton = new AppContext();

    private File pathImage;

    private File pathTmp;

    private String proxyHost;

    private int proxyPort;

    private AppContext() {
        AppConf conf = AppConf.getInstance();
        this.pathImage = new File(conf.getProperty(KEY_PATH_IMAGE, "."));
        System.out.println("[INFO] Images path: " + pathImage);
        if (!pathImage.exists()) pathImage.mkdir();
        this.pathTmp = new File(conf.getProperty(KEY_PATH_TMP, "."));
        System.out.println("[INFO] Tmp path: " + pathTmp);
        if (!pathTmp.exists()) pathTmp.mkdir();
        pathTmp.deleteOnExit();
        this.proxyHost = conf.getProperty(KEY_PROXY_HOST, null);
        this.proxyPort = conf.getProperty(KEY_PROXY_PORT, -1);
    }

    public static AppContext getInstance() {
        return singleton;
    }

    public File getImagePath() {
        return pathImage;
    }

    public File getTempPath() {
        return pathTmp;
    }

    public ServiceLocator getServiceLocator() {
        return ServiceLocator.getInstance();
    }

    public URLConnection createHttpURLConnection(String url) throws MalformedURLException, IOException {
        HttpURLConnection.setFollowRedirects(true);
        if (proxyHost != null && proxyPort > 0) {
            return new HttpURLConnectionViaProxy(new URL(url), proxyHost, proxyPort);
        } else {
            return new URL(url).openConnection();
        }
    }
}
