package com.volantis.mcs.runtime.packagers.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.charset.CharsetEncodingWriter;
import com.volantis.charset.Encoding;
import com.volantis.mcs.assets.Asset;
import com.volantis.mcs.assets.AssetGroup;
import com.volantis.mcs.assets.LinkAsset;
import com.volantis.mcs.assets.ImageAsset;
import com.volantis.mcs.context.ApplicationContext;
import com.volantis.mcs.context.ContextInternals;
import com.volantis.mcs.context.MarinerContextException;
import com.volantis.mcs.context.MarinerPageContext;
import com.volantis.mcs.context.MarinerRequestContext;
import com.volantis.mcs.context.MarinerSessionContext;
import com.volantis.mcs.integration.AssetURLRewriter;
import com.volantis.mcs.repository.RepositoryException;
import com.volantis.mcs.runtime.packagers.AbstractPackageBodyOutput;
import com.volantis.mcs.runtime.packagers.PackageBodySource;
import com.volantis.mcs.runtime.packagers.PackageResources;
import com.volantis.mcs.runtime.packagers.Packager;
import com.volantis.mcs.runtime.packagers.PackagingException;
import com.volantis.mcs.runtime.packagers.PackagedURLEncoder;
import com.volantis.mcs.utilities.MarinerURL;

/**
 * This implementation of {@link Packager} provides packaging into a MIME
 * multi-part response, taking anticipated device caching of asset resources
 * into account.
 * <p>Note that this class also implements the {@link AssetURLRewriter} and is
 * expected to be registered as both the Packager and the AssetURLRewriter in
 * the ApplicationContent. This allows the handling of URL mapping and the
 * packaging using these URLs to be performed in one place.</p>
 *
 * @author <a href="mailto:phil.weighill-smith@volantis.com">Phil W-S</a>
 */
public class MultipartPackageHandler implements AssetURLRewriter, Packager, PackagedURLEncoder {

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(MultipartPackageHandler.class);

    /**
     * Used to retrieve localized exception messages.
     */
    private static final ExceptionLocalizer exceptionLocalizer = LocalizationFactory.createExceptionLocalizer(MultipartPackageHandler.class);

    /**
     * The optional AssetURLRewriter that should be invoked against the
     * given asset URL before this class performs any URL rewriting.
     *
     * @supplierRole preRewriter
     * @supplierCardinality 0..1
     */
    protected AssetURLRewriter preRewriter = null;

    /**
     * Initializes the new instance using the given parameters. The preRewriter
     * is set null.
     */
    public MultipartPackageHandler() {
        this(null);
    }

    /**
     * Initializes the new instance using the given parameters.
     */
    public MultipartPackageHandler(AssetURLRewriter preRewriter) {
        this.preRewriter = preRewriter;
    }

    /**
     * This method is invoked whenever an Asset is to be processed during
     * page generation when the request indicates that MIME multi-part
     * packaging is supported and has been requested. The processing must
     * include:
     * <ol>
     *   <li>invoking the (optional) pre-rewriter to perform any initial URL
     * rewriting needed</li>
     *   <li>mapping of the re-written URL to a "package" URL, dependent on
     * the relevant device policy settings and the type of asset</li>
     *   <li>recording the mapping from original "plain" URL to "encoded" URL
     * for later use</li>
     * <ol>
     */
    public MarinerURL rewriteAssetURL(MarinerRequestContext requestContext, Asset asset, AssetGroup assetGroup, MarinerURL marinerURL) throws RepositoryException {
        MarinerURL rewritten = marinerURL;
        if (preRewriter != null) {
            rewritten = preRewriter.rewriteAssetURL(requestContext, asset, assetGroup, marinerURL);
        }
        if (!(asset instanceof LinkAsset)) {
            String plain = rewritten.getExternalForm();
            String encoded = getEncodedURI(plain);
            ApplicationContext ac = ContextInternals.getApplicationContext(requestContext);
            PackageResources pr = ac.getPackageResources();
            if (pr != null) {
                boolean onClientSide = false;
                if (assetGroup != null) {
                    onClientSide = (assetGroup.getLocationType() == AssetGroup.ON_DEVICE);
                }
                if (!onClientSide && asset instanceof ImageAsset) {
                    onClientSide = ((ImageAsset) asset).isLocalSrc();
                }
                PackageResources.Asset prAsset = new PackageResources.Asset(plain, onClientSide);
                pr.addAssetURLMapping(encoded, prAsset);
                rewritten = new MarinerURL(encoded);
            } else {
                throw new NullPointerException("PackageResources must be set in the application " + "context when using " + getClass().getName());
            }
        }
        return rewritten;
    }

    public String getEncodedURI(String plain) {
        return plain;
    }

    public void createPackage(MarinerRequestContext context, PackageBodySource bodySource, Object bodyContext) throws PackagingException {
        MarinerPageContext pageContext = ContextInternals.getMarinerPageContext(context);
        MimeMultipart pkg = new MimeMultipart();
        addBody(pkg, bodySource, context, bodyContext);
        addAssetResources(pkg, pageContext);
        outputPackage(pkg, pageContext);
    }

    /**
     * The body source's content is added as a body part to the given multipart
     * package.
     *
     * @param pkg            the multipart package to which the body content
     *                       should be added
     * @param bodySource     the body source who's content is to be added
     * @param requestContext the request context
     * @param bodyContext    a contextual object relevant to the body source
     *                       content generation
     * @throws PackagingException if a problem is encountered
     */
    protected void addBody(MimeMultipart pkg, PackageBodySource bodySource, final MarinerRequestContext requestContext, Object bodyContext) throws PackagingException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            BodyPart bodyPart = new MimeBodyPart();
            String charEncoding = requestContext.getCharacterEncoding();
            final Writer writer = new OutputStreamWriter(stream, charEncoding);
            bodySource.write(new AbstractPackageBodyOutput() {

                public OutputStream getRealOutputStream() {
                    return stream;
                }

                public Writer getRealWriter() {
                    MarinerPageContext pageContext = ContextInternals.getMarinerPageContext(requestContext);
                    Encoding charsetEncoding = pageContext.getCharsetEncoding();
                    if (charsetEncoding == null) {
                        throw new RuntimeException("No charset found, unable to generate page");
                    }
                    return new CharsetEncodingWriter(writer, charsetEncoding);
                }
            }, requestContext, bodyContext);
            writer.close();
            String bodyContentType = bodySource.getBodyType(requestContext);
            String charsetParam = ";charset=";
            StringBuffer contentTypeBuffer = new StringBuffer(bodyContentType.length() + charsetParam.length() + charEncoding.length());
            contentTypeBuffer.append(bodyContentType).append(charsetParam).append(charEncoding);
            String contentType = contentTypeBuffer.toString();
            bodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(stream.toByteArray(), contentType)));
            pkg.addBodyPart(bodyPart);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(exceptionLocalizer.format("unexpected-encoding"), e);
        } catch (MessagingException e) {
            throw new PackagingException(exceptionLocalizer.format("problem-adding-body"), e);
        } catch (IOException e) {
            throw new PackagingException(exceptionLocalizer.format("problem-adding-body"), e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unexpected problem releasing body stream resources: " + e);
                }
            }
        }
    }

    /**
     * All relevant assets are added to the package (unless they are deemed
     * to already be cached by the device).
     *
     * @param pkg the multipart package to which the assets are added
     * @param context the page context
     * @throws PackagingException if an error occurs while packaging
     */
    protected void addAssetResources(MimeMultipart pkg, MarinerPageContext context) throws PackagingException {
        boolean includeFullyQualifiedURLs = context.getBooleanDevicePolicyValue("protocol.mime.fully.qualified.urls");
        MarinerRequestContext requestContext = context.getRequestContext();
        ApplicationContext ac = ContextInternals.getApplicationContext(requestContext);
        PackageResources pr = ac.getPackageResources();
        List encodedURLs = pr.getEncodedURLs();
        Map assetURLMap = pr.getAssetURLMap();
        Iterator iterator;
        String encodedURL;
        PackageResources.Asset asset;
        String assetURL = null;
        BodyPart assetPart;
        if (encodedURLs != null) {
            iterator = encodedURLs.iterator();
        } else {
            iterator = assetURLMap.keySet().iterator();
        }
        while (iterator.hasNext()) {
            encodedURL = (String) iterator.next();
            asset = (PackageResources.Asset) assetURLMap.get(encodedURL);
            assetURL = asset.getValue();
            if (includeFullyQualifiedURLs || !isFullyQualifiedURL(assetURL)) {
                if (isToBeAdded(assetURL, context)) {
                    assetPart = new MimeBodyPart();
                    try {
                        if (!asset.getOnClientSide()) {
                            URL url = null;
                            URLConnection connection;
                            try {
                                url = context.getAbsoluteURL(new MarinerURL(assetURL));
                                connection = url.openConnection();
                                if (connection != null) {
                                    connection.setDoInput(true);
                                    connection.setDoOutput(false);
                                    connection.setAllowUserInteraction(false);
                                    connection.connect();
                                    connection.getInputStream();
                                    assetPart.setDataHandler(new DataHandler(url));
                                    assetPart.setHeader("Content-Location", assetURL);
                                    pkg.addBodyPart(assetPart);
                                }
                            } catch (MalformedURLException e) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Ignoring asset with malformed URL: " + url.toString());
                                }
                            } catch (IOException e) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Ignoring asset with URL that doesn't " + "exist: " + assetURL + " (" + url.toString() + ")");
                                }
                            }
                        } else {
                            assetPart.setHeader("Content-Location", "file://" + assetURL);
                        }
                    } catch (MessagingException e) {
                        throw new PackagingException(exceptionLocalizer.format("could-not-add-asset", encodedURL), e);
                    }
                }
            }
        }
    }

    /**
     * The package content is completed, a message generated containing it,
     * MIME headers corrected and the package output to the response.
     *
     * @param pkg the multipart package
     * @param context the page context
     * @throws PackagingException if a problem is encountered
     * @todo later if this packager is to be applied to other devices, the
     *       set of "erroneous" headers should probably be handled via some
     *       policy values in the device
     */
    protected void outputPackage(MimeMultipart pkg, MarinerPageContext context) throws PackagingException {
        MarinerRequestContext requestContext = context.getRequestContext();
        ApplicationContext ac = ContextInternals.getApplicationContext(requestContext);
        PackageResources pr = ac.getPackageResources();
        OutputStream outputStream = null;
        try {
            Message message = new MimeMessage(Session.getInstance(System.getProperties(), null));
            String messageContentType;
            message.setContent(pkg);
            message.saveChanges();
            for (int i = 0; i < pkg.getCount(); i++) {
                BodyPart part = pkg.getBodyPart(i);
                part.removeHeader("Content-Transfer-Encoding");
            }
            messageContentType = message.getContentType();
            message.removeHeader("Message-ID");
            message.removeHeader("Mime-Version");
            message.removeHeader("Content-Type");
            messageContentType = pr.getContentType() + messageContentType.substring(messageContentType.indexOf(';'));
            context.getEnvironmentContext().setContentType(messageContentType);
            try {
                outputStream = context.getEnvironmentContext().getResponseOutputStream();
                message.writeTo(outputStream);
                outputStream.flush();
            } catch (MarinerContextException e) {
                throw new PackagingException(exceptionLocalizer.format("no-response-stream"), e);
            }
        } catch (MessagingException e) {
            throw new PackagingException(exceptionLocalizer.format("message-writing-finalizing-error"), e);
        } catch (IOException e) {
            throw new PackagingException(exceptionLocalizer.format("response-writer-problem"), e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Determines whether the given asset should be added to the package,
     * based on whether the device is likely to have already cached it from
     * a previous response. This method has the side-effect of updating
     * Mariner's idea of what the device cache contains.
     *
     * @param assetURL the URL of the asset to be added
     * @param context the page context
     * @return true if the asset should be added to the package
     */
    protected boolean isToBeAdded(String assetURL, MarinerPageContext context) {
        MarinerSessionContext sc = context.getSessionContext();
        if (sc.getDeviceAssetCacheMaxSize() == 0) {
            int cacheSize = 0;
            String cacheSizeAsString = context.getDevicePolicyValue("protocol.mime.urls.to.cache");
            if (cacheSizeAsString != null) {
                try {
                    cacheSize = Integer.valueOf(cacheSizeAsString).intValue();
                } catch (NumberFormatException e) {
                    cacheSize = 0;
                }
            }
            if (cacheSize == 0) {
                sc.setDeviceAssetCacheMaxSize(-1);
            } else {
                sc.setDeviceAssetCacheMaxSize(cacheSize);
            }
        }
        return !sc.isAssetCached(assetURL);
    }

    /**
     * Returns true if the given URL is fully qualified. To be fully qualified
     * a URL must have a scheme (or "protocol") prefix.
     *
     * @param url the URL to be checked
     * @return true if the url is fully qualified
     */
    protected boolean isFullyQualifiedURL(String url) {
        MarinerURL theURL = new MarinerURL(url);
        return (theURL.getProtocol() != null);
    }
}
