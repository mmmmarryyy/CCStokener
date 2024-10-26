package tpac.lib.DAPSpider;

import java.util.*;
import java.net.*;
import java.io.*;
import tpac.lib.DigitalLibrary.Dataset;

/**
* The main work of spidering is done in this class.
* @author Ian C (ian@insight4.com), Pauline M (pauline@insight4.com)
**/
public class DapSpider {

    protected Collection workloadError;

    protected Collection workloadWaiting;

    protected Collection workloadProcessed;

    protected Collection dapUrls;

    protected ReportWriter report;

    protected boolean cancel = false;

    protected URL baseURL;

    private String proxy_userName;

    private String proxy_password;

    private String proxyHost, proxyPort;

    private boolean proxySet;

    private String spider_type;

    private Dataset dataset;

    private String filenameRegex;

    private String baseFileRegex;

    private DapAuthenticator auth;

    /**
	* Sole constructor for this class
	* @param _report - the report writer.  Currently there are two types: ReportWriterConsole and ReportWriterDatabase.
	* @param _baseURL - starting URL for "spidering".
	* @param _spider_type - the type of spider.  There are two types: "HTML_PARSER" for normal HTML parsers, and "IPCC_PARSERS" 
	* for special IPCC parsers (these doesn't crawl).
	**/
    public DapSpider(ReportWriter _report, URL _baseURL, String _spider_type, String _filenameRegex, String _baseFileRegex) throws InvalidParserTypeException {
        report = _report;
        MsgLog.setReportWriter(report);
        baseURL = _baseURL;
        proxySet = false;
        filenameRegex = _filenameRegex;
        baseFileRegex = _baseFileRegex;
        proxyHost = "";
        proxyPort = "";
        proxy_userName = "";
        proxy_password = "";
        workloadError = new Vector();
        workloadWaiting = new Vector();
        workloadProcessed = new Vector();
        dapUrls = new TreeSet();
        ParserFactory.isValidType(_spider_type);
        spider_type = _spider_type;
    }

    /**
	* Set the dataset to store the crawler results to.
	**/
    public void setDataset(Dataset dSet) {
        dataset = dSet;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public String getProxyUsername() {
        return proxy_userName;
    }

    public String getProxyPassword() {
        return proxy_password;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public boolean isProxySet() {
        return proxySet;
    }

    public void unsetProxy() {
        proxySet = false;
        proxyHost = "";
        proxyPort = "";
        proxy_userName = "";
        proxy_password = "";
        try {
            System.setProperty("proxySet", "false");
        } catch (IllegalStateException ie) {
            MsgLog.error("Error with unsetting proxy : IllegalStateException: " + ie.toString());
        } catch (NullPointerException nullpointer) {
            MsgLog.error("Error with unsetting proxy : key is null");
        }
    }

    /**
	* Call this to begin spidering.
	**/
    public void begin() {
        cancel = false;
        URL url;
        while (!workloadWaiting.isEmpty() && !cancel) {
            Object list[] = workloadWaiting.toArray();
            for (int i = 0; (i < list.length) && !cancel; i++) {
                url = (URL) list[i];
                MsgLog.mumble("==================");
                MsgLog.mumble("DapSpider.begin(): **processing url " + url.toString());
                processURL(url);
                MsgLog.mumble("==================");
            }
        }
    }

    /**
	* There are a number of "workload" lists.  This method clears these lists.
	**/
    public void clear() {
        workloadError.clear();
        workloadWaiting.clear();
        workloadProcessed.clear();
        dapUrls.clear();
    }

    /**
	* Stops the spidering.
	**/
    public void cancel() {
        cancel = true;
    }

    /**
	* Whenever a URL is encounter, this will be added to the spider for further crawling.
	* @param url - new URL to crawl.
	**/
    public void addURL(URL url) {
        if ((workloadWaiting.contains(url)) || (workloadError.contains(url)) || (workloadProcessed.contains(url))) {
            MsgLog.mumble("Already have url " + url.toString());
            return;
        }
        MsgLog.mumble("Adding to workload: " + url);
        workloadWaiting.add(url);
    }

    /**
	* Adds a valid DAP URL.  Note that any discovered URL will not be added again.
	* These URLs will not be spidered, but rather stored.
	* @param url - a valid DAP URL.
	**/
    public void addDapURL(String url) {
        if (dapUrls.contains(url)) {
            MsgLog.mumble("Already have DAP url: " + url);
            return;
        }
        dapUrls.add(url);
    }

    /**
	* This processes a given URL (attempts to connect to URL and retrieve DAP if possible).
	* @param url - url to process.
	**/
    private void processURL(URL url) {
        try {
            MsgLog.message("DapSpider.processURL(): Processing url: " + url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                MsgLog.error("Could not establish connection to URL");
            } else {
                String contentType = connection.getContentType();
                if (contentType == null) {
                    MsgLog.warning("DapSpider.processURL(): URL " + url + " is of unknown type");
                } else if ((contentType.indexOf("text") == 0) || (contentType.indexOf("html") >= 0)) {
                    MsgLog.mumble("DapSpider.processURL(): URL is of type html or text");
                    InputStream is = connection.getInputStream();
                    ParserFactory factory = ParserFactory.createFactory();
                    DapParser parser = factory.createParser(spider_type, this, baseURL, url.toString(), report, dataset, filenameRegex, baseFileRegex);
                    parser.parse(is);
                } else {
                    MsgLog.warning("DapSpider.processURL(): URL " + url + " is not of type text/html");
                }
            }
        } catch (IOException e) {
            workloadWaiting.remove(url);
            workloadError.add(url);
            MsgLog.error("Error with processing URL: " + url + " Exception thrown: " + e.toString());
            return;
        } catch (InvalidParserTypeException e) {
            workloadWaiting.remove(url);
            workloadError.add(url);
            MsgLog.error("Error with processing URL: " + url + " Exception thrown: " + e.toString());
            return;
        }
        workloadWaiting.remove(url);
        workloadProcessed.add(url);
        MsgLog.mumble("Complete: " + url);
        report.count(0);
    }

    public Collection getWorkloadError() {
        return workloadError;
    }

    public Collection getWorkloadWaiting() {
        return workloadWaiting;
    }

    public Collection getWorkloadProcessed() {
        return workloadProcessed;
    }

    public Collection getDapUrls() {
        return dapUrls;
    }

    public int getLogLevel() {
        return report.getLogLevel();
    }

    public void setLogLevel(int _logLevel) {
        report.setLogLevel(_logLevel);
    }
}
