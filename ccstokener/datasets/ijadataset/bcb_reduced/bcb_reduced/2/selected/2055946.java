package org.swingerproject.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

public final class Configuration {

    private AccessorsMapper methodMapper = new AccessorsMapper();

    private ComponentFactory componentFactory = new ComponentFactory();

    private ScriptMapper scriptMapper;

    private ObjectMapper objectMapper;

    ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    List<URL> libraries;

    public void load(InputStream config) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        SAXParser parser = factory.newSAXParser();
        ConfHandler handler = new ConfHandler(this);
        parser.parse(config, handler);
    }

    public void load(File file) throws SAXException, IOException, ParserConfigurationException {
        load(new FileInputStream(file));
    }

    public void load(URL url) throws SAXException, IOException, ParserConfigurationException {
        load(url.openStream());
    }

    public ScriptMapper getScriptMapper() {
        if (scriptMapper == null) {
            scriptMapper = new ScriptMapper();
        }
        return scriptMapper;
    }

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    public AccessorsMapper getMethodMapper() {
        return methodMapper;
    }

    public ComponentFactory getComponentFactory() {
        return componentFactory;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
