package it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

public abstract class AbstractItTestCase extends TestCase {

    SDLoader loader;

    WebAppContext context;

    @Override
    public void runBare() throws Throwable {
        this.loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        this.context = new WebAppContext("/it", "webapp");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        context.addLibDirPath("webapp/WEB-INF/lib");
        loader.addWebAppContext(context);
        try {
            loader.start();
            super.runBare();
        } finally {
            loader.stop();
        }
    }

    protected String getRequestContent(String urlText, String method, Map<String, String> paramMap, Map<String, String> headerMap) throws Exception {
        if (paramMap != null) {
            urlText += (urlText.indexOf("?") == -1) ? "?" : "";
            for (Entry<String, String> paramEntry : paramMap.entrySet()) {
                urlText += URLEncoder.encode(paramEntry.getKey(), "UTF-8") + "=" + URLEncoder.encode(paramEntry.getValue(), "UTF-8");
            }
        }
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestMethod(method);
        urlcon.setUseCaches(false);
        if (headerMap != null) {
            for (Entry<String, String> headerEntry : headerMap.entrySet()) {
                urlcon.addRequestProperty(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }

    protected String getRequestContent(String urlText, String method) throws Exception {
        return getRequestContent(urlText, method, null, null);
    }
}
