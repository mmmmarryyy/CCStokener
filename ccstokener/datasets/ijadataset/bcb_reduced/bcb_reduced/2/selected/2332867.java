package com.hand.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

/**
 * HTTP工具类
 * 
 * @author nianchun.li
 * @createTime 2011/5/4 17:10
 */
public class HttpUtil {

    /**
	 * 发送带参数的GET请求
	 * 
	 * @param url
	 *            访问URL地址
	 * @param param
	 *            请求参数
	 * @return 请求结果
	 */
    public static String sendGet(String url, String param) {
        String result = "";
        try {
            String urlName = url + "?" + param;
            URL U = new URL(urlName);
            URLConnection connection = U.openConnection();
            connection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            in.close();
        } catch (Exception e) {
            MsgPrint.showMsg("没有结果！" + e);
        }
        return result;
    }

    /**
	 * 发送post请求
	 * 
	 * @param url
	 *            访问URL地址
	 * @param param
	 *            参数
	 * @return 返回请求结果
	 */
    public static byte[] sendPost(String url, byte[] param) {
        try {
            URL httpurl = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) httpurl.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-length", "" + param.length);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(param);
            outputStream.flush();
            outputStream.close();
            InputStream is = httpURLConnection.getInputStream();
            byte[] resultBytes = new byte[httpURLConnection.getContentLength()];
            byte[] tempByte = new byte[1024];
            int length = 0;
            int index = 0;
            while ((length = is.read(tempByte)) != -1) {
                System.arraycopy(tempByte, 0, resultBytes, index, length);
                index += length;
            }
            is.close();
            return resultBytes;
        } catch (Exception e) {
            e.printStackTrace();
            MsgPrint.showMsg("没有结果！" + e);
        }
        return null;
    }

    /**
	 * 获取数据头信息
	 * 
	 * @param msgId
	 *            消息ID
	 * @param sessionId
	 *            用户标识
	 * @param md5Code
	 *            校验码
	 * @param retain
	 *            保留字段
	 * @return 数据头信息
	 */
    public static byte[] getHeaderByteInfo(short msgId, String sessionId, byte[] md5Code, byte[] retain) {
        List<byte[]> byteList = new LinkedList<byte[]>();
        try {
            byteList.add(TypeConvert.toBytesConverse(msgId));
            byteList.add(sessionId.getBytes("utf-8"));
            byteList.add(md5Code);
            byteList.add(retain);
            return MyUtils.byteListConvterToByteArray(byteList);
        } catch (Exception e) {
        }
        return null;
    }

    /**
	 * 提供两种形式MD5加密（如果不是加密码内容将其设为null,默认为字节数组）
	 * 
	 * @param bytes
	 *            字节数组
	 * @return MD5加密码后内容
	 */
    public static byte[] getValidateCode(byte[] bytes) {
        if (null == bytes || 0 == bytes.length) {
            return new byte[] { 0, 0, 0 };
        } else {
            try {
                byte[] md5Byte = new byte[3];
                md5Byte[0] = bytes[0];
                md5Byte[1] = bytes[bytes.length / 2];
                md5Byte[2] = bytes[bytes.length - 1];
                return md5Byte;
            } catch (ArrayIndexOutOfBoundsException e) {
                MsgPrint.showMsg(e.getMessage());
            }
        }
        return null;
    }

    /**
	 * 发送http POST请求
	 * 
	 * @param url
	 *            URL地址
	 * @param param
	 *            上传参数
	 * @return 请求结果
	 */
    public static String sendPost(String url, String param) {
        String result = "";
        try {
            URL httpurl = new URL(url);
            HttpURLConnection httpConn = (HttpURLConnection) httpurl.openConnection();
            httpConn.setRequestProperty("Accept-Language", "zh-CN");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            PrintWriter out = new PrintWriter(httpConn.getOutputStream());
            out.print(param);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            in.close();
        } catch (Exception e) {
            MsgPrint.showMsg(e.getMessage());
        }
        return result;
    }

    /**
	 * 发送短信息http请求方法
	 * 
	 * @param url
	 *            URL地址
	 * @param param
	 *            上传参数
	 * @return 短信息发送状态
	 */
    public static byte[] sendSmsRequest(String url, String param) {
        byte[] bytes = null;
        try {
            URL httpurl = new URL(url);
            HttpURLConnection httpConn = (HttpURLConnection) httpurl.openConnection();
            httpConn.setRequestProperty("Accept-Language", "zh-CN");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            PrintWriter out = new PrintWriter(httpConn.getOutputStream());
            out.print(param);
            out.flush();
            out.close();
            InputStream ism = httpConn.getInputStream();
            bytes = new byte[httpConn.getContentLength()];
            ism.read(bytes);
            ism.close();
            MsgPrint.showByteArray("result", bytes);
        } catch (Exception e) {
            return new byte[] { 0, 0, 0, 0 };
        }
        return bytes;
    }

    /**
	 * 参数形式http post
	 * 
	 * @param url
	 *            URL地址
	 * @param param
	 *            发送参数
	 * @return 操作结果
	 */
    public static byte[] sendParamPost(String urlString, String param) {
        try {
            URL url = new URL(urlString + "?" + param);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setDefaultUseCaches(false);
            urlConn.setDoInput(true);
            urlConn.setRequestMethod("POST");
            urlConn.connect();
            OutputStream ops = urlConn.getOutputStream();
            ops.close();
            InputStream is = urlConn.getInputStream();
            byte[] resultBytes = new byte[urlConn.getContentLength()];
            byte[] tempByte = new byte[1024];
            int length = 0;
            int index = 0;
            while ((length = is.read(tempByte)) != -1) {
                System.arraycopy(tempByte, 0, resultBytes, index, length);
                index += length;
            }
            is.close();
            return resultBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * 接收 Http URL 返回数据
	 * 
	 * @param httpURLConnection
	 * @return byte[] 数组形式
	 */
    public static byte[] getHttpURLReturnData(HttpServletRequest request) throws Exception {
        MsgPrint.showMsg("======contentlenght====" + request.getContentLength());
        if (0 >= request.getContentLength()) {
            throw new Exception();
        }
        ServletInputStream sis = request.getInputStream();
        byte[] resultBytes = new byte[request.getContentLength()];
        byte[] tempByte = new byte[1024];
        int length = 0;
        int index = 0;
        while ((length = sis.read(tempByte)) != -1) {
            System.arraycopy(tempByte, 0, resultBytes, index, length);
            index += length;
        }
        sis.close();
        return resultBytes;
    }

    /**
	 * 接收 Http URL 返回数据
	 * @param httpURLConnection
	 * @return byte[] 数组形式
	 * @throws IOException
	 */
    public static byte[] getHttpURLReturnData(HttpURLConnection httpURLConnection) throws IOException, Exception {
        InputStream is = httpURLConnection.getInputStream();
        MsgPrint.showMsg("接收到字节的长度=" + httpURLConnection.getContentLength());
        if (0 >= httpURLConnection.getContentLength()) {
            throw new Exception();
        }
        byte[] resultBytes = new byte[httpURLConnection.getContentLength()];
        byte[] tempByte = new byte[1024];
        int length = 0;
        int index = 0;
        while ((length = is.read(tempByte)) != -1) {
            System.arraycopy(tempByte, 0, resultBytes, index, length);
            index += length;
        }
        is.close();
        return resultBytes;
    }

    public static void main(String[] args) {
        String str = sendPost("http://localhost:8080/YeePay/servlet/Chaxunyue", "mobileNumber=13564261487&password=123456");
        MsgPrint.showMsg(str);
    }
}
