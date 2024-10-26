package com.dyuproject.web.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The credentials are retrieved from java.util.Properties.
 * 
 * @author David Yu
 * @created Jun 29, 2008
 */
public class SimpleCredentialSource implements CredentialSource {

    public static SimpleCredentialSource newInstance(File file) throws IOException {
        return newInstance(new FileInputStream(file));
    }

    public static SimpleCredentialSource newInstance(URL url) throws IOException {
        return newInstance(url.openStream());
    }

    public static SimpleCredentialSource newInstance(InputStream stream) throws IOException {
        Properties properties = new Properties();
        properties.load(stream);
        return new SimpleCredentialSource(properties);
    }

    private final Properties _properties;

    public SimpleCredentialSource(Properties properties) {
        _properties = properties;
    }

    public String getPassword(String realm, String username, HttpServletRequest request) {
        return _properties.getProperty(username);
    }

    public void onAuthenticated(String realm, String username, String password, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }
}
