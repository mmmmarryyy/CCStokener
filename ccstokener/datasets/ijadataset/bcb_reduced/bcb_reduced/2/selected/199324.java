package com.james.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MakeHtml {

    public String getHtmlCode(String httpUrl) {
        String htmlCode = "";
        HttpURLConnection connection = null;
        try {
            InputStream in;
            URL url = new java.net.URL(httpUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/4.0");
            connection.setRequestProperty("Content-type", "text/html");
            connection.setRequestProperty("Accept-Charset", "utf-8");
            connection.setRequestProperty("Accept-Encoding", "utf-8");
            connection.setRequestProperty("contentType", "utf-8");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                in = connection.getInputStream();
                java.io.BufferedReader breader = new BufferedReader(new InputStreamReader(in, "utf-8"));
                String currentLine;
                Pattern p = Pattern.compile("charset=utf-8\"", Pattern.CASE_INSENSITIVE);
                while ((currentLine = breader.readLine()) != null) {
                    Matcher m = p.matcher(currentLine);
                    currentLine = m.replaceAll("charset=gb2312\"");
                    htmlCode += "\n" + currentLine;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return htmlCode;
    }

    private boolean writeHtml2file(String filePath, String info, boolean flag) {
        if (info.equals("")) return false;
        PrintWriter pw = null;
        try {
            File writeFile = new File(filePath);
            boolean isExit = writeFile.exists();
            if (isExit != true) {
                writeFile.createNewFile();
            } else {
                if (flag) {
                    writeFile.delete();
                    writeFile.createNewFile();
                }
            }
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filePath, true), "gbk"));
            pw.println(info);
            pw.close();
        } catch (Exception ex) {
            return false;
        } finally {
            pw.close();
        }
        return true;
    }

    public boolean writeHtml(String filePath, String url, boolean flag) {
        try {
            File writeFile = new File(filePath);
            boolean isExit = writeFile.exists();
            if (isExit && flag) {
                writeFile.delete();
            }
        } catch (Exception ex) {
            return false;
        } finally {
        }
        return writeHtml2file(filePath, getHtmlCode(url), flag);
    }

    public static void main(String[] args) {
        MakeHtml mkhtml = new MakeHtml();
        String url = "http://www.yntrip.com/index.jsp";
        mkhtml.writeHtml("D:/project/travel_yninfo/build/web/indexO.html", url, true);
    }
}
