package gate.solr;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import org.apache.log4j.Logger;
import org.apache.nutch.crawl.Crawl;
import org.apache.hadoop.util.ToolRunner;
import org.apache.nutch.util.NutchConfiguration;

/**
 * This class is a thin layer over the nutch code that allows adding, deleting
 * and updating files in solr.
 */
public class SolrIndexer {

    /** Logger. */
    static Logger lgr = Logger.getLogger(SolrIndexer.class);

    /** URL of the server running solr index */
    String solrUrl;

    /** Construction. */
    public SolrIndexer(String solrUrl) {
        this.solrUrl = solrUrl;
        System.out.println(getClass().getClassLoader().getResource("nutch-site.xml").toString());
        System.out.println(getClass().getClassLoader().getResource("plugins").toString());
        System.out.println(getClass().getClassLoader().getResource("urlfilter-default.txt").toString());
    }

    /**
   * Removes the file from solr index
   * 
   * @param fileUrls
   *          A list of URLs of the files to be deleted
   * @param coreId
   *          core that holds the given file
   * @return true if file is deleted successfully, false otherwise
   */
    public boolean delete(List<String> fileUrls, int coreId) {
        StringBuilder query = new StringBuilder();
        if (fileUrls.isEmpty()) {
            query.append("*:*");
        } else {
            for (int i = 0; i < fileUrls.size(); i++) {
                if (i != 0) query.append(" OR ");
                query.append("url:\"" + fileUrls.get(i) + "\"");
            }
        }
        return deleteByQuery(query.toString(), coreId);
    }

    /**
   * Removes documents that match the given query
   * 
   * @param query
   *          search query to be used for obtaining a list of documents
   * @param coreId
   *          core that holds the given file
   * @return true if file is deleted successfully, false otherwise
   */
    public boolean deleteByQuery(String query, int coreId) {
        try {
            URL url = new URL(solrUrl + "/core" + coreId + "/update");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "text/xml");
            conn.setRequestProperty("charset", "utf-8");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            System.out.println("******************" + query);
            wr.write("<delete><query>" + query + "</query></delete>");
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
            }
            wr.close();
            rd.close();
            conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "text/xml");
            conn.setRequestProperty("charset", "utf-8");
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("<commit/>");
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
   * Removes documents that match the given query
   * 
   * @param query
   *          search query to be used for obtaining a list of documents
   * @param coreId
   *          core that holds the given file
   * @return true if file is deleted successfully, false otherwise
   */
    public boolean optimize(int coreId) {
        try {
            URL url = new URL(solrUrl + "/core" + coreId + "/update");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "text/xml");
            conn.setRequestProperty("charset", "utf-8");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            System.out.println("******************optimizing");
            wr.write("<optimize/>");
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
   * Removes the file from solr index and then adds it using nutch
   * 
   * @param fileUrls
   *          A list of URLs of the files to be updated
   * @param coreId
   *          core that holds the given file
   * @return true if file is updated successfully, false otherwise
   */
    public boolean update(List<String> fileUrls, int coreId) {
        if (!delete(fileUrls, coreId)) {
            return false;
        }
        index(fileUrls, coreId);
        return true;
    }

    /**
   * Adds a new file to solrIndexer
   * 
   * @param fileUrls
   *          a list of URLs of the files to be indexed
   * @param coreId
   *          core in which this file should be indexed
   * @return true if file is added successfully
   */
    public boolean index(final List<String> fileUrls, final int coreId) {
        System.out.println("Indexing started:");
        File indexDir = null;
        File urls = null;
        String depth = "1";
        String threads = "1";
        try {
            indexDir = new File(System.getProperty("java.io.tmpdir"), "crawl-" + getDate());
            indexDir.mkdirs();
            indexDir.deleteOnExit();
            urls = new File(indexDir, "urls");
            BufferedWriter writer = new BufferedWriter(new FileWriter(urls));
            StringBuilder filteringRegexs = new StringBuilder();
            for (String aFileUrl : fileUrls) {
                writer.write(aFileUrl);
                if (depth.equals("1") && new File(new URL(aFileUrl).toURI()).isDirectory()) {
                    if (!aFileUrl.endsWith("/")) {
                        aFileUrl += "/";
                    }
                    aFileUrl = aFileUrl.substring(8);
                    filteringRegexs.append("\n+").append(aFileUrl).append(".*\\.(html|htm|HTML|HTM|text|txt|doc|pdf|tex)$");
                    filteringRegexs.append("\n+").append(aFileUrl).append(".*/$");
                    depth = "70";
                    threads = "30";
                }
                filteringRegexs.append("\n+").append(aFileUrl).append("$");
                writer.newLine();
            }
            writer.close();
            File crawlFilter = new File(indexDir, "crawl-urlfilter.txt");
            writer = new BufferedWriter(new FileWriter(crawlFilter));
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("urlfilter-default.txt")));
            String line = br.readLine();
            while (line != null) {
                writer.write(line);
                writer.newLine();
                line = br.readLine();
            }
            writer.write(filteringRegexs.toString());
            writer.newLine();
            writer.write("-.");
            writer.close();
            addToClassPath(indexDir);
        } catch (IOException e1) {
            e1.printStackTrace();
            return false;
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
            return false;
        }
        String[] nutchParams = new String[] { urls.getAbsolutePath(), "-dir", indexDir.getAbsolutePath(), "-depth", depth, "-threads", threads };
        try {
            NutchCrawler.main(nutchParams);
            File[] segmentDirectories = new File(indexDir, "segments").listFiles();
            String[] solrParams = new String[segmentDirectories.length + 3];
            solrParams[0] = solrUrl + "/core" + coreId;
            solrParams[1] = new File(indexDir, "crawldb").getAbsolutePath();
            solrParams[2] = new File(indexDir, "linkdb").getAbsolutePath();
            for (int i = 3; i < solrParams.length; i++) {
                solrParams[i] = segmentDirectories[i - 3].getAbsolutePath();
            }
            ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.indexer.solr.SolrIndexer(), solrParams);
            if (fileUrls != null && fileUrls.size() > 0) {
                String query = new File(new URL(fileUrls.get(0)).toURI()).getAbsolutePath();
                deleteByQuery(query, coreId);
                optimize(coreId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /** method to modify classpath at runtime */
    private void addToClassPath(File file) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { file.toURL() });
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    /**
   * returns date object in simple format
   * 
   * @return
   */
    private static String getDate() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("usage: SolrIndexer <index|delete|update|delByQuery>" + " <solrUrl> <coreId> <fileUrl/query> [<fileUrl2> .... <fileUrlN>]");
            System.exit(1);
        }
        SolrIndexer solr = new SolrIndexer(args[1]);
        boolean success = false;
        int coreId = Integer.parseInt(args[2]);
        List<String> urls = new ArrayList<String>();
        for (int i = 3; i < args.length; i++) {
            urls.add(args[i]);
        }
        if (args[0].equals("index")) {
            if (urls.isEmpty()) {
                success = false;
                System.out.println("************Atleast one file url must be provided");
            } else {
                success = solr.index(urls, coreId);
            }
        } else if (args[0].equals("delete")) {
            success = solr.delete(urls, coreId);
        } else if (args[0].equals("update")) {
            if (urls.isEmpty()) {
                success = false;
                System.out.println("Atleast one file url must be provided");
            } else {
                success = solr.update(urls, coreId);
            }
        } else if (args[0].equals("delByQuery")) {
            success = solr.deleteByQuery(urls.get(0), coreId);
        }
        System.out.println("success : " + success);
        if (!success) System.exit(1); else System.exit(0);
    }
}
