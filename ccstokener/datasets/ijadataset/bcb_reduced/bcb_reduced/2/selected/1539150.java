package net.deytan.wofee.gae.service.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.deytan.wofee.exception.HttpException;
import net.deytan.wofee.persistable.FetchInfos;
import net.deytan.wofee.persistable.FetchInfos.FETCHING_RESULT;
import net.deytan.wofee.service.HttpService;
import org.apache.commons.digester.Digester;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

public class HttpServiceImpl implements HttpService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServiceImpl.class);

    private String dateFormat;

    private SimpleDateFormat dateFormater;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.dateFormater = new SimpleDateFormat(this.dateFormat);
    }

    public InputStream fetch(final FetchInfos fetchInfos) throws HttpException {
        URL url = null;
        try {
            url = new URL(fetchInfos.getUri());
        } catch (MalformedURLException exception) {
            throw new HttpException("what the fuck '" + fetchInfos.getUri() + "'", exception);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException exception) {
            fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            throw new HttpException("fetch '" + fetchInfos.getUri() + "' failed", exception);
        }
        if (fetchInfos.getETag() != null) {
            connection.setRequestProperty("If-None-Match", fetchInfos.getETag());
        }
        if (fetchInfos.getLastFetch() != null) {
            synchronized (this.dateFormater) {
                final String lastUpdate = this.dateFormater.format(fetchInfos.getLastFetch());
                connection.setRequestProperty("If-Modified-Since", lastUpdate);
            }
        }
        if (fetchInfos.isDeltaEncoding()) {
            connection.setRequestProperty("A-IM", "feed");
        }
        InputStream input = null;
        try {
            connection.connect();
            fetchInfos.setLastFetch(new Date());
            final int responseCode = connection.getResponseCode();
            if ((responseCode >= 200) && (responseCode < 300)) {
                fetchInfos.setResult(FETCHING_RESULT.OK);
            } else if ((responseCode >= 300) && (responseCode < 400)) {
                if (responseCode == 304) {
                    fetchInfos.setResult(FETCHING_RESULT.NOT_MODIFIED);
                } else {
                    fetchInfos.setResult(FETCHING_RESULT.OK);
                }
            } else if ((responseCode >= 400) && (responseCode < 500)) {
                fetchInfos.setResult(FETCHING_RESULT.PATH_ERROR);
            } else if ((responseCode >= 500) && (responseCode < 600)) {
                fetchInfos.setResult(FETCHING_RESULT.SITE_ERROR);
            } else {
                fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            }
            fetchInfos.setLastSiteAnswer(connection.getResponseCode() + "-" + connection.getResponseMessage());
            if (FETCHING_RESULT.OK.equals(fetchInfos.getResult())) {
                input = connection.getInputStream();
                if ("gzip".equals(connection.getHeaderField("content-encoding"))) {
                    input = new GZIPInputStream(input);
                }
                if (HttpServiceImpl.LOGGER.isDebugEnabled()) {
                    this.logConnection(connection);
                    input = new LoggingInputStream(input);
                }
            }
        } catch (SocketTimeoutException exception) {
            fetchInfos.setResult(FETCHING_RESULT.TIME_OUT);
            throw new HttpException("fetch '" + fetchInfos.getUri() + "' timeout", exception);
        } catch (IOException exception) {
            fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            throw new HttpException("fetch '" + fetchInfos.getUri() + "' failed", exception);
        }
        if (connection.getHeaderField("ETag") != null) {
            fetchInfos.setETag(connection.getHeaderField("ETag"));
        }
        if (connection.getLastModified() != 0) {
            fetchInfos.setLastResourceModification(new Date(connection.getLastModified()));
        }
        if ((connection.getHeaderField("IM") != null) && connection.getHeaderField("IM").contains("feed")) {
            fetchInfos.setDeltaEncoding(true);
        }
        if ((connection.getHeaderField("Cache-Control") != null) && connection.getHeaderField("Cache-Control").contains("max-age")) {
            final String maxAge = this.explodeHeader(connection.getHeaderField("Cache-Control")).get("max-age");
            try {
                fetchInfos.setUpdateInterval(new Duration(Integer.parseInt(maxAge) * 1000));
            } catch (NumberFormatException exception) {
            }
        }
        return input;
    }

    public String getFavIconURI(final String wwwUri) throws HttpException {
        final FavIconHtmlParser htmlParser = new FavIconHtmlParser();
        final Digester digester = new Digester();
        digester.push(htmlParser);
        digester.addCallMethod("head/link", "setPath", 2);
        digester.addCallParam("head/link", 0, "rel");
        digester.addCallParam("head/link", 1, "href");
        try {
            digester.parse(this.simpleFetch(wwwUri));
        } catch (IOException exception) {
            throw new HttpException("Reading " + wwwUri + " failed", exception);
        } catch (SAXException exception) {
            throw new HttpException("Reading " + wwwUri + " failed", exception);
        }
        return htmlParser.getPath();
    }

    @Override
    public String get(final FetchInfos fetchInfos) throws HttpException {
        URL url = null;
        try {
            url = new URL(fetchInfos.getUri());
        } catch (MalformedURLException exception) {
            throw new HttpException("uri is malformed '" + fetchInfos.getUri() + "'", exception);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException exception) {
            fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            throw new HttpException("get '" + fetchInfos.getUri() + "' failed", exception);
        }
        InputStream input = null;
        try {
            connection.setRequestMethod("GET");
            connection.connect();
            input = connection.getInputStream();
            if ("gzip".equals(connection.getHeaderField("content-encoding"))) {
                input = new GZIPInputStream(input);
            }
            if (HttpServiceImpl.LOGGER.isDebugEnabled()) {
                this.logConnection(connection);
                input = new LoggingInputStream(input);
            }
        } catch (SocketTimeoutException exception) {
            fetchInfos.setResult(FETCHING_RESULT.TIME_OUT);
            throw new HttpException("get '" + fetchInfos.getUri() + "' timeout", exception);
        } catch (IOException exception) {
            fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            throw new HttpException("get '" + fetchInfos.getUri() + "' failed", exception);
        }
        fetchInfos.setResult(FETCHING_RESULT.OK);
        String response = null;
        try {
            response = this.toString(input);
        } catch (IOException exception) {
            throw new HttpException("converting inputstream to string failed", exception);
        }
        return response;
    }

    @Override
    public String post(final FetchInfos fetchInfos, final String data) throws HttpException {
        URL url = null;
        try {
            url = new URL(fetchInfos.getUri());
        } catch (MalformedURLException exception) {
            throw new HttpException("uri is malformed '" + fetchInfos.getUri() + "'", exception);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException exception) {
            fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            throw new HttpException("get '" + fetchInfos.getUri() + "' failed", exception);
        }
        InputStream input = null;
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(data.getBytes().length));
            final DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
            input = connection.getInputStream();
            if ("gzip".equals(connection.getHeaderField("content-encoding"))) {
                input = new GZIPInputStream(input);
            }
            if (HttpServiceImpl.LOGGER.isDebugEnabled()) {
                this.logConnection(connection);
                input = new LoggingInputStream(input);
            }
        } catch (SocketTimeoutException exception) {
            fetchInfos.setResult(FETCHING_RESULT.TIME_OUT);
            throw new HttpException("get '" + fetchInfos.getUri() + "' timeout", exception);
        } catch (IOException exception) {
            fetchInfos.setResult(FETCHING_RESULT.IO_ERROR);
            throw new HttpException("get '" + fetchInfos.getUri() + "' failed", exception);
        }
        fetchInfos.setResult(FETCHING_RESULT.OK);
        String response = null;
        try {
            response = this.toString(input);
        } catch (IOException exception) {
            throw new HttpException("converting inputstream to string failed", exception);
        }
        return response;
    }

    public void setDateFormat(final String dateFormat) {
        this.dateFormat = dateFormat;
    }

    private InputStream simpleFetch(final String wwwUri) throws HttpException {
        URL url = null;
        try {
            url = new URL(wwwUri);
        } catch (MalformedURLException exception) {
            throw new HttpException("what the fuck '" + wwwUri + "'", exception);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException exception) {
            throw new HttpException("fetching '" + wwwUri + "' failed", exception);
        }
        connection.setRequestProperty("Accept-Encoding", "gzip");
        InputStream input = null;
        try {
            connection.connect();
            input = connection.getInputStream();
            if ("gzip".equals(connection.getHeaderField("content-encoding"))) {
                input = new GZIPInputStream(input);
            }
        } catch (SocketTimeoutException exception) {
            throw new HttpException("fetching '" + wwwUri + "' timeout", exception);
        } catch (IOException exception) {
            throw new HttpException("fetching '" + wwwUri + "' failed", exception);
        }
        return input;
    }

    private Map<String, String> explodeHeader(final String headerValue) {
        final String[] entries = headerValue.split(",");
        final Map<String, String> values = new HashMap<String, String>(entries.length);
        for (String entry : entries) {
            final String[] keyValue = entry.split("=");
            final String key = keyValue[0].trim().toLowerCase();
            if (keyValue.length == 1) {
                values.put(key, "");
            } else {
                values.put(key, keyValue[1].trim());
            }
        }
        return values;
    }

    private String toString(final InputStream is) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
	 * Log headers for debug.
	 * 
	 * @throws IOException 
	 */
    private void logConnection(final HttpURLConnection connection) throws IOException {
        final StringBuilder builder = new StringBuilder("Connection for URL '").append(connection.getURL()).append("'\n");
        builder.append("Response: ").append(connection.getResponseCode()).append("-").append(connection.getResponseMessage()).append("\n");
        builder.append("Headers:\n");
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ");
            for (String valeur : entry.getValue()) {
                builder.append(valeur).append(";");
            }
            builder.append("\n");
        }
        builder.append("\n");
        HttpServiceImpl.LOGGER.debug(builder.toString());
    }

    private class FavIconHtmlParser {

        private String path = null;

        public String getPath() {
            return this.path;
        }

        @SuppressWarnings("unused")
        public void setPath(final String rel, final String href) {
            if ("icon".equals(rel) || "shortcut icon".equals(rel)) {
                this.path = href;
            }
        }
    }

    private static class LoggingInputStream extends FilterInputStream {

        protected LoggingInputStream(final InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            final int value = super.read();
            HttpServiceImpl.LOGGER.debug(String.valueOf((char) value));
            return value;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            final int readLen = super.read(b, off, len);
            if (readLen != -1) {
                HttpServiceImpl.LOGGER.debug(new String(b, off, readLen));
            }
            return readLen;
        }
    }
}
