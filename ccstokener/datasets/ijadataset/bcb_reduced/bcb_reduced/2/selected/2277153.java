package org.bluprint.app.util;

import java.util.Iterator;
import java.util.Vector;
import java.util.HashMap;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.bluprint.data.type.Path;
import org.eclipse.emf.ecore.resource.impl.URIConverterImpl;
import org.eclipse.emf.common.util.URI;

public class URIConverter extends URIConverterImpl {

    private Vector<Path> modulePaths = new Vector<Path>();

    public void addModulePath(Path path) {
        modulePaths.add(path);
    }

    public void removeModulePath(Path path) {
        modulePaths.remove(path);
    }

    public int getModulePathsSize() {
        return modulePaths.size();
    }

    public Path getModulePathAt(int index) {
        return modulePaths.get(index);
    }

    public Iterator<Path> iteratorModulePaths() {
        return modulePaths.iterator();
    }

    private HashMap<URI, URI> normalizedUris = new HashMap<URI, URI>();

    public URI putNormalizedUri(URI key, URI value) {
        return normalizedUris.put(key, value);
    }

    public URI getNormalizedUri(URI key) {
        return normalizedUris.get(key);
    }

    public URI removeNormalizedUri(URI key) {
        return normalizedUris.remove(key);
    }

    public boolean containsNormalizedUriKey(URI key) {
        return normalizedUris.containsKey(key);
    }

    public boolean containsNormalizedUriValue(URI value) {
        return normalizedUris.containsValue(value);
    }

    public void clearNormalizedUris() {
        normalizedUris.clear();
    }

    public int sizeNormalizedUris() {
        return normalizedUris.size();
    }

    public boolean isNormalizedUrisEmpty() {
        return normalizedUris.isEmpty();
    }

    /**
	 * Helper function to add multiple module paths
	 */
    public void modulePaths(String[] paths) {
        for (int i = 0; i < paths.length; i++) {
            this.addModulePath(new Path(paths[i]));
        }
    }

    /**
     * Extend the super class implementation.
     */
    public URI normalize(final URI uri) {
        URI normalizedUri = super.normalize(uri);
        if (normalizedUri.equals(uri)) {
            String resourceName = uri.toString().replaceAll(".*(\\\\+|/)", "");
            if (!containsNormalizedUriKey(uri)) {
                for (Iterator<Path> iterator = this.iteratorModulePaths(); iterator.hasNext(); ) {
                    String searchPath = iterator.next().getPath();
                    String completePath = this.normalizePath(searchPath + '/' + resourceName);
                    try {
                        InputStream stream = null;
                        URL url = toURL(completePath);
                        if (url != null) {
                            try {
                                stream = url.openStream();
                                stream.close();
                            } catch (Exception exception) {
                                url = null;
                            } finally {
                                stream = null;
                            }
                            if (url != null) {
                                normalizedUri = URIUtil.createUri(url.toString());
                                this.putNormalizedUri(uri, normalizedUri);
                                break;
                            }
                        }
                    } catch (Exception exception) {
                    }
                }
            } else {
                normalizedUri = getNormalizedUri(uri);
            }
        }
        return normalizedUri;
    }

    /**
     * The forward slash character.
     */
    private static final String FORWARD_SLASH = "/";

    /**
     * The pattern used for normalizing paths paths with more than one back slash.
     */
    private static final String BACK_SLASH_NORMALIZATION_PATTERN = "\\\\+";

    /**
     * The pattern used for normalizing paths with more than one forward slash.
     */
    private static final String FORWARD_SLASH_NORMALIZATION_PATTERN = FORWARD_SLASH + "+";

    /**
     * Removes any extra path separators and converts all from back slashes to forward slashes.
     *
     * @param path the path to normalize.
     * @return the normalized path
     */
    private String normalizePath(final String path) {
        return path != null ? path.replaceAll(BACK_SLASH_NORMALIZATION_PATTERN, FORWARD_SLASH).replaceAll(FORWARD_SLASH_NORMALIZATION_PATTERN, FORWARD_SLASH) : null;
    }

    /**
     * Attempts to construct a URl from a <code>path</code>.
     *
     * @param path the path from which to construct the URL.
     * @return the constructed URL or null if one couldn't be constructed.
     */
    private URL toURL(String path) {
        URL url = null;
        if (path != null) {
            path = normalizePath(path);
            final File file = new File(path);
            if (file.exists()) {
                try {
                    url = file.toURL();
                } catch (MalformedURLException exception) {
                }
            } else {
                try {
                    url = new URL(path);
                } catch (MalformedURLException exception) {
                }
            }
        }
        return url;
    }
}
