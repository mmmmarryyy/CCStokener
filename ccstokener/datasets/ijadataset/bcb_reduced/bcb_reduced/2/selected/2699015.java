package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import net.sourceforge.processdash.templates.TemplateLoader;

/** Implementation of a persistence service that can load page content from
 * files in the dashboard template search path.
 */
public class TemplatePersistenceService implements PersistenceService {

    public InputStream open(String filename) throws IOException {
        URL url = TemplateLoader.resolveURL("cms/" + filename);
        if (url != null) return url.openStream();
        url = TemplateLoader.resolveURL(filename);
        if (url != null) return url.openStream();
        return null;
    }

    public OutputStream save(String qualifier, String filename) throws IOException {
        return null;
    }
}
