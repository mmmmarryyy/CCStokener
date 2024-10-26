package com.flsoft.sspider.bo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import com.flsoft.sspider.dao.BTML_Dao;
import com.flsoft.sspider.dao.ZZML_Dao;
import com.flsoft.sspider.db.DriverManager;
import com.flsoft.sspider.vo.BTML;
import com.flsoft.sspider.vo.ZZML;

public class BTML_Bo {

    private ZZML_Dao zdao;

    private BTML_Dao dao;

    public BTML_Bo(Connection conn) {
        dao = new BTML_Dao(conn);
        zdao = new ZZML_Dao(conn);
    }

    private InputStream loadSource(String url) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(HTTP.USER_AGENT, "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)");
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    public void loadData() {
        try {
            List<ZZML> zList = zdao.getList();
            String regex = "<[^>]*>";
            String bt = "", zy = "", url = "";
            List<BTML> list = new ArrayList<BTML>();
            for (ZZML zzml : zList) {
                LineIterator iterator = IOUtils.lineIterator(loadSource(zzml.getUrl()), "UTF-8");
                while (iterator.hasNext()) {
                    String line = iterator.nextLine();
                    if (StringUtils.containsIgnoreCase(line, "<h1><a href=\"/journal/summary/volume")) {
                        bt = line.replaceAll(regex, "").trim();
                        zy = iterator.nextLine().replaceAll(regex, "").trim();
                    }
                    if (StringUtils.containsIgnoreCase(line, "<a href=\"/journal/fulltext/volume")) {
                        url = "http://www.clinicmed.net" + line.substring(line.indexOf("\"") + 1, line.indexOf("?"));
                        list.add(new BTML(bt, zy, zzml.getId(), url));
                    }
                }
            }
            int i = 1;
            for (BTML btml : list) {
                System.out.println(i + "\t" + btml.getBt() + "\t" + btml.getUrl());
                dao.add(btml);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            BTML_Bo bo = new BTML_Bo(DriverManager.getConnection());
            bo.loadData();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
