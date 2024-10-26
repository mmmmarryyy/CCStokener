package ru.ksu.niimm.cll.mocassin.crawl.arxiv;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import ru.ksu.niimm.cll.mocassin.util.inject.log.InjectLogger;
import ru.ksu.niimm.cll.mocassin.util.model.ArticleMetadata;
import ru.ksu.niimm.cll.mocassin.util.model.ArticleMetadataReader;
import ru.ksu.niimm.cll.mocassin.util.model.Link;
import ru.ksu.niimm.cll.mocassin.util.model.Link.PdfLinkPredicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ArxivDAOFacadeImpl implements ArxivDAOFacade {

    private static final String USER_AGENT = "Mozilla/4.0";

    private static final String XML_CONTENT_TYPE = "application/xml";

    private static final String TXT_CONTENT_TYPE = "application/txt";

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private static final String SOURCE_PREFIX = "e-print";

    private static final String ABSTRACT_PREFIX = "abs";

    private static final String DEFAULT_CHARSET = "UTF-8";

    private String arxivConnectionUrl;

    private boolean isUseProxy;

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPassword;

    @InjectLogger
    private Logger logger;

    @Inject
    public ArxivDAOFacadeImpl(@Named("arxiv.api.connection.url") String arxivConnectionUrl, @Named("isUseProxy") boolean isUseProxy, @Named("proxy.host") String proxyHost, @Named("proxy.port") int proxyPort, @Named("proxy.user") String proxyUser, @Named("proxy.password") String proxyPassword) {
        this.arxivConnectionUrl = arxivConnectionUrl;
        this.isUseProxy = isUseProxy;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
    }

    @Override
    public ArticleMetadata retrieve(String arxivId) {
        if (arxivId == null) throw new NullPointerException("arxiv id cannot be null");
        try {
            String paramValue = arxivId;
            String query = String.format("id_list=%s", URLEncoder.encode(paramValue, DEFAULT_CHARSET));
            URL url = new URL(this.arxivConnectionUrl + "?" + query);
            InputStream response = loadFromUrl(url, XML_CONTENT_TYPE);
            ArticleMetadata metadata = ArticleMetadataReader.read(response);
            if (isUseProxy) {
                Authenticator.setDefault(null);
            }
            return metadata;
        } catch (Exception e) {
            logger.error("Failed to process the request with arXiv id='{}'", arxivId, e);
            return null;
        }
    }

    @Override
    public InputStream loadSource(ArticleMetadata metadata) {
        String id = metadata.getId();
        String sourceLink = id.replace(ABSTRACT_PREFIX, SOURCE_PREFIX);
        InputStream inputStream = null;
        try {
            URL sourceUrl = new URL(sourceLink);
            inputStream = new GZIPInputStream(loadFromUrl(sourceUrl, TXT_CONTENT_TYPE));
        } catch (MalformedURLException e) {
            logger.error("Failed to prepare a source URL from: {}", sourceLink, e);
        } catch (IOException e) {
            logger.error("Failed to get the source of {}", id, e);
        }
        if (isUseProxy) {
            Authenticator.setDefault(null);
        }
        return inputStream;
    }

    @Override
    public InputStream loadPDF(ArticleMetadata metadata) {
        Link pdfLink = Iterables.find(metadata.getLinks(), new PdfLinkPredicate(), null);
        if (pdfLink == null || (pdfLink.getHref() == null || pdfLink.getHref().length() == 0)) {
            logger.warn("Article metadata with id='{}' doesn't include PDF link; empty stream will be returned", metadata.getId());
            return null;
        }
        InputStream inputStream = null;
        try {
            URL sourceUrl = new URL(pdfLink.getHref());
            inputStream = loadFromUrl(sourceUrl, PDF_CONTENT_TYPE);
        } catch (MalformedURLException e) {
            logger.error("Failed to prepare source URL from: {}", pdfLink.getHref(), e);
        } catch (IOException e) {
            logger.error("Failed to get the source of {}", metadata.getId(), e);
        }
        if (isUseProxy) {
            Authenticator.setDefault(null);
        }
        return inputStream;
    }

    private InputStream loadFromUrl(URL url, String contentType) throws IOException {
        Proxy proxy = null;
        if (isUseProxy) {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                }
            });
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        } else {
            proxy = Proxy.NO_PROXY;
        }
        URLConnection connection = url.openConnection(proxy);
        connection.setRequestProperty("Accept-Charset", DEFAULT_CHARSET);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        InputStream response = connection.getInputStream();
        return response;
    }
}
