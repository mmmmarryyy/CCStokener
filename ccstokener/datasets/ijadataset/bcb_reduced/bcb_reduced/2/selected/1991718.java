package services.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class TextBasedDownloader implements Downloader {

    public StringBuffer get(URL url) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer page = new StringBuffer();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            String utf = new String(line.getBytes("UTF-8"), "UTF-8");
            page.append(utf);
        }
        bufferedReader.close();
        return page;
    }
}
