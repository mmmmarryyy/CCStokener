package com.google.apps.easyconnect.easyrp.client.basic.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Utility class to check whether is a domain is a dasher domain.
 * @author guibinkong@google.com (Guibin Kong)
 */
public class DasherDomainChecker {

    private static final Logger logger = Logger.getLogger(DasherDomainChecker.class.getName());

    private static final String DASHER_XRDS_URL_PREFIX = "https://www.google.com/accounts/" + "o8/site-xrds?hd=";

    private static final String XRDS_MIME_TYPE = "application/xrds+xml";

    private Cache<String, Boolean> cache;

    /**
   * Construct a checker instance.
   * @param cache a cache for domain/isDasher mapping. Use {@code null} if don't want to use cache.
   */
    public DasherDomainChecker(Cache<String, Boolean> cache) {
        this.cache = cache;
    }

    /**
   * Checks if a domain is a dasher domain. We will try to connect
   * 'https://www.google.com/accounts/o8/site-xrds?hd=domain', if an 'application/xrds+xml' content
   * found, then it is a dasher domain. Because discovery is time-consuming, the result will be
   * cached.
   * 
   * @param domain the domain to be checked
   * @return true if it is a dasher domain
   */
    public boolean isDasherDomain(String domain) {
        logger.entering("Utils", "isDasherDomain", domain);
        boolean isDasherDomain = false;
        try {
            Boolean result = cache == null ? null : cache.getIfPresent(domain);
            if (result != null) {
                logger.fine("found domain [" + domain + "] in cache.");
                isDasherDomain = result;
            } else {
                isDasherDomain = checkDasherDomain(domain);
                if (cache != null) {
                    cache.put(domain, isDasherDomain);
                }
            }
        } catch (IOException e) {
            logger.fine("retrieve xrds failed with error: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.fine(e.getMessage());
        }
        logger.fine("isDasherDomain('" + domain + "') return " + isDasherDomain + ".");
        return isDasherDomain;
    }

    /**
   * Checks whether a domain is dasher domain by connecting its XRDS url.
   * @param domain the domain under check
   * @return true if it is a dasher domain, false otherwise
   * @throws IOException when connecting through web
   */
    @VisibleForTesting
    protected static boolean checkDasherDomain(String domain) throws IOException {
        String endpoint = DASHER_XRDS_URL_PREFIX + domain;
        URL url = new URL(endpoint);
        String contentType = ((HttpURLConnection) url.openConnection()).getContentType().toLowerCase();
        logger.fine("retrieve xrds, return contentType='" + contentType + "'");
        return contentType.contains(XRDS_MIME_TYPE);
    }
}
