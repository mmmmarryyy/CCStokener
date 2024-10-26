package org.qsari.effectopedia.gui.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HTTPCommunicator {

    public HTTPCommunicator() {
        cookies = new HashMap<String, String>();
    }

    public HttpURLConnection openConnection(URL url, boolean doInput, boolean doOutput, String request, String data, boolean setCookie) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(doInput);
            connection.setDoOutput(doOutput);
            connection.setRequestMethod(request);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.0; de-DE; rv:1.7.5) Gecko/20041122 Firefox/1.0");
            connection.setRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            if (data != null) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(data.getBytes().length));
            }
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setRequestProperty("Keep-Alive", "300");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Referer", url.toString());
            if (setCookie) connection.setRequestProperty("Cookie", returnCookies());
            connection.connect();
            return connection;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeData(HttpURLConnection connection, String data) {
        DataOutputStream out;
        try {
            out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(data);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readData(HttpURLConnection connection) {
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line = in.readLine();
            while (line != null) {
                response.append(line);
                System.out.println(line);
                line = in.readLine();
            }
            in.close();
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveCookies(URLConnection connection) {
        for (int i = 0; ; i++) {
            String headerName = connection.getHeaderFieldKey(i);
            String headerValue = connection.getHeaderField(i);
            if (headerName == null && headerValue == null) break;
            if ("Set-Cookie".equalsIgnoreCase(headerName)) {
                String[] fields = headerValue.split(";\\s*");
                String cookieValue = fields[0];
                String cookieValueName = "";
                String cookieValueValue = "";
                if (cookieValue.indexOf("=") != -1) {
                    String[] sessionId = cookieValue.split("=");
                    cookieValueName = sessionId[0];
                    cookieValueValue = sessionId[1];
                } else {
                    cookieValueName = "cookieValue";
                    cookieValueValue = cookieValue;
                }
                cookies.put(cookieValueName, cookieValueValue);
                String expires = null;
                String path = null;
                String domain = null;
                boolean secure = false;
                for (int j = 1; j < fields.length; j++) {
                    if ("secure".equalsIgnoreCase(fields[j])) {
                        secure = true;
                    } else if (fields[j].indexOf('=') > 0) {
                        String[] f = fields[j].split("=");
                        if ("expires".equalsIgnoreCase(f[0])) {
                            expires = f[1];
                        } else if ("domain".equalsIgnoreCase(f[0])) {
                            domain = f[1];
                        } else if ("path".equalsIgnoreCase(f[0])) {
                            path = f[1];
                        }
                    }
                }
                System.out.print("Saved Cookie: " + cookieValue + "; expires=" + expires + "; path=" + path + "; domain=" + domain + "; " + secure);
            }
        }
    }

    public String returnCookies() {
        String returnString = "";
        Iterator<Map.Entry<String, String>> it = cookies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            returnString = returnString + "#" + entry.getKey() + "=" + entry.getValue() + "#";
        }
        returnString = returnString.replaceAll("##", ";");
        returnString = returnString.replaceAll("#", "");
        System.out.print("Loaded Cookie: " + returnString);
        return returnString;
    }

    public void clearCookies() {
        cookies.clear();
    }

    private HashMap<String, String> cookies;
}
