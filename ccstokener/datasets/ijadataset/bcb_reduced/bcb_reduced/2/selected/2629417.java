package connect_tx_sdk.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import connect_tx_sdk.config.QQConfig;
import connect_tx_sdk.utils.ConnectUtils;
import connect_tx_sdk.utils.HttpClientUtils;

/**
 * 
 * @创建作者：hiyoucai@126.com
 * @创建时间：2011-6-17 下午03:01:25
 * @文件描述：发送动态消息到腾讯QQ空间
 */
public class ShareToken {

    /**
	 * 
	 * 方法说明： API文档说明：<a href="http://wiki.opensns.qq.com/wiki/%E3%80%90QQ%E7%99%BB%E5%BD%95%E3%80%91Qzone_OAuth%E8%AE%A4%E8%AF%81%E7%AE%80%E4%BB%8B" target="_blank">查看</a><br/>
	 * <br/>
	 * 创建日期：2011-6-17,下午03:01:44,hyc <br/>
	 * @param access_token 这个参数的意义：Step5请求具有Qzone访问权限的access_token时返回的字会话串
	 */
    public String share(String access_token, Map<String, String> params) throws Exception {
        String shareUrl = "http://openapi.qzone.qq.com/share/add_share";
        Map<String, String> tokens = ConnectUtils.parseTokenResult(access_token);
        String oauth_token = tokens.get("oauth_token");
        String oauth_token_secret = tokens.get("oauth_token_secret");
        String openid = tokens.get("openid");
        String oauth_timestamp = ConnectUtils.getOauthTimestamp();
        String oauth_nonce = ConnectUtils.getOauthNonce();
        List<NameValuePair> shareParameters = new ArrayList<NameValuePair>();
        shareParameters.add(new BasicNameValuePair("format", "xml"));
        shareParameters.add(new BasicNameValuePair("images", params.get("images")));
        shareParameters.add(new BasicNameValuePair("oauth_consumer_key", QQConfig.appid));
        shareParameters.add(new BasicNameValuePair("oauth_nonce", oauth_nonce));
        shareParameters.add(new BasicNameValuePair("oauth_signature_method", "HMAC-SHA1"));
        shareParameters.add(new BasicNameValuePair("oauth_timestamp", oauth_timestamp));
        shareParameters.add(new BasicNameValuePair("oauth_token", oauth_token));
        shareParameters.add(new BasicNameValuePair("oauth_version", "1.0"));
        shareParameters.add(new BasicNameValuePair("openid", openid));
        shareParameters.add(new BasicNameValuePair("title", params.get("title")));
        shareParameters.add(new BasicNameValuePair("url", params.get("url")));
        String oauth_signature = ConnectUtils.getOauthSignature("POST", shareUrl, shareParameters, oauth_token_secret);
        shareParameters.add(new BasicNameValuePair("oauth_signature", oauth_signature));
        HttpPost sharePost = new HttpPost(shareUrl);
        sharePost.setHeader("Referer", "http://openapi.qzone.qq.com");
        sharePost.setHeader("Host", "openapi.qzone.qq.com");
        sharePost.setHeader("Accept-Language", "zh-cn");
        sharePost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        sharePost.setEntity(new UrlEncodedFormEntity(shareParameters, "UTF-8"));
        DefaultHttpClient httpclient = HttpClientUtils.getHttpClient();
        HttpResponse loginPostRes = httpclient.execute(sharePost);
        String shareHtml = HttpClientUtils.getHtml(loginPostRes, "UTF-8", false);
        return shareHtml;
    }

    public static void main(String[] args) throws Exception {
        if (true) {
            String access_token = "oauth_signature=H2we5IWQd%2BG7mlrGHs2DVkFhdYc%3D&oauth_token=12323181257513439816&oauth_token_secret=f3yCdgAD4En8Vcks&openid=6ED93B1934A4178F6E799C5180484D26&timestamp=1009145739";
            Map<String, String> params = new HashMap<String, String>();
            params.put("images", "http://mat1.gtimg.com/www/iskin960/qqcomlogo.png");
            params.put("title", "腾讯登录连接测试");
            params.put("url", "http://connect.opensns.qq.com/?t=" + Math.random());
            String html = new ShareToken().share(access_token, params);
            System.out.println(html);
        }
    }
}
