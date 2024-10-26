package com.flsoft.sspider.bo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import com.flsoft.sspider.dao.YPZS_Dao;
import com.flsoft.sspider.db.DriverManager;

public class YPZS_Bo {

    private Properties prop;

    private YPZS_Dao dao;

    public YPZS_Bo(Connection conn) throws IOException {
        super();
        dao = new YPZS_Dao(conn);
        prop = new Properties();
        InputStream is = YPZS_Bo.class.getResourceAsStream("/ypzs.properties");
        prop.load(is);
    }

    private InputStream loadSource(String url) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(HTTP.USER_AGENT, "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)");
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    private int read(String name) throws IOException {
        try {
            return Integer.parseInt(prop.getProperty(name));
        } catch (Exception e) {
            return 0;
        }
    }

    private void write(String name, String value) throws IOException {
        OutputStream os = new FileOutputStream("src/ypzs.properties");
        prop.setProperty(name, value);
        prop.store(os, "modified by mr.fan");
        os.flush();
    }

    public void loadData() throws IOException {
        int start = this.read("start");
        int end = this.read("end");
        String line = "";
        String mc = "", sm = "", ly = "";
        String regex = "<[^>]*>";
        for (int i = start; i <= end; i++) {
            try {
                String url = "http://www.pharmnet.com.cn/search/detail--32--" + i + ".html";
                LineIterator iterator = IOUtils.lineIterator(loadSource(url), "GB2312");
                while (iterator.hasNext()) {
                    line = iterator.nextLine();
                    String replaceStr = "<span style=\"font-size:14px; color:#444444; line-height:180%;\">";
                    if (StringUtils.containsIgnoreCase(line, "/images/menu_ico.gif")) {
                        mc = line.replaceAll(regex, "").trim();
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td width=\"25%\">")) {
                        ly = line.replaceAll(regex, "").trim();
                    }
                    if (StringUtils.containsIgnoreCase(line, replaceStr)) {
                        sm = line.replaceAll(replaceStr, "").replaceAll("<br><br></span></td>", "").trim();
                    }
                }
                dao.add(mc, sm, ly);
                System.out.println(i + " : " + mc);
            } catch (Exception e) {
                e.printStackTrace();
                this.write("error", this.read("error") + "," + i);
                this.write("start", "" + i);
            }
        }
        this.write("start", (this.read("end") + 1) + "");
        System.out.println("End with: " + this.read("end"));
    }

    public static void main(String[] args) {
        try {
            YPZS_Bo bo = new YPZS_Bo(DriverManager.getConnection());
            bo.loadData();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
