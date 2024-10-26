package org.xiangxji.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Http 连接工具类
 * <p>用于创建Http连接，发送参数，获取返回结果</p>
 * @author xiangxji
 * @version 1.0.0
 */
public class HttpConnUtils {

    public static String Http_Charset = "GBK";

    public static String Params_Charset = "utf-8";

    public static String Http_Method = "POST";

    /**
	 * 获取连接
	 * @author xiangxji
	 * @param urlStr Http连接的Url
	 * @param Method Http连接的方法，POST/GET
	 * @exception IOException 创建连接出错
	 * @return HttpUrlConnection对象
	 */
    public static HttpURLConnection getHttpConn(String urlStr, String Method) throws IOException {
        URL url = null;
        HttpURLConnection connection = null;
        url = new URL(urlStr);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod(Method);
        connection.setUseCaches(false);
        connection.connect();
        return connection;
    }

    /**
	 * 发送参数
	 * @author xiangxji
	 * @param connection HttpUrlConnection对象
	 * @param content 发送的参数内容，形如"a=1&b=2"
	 * @throws IOException 打开连接输出流出错
	 */
    public static void sendHttpParams(HttpURLConnection connection, String content) throws IOException {
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.write(content.getBytes(Params_Charset));
        out.flush();
        out.close();
    }

    /**
	 * 发送一个Http Post 请求
	 * @author xiangxji
	 * @param urlStr Http请求的URL
	 * @param content 需要发送的参数内容
	 * @return 请求连接返回的结果
	 */
    public static String HttpPost(String urlStr, String content) {
        HttpURLConnection connection = null;
        String response = "";
        try {
            connection = getHttpConn(urlStr, Http_Method);
            if (connection != null) {
                sendHttpParams(connection, content);
                response = getHttpResponse(connection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response;
    }

    /**
	 * 获取结果
	 * @author xiangxji
	 * @param connection HttpUrlConnection对象
	 * @return 获取到的请求结果
	 * @throws UnsupportedEncodingException 字符编码出错
	 * @throws IOException 打开连接的输入流出错
	 */
    public static String getHttpResponse(HttpURLConnection connection) throws UnsupportedEncodingException, IOException {
        StringBuffer buffer = new StringBuffer();
        String line = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Http_Charset));
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
            buffer.append("\n");
        }
        reader.close();
        return buffer.toString();
    }
}
