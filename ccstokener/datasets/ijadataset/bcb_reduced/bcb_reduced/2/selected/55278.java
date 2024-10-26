package org.oclc.da.ndiipp.extensions.heritrix;

import java.net.URL;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.apache.commons.httpclient.URIException;

/**
  * CandidateURIUtils
  *
  * This class overrides the Heritrix PathDepthFilter. It solves the issue of
  * the Heritrix filter not downloading dependencies.
  * 
  * @author JCG
  * @version 1.0, 
  * @created 08/24/2006
  */
public class CandidateURIUtils {

    /** Embed symbol for Heritrix download path (trail). This shows the type 
     * of links that occurred to reach the current content file. An embed is a
     * dependency, essentially. */
    public static final String PATH_EMBED = "E";

    /** Speculative Embed symbol for Heritrix download path (trail). A 
     * speculative embed is typically a javascript link that Heritrix thinks 
     * could be an embed. */
    public static final String PATH_SPEC_EMBED = "X";

    /** Link (e.g. Hyperlink) symbol for Heritrix download path (trail).*/
    public static final String PATH_LINK = "L";

    /** Redirect symbol for Heritrix download path (trail).*/
    public static final String PATH_REDIRECT = "R";

    /** Content type attribute name for cnadidate URI.*/
    private static final String SPEC_CONTENT_TYPE = "SpecContentType";

    /** Unknown content type constant.*/
    private static final String UNKNOWN_TYPE = "unknown/unknown";

    /** Candidate URI. */
    private CandidateURI candURI = null;

    /** Link trail for the URI. */
    private String linkTrail = null;

    /**
     * Construct a new candidate URI utilities object. 
     * @param candURI supplied cadidate uri
     */
    public CandidateURIUtils(CandidateURI candURI) {
        this.candURI = candURI;
        setupLinkTrail();
    }

    /**
     * Determine if the candidate URI is a strict embed (e.g. image).
     * Speculative embeds are not considered strict.
     * <p>
     * @return <code>true</code> if URI is a strict embed,
     *         <code>false</code> otherwise.
     */
    public boolean isEmbed() {
        return (linkTrail.endsWith(PATH_EMBED));
    }

    /**
     * Determine if the candidate URI is a link.
     * <p>
     * @return <code>true</code> if URI is a link,
     *         <code>false</code> otherwise.
     */
    public boolean isLink() {
        return (linkTrail.endsWith(PATH_LINK));
    }

    /**
     * Determine if the candidate URI is a speculative embed.
     * A speculative embed is a link that heritrix has extracted from javascript
     * that could likely be an embed.
     * <p>
     * @return <code>true</code> if URI is a speculative embed,
     *         <code>false</code> otherwise.
     */
    public boolean isSpecEmbed() {
        return (linkTrail.endsWith(PATH_SPEC_EMBED));
    }

    /**
     * Get the content type from the candidate URI. 
     * <p>
     * @return The content type or "unknown/unknown" if unknown.
     */
    public String getContentType() {
        String contType = UNKNOWN_TYPE;
        if (candURI instanceof CrawlURI) {
            contType = ((CrawlURI) candURI).getContentType();
        }
        if (contType == null) {
            if (candURI.containsKey(SPEC_CONTENT_TYPE)) {
                contType = candURI.getString(SPEC_CONTENT_TYPE);
            } else {
                String uri = candURI.getUURI().toString();
                try {
                    URL url = new URL(uri);
                    contType = url.openConnection().getContentType();
                } catch (Exception e) {
                    e.printStackTrace();
                    contType = UNKNOWN_TYPE;
                }
                contType = (contType != null) ? contType : UNKNOWN_TYPE;
                candURI.putString(SPEC_CONTENT_TYPE, contType);
            }
        }
        return contType;
    }

    /**
     * Get the host from the candidate URI. 
     * <p>
     * @return The host of the candidate URI. Returns <code>null</code>
     *         if the host cannot be obtained.
     */
    public String getHost() {
        try {
            return candURI.getUURI().getHost();
        } catch (URIException e) {
            return null;
        }
    }

    /**
     * Setup the link trail attribute. 
     * Handles null and removes trailing redirects. 
     */
    private void setupLinkTrail() {
        String trail = candURI.getPathFromSeed();
        if (trail != null) {
            while (trail.endsWith(PATH_REDIRECT)) {
                trail = trail.substring(0, trail.length() - 1);
            }
        }
        linkTrail = (trail != null) ? trail : "";
    }
}
