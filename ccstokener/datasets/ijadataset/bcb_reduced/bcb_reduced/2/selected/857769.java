package cn.nkjobsearch.html;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import cn.nkjobsearch.config.ConfigJob51;
import cn.nkjobsearch.mysql.MysqlConn;
import cn.nkjobsearch.publicClass.File;

public class Com51JobSearchList extends Thread {

    public Com51JobSearchList() {
        mysql = new MysqlConn();
    }

    public void close() {
        mysql.close();
    }

    public void run() {
        for (; ConfigJob51.JOB51_SEARCHLIST_C < 5; ConfigJob51.JOB51_SEARCHLIST_C++) {
            for (; ConfigJob51.JOB51_SEARCHLIST_P < ConfigJob51.JOB51_PROVINCE.length; ConfigJob51.JOB51_SEARCHLIST_P++) {
                for (int j = 1; j <= ConfigJob51.JOB51_SEARCHLIST_MAX_PAGE; j++) {
                    int idRes[] = get51JobId(ConfigJob51.JOB51_PROVINCE[ConfigJob51.JOB51_SEARCHLIST_P], ConfigJob51.JOB51_CATEGORY[ConfigJob51.JOB51_SEARCHLIST_C], j);
                    ConfigJob51.JOB51_SEARCHLIST_TIMER = 0;
                    File.writeLog(ConfigJob51.JOB51_SEARCHLIST_LOG_PATH, ConfigJob51.JOB51_SEARCHLIST_C + "\t" + ConfigJob51.JOB51_SEARCHLIST_P + "\t" + j + "\t" + idRes[0] + "\t" + idRes[1]);
                    if (idRes[0] != ConfigJob51.JOB51_SEARCHLIST_ID_NUM_EACH_PAGE) {
                        break;
                    }
                }
            }
            ConfigJob51.JOB51_SEARCHLIST_P = 0;
        }
        ConfigJob51.JOB51_SEARCHLIST_C = 0;
        ConfigJob51.JOB51_SEARCHLIST_RUNNING = false;
    }

    private int[] get51JobId(String address, String category, int pageNum) {
        StringBuffer htmlContent = null;
        try {
            URL url = new URL(ConfigJob51.STR_51JOB_ADVANCE);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            OutputStream raw = connection.getOutputStream();
            OutputStream buf = new BufferedOutputStream(raw);
            OutputStreamWriter out = new OutputStreamWriter(buf, "gb2312");
            out.write("jobarea=" + address + "&funtype=" + category + "&curr_page=" + pageNum + "");
            out.flush();
            out.close();
            InputStream in = connection.getInputStream();
            in = new BufferedInputStream(in);
            Reader r = new InputStreamReader(in);
            int c;
            htmlContent = new StringBuffer();
            while ((c = r.read()) != -1) {
                htmlContent.append((char) c);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Pattern p = Pattern.compile(JOB51_SEARCHLIST_URL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(htmlContent);
        int idSum = 0;
        int writeToDBSuccessful = 0;
        while (matcher.find()) {
            String s = matcher.group();
            String sql = "insert into `job51`(`id`,`retryCnt`,`Category`) values('" + s.replaceAll("[^0-9]", "") + "','0','" + category + "')";
            if (mysql.executeInsert(sql)) {
                writeToDBSuccessful++;
            }
            idSum++;
        }
        return new int[] { idSum, writeToDBSuccessful };
    }

    private MysqlConn mysql = null;

    private static final String JOB51_SEARCHLIST_URL_PATTERN = "/jobsearch/show_job_detail.php\\?id=\\([\\d]{6,14}\\)";
}
