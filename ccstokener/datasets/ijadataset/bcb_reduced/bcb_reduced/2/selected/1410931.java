package jk.spider.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jk.spider.model.ProxyInfo;
import org.apache.log4j.Logger;
import org.hsqldb.lib.StringUtil;
import com.sun.xml.internal.ws.util.StringUtils;

/**
 * ץȡʱ�ṩ����Ҫ����
 * @author kqy
 * @date Feb 17, 2009
 * @version 2.0
 */
public class SpiderUtil {

    private static final Logger log = Logger.getLogger(SpiderUtil.class);

    private static final String DEFAULT_ENCODING = "GBK";

    public static final synchronized boolean isStringNull(String str) {
        return str == null || str.trim().length() == 0 || str.equalsIgnoreCase("null");
    }

    /**
	 * �õ��ļ�����
	 * @param bytes
	 * @param encode
	 * @return
	 */
    public String getFileContent(byte[] bytes, String encode) {
        InputStream in = new ByteArrayInputStream(bytes);
        StringBuffer strResult = new StringBuffer();
        try {
            String inputLine = null;
            byte[] b = new byte[40960];
            int len = 0;
            while ((len = in.read(b)) > 0) {
                inputLine = new String(b, 0, len, encode);
                strResult.append(inputLine.replaceAll("[\t\n\r]", " "));
            }
            in.close();
        } catch (IOException e) {
            log.warn("SpiderUtil getFileContent IOException -> ", e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
        }
        return strResult.toString();
    }

    public String getFileContent(byte[] bytes) {
        return this.getFileContent(bytes, DEFAULT_ENCODING);
    }

    public static final String iso2gb(String s) {
        try {
            if (s != null) {
                byte abyte0[] = s.getBytes("iso-8859-1");
                return new String(abyte0, "GBK");
            } else {
                return "";
            }
        } catch (UnsupportedEncodingException unsupportedencodingexception) {
            return s;
        }
    }

    /**
	 * ��ȡҳ�������е�LINK
	 * @param content ҳ������
	 * @param url ԭ����URL
	 * @return
	 */
    public String[] findAllUrl(String content, String url) {
        content = content.replaceAll("<script[^>]*>.*?</script>", "");
        content = content.replaceAll("<style>.*?</style>", "");
        Set<String> set = new HashSet<String>();
        try {
            Pattern p = Pattern.compile("href *= *[\"']?([^\"' >]+)[\"']?", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            while (m.find()) {
                String detUrl = m.group(1);
                if (detUrl.toLowerCase().trim().indexOf("javascript") == -1 && detUrl.toLowerCase().trim().indexOf("about:blank") == -1 && !detUrl.endsWith("#")) set.add(constructUrl(detUrl, url));
            }
            p = Pattern.compile("src *= *[\"']?([^\"' >]+)[\"']?", Pattern.CASE_INSENSITIVE);
            m = p.matcher(content);
            while (m.find()) {
                String detUrl = m.group(1);
                if (detUrl.toLowerCase().trim().indexOf("javascript") == -1 && detUrl.toLowerCase().trim().indexOf("about:blank") == -1 && !detUrl.endsWith("#")) set.add(constructUrl(detUrl, url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    public static String constructUrl(String link, String base) {
        link = link.replaceAll("&amp;", "&");
        String path;
        boolean modified;
        boolean absolute;
        int index;
        URL url = null;
        try {
            if (('?' == link.charAt(0))) {
                if (-1 != (index = base.lastIndexOf('?'))) base = base.substring(0, index);
                url = new URL(base + link);
            } else url = new URL(new URL(base), link);
            path = url.getFile();
            modified = false;
            absolute = link.startsWith("/");
            if (!absolute) {
                while (path.startsWith("/.")) {
                    if (path.startsWith("/../")) {
                        path = path.substring(3);
                        modified = true;
                    } else if (path.startsWith("/./") || path.startsWith("/.")) {
                        path = path.substring(2);
                        modified = true;
                    } else break;
                }
            }
            while (-1 != (index = path.indexOf("/\\"))) {
                path = path.substring(0, index + 1) + path.substring(index + 2);
                modified = true;
            }
            if (modified) url = new URL(url, path);
        } catch (MalformedURLException e) {
            log.warn("link ->" + link + " url - > " + url, e);
        }
        return url.toString();
    }

    public Matcher getMatcher(final String reg, final String content) {
        Pattern pt = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
        Matcher mc = pt.matcher(content);
        return mc;
    }

    /**
	 * ��ȡ�ļ����ݣ�������LIST
	 * @param fileName
	 * @return
	 */
    public List<String> readFile(String fileName) {
        BufferedReader in = null;
        String line;
        List<String> keyList = new ArrayList<String>();
        try {
            in = new BufferedReader(new FileReader(new File(fileName)));
            line = in.readLine();
            while (line != null || !SpiderUtil.isStringNull(line)) {
                keyList.add(line);
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            log.error("IOException", e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                log.error("IOException", e);
            }
        }
        return keyList;
    }

    /**
	 * �õ�һ��HTTP������ҳ��Դ��
	 * @param sUrl
	 * @return
	 */
    public String getHTTPContent(String sUrl) {
        return this.getHTTPContent(sUrl, DEFAULT_ENCODING, "", "", "");
    }

    public String getHTTPContent(String sUrl, String encode) {
        return this.getHTTPContent(sUrl, encode, "", "", "");
    }

    public byte[] getHTTPByte(String sUrl) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        ByteArrayOutputStream os = null;
        try {
            URL url = new URL(sUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            int httpStatus = connection.getResponseCode();
            if (httpStatus != 200) log.info("getHTTPConent error httpStatus - " + httpStatus);
            inputStream = new BufferedInputStream(connection.getInputStream());
            os = new ByteArrayOutputStream();
            InputStream is = new BufferedInputStream(inputStream);
            byte bytes[] = new byte[40960];
            int nRead = -1;
            while ((nRead = is.read(bytes, 0, 40960)) > 0) {
                os.write(bytes, 0, nRead);
            }
            os.close();
            is.close();
            inputStream.close();
        } catch (IOException e) {
            log.warn("SpiderUtil getHTTPConent IOException -> ", e);
        } finally {
            if (inputStream != null) try {
                os.close();
                inputStream.close();
            } catch (IOException e) {
            }
        }
        return os.toByteArray();
    }

    public String getHTTPContent(String sUrl, String encode, String cookie, String host, String referer) {
        HttpURLConnection connection = null;
        InputStream in = null;
        StringBuffer strResult = new StringBuffer();
        try {
            URL url = new URL(sUrl);
            connection = (HttpURLConnection) url.openConnection();
            if (!isStringNull(host)) this.setHttpInfo(connection, cookie, host, referer);
            connection.connect();
            int httpStatus = connection.getResponseCode();
            if (httpStatus != 200) log.info("getHTTPConent error httpStatus - " + httpStatus);
            in = new BufferedInputStream(connection.getInputStream());
            String inputLine = null;
            byte[] b = new byte[40960];
            int len = 0;
            while ((len = in.read(b)) > 0) {
                inputLine = new String(b, 0, len, encode);
                strResult.append(inputLine.replaceAll("[\t\n\r ]", " "));
            }
            in.close();
        } catch (IOException e) {
            log.warn("SpiderUtil getHTTPConent IOException -> ", e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
        }
        return strResult.toString();
    }

    public int getHttpStatus(ProxyInfo proxyInfo, String sUrl, String cookie, String host) {
        HttpURLConnection connection = null;
        try {
            if (proxyInfo == null) {
                URL url = new URL(sUrl);
                connection = (HttpURLConnection) url.openConnection();
            } else {
                InetSocketAddress addr = new InetSocketAddress(proxyInfo.getPxIp(), proxyInfo.getPxPort());
                Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
                URL url = new URL(sUrl);
                connection = (HttpURLConnection) url.openConnection(proxy);
            }
            if (!isStringNull(host)) setHttpInfo(connection, cookie, host, "");
            connection.setConnectTimeout(90 * 1000);
            connection.setReadTimeout(90 * 1000);
            connection.connect();
            connection.getInputStream();
            return connection.getResponseCode();
        } catch (IOException e) {
            log.info(proxyInfo + " getHTTPConent Error ");
            return 0;
        } catch (Exception e) {
            log.info(proxyInfo + " getHTTPConent Error ");
            return 0;
        }
    }

    public boolean compareDate(String newDate, String oldDate, String dateFormat) {
        DateFormat df = new SimpleDateFormat(dateFormat);
        try {
            int r = df.parse(newDate).compareTo(df.parse(oldDate));
            if (r > -1) return true; else return false;
        } catch (ParseException e) {
            new RuntimeException("���ڸ�ʽ��������...");
        }
        return false;
    }

    protected static void setHttpInfo(HttpURLConnection connection, String cookie, String host, String referer) throws ProtocolException {
        connection.addRequestProperty("Cookie", cookie);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)");
        connection.setFollowRedirects(true);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("accept-language", "zh-cn");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Host", host);
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Referer", referer);
        connection.setAllowUserInteraction(false);
    }

    public static void main(String args[]) {
        String url = "http://202.108.23.172/m?word=rm,http://image.stareastnet.com/2006/03/23/Y2JjamVpaWtiZWNnaG2ecWlpZDQ$.rm,,[%D4%AA%C6%F8]&ct=134217728&tn=baidusg,Ԫ�� &si=%D4%AA%C6%F8;;%D4%AA%C6%F8;;0;;0&lm=16777216&sgid=1";
        SpiderUtil util = new SpiderUtil();
        String content = util.getHTTPContent(url, "utf-8", "", "", "");
        System.out.println(content);
    }
}
