package net.sf.lightbound.online;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import net.sf.lightbound.Cacheable;
import net.sf.lightbound.Request;
import net.sf.lightbound.RequestListener;
import net.sf.lightbound.Website;
import net.sf.lightbound.WebsiteContext;
import net.sf.lightbound.components.links.Link;
import net.sf.lightbound.components.links.ListenerActionLink;
import net.sf.lightbound.components.links.MethodActionLink;
import net.sf.lightbound.components.links.PageLink;
import net.sf.lightbound.controller.AssociatedProperties;
import net.sf.lightbound.controller.AssociationTreeStringValueConverter;
import net.sf.lightbound.controller.CachedInterfaceProvider;
import net.sf.lightbound.controller.DefaultPageResolver;
import net.sf.lightbound.controller.DefaultStringValueConverter;
import net.sf.lightbound.controller.DefaultWebsiteContext;
import net.sf.lightbound.controller.DocumentTranslator;
import net.sf.lightbound.controller.InterfaceProvider;
import net.sf.lightbound.controller.InternalPageResolver;
import net.sf.lightbound.controller.InternalRequest;
import net.sf.lightbound.controller.RequestPhase;
import net.sf.lightbound.controller.TranslationHelpers;
import net.sf.lightbound.controller.TranslationProperties;
import net.sf.lightbound.controller.html.HTMLDefaultTagHandlers;
import net.sf.lightbound.events.Event;
import net.sf.lightbound.events.RedirectEvent;
import net.sf.lightbound.events.SessionClosedEvent;
import net.sf.lightbound.events.StatusEvent;
import net.sf.lightbound.exceptions.BeanInterfaceException;
import net.sf.lightbound.exceptions.InitializationException;
import net.sf.lightbound.exceptions.LinkExpiredException;
import net.sf.lightbound.exceptions.OperationNotAllowedException;
import net.sf.lightbound.exceptions.TranslationException;
import net.sf.lightbound.extend.CacheEntry;
import net.sf.lightbound.extend.CacheKey;
import net.sf.lightbound.extend.CachingInfo;
import net.sf.lightbound.extend.CustomizableStringValueConverter;
import net.sf.lightbound.extend.DataSource;
import net.sf.lightbound.extend.DataSourceProvider;
import net.sf.lightbound.extend.PageCache;
import net.sf.lightbound.extend.TagHandlerCollection;
import net.sf.lightbound.util.BeanUtil;
import net.sf.lightbound.util.LightBoundUtil;
import net.sf.lightbound.util.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.io.XMLWriter;

/**
 * The online LightBound web page processor
 * 
 * @author esa
 *
 */
public class LightBound {

    private static final Log LOG = LogFactory.getLog(LightBound.class);

    private static final int TRANSFER_BUF_SIZE = 4096;

    public static final String ANCHOR_URL_START = "#";

    public static final String[] XHTML_CONTENT_TYPES = { "text/html", "application/xhtml+xml", "application/xhtml+xml", "application/vnd.wap.xhtml+xml" };

    public static final Pattern MIME_TYPE_CHARSET_MATCH = Pattern.compile(".*; *charset *= *(.*)");

    public static final int MIME_TYPE_CHARSET_MATCH_GROUP = 1;

    private final LightBoundConfig config;

    private final InternalPageResolver pageResolver;

    private final InterfaceProvider ifaceProvider;

    private final CustomizableStringValueConverter stringValueConverter;

    private final DocumentTranslator htmlTranslator;

    private final WebsiteContext websiteContext;

    private final TagHandlerCollection tagHandlers;

    private final Website website;

    private final ServletContext servletContext;

    private final List<String> xhtmlContentTypes = new ArrayList<String>();

    private final PageCache pageCache = new PageCache();

    public LightBound(LightBoundConfig config, ServletContext servletContext) {
        this(config, servletContext, HTMLDefaultTagHandlers.createInstance());
    }

    public LightBound(LightBoundConfig config, ServletContext servletContext, TagHandlerCollection tagHandlers) {
        this.config = config;
        this.servletContext = servletContext;
        this.tagHandlers = tagHandlers;
        this.xhtmlContentTypes.addAll(Arrays.asList(XHTML_CONTENT_TYPES));
        ifaceProvider = new CachedInterfaceProvider();
        stringValueConverter = new DefaultStringValueConverter();
        DefaultWebsiteContext websiteContextObj = new DefaultWebsiteContext(servletContext, stringValueConverter, tagHandlers, config.getInitParameters());
        websiteContext = websiteContextObj;
        websiteContext.getStringValueConverter().addDelegateConverter(new AssociationTreeStringValueConverter());
        htmlTranslator = new DocumentTranslator();
        Class<? extends Website> websiteClass = config.getWebsiteClass();
        if (websiteClass != null) {
            try {
                website = websiteClass.newInstance();
                BeanUtil.callPrivateSetter(website, Website.class, "setWebsiteContext", WebsiteContext.class, websiteContextObj);
                websiteContextObj.setWebsite(website);
                website.init(websiteContext);
            } catch (InstantiationException e) {
                throw new InitializationException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new InitializationException(e.getMessage(), e);
            } catch (InitializationException e) {
                throw new InitializationException(e.getMessage(), e);
            } catch (BeanInterfaceException e) {
                throw new InitializationException(e.getMessage(), e);
            }
        } else {
            website = null;
        }
        pageResolver = new DefaultPageResolver(website, websiteContext, config.getPageClassPackage(), ifaceProvider, config.getSessionObjectClass());
    }

    public void destroy() {
        pageResolver.onWebsiteDestroy();
        if (website != null) {
            website.destroy();
        }
    }

    /**
   * Runs a user request.
   * 
   * @param request the request
   * @param dataSourceProvider the DataSourceProvider which provides the web
   *  pages
   * @param requestInterface the HTTP request interface to use
   * @param responseInterface the HTTP response interface to use
   * @throws Exception if an exception occurs during the processing
   */
    public void doRequest(InternalRequest request, DataSourceProvider dataSourceProvider, RequestInterface requestInterface, ResponseInterface responseInterface) throws Exception {
        doRequest(request, dataSourceProvider, requestInterface, responseInterface, null, null);
    }

    /**
   * Runs a user request.
   * 
   * @param request the request
   * @param dataSourceProvider the DataSourceProvider which provides the web
   *  pages
   * @param requestInterface the HTTP request interface to use
   * @param responseInterface the HTTP response interface to use
   * @param renderObject the object to associate to the current page
   * @param executedEvent an event to be immediately execute when the processing
   *  begins, or null if no event should be executed
   * @throws Exception if an exception occurs during the processing
   */
    public void doRequest(InternalRequest requestContext, DataSourceProvider dataSourceProvider, RequestInterface requestInterface, ResponseInterface responseInterface, Object renderObject, Event executedEvent) throws Exception {
        while (true) {
            DataSource dataSource = dataSourceProvider.getDataSource(this, requestContext);
            if (dataSource != null) {
                String mimeType = dataSource.getMimeType();
                if (mimeType == null) {
                    mimeType = "text/plain";
                }
                requestContext.setResponseMIMEType(mimeType);
                LOG.debug("Initial mime type: " + requestContext.getResponseMIMEType());
            }
            Request.setCurrent(requestContext);
            try {
                if (executedEvent != null) {
                    Event event = executedEvent;
                    executedEvent = null;
                    throw event;
                }
                if (renderObject == null) {
                    renderObject = pageResolver.peekPageObject(requestContext);
                }
                requestContext.setRequestedPage(renderObject);
                if (website != null) {
                    website.onRequest(requestContext);
                }
                try {
                    if (renderObject != null) {
                        pageResolver.onRequest(requestContext);
                    }
                    if (dataSource != null) {
                        if (renderObject != null) {
                            try {
                                try {
                                    MethodActionLink.callAction(renderObject, requestContext);
                                    ListenerActionLink.callAction(requestContext);
                                } catch (OperationNotAllowedException e) {
                                    Event event = e.getExecutedEvent();
                                    e.printStackTrace();
                                    if (event != null) {
                                        throw event;
                                    }
                                    responseInterface.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                    Writer writer = responseInterface.openWriter();
                                    writer.write("The operation was not allowed");
                                    responseInterface.closeWriter(writer);
                                    return;
                                }
                                serveDynamicFile(requestContext, dataSource, renderObject, responseInterface, dataSourceProvider);
                            } catch (Event event) {
                                throw event;
                            } catch (Exception e) {
                                e.printStackTrace();
                                pageResolver.onException(e, requestContext);
                            } finally {
                                pageResolver.onRequestFinished(requestContext);
                            }
                            Event firedEvent = requestContext.getAfterRequestEvent();
                            if (firedEvent != null) {
                                if (firedEvent instanceof RedirectEvent) {
                                    throw (RedirectEvent) firedEvent;
                                }
                                throw firedEvent;
                            }
                        } else {
                            responseInterface.setMimeType(requestContext.getResponseMIMEType());
                            InputStream in = dataSource.openStream();
                            streamOutput(in, responseInterface);
                            in.close();
                        }
                    } else {
                        LOG.info("Could not serve: '" + requestContext.getPath() + "' (not found from server)");
                        responseInterface.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    }
                    break;
                } catch (Event event) {
                    throw event;
                } catch (Exception exception) {
                    if (website != null) {
                        website.onException(exception, requestContext);
                    } else {
                        throw exception;
                    }
                } finally {
                    if (website != null) {
                        website.onRequestFinished(requestContext);
                    }
                }
            } catch (StatusEvent event) {
                responseInterface.setStatus(event.getStatusCode());
                break;
            } catch (RedirectEvent event) {
                if (event instanceof SessionClosedEvent) {
                    pageResolver.onSessionEnd(requestContext);
                    requestContext.getHttpRequest().getSession().invalidate();
                }
                Link redirectPage = event.getRedirectPage();
                String redirectURL = redirectPage.toURL(requestContext);
                String processedURL = redirectURL;
                if (LightBoundUtil.isRelativeURL(processedURL)) {
                    processedURL = responseInterface.encodeInternalRedirectURL(processedURL);
                }
                if (event.isInlineRedirect()) {
                    requestContext = requestInterface.getRedirectRequest(requestContext, redirectURL, this);
                    if (requestContext != null) {
                        if (redirectPage instanceof PageLink) {
                            PageLink pageLink = (PageLink) redirectPage;
                            Class<?> pageClass = pageLink.getPageClass();
                            try {
                                renderObject = pageResolver.getPageObject(pageClass, requestContext);
                                continue;
                            } catch (InstantiationException e) {
                                throw new TranslationException(e);
                            } catch (IllegalAccessException e) {
                                throw new TranslationException(e);
                            } catch (BeanInterfaceException e) {
                                throw new TranslationException(e);
                            }
                        }
                        URL url = new URL(requestContext.getURL(processedURL));
                        LOG.debug("Redirect inlinely to " + url.toExternalForm());
                        InputStream in = url.openStream();
                        try {
                            streamOutput(in, responseInterface);
                        } finally {
                            LightBoundUtil.closeQuietly(in);
                        }
                    }
                } else {
                    responseInterface.sendRedirect(processedURL);
                }
                return;
            } catch (Event event) {
                throw new TranslationException("unrecognised event", event);
            } catch (LinkExpiredException e) {
                responseInterface.setStatus(HttpServletResponse.SC_OK);
                Writer writer = responseInterface.openWriter();
                writer.write("Sorry, but this link has expired.");
                writer.close();
                return;
            } finally {
                Request.removeCurrent();
            }
        }
    }

    /**
   * Serves a web page, processing the dynamic content
   * 
   * @param request the request to do
   * @param dataSource the DataSource of the current page
   * @param renderObject the object to render the page with
   * @param response the HTTP response interface
   * @param dataSourceProvider the DataSourceProvider which provides the other
   *  pages
   * @throws Event if an event is thrown from the user code
   * @throws Exception if an exception occurs during processing
   */
    public void serveDynamicFile(InternalRequest request, DataSource dataSource, Object renderObject, ResponseInterface response, DataSourceProvider dataSourceProvider) throws Event, Exception {
        LOG.debug("Serving a dynamic file: " + dataSource.getAbsolutePath());
        String requestCachingContext = request.getContentRelativePath();
        Cacheable cacheablePage = null;
        if (renderObject instanceof Cacheable) {
            cacheablePage = (Cacheable) renderObject;
        }
        CacheEntry cacheEntry = getCacheEntry(cacheablePage, requestCachingContext, request);
        if (cacheEntry != null) {
            if (serveCachedPage(cacheEntry, response)) {
                LOG.debug("Served from cache with key: " + cacheEntry.getCacheKey());
                return;
            }
        }
        AssociatedProperties pageClassProps = new AssociatedProperties();
        AssociatedProperties pageProps;
        try {
            if (website != null) {
                AssociatedProperties props = (AssociatedProperties) BeanUtil.callPrivateGetter(website, Website.class, "getAssociatedProperties");
                pageClassProps.addReferencedProperties(props);
            }
            pageProps = pageResolver.getPageData(renderObject.getClass()).getAssociatedProperties();
        } catch (BeanInterfaceException e) {
            throw new TranslationException("can't retrieve associated data", e);
        }
        pageClassProps.addReferencedProperties(pageProps);
        RequestListener page = null;
        if (renderObject instanceof RequestListener) {
            page = (RequestListener) renderObject;
        }
        TranslationProperties translationProps = new TranslationProperties(true);
        request.setPhase(RequestPhase.REWINDING);
        TranslationHelpers helpers = new TranslationHelpers(request, translationProps, stringValueConverter, pageClassProps, new AssociatedProperties(), ifaceProvider, tagHandlers, htmlTranslator);
        htmlTranslator.translate(dataSource, config.getEntityResolvingMethod(), renderObject, helpers);
        request.setPhase(RequestPhase.WEBSITE_RENDERING);
        if (website != null) {
            website.beforePageRender(request);
        }
        request.setPhase(RequestPhase.PAGE_RENDERING);
        if (page != null) {
            pageResolver.beforePageRender(request);
        }
        translationProps = new TranslationProperties(false);
        helpers = new TranslationHelpers(request, translationProps, stringValueConverter, pageClassProps, new AssociatedProperties(), ifaceProvider, tagHandlers, htmlTranslator);
        Document translatedDoc = htmlTranslator.translate(dataSource, config.getEntityResolvingMethod(), renderObject, helpers);
        LOG.debug("Final mime type: " + request.getResponseMIMEType());
        String mimeType = request.getResponseMIMEType();
        String printedDocument = getDocumentAsString(translatedDoc, mimeType);
        CachingInfo cachingInfo = getCachingInfo(cacheablePage, request);
        if (cachingInfo != null) {
            CacheKey cacheKey = getCacheKey(requestCachingContext, cachingInfo);
            cacheEntry = new CacheEntry(cacheKey, cachingInfo, translatedDoc, printedDocument, mimeType);
            pageCache.put(cacheEntry);
            LOG.debug("Added to cache with key: " + cachingInfo);
        }
        writeDocument(response, printedDocument, mimeType);
    }

    public LightBoundConfig getConfig() {
        return config;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public WebsiteContext getWebsiteContext() {
        return websiteContext;
    }

    public InternalPageResolver getPageResolver() {
        return pageResolver;
    }

    public void addXHTMLContentType(String contentType) {
        xhtmlContentTypes.add(contentType);
    }

    public void removeXHTMLContentType(String contentType) {
        xhtmlContentTypes.remove(contentType);
    }

    public Iterable<String> getXHTMLContentTypes() {
        return xhtmlContentTypes;
    }

    public boolean isXHTMLContentType(String contentType) {
        if (xhtmlContentTypes.contains(contentType)) {
            return true;
        }
        for (String xhtmlContentType : xhtmlContentTypes) {
            if (xhtmlContentType.startsWith(contentType)) {
                String theRest = contentType.substring(xhtmlContentType.length());
                theRest = theRest.trim();
                if (theRest.startsWith(";")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void streamOutput(InputStream in, ResponseInterface responseInterface) throws IOException {
        OutputStream out = responseInterface.openStream();
        byte[] buffer = new byte[TRANSFER_BUF_SIZE];
        int readLength;
        while ((readLength = in.read(buffer)) > 0) {
            out.write(buffer, 0, readLength);
        }
        in.close();
        out.close();
    }

    private String getDocumentAsString(Document document, String mimeType) throws IOException {
        XMLWriter xmlWriter;
        if (isXHTMLContentType(mimeType)) {
            xmlWriter = XMLUtil.createXHTMLWriter(true);
        } else {
            xmlWriter = XMLUtil.createGenericWriter(true);
        }
        StringWriter writer = new StringWriter();
        xmlWriter.setWriter(writer);
        try {
            xmlWriter.write(document);
        } finally {
            writer.close();
        }
        return writer.toString();
    }

    private void writeDocument(ResponseInterface response, String document, String mimeType) throws IOException {
        response.setEncoding("utf-8");
        response.setMimeType(mimeType);
        Writer writer = response.openWriter();
        try {
            writer.write(document);
        } finally {
            response.closeWriter(writer);
        }
    }

    private CacheKey getCacheKey(String context, CachingInfo cachingInfo) {
        return cachingInfo.constructSubContextCacheKey(new CacheKey(context, null));
    }

    private boolean serveCachedPage(CacheEntry cacheEntry, ResponseInterface response) throws IOException {
        if (cacheEntry == null) {
            return false;
        }
        String documentPrinted = null;
        if (cacheEntry.getResultPrinted() != null) {
            documentPrinted = cacheEntry.getResultPrinted();
        } else if (cacheEntry.getResultDocument() != null) {
            documentPrinted = getDocumentAsString(cacheEntry.getResultDocument(), cacheEntry.getMimeType());
        }
        if (documentPrinted == null) {
            return false;
        }
        writeDocument(response, cacheEntry.getResultPrinted(), cacheEntry.getMimeType());
        return true;
    }

    private CachingInfo getCachingInfo(Cacheable page, Request request) {
        CachingInfo cachingInfo = null;
        if (page != null) {
            cachingInfo = page.getCachingInfo(request);
        }
        if (cachingInfo == null && website != null) {
            cachingInfo = website.getCachingInfo(request);
        }
        return cachingInfo;
    }

    private CacheEntry getCacheEntry(Cacheable page, String context, Request request) {
        CachingInfo cachingInfo = getCachingInfo(page, request);
        if (cachingInfo == null) {
            return null;
        }
        CacheKey cacheKey = getCacheKey(context, cachingInfo);
        return pageCache.get(cacheKey, cachingInfo);
    }
}
