package jomm.wikipedia;

import com.wcohen.ss.BasicStringWrapper;
import com.wcohen.ss.api.StringWrapper;
import com.wcohen.ss.api.StringWrapperIterator;
import com.wcohen.ss.tokens.SimpleTokenizer;
import experiments.TextSimilarityScorer;
import jomm.utils.StreamsUtils;
import nmaf.util.DomUtil;
import org.w3c.dom.Document;
import pt.utl.ist.lucene.treceval.geotime.webservices.CallWebServices;
import pt.utl.ist.lucene.utils.temporal.TimeExpression;
import pt.utl.ist.lucene.utils.temporal.tides.TimexesDocument;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jorge
 * Date: 7/Jul/2011
 * Time: 12:29:54
 * To change this template use File | Settings | File Templates.
 */
public class WikipediaClient {

    public static WikipediaDocument retreiveWikipediaDocument(String location) throws IOException {
        URL url = new URL(location);
        System.out.println("Geting location from wikipedia:" + location);
        InputStream is = url.openStream();
        String html = StreamsUtils.readString(is);
        WikipediaDocument wk = new WikipediaDocument();
        int index = 0;
        int startIndex = 0;
        while ((index = html.indexOf("<p>", startIndex)) > 0) {
            int indexEnd = html.indexOf("</p>", index);
            String paragraph = html.substring(index + 3, indexEnd);
            paragraph = paragraph.replaceAll("<[^>]+>", "");
            wk.getParagraphs().add(paragraph);
            startIndex = index + 3;
        }
        int indexTitle = html.indexOf("<title>");
        int indexTitleEnd = html.indexOf("- Wikipedia");
        wk.setTitle(html.substring(indexTitle + 6, indexTitleEnd));
        is.close();
        return wk;
    }

    static TextSimilarityScorer tfidf = new TextSimilarityScorer(new SimpleTokenizer(true, true));

    public static void main(String[] args) throws Exception {
        final WikipediaDocument wk = retreiveWikipediaDocument("http://en.wikipedia.org/wiki/Concorde");
        System.out.println("#####" + wk.getTitle());
        StringWrapperIterator it = new StringWrapperIterator() {

            Iterator<String> iter = wk.getParagraphs().iterator();

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return null;
            }

            public void remove() {
            }

            public StringWrapper nextStringWrapper() {
                return new BasicStringWrapper(iter.next());
            }
        };
        tfidf.train(it);
        String maxDate = "";
        int i = 1;
        double maxScore = 0;
        for (String paragraph : wk.getParagraphs()) {
            double score = tfidf.tfidf("last flight Concorde  land", paragraph);
            if (score > 0.01) {
                Document dom = CallWebServices.callTimextag("http://192.168.1.253:8080/jmachado/TIMEXTAG/index.php", "<DOC><TEXT>" + paragraph + "</TEXT></DOC>", wk.getTitle(), 0, 0, 0, "", "" + i);
                TimexesDocument td = new TimexesDocument("<doc id=\"" + i + "\">\n" + DomUtil.domToString(dom, false) + "\n</doc>");
                if (td.hasIndexableTimeExpressions()) {
                    if (score > maxScore) {
                        maxScore = score;
                        System.out.println("MaxScore Change " + maxScore);
                    }
                    System.out.println("<P score=\"" + score + "\">");
                    System.out.println(paragraph);
                    for (TimeExpression te : td.getAllIndexableTimeExpressions()) {
                        if (maxDate.compareTo(te.getNormalizedExpression()) < 0) {
                            maxDate = te.getNormalizedExpression();
                            System.out.println("Max DATE CHANGE for paragraph: " + i + " -> " + maxDate);
                        }
                    }
                    System.out.println("</p>");
                }
            }
        }
    }
}
