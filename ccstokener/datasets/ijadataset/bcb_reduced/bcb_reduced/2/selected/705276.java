package com.bitcomm;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取本机外网IP地址 思想是访问网站http://checkip.dyndns.org/，得到返回的文本后解析出本机在外网的IP地址
 * 
 * @author Administrator
 * 
 */
public class ExternalIPFetcher {

    private String myExternalIpAddress;

    public ExternalIPFetcher(String externalIpProviderUrl) {
        myExternalIpAddress = null;
        String returnedhtml = fetchExternalIpProviderHTML(externalIpProviderUrl);
        parse(returnedhtml);
    }

    /**
	 * 从外网提供者处获得包含本机外网地址的字符串 从http://checkip.dyndns.org返回的字符串如下
	 * <html><head><title>Current IP Check</title></head><body>Current IP
	 * Address: 123.147.226.222</body></html>
	 * 
	 * @param externalIpProviderUrl
	 * @return
	 */
    private String fetchExternalIpProviderHTML(String externalIpProviderUrl) {
        InputStream in = null;
        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(externalIpProviderUrl);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setConnectTimeout(20000);
            httpConn.setReadTimeout(10000);
            HttpURLConnection.setFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
            in = httpConn.getInputStream();
            byte[] bytes = new byte[1024];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            String receivedString = new String(bytes, "UTF-8");
            return receivedString;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                httpConn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
	 * 使用正则表达式解析返回的HTML文本,得到本机外网地址
	 * 
	 * @param html
	 */
    private void parse(String html) {
        Pattern pattern = Pattern.compile("(\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})", Pattern.CASE_INSENSITIVE);
        if (html == null) return;
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            myExternalIpAddress = matcher.group(0);
        }
    }

    /**
	 * 得到本机外网地址,得不到则为空
	 * 
	 * @return
	 */
    public String getMyExternalIpAddress() {
        return myExternalIpAddress;
    }
}
