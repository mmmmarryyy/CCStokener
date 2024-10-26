package org.inigma.migrations.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.inigma.migrations.AbstractMigrationResource;
import org.inigma.migrations.SchemaInfo;

class ClasspathMigrationResource extends AbstractMigrationResource {

    private static final long serialVersionUID = 5075811847612235350L;

    private final URL url;

    public ClasspathMigrationResource(String uri, SchemaInfo ref) {
        super(uri.substring(uri.lastIndexOf("/")), ref);
        this.url = getClass().getResource(uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClasspathMigrationResource) {
            return url.equals(((ClasspathMigrationResource) obj).url);
        } else if (obj instanceof URL) {
            return url.equals(obj);
        }
        return false;
    }

    /**
     * @see org.inigma.migrations.MigrationResource#getReader()
     */
    public Reader getReader() {
        try {
            return new InputStreamReader(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException("Resource missing: " + url);
        }
    }

    public long getSize() {
        try {
            return url.openStream().available();
        } catch (IOException e) {
            throw new RuntimeException("Resource missing: " + url);
        }
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
