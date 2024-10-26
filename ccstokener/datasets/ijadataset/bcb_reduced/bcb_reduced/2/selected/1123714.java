package org.codegallery.javagal.http;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class HttpGal {

    public static void main(String[] args) {
        HttpGal gal = new HttpGal();
        try {
            gal.pingAddress();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void httpGet() throws Exception {
        Properties systemSettings = System.getProperties();
        systemSettings.put("proxySet", "true");
        systemSettings.put("http.proxyHost", "192.168.80.3");
        systemSettings.put("http.proxyPort", "8080");
        URL url = new URL("http://www.baidu.com");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setRequestMethod("GET");
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        String encodedUserPwd = encoder.encode("ncs\\wenwei:Moto*1234".getBytes());
        httpCon.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
        httpCon.setRequestMethod("HEAD");
        long date = httpCon.getDate();
        if (date == 0) {
            System.out.println("No date information.");
        } else {
            System.out.println("Date: " + new Date(date));
        }
        System.out.println(httpCon.getResponseCode() + " : " + httpCon.getResponseMessage());
    }

    public void selectProxy() throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        List l = ProxySelector.getDefault().select(new URI("http://www.baidu.com"));
        for (Iterator iter = l.iterator(); iter.hasNext(); ) {
            Proxy proxy = (Proxy) iter.next();
            System.out.println("proxy hostname : " + proxy.type());
            InetSocketAddress addr = (InetSocketAddress) proxy.address();
            if (addr == null) {
                System.out.println("No Proxy");
            } else {
                System.out.println("proxy hostname : " + addr.getHostName());
                System.out.println("proxy port : " + addr.getPort());
            }
        }
    }

    public void pingAddress() throws Exception {
        InetAddress address = InetAddress.getByName("www.facebook.com");
        System.out.println("Name: " + address.getHostName());
        System.out.println("Addr: " + address.getHostAddress());
        System.out.println("Reach: " + address.isReachable(3000));
    }
}
