package com.litt.core.net.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import com.litt.core.common.Utility;

/**
 * <b>标题:</b> URL功能类.
 * <pre><b>描述:</b> 
 *   1.对URL中文进行转码,2.对URL中文进行反转码，需要JDK5以上支持
 * </pre>
 * 
 * 
 * @author <a href="mailto:littcai@hotmail.com">空心大白菜</a>
 * @since 2006-09-07
 * @version 1.0
 */
public class URLUtil {

    private static final Logger logger = Logger.getLogger(URLUtil.class);

    /**
     * 对URL中文进行转码 一般get方式查询参数用到
     * @param str 中文字符串
     * @param coder 编码方式，如 UTF-8\GBK
     * @return
     */
    public static String urlEncoder(String url, String coder) {
        url = Utility.trimNull(url);
        if (coder == null || coder.equals("")) {
            coder = "UTF-8";
        }
        try {
            url = java.net.URLEncoder.encode(url, coder);
        } catch (UnsupportedEncodingException e) {
            logger.error("URL编码出错，编码类型 - " + coder, e);
        }
        return url;
    }

    /**
     * 对URL中文进行反转码，一般get方式查询参数用到
     * @param str 中文字符串
     * @param coder 编码方式，如 UTF-8\GBK
     * @return
     */
    public static String urlDecoder(String url, String coder) {
        url = Utility.trimNull(url);
        if (coder == null || coder.equals("")) {
            coder = "UTF-8";
        }
        try {
            url = java.net.URLDecoder.decode(url, coder);
        } catch (UnsupportedEncodingException e) {
            logger.error("URL解码出错，编码类型 - " + coder, e);
        }
        return url;
    }

    /**
     * 取得请求带参数的URL的方法
     * @param request
     * @return
     */
    public static String getUrl(HttpServletRequest request) {
        StringBuffer originalURL = request.getRequestURL();
        Map parameters = request.getParameterMap();
        if (parameters != null && parameters.size() > 0) {
            originalURL.append("?");
            for (Iterator iter = parameters.keySet().iterator(); iter.hasNext(); ) {
                String key = (String) iter.next();
                String[] values = (String[]) parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    originalURL.append(key).append("=").append(values[i]).append("&");
                }
            }
        }
        return originalURL.toString();
    }

    /**
     * 取得基于主机名的url，定向根目录的域名。（端口自动识别）
     * @param request
     * @return
     */
    public static String getServerUrl(HttpServletRequest request) {
        String str = "http://" + request.getServerName();
        int port = request.getServerPort();
        if (port != 80) {
            str = str + ":" + String.valueOf(port);
        }
        return str;
    }

    /**
     * 根据提供的域名地址返回网页html内容
     * @param remoteUrl
     * @return
     */
    public static String getWebContent(String remoteUrl) {
        StringBuffer sb = new StringBuffer();
        try {
            java.net.URL url = new java.net.URL(remoteUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
        } catch (Exception e) {
            logger.error("获取远程网址内容失败 - " + remoteUrl, e);
        }
        return sb.toString();
    }

    /**
     * 根据提供的域名地址返回网页html内容。指定编码类型
     * @param remoteUrl 
     * @param encoding 编码类型
     * @return
     */
    public static String getWebContent(String remoteUrl, String encoding) {
        StringBuffer sb = new StringBuffer();
        try {
            java.net.URL url = new java.net.URL(remoteUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), encoding));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
        } catch (Exception e) {
            logger.error("获取远程网址内容失败 - " + remoteUrl, e);
        }
        return sb.toString();
    }

    /**
     * 获得HttpServletRequest的头信息
     * @param request
     * @param str
     * @return
     */
    public static String getHeader(HttpServletRequest request, String str) {
        String s = "";
        try {
            s = request.getHeader(str);
        } catch (Exception e) {
            logger.error("获取网页头内容失败 - ", e);
        }
        return s;
    }

    public static void post(String action, String formData) throws Exception {
        URL url = new URL(action);
        SocketAddress addr = new InetSocketAddress("210.51.14.197", 80);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
        URLConnection conn = url.openConnection(proxy);
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("proxyHost", "210.51.14.197");
        System.getProperties().put("proxyPort", "80");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(formData);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
        wr.close();
        rd.close();
    }

    public static void main(String[] args) throws Exception {
        String data = URLEncoder.encode("xuhao", "GBK") + "=18";
        post("http://www.harbinyouth.com.cn/UploadFiles/tp/save.asp", data);
    }
}
