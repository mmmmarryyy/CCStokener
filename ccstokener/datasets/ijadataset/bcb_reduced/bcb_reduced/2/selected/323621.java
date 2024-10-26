package edu.unl.cse.activitygraph.sources;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import edu.unl.cse.activitygraph.SeriesGroup;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class TracDataSource extends GenericDataSource {

    public TracDataSource(String urlString) throws IOException, ValidityException, ParsingException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Builder parser = new Builder();
        Document doc = parser.build(conn.getInputStream());
        this.seriesGroups = new ArrayList<SeriesGroup>();
        this.parseXml(doc);
    }

    private void parseXml(Document doc) throws ValidityException {
        Element root = doc.getRootElement();
        if (root.getLocalName() != "rss") {
            throw new ValidityException("Root node is type: " + root.getLocalName() + ", expected type: rss");
        }
        Node child;
        Element elt;
        for (int i = 0; i < root.getChildCount(); ++i) {
            child = root.getChild(i);
            if (child instanceof Element) {
                elt = (Element) child;
                if (elt.getLocalName() == "item") {
                    processItem(elt);
                }
            }
        }
    }

    private void processItem(Element elt) {
    }

    public boolean isEmpty() {
        return true;
    }
}
