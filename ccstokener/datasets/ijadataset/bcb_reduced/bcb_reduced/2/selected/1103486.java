package com.hmw.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Java向Web站点发送POST请求 <br>
 * <b>创建日期</b>：2011-3-7
 * @author <a href="mailto:hemingwang0902@126.com">何明旺</a>
 */
public class PostFormData {

    private static final String URL = "http://www.88wanmei.cn/register.php";

    private static final String CHARSET = "utf-8";

    public static void main(String[] args) {
        try {
            String temp = "renyanwei";
            for (int i = 1; i < 10; i++) {
                URL url = new URL(URL);
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), CHARSET);
                String str = String.format(Locale.CHINA, "login=%s&&passwd=%s&&repasswd=%s&&Prompt=%s&&answer=%s&&email=%s", temp + i, temp + i, temp + i, temp + i, URLEncoder.encode("中文", CHARSET), "ren@ren.com");
                out.write(str);
                out.flush();
                out.close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                int lineNum = 1;
                while ((line = reader.readLine()) != null) {
                    ++lineNum;
                    if (lineNum == 174) System.out.println(line);
                }
            }
        } catch (Exception x) {
            System.out.println(x.toString());
        }
    }
}
