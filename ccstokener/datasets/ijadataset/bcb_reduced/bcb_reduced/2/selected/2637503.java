package nl.runnable.solr.xslt.client.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import nl.runnable.solr.xslt.client.Parameters;
import nl.runnable.solr.xslt.client.SolrXmlIndexService;
import nl.runnable.solr.xslt.client.UpdateException;
import nl.runnable.solr.xslt.client.XmlContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Spring-based {@link SolrXmlIndexService} implementation that connects to a remote Solr instance using a
 * {@link ClientHttpRequest}.
 * 
 * @author Laurens Fridael
 * 
 */
@ManagedBean
public class SpringSolrXmlIndexService implements SolrXmlIndexService {

    private ClientHttpRequestFactory clientHttpRequestFactory;

    private String solrUrl;

    /**
	 * The path of the XSLT Update plugin endpoint. This setting depends on how the XsltUpdateHandler has been
	 * configured in solrconfig.xml. Therefore this property must be configured explicitly.
	 */
    private String xsltUpdatePath;

    /** The path of the default Update endpoint. */
    private String defaultUpdatePath = "/update";

    @Override
    public void addContentToIndex(final XmlContent xmlContent) throws IOException {
        Assert.notNull(xmlContent, "XML content cannot be null.");
        addContentToIndex(xmlContent, Parameters.withCommit());
    }

    @Override
    public void addContentToIndex(final XmlContent xmlContent, final Parameters parameters) throws IOException {
        Assert.notNull(xmlContent, "XML content cannot be null.");
        Assert.notNull(parameters, "Parameters cannot be null.");
        final URI uri = createURI(getXsltUpdatePath(), parameters.toMap());
        final ClientHttpRequest request = getClientHttpRequestFactory().createRequest(uri, HttpMethod.POST);
        final String contentType;
        if (StringUtils.hasText(xmlContent.getEncoding())) {
            contentType = String.format("%s; charset=%s", xmlContent.getMimeType(), xmlContent.getEncoding());
        } else {
            contentType = xmlContent.getMimeType();
        }
        request.getHeaders().add("Content-Type", contentType);
        FileCopyUtils.copy(xmlContent.getContentAsStream(), request.getBody());
        performHttpRequest(request);
    }

    /**
	 * Creates an URI with the given path and query parameters.
	 * 
	 * @param path
	 * @param parameters
	 * @return
	 */
    private URI createURI(final String path, final Map<String, String> parameters) {
        Assert.hasText(path, "Path cannot be empty.");
        Assert.notNull(parameters, "Parameters cannot be null.");
        final URI uri;
        if (parameters.isEmpty()) {
            uri = URI.create(String.format("%s%s", getSolrUrl(), path));
        } else {
            final StringBuilder query = new StringBuilder();
            for (final Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); ) {
                final Entry<String, String> param = iterator.next();
                try {
                    query.append(URLEncoder.encode(param.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(param.getValue(), "UTF-8"));
                } catch (final UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                if (iterator.hasNext()) {
                    query.append('&');
                }
            }
            uri = URI.create(String.format("%s%s?%s", getSolrUrl(), path, query));
        }
        return uri;
    }

    /**
	 * Performs the {@link ClientHttpRequest} and the response.
	 * 
	 * @param request
	 * @throws IOException
	 */
    protected void performHttpRequest(final ClientHttpRequest request) throws IOException {
        final ClientHttpResponse response = request.execute();
        final HttpStatus statusCode = response.getStatusCode();
        if (Series.SUCCESSFUL.equals(statusCode.series()) == false) {
            final String message = String.format("Error performing update request. Received HTTP status code %d %s with message: %s", statusCode.value(), statusCode.name(), response.getStatusText());
            throw new UpdateException(message);
        }
    }

    @Override
    public void deleteContentFromIndex(final String uniqueKeyValue) throws IOException, UpdateException {
        Assert.hasText(uniqueKeyValue, "Unique key value cannot be empty.");
        deleteContentFromIndex(uniqueKeyValue, Parameters.withCommit());
    }

    @Override
    public void deleteContentFromIndex(final String uniqueKeyValue, final Parameters parameters) throws IOException, UpdateException {
        final URI uri = createURI(getDefaultUpdatePath(), parameters.toMap());
        final ClientHttpRequest request = getClientHttpRequestFactory().createRequest(uri, HttpMethod.POST);
        request.getHeaders().add("Content-Type", "text/xml; charset=UTF-8");
        FileCopyUtils.copy(createXmlDeleteMessage(uniqueKeyValue, "UTF-8"), request.getBody());
        performHttpRequest(request);
    }

    /**
	 * Creates an XML delete message for the given unique key in the specified encoding and returns an
	 * {@link InputStream} to its raw XML content.
	 * <p>
	 * Due to the relative simplicity of the XML delete message, this implementation writes the literal XML message to
	 * an internal buffer.
	 * 
	 * @param uniqueKeyValue
	 * @param encoding
	 * @return The Input
	 * @throws IOException
	 */
    protected InputStream createXmlDeleteMessage(final String uniqueKeyValue, final String encoding) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final OutputStreamWriter out = new OutputStreamWriter(buffer, encoding);
        out.write("<?xml version=\"1.0\" encoding=\"");
        out.write(encoding);
        out.write("\"?>\n");
        out.write("<delete><id>");
        out.write(uniqueKeyValue);
        out.write("</id></delete>");
        out.flush();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    @Override
    public void commit() throws IOException {
        final Parameters parameters = new Parameters();
        parameters.setCommit(false);
        final URI uri = createURI(getDefaultUpdatePath(), parameters.toMap());
        final ClientHttpRequest request = getClientHttpRequestFactory().createRequest(uri, HttpMethod.POST);
        request.getHeaders().add("Content-Type", "text/xml; charset=UTF-8");
        FileCopyUtils.copy(createXmlCommitMessage("UTF-8"), request.getBody());
        performHttpRequest(request);
    }

    /**
	 * Creates an XML commit message in the specified encoding and returns an {@link InputStream} to its raw XML
	 * content.
	 * 
	 * @param uniqueKeyValue
	 * @param encoding
	 * @return The Input
	 * @throws IOException
	 */
    protected InputStream createXmlCommitMessage(final String encoding) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final OutputStreamWriter out = new OutputStreamWriter(buffer, encoding);
        out.write("<?xml version=\"1.0\" encoding=\"");
        out.write(encoding);
        out.write("\"?>\n");
        out.write("<commit/>");
        out.flush();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    @Override
    public void rollback() throws IOException {
        final Parameters parameters = new Parameters();
        parameters.setCommit(false);
        final URI uri = createURI(getDefaultUpdatePath(), parameters.toMap());
        final ClientHttpRequest request = getClientHttpRequestFactory().createRequest(uri, HttpMethod.POST);
        request.getHeaders().add("Content-Type", "text/xml; charset=UTF-8");
        FileCopyUtils.copy(createXmlRollbackMessage("UTF-8"), request.getBody());
        performHttpRequest(request);
    }

    /**
	 * Creates an XML rollback message in the specified encoding and returns an {@link InputStream} to its raw XML
	 * content.
	 * 
	 * @param uniqueKeyValue
	 * @param encoding
	 * @return The Input
	 * @throws IOException
	 */
    protected InputStream createXmlRollbackMessage(final String encoding) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final OutputStreamWriter out = new OutputStreamWriter(buffer, encoding);
        out.write("<?xml version=\"1.0\" encoding=\"");
        out.write(encoding);
        out.write("\"?>\n");
        out.write("<rollback/>");
        out.flush();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    @Autowired(required = false)
    public void setClientHttpRequestFactory(final ClientHttpRequestFactory clientHttpRequestFactory) {
        Assert.notNull(clientHttpRequestFactory);
        this.clientHttpRequestFactory = clientHttpRequestFactory;
    }

    protected ClientHttpRequestFactory getClientHttpRequestFactory() {
        return clientHttpRequestFactory;
    }

    /**
	 * Initializes dependencies that have not been configured. This implementation instantiates a
	 * {@link SimpleClientHttpRequestFactory} if necessary.
	 * <p>
	 * This method must be called manually in case the runtime environment (i.e. Spring application context) does not
	 * support the {@link PostConstruct} annotation and the {@link ClientHttpRequest} dependency has not been configured
	 * explicitly.
	 */
    @PostConstruct
    protected void initializeDependencies() {
        if (getClientHttpRequestFactory() == null) {
            setClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        }
    }

    @Required
    public void setSolrUrl(String solrUrl) {
        Assert.hasText(solrUrl);
        if (solrUrl.endsWith("/")) {
            solrUrl = solrUrl.substring(0, solrUrl.length() - 1);
        }
        this.solrUrl = solrUrl;
    }

    protected String getSolrUrl() {
        return solrUrl;
    }

    @Required
    public void setXsltUpdatePath(final String updatePath) {
        Assert.hasText(updatePath);
        this.xsltUpdatePath = updatePath;
    }

    protected String getXsltUpdatePath() {
        return xsltUpdatePath;
    }

    public void setDefaultUpdatePath(final String defaultUpdatePath) {
        this.defaultUpdatePath = defaultUpdatePath;
    }

    protected String getDefaultUpdatePath() {
        return defaultUpdatePath;
    }
}
