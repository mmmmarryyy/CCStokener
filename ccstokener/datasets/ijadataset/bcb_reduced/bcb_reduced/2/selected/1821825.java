package com.mlib.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.mlib.xml.XPathResolver;

/**
 * 用于获取html页面的源代码<br>
 * 本类支持自动转换编码功能,可以自动识别网页的编码,处理乱码<br>
 * 
 * @author zhaotao
 * 
 */
public class HtmlUtil {

    /**
	 * 默认编码
	 */
    private static final String DEFAULT_ENCODE = "GB2312";

    private static final int MAX_CONTENT_SIZE = 500 * 1024;

    private static final String contentByteField = "contentByte";

    public static final String contentFiled = "content";

    public static final String codeField = "encoding";

    /**
	 * 获取页面内容 字节形式存储
	 * 
	 * @param wwwurl
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public static byte[] getURLContent(String wwwurl) throws IOException, URISyntaxException {
        return (byte[]) getURLContentMap(wwwurl).get(contentByteField);
    }

    /**
	 * 获取页面内容 字节形式存储
	 * 
	 * @param wwwurl
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public static synchronized Map<String, Object> getURLContentMap(String wwwurl) throws IOException, URISyntaxException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        URI uri = new URI(wwwurl);
        URL url = new URL(uri.toASCIIString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10 * 1000);
        HttpURLConnection.setFollowRedirects(true);
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110421 Red Hat/3.6-1.el5_6 Firefox/3.6.17");
            for (String key : conn.getHeaderFields().keySet()) {
                List<String> headerInfo = conn.getHeaderFields().get(key);
                if (headerInfo.size() > 0) {
                    resultMap.put(key, headerInfo.get(0));
                }
            }
            String contentType = conn.getContentType();
            if (!(contentType == null || contentType.toLowerCase().contains("text") || contentType.toLowerCase().contains("html"))) {
                return resultMap;
            }
            ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            InputStream instream = conn.getInputStream();
            synchronized (instream) {
                int readSize = 0;
                int totalSize = 0;
                byte[] contentByte = null;
                byte[] buffer = new byte[1024];
                while ((readSize = instream.read(buffer)) > 0) {
                    outstream.write(buffer, 0, readSize);
                    totalSize += readSize;
                    if (totalSize >= MAX_CONTENT_SIZE) {
                        contentByte = ("[FAILD] content size is larger than " + MAX_CONTENT_SIZE + " byte.").getBytes();
                    }
                }
                if (contentByte == null) {
                    contentByte = outstream.toByteArray();
                }
                instream.close();
                outstream.close();
                resultMap.put(contentByteField, contentByte);
            }
        } finally {
        }
        return resultMap;
    }

    /**
	 * 获取页面编码
	 * 
	 * @param contentByte
	 * @return
	 */
    public static String getEncode(byte[] contentByte) {
        String content = new String(contentByte);
        String regex = "(?i)charset[ ]*=[ ]*[a-zA-Z0-9-]+";
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (matcher.find()) {
            String encode = matcher.group().toUpperCase();
            return encode.replaceAll("(?i)charset[ ]*=[ ]*", "");
        }
        return null;
    }

    /**
	 * 获取页面内容 经过编码处理的字符串内容
	 * 
	 * @param wwwurl
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public static String getContent(String wwwurl) throws IOException, URISyntaxException {
        byte[] contentByte = getURLContent(wwwurl);
        String encode = getEncode(contentByte);
        if (encode == null) encode = DEFAULT_ENCODE;
        return new String(contentByte, encode);
    }

    /**
	 * 获取页面内容 经过编码处理的字符串内容
	 * 
	 * @param wwwurl
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public static Map<String, String> getContentMap(String wwwurl) throws IOException, URISyntaxException {
        Map<String, Object> map = getURLContentMap(wwwurl);
        Map<String, String> resultMap = new HashMap<String, String>();
        byte[] contentByte = (byte[]) map.get("contentByte");
        if (contentByte != null) {
            String encode = getEncode(contentByte);
            if (encode == null) encode = DEFAULT_ENCODE;
            String content = new String(contentByte, encode);
            resultMap.put(codeField, encode);
            resultMap.put(contentFiled, content);
        }
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value != null && value instanceof String) {
                resultMap.put(key, value.toString());
            }
        }
        return resultMap;
    }

    /**
	 * 获取页面内容(页面内容字节) 经过编码处理的字符串内容
	 * 
	 * @param contentByte
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    public static String getContent(byte[] contentByte) throws UnsupportedEncodingException {
        String encode = getEncode(contentByte);
        if (encode == null) encode = DEFAULT_ENCODE;
        return new String(contentByte, encode);
    }

    /**
	 * 从内容中提取所有的超链地址
	 * 
	 * @return
	 * @return List<String>
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
    public static List<String> getAllHrefFromContent(String pageURL, String content) throws MalformedURLException, URISyntaxException {
        List<String> allURLs = new ArrayList<String>();
        if (content != null) {
            URI uri = new URI(pageURL);
            String exp = "//a";
            try {
                XPathResolver xpath = new XPathResolver(HtmlUtil.toWellFormXML(content));
                NodeList list = xpath.findNodes(exp);
                for (int i = 0; i < list.getLength(); i++) {
                    Node node = list.item(i);
                    Node item = node.getAttributes().getNamedItem("href");
                    if (item != null) {
                        String link = item.getNodeValue();
                        link = getAbsoluteURL(uri, link);
                        allURLs.add(link);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return allURLs;
    }

    /**
	 * 相对路径转绝对路径
	 * 
	 * @param baseURI
	 * @param relativePath
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
    public static String getAbsoluteURL(URI baseURI, String path) throws URISyntaxException, MalformedURLException {
        String query = "";
        if (path.indexOf("?") > 0) {
            query = path.substring(path.indexOf("?"));
            path = path.substring(0, path.indexOf("?"));
        }
        if (path.toLowerCase().startsWith("http://")) {
        } else if (path.startsWith("/")) path = "http://" + baseURI.getHost() + path; else try {
            path = URLEncoder.encode(path, "UTF-8");
            path = baseURI.resolve(path).toString();
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            path = null;
            e.printStackTrace();
        }
        if (path != null) path = path.replace(" ", "%20");
        return path + query;
    }

    /**
	 * 将不规范的 html 代码转换成规范的 xml 格式代码
	 * @param html
	 * @return
	 * @throws IOException
	 */
    public static String toWellFormXML(String html) throws IOException {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();
        props.setUseCdataForScriptAndStyle(true);
        props.setRecognizeUnicodeChars(true);
        props.setUseEmptyElementTags(true);
        props.setAdvancedXmlEscape(true);
        props.setTranslateSpecialEntities(true);
        props.setBooleanAttributeValues("empty");
        org.htmlcleaner.TagNode node = cleaner.clean(html);
        return new PrettyXmlSerializer(props).getAsString(node);
    }
}
