package dg.core.util.taobao;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * 获取淘宝API返回的结果
 * 
 * @author Nanlei
 * 
 */
public class GetResult {

    private static final String URL = "http://gw.api.taobao.com/router/rest";

    private static final String APP_KEY = "12147025";

    private static final String APP_SECRET = "74c43a5b07e8662b22488c5f94e32d43";

    private static final String FORMAT = "xml";

    private static final String METHOD = "taobao.item.get";

    private static final String VERSION = "2.0";

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
	 * 获取结果的方法
	 * 
	 * @param fields
	 *            需要请求的商品字段
	 * @param num_iid
	 *            商品ID，淘宝网URL中获得
	 * @return
	 */
    public static String getResult(String fields, String num_iid) {
        String content = null;
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(URL);
        String timestamp = getFullTime();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("app_key", APP_KEY));
        params.add(new BasicNameValuePair("format", FORMAT));
        params.add(new BasicNameValuePair("method", METHOD));
        params.add(new BasicNameValuePair("num_iid", num_iid));
        params.add(new BasicNameValuePair("fields", fields));
        params.add(new BasicNameValuePair("timestamp", timestamp));
        params.add(new BasicNameValuePair("partner_id", "911"));
        params.add(new BasicNameValuePair("v", VERSION));
        String sign = getSignature(fields, num_iid);
        params.add(new BasicNameValuePair("sign", sign));
        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_IMPLEMENTED) {
                System.err.println("The Post Method is not implemented by this URI");
            } else {
                content = EntityUtils.toString(response.getEntity());
            }
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return content + sign;
    }

    public static String getSignature(String fields, String num_iid) {
        String timestamp = getFullTime();
        return SignatureGenerator.getMD5Signature(getParams(timestamp, fields, num_iid), APP_SECRET);
    }

    /**
	 * 拼装参数
	 * 
	 * @param timestamp
	 *            当前时间戳
	 * @param fields
	 *            需要请求的商品字段
	 * @param num_iid
	 *            商品ID，淘宝网URL中获得
	 * @return
	 */
    public static TreeMap<String, String> getParams(String timestamp, String fields, String num_iid) {
        TreeMap<String, String> treeMap = new TreeMap<String, String>();
        treeMap.put("timestamp", timestamp);
        treeMap.put("v", VERSION);
        treeMap.put("app_key", APP_KEY);
        treeMap.put("method", METHOD);
        treeMap.put("partner_id", "911");
        treeMap.put("format", FORMAT);
        treeMap.put("fields", fields);
        treeMap.put("num_iid", num_iid);
        return treeMap;
    }

    /**
	 * 获取格式化好的时间
	 * 
	 * @return
	 */
    public static String getFullTime() {
        return df.format(new java.util.Date());
    }
}
