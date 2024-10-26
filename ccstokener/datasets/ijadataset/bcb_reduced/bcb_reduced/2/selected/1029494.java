package net.charabia.ac;

import net.charabia.ac.spedia.*;
import java.net.*;
import java.io.*;

public class BrowserStreamHandlerFactory implements URLStreamHandlerFactory {

    static {
        System.out.println("REGISTERING BROWSER STREAM FACTORY");
        URL.setURLStreamHandlerFactory(new BrowserStreamHandlerFactory());
    }

    private class ACMapsURLStreamHandler extends URLStreamHandler {

        protected void parseURL(URL u, String spec, int start, int limit) {
            super.parseURL(u, spec, start, limit);
            System.out.println("parseURL");
        }

        protected URLConnection openConnection(URL u) throws IOException {
            System.out.println("ACMAPS, url host=" + u.getHost());
            System.out.println("ACMAPS, url path=" + u.getPath());
            System.out.println("ACMAPS, url file=" + u.getFile());
            String newurl;
            String base = Configuration.getConfiguration().get(ACMapsBase.ACMAPS_URLBASE_OPTION, ACMapsBase.ACMAPS_URLBASE_DEFAULT_VALUE);
            if (base.endsWith("/")) newurl = base + u.getFile(); else newurl = base + "/" + u.getFile();
            return new URL(newurl).openConnection();
        }
    }

    private class BrowserHttpURLStreamHandler extends URLStreamHandler {

        protected void parseURL(URL u, String spec, int start, int limit) {
            super.parseURL(u, spec, start, limit);
            System.out.println("parseURL");
        }

        protected URLConnection openConnection(URL u) throws IOException {
            URL httpurl;
            if (u.getPort() != -1) httpurl = new URL("http", u.getHost(), u.getPort(), u.getFile()); else httpurl = new URL("http", u.getHost(), u.getFile());
            System.out.println("Transformed " + u.getProtocol() + " into http");
            return httpurl.openConnection();
        }
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        System.out.println("MY FACTORY");
        if (protocol.equals("acmaps")) {
            return new ACMapsURLStreamHandler();
        }
        return null;
    }
}
