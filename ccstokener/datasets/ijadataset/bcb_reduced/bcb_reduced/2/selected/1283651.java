package com.hcs.service.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

public class HttpUtil {

    public String sendGet(String url, String param) {
        String result = "";
        try {
            String urlName = url + "?" + param;
            URL U = new URL(urlName);
            URLConnection connection = U.openConnection();
            connection.setConnectTimeout(10000);
            connection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * 发送post请求
	 * @param url
	 * @param param
	 * @return
	 */
    public byte[] sendPost(String url, byte[] param) {
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
            byte[] bytes = new byte[httpURLConnection.getContentLength()];
            is.read(bytes, 0, httpURLConnection.getContentLength());
            is.close();
            return bytes;
        } catch (Exception e) {
        }
        return null;
    }

    public static byte[] getHeaderByteInfo(int msgId, String sessionId, byte[] md5Code, byte[] retain) {
        List<byte[]> byteList = new LinkedList<byte[]>();
        try {
            byteList.add(TypeConvert.toBytesConverse((short) msgId));
            byteList.add(sessionId.getBytes("utf-8"));
            byteList.add(md5Code);
            byteList.add(retain);
            return MyUtils.byteListConvterToByteArray(byteList);
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * 提供两种形式md5加密（如果不是加密码内容将其设为null,默认为字节数组）
	 * 
	 * @param s
	 *            字符串
	 * @param bytes
	 *            字节数组
	 * @return 加密码后内容
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
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    public static void main(String[] args) {
    }
}
