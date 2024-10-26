package com.linguamathematica.translate4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.String.format;
import static org.apache.http.client.utils.URIUtils.createURI;
import static com.linguamathematica.translate4j.Base.ensureResponse;
import static com.linguamathematica.translate4j.Fluency.afterAborting;
import static com.linguamathematica.translate4j.Fluency.ifFailureThenReportAbout;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;

class HTTPService {

    private static final String NO_FRAGMENT = null;

    private static final int EMPTY = 0;

    private static final int MAX_EXPECTED_CONTENT_LENGTH = 2048;

    private static final Logger log = Logger.getLogger(HTTPService.class);

    private final HttpClient client;

    private final String path;

    private final String host;

    private final String protocol;

    private final int port;

    HTTPService(final String protocol, final String host, final int port, final String path, final HttpClient client) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.client = client;
    }

    String query(final String query) {
        HttpGet request = null;
        try {
            request = buildRequest(query, protocol, host, port, path);
            final HttpResponse response = execute(request);
            final StatusLine status = response.getStatusLine();
            final String responseBody = extractStringFrom(response);
            ensureResponse(status.getStatusCode() == HttpStatus.SC_OK, format("Server responded with %s to request %s", responseBody, request.getURI()), status.getStatusCode(), status.getReasonPhrase());
            return responseBody;
        } catch (final RuntimeException anyException) {
            rethrow(anyException, afterAborting(request));
        }
        return null;
    }

    void shutDown() {
        client.getConnectionManager().shutdown();
    }

    private HttpResponse execute(final HttpGet GET) {
        log.debug(format("executing request %s...", GET.getURI()));
        try {
            return client.execute(GET);
        } catch (final ClientProtocolException exception) {
            throw new RequestException(format("Error using protocol while requesting %s. Report as bug", GET), exception);
        } catch (final IOException exception) {
            throw new ConnectionException(format("Error connecting to service while requesting %s", GET.getURI()), exception);
        }
    }

    private static HttpGet buildRequest(final String query, final String protocol, final String host, final int port, final String path) {
        log.debug(format("creating request with URI [%s]:[%s]:[%s]:[%s]...", protocol, host, path, query));
        try {
            return new HttpGet(createURI(protocol, host, port, path, query, NO_FRAGMENT));
        } catch (final Exception exception) {
            throw new RequestException(format("Error whilst building URI [%s]:[%s]:[%s]:[%s]. Report as bug", protocol, host, path, query), exception);
        }
    }

    private static void close(final InputStream content, final StatusLine statusLine) {
        if (content != null) {
            try {
                content.close();
            } catch (final IOException exception) {
                throw new ResponseException("Unable to close response stream", statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
        }
    }

    private static boolean contentSizeIsValid(final HttpEntity entity) {
        return entity.getContentLength() != EMPTY && entity.getContentLength() <= MAX_EXPECTED_CONTENT_LENGTH;
    }

    private static String extractStringFrom(final HttpResponse response) {
        final HttpEntity entity = response.getEntity();
        final StatusLine status = response.getStatusLine();
        InputStream stream = null;
        ensureResponse(contentSizeIsValid(entity), format("Response content has unexpected length [%s]", entity.getContentLength()), status.getStatusCode(), status.getReasonPhrase());
        try {
            stream = entity.getContent();
        } catch (final Exception exception) {
            throw new ResponseException("Error while opening response stream", status.getStatusCode(), status.getReasonPhrase(), exception);
        }
        return readStringFrom(stream, ifFailureThenReportAbout(status));
    }

    private static boolean isValid(final String response) {
        return response != null && response.length() != 0;
    }

    private static String readStringFrom(final InputStream stream, final StatusLine status) {
        try {
            final String response = new BufferedReader(new InputStreamReader(stream), MAX_EXPECTED_CONTENT_LENGTH).readLine();
            ensureResponse(isValid(response), format("Response content was invalid, got this [%s]", response), status.getStatusCode(), status.getReasonPhrase());
            return response;
        } catch (final IOException exception) {
            throw new ResponseException("Error while reading response stream", status.getStatusCode(), status.getReasonPhrase());
        } finally {
            close(stream, ifFailureThenReportAbout(status));
        }
    }

    private static void rethrow(final RuntimeException exception, final HttpGet GET) {
        if (GET != null) {
            GET.abort();
        }
        throw exception;
    }
}
