package org.swingerproject;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.CSSException;
import org.swingerproject.conf.Configuration;
import org.swingerproject.conf.SwingerAdapterFactory;
import org.swingerproject.parser.SWGHandler;
import org.swingerproject.xml.css.CSS2EngineImpl;
import org.swingerproject.components.SwingerAdapter;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class Swinger {

    private static final Swinger instance = new Swinger();

    private Configuration configuration = new Configuration();

    private Context context = new Context();

    private Swinger() {
    }

    public static Swinger getInstance() {
        return instance;
    }

    public void configure(InputStream config) throws SAXException, IOException, ParserConfigurationException {
        configuration.load(config);
    }

    public void configure(File file) throws SAXException, IOException, ParserConfigurationException {
        configuration.load(file);
    }

    public void configure(URL url) throws SAXException, IOException, ParserConfigurationException {
        configuration.load(url);
    }

    public Component parse(URL url) throws SAXException, IOException, ParserConfigurationException {
        return parse(url.openStream());
    }

    public Component parse(File file) throws SAXException, IOException, ParserConfigurationException {
        return parse(new FileInputStream(file));
    }

    public Component parse(InputStream swg) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        SAXParser parser = factory.newSAXParser();
        SWGHandler handler = new SWGHandler(configuration);
        parser.parse(swg, handler);
        return handler.getRootComponent();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Context getContext() {
        return context;
    }
}
