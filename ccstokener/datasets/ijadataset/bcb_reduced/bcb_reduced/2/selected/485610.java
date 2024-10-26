package com.onehao.network;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;

public class UrlConnection3 {

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://www.sohu.com");
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is, Charset.forName("GB18030"));
        FileOutputStream fos = new FileOutputStream("gen/sohu2.html");
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        char[] b = new char[2048];
        int temp;
        while (-1 != (temp = isr.read(b, 0, b.length))) {
            osw.write(b);
        }
        osw.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = null;
    }
}
