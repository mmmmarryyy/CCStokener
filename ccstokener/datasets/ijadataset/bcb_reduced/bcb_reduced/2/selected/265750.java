package org.openremote.android.console.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.openremote.android.console.Constants;
import org.openremote.android.console.model.ControllerException;
import org.openremote.android.console.net.SelfCertificateSSLSocketFactory;
import android.content.Context;
import android.util.Log;

/**
 * Does the HTTP stuff, anything related to HttpClient should go here.
 * 
 * @author Andrew C. Oliver <acoliver at osintegrators.com>
 * @author Dan Cong
 */
public class HTTPUtil {

    /**
    * Down load panel.xml from controller.
    * 
    * @param serverUrl the current server url
    * @param panelName the current panel name
    * 
    * @return the int
    */
    public static int downLoadPanelXml(Context context, String serverUrl, String panelName) {
        return downLoadFile(context, serverUrl + "/rest/panel/" + encodePercentUri(panelName), Constants.PANEL_XML);
    }

    public static int downLoadImage(Context context, String serverUrl, String imageName) {
        if (FileUtil.checkFileExists(context, imageName)) {
            Log.i("OpenRemote-SKIP IMAGE", imageName + " is already in cache");
        } else {
            Log.i("OpenRemote-NEW IMAGE", imageName + " downloading...");
            return downLoadFile(context, serverUrl + "/resources/" + encodePercentUri(imageName), imageName);
        }
        return 200;
    }

    public static int downLoadImageIgnoreCache(Context context, String serverUrl, String imageName) {
        return downLoadFile(context, serverUrl + "/resources/" + encodePercentUri(imageName), imageName);
    }

    /**
    * Encode percent uri to "UTF-8".
    * 
    * @param uri the uri
    * 
    * @return the string
    */
    public static String encodePercentUri(String uri) {
        String encodedUri = null;
        try {
            encodedUri = URLEncoder.encode(uri, "UTF-8");
            if (!encodedUri.equals(uri)) {
                Log.i("OpenRemote-URLEncoder", encodedUri);
            }
        } catch (UnsupportedEncodingException e) {
            encodedUri = uri;
            Log.e("OpenRemote-UnsupportedEncodingException", "Failed to encode percent : " + uri, e);
        }
        return encodedUri;
    }

    /**
    * Down load file and store it in local context.
    * 
    * @param serverUrl the current server url
    * @param fileName the file name for downloading
    * 
    * @return the int
    */
    private static int downLoadFile(Context context, String serverUrl, String fileName) {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 5 * 1000);
        HttpConnectionParams.setSoTimeout(params, 5 * 1000);
        HttpClient client = new DefaultHttpClient(params);
        int statusCode = ControllerException.CONTROLLER_UNAVAILABLE;
        try {
            URL uri = new URL(serverUrl);
            if ("https".equals(uri.getProtocol())) {
                Scheme sch = new Scheme(uri.getProtocol(), new SelfCertificateSSLSocketFactory(), uri.getPort());
                client.getConnectionManager().getSchemeRegistry().register(sch);
            }
            HttpGet get = new HttpGet(serverUrl);
            SecurityUtil.addCredentialToHttpRequest(context, get);
            HttpResponse response = client.execute(get);
            statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == Constants.HTTP_SUCCESS) {
                FileOutputStream fOut = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                InputStream is = response.getEntity().getContent();
                byte buf[] = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fOut.write(buf, 0, len);
                }
                fOut.close();
                is.close();
            }
        } catch (MalformedURLException e) {
            Log.e("OpenRemote-HTTPUtil", "Create URL fail:" + serverUrl);
        } catch (IllegalArgumentException e) {
            Log.e("OpenRemote-IllegalArgumentException", "Download file " + fileName + " failed with URL: " + serverUrl, e);
        } catch (ClientProtocolException cpe) {
            Log.e("OpenRemote-ClientProtocolException", "Download file " + fileName + " failed with URL: " + serverUrl, cpe);
        } catch (IOException ioe) {
            Log.e("OpenRemote-IOException", "Download file " + fileName + " failed with URL: " + serverUrl, ioe);
        }
        return statusCode;
    }
}
