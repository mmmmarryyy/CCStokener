package lb.prove;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import android.content.Context;

public class RssRead extends DefaultHandler {

    boolean newData = false;

    String sourceUrl;

    Context context;

    DBHelper db;

    public RssRead() {
        ;
    }

    RssItem currItem = null;

    StringBuilder inputText = new StringBuilder();

    public void startElement(String uri, String name, String qName, Attributes atts) {
        String theName = qName.trim();
        if (theName.equals("item")) {
            currItem = new RssItem();
        }
    }

    public void endElement(String uri, String name, String qName) {
        String elmtName = qName.trim();
        if (elmtName.equals("item")) {
            addItem(currItem);
            currItem = null;
        } else if (elmtName.equals("title")) {
            if (null != currItem) currItem.title = inputText.toString().trim();
        } else if (elmtName.equals("description")) {
            if (null != currItem) currItem.body = inputText.toString().trim();
        } else if (elmtName.equals("guid")) {
            if (null != currItem) currItem.guid = inputText.toString().trim();
        } else if (elmtName.equals("pubDate")) {
            if (null != currItem) currItem.setPubDate(inputText.toString().trim());
        } else if (elmtName.equals("link")) {
            if (null != currItem) currItem.url = inputText.toString().trim();
        }
        inputText.setLength(0);
    }

    public void characters(char ch[], int start, int length) {
        this.inputText.append(ch, start, length);
    }

    public boolean getRSSEntries(String theUrl, Context contextParam, DBHelper dbParam) {
        context = contextParam;
        db = dbParam;
        sourceUrl = theUrl;
        try {
            try {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                xr.setContentHandler(this);
                URL url = new URL(sourceUrl);
                xr.parse(new InputSource(url.openStream()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newData;
    }

    void addItem(RssItem item) {
        if (db.addEntry(item.guid, item.title, item.body, "", new Date(), item.url)) newData = true;
    }
}
