package anuo_xxz.tools.autovote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;

public class VoteBean implements IVoteBean {

    @Override
    public void vote(String urlString, Map<String, String> headData, Map<String, String> paramData) {
        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(urlString);
            httpConn = (HttpURLConnection) url.openConnection();
            String cookies = getCookies(httpConn);
            System.out.println(cookies);
            BufferedReader post = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "GB2312"));
            String text = null;
            while ((text = post.readLine()) != null) {
                System.out.println(text);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new VoteBeanException("网址不正确", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new VoteBeanException("不能打开网址", e);
        }
    }

    public static String getCookies(HttpURLConnection conn) {
        StringBuffer cookies = new StringBuffer();
        String headName;
        for (int i = 7; (headName = conn.getHeaderField(i)) != null; i++) {
            StringTokenizer st = new StringTokenizer(headName, "; ");
            while (st.hasMoreTokens()) {
                cookies.append(st.nextToken() + "; ");
            }
        }
        return cookies.toString();
    }
}
