package sample.gbase.basic;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *  Display the titles of the Google Base items that match a specific query. 
 */
public class QueryExample2 {

    /**
   * Url of the Google Base data API snippet feed.
   */
    private static final String SNIPPETS_FEED = "http://base.google.com/base/feeds/snippets";

    /**
   * The query that is sent over to the Google Base data API server.
   */
    private static final String QUERY = "cars [item type : products]";

    /**
   * Create a <code>QueryExample2</code> instance and
   * call <code>displayItems</code>. 
   */
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        new QueryExample2().displayItems();
    }

    /**
   * Connect to the Google Base data API server, retrieve the items that match
   * <code>QUERY</code> and call <code>DisplayTitlesHandler</code> to extract
   * and display the titles from the XML response.
   */
    public void displayItems() throws IOException, SAXException, ParserConfigurationException {
        URL url = new URL(SNIPPETS_FEED + "?bq=" + URLEncoder.encode(QUERY, "UTF-8"));
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        InputStream inputStream = httpConnection.getInputStream();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputStream, new DisplayTitlesHandler());
    }

    /**
   * Simple SAX event handler, which prints out the titles of all entries in the 
   * Atom response feed. 
   */
    private static class DisplayTitlesHandler extends DefaultHandler {

        /**
     * Stack containing the opening XML tags of the response.
     */
        private Stack<String> xmlTags = new Stack<String>();

        /**
     * Counter that keeps track of the currently parsed item.
     */
        private int itemNo = 0;

        /**
     * True if we are inside of a data entry's title, false otherwise.
     */
        private boolean insideEntryTitle = false;

        /**
     * Receive notification of an opening XML tag: push the tag to 
     * <code>xmlTags</code>. If the tag is a title tag inside an entry tag, 
     * turn <code>insideEntryTitle</code> to <code>true</code>.
     */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("title") && xmlTags.peek().equals("entry")) {
                insideEntryTitle = true;
                System.out.print("Item " + ++itemNo + ": ");
            }
            xmlTags.push(qName);
        }

        /**
     * Receive notification of a closing XML tag: remove the tag from teh stack.
     * If we were inside of an entry's title, turn <code>insideEntryTitle</code>
     * to <code>false</code>.
     */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            xmlTags.pop();
            if (insideEntryTitle) {
                insideEntryTitle = false;
                System.out.println();
            }
        }

        /**
     * Callback method for receiving notification of character data inside an
     * XML element. 
     */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (insideEntryTitle) {
                System.out.print(new String(ch, start, length));
            }
        }
    }
}
