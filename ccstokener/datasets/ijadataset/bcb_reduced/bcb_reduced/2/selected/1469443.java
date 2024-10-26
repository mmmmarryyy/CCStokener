package com.hcs.protocol.utils;

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
import android.util.Log;

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
	 * 
	 * @param url
	 * @param param
	 * @return
	 * @throws Exception
	 */
    public byte[] sendPost(String url, byte[] param) throws Exception {
        HttpURLConnection huc = null;
        InputStream is = null;
        OutputStream outputStream = null;
        try {
            URL httpurl = new URL(url);
            huc = (HttpURLConnection) httpurl.openConnection();
            huc.setDoOutput(true);
            huc.setConnectTimeout(10000);
            huc.setReadTimeout(10000);
            huc.setRequestProperty("Content-length", "" + param.length);
            outputStream = huc.getOutputStream();
            outputStream.write(param);
            outputStream.flush();
            is = huc.getInputStream();
            byte[] resultBytes = new byte[huc.getContentLength()];
            byte[] tempByte = new byte[1024];
            int length = 0;
            int index = 0;
            while ((length = is.read(tempByte)) != -1) {
                System.arraycopy(tempByte, 0, resultBytes, index, length);
                index += length;
            }
            return resultBytes;
        } catch (Exception e) {
            throw new Exception();
        } finally {
            is.close();
            huc.disconnect();
            outputStream.close();
        }
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
