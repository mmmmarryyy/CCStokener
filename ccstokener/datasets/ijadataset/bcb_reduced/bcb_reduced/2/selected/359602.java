package de.radis.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class HttpRequestTest {

    public static void main(String[] args) throws IOException {
        URL url = new URL("http://radis.sf.net/update.txt");
        InputStreamReader isr = new InputStreamReader(url.openStream());
        BufferedReader br = new BufferedReader(isr);
        while (br.ready()) {
            System.out.println(br.readLine());
        }
    }
}
