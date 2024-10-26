package org.thymeleaf.resourceresolver;

import java.io.InputStream;
import java.net.URL;
import org.thymeleaf.Arguments;
import org.thymeleaf.util.Validate;

/**
 * <p>
 *   Implementation of {@link IResourceResolver} that resolves
 *   resources as URLs:
 * </p>
 * <p>
 *   <tt><pre>
 *      try {
 *          final URL url = new URL(resourceName);
 *          return url.openStream();
 *      } catch (final Exception e1) {
 *          return null;
 *      }
 *   </pre></tt>
 * </p>
 * 
 * @author Daniel Fern&aacute;ndez
 * 
 * @since 1.0
 *
 */
public final class UrlResourceResolver implements IResourceResolver {

    public static final String NAME = "URL";

    public UrlResourceResolver() {
        super();
    }

    public String getName() {
        return NAME;
    }

    public InputStream getResourceAsStream(final Arguments arguments, final String resourceName) {
        Validate.notNull(resourceName, "Resource name cannot be null");
        try {
            final URL url = new URL(resourceName);
            return url.openStream();
        } catch (final Exception e1) {
            return null;
        }
    }
}
