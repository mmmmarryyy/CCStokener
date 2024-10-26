package sfljtse.tsf.coreLayer.utils;

import java.net.URL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.*;

/**
 * @title		: URLRip       
 * @description	:  
 * @date		: 10-mag-2006   
 * @author		: Alberto Sfolcini  <a.sfolcini@gmail.com>
 */
public class URLrip {

    private static boolean debug = false;

    private InputStream getDocumentAsInputStream(URL url) throws IOException {
        InputStream in = url.openStream();
        return in;
    }

    private InputStream getDocumentAsInputStream(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsInputStream(u);
    }

    private String getDocumentAsString(URL url) throws IOException {
        StringBuffer result = new StringBuffer();
        InputStream in = url.openStream();
        int c;
        while ((c = in.read()) != -1) result.append((char) c);
        return result.toString();
    }

    private String getDocumentAsString(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsString(u);
    }

    /**
   * URLripper
   * @param urlstr
   * @param outputFile
   * @return
   */
    public boolean URLripper(String urlstr, String outputFile) {
        String doc;
        try {
            doc = this.getDocumentAsString(urlstr);
            if (debug) System.out.println(doc);
        } catch (Exception e) {
            if (debug) System.err.println("Fatal error while fetching datas...");
            return false;
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            out.write(doc);
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
   * Main method, for test purpose only
   * @param args
   */
    public static void main(String[] args) {
        URLrip rip = new URLrip();
        if (rip.URLripper("http://www.sflweb.org/", "C:\\sflweb.htm")) System.out.println("The file has been correctly downloaded."); else System.out.println("Cannot download the file!");
    }
}
