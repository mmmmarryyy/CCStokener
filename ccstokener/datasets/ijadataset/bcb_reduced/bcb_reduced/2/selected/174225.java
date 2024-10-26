package com.kdev.qq.utils;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

public class BaikeSearch {

    /**
	 * @param args
	 * @throws UnsupportedEncodingException
	 */
    public static String BaiKe(String unknown) {
        String encodeurl = "";
        long sTime = System.currentTimeMillis();
        long eTime;
        try {
            String regEx = "\\#(.+)\\#";
            String searchText = "";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(unknown);
            if (m.find()) {
                searchText = m.group(1);
            }
            System.out.println("searchText :  " + searchText);
            encodeurl = URLEncoder.encode(searchText, "UTF-8");
            String url = "http://www.hudong.com/wiki/" + encodeurl;
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setConnectTimeout(10000);
            Parser parser = new Parser(conn);
            parser.setEncoding(parser.getEncoding());
            NodeFilter filtera = new TagNameFilter("DIV");
            NodeList nodes = parser.extractAllNodesThatMatch(filtera);
            String textInPage = "";
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    Node textnode = (Node) nodes.elementAt(i);
                    if ("div class=\"summary\"".equals(textnode.getText())) {
                        String temp = textnode.toPlainTextString();
                        textInPage += temp + "\n";
                    }
                }
            }
            String s = Replace(textInPage, searchText);
            eTime = System.currentTimeMillis();
            String time = "搜索[" + searchText + "]用时:" + (eTime - sTime) / 1000.0 + "s";
            System.out.println(s);
            return time + "\r\n" + s;
        } catch (Exception e) {
            e.printStackTrace();
            return "大姨妈来了";
        }
    }

    public static String Replace(String s, String searhText) {
        s = s.replace("&nbsp;", " ").replace("纠错 编辑摘要", "").replace("请用一段简单的话描述该词条，马上添加摘要。", "");
        s = s.replace("纠错", "").replace("编辑摘要", "").replace("摘要", "");
        s = s.trim();
        if ("".equals(s)) {
            s = "没有[" + searhText + "]相关数据解析,人品你懂的";
        }
        return s;
    }

    public static void main(String a[]) throws UnsupportedEncodingException {
        System.out.println(BaiKe("#天涯#sda"));
    }
}
