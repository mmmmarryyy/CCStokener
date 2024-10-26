package com.affectu.search;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.affectu.common.base.UtilBase;

/**
 * Sep 16, 2008
 * 
 * @author daniel nathan
 */
public class Spider extends UtilBase {

    public String getContentAsString(String url) {
        StringBuffer sb = new StringBuffer("");
        try {
            URL urlmy = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlmy.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            con.setInstanceFollowRedirects(false);
            con.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String s = "";
            while ((s = br.readLine()) != null) {
                sb.append(s + "\r\n");
            }
            con.disconnect();
        } catch (Exception ex) {
            this.logException(ex);
        }
        return sb.toString();
    }

    public Integer fetchInterger(String str) {
        Integer integer = new Integer(0);
        try {
            String REGEX = "^.*?(-?\\d+)$";
            Pattern p = Pattern.compile(REGEX, Pattern.DOTALL);
            Matcher matcher = p.matcher(str);
            if (matcher.matches()) {
                integer = new Integer(matcher.group(1));
            }
        } catch (Exception ex) {
            this.logAffectuException(ex, "出错:" + str);
        }
        return integer;
    }

    public String fetchSpecificText(String regularExpress, String contents) {
        String resultStr = "";
        try {
            Pattern p = Pattern.compile(regularExpress);
            Matcher matcher = p.matcher(contents);
            String startTag = regularExpress.substring(0, regularExpress.indexOf("["));
            String endTag = regularExpress.substring(regularExpress.indexOf("]"), regularExpress.length());
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String match = contents.substring(start, end);
                resultStr = match.replaceAll(startTag, "").replaceAll(endTag, "");
            }
        } catch (Exception ex) {
            this.logAffectuException(ex, "fetchSpecificText出错:" + contents);
        }
        return resultStr;
    }
}
