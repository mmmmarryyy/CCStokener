package com.dyuproject.openid.ext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import com.dyuproject.openid.Identifier;
import com.dyuproject.openid.OpenIdContext;
import com.dyuproject.util.ClassLoaderUtil;
import com.dyuproject.util.validate.IPDomainValidator;

/**
 * Resolves the url from a given email address.
 * 
 * @author David Yu
 * @created Jan 11, 2009
 */
public final class EmailResolver implements Identifier.Resolver {

    /**
     * The default resource location. ("email_resolver.properties")
     */
    public static final String DEFAULT_RESOURCE_LOCATION = "email_resolver.properties";

    private final Properties _urls = new Properties();

    public EmailResolver() {
        this(DEFAULT_RESOURCE_LOCATION);
    }

    public EmailResolver(String resourceLoc) {
        URL resource = ClassLoaderUtil.getResource(resourceLoc, getClass());
        if (resource == null) throw new IllegalStateException("resource: " + resourceLoc + " not found");
        try {
            _urls.load(resource.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public EmailResolver(URL resource) throws IOException {
        _urls.load(resource.openStream());
    }

    public EmailResolver(InputStream resource) throws IOException {
        _urls.load(resource);
    }

    public EmailResolver(Properties urls) {
        _urls.putAll(urls);
    }

    /**
     * Add the domain of an email (e.g gmail.com) mapped with the given openid server {@code url}.
     */
    public void add(String emailDomain, String url) {
        _urls.put(emailDomain, url);
    }

    /**
     * Add the domain of an email (e.g gmail.com) mapped with the given openid server {@code url}.
     */
    public EmailResolver addDomain(String emailDomain, String url) {
        _urls.put(emailDomain, url);
        return this;
    }

    public void resolve(Identifier identifier, OpenIdContext context) {
        String id = identifier.getId();
        int idx = id.indexOf('@');
        if (idx > 0) {
            int start = idx + 1;
            char[] domain = new char[id.length() - start];
            id.getChars(start, id.length(), domain, 0);
            if (IPDomainValidator.isValid(domain)) {
                identifier.resolve(_urls.getProperty(new String(domain)));
            }
        }
    }
}
