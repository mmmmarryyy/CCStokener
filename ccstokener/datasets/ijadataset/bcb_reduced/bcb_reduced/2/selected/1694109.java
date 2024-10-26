package com.rubika.aotalk;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.util.Log;

public class ItemXMLParser {

    protected static final String APPTAG = "--> AOTalk::ItemXMLParser";

    private String xmlpath = "";

    private ItemXMLData itemdata = null;

    public ItemXMLData getData() {
        try {
            URL url = new URL(xmlpath);
            ReadXML(url);
        } catch (MalformedURLException e) {
            Log.d("ItemXMLParser::getArray", "Bad URL");
        }
        return itemdata;
    }

    public void setXMLPath(String xml) {
        this.xmlpath = xml;
    }

    public String getXMLPath() {
        return this.xmlpath;
    }

    private void ReadXML(URL url) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            ItemXMLHandler xmlHandler = new ItemXMLHandler();
            xr.setContentHandler(xmlHandler);
            InputStream xmldata = url.openStream();
            xr.parse(new InputSource(xmldata));
            ItemXMLData parsedXMLDataSet = xmlHandler.getParsedData();
            itemdata = parsedXMLDataSet;
        } catch (Exception e) {
            Log.d("ItemXMLParser::ReadXML", "Error parsing : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
