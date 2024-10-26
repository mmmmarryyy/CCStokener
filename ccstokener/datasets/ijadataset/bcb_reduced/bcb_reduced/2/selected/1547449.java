package edu.jhu.nlp.ir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class SimpleHTTPRequest {

    private HTTPRequestCallback callback;

    public void setCallback(HTTPRequestCallback callback) {
        this.callback = callback;
    }

    public void httpGetAsync(URL url) throws Exception {
        if (callback == null) throw new Exception("Callback not set before httpGetAsync");
        (new AsyncRequestHandler(url, callback)).start();
    }

    public static String httpGet(URL url) throws Exception {
        URLConnection connection = url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer content = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        return content.toString();
    }
}
