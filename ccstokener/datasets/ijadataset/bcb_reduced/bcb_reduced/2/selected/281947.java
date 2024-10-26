package Honeysift;

import websphinx.Crawler;
import websphinx.CrawlEvent;
import websphinx.DownloadParameters;
import websphinx.EventLog;
import websphinx.Link;
import websphinx.Page;
import websphinx.searchengine.AltaVista;
import websphinx.searchengine.Excite;
import websphinx.searchengine.Google;
import websphinx.searchengine.HotBot;
import websphinx.searchengine.MetaCrawler;
import websphinx.searchengine.NewsBot;
import websphinx.searchengine.NewsIndex;
import websphinx.searchengine.Search;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;

/**
 * Web crawler based on websphynx
 * @author Damien
 *
 */
public class FastCrawler {

    private static String seedfile = "seeds.txt";

    private static int depth = 1;

    private static final String crawlfile = "crawl.txt";

    private static MyCrawler crawler;

    private static EventLog logger;

    private static DownloadParameters dp;

    private static String links;

    public static void FastCrawlerFile(String filename, int dep) throws IOException {
        if (dep > 0) depth = dep;
        if (filename.length() > 1) seedfile = filename;
        links = loadLinks(seedfile);
        run(links);
        print(true);
    }

    public static void FastCrawlerURL(int dep, String URL) throws IOException {
        if (dep > 0) depth = dep;
        run(URL);
        print(true);
    }

    private static String loadLinks(String fname) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(fname)));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            System.out.println("Reading from: " + fname);
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            reader.close();
            System.out.println(buffer.toString());
            return buffer.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void run(String links) {
        crawler = new MyCrawler();
        logger = new EventLog();
        dp = DownloadParameters.DEFAULT;
        dp = dp.changeInteractive(false);
        crawler.addCrawlListener(logger);
        crawler.setDomain(Crawler.SUBTREE);
        crawler.setDownloadParameters(dp);
        crawler.setMaxDepth(depth);
        try {
            crawler.setRootHrefs(links);
        } catch (java.net.MalformedURLException e) {
            e.printStackTrace();
        }
        crawler.run();
    }

    private static void print(boolean write) throws IOException {
        Vector<Page> pages = crawler.getPages();
        System.out.println("Crawl Links: " + pages.size());
        for (int a = 0; a < pages.size(); a++) {
            System.out.println(pages.get(a).getURL().toString());
            CreateDirectory CrD = new CreateDirectory();
            CrD.CreateDir("Captures");
            String currentDir = new File("").getAbsolutePath();
            Download UD = new Download();
            UD.DL(pages.get(a).getURL().toString(), currentDir + "//Captures");
        }
        if (!write) return;
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(new File(crawlfile)));
            for (int a = 0; a < pages.size(); a++) {
                writer.println(pages.get(a).getURL().toString());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MyCrawler extends Crawler {

    private Vector<Page> pages;

    private Vector<String> types;

    MyCrawler() {
        super();
        pages = new Vector<Page>();
        types = new Vector<String>();
        types.add("htm");
        types.add("html");
        types.add("php");
        types.add("exe");
    }

    public void visit(Page page) {
        boolean accept = false;
        for (int a = 0; a < types.size(); a++) {
            if (page.getContentType().contains(types.get(a))) {
                accept = true;
                break;
            }
        }
        if (accept) pages.add(page);
    }

    public Vector<Page> getPages() {
        return pages;
    }
}

class Download {

    /**
 	 * @param args
 	 */
    public static void DL(String fAddress, String destinationDir) {
        int slashIndex = fAddress.lastIndexOf('/');
        int periodIndex = fAddress.lastIndexOf('.');
        String fileName = fAddress.substring(slashIndex + 1);
        try {
            URL url = new URL(fAddress);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            InputStream in = null;
            in = url.openStream();
            String content = pipe(in, "utf-8", fileName, destinationDir);
        } catch (Exception e) {
            System.out.println(" Page could not be downloaded, file was deleted go to next URL");
        }
    }

    static String pipe(InputStream in, String charset, String localFileName, String destinationDir) throws IOException {
        StringBuffer s = new StringBuffer();
        if (charset == null || "".equals(charset)) {
            charset = "utf-8";
        }
        String rLine = null;
        BufferedReader bReader = new BufferedReader(new InputStreamReader(in, charset));
        PrintWriter pw = null;
        FileOutputStream fo = new FileOutputStream(destinationDir + "//" + localFileName);
        OutputStreamWriter writer = new OutputStreamWriter(fo, "utf-8");
        pw = new PrintWriter(writer);
        while ((rLine = bReader.readLine()) != null) {
            String tmp_rLine = rLine;
            int str_len = tmp_rLine.length();
            if (str_len > 0) {
                s.append(tmp_rLine);
                pw.println(tmp_rLine);
                pw.flush();
            }
            tmp_rLine = null;
        }
        in.close();
        pw.close();
        return s.toString();
    }
}

class CreateDirectory {

    public static void CreateDir(String DirName) {
        String currentDir = new File(".").getAbsolutePath();
        try {
            boolean success = (new File(DirName)).mkdir();
            if (success) {
                System.out.println("Directory: " + currentDir + "/" + DirName + " created");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

class Delete {

    public static void DelFile(String fileName) {
        File f = new File(fileName);
        if (!f.exists()) throw new IllegalArgumentException("Delete: no such file or directory: " + fileName);
        if (!f.canWrite()) throw new IllegalArgumentException("Delete: write protected: " + fileName);
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0) throw new IllegalArgumentException("Delete: directory not empty: " + fileName);
        }
        boolean success = f.delete();
        if (!success) throw new IllegalArgumentException("Delete: deletion failed");
    }
}
