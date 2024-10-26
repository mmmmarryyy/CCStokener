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
import com.flsoft.sspider.dao.GCYPSJK_Dao;
import com.flsoft.sspider.db.DriverManager;
import com.flsoft.sspider.vo.GCYPSJK;

public class GCYPSJK_Bo {

    private Properties prop = null;

    private GCYPSJK_Dao dao = null;

    public GCYPSJK_Bo(Connection conn) throws IOException {
        dao = new GCYPSJK_Dao(conn);
        prop = new Properties();
        InputStream is = YPZS_Bo.class.getResourceAsStream("/gcypsjk.properties");
        prop.load(is);
    }

    private String read(String name) throws IOException {
        return prop.getProperty(name);
    }

    private void write(String name, String value) throws IOException {
        OutputStream os = new FileOutputStream("src/gcypsjk.properties");
        prop.setProperty(name, value);
        prop.store(os, "modified by mr.fan");
        os.flush();
    }

    private InputStream loadSource(String url) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(HTTP.USER_AGENT, "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)");
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    public void loadData() throws IOException {
        int start = Integer.parseInt(this.read("start"));
        int end = Integer.parseInt(this.read("end"));
        String error = "";
        String line = "";
        String regex = "<[^>]*>";
        for (int i = start; i <= end; i++) {
            try {
                String url = "http://www.pharmnet.com.cn/search/detail--24--" + i + ".html";
                LineIterator iterator = IOUtils.lineIterator(loadSource(url), "GB2312");
                GCYPSJK yp = new GCYPSJK();
                while (iterator.hasNext()) {
                    line = iterator.nextLine();
                    if (line.startsWith("<TITLE>")) {
                        yp.setMc(line.split("-")[1]);
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td width=\"12%\" align=\"right\" bgcolor=\"f6f6f6\">��Ʒ���</td>")) {
                        yp.setYplb(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">��׼�ĺţ�</td>")) {
                        yp.setPzwh(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">��׼���ڣ�</td>")) {
                        yp.setPzrq(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">ԭ�ĺţ�</td>")) {
                        yp.setYwh(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">�����ͣ�</td>")) {
                        yp.setGgjx(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">ҩƷ��λ�룺</td>")) {
                        yp.setYpbwm(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">��λ�뱸ע��</td>")) {
                        yp.setBwmbz(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">���λ��</td>")) {
                        int idx = line.indexOf("(����鿴)");
                        String replaceStr = iterator.nextLine().replaceAll(regex, "").trim();
                        if (idx == -1) {
                            idx = replaceStr.length();
                        }
                        yp.setScdw(replaceStr.substring(0, idx));
                    }
                    if (StringUtils.containsIgnoreCase(line, "<td align=\"right\" bgcolor=\"f6f6f6\">����ַ��</td>")) {
                        yp.setScdz(iterator.nextLine().replaceAll(regex, "").trim());
                    }
                }
                System.out.println(yp);
            } catch (Exception e) {
                e.printStackTrace();
                error = this.read("error");
                this.write("error", error + "," + i);
                this.write("start", "" + i);
            }
        }
    }

    public static void main(String[] args) {
        try {
            GCYPSJK_Bo bo = new GCYPSJK_Bo(DriverManager.getConnection());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
