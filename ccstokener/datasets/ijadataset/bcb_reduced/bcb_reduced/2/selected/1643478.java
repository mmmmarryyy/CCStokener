package net.sf.barttracker.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

/** Retrieves the contents of a given URL and returns them in a String */
public class URLFetcher implements Callable<String> {

    private final URL url;

    public URLFetcher(String url) {
        this(stringToUrl(url));
    }

    private static URL stringToUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URLFetcher(URL url) {
        this.url = url;
    }

    public String call() throws Exception {
        InputStream stream = url.openStream();
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
