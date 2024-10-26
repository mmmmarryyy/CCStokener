package de.uni_leipzig.lots.common.xml;

import com.sun.tools.xjc.reader.xmlschema.parser.LSInputSAXWrapper;
import org.apache.xml.resolver.Catalog;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the main EntityResolver and LSResourceResolver of LOTS.
 * <p/>
 * It is available through applicationContext:resolver.
 *
 * @author Alexander Kiel
 * @version $Id: Resolver.java,v 1.10 2007/10/23 06:30:33 mai99bxd Exp $
 */
public class Resolver implements EntityResolver, LSResourceResolver {

    protected static final Logger logger = Logger.getLogger(Resolver.class.getName());

    private Resource catalogResource;

    private Catalog catalog;

    /**
     * Do not instantiate directly. Use applicationContext:resolver.
     */
    public Resolver() {
    }

    @Required
    public void setCatalogResource(Resource catalogResource) {
        this.catalogResource = catalogResource;
    }

    public void init() throws IOException {
        URL catalogUrl = catalogResource.getURL();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Open catalog from: " + catalogUrl);
        }
        catalog = new Catalog();
        catalog.setupReaders();
        catalog.parseCatalog(catalogUrl);
    }

    /**
     * @param publicId
     * @param systemId
     * @return can return <tt>null</tt>
     * @throws SAXException
     * @throws IOException
     */
    @Nullable
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (publicId == null && systemId == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Nothing to resolve!");
            }
            return null;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Try to resolve the entity with the public ID: " + publicId + " and system ID: " + systemId + ".");
        }
        InputSource inputSource = resolveIntern(publicId, systemId);
        if (inputSource == null && logger.isLoggable(Level.WARNING)) {
            logger.warning("Failed to resolve the entity with the public ID: " + publicId + " and system ID: " + systemId + ".");
        }
        return inputSource;
    }

    @Nullable
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Try to resolve the resource with the public ID: " + publicId + ", system ID: " + systemId + " and baseURI " + baseURI + ".");
        }
        InputSource inputSource = null;
        try {
            inputSource = resolveIntern(publicId, systemId);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "", e);
        }
        if (inputSource != null) {
            return new LSInputSAXWrapper(inputSource);
        }
        if (baseURI != null) {
            String resolved = baseURI.substring(0, baseURI.lastIndexOf('/') + 1) + systemId;
            try {
                URL url = new URL(resolved);
                url.openConnection().connect();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Resolve with help of baseURI to: " + resolved);
                }
                inputSource = new InputSource(resolved);
                return new LSInputSAXWrapper(inputSource);
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
        }
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning("Failed to resolve the resource with the public ID: " + publicId + ", system ID: " + systemId + " and baseURI " + baseURI + ".");
        }
        return null;
    }

    @Nullable
    private InputSource resolveIntern(@Nullable String publicId, @Nullable String systemId) throws IOException {
        String resolved;
        if (publicId == null) {
            resolved = catalog.resolveSystem(systemId);
        } else {
            resolved = catalog.resolvePublic(publicId, systemId);
        }
        if (resolved != null) {
            InputSource inputSource = new InputSource(resolved);
            inputSource.setPublicId(publicId);
            return inputSource;
        } else {
            return null;
        }
    }
}
