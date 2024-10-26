package com.gjzq.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * ����:
 * ��Ȩ:   Copyright (c) 2005
 * ��˾:   ˼�ϿƼ�
 * ����:   �����
 * �汾:   1.0
 * ��������: 2006-10-5
 * ����ʱ��: 17:57:18
 */
public class HttpHelper {

    private static Logger logger = Logger.getLogger(HttpHelper.class);

    private static final int DEFAULT_INITIAL_BUFFER_SIZE = 4 * 1024;

    private HttpHelper() {
    }

    /**
	 * ���ض�ӦURL��ַ�����ݣ�ע�⣬ֻ��������Ӧ(״̬��Ӧ����Ϊ200)������
	 * 
	 * @param urlPath
	 *            ��Ҫ��ȡ���ݵ�URL��ַ
	 * @return ��ȡ�������ֽ�����
	 */
    public static byte[] getURLContent(String urlPath) {
        HttpURLConnection conn = null;
        InputStream inStream = null;
        byte[] buffer = null;
        try {
            URL url = new URL(urlPath);
            HttpURLConnection.setFollowRedirects(false);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);
            conn.connect();
            int repCode = conn.getResponseCode();
            if (repCode == 200) {
                inStream = conn.getInputStream();
                int contentLength = conn.getContentLength();
                buffer = getResponseBody(inStream, contentLength);
            }
        } catch (Exception ex) {
            logger.error("", ex);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception ex) {
            }
        }
        return buffer;
    }

    /**
	 * ��������ȡBODY���ֵ��ֽ�����
	 * ���ߣ���֪֮
	 * ʱ�䣺2010-9-7 ����03:16:46
	 * @param instream
	 * @param contentLength
	 * @return
	 * @throws Exception
	 */
    private static byte[] getResponseBody(InputStream instream, int contentLength) throws Exception {
        if (contentLength == -1) {
            logger.debug("Going to buffer response body of large or unknown size. ");
        }
        ByteArrayOutputStream outstream = new ByteArrayOutputStream(contentLength > 0 ? (int) contentLength : DEFAULT_INITIAL_BUFFER_SIZE);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = instream.read(buffer)) > 0) {
            outstream.write(buffer, 0, len);
        }
        outstream.close();
        return outstream.toByteArray();
    }

    private static void readFixedLenToBuffer(InputStream inStream, byte[] buffer) throws Exception {
        int count = 0;
        int remainLength = buffer.length;
        int bufLength = buffer.length;
        int readLength = 0;
        do {
            count = inStream.read(buffer, readLength, remainLength);
            if (count == -1) {
                if (readLength != bufLength) {
                    throw new Exception("��ȡ��ݳ��?����ȷ����ݽ���");
                }
            }
            readLength += count;
            if (readLength == bufLength) {
                return;
            }
            remainLength = bufLength - readLength;
        } while (true);
    }

    /**
	 * ���ض�ӦURL��ַ�����ݣ�ע�⣬ֻ��������Ӧ(״̬��Ӧ����Ϊ200)������
	 * 
	 * @param urlPath
	 *            ��Ҫ��ȡ�ڿյ�URL��ַ
	 * @param charset
	 *            �ַ���뷽ʽ
	 * @return ��ȡ�������ִ�
	 */
    public static String getURLContent(String urlPath, String charset) {
        BufferedReader reader = null;
        HttpURLConnection conn = null;
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(urlPath);
            HttpURLConnection.setFollowRedirects(false);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);
            conn.connect();
            int repCode = conn.getResponseCode();
            if (repCode == 200) {
                int count = 0;
                char[] chBuffer = new char[1024];
                BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
                while ((count = input.read(chBuffer)) != -1) {
                    buffer.append(chBuffer, 0, count);
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception ex) {
            }
        }
        return buffer.toString();
    }

    public static String getURLContent(String urlPath, String requestData, String charset) {
        BufferedReader reader = null;
        HttpURLConnection conn = null;
        StringBuffer buffer = new StringBuffer();
        OutputStreamWriter out = null;
        try {
            URL url = new URL(urlPath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);
            out = new OutputStreamWriter(conn.getOutputStream(), charset);
            out.write(requestData);
            out.flush();
            int repCode = conn.getResponseCode();
            if (repCode == 200) {
                int count = 0;
                char[] chBuffer = new char[1024];
                BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
                while ((count = input.read(chBuffer)) != -1) {
                    buffer.append(chBuffer, 0, count);
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception ex) {
            }
        }
        return buffer.toString();
    }

    public static void main(String[] args) {
        try {
            byte[] byteContent = HttpHelper.getURLContent("http://hq1.cgws.com/cgi-bin/market?funcno=11000&stock_code=000002&market=SZ&flowno=1000");
            System.out.println(byteContent.length);
            System.out.println(new String(byteContent, "GBK"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
