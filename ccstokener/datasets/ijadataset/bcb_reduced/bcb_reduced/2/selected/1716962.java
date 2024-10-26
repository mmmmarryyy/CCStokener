package linkChecker.model;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import org.apache.commons.net.ftp.*;

public class linkCheckerModel {

    private static final String FTPSERVER = "ftp.software.ibm.com";

    private static final String FTPUSER = "anonymous";

    private static final String FTPPW = "";

    private static final String BASEURL = "http://www-304.ibm.com";

    private static final String TITLE = "Software and device drivers";

    private static final int MAXSINGLETHREADSIZE = 25;

    private LinkedList<TreeNode> toSearchList, failedList, searchedList;

    private String curStatus;

    private HashMap<String, ArrayList<TreeNode>> categories;

    private ArrayList<TreeNode> rootNodeList, httpURLs, ftpURLs, uncheckedURLs, licenseURLs;

    private int failCnt;

    private boolean driverPage;

    public linkCheckerModel() {
        this.failCnt = 0;
        this.driverPage = false;
        this.toSearchList = new LinkedList<TreeNode>();
        this.searchedList = new LinkedList<TreeNode>();
        this.failedList = new LinkedList<TreeNode>();
        this.rootNodeList = new ArrayList<TreeNode>();
        this.httpURLs = new ArrayList<TreeNode>();
        this.ftpURLs = new ArrayList<TreeNode>();
        this.uncheckedURLs = new ArrayList<TreeNode>();
        this.licenseURLs = new ArrayList<TreeNode>();
        this.categories = new HashMap<String, ArrayList<TreeNode>>();
        HttpURLConnection.setFollowRedirects(false);
        this.curStatus = "";
    }

    public void advancedSearch(final ArrayList<String> catsToSearch) {
        ArrayList<Thread> threadList = new ArrayList<Thread>();
        class advSearchElem extends Thread {

            TreeNode toSearch;

            protected advSearchElem(TreeNode node) {
                super();
                this.toSearch = node;
            }

            protected TreeNode getToSearch() {
                return this.toSearch;
            }

            public void run() {
                parseHTMLPage(getToSearch(), false);
            }
        }
        for (String cat : catsToSearch) {
            for (TreeNode searchNode : (ArrayList<TreeNode>) categories.get(cat)) {
                advSearchElem e = new advSearchElem(searchNode);
                e.start();
                threadList.add(e);
            }
        }
        try {
            for (Thread t : threadList) {
                t.join();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        checkLinks();
        System.out.println("Check finished: " + failCnt);
        if (failCnt > 0) {
            for (TreeNode node : failedList) {
                System.out.println("failed links is: " + node.getURL());
            }
        }
    }

    private void checkLinks() {
        int listSize = httpURLs.size();
        int index = 0;
        int sqrtSize = new Double(Math.sqrt((double) listSize) + 0.5).intValue();
        ArrayList<CheckHTTPLinks> httpList = new ArrayList<CheckHTTPLinks>();
        if (listSize > MAXSINGLETHREADSIZE) {
            try {
                while (listSize > 0) {
                    if ((index * sqrtSize + sqrtSize) < httpURLs.size()) {
                        httpList.add(new CheckHTTPLinks(new ArrayList<TreeNode>(httpURLs.subList(index * sqrtSize, index * sqrtSize + sqrtSize))));
                    } else {
                        httpList.add(new CheckHTTPLinks(new ArrayList<TreeNode>(httpURLs.subList(index * sqrtSize, httpURLs.size() - 1))));
                    }
                    index++;
                    listSize -= sqrtSize;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        CheckFTPLinks ftpCheck = new CheckFTPLinks();
        for (CheckHTTPLinks httpCheck : httpList) {
            httpCheck.start();
        }
        ftpCheck.start();
        try {
            if (ftpCheck.isAlive()) {
                System.out.println("FTP THREAD JOIN " + ftpCheck.getName());
                ftpCheck.join();
            }
            for (CheckHTTPLinks httpCheck : httpList) {
                System.out.println("HTTP THREAD JOIN " + httpCheck.getName());
                httpCheck.join();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void basicSearch(int depth) {
    }

    public void startOver() {
        this.failCnt = 0;
        this.categories.clear();
        this.toSearchList.clear();
        this.uncheckedURLs.clear();
        this.failedList.clear();
        this.searchedList.clear();
        this.ftpURLs.clear();
        this.httpURLs.clear();
        this.categories.clear();
        this.driverPage = false;
        this.rootNodeList.clear();
        this.curStatus = "";
    }

    public HashMap<String, ArrayList<TreeNode>> getCategories() {
        return this.categories;
    }

    public boolean isDriversPage() {
        return this.driverPage;
    }

    public void setDriverPage(boolean driverPage) {
        this.driverPage = driverPage;
    }

    public void addBaseURL(String url) {
        this.rootNodeList.add(new TreeNode(null, url));
        this.curStatus = url + "\n";
    }

    public ArrayList<TreeNode> getAllURL() {
        return this.rootNodeList;
    }

    public String getStatus() {
        return this.curStatus;
    }

    public void updateCurStatus(String msg, String val) {
        this.curStatus = msg + val;
    }

    public void execute() {
        curStatus = "Stating to execute the program";
        toSearchList = new LinkedList<TreeNode>(rootNodeList);
        for (TreeNode s : rootNodeList) {
            parseHTMLPage(s, true);
        }
    }

    private void parseHTMLPage(TreeNode node, boolean isRoot) {
        String checkURI = node.getURL();
        HTMLEditorKit kit = new HTMLEditorKit();
        try {
            HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
            doc.putProperty("IgnoreCharsetDirective", true);
            Reader rd;
            if ((rd = getReader(checkURI)) == null) {
                System.exit(0);
            }
            kit.read(rd, doc, 0);
            ElementIterator it = new ElementIterator(doc);
            Element elem;
            this.curStatus = "Starting Validation from: " + checkURI;
            String categoryKey = null;
            ArrayList<TreeNode> categoryValue = new ArrayList<TreeNode>();
            boolean validCategory = false;
            while ((elem = it.next()) != null) {
                AttributeSet as = (AttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
                if (as != null) {
                    String name = (String) as.getAttribute(HTML.Attribute.NAME);
                    String id = (String) as.getAttribute(HTML.Attribute.ID);
                    String href = (String) as.getAttribute(HTML.Attribute.HREF);
                    if ((name != null) && name.equals("DOCTOP") && isRoot) {
                        it.next();
                        it.next();
                        elem = it.next();
                        int d = elem.getEndOffset() - elem.getStartOffset();
                        String title = doc.getText(elem.getStartOffset(), d);
                        setDriverPage(title.contains(TITLE));
                    } else if (href != null && href.equals("#DOCTOP") && isRoot) {
                        validCategory = false;
                        if (categoryKey != null && !categoryValue.isEmpty()) {
                            categories.put(categoryKey, new ArrayList<TreeNode>(categoryValue));
                            System.out.println("Key: " + categoryKey + "\n\tSize: " + categoryValue.size() + "\n\tVal: " + categoryValue);
                            categoryKey = null;
                            categoryValue.clear();
                        }
                    }
                    if ((elem.getParentElement() != null) && (id != null) && (name != null) && isRoot && isDriversPage()) {
                        if (categoryKey != null) {
                            categories.put(categoryKey, new ArrayList<TreeNode>(categoryValue));
                            System.out.println("Key: " + categoryKey + "\n\tSize: " + categoryValue.size() + "\n\tVal: " + categoryValue);
                            categoryValue.clear();
                        }
                        int diff = elem.getParentElement().getEndOffset() - elem.getParentElement().getStartOffset();
                        categoryKey = (doc.getText(elem.getParentElement().getStartOffset(), diff)).trim();
                        validCategory = true;
                    } else if (href != null && !toSearchList.contains(href) && !searchedList.contains(href)) {
                        if (href.startsWith("/jct01004c/")) {
                            href = BASEURL + href;
                        } else if (href.startsWith("//www.ibm.com")) {
                            href = "http:" + href;
                        }
                        toSearchList.add(new TreeNode(node, href));
                        if (validCategory && isDriversPage() && isRoot) {
                            categoryValue.add(new TreeNode(node, href.trim()));
                        }
                        buildURLLists(new TreeNode(node, href));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.curStatus = "finished building URL Lists";
    }

    private Reader getReader(String uri) throws IOException {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            URLConnection conn = new URL(uri).openConnection();
            return new InputStreamReader(conn.getInputStream());
        } else {
            try {
                return new FileReader(uri);
            } catch (FileNotFoundException fe) {
                System.out.println("FILE NOT FOUND: Search Cancelled");
                fe.printStackTrace();
                return null;
            }
        }
    }

    private void buildURLLists(TreeNode node) {
        String urlString = node.getURL();
        if (urlString.startsWith("ftp://")) {
            if (!ftpURLs.contains(urlString)) {
                updateCurStatus("adding to ftpURLs: ", node.getURL());
                ftpURLs.add(node);
            }
        } else if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
            updateCurStatus("adding to httpURLs: ", node.getURL());
            httpURLs.add(node);
        } else if (urlString.startsWith("license")) {
            updateCurStatus("adding to license list: ", node.getURL());
            licenseURLs.add(node);
        } else {
            updateCurStatus("Connection not HTTP, HTTPS, or FTP: ", urlString);
            uncheckedURLs.add(node);
        }
    }

    private class CheckFTPLinks extends Thread {

        public void run() {
            ArrayList<Thread> ftpThreadList = new ArrayList<Thread>();
            class ftpLinkElem extends Thread {

                TreeNode node;

                FTPClient ftp;

                protected ftpLinkElem(TreeNode node, FTPClient ftpC) {
                    super();
                    this.node = node;
                    this.ftp = ftpC;
                }

                public void run() {
                    try {
                        String fileLoc = getFileLocation(node.getURL());
                        FTPFile[] fTest = ftp.listFiles(fileLoc);
                        if (fTest[0].isFile()) {
                            int rsp = ftp.getReplyCode();
                            String msg = ftp.getReplyString();
                            if (!FTPReply.isPositiveCompletion(rsp)) {
                                ftp.disconnect();
                                System.out.print("[FAILED] " + msg);
                                failedList.add(node);
                                failCnt++;
                            }
                            System.out.println("FTP Response: " + msg);
                        } else {
                            System.out.print("[FAILED]");
                            failedList.add(node);
                            failCnt++;
                        }
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }
            }
            updateCurStatus("Checking ftp links", "");
            for (TreeNode testFtp : ftpURLs) {
                try {
                    FTPClient ftp = new FTPClient();
                    ftp.connect(FTPSERVER);
                    ftp.login(FTPUSER, FTPPW);
                    updateCurStatus(testFtp.getURL(), "");
                    ftpLinkElem fe = new ftpLinkElem(testFtp, ftp);
                    fe.start();
                    ftpThreadList.add(fe);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (Thread t : ftpThreadList) {
                try {
                    if (t.isAlive()) {
                        t.join();
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    private String getFileLocation(String ftpString) {
        String[] temp = ftpString.split(FTPSERVER);
        updateCurStatus("path: ", temp[1]);
        return temp[1];
    }

    private class CheckHTTPLinks extends Thread {

        ArrayList<TreeNode> urlList;

        private CheckHTTPLinks(ArrayList<TreeNode> urlList) {
            super();
            this.urlList = urlList;
        }

        public void run() {
            updateCurStatus("Checking HTTP Links", "");
            for (TreeNode node : this.urlList) {
                try {
                    URL url = new URL(node.getURL());
                    URLConnection connection = url.openConnection();
                    if (connection instanceof HttpURLConnection) {
                        httpValidate((HttpURLConnection) connection, node);
                    }
                } catch (IOException e) {
                    System.out.println("HTTP LINK Failed: " + node.getURL());
                    e.printStackTrace();
                }
            }
        }
    }

    private void httpValidate(HttpURLConnection httpCon, TreeNode node) throws IOException {
        httpCon.setRequestMethod("HEAD");
        httpCon.connect();
        int res = httpCon.getResponseCode();
        String msg = httpCon.getResponseMessage();
        if (res >= 400) {
            System.out.println("[FAILED] " + msg);
            System.out.println(getConnectionString(httpCon));
            failedList.add(node);
            failCnt++;
        }
        updateCurStatus("HTTP Response: ", res + " " + msg);
        System.out.println("HTTP Response: " + res + " " + msg);
        String loc = httpCon.getHeaderField("Location");
        if (loc != null) {
            updateCurStatus("Location: ", loc);
            this.curStatus = "Location: " + loc;
        }
        httpCon.disconnect();
    }

    private String getConnectionString(HttpURLConnection httpCon, boolean https) {
        String[] temp;
        if (https) temp = httpCon.toString().split("https.d"); else temp = httpCon.toString().split("Connection:");
        return temp[1];
    }

    private String getConnectionString(HttpURLConnection httpCon) {
        return getConnectionString(httpCon, false);
    }
}
