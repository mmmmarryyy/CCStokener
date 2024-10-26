package rath.msnm.util;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * SSL based Passport Login class :)
 * <p>
 * TWN class is execute on JDK 1.4 or later for SSL
 * class(javax.net.ssl.HttpsURLConnection).
 * 
 * @author Jang-Ho Hwang, imrath@empal.com
 * @version 1.0.000, 2003/10/16
 */
public class TWN {

    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(TWN.class);

    static {
        java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    public TWN() {
    }

    private static String getContent(InputStream in, int len) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("getContent(InputStream, int) - start");
        }
        byte[] buf = new byte[32767];
        int off = 0;
        while (off < buf.length) {
            int readlen = in.read(buf, off, buf.length - off);
            if (readlen < 1) break;
            off += readlen;
        }
        in.close();
        String returnString = new String(buf, 0, off);
        if (logger.isDebugEnabled()) {
            logger.debug("getContent(InputStream, int) - end");
        }
        return returnString;
    }

    public static String getTNP(String userid, String password, String tpf) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("getTNP(String, String, String) - start");
        }
        String returnString = getTNPImpl(userid, password, tpf, 1);
        if (logger.isDebugEnabled()) {
            logger.debug("getTNP(String, String, String) - end");
        }
        return returnString;
    }

    private static String getTNPImpl(String userid, String password, String tpf, int count) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("getTNPImpl(String, String, String, int) - start");
        }
        String domain = "loginnet.passport.com";
        URL url0 = new URL("https://" + domain + "/login2.srf");
        HttpURLConnection con0 = (HttpURLConnection) url0.openConnection();
        con0.setRequestMethod("GET");
        con0.setUseCaches(false);
        con0.setDoInput(true);
        con0.setRequestProperty("Host", domain);
        String author = "Passport1.4 OrgVerb=GET,OrgURL=http://messenger.msn.com," + "sign-in=" + URLEncoder.encode(userid, "EUC-KR") + ",pwd=" + password + "," + tpf;
        con0.setRequestProperty("Authorization", author);
        String ret = getContent(con0.getInputStream(), con0.getContentLength());
        con0.disconnect();
        String auth = con0.getHeaderField("Authentication-Info");
        if (auth == null) return "t=0&p=0";
        String da_status = getValueFromKey(auth, "da-status");
        String fromPP = getValueFromKey(auth, "from-PP");
        if (logger.isDebugEnabled()) {
            logger.debug("getTNPImpl(String, String, String, int) - end");
        }
        return fromPP;
    }

    private static String getValueFromKey(String str, String key) {
        if (logger.isDebugEnabled()) {
            logger.debug("getValueFromKey(String, String) - start");
        }
        int i0 = str.indexOf(key);
        if (i0 == -1) return null;
        int i1 = str.indexOf(',', i0 + 1);
        if (i1 == -1) i1 = str.length();
        int i2 = str.indexOf('=', i0 + 1);
        String ret = str.substring(i2 + 1, i1);
        ret = ret.replaceAll("'", "");
        if (logger.isDebugEnabled()) {
            logger.debug("getValueFromKey(String, String) - end");
        }
        return ret;
    }
}
