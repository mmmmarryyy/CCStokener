package com.coyousoft.wangyu.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.sysolar.util.array.ByteArray;

public final class HtmlReader {

    private static final Log log = LogFactory.getLog(HtmlReader.class);

    private static final boolean DEBUG = log.isDebugEnabled();

    private static final boolean isProxyNeeded = false;

    public static final int TIME_OUT = 30000;

    public static final String DEFAULT_CHARSET = "GB18030";

    private static final Pattern P_HEAD;

    private static final Pattern P_CHARSET;

    private static final Pattern P_TITLE;

    private static final Pattern P_DESCRIPTION;

    private static final Pattern P_KEYWORDS;

    private static final Pattern P_CONTENT;

    private static final Pattern P_TAIL;

    private static final Pattern P_ICON_LINK;

    static {
        if (isProxyNeeded) {
            initProxy();
        }
        P_HEAD = Pattern.compile("<head[\\s>]+.+</head>", Pattern.CASE_INSENSITIVE);
        String s = "<meta [^<]*charset\\s*=\\s*[\"\']?([\\w\\-]+)[\"\']?\\s*[^<]*/?>";
        P_CHARSET = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
        P_TITLE = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        s = "<meta\\s+[^<>]*name\\s*=\\s*((\"\\s*description\\s*\")|(\'\\s*description\\s*\'))[^<>]*/?>";
        P_DESCRIPTION = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
        s = "<meta\\s+[^<>]*name\\s*=\\s*((\"\\s*keywords\\s*\")|(\'\\s*keywords\\s*\'))[^<>]*/?>";
        P_KEYWORDS = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
        s = "\\s+content\\s*=\\s*((\"[^\"]+\")|(\'[^\']+\')|([^<>/]+))";
        P_CONTENT = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
        s = "<link\\s+[^<>]*href\\s*=\\s*[\"\']?([\\-\\.:/\\w]+\\.((ico)|(png)|(gif)))[\"\']?[^<>]*/?>";
        P_ICON_LINK = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
        P_TAIL = Pattern.compile("[=\"\']+", Pattern.CASE_INSENSITIVE);
    }

    /**
     * 抓取网页head里的内容。
     * 
     * @param url
     * @return [0] - title, [1] - description, [2] - keywords
     * @throws Exception 
     */
    public static String[] parse(String url, Integer timeOut) throws Exception {
        String head = parseHead(url, timeOut);
        String[] arr = new String[] { findTitle(head), findDescription(head), findKeywords(head) };
        for (int i = 0; i < arr.length; i++) {
            if (null != arr[i]) {
                arr[i] = arr[i].replace('\'', ' ');
            }
        }
        return arr;
    }

    public static String parseHead(String url) throws Exception {
        return parseHead(url, null);
    }

    /**
     * 解析位于 <head> </head> 标签里的内容。
     * 
     * @param url
     * @return
     * @throws Exception 
     */
    public static String parseHead(String url, Integer timeOut) throws Exception {
        byte[] head = findHead(url, timeOut);
        String charset = findCharset(head);
        if ("gbk".equalsIgnoreCase(charset) || "gb2312".equalsIgnoreCase(charset)) {
            charset = "GB18030";
        }
        String result = null;
        String tmpResult = null;
        try {
            result = new String(head, charset).replaceAll("\\s+", " ");
            if (result.indexOf('�') != -1 || result.indexOf('') != -1) {
                String tmpCharset = "UTF-8".equalsIgnoreCase(charset) || "UTF8".equalsIgnoreCase(charset) ? "GB18030" : "UTF-8";
                tmpResult = new String(head, tmpCharset);
            }
            if (tmpResult != null) {
                if (tmpResult.indexOf('�') == -1 && tmpResult.indexOf('') == -1) {
                    result = tmpResult;
                } else {
                    int tmpErrCharCount = StringUtil.count(tmpResult, '�') + StringUtil.count(tmpResult, '');
                    int errCharCount = StringUtil.count(result, '�') + StringUtil.count(result, '');
                    result = tmpErrCharCount > errCharCount ? result : tmpResult;
                }
            }
        } catch (Exception e) {
            log.error(url, e);
        }
        if (DEBUG) {
            log.debug("result=" + result + ", charset=" + charset + ", url=" + url);
        }
        return result;
    }

    /**
     * 从 url 地址里提取网址标题，结果里不包含 <title> 和 </title> 标签。
     * 
     * @param url
     * @return
     * @throws Exception 
     */
    public static String parseTitle(String url, Integer timeOut) throws Exception {
        return findTitle(parseHead(url, timeOut));
    }

    public static String parseTitle(String url) throws Exception {
        return parseTitle(url, null);
    }

    /**
     * 解析 <meta content="" http-equiv="description"/> 里的 content 属性值。
     * 
     * @param head
     * @return
     * @throws Exception 
     */
    public static String parseDescription(String url) throws Exception {
        return findDescription(parseHead(url));
    }

    /**
     * 解析 <meta content=" " http-equiv="keywords"/> 里的 content 属性值。
     * 
     * @param head
     * @return
     * @throws Exception 
     */
    public static String parseKeywords(String url) throws Exception {
        return findKeywords(parseHead(url));
    }

    /**
     * 解析 icon 图标链接地址。
     * 
     * @param url
     * @return
     * @throws Exception
     */
    public static String parseIconLink(String url) throws Exception {
        return findIconLink(parseHead(url));
    }

    /**
     * 从 head 标签里读取网站编码，提取如下标签里 charset。
     * <meta content="text/html; charset=gb2312" http-equiv="Content-Type"/> 
     * 
     * @param head
     * @return
     */
    private static String findCharset(byte[] head) {
        String headString = new String(head).replaceAll("\\s+", " ");
        Matcher m = P_CHARSET.matcher(headString);
        if (m.find()) {
            return m.group(1);
        }
        return DEFAULT_CHARSET;
    }

    /**
     * 解析位于 <title> </title> 标签里的内容。
     * 
     * @param head
     * @return
     */
    private static String findTitle(String head) {
        if (head == null) {
            return null;
        }
        String title = null;
        try {
            Matcher m = P_TITLE.matcher(head.replaceAll("\\s+", " "));
            if (m.find()) {
                title = m.group(1).trim();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return title;
    }

    /**
     * 解析 <meta content="" http-equiv="description"/> 里的 content 属性值。
     * 
     * @param head
     * @return
     */
    private static String findDescription(String head) {
        if (head == null) {
            return null;
        }
        String description = null;
        try {
            Matcher m = P_DESCRIPTION.matcher(head);
            if (m.find()) {
                description = findContent(m.group());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return description;
    }

    /**
     * 解析 <meta content=" " http-equiv="keywords"/> 里的 content 属性值。
     * 
     * @param head
     * @return
     */
    private static String findKeywords(String head) {
        if (head == null) {
            return null;
        }
        String keywords = null;
        try {
            Matcher m = P_KEYWORDS.matcher(head);
            if (m.find()) {
                keywords = findContent(m.group());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return keywords;
    }

    /**
     * 获得 content 属性值。
     * 
     * @param src
     * @return
     */
    private static String findContent(String src) {
        String content = null;
        try {
            Matcher m = P_CONTENT.matcher(src);
            if (m.find()) {
                content = m.group().trim().substring(7);
            }
            if (content == null) {
                return null;
            }
            m = P_TAIL.matcher(content);
            content = m.replaceAll("").trim();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return content;
    }

    /**
     * 判断是否需要在网址后面添加斜线 / ，如果需要则添加。
     * 
     * @param url
     * @return
     */
    private static String appendSlash(String url) {
        if (url.lastIndexOf('/') == (url.indexOf('/') + 1)) {
            url = url + "/";
        }
        return url;
    }

    private static String findIconLink(String head) {
        if (head == null) {
            return null;
        }
        String iconLink = null;
        try {
            Matcher m = P_ICON_LINK.matcher(head);
            if (m.find()) {
                iconLink = m.group(1);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return iconLink;
    }

    public static boolean exist(String file) {
        boolean result = true;
        InputStream in = null;
        try {
            in = new URL(file).openStream();
        } catch (Exception e) {
            result = false;
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (null != in) in.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    /**
     * 抓取网站 icon 图标，并保存至本地。
     * 
     * @param url
     * @param filePath
     * @return
     */
    public static File saveIcon(String url, String filePath) {
        File iconFile = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        HttpEntity httpEntity = null;
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIME_OUT);
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
            HttpGet request = new HttpGet(url);
            HttpResponse response = HttpUtil.doGet(request, client);
            httpEntity = response.getEntity();
            if (null == httpEntity) {
                return null;
            }
            in = new BufferedInputStream(httpEntity.getContent());
            byte[] buffer = new byte[128];
            int length;
            boolean checked = false;
            while ((length = in.read(buffer)) != -1) {
                if (!checked) {
                    if (!ByteArray.startWith(buffer, new byte[] { 0, 0, 1, 0 }) && !ByteArray.startWith(buffer, new byte[] { -119, 80, 78, 71 }) && !ByteArray.startWith(buffer, new byte[] { 71, 73, 70, 56 })) {
                        break;
                    }
                    checked = true;
                }
                if (null == out) {
                    iconFile = new File(filePath);
                    out = new BufferedOutputStream(new FileOutputStream(iconFile));
                }
                out.write(buffer, 0, length);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            iconFile = null;
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (null != httpEntity) {
                try {
                    httpEntity.consumeContent();
                } catch (IOException e) {
                }
            }
            if (null != out) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return iconFile;
    }

    /**
     * 读取网络上的文件存储为本地文件。
     * 
     * @param fileName
     * @param url
     */
    public static void readAsFile(String fileName, String url) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        URLConnection conn = null;
        try {
            conn = new URL(url).openConnection();
            conn.setDoInput(true);
            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(fileName));
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (null != out) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 根据网页地址，读取其 head 标签的内容，结果里包含<head> 和 </head> 标签。
     * 
     * @param url
     * @return
     * @throws Exception 
     */
    public static byte[] findHead(String url, Integer timeOut) throws Exception {
        byte[] result = new byte[0];
        InputStream in = null;
        HttpEntity httpEntity = null;
        try {
            if (null == timeOut) {
                timeOut = TIME_OUT;
            }
            DefaultHttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeOut);
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeOut);
            HttpGet request = new HttpGet(appendSlash(url));
            HttpResponse response = HttpUtil.doGet(request, client);
            httpEntity = response.getEntity();
            if (null == httpEntity) {
                return null;
            }
            in = new BufferedInputStream(httpEntity.getContent());
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = in.read(buffer)) != -1) {
                byte[] temp = new byte[result.length + len];
                System.arraycopy(result, 0, temp, 0, result.length);
                System.arraycopy(buffer, 0, temp, result.length, len);
                result = temp;
                if (DEBUG) {
                    log.debug(String.format("len=%d, result.length=%d", len, result.length));
                }
                if (result.length > 10240) {
                    break;
                }
                if (result.length > 1024) {
                    String s = new String(result).replaceAll("\\s+", " ");
                    Matcher m = P_HEAD.matcher(s);
                    if (m.find()) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (null != httpEntity) {
                try {
                    httpEntity.consumeContent();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }

    private static void initProxy() {
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("guest", "nanjing".toCharArray());
            }
        });
        System.setProperty("http.proxySet", "true");
        System.setProperty("http.proxyType", "4");
        System.setProperty("http.proxyHost", "10.170.253.153");
        System.setProperty("http.proxyPort", "808");
        System.setProperty("https.proxySet", "true");
        System.setProperty("https.proxyType", "4");
        System.setProperty("https.proxyHost", "10.170.253.153");
        System.setProperty("https.proxyPort", "808");
    }

    public static void testImg(File file) {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[64];
            int length = in.read(buffer);
            for (int i = 0; i < length; i++) {
                System.out.print(buffer[i] + " ");
            }
            System.out.println();
        } catch (Exception ex) {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String[] urlArray = new String[] { "http://www.javaeye.com", "http://www.sinoweb.com.cn", "http://www.taojapan.com", "http://www.codyy.com", "http://www.juandou.com", "http://www.m8fans.com", "http://www.battlenet.com.cn", "http://www.tutzor.com", "http://icon.cn/cn/", "http://piao.kuxun.cn", "http://www.mysites.com", "http://www.gtcfla.net/" };
        for (int i = 0; i < urlArray.length; i++) {
            System.out.println(parseTitle(urlArray[i]));
        }
    }
}
