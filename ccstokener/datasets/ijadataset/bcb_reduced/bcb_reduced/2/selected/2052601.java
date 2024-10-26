package com.volantis.resource;

import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.synergetics.log.LogDispatcher;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Accesses XML policies by resolving them against the URL for the policy root.
 */
public class XMLPoliciesResourceAccessor implements ResourceAccessor {

    /**
     * Used for logging.
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(XMLPoliciesResourceAccessor.class);

    /**
     * The policy root.
     */
    private final URL root;

    /**
     * Initialise.
     *
     * @param root The policy root.
     */
    public XMLPoliciesResourceAccessor(URL root) {
        this.root = root;
    }

    public InputStream getResourceAsStream(String projectRelativePath) {
        try {
            URL url = new URL(root, projectRelativePath);
            return url.openStream();
        } catch (IOException e) {
            logger.error("Cannot find resource for path " + projectRelativePath, e);
            return null;
        }
    }
}
