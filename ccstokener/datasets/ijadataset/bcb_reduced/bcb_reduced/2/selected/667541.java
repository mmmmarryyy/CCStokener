package com.aceitunproject.shared.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

/**
 * @author Juan Irungaray
 * 
 */
public class FileUtil {

    public static Properties loadClasspathProperties(String path) throws Exception {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource(path);
        props.load(url.openStream());
        return props;
    }

    public static Properties loadFileProperties(String path) throws Exception {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(new File(path));
        props.load(fis);
        return props;
    }
}
