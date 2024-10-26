package com.dm.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class URLConnectionDemo {

    public static void main(String[] args) throws IOException {
        URL url = new URL("http://www.hust.edu.cn");
        URLConnection uc = url.openConnection();
        uc.connect();
        Map m = uc.getHeaderFields();
        Iterator i = m.entrySet().iterator();
        while (i.hasNext()) System.out.println(i.next());
        System.out.println("Input allowed = " + uc.getDoInput());
        System.out.println("Output allowed = " + uc.getDoOutput());
    }
}
