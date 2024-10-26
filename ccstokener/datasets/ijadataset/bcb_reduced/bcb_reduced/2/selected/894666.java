package utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.facelets.impl.DefaultResourceResolver;
import com.sun.facelets.impl.ResourceResolver;

/**
 * Allows to use templates outside of the application root
 * 
 * @author k5
 */
public class TemplateResolver extends DefaultResourceResolver implements ResourceResolver {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<String> path;

    public TemplateResolver() throws IOException {
        logger.info("Init TemplateResolver");
        path = new LinkedList<String>();
        path.add("");
        for (Enumeration<URL> urlEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/template-resolver.properties"); urlEnum.hasMoreElements(); ) {
            Properties properties = new Properties();
            properties.load(urlEnum.nextElement().openStream());
            if (properties.containsKey("path")) {
                path.add(properties.getProperty("path"));
            }
        }
        logger.info("TemplateResolver initialised");
    }

    /** first check the context root, then the classpath */
    public URL resolveUrl(String path) {
        logger.debug("Resolving URL " + path);
        URL url = super.resolveUrl(path);
        if (url == null) {
            Iterator<String> prefixes = this.path.iterator();
            while (url == null && prefixes.hasNext()) {
                String fullPath = prefixes.next() + path;
                try {
                    File f = new File(fullPath);
                    if (f.exists()) {
                        url = new File(fullPath).toURI().toURL();
                        logger.info(url.toString());
                    }
                } catch (MalformedURLException e) {
                }
            }
        }
        return url;
    }
}
