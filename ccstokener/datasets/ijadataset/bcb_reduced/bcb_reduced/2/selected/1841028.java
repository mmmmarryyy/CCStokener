package com.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * 
 * @author 
 *
 */
public class HtmlUtil {

    /**
	 * ��ȡ���ҳ���html
	 * 
	 * @param url
	 *            ��ȡ��url��ַ
	 * @return
	 */
    public String getHtmlSource(String url) {
        StringBuffer codeBuffer = null;
        BufferedReader in = null;
        URLConnection uc = null;
        try {
            uc = new URL(url).openConnection();
            uc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows XP; DigExt)");
            in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
            codeBuffer = new StringBuffer();
            String tempCode = "";
            while ((tempCode = in.readLine()) != null) {
                codeBuffer.append(tempCode).append("\n");
            }
            in.close();
            tempCode = null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != in) in = null;
            if (null != uc) uc = null;
        }
        return codeBuffer.toString();
    }

    /**
	 * ��ȡhtml ���ʼ�ַ�ͽ����ַ������Ĭ��Ϊ��ͷ�������
	 * 
	 * @param url
	 *            Ҫ��ȡ��url��ַ
	 * @param startStr
	 *            html��ʼ�ַ�
	 * @param endStr
	 *            html�����ַ�
	 * @return
	 */
    public String getHtml(String url, String startStr, String endStr) {
        if (null == url || "".equalsIgnoreCase(url)) return "";
        String str = getHtmlSource(url);
        int start = str.indexOf(startStr);
        if (start < 0) {
            start = 0;
        }
        int end = str.indexOf(endStr);
        if (end < 0) {
            end = str.length();
        }
        return str.substring(start, end);
    }

    /**
	 * ����2������ obj[0]Ϊ���˺��html obj[1]Ϊimg����list
	 * 
	 * @param srcHtml
	 *            src html
	 * @param startFilterStr
	 *            ���˵Ŀ�ʼ�ַ�
	 * @param endFilterStr
	 *            ���˵Ľ����ַ�
	 * @return
	 */
    public Object[] fiterImg(String srcHtml, String startFilterStr, String endFilterStr) {
        Object obj[] = new Object[2];
        List<String> imgList = new ArrayList<String>();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i != -1; i = srcHtml.indexOf(startFilterStr)) {
            if (srcHtml.indexOf(startFilterStr) > -1) {
                int imgStart = srcHtml.indexOf(startFilterStr);
                buf.append(srcHtml.substring(0, imgStart));
                String tmp = srcHtml.substring(imgStart);
                System.out.println("img ��Ϣ" + tmp);
                int imgEnd = tmp.indexOf(endFilterStr);
                if (imgEnd < -1 || imgEnd > srcHtml.length()) {
                    break;
                }
                String endTmp = tmp;
                tmp = tmp.substring(0, imgEnd + endFilterStr.length());
                buf.append(endTmp.substring(imgEnd + endFilterStr.length()));
                imgList.add(tmp);
                srcHtml = buf.toString();
                tmp = null;
                endTmp = null;
            } else {
                System.out.println("break");
                break;
            }
        }
        obj[0] = buf.toString();
        obj[1] = imgList;
        imgList = null;
        buf = null;
        return obj;
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
