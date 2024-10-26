package com.generatescape.htmlparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Kent Gibson
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class HTMLParserTest {

    public static void main(String[] args) {
        URL url;
        try {
            url = new URL("http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2");
            URLConnection urlConnection = url.openConnection();
            BufferedReader htmlPage = new BufferedReader(new InputStreamReader(url.openStream()));
            HTMLParser htmlparser = new HTMLParser(htmlPage);
            LineNumberReader reader = new LineNumberReader(htmlparser.getReader());
            for (String l = reader.readLine(); l != null; l = reader.readLine()) {
                System.out.println(l);
            }
            htmlPage.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
