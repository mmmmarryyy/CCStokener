package com.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class New {

    /**  
     * ��ȡ��������htmlԴ��  
     * */
    public static String getHtmlSource(String url) {
        StringBuffer codeBuffer = null;
        BufferedReader in = null;
        try {
            URLConnection uc = new URL(url).openConnection();
            uc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows XP; DigExt)");
            in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "gb2312"));
            codeBuffer = new StringBuffer();
            String tempCode = "";
            while ((tempCode = in.readLine()) != null) {
                codeBuffer.append(tempCode).append("\n");
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return codeBuffer.toString();
    }

    public static void main(String[] args) {
        String str = null;
        str += New.getHtmlSource("http://www.oksports.com.cn/1000.htm");
        String start_str = "<table width=\"950\" border=\"0\" align=\"center\" cellpadding=\"5\" cellspacing=\"5\" class=\"tdbk\">";
        String end_str = "<p align=\"center\"><script";
        int start = str.indexOf(start_str);
        int end = str.indexOf(end_str);
        System.out.println(start + "===" + end);
        str = str.substring(start, end);
        System.out.println(str);
        String img_start_str = "<IMG ";
        String img_end_str = ">";
        List imgList = new ArrayList();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i != -1; i = str.indexOf(img_start_str)) {
            if (str.indexOf(img_start_str) > -1) {
                int img_start = str.indexOf(img_start_str);
                buf.append(str.substring(0, img_start));
                String tmp = str.substring(img_start);
                System.out.println("img ��Ϣ" + tmp);
                int img_end = tmp.indexOf(img_end_str);
                String imgTmp = tmp;
                tmp = tmp.substring(0, img_end + img_end_str.length());
                buf.append(imgTmp.substring(img_end + img_end_str.length()));
                imgList.add(tmp);
                str = buf.toString();
            } else {
                System.out.println("break");
                break;
            }
        }
        img_start_str = "<img ";
        img_end_str = ">";
        buf = new StringBuffer();
        for (int i = 0; i != -1; i = str.indexOf(img_start_str)) {
            if (str.indexOf(img_start_str) > -1) {
                int img_start = str.indexOf(img_start_str);
                buf.append(str.substring(0, img_start));
                String tmp = str.substring(img_start);
                System.out.println("img ��Ϣ" + tmp);
                int img_end = tmp.indexOf(img_end_str);
                String imgTmp = tmp;
                tmp = tmp.substring(0, img_end + img_end_str.length());
                buf.append(imgTmp.substring(img_end + img_end_str.length()));
                imgList.add(tmp);
                str = buf.toString();
            } else {
                System.out.println("break");
                break;
            }
        }
        System.out.println("�������" + str);
    }
}
