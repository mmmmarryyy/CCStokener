package it.dangelo.saj.impl;

import java.io.InputStream;
import java.net.URI;
import it.dangelo.saj.SAJException;
import it.dangelo.saj.parser.ResourceResolver;

public class DefaultResourceResolver implements ResourceResolver {

    public InputStream resolve(String uri) throws SAJException {
        try {
            URI url = new URI(uri);
            InputStream stream = url.toURL().openStream();
            if (stream == null) throw new SAJException("URI " + uri + " can't be resolved");
            return stream;
        } catch (SAJException e) {
            throw e;
        } catch (Exception e) {
            throw new SAJException("Invalid uri to resolve " + uri, e);
        }
    }
}
