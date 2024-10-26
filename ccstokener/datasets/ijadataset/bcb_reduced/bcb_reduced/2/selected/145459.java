package javamath.hasse.util;

import java.awt.*;
import java.io.*;
import java.net.*;

public class FetchURL {

    /**
    * Get the contents of a URL and return it as a string.
    */
    public static String fetch(String address) throws MalformedURLException, IOException {
        URL url = new URL(address);
        URLConnection connection = url.openConnection();
        DataInputStream in = new DataInputStream(connection.getInputStream());
        String line = in.readLine();
        String all = "";
        while (line != null) {
            all += line;
            line = in.readLine();
        }
        return all;
    }

    public static void main(String[] args) throws MalformedURLException, IOException {
        System.out.println(fetch(args[0]));
    }
}
