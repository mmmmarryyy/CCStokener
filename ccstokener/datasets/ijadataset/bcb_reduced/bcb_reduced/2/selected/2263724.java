package at.ac.tuwien.law.yaplaf.plugin.input.yahooreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Properties;
import java.util.Date;
import java.util.StringTokenizer;
import java.io.*;
import java.net.*;
import org.apache.log4j.Logger;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;
import at.ac.tuwien.law.yaplaf.entities.*;
import at.ac.tuwien.law.yaplaf.exceptions.MissingPropertyException;
import at.ac.tuwien.law.yaplaf.interfaces.*;
import at.ac.tuwien.law.yaplaf.plugin.filter.html2textfilter.Html2TextFilter;
import com.yahoo.search.*;

/**
 * Liest von Yahoo Suchergebnisse aus und stellt sie als WebPaper bereit
 * @author TU Wien, YAPLAF++, Michael Appinger, 0425124
 */
public class YahooReader implements SearchEngine {

    private static Logger logger = Logger.getLogger(YahooReader.class);

    private int max_result = 0;

    private ArrayList<Range> ranges = new ArrayList<Range>();

    Properties props = null;

    @Override
    public List<Paper> execute() throws MissingPropertyException {
        LinkedList<Paper> papers = new LinkedList<Paper>();
        logger.info("Initialisiere Yahoo Search API");
        SearchClient client = new SearchClient("TIJb98PV34EpCL7Nnxb.M8T9yAa_8lkuuNxPdeGdAxvPTCiR.phrXJEvWRPC");
        WebSearchRequest request = new WebSearchRequest(props.getProperty("keywords"));
        try {
            logger.info("Setze Such-Anfrage ab");
            request.setResults(max_result);
            WebSearchResults results = client.webSearch(request);
            logger.info(results.getTotalResultsReturned() + " von insgesamt " + results.getTotalResultsAvailable() + " f�r Suchanfrage " + props.getProperty("keywords") + " gefunden.");
            Iterator<Range> it_range = ranges.iterator();
            while (it_range.hasNext()) {
                Range r = it_range.next();
                int low = r.isRealRange() ? r.getMin() - 1 : 0;
                int high = r.getMax();
                for (int i = low; i < high && i < results.getTotalResultsReturned().intValue(); i++) {
                    WebSearchResult result = results.listResults()[i];
                    papers.add(getPaperFromResult(result, i));
                }
            }
        } catch (IOException e) {
            System.err.println("Error calling Yahoo! Search Service: " + e.toString());
            e.printStackTrace(System.err);
            logger.error("IOException w�hrend der Suchmaschinenabfrage!", e);
        } catch (SearchException e) {
            System.err.println("Error calling Yahoo! Search Service: " + e.toString());
            e.printStackTrace(System.err);
            logger.error("SearchException w�hrend der Suchmaschinenabfrage!", e);
        }
        return papers;
    }

    /**
	 * Erstellt aus einem Search-Result ein Paper
	 * @param result SearchResult der Suchabfrage
	 * @param cnt Z�hler, um Index in Titel einzuf�gen
	 * @return WebPaper Instanz
	 */
    private Paper getPaperFromResult(WebSearchResult result, int cnt) {
        String contenttype = "";
        String content = "";
        WebPaper p = new WebPaper();
        try {
            URL url = new URL(result.getUrl());
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
            contenttype = urlConnection.getContentType();
            if (contenttype.contains("text/html")) {
                InputStream input = url.openStream();
                Reader reader = new InputStreamReader(input);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String strLine = "";
                int count = 0;
                while (count < 10000) {
                    strLine = bufferedReader.readLine();
                    if (strLine != null && strLine != "") {
                        content += strLine;
                    }
                    count++;
                }
                bufferedReader.close();
                p.setPaperText(content);
                Html2TextFilter filter = new Html2TextFilter();
                filter.execute(p);
            } else if (contenttype.contains("application/pdf")) {
                BufferedInputStream bis = new BufferedInputStream(urlConnection.getInputStream());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("./temp.pdf"));
                int i;
                while ((i = bis.read()) != -1) {
                    bos.write(i);
                }
                bis.close();
                bos.close();
                PDFTextStripper pts = new PDFTextStripper();
                content = pts.getText(PDDocument.load("temp.pdf"));
                File f = new File("temp.pdf");
                f.delete();
                p.setPaperText(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        p.setSource(result.getUrl());
        p.setAccess_date(new Date());
        p.setTitle((cnt + 1) + ", " + result.getTitle());
        p.setNachname((cnt + 1) + ", " + result.getTitle());
        logger.info("Paper erstellt: " + p.getTitle() + ", " + p.getSource());
        return p;
    }

    @Override
    public Properties getProperties() {
        return props;
    }

    @Override
    public Map<String, String> getPropertiesDescription() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("keywords", "Keywords, die bei der Suche verwendet werden");
        map.put("limit", "Anzahl der Eintr�ge, die untersucht werden sollen bzw. Bereich der betrachtet wird");
        return map;
    }

    @Override
    public void setProperties(Properties props) throws MissingPropertyException {
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        StringTokenizer chunks = new StringTokenizer(props.getProperty("limit"), ";-");
        this.props = props;
        while (chunks.hasMoreTokens()) {
            try {
                numbers.add(Integer.parseInt(chunks.nextToken()));
                Collections.sort(numbers);
                max_result = numbers.get(numbers.size() - 1);
            } catch (NumberFormatException ex) {
                logger.error("Konnte limit-Property nicht korrekt in Int umwandeln", ex);
            }
        }
        String[] chunks_range = props.get("limit").toString().split(";");
        ranges.clear();
        for (int i = 0; i < chunks_range.length; i++) {
            Range r = new Range(chunks_range[i]);
            ranges.add(r);
        }
        Collections.sort(ranges);
    }
}
