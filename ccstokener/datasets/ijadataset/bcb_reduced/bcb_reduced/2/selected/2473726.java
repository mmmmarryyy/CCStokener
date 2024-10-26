package bt747.j2se_view;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import bt747.sys.Generic;

/**
 * @author Mario
 * 
 */
public class J2SEAGPS {

    private static final String TRANS_FTP_SITE = "http://bt747.free.fr/";

    private static final String TRANS_AGPS_14d = TRANS_FTP_SITE + "MTK14.EPO";

    private static final String TRANS_AGPS_7d = TRANS_FTP_SITE + "MTK7d.EPO";

    public static final byte[] getAGPS7d() {
        return getBytesFromUrl(TRANS_AGPS_7d);
    }

    public static final byte[] getBytesFromUrl(String urlString) {
        byte[] result = null;
        try {
            URL url = new URL(urlString);
            URLConnection urlc = url.openConnection();
            urlc.setConnectTimeout(5000);
            urlc.setReadTimeout(5000);
            InputStream ins = urlc.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream(120 * 1024);
            byte[] buf = new byte[1024];
            while (true) {
                int n = ins.read(buf);
                if (n == -1) {
                    break;
                }
                bout.write(buf, 0, n);
            }
            result = bout.toByteArray();
            bout.close();
            bout = null;
            ins = null;
        } catch (Exception e) {
            Generic.debug("Problem downloading AGPS data");
        }
        return result;
    }
}
