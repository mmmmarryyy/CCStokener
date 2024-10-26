package com.dyuproject.ioc;

import java.io.IOException;
import java.net.URL;

/**
 *  A resolver that resolves a resource by opening the stream to the {@link URL}.
 * 
 * @author David Yu
 * @created Feb 23, 2009
 */
public final class URLResolver extends AbstractResolver {

    /**
     * The type of this resolver. ("url")
     */
    public static final String TYPE = generateTypeFromClass(URLResolver.class);

    /**
     * The default instance.
     */
    public static final URLResolver DEFAULT = new URLResolver();

    /**
     * Gets the default instance.
     */
    public static URLResolver getDefault() {
        return DEFAULT;
    }

    public URLResolver() {
    }

    public String getType() {
        return TYPE;
    }

    public void resolve(Resource resource, Context context) throws IOException {
        resource.resolve(newReader(new URL(resource.getPath()).openStream()), getType());
    }

    public Resource createResource(String path) throws IOException {
        return new Resource(path, getType(), newReader(new URL(path).openStream()));
    }

    /**
     * Creates a resource from a given {@code url}.
     */
    public Resource createResource(URL url) throws IOException {
        return new Resource(url.toString(), getType(), newReader(url.openStream()));
    }
}
