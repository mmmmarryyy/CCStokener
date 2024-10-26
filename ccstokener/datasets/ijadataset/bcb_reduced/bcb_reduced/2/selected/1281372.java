package com.diipo.weibo.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import com.diipo.weibo.ConfigInfo;
import com.diipo.weibo.entity.Check_url;

public class HttpUtils {

    /**
	 * get方式请求连接
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
    public static String doGet(String url, String user, String passd) throws ClientProtocolException, IOException {
        String result = "";
        HttpGet httpRequest = new HttpGet(url);
        if (!user.equals("") && !passd.equals("")) {
            httpRequest.setHeader("Authorization", "Basic " + Base64.encodeBytes((user + ":" + passd).getBytes()));
        }
        HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            result = EntityUtils.toString(httpResponse.getEntity());
        }
        if (httpResponse.getStatusLine().getStatusCode() == 401) {
            result = ConfigInfo.HTTPRETURN.HTTP_ERROR_401;
        } else if (result.equals("")) {
            result = ConfigInfo.HTTPRETURN.HTTPERROR;
        }
        return result;
    }

    /**
	 * 验证修改后的域名地址是否有效
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
    public static boolean check_url(String url) {
        HttpGet httpRequest = new HttpGet(url);
        HttpResponse httpResponse = null;
        String result = "";
        try {
            httpResponse = new DefaultHttpClient().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(httpResponse.getEntity());
                Check_url cu = JsonUtils.parseUserFromJson(result, Check_url.class);
                if (cu.getCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
	 * 验证修改后的域名地址是否有效
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
    public static String[] get_url(String url) {
        HttpGet httpRequest = new HttpGet(url);
        HttpResponse httpResponse = null;
        String result = "";
        String s[] = null;
        String add[] = null;
        try {
            httpResponse = new DefaultHttpClient().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(httpResponse.getEntity());
                result = result.substring(1, result.length() - 1);
                s = result.split(",");
                add = new String[s.length];
                for (int i = 0; i < s.length; i++) {
                    add[i] = s[i].substring(1, s[i].length() - 1);
                }
            }
        } catch (Exception e) {
            return add;
        }
        return add;
    }

    /**
	 * POST 方式请求连接
	 * 
	 * @param url
	 *            连接地址
	 * @param params
	 *            用户参数
	 * 
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
    public static String doPost(String url, String user, String passd, Map params) {
        HttpPost httpRequest = new HttpPost(url);
        String result = "";
        if (!user.equals("") && !passd.equals("")) {
            httpRequest.setHeader("Authorization", "Basic " + Base64.encodeBytes((user + ":" + passd).getBytes()));
        }
        List<BasicNameValuePair> list = new ArrayList<BasicNameValuePair>();
        if (params != null) {
            Set set = params.keySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                if (!set.isEmpty()) {
                    list.add(new BasicNameValuePair(iterator.next().toString(), params.get(iterator.next()).toString()));
                }
            }
        }
        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(list, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                return result = EntityUtils.toString(httpResponse.getEntity());
            } else if (httpResponse.getStatusLine().getStatusCode() == 401) {
                return result = ConfigInfo.HTTPRETURN.HTTP_ERROR_401;
            } else if (httpResponse.getStatusLine().getStatusCode() == 400) {
                return result = ConfigInfo.HTTPRETURN.HTTP_ERROR_400;
            } else if (httpResponse.getStatusLine().getStatusCode() == 404) {
                return "404";
            } else if (result.equals("")) {
                return result = ConfigInfo.HTTPRETURN.HTTPERROR;
            } else {
                return result = ConfigInfo.HTTPRETURN.COMMHTTPERRORS;
            }
        } catch (Exception e) {
            return result = ConfigInfo.HTTPRETURN.COMMHTTPERRORS;
        }
    }

    public static String doPost2(String url, String user, String passd, List<BasicNameValuePair> list) {
        HttpPost httpRequest = new HttpPost(url);
        String result = "";
        if (!user.equals("") && !passd.equals("")) {
            httpRequest.setHeader("Authorization", "Basic " + Base64.encodeBytes((user + ":" + passd).getBytes()));
        }
        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(list, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                System.out.println(httpResponse.getStatusLine().getStatusCode());
                return result = EntityUtils.toString(httpResponse.getEntity());
            } else if (httpResponse.getStatusLine().getStatusCode() == 401) {
                return result = ConfigInfo.HTTPRETURN.HTTP_ERROR_401;
            } else if (result.equals("")) {
                return result = ConfigInfo.HTTPRETURN.HTTPERROR;
            } else if (httpResponse.getStatusLine().getStatusCode() == 400) {
                return result = "";
            } else {
                return result = ConfigInfo.HTTPRETURN.COMMHTTPERRORS;
            }
        } catch (Exception e) {
            return result = ConfigInfo.HTTPRETURN.COMMHTTPERRORS;
        }
    }
}
