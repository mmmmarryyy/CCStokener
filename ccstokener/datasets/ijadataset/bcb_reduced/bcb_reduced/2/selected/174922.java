package net.sf.bt747.j2se.app.agps;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import bt747.sys.I18N;
import bt747.sys.interfaces.BT747Exception;

/**
 * @author Mario
 * 
 */
public class J2SEAGPS {

    private static final String TRANS_FTP_SITE = "http://bt747.free.fr/";

    private static final String TRANS_AGPS_14d = TRANS_FTP_SITE + "MTK14.EPO";

    private static final String TRANS_AGPS_7d = TRANS_FTP_SITE + "MTK7d.EPO";

    private static int timeout = 3 * 60000;

    static {
        try {
            timeout = Integer.valueOf(java.lang.System.getProperty("agpsTimeOut", String.valueOf(timeout)));
        } catch (Throwable e) {
        }
    }

    public static final byte[] getAGPS7d() throws BT747Exception {
        return getBytesFromUrl(TRANS_AGPS_7d);
    }

    public static final byte[] getBytesFromUrl(final String urlString) throws BT747Exception {
        byte[] result = null;
        try {
            final URL url = new URL(urlString);
            final URLConnection urlc = url.openConnection();
            urlc.setConnectTimeout(timeout);
            urlc.setReadTimeout(timeout);
            final InputStream ins = urlc.getInputStream();
            final ByteArrayOutputStream bout = new ByteArrayOutputStream(120 * 1024);
            final byte[] buf = new byte[1024];
            while (true) {
                final int n = ins.read(buf);
                if (n == -1) {
                    break;
                }
                bout.write(buf, 0, n);
            }
            result = bout.toByteArray();
            bout.close();
        } catch (final Exception e) {
            throw new BT747Exception(I18N.i18n("Problem downloading AGPS data."), e);
        }
        return result;
    }
}
