package com.fogas.koll3ctions.tools;

import static com.fogas.konsole.Konsole.println;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import com.fogas.koll3ctions.tools.xml.XmlContentHandler;
import com.fogas.koll3ctions.tools.xml.bean.VersionInfo;

public class KUpdater {

    private static final String UPDATE_INFO_URL = "http://janos.fogas.eu/koll3ctions/latestinfo.xml";

    private boolean debug;

    public KUpdater() {
        debug = false;
    }

    public KUpdater(boolean debug) {
        this.debug = debug;
    }

    public VersionInfo getVersionInfo() {
        return getVersionInfo(UPDATE_INFO_URL);
    }

    public VersionInfo getVersionInfo(String url) {
        try {
            XmlContentHandler handler = new XmlContentHandler();
            XMLReader myReader = XMLReaderFactory.createXMLReader();
            myReader.setContentHandler(handler);
            myReader.parse(new InputSource(new URL(url).openStream()));
            return handler.getVersionInfo();
        } catch (SAXException e) {
            if (debug) {
                println("SAXException was thrown!");
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            if (debug) {
                println("MalformedURLException was thrown!");
                e.printStackTrace();
            }
        } catch (IOException e) {
            if (debug) {
                println("IOException was thrown!");
                e.printStackTrace();
            }
        }
        return null;
    }
}
