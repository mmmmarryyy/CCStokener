package com.planes.automirror;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Steve Tousignant (planestraveler)
 */
public class Statistique implements Runnable {

    private static final Logger LogPrinter = Logger.getLogger("autosync-mirror.Statistique");

    private static final int BUFFER_SIZE = 4096;

    private static final int MAX_SIZE = 15000;

    private URL ToTest;

    private BigDecimal Speed;

    private boolean TestDone;

    private String Message;

    private long SizeOfDownload;

    private Node Root;

    private long LastTimeUpdated = 0;

    /** Creates a new instance of Statistique 
     * @param urltoTest the URL we need to test
     */
    public Statistique(URL urltoTest) {
        ToTest = urltoTest;
        Message = null;
        SizeOfDownload = 0L;
    }

    private long walkinURL(URL testurl, Node baseNode) {
        long ret = 0;
        InputStream list = null;
        StringBuilder toAnalyse = new StringBuilder(BUFFER_SIZE * 2);
        try {
            long resolutionstart = System.currentTimeMillis();
            URLConnection conn = testurl.openConnection();
            long resolutionend = System.currentTimeMillis();
            ret += resolutionend - resolutionstart;
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) conn;
                if (http.getContentLength() > MAX_SIZE) {
                    Message = "This is bad news cause it's may not be a mirror";
                } else {
                    String charset = http.getContentEncoding();
                    if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        list = http.getInputStream();
                        long start = System.currentTimeMillis();
                        byte buffer[] = new byte[BUFFER_SIZE];
                        int read = list.read(buffer);
                        while (read >= 0) {
                            SizeOfDownload += read;
                            if (charset != null) {
                                toAnalyse.append(new String(buffer, 0, read, charset));
                            } else {
                                toAnalyse.append(new String(buffer, 0, read));
                            }
                            buildHttpNodes(toAnalyse, baseNode, testurl);
                            read = list.read(buffer);
                        }
                        long end = System.currentTimeMillis();
                        ret += end - start;
                    } else {
                        Message = http.getResponseMessage();
                    }
                }
                http.disconnect();
            }
        } catch (IOException e) {
            Message = e.getMessage();
        } finally {
            if (list != null) {
                try {
                    list.close();
                } catch (IOException e) {
                }
            }
        }
        return ret;
    }

    private static final String HREF = "href=\"";

    private Pattern PathDetector = Pattern.compile(".*/$");

    private void buildHttpNodes(StringBuilder toAnalyse, Statistique.Node node, URL pathURL) {
        int posHref = toAnalyse.indexOf(HREF);
        if (posHref < 0) {
            posHref = toAnalyse.indexOf(HREF.toUpperCase());
        }
        String host = ToTest.getHost();
        String path = ToTest.getPath();
        String proto = ToTest.getProtocol();
        while (posHref > 0) {
            int posEndQuote = toAnalyse.indexOf("\"", posHref + HREF.length() + 2);
            if (posEndQuote <= posHref) {
                break;
            }
            String possibleNode = toAnalyse.substring(posHref + HREF.length(), posEndQuote);
            if (PathDetector.matcher(possibleNode).matches()) {
                if (possibleNode.startsWith(proto)) {
                    try {
                        URL childUrl = new URL(possibleNode);
                        if (childUrl.getHost().equalsIgnoreCase(host) && childUrl.getPath().contains(path)) {
                            Node childNode = new Node(childUrl.getPath(), childUrl);
                            node.addChild(childNode);
                        }
                    } catch (MalformedURLException ex) {
                    }
                } else {
                    try {
                        URL childUrl = new URL(pathURL.toString() + "/" + possibleNode);
                        Node childNode = new Node(possibleNode, childUrl);
                        node.addChild(childNode);
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    node.addFile(possibleNode, new FileMetaData(new URL(pathURL.toString() + possibleNode)));
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                }
            }
            toAnalyse.delete(0, posHref + HREF.length() + 2);
            posHref = toAnalyse.indexOf(HREF);
            if (posHref < 0) {
                posHref = toAnalyse.indexOf(HREF.toUpperCase());
            }
        }
    }

    private void buildTree(PrintWriter out, Statistique.Node node, String prefix) throws IOException {
        for (Map.Entry<String, FileMetaData> file : node.getFileList().entrySet()) {
            out.println(prefix + file.getKey());
        }
        for (Node child : node.getChilds()) {
            out.println(prefix + child);
            buildTree(out, child, " " + prefix);
        }
    }

    public void run() {
        TestDone = false;
        Root = new Node("root", ToTest);
        long timeOfTest = walkinURL(ToTest, Root);
        LastTimeUpdated = System.currentTimeMillis();
        LogPrinter.log(Level.FINER, "Tested " + ToTest.toString() + " and finished at " + new Date(LastTimeUpdated));
    }

    public BigDecimal getSpeed() {
        return Speed;
    }

    public boolean isTestDone() {
        return TestDone;
    }

    public URL getURL() {
        return ToTest;
    }

    public String getMessage() {
        return Message;
    }

    public int hashCode() {
        return ToTest.hashCode();
    }

    public void buildTree(PrintWriter out) throws IOException {
        out.println("<pre>");
        buildTree(out, Root, " - ");
        out.println("</pre>");
    }

    public Set<String> getBaseDirectory() {
        Set<String> ret = new HashSet<String>();
        if (Root != null) {
            for (Node child : Root.getChilds()) {
                ret.add(child.toString());
            }
        }
        return ret;
    }

    public void updateSpeed(BigDecimal speed) {
        Speed = speed;
        LastTimeUpdated = System.currentTimeMillis();
    }

    public long getLastTimeUpdated() {
        return LastTimeUpdated;
    }

    private static class Node implements Comparable {

        private String NodeName;

        private URL Link;

        private Map<String, FileMetaData> ListOfSeenFiles;

        private Set<Node> Childs;

        private Node(String nodeName, URL urlLink) {
            NodeName = nodeName;
            Link = urlLink;
            ListOfSeenFiles = new TreeMap<String, FileMetaData>();
            Childs = new TreeSet<Node>();
        }

        private void addFile(String name, FileMetaData metaData) {
            ListOfSeenFiles.put(name, metaData);
        }

        private void addChild(Node childNode) {
            Childs.add(childNode);
        }

        public String toString() {
            return NodeName;
        }

        public int compareTo(Object o) {
            return NodeName.compareTo(o.toString());
        }

        public Set<Node> getChilds() {
            return Childs;
        }

        public Map<String, FileMetaData> getFileList() {
            return ListOfSeenFiles;
        }
    }
}
