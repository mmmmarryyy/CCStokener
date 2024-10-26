package com.hmw.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * Java 抓取网页内容
 * <br><b>创建日期</b>：2011-3-2
 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
 */
public class FetchHtml {

    public static void main(String[] args) throws UnsupportedEncodingException {
        FetchHtml fetch = new FetchHtml();
        String html = null;
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("sourceid", "chrome");
        parameterMap.put("ie", "UTF-8");
        parameterMap.put("q", "火车票");
        html = fetch.htmlContentByApachePost("http://www.google.com.hk/search", parameterMap);
        System.out.println(html);
    }

    /**
	 * 最简单的抓取网页内容的方法<br>
	 * 这种方法抓取一般的网页应该没有问题，但当有些网页中存在一些嵌套的redirect连接时，它就会报Server redirected too many
	 * times这样的错误，这是因为此网页内部又有一些代码是转向其它网页的，循环过多导致程序出错。 <br>
	 * <b>创建日期</b>：2011-1-20
	 * @param urlStr 网页的 URL
	 * @param charset 网页编码
	 * @return
	 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
	 */
    public String htmlContentSimple(String urlStr, String charset) {
        StringBuffer html = new StringBuffer();
        URL url = null;
        BufferedReader reader = null;
        try {
            url = new URL(urlStr);
            reader = new BufferedReader(new InputStreamReader(url.openStream(), charset));
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append("\r\n");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return html.toString();
    }

    /**
	 * 抓取网页内容，当有些网页中存在一些嵌套的redirect连接时，此方法只会抓取指定 URL 对应网页中的内容，而不是抓取跳转后的网页中的内容
	 * <br><b>创建日期</b>：2011-1-20
	 * @param urlStr
	 * @param charset
	 * @return
	 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
	 */
    public String htmlContent(String urlStr, String charset) {
        StringBuffer html = new StringBuffer();
        BufferedReader reader = null;
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
            con.setInstanceFollowRedirects(false);
            con.connect();
            reader = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append("\r\n");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html.toString();
    }

    /**
	 * 用 apache 提供的包抓取网页内容, Get 形式抓取，需要用到如下依赖包：commons-codec-1.4.jar、commons-httpclient-3.1.jar、commons-logging-1.0.4.jar<br> 
	 * <br><b>创建日期</b>：2011-1-20
	 * @param urlStr 要抓取的网页的 URL
	 * @return
	 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
	 */
    public String htmlContentByApacheGet(String url) {
        try {
            GetMethod getMethod = new GetMethod(url);
            new HttpClient().executeMethod(getMethod);
            return getMethod.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * 用 apache 提供的包抓取网页内容, Post 形式抓取
	 * <br><b>创建日期</b>：2011-1-20
	 * @param urlStr 要抓取的网页的 URL
	 * @param charset 网页编码
	 * @return
	 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
	 * @see #htmlContentByApachePost(String, String, Map)
	 */
    public String htmlContentByApachePost(String url) {
        return htmlContentByApachePost(url, null);
    }

    /**
	 * 用 apache 提供的包抓取网页内容, Post 形式抓取，需要用到如下依赖包：commons-codec-1.4.jar、commons-httpclient-3.1.jar、commons-logging-1.0.4.jar<br> 
	 * <Strong style="color:#f00">不知道为什么用 Post 方法抓取时好多网站都抓取不到对应的网页内容</Strong>
	 * <br><b>创建日期</b>：2011-1-20
	 * @param urlStr 要抓取的网页的 URL
	 * @param charset 网页编码
	 * @param parameterMap 请求参数
	 * @return
	 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
	 */
    public String htmlContentByApachePost(String url, Map<String, String> parameterMap) {
        NameValuePair[] data = null;
        if (!(parameterMap == null || parameterMap.isEmpty())) {
            data = new NameValuePair[parameterMap.size()];
            int index = 0;
            for (Iterator<Entry<String, String>> i = parameterMap.entrySet().iterator(); i.hasNext(); index++) {
                Entry<String, String> param = i.next();
                data[index] = new NameValuePair(param.getKey(), param.getValue());
            }
        }
        try {
            PostMethod postMethod = new PostMethod(url);
            if (data != null) postMethod.setRequestBody(data);
            new HttpClient().executeMethod(postMethod);
            return postMethod.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
