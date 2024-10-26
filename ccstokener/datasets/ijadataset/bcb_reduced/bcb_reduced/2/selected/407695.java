package com.jlunch.batch;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;

/**
 *
 * @author griv
 */
public abstract class AbstractLunchImporter implements LunchImporter {

    private final String encoding;

    protected final String name;

    protected AbstractLunchImporter(String name) {
        this("8859_1", name);
    }

    protected AbstractLunchImporter(String encoding, String name) {
        this.encoding = encoding;
        this.name = name;
    }

    protected String readFrom(URL url) throws ProtocolException, IOException {
        Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress("www-proxy.ikea.com", 8080));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
        conn.setRequestMethod("GET");
        conn.setAllowUserInteraction(false);
        conn.setUseCaches(false);
        conn.setDoOutput(false);
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();
        String res = readEverything(is);
        conn.disconnect();
        return res;
    }

    protected String readEverything(InputStream is) throws IOException {
        int estimatedLength = 10 * 1024;
        StringBuilder buf = new StringBuilder(estimatedLength);
        int chunkSize = Math.min(estimatedLength, 4 * 1024);
        byte[] ba = new byte[chunkSize];
        int bytesRead;
        while ((bytesRead = is.read(ba, 0, chunkSize)) >= 0) {
            if (bytesRead == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                buf.append(new String(ba, 0, bytesRead, encoding));
            }
        }
        return buf.toString();
    }

    protected String format(String input) {
        return input.replaceAll("&#246;", "�").replaceAll("&#247;", "�").replaceAll("&#228;", "�").replaceAll("&#196;", "�").replaceAll("&#229;", "�").replaceAll("&#197;", "�").replaceAll("&#233;", "�").replaceAll("&nbsp;", " ").replaceAll("&#225;", "�").replaceAll("&#232;", "�").replaceAll("å", "�").replaceAll("��", "�").replaceAll("��", "�");
    }
}
