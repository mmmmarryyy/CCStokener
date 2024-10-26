package org.zkoss.zrss;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.zkoss.image.AImage;
import org.zkoss.image.Image;

/**
 * 
 * a util class to do such Http Connection work.
 * @author Ian Tsai
 * @date 2007/5/15
 */
public class RssUtil {

    private RssUtil() {
    }

    /**
     * a rough method to GET html Content.
     * @param url
     * @return
     */
    public static String getHtmlContent(String url) {
        String content = null;
        return content;
    }

    /**
     * 
     * @param urlStr
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpConnection(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        return conn;
    }

    /**
     * 
     * @param url
     * @return
     * @throws IOException 
     * @throws IOException 
     * @throws HttpException 
     */
    public static InputStream getHttpInputStream(String urlStr) throws IOException {
        HttpURLConnection conn = getHttpConnection(urlStr);
        conn.setRequestProperty("User-Agent", Const.USER_AGENT);
        return conn.getInputStream();
    }

    /**
     * 
     * @param url
     * @return
     * @throws IOException 
     */
    public static int getHttpImage(String urlStr, Ref<Image> imgRef) throws IOException {
        HttpURLConnection conn = getHttpConnection(urlStr);
        conn.connect();
        int ans = conn.getResponseCode();
        conn.disconnect();
        if (ans < 400) imgRef.set(new AImage(new URL(urlStr)));
        return ans;
    }

    /**
     * 
     * @param htmlContent
     * @return
     */
    public static String getIconLinkFromHtmlContent(String htmlContent) {
        String ans = null;
        Pattern p = Pattern.compile("cat");
        String text = null;
        Matcher m = p.matcher(text);
        return ans;
    }
}
