package traviaut;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.w3c.tidy.Tidy;

public abstract class Utils {

    private static final int TIMEOUT = 30000;

    public static final Random RND = new Random(System.currentTimeMillis());

    public static String encStr(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    public static String getEncodedReq(Map<String, String> req) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> ent : req.entrySet()) {
                sb.append(encStr(ent.getKey()));
                sb.append('=');
                sb.append(encStr(ent.getValue()));
                sb.append('&');
            }
            sb.setLength(sb.length() - 1);
        } catch (UnsupportedEncodingException uee) {
            Log.except("unsupported UTF-8 encoding", uee);
        }
        return sb.toString();
    }

    public static Tidy getDefTidy() {
        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        tidy.setOnlyErrors(true);
        Properties props = new Properties();
        props.setProperty("char-encoding", "utf8");
        tidy.setConfigurationFromProps(props);
        return tidy;
    }

    public static InputStream openURL(String url, ConnectData data) {
        try {
            URLConnection con = new URL(url).openConnection();
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);
            con.setUseCaches(false);
            con.setRequestProperty("Accept-Charset", "utf-8");
            setUA(con);
            if (data.cookie != null) con.setRequestProperty("Cookie", data.cookie);
            InputStream is = con.getInputStream();
            parseCookie(con, data);
            return new BufferedInputStream(is);
        } catch (IOException ioe) {
            Log.except("failed to open URL " + url, ioe);
        }
        return null;
    }

    public static InputStream sendReq(String url, String content, ConnectData data) {
        try {
            URLConnection con = new URL(url).openConnection();
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);
            con.setUseCaches(false);
            setUA(con);
            con.setRequestProperty("Accept-Charset", "utf-8");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (data.cookie != null) con.setRequestProperty("Cookie", data.cookie);
            HttpURLConnection httpurl = (HttpURLConnection) con;
            httpurl.setRequestMethod("POST");
            Writer wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(content);
            wr.flush();
            con.connect();
            InputStream is = con.getInputStream();
            is = new BufferedInputStream(is);
            wr.close();
            parseCookie(con, data);
            return is;
        } catch (IOException ioe) {
            Log.except("failed to send request " + url, ioe);
        }
        return null;
    }

    private static void parseCookie(URLConnection con, ConnectData data) {
        String c = con.getHeaderField("Set-Cookie");
        if (c == null) return;
        c = c.split(";")[0];
        data.cookie = c;
    }

    public static Date getDate(String str) {
        String[] spl = str.split(":");
        int hr = Integer.parseInt(spl[0]);
        int min = Integer.parseInt(spl[1]);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, min);
        return cal.getTime();
    }

    public static long timeStrToSec(String str) {
        String[] spl = str.split(":");
        long total = 0;
        int mul = 1;
        try {
            for (int i = spl.length - 1; i >= 0; i--) {
                total += mul * Integer.parseInt(spl[i]);
                mul *= 60;
            }
        } catch (NumberFormatException nfe) {
            Log.except("unknown time format: " + str, nfe);
            return 0;
        }
        return total;
    }

    public static long timeStrToMillis(String str) {
        return timeStrToSec(str) * 1000;
    }

    public static String timeSecToStr(long sec) {
        long min = sec / 60;
        long hr = min / 60;
        min %= 60;
        sec %= 60;
        return String.format("%1$d:%2$02d:%3$02d", hr, min, sec);
    }

    public static String timeMillisToStr(long millis) {
        return timeSecToStr(millis / 1000);
    }

    private static void setUA(URLConnection con) {
        String ua = Main.getUserAgent();
        if ((ua != null) && (ua.length() > 0)) con.setRequestProperty("User-Agent", ua);
    }

    public static void addRandClick(Map<String, String> req, int maxX) {
        int x = RND.nextInt(maxX);
        int y = RND.nextInt(20);
        req.put("s1.x", String.valueOf(x));
        req.put("s1.y", String.valueOf(y));
    }
}
