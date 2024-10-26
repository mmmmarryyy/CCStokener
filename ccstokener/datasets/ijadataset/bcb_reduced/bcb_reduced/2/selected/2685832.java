package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.grlea.log.SimpleLogger;

public class Http {

    private static final SimpleLogger LOG = new SimpleLogger(Http.class);

    private static final int TIMEOUT = 2000;

    private static final int RETRYS = 3;

    private static final String LOC = "location";

    /**
     *
     * @param link
     * @return Website als String mit fixem nicht einstellbarem timeout sowie mit 3 versuchen bei Fehler
     */
    public String getWebcontent(final String link) {
        return getWebcontent(link, TIMEOUT, RETRYS);
    }

    /**
     * @param method
     * @return Website als String mit fixem nicht einstellbarem timeout sowie mit 3 versuchen bei Fehler
     */
    public String getWebcontent(final PostMethod method) {
        return getWebcontent(method, TIMEOUT, RETRYS);
    }

    /**
     *
     * @param String link
     * @param int timeout_ms
     * @param int retrys
     * @return Liefert den inhalt einer Website als String zurück
     */
    public String getWebcontent(final String link, final int timeoutMs, final int retrys) {
        final HttpClientParams params = new HttpClientParams();
        final HttpClient client = new HttpClient(params);
        client.getHttpConnectionManager().getParams().setConnectionTimeout(timeoutMs);
        String contents = "";
        String redirectLocation = "";
        int trys = 0;
        boolean connection = false;
        GetMethod gmethod = new GetMethod(link.replaceAll("&amp;", "&"));
        while (!connection && trys < retrys) {
            try {
                int statusCode = client.executeMethod(gmethod);
                if (statusCode != -1) {
                    Header locationHeader = gmethod.getResponseHeader(LOC);
                    while (locationHeader != null) {
                        redirectLocation = locationHeader.getValue();
                        gmethod = new GetMethod(redirectLocation);
                        statusCode = client.executeMethod(gmethod);
                        locationHeader = gmethod.getResponseHeader(LOC);
                    }
                    contents = gmethod.getResponseBodyAsString();
                    connection = true;
                }
            } catch (final IOException e) {
                LOG.ludicrous("getWebContent(String link, int timeout_ms, int retrys), failure attempt: " + trys + "\040" + e.toString());
                if (trys + 1 == retrys) {
                    LOG.error("getWebContent(String link, int timeout_ms, int retrys), last attempt failed: " + link + "\040" + e.toString());
                }
                trys++;
            } finally {
                gmethod.releaseConnection();
            }
        }
        return contents;
    }

    /**
     *
     * @param PostMethod method
     * @return Liefert den inhalt einer Website als String zurück
     */
    public String getWebcontent(PostMethod method, final int timeoutMs, final int retrys) {
        String content = "";
        String f = "";
        try {
            f = method.getURI().toString();
        } catch (final URIException e1) {
            LOG.error("getWebcontent(PostMethod method, int timeout_ms, int retrys): " + e1.toString());
        }
        String redirectLocation = "";
        final HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(timeoutMs);
        boolean connection = false;
        int trys = 0;
        while (!connection && trys < retrys) {
            try {
                int statusCode = client.executeMethod(method);
                if (statusCode != -1) {
                    Header locationHeader = method.getResponseHeader(LOC);
                    while (locationHeader != null) {
                        redirectLocation = locationHeader.getValue();
                        method = new PostMethod(redirectLocation);
                        statusCode = client.executeMethod(method);
                        locationHeader = method.getResponseHeader(LOC);
                    }
                    content = method.getResponseBodyAsString();
                    connection = true;
                }
            } catch (final IOException e) {
                LOG.ludicrous("getWebContent(PostMethod method, int timeout_ms, int retrys), failure attempt: " + trys + "\040" + e.toString());
                if (trys + 1 == retrys) {
                    LOG.error("getWebContent(PostMethod method, int timeout_ms, int retrys), last attempt failed: " + e.toString() + "\012" + f);
                }
                trys++;
            } finally {
                method.releaseConnection();
            }
        }
        return content;
    }

    /**
     *
     * @param link
     * @param postdata
     * @return
     */
    public String getWebcontent(final String link, final String postdata) {
        final StringBuffer response = new StringBuffer();
        try {
            DisableSSLCertificateCheckUtil.disableChecks();
            final URL url = new URL(link);
            final URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            final OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(postdata);
            wr.flush();
            final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String content = "";
            while ((content = rd.readLine()) != null) {
                response.append(content);
                response.append('\n');
            }
            wr.close();
            rd.close();
        } catch (final Exception e) {
            LOG.error("getWebcontent(String link, String postdata): " + e.toString() + "\012" + link + "\012" + postdata);
        }
        return response.toString();
    }
}
