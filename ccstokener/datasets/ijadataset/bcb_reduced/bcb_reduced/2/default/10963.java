import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class WebUrl {

    private String url, ip, port;

    private boolean useProxy;

    WebUrl(String url) {
        this.url = url;
    }

    WebUrl(String url, String ip, String port) {
        this.url = url;
        this.ip = ip;
        this.port = port;
        useProxy = true;
    }

    public ArrayList<String> getData() {
        ArrayList<String> data = new ArrayList<String>();
        String line = null;
        URL address = null;
        try {
            address = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection urlconn = null;
        if (useProxy) {
            SocketAddress addr = new InetSocketAddress(ip, Integer.parseInt(port));
            java.net.Proxy httpProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, addr);
            try {
                urlconn = address.openConnection(httpProxy);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                urlconn = address.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            urlconn.connect();
        } catch (IOException e) {
            return null;
        }
        BufferedReader buffreader = null;
        try {
            buffreader = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            line = buffreader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (line != null) {
            data.add(line);
            try {
                line = buffreader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }
}
