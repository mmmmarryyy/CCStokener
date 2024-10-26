package com.flsoft.sspider.bo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
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
import com.flsoft.sspider.dao.ZZNR_Dao;
import com.flsoft.sspider.db.DriverManager;
import com.flsoft.sspider.vo.BTML;
import com.flsoft.sspider.vo.ZZNR;

public class ZZNR_Bo {

    private BTML_Dao bdao;

    private ZZNR_Dao dao;

    public ZZNR_Bo(Connection conn) {
        bdao = new BTML_Dao(conn);
        dao = new ZZNR_Dao(conn);
    }

    private InputStream loadSource(String url) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(HTTP.USER_AGENT, "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)");
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    public void loadDate() {
        try {
            List<BTML> mlList = bdao.getList(1577);
            String regex = "<[^>]*>";
            for (BTML btml : mlList) {
                LineIterator iterator = IOUtils.lineIterator(loadSource(btml.getUrl()), "UTF-8");
                String zzxx = "", zy = "", gjc = "", content = "";
                while (iterator.hasNext()) {
                    String line = iterator.nextLine();
                    if (line.trim().startsWith("<meta name=\"description")) {
                        zy = line.trim().replaceAll("<meta name=\"description\" content=\"", "").replaceAll("\" />", "");
                    }
                    if (line.trim().startsWith("<meta name=\"keywords")) {
                        gjc = line.trim().replaceAll("<meta name=\"keywords\" content=\"", "").replaceAll("\" />", "");
                    }
                    if (line.trim().endsWith("</br></p>") && line.trim().startsWith("<p style=\"font-size:12px;\">")) {
                        zzxx = line.replaceAll(regex, "").trim();
                    }
                    StringBuilder sb = new StringBuilder();
                    if (StringUtils.containsIgnoreCase(line, "fulltextMainContent")) {
                        test: while (true) {
                            String aline = iterator.nextLine();
                            if (aline.trim().startsWith("<font style=\"float:right")) {
                                content = sb.toString();
                                sb = new StringBuilder();
                                break test;
                            }
                            sb.append(aline);
                        }
                    }
                }
                dao.add(new ZZNR(zzxx, zy, gjc, content, btml.getId()));
                System.out.println(btml.getId() + "\t" + btml.getBt());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ZZNR_Bo bo = new ZZNR_Bo(DriverManager.getConnection());
            bo.loadDate();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
