package de.tmtools.core.io;

import org.tm4j.topicmap.TopicMap;
import org.tm4j.topicmap.TopicMapProviderException;
import org.tm4j.topicmap.TopicMapProviderFactory;
import org.tm4j.topicmap.TopicMapProvider;
import org.tm4j.topicmap.source.TopicMapSource;
import org.tm4j.topicmap.source.SerializedTopicMapSource;
import org.tm4j.net.Locator;
import org.tm4j.net.LocatorFactoryException;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;
import java.util.Properties;

public class XTMReader {

    private final String backendClassName = "org.tm4j.topics.memory.TopicMapProviderFactoryImpl";

    private static final Properties backendProps = System.getProperties();

    public static TopicMap getTopicMap(String inputAddress) {
        File inputFile = null;
        URL inputURL = null;
        try {
            inputURL = new URL(inputAddress);
        } catch (java.net.MalformedURLException ex) {
            inputFile = new File(inputAddress);
            if (!inputFile.exists()) {
            }
        }
        String baseURI = null;
        if (inputFile != null) {
            try {
                baseURI = inputFile.toURL().toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            baseURI = inputURL.toString();
        }
        InputStream inputStream = null;
        if (inputFile != null) {
            try {
                inputStream = new FileInputStream(inputFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                inputStream = inputURL.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return getTopicMap(inputStream, baseURI);
    }

    public static TopicMap getTopicMap(URL url) {
        String baseURI = url.toString();
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getTopicMap(inputStream, baseURI);
    }

    public static TopicMap getTopicMap(File file) {
        String baseURI = null;
        try {
            baseURI = file.toURL().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return getTopicMap(inputStream, baseURI);
    }

    private static TopicMap getTopicMap(InputStream inputStream, String baseURI) {
        TopicMapProviderFactory m_providerFactory = TopicMapProviderFactory.newInstance();
        TopicMapProvider m_provider = null;
        try {
            m_provider = m_providerFactory.newTopicMapProvider(backendProps);
        } catch (TopicMapProviderException e) {
            e.printStackTrace();
        }
        Locator baseLocator = null;
        try {
            baseLocator = m_provider.getLocatorFactory().createLocator("URI", baseURI);
        } catch (LocatorFactoryException e) {
            e.printStackTrace();
        }
        TopicMapSource src = new SerializedTopicMapSource(inputStream, baseLocator);
        System.out.println("Parsing configuration map: " + baseLocator.getAddress());
        TopicMap tm = null;
        try {
            tm = m_provider.addTopicMap(src);
        } catch (TopicMapProviderException e) {
            e.printStackTrace();
        }
        return tm;
    }
}
