package neembuuuploader.versioning;

import java.util.logging.Level;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/** This thread checks for new updates at every launch.
 *
 * @author vigneshwaran
 */
public class CheckUpdate extends Thread {

    static float availablever, currentver;

    /**
     * 
     * @return whether the NeembuuUploader is uptodate or not. If not, launch Update Notification window..
     */
    public static boolean isCurrentVersion() {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://neembuuuploader.sourceforge.net/version.xml");
        NULogger.getLogger().info("Checking for new version...");
        try {
            HttpResponse response = httpclient.execute(httpget);
            String respxml = EntityUtils.toString(response.getEntity());
            availablever = getVersionFromXML(respxml);
            NULogger.getLogger().log(Level.INFO, "Available version: {0}", availablever);
            currentver = NeembuuUploader.version;
            NULogger.getLogger().log(Level.INFO, "Current version: {0}", currentver);
            if (availablever > currentver) {
                return false;
            }
        } catch (Exception ex) {
            NULogger.getLogger().log(Level.INFO, "Exception while checking update\n{0}", ex);
        }
        return true;
    }

    /**
     * 
     * @param str
     * @return the value between <version> and </version> tags from the specified string.
     */
    public static float getVersionFromXML(String str) {
        float ver = 0;
        try {
            String start = "<version>";
            String end = "</version>";
            str = str.substring(str.indexOf(start) + start.length());
            str = str.substring(0, str.indexOf(end));
            ver = Float.parseFloat(str);
        } catch (Exception any) {
            NULogger.getLogger().severe(any.toString());
        }
        return ver;
    }

    @Override
    public void run() {
        if (!(CheckUpdate.isCurrentVersion())) {
            NULogger.getLogger().info("New version found..");
            new NotifyUpdate().setVisible(true);
        }
    }
}
