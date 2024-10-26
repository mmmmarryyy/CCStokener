package edu.columbia.hypercontent;

import edu.columbia.filesystem.FileSystemException;
import edu.columbia.filesystem.IFileSystemManager;
import edu.columbia.hypercontent.util.SoftHashMap;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.jasig.portal.services.LogService;
import org.jasig.portal.utils.ResourceLoader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.Reader;
import java.io.CharArrayReader;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: Jul 8, 2003
 * Time: 3:17:18 PM
 * To change this template use Options | File Templates.
 */
public class Resolver implements EntityResolver, URIResolver {

    private static CatalogResolver cresolver;

    private final Project project;

    public Resolver(Project project) throws FileSystemException {
        this.project = project;
    }

    public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
        InputSource source = null;
        try {
            IFileSystemManager repository = project.getRepositoryManager();
            String trimmed = (trimFileURL(systemID));
            if (repository.exists(trimmed)) {
                source = new InputSource(repository.getFile(trimmed).getInputStream());
                source.setEncoding("UTF-8");
            } else if (trimmed.endsWith(".rdf")) {
                String sr = trimmed.substring(0, trimmed.indexOf(".rdf"));
                if (repository.exists(sr)) {
                    Reader r = new CharArrayReader(DocumentFactory.getRDFChars(repository.getFile(sr).getMetaData()));
                    source = new InputSource(r);
                }
            }
        } catch (FileSystemException e) {
            throw new SAXException(e);
        }
        if (source == null) {
            source = getCatalogResolver().resolveEntity(publicID, systemID);
        }
        return source;
    }

    protected static String trimFileURL(String url) {
        if (url.startsWith("file://")) {
            return url.substring(7);
        }
        return url;
    }

    public Source resolve(String href, String base) throws TransformerException {
        Source source = null;
        try {
            IFileSystemManager repository = project.getRepositoryManager();
            String trimmed = (trimFileURL(href));
            if (repository.exists(trimmed)) {
                source = new SAXSource(DocumentFactory.getXMLReader(this), new InputSource(repository.getFile(trimmed).getInputStream()));
            } else if (trimmed.endsWith(".rdf")) {
                String sr = trimmed.substring(0, trimmed.indexOf(".rdf"));
                if (repository.exists(sr)) {
                    Reader r = new CharArrayReader(DocumentFactory.getRDFChars(repository.getFile(sr).getMetaData()));
                    source = new SAXSource(DocumentFactory.getXMLReader(this), new InputSource(r));
                }
            }
        } catch (Exception e) {
            throw new TransformerException(e);
        }
        if (source == null) {
            source = getCatalogResolver().resolve(href, base);
        }
        return source;
    }

    public static URL resolveExternalURL(String location) {
        if (location == null || location.trim().equals("")) {
            return null;
        }
        URL url = null;
        try {
            url = new URL(getCatalogResolver().getResolvedEntity(null, location));
            java.io.InputStream stream = url.openStream();
            stream.close();
        } catch (Exception e) {
        }
        if (url == null) {
            try {
                url = new URL(location);
                java.io.InputStream stream = url.openStream();
                stream.close();
            } catch (Exception e) {
                url = null;
            }
        }
        return url;
    }

    public static synchronized CatalogResolver getCatalogResolver() {
        if (cresolver == null) {
            try {
                System.setProperty("xml.catalog.files", ResourceLoader.getResourceAsURLString(Resolver.class, "/properties/hypercontent/catalog.xml"));
                System.setProperty("xml.catalog.verbosity", "0");
                cresolver = new CachingCatalogResolver();
            } catch (Exception e) {
                LogService.log(LogService.ERROR, "DocumentFactory: unable to initialize catalog resolver");
                LogService.log(LogService.ERROR, e);
            }
        }
        return cresolver;
    }

    protected static class CachingCatalogResolver extends CatalogResolver {

        protected static final SoftHashMap cache = new SoftHashMap(100);

        public String getResolvedEntity(java.lang.String publicId, java.lang.String systemId) {
            String resolved = (String) cache.get(systemId);
            if (resolved == null) {
                resolved = super.getResolvedEntity(publicId, systemId);
                if (resolved == null) {
                    resolved = "null";
                }
                cache.put(systemId, resolved);
            }
            if ("null".equals(resolved)) {
                return null;
            }
            return resolved;
        }
    }
}
