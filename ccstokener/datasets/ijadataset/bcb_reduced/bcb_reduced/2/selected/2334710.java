package com.cnoja.jmsncn.utils.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import utils.file.FileUtils;

public class PropertyLoader {

    public static Properties loadProperty(String path) throws IOException, FileNotFoundException, URISyntaxException {
        return loadProperty(new File(path));
    }

    public static Properties loadProperty(File f) throws IOException, FileNotFoundException, URISyntaxException {
        return loadProperty(new FileInputStream(f));
    }

    public static Properties loadProperty(URL url) throws IOException, FileNotFoundException, URISyntaxException {
        return loadProperty(url.openStream());
    }

    public static Properties loadProperty(InputStream stream) throws IOException, FileNotFoundException, URISyntaxException {
        Properties properties = new Properties();
        properties.load(stream);
        return properties;
    }

    public void save(Properties properties, String path, String comment) throws IOException {
        FileUtils fileUtils = new FileUtils();
        OutputStream outputStream = fileUtils.openOutputStream(path, true);
        properties.store(outputStream, comment);
    }
}

class Dummy {
}
