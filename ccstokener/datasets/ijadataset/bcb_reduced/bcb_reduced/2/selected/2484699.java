package com.ibm.wala.cast.js.html;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.util.debug.Assertions;

public class WebUtil {

    public static final String preamble = "preamble.js";

    private static IHtmlParserFactory factory = new IHtmlParserFactory() {

        public IHtmlParser getParser() {
            return new JerichoHtmlParser();
        }
    };

    public static void setFactory(IHtmlParserFactory factory) {
        WebUtil.factory = factory;
    }

    public static Set<MappedSourceModule> extractScriptFromHTML(String url) {
        try {
            if (!url.startsWith("file://")) {
                url = "file://" + url;
            }
            return extractScriptFromHTML(new URL(url));
        } catch (MalformedURLException e) {
            Assertions.UNREACHABLE(e.toString());
            return null;
        }
    }

    public static Set<MappedSourceModule> extractScriptFromHTML(URL url) {
        try {
            JSSourceExtractor extractor = new DefaultSourceExtractor();
            return extractor.extractSources(url, factory.getParser(), new IdentityUrlResolver());
        } catch (IOException e) {
            throw new RuntimeException("trouble with " + url, e);
        }
    }

    public static void main(String[] args) throws MalformedURLException {
        System.err.println(extractScriptFromHTML(new URL(args[0])));
    }

    public static InputStream getStream(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setDefaultUseCaches(false);
        conn.setUseCaches(false);
        return conn.getInputStream();
    }
}
