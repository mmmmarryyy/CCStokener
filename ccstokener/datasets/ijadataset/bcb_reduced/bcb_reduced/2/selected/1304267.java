package leeon.kaixin.wap.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import leeon.kaixin.wap.util.HttpUtil;
import leeon.mobile.BBSBrowser.ContentException;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class DiaryAction {

    public static Map<String, String> prePostDiary(String verify) throws NetworkException {
        String url = HttpUtil.KAXIN_DAIRY_WRITE_URL + HttpUtil.KAIXIN_PARAM_VERIFY + verify;
        HttpClient client = HttpConfig.newInstance();
        HttpGet get = new HttpGet(url);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            String html = HTTPUtil.toString(response.getEntity());
            return PictureAction.dealSelectOptions(html, "name=\"classid\"");
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static void postDiary(String title, String content, String classid) throws ContentException, NetworkException {
        HttpClient client = HttpConfig.newInstance();
        HttpPost post = new HttpPost(HttpUtil.KAXIN_WWW_DAIRY_WRITE_URL);
        HttpResponse response = null;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("classid", classid));
        nvps.add(new BasicNameValuePair("title", title));
        nvps.add(new BasicNameValuePair("title2", title));
        nvps.add(new BasicNameValuePair("content", content));
        nvps.add(new BasicNameValuePair("privacy", "1"));
        nvps.add(new BasicNameValuePair("texttype", "html"));
        nvps.add(new BasicNameValuePair("repaste", "1"));
        nvps.add(new BasicNameValuePair("type", "1"));
        nvps.add(new BasicNameValuePair("draft", "0"));
        nvps.add(new BasicNameValuePair("sendpassword", "1"));
        nvps.add(new BasicNameValuePair("ctimeupdate", "1"));
        HttpUtil.setHeader(post);
        try {
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            response = client.execute(post);
            HTTPUtil.consume(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
        if (!HTTPUtil.isHttp200(response)) {
            throw new ContentException("post repaste failed");
        }
    }

    public static void main(String[] args) throws NetworkException {
        Map<String, String> m = prePostDiary("12560532_2538938_1301044192_4380f8867817e34659d1aac7aa80a5ba_kx");
        System.out.println(m);
    }
}
