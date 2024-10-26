package org.qtitools.constructr.itembank;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.qtitools.util.ContentPackage;
import org.qtitools.util.PropertiesManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class MinibixQTIbankItem implements Item {

    private static final long serialVersionUID = 1L;

    private Map<String, String> deposit;

    private String itembankURL;

    public MinibixQTIbankItem(Map<String, String> deposit) {
        this(deposit, PropertiesManager.getProperty("constructr", "minibix.qtibank.url"));
    }

    public MinibixQTIbankItem(Map<String, String> deposit, String itembankURL) {
        this.deposit = deposit;
        this.itembankURL = itembankURL;
    }

    public MinibixQTIbankItem(String ticket) {
        this(ticket, PropertiesManager.getProperty("constructr", "minibix.qtibank.url"));
    }

    protected MinibixQTIbankItem(String ticket, String itembankURL) {
        this.itembankURL = itembankURL;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser saxParser;
        try {
            saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            DepositHandler handler = new DepositHandler();
            xmlReader.setContentHandler(handler);
            URL url = new URL(itembankURL + "/" + ticket + "/metadata");
            xmlReader.parse(url.toString());
            deposit = handler.getDeposit();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return deposit.get(PropertiesManager.getProperty("constructr", "minibix.qtibank.descriptionfield"));
    }

    public String getName() {
        return deposit.get(PropertiesManager.getProperty("constructr", "minibix.qtibank.titlefield"));
    }

    public ContentPackage resolveItem() {
        URL url;
        try {
            url = new URL(itembankURL + "/" + deposit.get("http://www.caret.cam.ac.uk/minibix/metadata/ticket"));
            return new ContentPackage(url.openStream());
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "item(name=\"" + getName() + "\")";
    }

    private class DepositHandler extends DefaultHandler {

        Map<String, String> deposit;

        String key;

        StringBuffer textBuffer;

        public Map<String, String> getDeposit() {
            return deposit;
        }

        @Override
        public void startDocument() throws SAXException {
            deposit = new HashMap<String, String>();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equals("pair")) {
                key = atts.getValue("key");
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (qName.equals("pair")) {
                deposit.put(key, textBuffer.toString());
                textBuffer = null;
            }
        }

        @Override
        public void characters(char buf[], int offset, int len) throws SAXException {
            String s = new String(buf, offset, len);
            if (textBuffer == null) {
                textBuffer = new StringBuffer(s);
            } else {
                textBuffer.append(s);
            }
        }
    }

    public static void main(String[] args) {
        MinibixQTIbankItem item = new MinibixQTIbankItem("5", "http://qtitools.caret.cam.ac.uk/qtibank-webserv/deposits/all/");
        System.out.println(item);
        System.out.println(item.deposit);
    }
}
