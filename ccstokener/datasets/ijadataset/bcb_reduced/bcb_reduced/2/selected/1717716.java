package net.sf.freecol.common.resources;

import java.net.URI;
import java.net.URL;

/**
 * A <code>Resource</code> wrapping a <code>FAFile</code>.
 *
 * @see Resource
 * @see FAFile
 */
public class FAFileResource extends Resource {

    private FAFile FAFile;

    public FAFileResource(FAFile FAFile) {
        this.FAFile = FAFile;
    }

    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    FAFileResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        URL url = resourceLocator.toURL();
        FAFile = new FAFile(url.openStream());
    }

    /**
     * Preloading is a noop for this resource type.
     */
    public void preload() {
    }

    /**
     * Gets the <code>FAFile</code> represented by this resource.
     *
     * @return The <code>FAFile</code> for this resource, or the default
     *     Java FAFile if none found.
     */
    public FAFile getFAFile() {
        return FAFile;
    }
}
