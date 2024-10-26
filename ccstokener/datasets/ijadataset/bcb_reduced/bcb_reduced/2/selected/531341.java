package de.consolewars.api.parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import de.consolewars.api.data.AuthStatus;
import de.consolewars.api.exception.ConsolewarsAPIException;

/**
 * @author cerpin (arrewk@gmail.com)
 */
public abstract class AbstractSAXParser<T> extends DefaultHandler {

    private String APIURL;

    private T tempItem;

    protected String tempValue;

    private AuthStatus authStatus;

    private boolean isInAPISubtree = false;

    private ArrayList<T> items;

    public AbstractSAXParser(String APIURL) {
        this.APIURL = APIURL;
        items = new ArrayList<T>();
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempValue = "";
        if (qName.equals("item")) {
            tempItem = createTempItem();
        }
        if (qName.equals("api")) {
            isInAPISubtree = true;
        }
    }

    public void endElement(String uri, String localName, String qName) {
        parseAuthStatus(uri, localName, qName);
        if (qName.equals("item") && isValidItem()) {
            items.add(tempItem);
        }
        parseItem(uri, localName, qName);
    }

    /**
	 * additional item-requirements in order to add it to the list
	 * 
	 * @author cerpin (arrewk@gmail.com)
	 * @return true 
	 */
    protected abstract boolean isValidItem();

    /**
	 * parsing the item subtrees
	 * 
	 * @author cerpin (arrewk@gmail.com)
	 */
    protected abstract void parseItem(String uri, String localName, String qName);

    protected void parseAuthStatus(String uri, String localName, String qName) {
        if (!isInAPISubtree) return;
        if (qName.equals("authstatus")) {
            authStatus = new AuthStatus(tempValue);
        }
        if (qName.equals("reason") && isInAPISubtree) {
            authStatus.setReason(tempValue);
        }
        if (qName.equals("api")) {
            isInAPISubtree = false;
        }
    }

    public ArrayList<T> parseDocument() throws ConsolewarsAPIException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        URL url;
        try {
            url = new URL(APIURL);
            URLConnection connection = url.openConnection();
            connection.connect();
            SAXParser parser = spf.newSAXParser();
            parser.parse(connection.getInputStream(), this);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (MalformedURLException e1) {
            throw new ConsolewarsAPIException("Es sind Verbindungsprobleme aufgetreten", e1);
        } catch (IOException e) {
            throw new ConsolewarsAPIException("Ein-/Ausgabefehler", e);
        }
        return items;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempValue += new String(ch, start, length);
    }

    public AuthStatus getAuthStatus() throws ConsolewarsAPIException {
        if (authStatus == null) {
            parseDocument();
        }
        return authStatus;
    }

    public T getTempItem() {
        return tempItem;
    }

    /**
	 * creating a new item
	 * 
	 * @author cerpin (arrewk@gmail.com)
	 * @return
	 */
    protected abstract T createTempItem();
}
