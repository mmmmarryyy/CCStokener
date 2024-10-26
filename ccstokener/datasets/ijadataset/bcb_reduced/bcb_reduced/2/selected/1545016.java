package cn.edu.wuse.musicxml.demo;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public class DisplayMsg {

    public static void main(String[] args) {
        try {
            String s = "http://localhost:8080/shixi/download/2.mxl";
            System.out.println(s);
            URL url = new URL(s);
            InputStream is = url.openStream();
            byte[] b = new byte[1024];
            while (is.read(b) != -1) {
                System.out.println(new String(b));
                break;
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
