package org.apache.shindig.gadgets.templates;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.w3c.dom.Element;
import com.google.inject.Inject;

/**
 * Factory for template libraries.
 */
public class TemplateLibraryFactory {

    private static final String PARSED_XML_CACHE = "parsedXml";

    private final RequestPipeline pipeline;

    private final Cache<String, Element> parsedXmlCache;

    @Inject
    public TemplateLibraryFactory(RequestPipeline pipeline, CacheProvider cacheProvider) {
        this.pipeline = pipeline;
        if (cacheProvider == null) {
            this.parsedXmlCache = null;
        } else {
            this.parsedXmlCache = cacheProvider.createCache(PARSED_XML_CACHE);
        }
    }

    public TemplateLibrary loadTemplateLibrary(GadgetContext context, Uri uri) throws GadgetException {
        HttpRequest request = new HttpRequest(uri);
        request.setCacheTtl(300);
        HttpResponse response = pipeline.execute(request);
        if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
            throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, "Unable to retrieve template library xml. HTTP error " + response.getHttpStatusCode());
        }
        String content = response.getResponseAsString();
        try {
            String key = null;
            Element element = null;
            if (!context.getIgnoreCache()) {
                key = HashUtil.rawChecksum(content.getBytes());
                element = parsedXmlCache.getElement(key);
            }
            if (element == null) {
                element = XmlUtil.parse(content);
                if (key != null) {
                    parsedXmlCache.addElement(key, element);
                }
            }
            return new XmlTemplateLibrary(uri, element, content);
        } catch (XmlException e) {
            throw new GadgetException(GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
        }
    }
}
