package org.charvolant.tmsnet.resources;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import com.hp.hpl.jena.util.Locator;
import com.hp.hpl.jena.util.TypedStream;

/**
 * Locate a resource within a JAR.
 * <p>
 * This class does two things:
 * <ul>
 * <li>See whether any URI that starts with a common base can be
 * located within a stack of jar locations.</li>
 * <li>Add an extension onto any bare resource URI. Any resource that
 * has a period as part of its file name is assumed to have an extension.</li>
 * </ul>
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class LocatorJar implements Locator {

    /** The base URI for relocation */
    private String base;

    /** The extension to add, if the resource does not have an extension */
    private String ext;

    /** The stack of relocations which map onto the base */
    private LinkedList<String> relocations;

    /**
   * Construct for a base URI and an extension.
   *
   * @param base The base URI to relocate
   * @param ext The extension to add to bare URIs (null for none) (The period is not required, eg. "rdf")
   */
    public LocatorJar(String base, String ext) {
        this.base = base;
        this.ext = ext;
        this.relocations = new LinkedList<String>();
    }

    /**
   * Push a relocation onto the front of the relocation list.
   * <p>
   * Relocation is performed by supplying an original and relocated
   * pair of uris. The relocation required to move the uri from the
   * base uri to the new location is then added.
   * 
   * @param original The original uri
   * @param relocated The relocated uri
   * 
   * @throws IllegalArgumentException if the original uri does not start with the base uti
   * 
   * @see #appendRelocation(String, String)
   */
    public void pushRelocation(String original, String relocated) throws IllegalArgumentException {
        if (!original.startsWith(this.base)) throw new IllegalArgumentException(original + " must start with " + this.base);
        int tail = original.length() - this.base.length();
        relocated = relocated.substring(0, relocated.length() - tail);
        this.relocations.addFirst(relocated);
    }

    /**
   * Append a relocation onto the end of the relocation list.
   * <p>
   * Relocation is performed by supplying an original and relocated
   * pair of uris. The relocation required to move the uri from the
   * base uri to the new location is then added.
   * 
   * @param original The original uri
   * @param relocated The relocated uri
   * 
   * @throws IllegalArgumentException if the original uri does not start with the base uti
   *
   * @see #pushRelocation(String, String)
   */
    public void appendRelocation(String original, String relocated) throws IllegalArgumentException {
        if (!original.startsWith(this.base)) throw new IllegalArgumentException(original + " must start with " + this.base);
        int tail = original.length() - this.base.length();
        relocated = relocated.substring(0, relocated.length() - tail);
        this.relocations.addFirst(relocated);
    }

    /**
   * Add any extension needed to the URI.
   * 
   * @param uri The uri
   * @return The uri plus the extension
   */
    private String addExt(String uri) {
        int lastSlash;
        if (this.ext == null) return uri;
        lastSlash = uri.lastIndexOf('/');
        return (uri.lastIndexOf('.') <= lastSlash) ? uri = uri + "." + this.ext : uri;
    }

    /**
   * Try to open a uri
   * 
   * @param uri The uri
   * 
   * @return A typed stream, or null for not found
   */
    private TypedStream tryOpen(String uri) {
        try {
            URL url = new URL(uri);
            InputStream is = url.openStream();
            return new TypedStream(is);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
   * {@inheritDoc}
   * <p>
   * If the URI has the base prefix, a search is made through the
   * possible relocations to see whether the file can be found.
   *
   * @param uri The uri
   * 
   * @return An open typed stream for the uri or null for not found.
   * 
   * @see com.hp.hpl.jena.util.Locator#open(java.lang.String)
   */
    @Override
    public TypedStream open(String uri) {
        if (!uri.startsWith(this.base)) {
            return this.tryOpen(uri);
        }
        uri = this.addExt(uri.substring(this.base.length()));
        for (String reloc : this.relocations) {
            String reuri = reloc + uri;
            TypedStream ts = this.tryOpen(reuri);
            if (ts != null) return ts;
        }
        return null;
    }

    /**
   * {@inheritDoc}
   *
   * @return The class name of this locator
   * 
   * @see com.hp.hpl.jena.util.Locator#getName()
   */
    @Override
    public String getName() {
        return this.getClass().getName();
    }
}
