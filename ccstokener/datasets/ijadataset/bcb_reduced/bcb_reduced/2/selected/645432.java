package com.flsoft.sspider.bo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import com.flsoft.sspider.dao.Error_Dao;
import com.flsoft.sspider.dao.YPSYSMS_Dao;
import com.flsoft.sspider.db.DriverManager;
import com.flsoft.sspider.vo.Temp;
import com.flsoft.sspider.vo.YPSYSMS;

public class YPSYSMS_Bo {

    private YPSYSMS_Dao dao;

    private Error_Dao edao;

    private Connection conn;

    public YPSYSMS_Bo(Connection conn) {
        this.conn = conn;
        dao = new YPSYSMS_Dao(conn);
        edao = new Error_Dao(conn);
    }

    private void save(YPSYSMS sms) throws SQLException {
        dao.save(sms);
    }

    private void del(int id) throws SQLException {
        dao.del(id);
    }

    private InputStream loadSource(String url) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(HTTP.USER_AGENT, "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)");
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    public void loadData(String url) {
        try {
            LineIterator iterator = IOUtils.lineIterator(loadSource(url), "GB2312");
            String line = "";
            String regex = "<[^>]*>";
            while (iterator.hasNext()) {
                line = iterator.nextLine();
                String tmpUrlMc = "", tmpUrl = "";
                if (line.startsWith("<li><a href=\"/")) {
                    tmpUrlMc = line.substring(line.indexOf("title=\"") + "title=\"".length(), line.indexOf("\" target"));
                    tmpUrl = "http://www.pharmnet.com.cn" + line.substring(line.indexOf("/product"), line.indexOf("\" title"));
                    String sql = "INSERT INTO `medicine`.`temp`(`id`,`mc`,`url`) VALUES ( NULL,?,?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, tmpUrlMc);
                    ps.setString(2, tmpUrl);
                    System.out.println(tmpUrlMc + "\t" + tmpUrl);
                    ps.execute();
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            this.err("----", url, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            this.err("----", url, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            this.err("----", url, e.getMessage());
        }
    }

    public void loadToDB(int index, int lenght) {
        String url = "";
        String regex = "<[^>]*>";
        try {
            List<Temp> list = dao.getTemp(index, lenght);
            int l = 1;
            for (Temp temp : list) {
                LineIterator detailIterator = IOUtils.lineIterator(loadSource(temp.getUrl()), "GB2312");
                YPSYSMS sms = new YPSYSMS();
                sms.setMc(temp.getMc());
                String detailLine = "";
                while (detailIterator.hasNext()) {
                    detailLine = detailIterator.nextLine();
                    if (detailLine.startsWith("    <td align=\"center\" class=\"green\">")) {
                        String lineTag = detailLine.replaceAll(regex, "").trim();
                        if (lineTag.equals("Ӣ�����")) {
                            sms.setYwmc(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("��Ʒ����")) {
                            sms.setCpfl(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("��;����")) {
                            sms.setYtfl(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("��Ҫ�ɷ�")) {
                            sms.setZycf(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("��������")) {
                            sms.setJx(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("�á���;")) {
                            sms.setYt(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("�÷�����")) {
                            sms.setYfyl(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                        if (lineTag.equals("��Ʒ˵��")) {
                            sms.setCpsm(detailIterator.nextLine().replaceAll(regex, "").trim());
                        }
                    }
                }
                this.save(sms);
                this.del(temp.getId());
                System.out.println(sms.getMc() + "\t" + temp.getUrl());
                if (l == list.size()) {
                    System.out.println("======������TMP���ID�ǣ�" + temp.getId() + "========");
                    continue;
                }
                l++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            this.err("----", url, e.getMessage());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            this.err("----", url, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            this.err("----", url, e.getMessage());
        }
    }

    public void err(String mc, String url, String cause) {
        try {
            edao.save(mc, url, cause);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void testTmp() {
        try {
            List<Temp> list = dao.getTemp(0, 10);
            for (Temp temp : list) {
                System.out.println(temp.getId() + ":" + temp.getMc() + "\t" + temp.getUrl());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        YPSYSMS_Bo bo = new YPSYSMS_Bo(DriverManager.getConnection());
        bo.loadToDB(0, 684);
    }
}
