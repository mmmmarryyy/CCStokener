package com.uglygreencar.tribalwars.data.village;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class LiveVillageDAO extends BaseVillageDAO {

    private String url;

    public LiveVillageDAO(String url) {
        this.url = url;
    }

    protected InputStream getInputStream() {
        try {
            URL url = new URL(this.url);
            URLConnection connection = url.openConnection();
            return connection.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
