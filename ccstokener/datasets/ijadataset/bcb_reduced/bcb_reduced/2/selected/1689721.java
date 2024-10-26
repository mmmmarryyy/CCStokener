package de.fmf.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;

public class UrlReader implements Runnable {

    private String urlString;

    private File destFile;

    public static void main(String[] args) {
        File baseDownloadFolder = new File("c:\\Dokumente und Einstellungen\\fma\\Desktop\\download");
        String toDownload = "http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie1.jpg,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie2a.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie2b.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie2c.jpg,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie2.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie3a.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie3.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie4.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie5a.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie5b.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie5.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie6.jpg,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie7.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie8.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie9.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie10a.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie10.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie11.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie12a.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie12.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie13.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie14.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie17.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie18.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie19.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie20.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie21.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie15.JPG,http://www.db-thueringen.de/servlets/DerivateServlet/Derivate-3949/Folie16.JPG";
        String[] tod = toDownload.split(",");
        for (int i = 0; i < tod.length; i++) {
            String url = tod[i];
            File downloaddest = new File(baseDownloadFolder, url.substring(url.lastIndexOf('/')));
            new UrlReader(false, url, downloaddest);
        }
    }

    public UrlReader(boolean proxy, String url, File destFile) {
        if (proxy = true) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", "140.5.1.230");
            System.getProperties().put("proxyPort", "3128");
        }
        this.urlString = url;
        this.destFile = destFile;
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        OutputStream out;
        try {
            out = new FileOutputStream(destFile);
            URL url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")));
            int inputLine;
            while ((inputLine = in.read()) != -1) {
                out.write(inputLine);
            }
            out.close();
            System.out.println("FINISHED: " + destFile.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
