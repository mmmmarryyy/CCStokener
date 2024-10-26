package cn.nkjobsearch.html;

import java.io.*;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import cn.nkjobsearch.config.Config;
import cn.nkjobsearch.config.ConfigJob51;
import cn.nkjobsearch.mysql.MysqlConn;
import cn.nkjobsearch.publicClass.Strings;

public class Com51JobContent extends Thread {

    public Com51JobContent() {
        mysql = new MysqlConn();
    }

    public void close() {
        mysql.close();
    }

    public void run() {
        ResultSet rs = null;
        String infoId = null;
        String category = null;
        int count = 0;
        while (true) {
            try {
                rs = mysql.executeQuery("select * from `job51` where `retryCnt`<" + Config.CONTENT_MAX_TRY_NUM + " order by `retryCnt` limit 0,1;");
                if (rs.next()) {
                    infoId = rs.getString("id");
                    category = rs.getString("Category");
                    if (get51JobHtml(infoId, category)) {
                        System.out.println(++count);
                    } else {
                        System.out.println("ERROR\t" + infoId);
                    }
                    ConfigJob51.JOB51_CONTENT_TIMER = 0;
                } else {
                    ConfigJob51.JOB51_CONTENT_RUNNING = false;
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean get51JobHtml(String infoId, String category) {
        StringBuffer htmlContent = new StringBuffer();
        String title = null, orgName = null, city = null;
        String exp = null, pubDate = null, edu = null, salary = null, jobIntro = null, pubURL = null;
        pubURL = ConfigJob51.JOB51_CONTENT_DETAIL_URL + "(" + infoId + ")";
        try {
            URL url = new URL(pubURL);
            URLConnection connection = url.openConnection();
            InputStream in = connection.getInputStream();
            in = new BufferedInputStream(in);
            Reader r = new InputStreamReader(in);
            int c;
            while ((c = r.read()) != -1) {
                htmlContent.append((char) c);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Pattern pattern1 = Pattern.compile(patternStringTitle, Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(htmlContent);
        if (matcher1.find()) {
            title = htmlContent.substring(matcher1.start() + 46, matcher1.end() - 14);
        }
        Pattern pattern2 = Pattern.compile(patternStringOrgName, Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(htmlContent);
        if (matcher2.find()) {
            orgName = htmlContent.substring(matcher2.start() + 33, matcher2.end() - 5);
        }
        Pattern pattern4 = Pattern.compile(patternStringAddress, Pattern.CASE_INSENSITIVE);
        Matcher matcher4 = pattern4.matcher(htmlContent);
        if (matcher4.find()) {
            city = htmlContent.substring(matcher4.start() + 56, matcher4.end() - 5);
        }
        Pattern pattern5 = Pattern.compile(patternStringExp, Pattern.CASE_INSENSITIVE);
        Matcher matcher5 = pattern5.matcher(htmlContent);
        if (matcher5.find()) {
            exp = htmlContent.substring(matcher5.start() + 52, matcher5.end() - 5);
        }
        Pattern pattern6 = Pattern.compile(patternStringPubDate, Pattern.CASE_INSENSITIVE);
        Matcher matcher6 = pattern6.matcher(htmlContent);
        if (matcher6.find()) {
            pubDate = htmlContent.substring(matcher6.start() + 56, matcher6.end() - 5);
        }
        Pattern pattern7 = Pattern.compile(patternStringEdu, Pattern.CASE_INSENSITIVE);
        Matcher matcher7 = pattern7.matcher(htmlContent);
        if (matcher7.find()) {
            edu = htmlContent.substring(matcher7.start() + 74, matcher7.end() - 5);
        }
        Pattern pattern8 = Pattern.compile(patternStringSalary, Pattern.CASE_INSENSITIVE);
        Matcher matcher8 = pattern8.matcher(htmlContent);
        if (matcher8.find()) {
            salary = htmlContent.substring(matcher8.start() + 52, matcher8.end() - 5);
        }
        Pattern pattern9 = Pattern.compile(patternStringJobIntro, Pattern.CASE_INSENSITIVE);
        Matcher matcher9 = pattern9.matcher(htmlContent);
        if (matcher9.find()) {
            jobIntro = Strings.strip_tags(htmlContent.substring(matcher9.start() + 26, matcher9.end() - 28));
        }
        if (null != title && "" != title) {
            String insertSql = "INSERT INTO  `jobinfo` (`title` ,  `orgName` ,  " + "`city` ,  `exp` ,  `pubDate` ,  `edu` ,  `category` ,  " + "`salary` ,  `jobIntro` ,  `jobNature` ,  `fromSite` ,  `jobURLId` ) " + "VALUES('" + title + "', " + (orgName != null ? ("'" + orgName + "'") : "NULL") + " , " + (city != null ? ("'" + city + "'") : "NULL") + ", " + (exp != null ? ("'" + exp + "'") : "NULL") + ", '" + pubDate + "', " + (edu != null ? ("'" + edu + "'") : "NULL") + ", " + (category != null ? ("'" + category + "'") : "NULL") + ", " + (salary != null ? ("'" + salary + "'") : "NULL") + ", " + (jobIntro != null ? ("'" + jobIntro.replaceAll("'", "\\\\'") + "'") : "NULL") + ", " + "NULL" + ", '5', '" + infoId + "');";
            if (mysql.executeInsert(insertSql)) {
                mysql.executeUpdate("update job51 set retryCnt = '9' where Id=" + infoId);
                return true;
            } else {
                mysql.executeUpdate("update job51 set `retryCnt` = `retryCnt` + 1 where Id=" + infoId);
                return false;
            }
        } else {
            mysql.executeUpdate("update job51 set `retryCnt` = `retryCnt` + 1 where Id=" + infoId);
            return false;
        }
    }

    private MysqlConn mysql = null;

    private static String patternStringTitle = "class=[\\S]{1}title02[\\S]{1}[\\s]{1}style=[\\S]{1}padding-left:15px;[\\S]{1}>[\\s]+[\\S]+[\\s]+</td></tr>";

    private static String patternStringOrgName = "<td[\\s]{1}align=[\\S]{1}left[\\S]{1}[\\s]{1}class=[\\S]{1}title02[\\S]{1}>[\\S]+</TD>";

    private static String patternStringAddress = "工作地点：</td>[\\s]+<td[\\s]{1}width=[\\S]{1}14%[\\S]{1}[\\s]{1}style=[\\S]{1}padding-left:8px;[\\S]{1}>[\\S]+</td>";

    private static String patternStringExp = "工作年限：</td><td[\\s]{1}width=[\\S]{1}14%[\\S]{1}[\\s]{1}style=[\\S]{1}padding-left:8px;[\\S]{1}>[\\S]+</td>";

    private static String patternStringPubDate = "发布日期：</td>[\\s]+<td[\\s]{1}width=[\\S]{1}14%[\\S]{1}[\\s]{1}style=[\\S]{1}padding-left:8px;[\\S]{1}>[\\S]+</td>";

    private static String patternStringEdu = "学[\\S]{24}历：</td><td[\\s]{1}width=[\\S]{1}14%[\\S]{1}[\\s]{1}style=[\\S]{1}padding-left:8px;[\\S]{1}>[\\S]+</td>";

    private static String patternStringJobIntro = "职位描述[(\\S|\\s)]+</p>\\s+</td>[\\s]{3}</tr>[\\s]{3}</table>";

    private static String patternStringSalary = "薪水范围：</td><td[\\s]{1}width=[\\S]{1}14%[\\S]{1}[\\s]{1}style=[\\S]{1}padding-left:8px;[\\S]{1}>[\\S]+</td>";
}
