package com.volantis.mcs.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import com.volantis.mcs.assets.Asset;
import com.volantis.mcs.assets.AssetGroup;
import com.volantis.mcs.assets.SubstantiveAsset;
import com.volantis.mcs.integration.PageURLRewriter;
import com.volantis.mcs.integration.PageURLType;
import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.mcs.policies.BaseURLPolicy;
import com.volantis.mcs.policies.Policy;
import com.volantis.mcs.policies.PolicyType;
import com.volantis.mcs.policies.variants.Variant;
import com.volantis.mcs.policies.variants.content.BaseLocation;
import com.volantis.mcs.policies.variants.content.BaseURLRelative;
import com.volantis.mcs.policies.variants.content.Content;
import com.volantis.mcs.policies.variants.content.EmbeddedContent;
import com.volantis.mcs.policies.variants.metadata.EncodingCollection;
import com.volantis.mcs.protocols.assets.implementation.AssetResolver;
import com.volantis.mcs.repository.RepositoryException;
import com.volantis.mcs.runtime.PageURLDetailsFactory;
import com.volantis.mcs.runtime.RuntimeProject;
import com.volantis.mcs.runtime.policies.ActivatedVariablePolicy;
import com.volantis.mcs.runtime.policies.PolicyFetcher;
import com.volantis.mcs.runtime.policies.PolicyReferenceFactory;
import com.volantis.mcs.runtime.policies.RuntimePolicyReference;
import com.volantis.mcs.runtime.policies.SelectedVariant;
import com.volantis.mcs.runtime.policies.SelectionContext;
import com.volantis.mcs.runtime.policies.base.ActivatedBaseURLPolicy;
import com.volantis.mcs.runtime.selection.VariantSelectionPolicy;
import com.volantis.mcs.utilities.MarinerURL;
import com.volantis.synergetics.log.LogDispatcher;

/**
 * Provides methods to select assets.
 */
public class AssetResolverImpl implements AssetResolver {

    /**
     * Used for logging.
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(AssetResolverImpl.class);

    private final MarinerPageContext context;

    private final SelectionContext selectionContext;

    private final VariantSelectionPolicy selectionPolicy;

    private final PolicyReferenceFactory referenceFactory;

    private final PolicyFetcher policyFetcher;

    private PageURLRewriter pageURLRewriter;

    public AssetResolverImpl(MarinerPageContext context, SelectionContext selectionContext, final VariantSelectionPolicy variantSelectionPolicy, final PageURLRewriter pageURLRewriter, PolicyReferenceFactory referenceFactory) {
        this.context = context;
        this.selectionContext = selectionContext;
        selectionPolicy = variantSelectionPolicy;
        this.referenceFactory = referenceFactory;
        this.policyFetcher = context.getPolicyFetcher();
        this.pageURLRewriter = pageURLRewriter;
    }

    /**
     * Retrieve the URL for an asset as a String
     *
     * @param asset the asset
     * @return the URL as a String
     */
    public String computeURLAsString(Asset asset) {
        MarinerURL url = computeURL(asset);
        String urlStr = null;
        if (url != null) {
            urlStr = url.getExternalForm();
        }
        return urlStr;
    }

    public String getContentsFromVariant(SelectedVariant selected) {
        if (selected == null) {
            return null;
        }
        ActivatedVariablePolicy policy = selected.getPolicy();
        Variant variant = selected.getVariant();
        if (variant == null) {
            return null;
        }
        Content content = variant.getContent();
        if (content instanceof EmbeddedContent) {
            EmbeddedContent embedded = (EmbeddedContent) content;
            return embedded.getData();
        } else {
            MarinerURL marinerURL = computeURL((Asset) selected.getOldObject());
            URL url;
            try {
                url = context.getAbsoluteURL(marinerURL);
            } catch (MalformedURLException e) {
                logger.warn("asset-mariner-url-retrieval-error", new Object[] { policy.getName(), ((marinerURL == null) ? "" : marinerURL.getExternalForm()) }, e);
                return null;
            }
            String text = null;
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Retrieving contents of URL " + url);
                }
                URLConnection connection = url.openConnection();
                int contentLength = connection.getContentLength();
                if (contentLength > 0) {
                    String charset = connection.getContentEncoding();
                    if (charset == null) {
                        charset = "UTF-8";
                    }
                    InputStreamReader is = new InputStreamReader(connection.getInputStream(), charset);
                    BufferedReader br = new BufferedReader(is);
                    char[] buf = new char[contentLength];
                    int length = br.read(buf, 0, buf.length);
                    text = String.copyValueOf(buf, 0, length);
                }
            } catch (IOException e) {
                logger.warn("asset-url-retrieval-error", new Object[] { policy.getName(), url }, e);
            }
            return text;
        }
    }

    public String retrieveVariantURLAsString(RuntimePolicyReference reference, EncodingCollection requiredEncodings) {
        SelectedVariant selectedVariant = selectBestVariant(reference, requiredEncodings);
        return computeURLAsString(selectedVariant);
    }

    public String retrieveVariantURLAsString(SelectedVariant selected) {
        return computeURLAsString(selected);
    }

    /**
     * Retrieve the URL for an asset as a MarinerURL
     *
     * @param asset the asset
     * @return A MarinerURL object
     */
    private MarinerURL computeURL(Asset asset) {
        MarinerURL marinerURL = null;
        if (asset == null) {
            logger.warn("asset-null-no-url");
        } else {
            try {
                ApplicationContext applicationContext = context.getApplicationContext();
                MarinerRequestContext requestContext = context.getRequestContext();
                marinerURL = applicationContext.getAssetURLRewriter().rewriteAssetURL(requestContext, asset, getAssetGroup(asset), new MarinerURL(getAssetValue(asset)));
            } catch (RepositoryException re) {
                logger.warn("repository-exception", re);
            }
        }
        return marinerURL;
    }

    /**
     * Retrive an assets AssetGroup (if any).
     *
     * @param asset the asset
     * @return the associated AssetGroup if one was specified, null otherwise.
     */
    public AssetGroup getAssetGroup(Asset asset) {
        if (asset == null) {
            logger.warn("asset-null-no-value");
            return null;
        }
        if (!(asset instanceof SubstantiveAsset)) {
            return null;
        }
        SubstantiveAsset sAsset = (SubstantiveAsset) asset;
        String assetGroupName = sAsset.getAssetGroupName();
        if (assetGroupName == null) {
            return null;
        }
        RuntimeProject project = (RuntimeProject) sAsset.getAssetGroupProject();
        if (project == null) {
            project = (RuntimeProject) sAsset.getProject();
            if (project == null) {
                project = context.getCurrentProject();
            }
        }
        RuntimePolicyReference reference = referenceFactory.createLazyNormalizedReference(project, new MarinerURL(sAsset.getName()), assetGroupName, PolicyType.BASE_URL);
        Policy policy = policyFetcher.fetchPolicy(reference);
        AssetGroup group = null;
        if (policy == null) {
            logger.warn("asset-group-not-found", new Object[] { reference.getName(), asset.getName() });
        } else {
            ActivatedBaseURLPolicy baseURL = (ActivatedBaseURLPolicy) policy;
            group = new AssetGroup();
            group.setProject(baseURL.getActualProject());
            group.setName(assetGroupName);
            group.setPrefixURL(baseURL.getBaseURL());
            final BaseLocation baseLocation = baseURL.getBaseLocation();
            if (baseLocation == BaseLocation.DEVICE) {
                group.setLocationType(AssetGroup.ON_DEVICE);
            } else if (baseLocation == BaseLocation.CONTEXT) {
                group.setLocationType(AssetGroup.CONTEXT);
            } else if (baseLocation == BaseLocation.HOST) {
                group.setLocationType(AssetGroup.HOST);
            }
        }
        return group;
    }

    public BaseLocation getBaseLocation(SelectedVariant selected) {
        Variant variant = selected.getVariant();
        Content content = variant.getContent();
        if (content instanceof BaseURLRelative) {
            BaseURLRelative relative = (BaseURLRelative) content;
            BaseLocation baseLocation = relative.getBaseLocation();
            if (baseLocation != BaseLocation.DEFAULT) {
                return baseLocation;
            }
            RuntimePolicyReference reference = (RuntimePolicyReference) relative.getBaseURLPolicyReference();
            if (reference != null) {
                BaseURLPolicy policy = (BaseURLPolicy) policyFetcher.fetchPolicy(reference);
                if (policy != null) {
                    baseLocation = policy.getBaseLocation();
                    if (baseLocation != BaseLocation.DEFAULT) {
                        return baseLocation;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrive an assets value (if any).
     *
     * @param asset the asset
     * @return the associated AssetGroup if one was specified, null otherwise.
     */
    private String getAssetValue(Asset asset) {
        if (asset == null) {
            logger.warn("asset-null-no-value");
            return null;
        }
        if (!(asset instanceof SubstantiveAsset)) {
            return null;
        }
        SubstantiveAsset sAsset = (SubstantiveAsset) asset;
        return sAsset.getValue();
    }

    public SelectedVariant selectBestVariant(RuntimePolicyReference reference, EncodingCollection requiredEncodings) {
        return selectionPolicy.retrieveBestObject(selectionContext, reference, requiredEncodings);
    }

    /**
     * Rewrite the given URL string using the Volantis pageURLRewriter.
     *
     * @param urlString   the String representing the url to rewrite
     * @param pageURLType the PageURLType of the url
     * @return the rewritten url.
     */
    public String rewriteURLWithPageURLRewriter(String urlString, final PageURLType pageURLType) {
        if (pageURLRewriter != null) {
            MarinerURL originalURL = new MarinerURL(urlString);
            if (originalURL.getExternalForm().equals(urlString)) {
                MarinerURL rewrittenURL = pageURLRewriter.rewriteURL(context.getRequestContext(), originalURL, PageURLDetailsFactory.createPageURLDetails(pageURLType));
                urlString = rewrittenURL.getExternalForm();
            }
        }
        return urlString;
    }

    public String computeURLAsString(SelectedVariant selectedVariant) {
        if (selectedVariant == null) {
            return null;
        }
        Asset asset = (Asset) selectedVariant.getOldObject();
        return computeURLAsString(asset);
    }
}
