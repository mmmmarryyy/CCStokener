package net.sf.topspeed.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class TestURL {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            URL url = new URL("http://grid.hust.edu.cn/index.php");
            URLConnection conn = url.openConnection();
            System.out.println("Path : " + url.getPath());
            System.out.println("File : " + url.getFile());
            System.out.println("Host : " + url.getHost());
            System.out.println("Port : " + url.getPort());
            System.out.println("Protocal : " + url.getProtocol());
            conn.connect();
            System.out.println("Length : " + conn.getContentLength());
            System.out.println(conn.getContent());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
