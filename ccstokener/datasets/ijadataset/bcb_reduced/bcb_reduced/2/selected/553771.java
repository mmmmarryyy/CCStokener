package net.stickycode.mockwire;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class PomUtils {

    public static String loadVersion(String groupId, String artifactId) {
        URL url = PomUtils.class.getResource("/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
        if (url == null) return "SNAPSHOT";
        Properties p = new Properties();
        try {
            InputStream in = url.openStream();
            p.load(in);
            return p.getProperty("version");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + url, e);
        }
    }
}
