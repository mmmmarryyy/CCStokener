package hapax;

import hapax.parser.TemplateParser;
import hapax.parser.CTemplateParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory cache of parsed {@link Templates} intended to be
 * shared across threads.
 *
 * @author dcoker
 * @author jdp
 */
public class TemplateCache extends Object implements TemplateLoader {

    /**
     * Creates a TemplateLoader for CTemplate language
     */
    public static TemplateLoader create(String base_path) {
        return new TemplateCache(base_path);
    }

    /**
     * Creates a TemplateLoader using the argument parser.
     */
    public static TemplateLoader createForParser(String base_path, TemplateParser parser) {
        return new TemplateCache(base_path, parser);
    }

    private final Map<String, Template> cache = new HashMap<String, Template>();

    private final String baseDir;

    private final TemplateParser parser;

    public TemplateCache(String baseDir) {
        super();
        this.baseDir = baseDir;
        this.parser = null;
    }

    public TemplateCache(String baseDir, TemplateParser parser) {
        super();
        this.baseDir = baseDir;
        this.parser = parser;
    }

    /**
     * Parses and fetches a template from disk.
     *
     * @param resource The path to the template, relative to the templateDirectory
     *                 passed to the ctor of TemplateCache.
     */
    public Template getTemplate(String resource) throws TemplateException {
        String filename = Path.toFile(this.baseDir, resource);
        try {
            URL url = new URL(filename);
            return this.read(url);
        } catch (java.net.MalformedURLException exc) {
            File file = new File(filename);
            long fileLast = file.lastModified();
            Template template = this.hitCache(file.getPath(), fileLast);
            if (null != template) return template; else return this.read(file, fileLast);
        }
    }

    /**
     * Url fetch with no caching.
     */
    protected Template read(URL url) throws TemplateException {
        String parent = Parent(url);
        TemplateLoaderContext context = new TemplateLoaderContext(this, parent, false);
        String contents;
        InputStream in = null;
        try {
            in = url.openStream();
            contents = this.readToString(new java.io.InputStreamReader(in));
        } catch (IOException exc) {
            throw new TemplateException(url.toString(), exc);
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
        TemplateParser parser = this.parser;
        if (null == parser) return (new Template(contents, context)); else return (new Template(parser, contents, context));
    }

    protected Template read(File file, long fileLast) throws TemplateException {
        TemplateLoaderContext context = new TemplateLoaderContext(this, file.getParent());
        String contents;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            contents = this.readToString(reader);
        } catch (IOException exc) {
            throw new TemplateException(file.getPath(), exc);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
        Template template;
        TemplateParser parser = this.parser;
        if (null == parser) template = new Template(fileLast, contents, context); else template = new Template(fileLast, parser, contents, context);
        synchronized (this.cache) {
            this.cache.put(file.getPath(), template);
        }
        return template;
    }

    protected final Template hitCache(String filename, long fileLast) {
        Template template = this.cache.get(filename);
        if (null != template) {
            long templateLast = template.getLastModified();
            if (templateLast >= fileLast) return template;
        }
        return null;
    }

    protected static final String readToString(Reader in) throws IOException {
        StringBuilder string = new StringBuilder();
        char[] buf = new char[0x200];
        int read;
        while (0 < (read = in.read(buf, 0, 0x200))) {
            string.append(buf, 0, read);
        }
        return string.toString();
    }

    protected static final String Parent(URL u) {
        String url = u.toExternalForm();
        int idx = url.lastIndexOf('/');
        if (-1 != idx) return url.substring(0, idx); else return url;
    }
}
