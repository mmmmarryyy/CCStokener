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
import com.flsoft.sspider.dao.ZZML_Dao;
import com.flsoft.sspider.db.DriverManager;
import com.flsoft.sspider.vo.ZZML;

public class ZZML_Bo {

    private ZZML_Dao dao;

    /**
	 * @param conn
	 */
    public ZZML_Bo(Connection conn) {
        super();
        dao = new ZZML_Dao(conn);
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
            String firstUrl = "http://www.clinicmed.net/journal/archives";
            LineIterator iterator = IOUtils.lineIterator(loadSource(firstUrl), "UTF-8");
            String kh = "", url = "";
            String regex = "<[^>]*>";
            List<ZZML> list = new ArrayList<ZZML>();
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                String replaceStr = "<li style=\"font-size: 14px; \">";
                if (StringUtils.containsIgnoreCase(line, replaceStr)) {
                    kh = line.replaceAll(regex, "").trim();
                }
                if (line.trim().startsWith("Ŀ¼")) {
                    url = "http://www.clinicmed.net" + line.substring(line.indexOf("\"") + 1, line.indexOf("?"));
                    list.add(new ZZML(kh, url));
                }
            }
            for (int i = list.size() - 1; i >= 0; i--) {
                ZZML ml = list.get(i);
                System.out.println(ml.getKh() + "\t" + ml.getUrl());
                dao.add(ml.getKh(), ml.getUrl());
            }
            System.out.println(list.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ZZML_Bo bo = new ZZML_Bo(DriverManager.getConnection());
            bo.loadData();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
