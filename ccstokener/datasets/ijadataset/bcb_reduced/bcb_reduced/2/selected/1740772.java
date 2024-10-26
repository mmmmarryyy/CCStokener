package com.netx.ut.lib.servlet;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.List;

public class ReadServlet {

    public static void main(String[] args) throws Exception {
        ReadServlet rs = new ReadServlet();
        rs.readSocket();
    }

    public void readSocket() throws Exception {
        Socket s = new Socket("localhost", 8080);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        writer.println("GET /cubigraf3/files/start-menu.swf HTTP/1.1");
        writer.println("Host: localhost:8080");
        writer.println("Connection: Close");
        writer.println();
        int bytesRead = -1;
        boolean messageBody = false;
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            if (messageBody) {
                bytesRead += line.length();
                bytesRead += 1;
            }
            if (line.equals("")) {
                messageBody = true;
            }
            line = reader.readLine();
        }
        writer.close();
        reader.close();
        System.out.println("[bytes read: " + bytesRead + "]");
    }

    public void readSocketForOptions() throws Exception {
        Socket s = new Socket("localhost", 8080);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        writer.println("OPTIONS /cubigraf3/login HTTP/1.1");
        writer.println("Host: localhost:8080");
        writer.println("Connection: Close");
        writer.println();
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        writer.close();
        reader.close();
    }

    public void readURL() throws Exception {
        URL url = new URL("http://www.google.com");
        URLConnection c = url.openConnection();
        Map<String, List<String>> headers = c.getHeaderFields();
        for (String s : headers.keySet()) {
            System.out.println(s + ": " + headers.get(s));
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        reader.close();
    }
}
