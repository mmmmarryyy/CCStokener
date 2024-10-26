package jamsa.rcp.downloader.http;

import jamsa.rcp.downloader.IConstants;
import jamsa.rcp.downloader.Messages;
import jamsa.rcp.downloader.utils.Logger;
import jamsa.rcp.downloader.utils.StringUtils;
import jamsa.rcp.downloader.views.DefaultConsoleWriter;
import jamsa.rcp.downloader.views.IConsoleWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Properties;

public class HttpClientUtils {

    private static Logger logger = new Logger(HttpClientUtils.class);

    public static final String DEFAULT_USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)";

    /**
	 * 获取远程文件信息
	 * 
	 * @param urlString
	 * @param retryTimes
	 * @param retryDelay
	 * @param properties
	 * @param writer
	 * @param label
	 * @return
	 * @throws Exception
	 */
    public static RemoteFileInfo getRemoteFileInfo(String urlString, int retryTimes, int retryDelay, int timeout, Properties properties, String method, IConsoleWriter writer, String label) {
        if (writer == null) writer = new DefaultConsoleWriter();
        HttpURLConnection conn = getHttpURLConnection(urlString, retryTimes, retryDelay, timeout, properties, method, writer, label);
        if (conn == null) return null;
        return getRemoteSiteFile(conn, writer, label);
    }

    /**
	 * 获取远程文件信息
	 * 
	 * @param conn
	 * @param writer
	 * @param label
	 * @return
	 */
    public static RemoteFileInfo getRemoteSiteFile(HttpURLConnection conn, IConsoleWriter writer, String label) {
        if (writer == null) writer = new DefaultConsoleWriter();
        RemoteFileInfo result = new RemoteFileInfo();
        URL url = conn.getURL();
        result.setHostname(url.getHost());
        result.setPort(url.getPort());
        String fileName = url.getFile();
        int start = fileName.lastIndexOf("/") + 1;
        int end = fileName.indexOf("?");
        if (end > start) result.setFileName(fileName.substring(start, end)); else result.setFileName(fileName.substring(start, fileName.length()));
        writer.writeMessage("Task", Messages.HttpClientUtils_MSG_Remote_File_Name + result.getFileName());
        result.setFileSize(conn.getContentLength());
        writer.writeMessage("Task", Messages.HttpClientUtils_MSG_Remote_File_Size + result.getFileSize());
        logger.info("远程文件信息：\n" + result);
        return result;
    }

    /**
	 * 获取远程Http连接
	 * 
	 * @param urlString
	 *            连接地址
	 * @param retryTimes
	 *            重试次数
	 * @param retryDelay
	 *            重试延时时间
	 * @param timeout
	 *            超时时间
	 * @param properties
	 *            请求属性
	 * @param method
	 *            请求方式：GET,POST,HEAD...
	 * @param writer
	 *            日志输出接口，为null时将输出到控制台
	 * @return HttpURLConnection对象
	 * @throws Exception
	 *             如果连接错误则抛出经过处理的异常
	 */
    public static HttpURLConnection getHttpURLConnection(String urlString, int retryTimes, int retryDelay, int timeout, Properties properties, String method, IConsoleWriter writer, String label) {
        if (writer == null) writer = new DefaultConsoleWriter();
        if (label == null) label = "HttpURLConnection";
        int count = 0;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            while (conn == null && retryTimes >= count) {
                count++;
                writer.writeMessage(label, Messages.HttpClientUtils_MSG_Times + count + Messages.HttpClientUtils_MSG_Connect);
                logger.info("第" + count + "次连接...");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(timeout);
                conn.setRequestMethod(method);
                for (Iterator it = properties.keySet().iterator(); it.hasNext(); ) {
                    String key = String.valueOf(it.next());
                    conn.setRequestProperty(key, String.valueOf(properties.get(key)));
                }
                printRequestInfo(conn, writer, label);
                int code = conn.getResponseCode();
                printResponseInfo(conn, writer, label);
                String newURLString = String.valueOf(conn.getURL());
                if (code == 404 && !urlString.equals(newURLString)) {
                    String encode = conn.getContentEncoding();
                    if (StringUtils.isEmpty(encode)) encode = IConstants.DEFAULT_ENCODING;
                    url = new URL(new String(newURLString.getBytes(encode), IConstants.FILE_ENCODING));
                    writer.writeMessage(label, Messages.HttpClientUtils_MSG_Redirect_To + url);
                    logger.info("发生重定向：" + url);
                    conn = null;
                    continue;
                }
                if (code >= 400) {
                    conn = null;
                }
                if (conn == null) {
                    writer.writeMessage(label, Messages.HttpClientUtils_ERR_Connect_Fail + retryDelay / 1000 + Messages.HttpClientUtils_MSG_Retry_After);
                    logger.info("连接失败," + retryDelay / 1000 + "秒后重试...");
                    Thread.sleep(retryDelay);
                } else {
                    writer.writeMessage(label, Messages.HttpClientUtils_MSG_Connected_Success);
                    logger.info("连接成功！");
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            writer.writeMessage(label, Messages.HttpClientUtils_ERR_URL_Error + e.getLocalizedMessage());
            logger.error("URL错误！", e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            writer.writeMessage(label, Messages.HttpClientUtils_ERR_IO_Error + e.getLocalizedMessage());
            logger.error("I/O错误！", e);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            writer.writeMessage(label, Messages.HttpClientUtils_ERR_Error + e.getLocalizedMessage());
            logger.error("连接错误！", e);
            return null;
        }
        return conn;
    }

    /**
	 * 获取输入流
	 * 
	 * @param urlString
	 * @param retryTimes
	 * @param retryDelay
	 * @param properties
	 * @param writer
	 * @param label
	 * @return
	 */
    public static InputStream getInputStream(String urlString, int retryTimes, int retryDelay, int timeout, Properties properties, String method, IConsoleWriter writer, String label) {
        InputStream ret = null;
        HttpURLConnection conn = HttpClientUtils.getHttpURLConnection(urlString, retryTimes, retryDelay, timeout, properties, method, writer, label);
        if (conn != null) {
            try {
                ret = conn.getInputStream();
            } catch (Exception e) {
                writer.writeMessage(label, e.getLocalizedMessage());
                logger.error("获取输入流发生错误！", e);
            }
        }
        return ret;
    }

    /**
	 * 打印Http响应头
	 * 
	 * @param conn
	 * @param writer
	 * @param label
	 */
    public static void printResponseInfo(URLConnection conn, IConsoleWriter writer, String label) {
        logger.info("Http响应信息：");
        for (Iterator iter = conn.getHeaderFields().keySet().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            writer.writeMessage(label, key + ":" + conn.getHeaderField(key));
            logger.info(key + ":" + conn.getHeaderField(key));
        }
    }

    /**
	 * 打印Http请求信息
	 * 
	 * @param conn
	 * @param writer
	 * @param label
	 */
    public static void printRequestInfo(HttpURLConnection conn, IConsoleWriter writer, String label) {
        logger.info("Http请求信息：");
        logger.info("RequestMethod:" + conn.getRequestMethod());
        for (Iterator iter = conn.getRequestProperties().keySet().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            writer.writeMessage(label, key + ":" + conn.getHeaderField(key));
            logger.info(key + ":" + conn.getHeaderField(key));
        }
    }
}
