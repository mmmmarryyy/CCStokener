package login;

import java.io.File;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * 使用HttpClient4模拟新浪微博登录+发微博
 * @author liuy
 */
public class Login {

    private static DefaultHttpClient client = new DefaultHttpClient();

    /**
	 * get
	 */
    public static String get(String url) throws Throwable {
        HttpGet get = new HttpGet(url);
        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, HTTP.UTF_8);
        get.abort();
        System.out.println("INFO get method: " + url + "\ncontentLength:" + result.length());
        return result;
    }

    /**
	 * 获取 HttpPost
	 */
    public static HttpPost getHttpPost(String url, String uid) {
        System.out.println(url);
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux i686; rv:5.0) Gecko/20100101 Firefox/5.0");
        post.setHeader("Referer", "http://weibo.com/" + uid);
        return post;
    }

    /**
	 * 模拟登录
	 */
    public static String login(String username, String password) throws Throwable {
        long timeStamp = new Date().getTime();
        String preLoginUrl = "http://login.sina.com.cn/sso/prelogin.php?entry=miniblog&callback=sinaSSOController.preloginCallBack&client=ssologin.js(v1.3.19)&_=" + timeStamp;
        String result = get(preLoginUrl);
        String json = result.subSequence(35, result.length() - 1).toString();
        JSONObject obj = (JSONObject) JSONValue.parse(json);
        String servertime = obj.get("servertime").toString();
        String nonce = obj.get("nonce").toString();
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("entry", "weibo"));
        qparams.add(new BasicNameValuePair("gateway", "1"));
        qparams.add(new BasicNameValuePair("from", ""));
        qparams.add(new BasicNameValuePair("savestate", "7"));
        qparams.add(new BasicNameValuePair("useticket", "1"));
        qparams.add(new BasicNameValuePair("ssosimplelogin", "1"));
        qparams.add(new BasicNameValuePair("service", "miniblog"));
        qparams.add(new BasicNameValuePair("pwencode", "wsse"));
        qparams.add(new BasicNameValuePair("vsnf", "1"));
        qparams.add(new BasicNameValuePair("vsnval", ""));
        qparams.add(new BasicNameValuePair("servertime", servertime));
        qparams.add(new BasicNameValuePair("nonce", nonce));
        qparams.add(new BasicNameValuePair("encoding", HTTP.UTF_8));
        qparams.add(new BasicNameValuePair("url", "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
        qparams.add(new BasicNameValuePair("returntype", "META"));
        qparams.add(new BasicNameValuePair("su", Base64.encodeBase64String(URLEncoder.encode(username, HTTP.UTF_8).getBytes())));
        qparams.add(new BasicNameValuePair("sp", new SinaSSOEncoder().encode(password, servertime, nonce)));
        UrlEncodedFormEntity params = new UrlEncodedFormEntity(qparams, HTTP.UTF_8);
        HttpPost post = getHttpPost("http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.3.19)", StringUtils.EMPTY);
        post.setEntity(params);
        HttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        String location = getRedirectLocation(EntityUtils.toString(entity, HTTP.UTF_8));
        post.abort();
        String ajaxLoginResponse = get(location);
        String uuId = getUniqueid(ajaxLoginResponse);
        return uuId;
    }

    /**
	 * 获取重定向链接
	 */
    private static String getRedirectLocation(String content) {
        String regex = "location\\.replace\\(\'(.*?)\'\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    /**
	 * 获取用户id
	 */
    private static String getUniqueid(String ajaxLoginResponse) {
        int start = ajaxLoginResponse.indexOf("uniqueid") + 11;
        int end = ajaxLoginResponse.indexOf("userid") - 3;
        return ajaxLoginResponse.substring(start, end);
    }

    /**
	 * 发围脖
	 */
    public static String addWeibo(String weibo, File pic, String uid) throws Throwable {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("_surl", ""));
        qparams.add(new BasicNameValuePair("_t", "0"));
        qparams.add(new BasicNameValuePair("location", "home"));
        qparams.add(new BasicNameValuePair("module", "stissue"));
        if (pic != null) {
            String picId = upLoadImg(pic, uid);
            qparams.add(new BasicNameValuePair("pic_id", picId));
        }
        qparams.add(new BasicNameValuePair("rank", "weibo"));
        qparams.add(new BasicNameValuePair("text", weibo));
        HttpPost post = getHttpPost("http://weibo.com/aj/mblog/add?__rnd=1333611402611", uid);
        UrlEncodedFormEntity params = new UrlEncodedFormEntity(qparams, HTTP.UTF_8);
        post.setEntity(params);
        HttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity, HTTP.UTF_8);
        post.abort();
        return content;
    }

    /**
	 * 上传图片,返回pid
	 */
    public static String upLoadImg(File pic, String uid) throws Throwable {
        System.out.println("开始上传=======================================================");
        HttpPost post = getHttpPost(getUploadUrl(uid), uid);
        FileBody file = new FileBody(pic, "image/jpg");
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("pic1", file);
        post.setEntity(reqEntity);
        HttpResponse response = client.execute(post);
        int status = response.getStatusLine().getStatusCode();
        post.abort();
        if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_MOVED_PERMANENTLY) {
            String newuri = response.getHeaders("location")[0].getValue();
            System.out.println(newuri);
            return newuri.substring(newuri.indexOf("pid=") + 4, newuri.indexOf("&token="));
        }
        return null;
    }

    /**
	 * 获取nick
	 */
    public static String getNick(String uid) throws Throwable {
        String content = get("http://weibo.com/" + uid);
        String regex = "\\$CONFIG\\[\\'nick\\'\\]\\ \\=\\ \\'(.*?)\\'\\;";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    /**
	 * 组装上传url
	 */
    public static String getUploadUrl(String uid) throws Throwable {
        String nick = URLEncoder.encode("@" + getNick(uid), HTTP.UTF_8);
        String cb = URLEncoder.encode("http://weibo.com/aj/static/upimgback.html?callback=STK_ijax_" + new Date().getTime() + "435", HTTP.UTF_8);
        return MessageFormat.format("http://picupload.service.weibo.com/interface/pic_upload.php?cb={0}&marks=1&app=miniblog&s=rdxt&url=0&markpos=1&logo=0&nick={1}", cb, nick);
    }

    public static void main(String[] args) {
        try {
            String uid = Login.login("77804248@qq.com", "XXXXXX");
            File pic = new File("f:\\1.jpg");
            String content = Login.addWeibo(String.valueOf(new Date().getTime()) + "@任志强", pic, uid);
            System.out.println(content);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
