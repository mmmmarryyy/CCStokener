package org.lindenb.bio.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * QueryKeyHandler
 * @author pierre
 *
 */
public class QueryKeyHandler extends DefaultHandler {

    private String QueryKey = null;

    private String WebEnv = null;

    private int countId = 0;

    private StringBuilder builder = null;

    /** specialized class returning a set of pmid */
    public static class FetchSet extends QueryKeyHandler {

        private HashSet<Integer> pmids = new HashSet<Integer>();

        public FetchSet() {
        }

        @Override
        public void foundId(String pmid) {
            this.pmids.add(Integer.parseInt(pmid.trim()));
        }

        /** return the set of pmid */
        public Set<Integer> getPMID() {
            return this.pmids;
        }
    }

    public QueryKeyHandler() {
    }

    @Override
    public void startDocument() throws SAXException {
        this.QueryKey = null;
        this.WebEnv = null;
        this.countId = 0;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (name.equals("WebEnv") || name.equals("QueryKey") || name.equals("Id")) {
            this.builder = new StringBuilder();
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (name.equals("WebEnv")) {
            this.WebEnv = this.builder.toString();
        } else if (name.equals("QueryKey")) {
            this.QueryKey = this.builder.toString();
        } else if (name.equals("Id")) {
            foundId(this.builder.toString());
            this.countId++;
        }
        this.builder = null;
    }

    public void foundId(String id) {
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.builder != null) {
            this.builder.append(ch, start, length);
        }
    }

    public String getWebEnv() {
        return WebEnv;
    }

    public String getQueryKey() {
        return QueryKey;
    }

    public int getIdCount() {
        return countId;
    }

    @Override
    public String toString() {
        return "{QueryKey:" + getQueryKey() + ",WebEnv:" + getWebEnv() + ",countId:" + getIdCount() + "}";
    }

    public static QueryKeyHandler parse(URL url) throws IOException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        SAXParser parser = null;
        try {
            parser = factory.newSAXParser();
        } catch (ParserConfigurationException err) {
            throw new SAXException(err);
        }
        QueryKeyHandler qkh = new QueryKeyHandler();
        InputStream in = url.openStream();
        parser.parse(in, qkh);
        in.close();
        return qkh;
    }
}
