package randres.retrieve;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calibsite {

    private String baseUrl;

    private String targetFolder;

    private static String PATTERN = "href=[\"]?([^\" >]+)";

    private List<String> bookUrls;

    private FileWriter fw;

    public Calibsite(String baseUrl, String targetFolder) {
        this.baseUrl = baseUrl;
        this.targetFolder = targetFolder;
        bookUrls = new ArrayList<String>();
        try {
            fw = new FileWriter(targetFolder + File.separator + "list");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void analyseSite() throws Exception {
        analyseSiteRec(baseUrl, 0);
    }

    public void analyseSiteRec(String folderUrl, int level) throws Exception {
        String result = getContents(folderUrl);
        Pattern p = Pattern.compile(PATTERN);
        Matcher m = p.matcher(result);
        while (m.find()) {
            String link = m.group(1);
            if (link.endsWith("/") && link.length() > 1 && !link.startsWith("_") && link.indexOf("/") == (link.length() - 1) && (level != 0 || link.compareTo("Winston%20Groon") > 0)) {
                System.out.println("Entering: " + folderUrl + link);
                analyseSiteRec(folderUrl + link, level++);
            } else if (link.toLowerCase().endsWith(".mobi") || link.toLowerCase().endsWith(".prc")) {
                File f = new File(targetFolder + File.separator + link);
                if (!f.exists() || f.getTotalSpace() < 200000) {
                    ;
                    fw.append(download(folderUrl + link, targetFolder, link));
                    fw.append("\n");
                    fw.flush();
                }
            }
        }
    }

    public static String getContents(String urlStr) throws Exception {
        String contents = "";
        URL url = new URL(urlStr);
        URLConnection openConnection = url.openConnection();
        final char[] buffer = new char[1024 * 1024];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(openConnection.getInputStream(), "UTF-8");
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.append(buffer, 0, read);
            }
        } while (read >= 0);
        contents = out.toString();
        return contents;
    }

    public static String download(String urlStr, String folder, String title) {
        String result = "";
        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL(urlStr);
            url.openConnection();
            InputStream reader = url.openStream();
            FileOutputStream writer = new FileOutputStream(folder + File.separator + title);
            byte[] buffer = new byte[1024 * 1024];
            int totalBytesRead = 0;
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
            }
            long endTime = System.currentTimeMillis();
            result = "Done. " + (new Integer(totalBytesRead).toString()) + " bytes read (" + (new Long(endTime - startTime).toString()) + " millseconds).\n";
            writer.close();
            reader.close();
        } catch (Exception e) {
            result = "Can not download. " + folder + File.separator + title + ":\n" + e.getMessage();
        }
        return result;
    }

    public List<String> getBookUrls() {
        return bookUrls;
    }

    public static void main(String[] args) {
        String baseUrlStr = "http://ozn.es/ebook/";
        String folder = "/home/randres/Desktop/ozn";
        Calibsite site = new Calibsite(baseUrlStr, folder);
        try {
            site.analyseSite();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
