package poi.build;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import poi.configuration.*;
import poi.gps.*;
import poi.search.*;
import poi.net.*;
import poi.db.*;
import org.apache.log4j.*;
import edu.mit.jwi.item.POS;
import poi.analyses.*;

public class ResultInterpreter implements Callable<GPSList> {

    private SearchResultBuilder rb;

    private EnvironmentDB db;

    private GPSList pois;

    private Logger logger = LoggerFactory.getInstance().getLog(getClass());

    public ResultInterpreter(String locationtype, POS pos, EnvironmentDB db) {
        this(db);
        logger.info("Creating a result builder using a provided query term");
        this.rb = new SearchResultBuilder(locationtype, db.getEnvironment(), pos);
    }

    private ResultInterpreter(EnvironmentDB db) {
        this.db = db;
        this.pois = new GPSList();
    }

    /**
     * Interprets the URL's found by the search and tries to match
     * locations of the nodemap with it
     */
    private void interpret() {
        boolean dnsFound;
        List<SearchResult> results = rb.buildResults();
        logger.info("Interpreting search results...");
        for (SearchResult sr : results) {
            System.out.println("analyzing " + sr.getUrlString());
            URL url;
            try {
                dnsFound = DnsImpl.lookup(sr.getUrlString());
                if (dnsFound) {
                    url = new URL(sr.getUrlString());
                    URLConnection yc = url.openConnection();
                    yc.setConnectTimeout(5000);
                    locateAndAddPOIs(yc);
                } else System.out.println("DNS not found!");
            } catch (MalformedURLException e) {
                logger.error("Bad url exception for " + sr.getUrlString() + " " + e.getMessage());
            } catch (IOException e) {
                logger.error("I/O exception: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Common exception: " + e.getMessage());
            }
        }
    }

    private void locateAndAddPOIs(URLConnection yc) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine;
        StringBuffer htmlcontent = new StringBuffer();
        while ((inputLine = reader.readLine()) != null) htmlcontent.append(inputLine);
        PoiAnalyse a = new PoiAnalyse(htmlcontent.toString(), db);
        a.analyse();
        pois.addAll(a.getPOIs());
        reader.close();
    }

    @Override
    public GPSList call() throws Exception {
        interpret();
        return pois;
    }
}
