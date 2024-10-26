package edu.jlu.fuliang;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SearchHandler {

    public List<MusicEntity> search(String keyword) {
        InputStream is = doSearch(keyword);
        List<MusicEntity> musicList = parseResult(is);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return musicList;
    }

    private List<MusicEntity> parseResult(InputStream is) {
        MusicParser parser = new MusicParser();
        return parser.doPaser(is);
    }

    private InputStream doSearch(String keyword) {
        String urlStr = Config.getBaseUrl();
        InputStream is = null;
        urlStr += keyword;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) is = connection.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    public static void main(String[] args) throws Exception {
        SearchHandler handler = new SearchHandler();
        handler.search("��");
    }
}
