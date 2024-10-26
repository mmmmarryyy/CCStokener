package com.dm.net;

import java.io.*;
import java.net.*;

class URLConnectionSend {

    public static void main(String[] args) throws IOException {
        String result = URLEncoder.encode("a b");
        System.out.println(result);
        String[] table = { "name", "hellojava", "age", "24", "sex", "femail" };
        URL url;
        url = new URL("http://168.192.0.139");
        URLConnection uc = url.openConnection();
        if (!(uc instanceof HttpURLConnection)) {
            System.err.println("Wrong connection type");
            return;
        }
        uc.setDoOutput(true);
        uc.setUseCaches(false);
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        String content = buildContent(table);
        uc.setRequestProperty("Content-Length", "" + content.length());
        HttpURLConnection hc = (HttpURLConnection) uc;
        hc.setRequestMethod("POST");
        OutputStream os = uc.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeBytes(content);
        dos.flush();
        dos.close();
        InputStream is = uc.getInputStream();
        int ch;
        while ((ch = is.read()) != -1) System.out.print((char) ch);
        is.close();
    }

    static String buildContent(String[] args) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            String encodedItem = URLEncoder.encode(args[i]);
            sb.append(encodedItem);
            if (i % 2 == 0) sb.append("="); else sb.append("&");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
