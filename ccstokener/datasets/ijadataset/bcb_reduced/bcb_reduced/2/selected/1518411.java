package fr.plaisance.ip.wimi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import fr.plaisance.ip.IpFinder;

public class WhatIsMyIpFinder implements IpFinder {

    private ResourceBundle bundle;

    public WhatIsMyIpFinder() {
        this.bundle = ResourceBundle.getBundle("fr.plaisance.ip.wimi.wimi");
    }

    @Override
    public String findIp() throws IOException {
        URL url = new URL(bundle.getString("ip.url"));
        Proxy proxy = null;
        String proxyName = bundle.getString("proxy.name");
        if (proxyName == null || proxyName.trim().length() == 0) {
            proxy = Proxy.NO_PROXY;
        } else {
            int proxyPort = Integer.parseInt(bundle.getString("proxy.port"));
            SocketAddress socket = new InetSocketAddress(proxyName, proxyPort);
            proxy = new Proxy(Proxy.Type.HTTP, socket);
        }
        URLConnection connection = url.openConnection(proxy);
        connection.connect();
        InputStream stream = connection.getInputStream();
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        String ip = buffer.readLine();
        buffer.close();
        reader.close();
        stream.close();
        buffer = null;
        reader = null;
        stream = null;
        connection = null;
        url = null;
        return ip;
    }
}
