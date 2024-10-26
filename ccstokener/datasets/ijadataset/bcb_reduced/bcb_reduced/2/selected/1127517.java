package com.adobe.epubcheck.xml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.adobe.epubcheck.util.ResourceUtil;
import com.thaiopensource.resolver.Identifier;
import com.thaiopensource.resolver.Input;
import com.thaiopensource.resolver.Resolver;
import com.thaiopensource.resolver.ResolverException;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.auto.AutoSchemaReader;

public class XMLValidator {

    String schemaName;

    Schema schema;

    /**
	 * Basic Resolver from Jing modified to add support for 
	 * resolving zip and jar relative locations.
	 * 
	 * @author george@oxygenxml.com
	 */
    public static class BasicResolver implements Resolver {

        private static final BasicResolver theInstance = new BasicResolver();

        protected BasicResolver() {
        }

        public static BasicResolver getInstance() {
            return theInstance;
        }

        public void resolve(Identifier id, Input input) throws IOException, ResolverException {
            if (!input.isResolved()) input.setUri(resolveUri(id));
        }

        public void open(Input input) throws IOException, ResolverException {
            if (!input.isUriDefinitive()) return;
            URI uri;
            try {
                uri = new URI(input.getUri());
            } catch (URISyntaxException e) {
                throw new ResolverException(e);
            }
            if (!uri.isAbsolute()) throw new ResolverException("cannot open relative URI: " + uri);
            URL url = new URL(uri.toASCIIString());
            input.setByteStream(url.openStream());
        }

        public static String resolveUri(Identifier id) throws ResolverException {
            try {
                String uriRef = id.getUriReference();
                URI uri = new URI(uriRef);
                if (!uri.isAbsolute()) {
                    String base = id.getBase();
                    if (base != null) {
                        URI baseURI = new URI(base);
                        if ("zip".equals(baseURI.getScheme()) || "jar".equals(baseURI.getScheme())) {
                            uriRef = new URL(new URL(base), uriRef).toExternalForm();
                        } else {
                            uriRef = baseURI.resolve(uri).toString();
                        }
                    }
                }
                return uriRef;
            } catch (URISyntaxException e) {
                throw new ResolverException(e);
            } catch (MalformedURLException e) {
                throw new ResolverException(e);
            }
        }
    }

    private class ErrorHandlerImpl implements ErrorHandler {

        public void error(SAXParseException exception) throws SAXException {
            exception.printStackTrace();
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            exception.printStackTrace();
        }

        public void warning(SAXParseException exception) throws SAXException {
            exception.printStackTrace();
        }
    }

    public XMLValidator(String schemaName) {
        try {
            String resourcePath = ResourceUtil.getResourcePath(schemaName);
            URL systemIdURL = ResourceUtil.getResourceURL(resourcePath);
            if (systemIdURL == null) {
                throw new RuntimeException("Could not find resource " + resourcePath);
            }
            InputSource schemaSource = new InputSource(systemIdURL.toString());
            PropertyMapBuilder mapBuilder = new PropertyMapBuilder();
            mapBuilder.put(ValidateProperty.RESOLVER, BasicResolver.getInstance());
            mapBuilder.put(ValidateProperty.ERROR_HANDLER, new ErrorHandlerImpl());
            AutoSchemaReader schemaReader = new AutoSchemaReader();
            this.schemaName = schemaName;
            schema = schemaReader.createSchema(schemaSource, mapBuilder.toPropertyMap());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Internal error: " + e + " " + schemaName);
        }
    }
}
