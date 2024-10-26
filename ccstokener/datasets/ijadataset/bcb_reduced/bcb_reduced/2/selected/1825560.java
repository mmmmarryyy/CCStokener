package org.furthurnet.xmlparser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import org.furthurnet.datastructures.supporting.Common;
import org.furthurnet.furi.Cfg;
import org.furthurnet.furi.Res;

public class HttpThread extends Thread {

    private String url;

    private HttpRequestListener listener;

    private static String proxyAuth;

    public HttpThread(String _url, HttpRequestListener _listener) {
        url = _url;
        listener = _listener;
    }

    public void run() {
        org.furthurnet.furi.FurthurThread.logPid("xmlparser.HttpThread " + hashCode());
        try {
            listener.htmlReceived(fetchUrl(url, false));
        } catch (IOException e) {
        }
    }

    public static void setProxyAuth(String auth) {
        proxyAuth = auth;
    }

    private static String fetchUrl(String url, boolean keepLineEnds) throws IOException, MalformedURLException {
        URLConnection destConnection = (new URL(url)).openConnection();
        BufferedReader br;
        String inputLine;
        StringBuffer doc = new StringBuffer();
        String contentEncoding;
        destConnection.setRequestProperty("Accept-Encoding", "gzip");
        if (proxyAuth != null) destConnection.setRequestProperty("Proxy-Authorization", proxyAuth);
        destConnection.connect();
        contentEncoding = destConnection.getContentEncoding();
        if ((contentEncoding != null) && contentEncoding.equals("gzip")) {
            br = new BufferedReader(new InputStreamReader(new GZIPInputStream(destConnection.getInputStream())));
        } else {
            br = new BufferedReader(new InputStreamReader(destConnection.getInputStream()));
        }
        while ((inputLine = br.readLine()) != null) {
            if (keepLineEnds) doc.append(inputLine + "\n"); else doc.append(inputLine);
        }
        br.close();
        return doc.toString();
    }

    public static String fetchFurthurFile(String filename, boolean keepLineEnds) throws IOException, MalformedURLException {
        String result;
        final String noDotVersion = Common.replaceAll(Res.getStr("Program.Version"), ".", "");
        String url;
        if (filename.indexOf('?') == -1) {
            url = Cfg.mUrlBase + filename + "?version=" + noDotVersion;
        } else {
            url = Cfg.mUrlBase + filename + "&version=" + noDotVersion;
        }
        return fetchUrl(url, keepLineEnds);
    }
}
