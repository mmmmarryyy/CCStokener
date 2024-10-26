import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * @author li.li
 * 
 *         Apr 5, 2012
 * 
 */
public class BaiduUtils {

    public static final String ENCODING = "utf-8";

    public static final String IMAGE_PATH = BaiduUtils.class.getClassLoader().getResource("").getPath() + "temp.jpg";

    public static Result post(String url, Map<String, String> headers, Map<String, String> params, String encoding) throws ClientProtocolException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (String temp : params.keySet()) {
            list.add(new BasicNameValuePair(temp, params.get(temp)));
        }
        HttpPost post = new HttpPost(url);
        if (null != headers) post.setHeaders(assemblyHeader(headers));
        post.setEntity(new UrlEncodedFormEntity(list, encoding));
        return request(client, post);
    }

    public static Result sendGet(String url, Map<String, String> headers, Map<String, String> params, String encoding) throws ClientProtocolException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        url = url + (null == params ? "" : assemblyParameter(params));
        HttpGet get = new HttpGet(url);
        if (null != headers) get.setHeaders(assemblyHeader(headers));
        return request(client, get);
    }

    private static Result request(AbstractHttpClient client, HttpUriRequest request) throws ClientProtocolException, IOException {
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        Result result = new Result();
        result.setStatusCode(response.getStatusLine().getStatusCode());
        result.setHeaders(response.getAllHeaders());
        result.setCookie(assemblyCookie(client.getCookieStore().getCookies()));
        result.setHttpEntity(entity);
        return result;
    }

    public static Result reply(String content, String postsUrl, String cookie) throws ClientProtocolException, IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", cookie);
        String html = EntityUtils.toString(sendGet(postsUrl, headers, null, ENCODING).getHttpEntity(), ENCODING);
        String needParametersResolve[] = HtmlParse.prase(html, "kw:'.+',ie:'.+',rich_text:'\\d+',floor_num:'\\d+',fid:'\\d+',tid:'\\d+'").get(0).replaceAll("'", "").split(",");
        String floor_num = needParametersResolve[3].split(":")[1];
        String fid = needParametersResolve[4].split(":")[1];
        String tid = needParametersResolve[5].split(":")[1];
        String kw = needParametersResolve[0].split(":")[1];
        String vk_code = EntityUtils.toString(sendGet("http://tieba.baidu.com/f/user/json_vcode?lm=" + fid + "&rs10=2&rs1=0&t=0.5954980056343667", null, null, ENCODING).getHttpEntity(), ENCODING);
        String code = vk_code.split("\"")[7];
        String tbs = EntityUtils.toString(sendGet("http://tieba.baidu.com/dc/common/tbs?t=0.17514605234768638", headers, null, ENCODING).getHttpEntity(), ENCODING).split("\"")[3];
        VerificationcCode.showGetVerificationcCode("http://tieba.baidu.com/cgi-bin/genimg?" + code, null, IMAGE_PATH);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("add_post_submit", "发 表 ");
        parameters.put("kw", kw);
        parameters.put("floor_num", floor_num);
        parameters.put("ie", "utf-8");
        parameters.put("rich_text", "1");
        parameters.put("hasuploadpic", "0");
        parameters.put("fid", fid);
        parameters.put("rich_text", "1");
        parameters.put("tid", tid);
        parameters.put("hasuploadpic", "0");
        parameters.put("picsign", "");
        parameters.put("quote_id", "0");
        parameters.put("useSignName", "on");
        parameters.put("content", content);
        parameters.put("vcode_md5", code);
        parameters.put("tbs", tbs);
        parameters.put("vcode", JOptionPane.showInputDialog(null, "<html><img src=\"file:///" + IMAGE_PATH + "\" width=\33\" height=\55\"><br><center>请输入验证码</center><br></html>"));
        Result res = post("http://tieba.baidu.com/f/commit/post/add", headers, parameters, "utf-8");
        return res;
    }

    public static Header[] assemblyHeader(Map<String, String> headers) {
        Header[] allHeader = new BasicHeader[headers.size()];
        int i = 0;
        for (String str : headers.keySet()) {
            allHeader[i] = new BasicHeader(str, headers.get(str));
            i++;
        }
        return allHeader;
    }

    public static String assemblyCookie(List<Cookie> cookies) {
        StringBuffer sbu = new StringBuffer();
        for (Cookie cookie : cookies) {
            sbu.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
        }
        if (sbu.length() > 0) sbu.deleteCharAt(sbu.length() - 1);
        return sbu.toString();
    }

    public static String assemblyParameter(Map<String, String> parameters) {
        String para = "?";
        for (String str : parameters.keySet()) {
            para += str + "=" + parameters.get(str) + "&";
        }
        return para.substring(0, para.length() - 1);
    }

    public static void main(String[] args) {
        System.out.println(IMAGE_PATH);
    }
}
