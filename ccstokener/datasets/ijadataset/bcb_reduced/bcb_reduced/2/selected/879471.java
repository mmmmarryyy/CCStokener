package uk.ac.lkl.common.util.database;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import org.xml.sax.InputSource;

public class FileDefinitionSource extends DefinitionSource {

    private Object object;

    private String sourcePath;

    private URL url;

    public FileDefinitionSource(String sourcePath) {
        this(null, sourcePath);
    }

    public FileDefinitionSource(Object object, String sourcePath) {
        this.object = object;
        this.sourcePath = sourcePath;
        this.url = deriveUrl();
    }

    public final InputSource getInputSource() {
        if (url == null) throw new RuntimeException("Cannot find table defs");
        try {
            InputStream stream = url.openStream();
            InputStreamReader reader = new InputStreamReader(stream);
            return new InputSource(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final URL getUrl() {
        return url;
    }

    private URL deriveUrl() {
        if (object == null) return getClass().getClassLoader().getResource(sourcePath); else {
            if (object instanceof Class<?>) return ((Class<?>) object).getResource(sourcePath); else return object.getClass().getResource(sourcePath);
        }
    }

    public final String getSourcePath() {
        return sourcePath;
    }

    public boolean isRelative() {
        return object != null;
    }

    public boolean isAbsolute() {
        return object == null;
    }
}
