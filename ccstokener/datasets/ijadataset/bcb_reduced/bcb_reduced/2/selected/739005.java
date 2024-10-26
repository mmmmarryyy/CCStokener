package leeon.kaixin.wap.action;

import java.util.ArrayList;
import java.util.List;
import leeon.kaixin.wap.util.HttpUtil;
import leeon.mobile.BBSBrowser.ContentException;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.utils.HTMLUtil;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class LoginAction {

    public static String home() throws NetworkException {
        HttpClient client = HttpUtil.newInstance();
        HttpGet get = new HttpGet(HttpUtil.KAIXIN_URL);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            if (HTTPUtil.isHttp200(response)) {
                String ret = HTTPUtil.toString(response.getEntity());
                ret = HTMLUtil.findStr(ret, HttpUtil.KAIXIN_LOGIN_URL, "\"");
                ret = ret.replace("&amp;", "&");
                return ret;
            } else throw new NetworkException();
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static String login(String user, String password) throws ContentException, NetworkException {
        return login(user, password, null);
    }

    public static String login(String user, String password, String code) throws ContentException, NetworkException {
        HttpClient client = HttpUtil.newInstance();
        HttpPost post = new HttpPost(HttpUtil.KAIXIN_LOGIN_URL + (code == null ? "" : code));
        HttpResponse response = null;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", user));
        nvps.add(new BasicNameValuePair("password", password));
        nvps.add(new BasicNameValuePair("login", "登录"));
        HttpUtil.setHeader(post);
        try {
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            response = client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
        if (HTTPUtil.isHttp200(response)) {
            String ret = HTTPUtil.toString(response.getEntity());
            ret = HTMLUtil.findStrRegex(ret, "verify=", "[^\\w]");
            if (ret == null) throw new ContentException("账户或者密码不正确");
            return ret;
        } else if (HTTPUtil.isHttp302(response)) {
            throw new ContentException("账户或者密码不正确");
        } else {
            throw new NetworkException();
        }
    }

    public static String login4www(String user, String password) throws ContentException, NetworkException {
        HttpClient client = HttpUtil.newInstance();
        HttpPost post = new HttpPost(HttpUtil.KAIXIN_WWW_LOGIN_URL);
        HttpResponse response = null;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", user));
        nvps.add(new BasicNameValuePair("password", password));
        HttpUtil.setHeader(post);
        try {
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            response = client.execute(post);
            HTTPUtil.consume(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
        if (HTTPUtil.isHttp200(response)) {
            throw new ContentException("账户或者密码不正确");
        } else if (HTTPUtil.isHttp302(response)) {
            Header h = response.getFirstHeader("Location");
            if (h != null) return h.getValue(); else throw new NetworkException();
        } else {
            throw new NetworkException();
        }
    }

    public static void home4www(String location) throws NetworkException {
        HttpClient client = HttpUtil.newInstance();
        HttpGet get = new HttpGet(HttpUtil.KAIXIN_WWW_URL + location);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            HTTPUtil.consume(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static void logout(String verify) throws NetworkException {
        HttpClient client = HttpUtil.newInstance();
        HttpGet get = new HttpGet(HttpUtil.KAIXIN_LOGOUT_URL + HttpUtil.KAIXIN_PARAM_VERIFY + verify);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            if (response != null && response.getEntity() != null) {
                HTTPUtil.consume(response.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static void logout4www() throws NetworkException {
        HttpClient client = HttpUtil.newInstance();
        HttpGet get = new HttpGet(HttpUtil.KAIXIN_WWW_LOGOUT_URL);
        HttpUtil.setHeader(get);
        try {
            HttpResponse response = client.execute(get);
            if (response != null && response.getEntity() != null) {
                HTTPUtil.consume(response.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NetworkException(e);
        }
    }

    public static String uid(String verify) {
        if (verify == null || verify.length() == 0) return null;
        return verify.substring(0, verify.indexOf("_"));
    }

    public static String uicon(String uid) {
        if (uid == null || uid.length() == 0) return null;
        if (uid.length() == 7) {
            String s1 = uid.substring(1, 3);
            String s2 = uid.substring(3, 5);
            return "http://pic.kaixin001.com.cn/logo/" + s1 + "/" + s2 + "/50_" + uid + "_1.jpg";
        } else if (uid.length() == 8) {
            String s1 = uid.substring(2, 4);
            String s2 = uid.substring(4, 6);
            return "http://pic.kaixin001.com.cn/logo/" + s1 + "/" + s2 + "/50_" + uid + "_1.jpg";
        } else {
            return "http://pic.kaixin001.com.cn/logo/01/01/50_01_01.jpg";
        }
    }

    /**
	 * @param args
	 * @throws NetworkException 
	 * @throws ContentException 
	 */
    public static void main(String[] args) throws ContentException, NetworkException {
    }
}
