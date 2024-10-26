package org.phylowidget.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class PhyloTransformServices {

    private static String replaceXmlChars(String s) {
        s = s.replaceAll("&", "&amp;");
        return s;
    }

    public static String transformTree(String urlString, String nexml) throws Exception {
        nexml = replaceXmlChars(nexml);
        String data = URLEncoder.encode("nexml", "UTF-8") + "=" + URLEncoder.encode(nexml, "UTF-8");
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        wr.close();
        rd.close();
        String s = sb.toString();
        return s;
    }
}
