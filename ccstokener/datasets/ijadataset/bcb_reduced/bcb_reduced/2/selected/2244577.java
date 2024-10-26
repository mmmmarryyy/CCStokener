package jvc.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class NetUtils {

    public static String getMacAddressIP(String remotePcIP) {
        String str = "";
        String macAddress = "";
        try {
            Process pp = Runtime.getRuntime().exec("nbtstat -A " + remotePcIP);
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (int i = 1; i < 100; i++) {
                str = input.readLine();
                if (str != null) {
                    if (str.indexOf("MAC Address") > 1) {
                        macAddress = str.substring(str.indexOf("MAC Address") + 14, str.length());
                        break;
                    }
                }
            }
            input.close();
        } catch (IOException ex) {
        }
        return macAddress;
    }

    public static String getMacAddressName(String remotePcIP) {
        String str = "";
        String macAddress = "";
        try {
            Process pp = Runtime.getRuntime().exec("nbtstat -a " + remotePcIP);
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (int i = 1; i < 100; i++) {
                str = input.readLine();
                if (str != null) {
                    if (str.indexOf("MAC Address") > 1) {
                        macAddress = str.substring(str.indexOf("MAC Address") + 14, str.length());
                        break;
                    }
                }
            }
            input.close();
        } catch (IOException ex) {
        }
        return macAddress;
    }

    public static boolean isUrl(String url) {
        try {
            java.net.URL l_url = new java.net.URL(url);
            java.net.HttpURLConnection l_connection = (java.net.HttpURLConnection) l_url.openConnection();
            if (System.getProperty("java.runtime.version").startsWith("1.4")) System.setProperty("sun.net.client.defaultReadTimeout", "2000"); else l_connection.setConnectTimeout(2000);
            l_connection.connect();
            l_connection.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getHtmlSource(String url) throws Exception {
        String sCurrentLine;
        String sTotalString;
        sCurrentLine = "";
        sTotalString = "";
        java.io.InputStream l_urlStream;
        java.net.URL l_url = new java.net.URL(url);
        java.net.HttpURLConnection l_connection = (java.net.HttpURLConnection) l_url.openConnection();
        if (System.getProperty("java.runtime.version").startsWith("1.4")) {
            System.setProperty("sun.net.client.defaultConnectTimeout", "3000");
            System.setProperty("sun.net.client.defaultReadTimeout", "3000");
        } else {
            l_connection.setConnectTimeout(3000);
            l_connection.setReadTimeout(3000);
        }
        l_connection.connect();
        l_urlStream = l_connection.getInputStream();
        java.io.BufferedReader l_reader = new java.io.BufferedReader(new java.io.InputStreamReader(l_urlStream));
        while ((sCurrentLine = l_reader.readLine()) != null) {
            sTotalString += sCurrentLine;
        }
        return sTotalString;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(NetUtils.getHtmlSource("http://59.63.158.212:8081/friends/home.page"));
    }
}
