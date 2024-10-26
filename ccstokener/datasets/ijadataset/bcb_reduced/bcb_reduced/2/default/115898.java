import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

class ImageHarvester {

    protected HttpURLConnection conn;

    FileWriter log404;

    PrintWriter file;

    public static void main(String[] args) {
        ImageHarvester session = new ImageHarvester();
        Properties p = new Properties();
        int start = 0, end = 0;
        try {
            p.load(new FileInputStream("config.properties"));
            start = Integer.parseInt(p.getProperty("start"));
            end = Integer.parseInt(p.getProperty("end"));
        } catch (Exception e) {
            System.err.println("Can't Initialize");
            System.exit(3);
        }
        try {
            session.log404 = new FileWriter("log404.txt", true);
            session.file = new PrintWriter(new FileWriter("result.txt", true));
        } catch (IOException e) {
            System.err.println("Cannot open log file");
            System.exit(4);
        }
        while (start <= end) {
            String filename = "" + start;
            start += 6;
            session.makeConn(filename);
            session.retrieveAndSave(filename);
            session.freeConn();
        }
        try {
            session.log404.close();
            session.file.close();
        } catch (IOException e) {
            System.err.println("Can not close logfile");
            System.exit(5);
        }
    }

    private void makeConn(String filename) {
        String basename = "http://community.funstars.com/cgi-bin/imageView.cgi?direct=Wellweb/Western&img=";
        String urlname = basename + filename;
        URL url = null;
        try {
            url = new URL(urlname);
        } catch (MalformedURLException e) {
            System.err.println("URL Format Error!");
            System.exit(1);
        }
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.err.println("Error IO");
            System.exit(2);
        }
    }

    private void freeConn() {
        conn.disconnect();
    }

    private void getConnInfo() {
        try {
            conn.connect();
            System.out.println("Response code/message: " + conn.getResponseCode() + " / " + conn.getResponseMessage());
            System.out.println("Content Encoding: " + conn.getContentEncoding());
            System.out.println("Content Length: " + conn.getContentLength());
            System.out.println("Content Type: " + conn.getContentType());
            int i = 0;
            while (conn.getHeaderFieldKey(i) != null) {
                System.out.println(conn.getHeaderFieldKey(i) + ": " + conn.getHeaderField(i));
                i++;
            }
        } catch (Exception e) {
            System.err.println("Error");
            System.exit(2);
        }
    }

    private void retrieveAndSave(String filename) {
        try {
            if (conn.getResponseCode() == 404) {
                System.out.println("404: " + filename);
                log404.write(filename + "\n");
                return;
            } else if (conn.getResponseCode() != 200) {
                System.out.println("HTTP code is: " + conn.getResponseCode());
                return;
            } else {
                InputStream stream = conn.getInputStream();
                StringBuffer sb = new StringBuffer();
                try {
                    int ch = stream.read();
                    while (ch != -1) {
                        sb.append((char) ch);
                        ch = stream.read();
                    }
                } catch (Exception e) {
                    System.err.println("IO error");
                    System.exit(1);
                }
                Pattern pattern = Pattern.compile("<a .*?&image=(.*?[.]jpg)&.*?>", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(sb);
                while (matcher.find()) {
                    String group = matcher.group(1);
                    System.out.println(group);
                    file.println("http://community.funstars.com/gallery/Wellweb/Western/" + group);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }
}
