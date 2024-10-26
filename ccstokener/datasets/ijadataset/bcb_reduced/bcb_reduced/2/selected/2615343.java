package edu.upmc.opi.caBIG.caTIES.fusion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
import com.google.gdata.util.common.util.Base64;
import edu.upmc.opi.caBIG.caTIES.common.CaTIES_CertificateUtils;

public class CaTIES_OAuthSubCaller {

    private static Logger logger = Logger.getLogger(CaTIES_OAuthSubCaller.class);

    public static String read(String url) {
        StringBuffer buffer = new StringBuffer();
        try {
            String[][] data = { { "oauth_callback", URLEncoder.encode("http://googlecodesamples.com/oauth_playground/index.php", "UTF-8") }, { "oauth_consumer_key", "anonymous" }, { "oauth_nonce", a64BitRandomString() }, { "oauth_signature_method", "HMAC-SHA1" }, { "oauth_timestamp", timeSinceEpochInMillis() }, { "oauth_signature", "" }, { "oauth_version", "1.0" }, { "scope", URLEncoder.encode("https://www.google.com/calendar/feeds/", "UTF-8") } };
            String signature_base_string = "GET&" + URLEncoder.encode(url, "UTF-8") + "&";
            for (int i = 0; i < data.length; i++) {
                if (i != 5) {
                    logger.debug(i);
                    signature_base_string += URLEncoder.encode(data[i][0], "UTF-8") + "%3D" + URLEncoder.encode(data[i][1], "UTF-8") + "%26";
                }
            }
            signature_base_string = signature_base_string.substring(0, signature_base_string.length() - 3);
            Mac m = Mac.getInstance("HmacSHA1");
            m.init(new SecretKeySpec("anonymous".getBytes(), "HmacSHA1"));
            m.update(signature_base_string.getBytes());
            byte[] res = m.doFinal();
            String sig = URLEncoder.encode(String.valueOf(Base64.encode(res)), "UTF8");
            data[5][1] = sig;
            String header = "OAuth ";
            int i = 0;
            for (String[] item : data) {
                if (i != 7) {
                    header += item[0] + "=\"" + item[1] + "\", ";
                }
                i++;
            }
            header = header.substring(0, header.length() - 2);
            logger.debug("Signature Base String: " + signature_base_string);
            logger.debug("Authorization Header: " + header);
            logger.debug("Signature: " + sig);
            String charset = "UTF-8";
            URLConnection connection = new URL(url + "?scope=" + URLEncoder.encode("https://www.google.com/calendar/feeds/", "UTF-8")).openConnection();
            connection.setRequestProperty("Authorization", header);
            connection.setRequestProperty("Accept", "*/*");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String read;
            while ((read = reader.readLine()) != null) {
                buffer.append(read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public static void main(String[] args) {
        boolean debug = false;
        if (!debug) {
            logger.debug(CaTIES_OAuthSubCaller.read("https://www.google.com/accounts/OAuthGetRequestToken"));
        } else {
            logger.debug(CaTIES_OAuthSubCaller.read("http://localhost/accounts/OAuthGetRequestToken"));
        }
    }

    private static String a64BitRandomString() {
        StringBuffer sb = new StringBuffer();
        Random generator = new Random();
        for (int i = 0; i < 32; i++) {
            Integer r = generator.nextInt();
            if (r < 0) {
                r = r * -1;
            }
            r = r % 16;
            sb.append(r.toHexString(r));
        }
        return sb.toString();
    }

    private static String timeSinceEpochInMillis() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        Long time = date.getTime();
        Integer i = (int) (time / 1000);
        return i.toString();
    }
}
