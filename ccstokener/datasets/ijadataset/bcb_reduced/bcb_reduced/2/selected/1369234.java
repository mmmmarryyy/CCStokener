package org.t2framework.t2.adapter.s2.it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

public abstract class AbstractS2IntegationTestCase extends TestCase {

    protected SDLoader loader;

    protected WebAppContext context;

    @Override
    public void runBare() throws Throwable {
        this.loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        this.context = new WebAppContext("/s2it", "s2adapter_it/webapp_it");
        context.addClassPath("bin");
        context.addClassPath("s2adapter_it/webapp_it/WEB-INF/classes");
        context.addLibDirPath("lib");
        context.addLibDirPath("lib/s2adapter");
        context.addLibDirPath("s2adapter_it/webapp_it/WEB-INF/lib");
        loader.addWebAppContext(context);
        try {
            loader.start();
            super.runBare();
        } finally {
            loader.stop();
        }
    }

    protected String getRequestContent(String urlText, String method) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestMethod(method);
        urlcon.setUseCaches(false);
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
