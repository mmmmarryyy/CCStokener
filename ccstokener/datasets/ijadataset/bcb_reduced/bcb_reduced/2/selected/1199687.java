package org.qsari.effectopedia.defaults;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class DefaultServerSettings {

    public static final String signInURL = "http://effectopedia.org/hybridauth/test/index.php";

    public static final String registerURL = "http://effectopedia.org/hybridauth/test/?route=users/register";

    public static final String signOutURL = "http://effectopedia.org/hybridauth/test/index.php";

    ;

    public static final String profileURL = "http://effectopedia.org/hybridauth/test/index.php?route=users/profile";

    public static final String defaultFileName = "Effectopedia";

    public static final String defaultFileExt = "aopz";

    public static final String baseURL = "http://effectopedia.org/rev/";

    public static final String currentRevision = "http://effectopedia.org/rev/info.php?revision=current";

    public static final String getRevision = "http://effectopedia.org/rev/info.php?revision=get";

    public static final String commitRevision = "http://effectopedia.org/rev/info.php?revision=commit&number=";

    public static final String ftpServer = "ftp.effectopedia.org";

    private static final String ftpUser = "u39607026-contr";

    private static final String ftpPassword = "AOPContributor@)!!";

    private static String responce = null;

    public static boolean isOnline() {
        try {
            URL url = new URL(currentRevision);
            url.openStream().close();
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static String getCurrentRevision() {
        responce = getServiceResponse(currentRevision);
        return baseURL + defaultFileName + responce + "." + defaultFileExt;
    }

    public static final String getResponce() {
        return responce;
    }

    public static String commitRevision(String revision, String userID) {
        return getServiceResponse(commitRevision + revision + "&user=" + userID);
    }

    public static String getServiceResponse(String service) {
        InputStream inputStream;
        URL url;
        String revision = "";
        try {
            url = new URL(service);
            inputStream = url.openStream();
        } catch (MalformedURLException e) {
            inputStream = null;
        } catch (IOException e) {
            inputStream = null;
        }
        if (inputStream != null) {
            try {
                int bytesRead;
                BufferedInputStream buffer = new BufferedInputStream(inputStream);
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                byte data[] = new byte[512];
                while ((bytesRead = buffer.read(data, 0, 512)) != -1) content.write(data, 0, bytesRead);
                revision = content.toString("UTF-8").trim();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return revision;
    }

    public static String getFTPURL() {
        StringBuffer sb = new StringBuffer("ftp://");
        try {
            sb.append(URLEncoder.encode(ftpUser, "UTF-8"));
            sb.append(':');
            sb.append(URLEncoder.encode(ftpPassword, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sb.append('@');
        sb.append(ftpServer);
        sb.append('/');
        sb.append(defaultFileName);
        responce = getServiceResponse(getRevision);
        sb.append(responce);
        sb.append(".");
        sb.append(defaultFileExt);
        sb.append(";type=i");
        return sb.toString();
    }

    public static boolean isInternallyLoadedURL(URL url) {
        String urlString = url.toString();
        return signInURL.equals(urlString) || profileURL.equals(urlString) || registerURL.equals(urlString);
    }
}
