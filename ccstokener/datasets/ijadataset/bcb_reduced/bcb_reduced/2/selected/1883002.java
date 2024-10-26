package com.michael.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 
 * @author <a href="mailto:hanxiaojun85@gmail.com">韩晓军</a>
 * @version 2010-8-29
 * @since 1.6
 */
public class StringUtil extends GlobalBase {

    /**
	 * 根据url得到网页内容
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
    public static String getContent(String url) {
        return getContent(url, "gbk");
    }

    public static String getContent(String url, String code) {
        HttpURLConnection connect = null;
        try {
            URL myurl = new URL(url);
            connect = (HttpURLConnection) myurl.openConnection();
            connect.setConnectTimeout(30000);
            connect.setReadTimeout(30000);
            connect.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; GTB5; .NET CLR 1.1.4322; .NET CLR 2.0.50727; Alexa Toolbar; MAXTHON 2.0)");
            return StringUtil.convertStreamToString(connect.getInputStream(), code);
        } catch (Exception e) {
            slogger.warn(e.getMessage());
        } finally {
            if (connect != null) {
                connect.disconnect();
            }
        }
        slogger.warn("这个没找到" + url);
        return null;
    }

    /**
	 * 通过post方式访问
	 * @param url
	 * @param param
	 * @return
	 */
    public static String getContentPost(String url, String param) {
        return getContentPost(url, param, "gbk");
    }

    /**
	 * 通过post方式访问
	 * 
	 * @param url
	 * @param param
	 * @param code
	 * @return
	 */
    public static String getContentPost(String url, String param, String code) {
        HttpURLConnection connect = null;
        try {
            URL myurl = new URL(url);
            connect = (HttpURLConnection) myurl.openConnection();
            connect.setConnectTimeout(30000);
            connect.setReadTimeout(30000);
            connect.setDoInput(true);
            connect.setDoOutput(true);
            connect.setRequestMethod("POST");
            connect.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; GTB5; .NET CLR 1.1.4322; .NET CLR 2.0.50727; Alexa Toolbar; MAXTHON 2.0)");
            OutputStreamWriter out = new OutputStreamWriter(connect.getOutputStream());
            out.write(param);
            out.flush();
            out.close();
            return StringUtil.convertStreamToString(connect.getInputStream(), code);
        } catch (Exception e) {
            slogger.warn(e.getMessage());
        } finally {
            if (connect != null) {
                connect.disconnect();
            }
        }
        return null;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    public static String getMethod(String field) {
        return "get" + upperCaseFirst(field);
    }

    public static String setMethod(String field) {
        return "set" + upperCaseFirst(field);
    }

    public static String upperCaseFirst(String field) {
        String s1 = field.substring(0, 1);
        s1 = s1.toUpperCase();
        return s1 + field.substring(1);
    }

    /**
	 * 过滤
	 * 
	 * @param input
	 * @return
	 */
    public static String filter(String input) {
        if (!hasSpecialChars(input)) {
            return input;
        }
        StringBuffer filtered = new StringBuffer(input.length());
        char c;
        for (int i = 0; i <= input.length() - 1; i++) {
            c = input.charAt(i);
            switch(c) {
                case '<':
                    filtered.append("&lt;");
                    break;
                case '>':
                    filtered.append("&gt;");
                    break;
                case '"':
                    filtered.append("&quot;");
                    break;
                case '&':
                    filtered.append("&amp;");
                    break;
                default:
                    filtered.append(c);
            }
        }
        return (filtered.toString());
    }

    public static boolean hasSpecialChars(String input) {
        boolean flag = false;
        if ((input != null) && (input.length() > 0)) {
            char c;
            for (int i = 0; i <= input.length() - 1; i++) {
                c = input.charAt(i);
                switch(c) {
                    case '>':
                        flag = true;
                        break;
                    case '<':
                        flag = true;
                        break;
                    case '"':
                        flag = true;
                        break;
                    case '&':
                        flag = true;
                        break;
                }
            }
        }
        return flag;
    }

    /**
	 * 将输入流转化成字符串
	 * 
	 * @param is
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    public static String convertStreamToString(InputStream is) throws UnsupportedEncodingException {
        return convertStreamToString(is, "gbk");
    }

    /**
	 * 将输入流转化成字符串
	 * 
	 * @param is
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    public static String convertStreamToString(InputStream is, String code) throws UnsupportedEncodingException {
        if (isEmpty(code)) {
            code = "gbk";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, code));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            slogger.warn(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                is.close();
            } catch (IOException e) {
                slogger.warn(e.getMessage());
            }
        }
        return sb.toString();
    }

    public static String clean(String str, int length) {
        if (isEmpty(str)) {
            return null;
        }
        str = str.replaceAll("</?.*?>", "");
        if (str.length() > length) {
            str = str.substring(0, length);
        }
        return str;
    }

    public static String change(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("'", "''");
    }
}
