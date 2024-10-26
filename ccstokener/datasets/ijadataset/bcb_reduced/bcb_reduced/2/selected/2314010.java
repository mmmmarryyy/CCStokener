package edu.harvard.iq.safe.saasystem.etl.http;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import edu.harvard.iq.safe.saasystem.etl.util.lockss.LOCKSSDaemonStatusTableTO;
import edu.harvard.iq.safe.saasystem.etl.util.lockss.LOCKSSDaemonStatusTableXmlStreamParser;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Akio Sone
 */
public class HttpClientDAO implements Serializable {

    private static final Logger logger = Logger.getLogger(HttpClientDAO.class.getName());

    static String LOGIN_ROOT = "/j_security_check";

    private XStream xstream = new XStream(new JsonHierarchicalStreamDriver());

    /**
     *
     */
    protected String ip;

    /**
     *
     */
    protected String portNumber;

    /**
     *
     */
    protected String queryStringRoot;

    /**
     *
     */
    protected String tableId;

    /**
     *
     */
    protected String outputFormat;

    /**
     *
     */
    protected String dataUrl;

    /**
     *
     */
    protected DefaultHttpClient httpClient;

    protected String protocol;

    protected String loginUrl;

    /**
     *
     * @param httpClient
     * @param ip
     * @param tableId
     */
    public HttpClientDAO(DefaultHttpClient httpClient, String ip, String tableId) {
        if (httpClient == null) {
            throw new IllegalArgumentException("http client is null");
        }
        if (ip == null) {
            throw new IllegalArgumentException("Ip Addrees is null");
        }
        if (tableId == null) {
            throw new IllegalArgumentException("table id is null");
        }
        this.httpClient = httpClient;
        this.ip = ip;
        this.tableId = tableId;
        this.portNumber = "8081";
        this.queryStringRoot = "/DaemonStatus?table=";
        this.outputFormat = "xml";
        this.dataUrl = "http://" + ip + ":" + portNumber + queryStringRoot + tableId + "&output=" + outputFormat;
    }

    public HttpClientDAO(DefaultHttpClient httpClient, String ip, String tableId, String protocol) {
        if (httpClient == null) {
            throw new IllegalArgumentException("http client is null");
        }
        if (ip == null) {
            throw new IllegalArgumentException("Ip Addrees is null");
        }
        if (tableId == null) {
            throw new IllegalArgumentException("table id is null");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("protocol is null");
        }
        this.httpClient = httpClient;
        this.ip = ip;
        this.tableId = tableId;
        this.portNumber = "8081";
        this.queryStringRoot = "/DaemonStatus?table=";
        this.outputFormat = "xml";
        this.protocol = protocol;
        this.dataUrl = protocol + "://" + ip + ":" + portNumber + queryStringRoot + tableId + "&output=" + outputFormat;
        this.loginUrl = protocol + "://" + ip + ":" + portNumber + LOGIN_ROOT;
    }

    /**
     *
     * @param httpClient
     * @param ip
     * @param portNumber
     * @param queryStringRoot
     * @param tableId
     * @param outputFormat
     */
    public HttpClientDAO(DefaultHttpClient httpClient, String ip, String portNumber, String queryStringRoot, String tableId, String outputFormat) {
        if (httpClient == null) {
            throw new IllegalArgumentException("http client is null");
        }
        if (ip == null) {
            throw new IllegalArgumentException("Ip Addrees is null");
        }
        if (tableId == null) {
            throw new IllegalArgumentException("table id is null");
        }
        if (queryStringRoot == null) {
            throw new IllegalArgumentException("query String is null");
        }
        if (outputFormat == null) {
            throw new IllegalArgumentException("output format is null");
        }
        this.httpClient = httpClient;
        this.ip = ip;
        this.portNumber = portNumber;
        this.queryStringRoot = queryStringRoot;
        this.tableId = tableId;
        this.outputFormat = outputFormat;
        this.dataUrl = "http://" + ip + ":" + portNumber + queryStringRoot + tableId + "&output=" + outputFormat;
    }

    public HttpClientDAO(DefaultHttpClient httpClient, String ip, String portNumber, String queryStringRoot, String tableId, String outputFormat, String protocol) {
        if (httpClient == null) {
            throw new IllegalArgumentException("http client is null");
        }
        if (ip == null) {
            throw new IllegalArgumentException("Ip Addrees is null");
        }
        if (tableId == null) {
            throw new IllegalArgumentException("table id is null");
        }
        if (queryStringRoot == null) {
            throw new IllegalArgumentException("query String is null");
        }
        if (outputFormat == null) {
            throw new IllegalArgumentException("output format is null");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("protocol is null");
        }
        this.httpClient = httpClient;
        this.ip = ip;
        this.portNumber = portNumber;
        this.queryStringRoot = queryStringRoot;
        this.tableId = tableId;
        this.outputFormat = outputFormat;
        this.protocol = protocol;
        this.dataUrl = protocol + "://" + ip + ":" + portNumber + queryStringRoot + tableId + "&output=" + outputFormat;
        this.loginUrl = protocol + "://" + ip + ":" + portNumber + LOGIN_ROOT;
    }

    /**
     *
     * @return
     */
    public DefaultHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * 
     * @param httpClient
     */
    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * @param portNumber the portNumber to set
     */
    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    public String getPortNumber() {
        return portNumber;
    }

    /**
     * @param queryStringRoot the queryStringRoot to set
     */
    public void setQueryStringRoot(String queryStringRoot) {
        this.queryStringRoot = queryStringRoot;
    }

    /**
     * @param tableId the tableId to set
     */
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    /**
     * @param outputFormat the outputFormat to set
     */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * @param dataUrl the dataUrl to set
     */
    public void setDataUrl(String dataUrl) {
        this.dataUrl = dataUrl;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    /**
     *
     * @return
     * @throws HttpResponseException
     */
    public LOCKSSDaemonStatusTableTO getDataFromDaemonStatusTable() throws HttpResponseException {
        LOCKSSDaemonStatusTableXmlStreamParser ldstxp = null;
        LOCKSSDaemonStatusTableTO ldstTO = null;
        HttpEntity entity = null;
        HttpGet httpget = null;
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("HttpClientDAO", HttpClientDAO.class);
        try {
            httpget = new HttpGet(dataUrl);
            logger.log(Level.INFO, "executing request {0}", httpget.getURI());
            HttpResponse resp = httpClient.execute(httpget);
            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                logger.log(Level.WARNING, "response to the request is not OK: skip this IP: status code={0}", statusCode);
                httpget.abort();
                ldstTO = new LOCKSSDaemonStatusTableTO();
                ldstTO.setBoxHttpStatusOK(false);
                return ldstTO;
            }
            entity = resp.getEntity();
            InputStream is = entity.getContent();
            ldstxp = new LOCKSSDaemonStatusTableXmlStreamParser();
            ldstxp.read(new BufferedInputStream(is));
            ldstTO = ldstxp.getLOCKSSDaemonStatusTableTO();
            ldstTO.setIpAddress(this.ip);
            logger.log(Level.INFO, "After parsing [{0}] table", this.tableId);
            logger.log(Level.FINEST, "After parsing {0}: contents of ldstTO:\n{1}", new Object[] { this.tableId, ldstTO });
            if (ldstTO.hasIncompleteRows) {
                logger.log(Level.WARNING, "!!!!!!!!! incomplete rows are found for {0}", tableId);
                if (ldstTO.getTableData() != null && ldstTO.getTableData().size() > 0) {
                    logger.log(Level.FINE, "incomplete rows: table(map) data dump =[\n{0}\n]", xstream.toXML(ldstTO.getTableData()));
                }
            } else {
                logger.log(Level.INFO, "All rows are complete for {0}", tableId);
            }
        } catch (ConnectTimeoutException ce) {
            logger.log(Level.WARNING, "ConnectTimeoutException occurred", ce);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } catch (SocketTimeoutException se) {
            logger.log(Level.WARNING, "SocketTimeoutException occurred", se);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } catch (ClientProtocolException pe) {
            logger.log(Level.SEVERE, "The protocol was not http; https is suspected", pe);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            ldstTO.setHttpProtocol(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IO exception occurs", ex);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } finally {
            if (entity != null) {
                try {
                    EntityUtils.consume(entity);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "io exception when entity was to be" + "consumed", ex);
                }
            }
        }
        return ldstTO;
    }

    public LOCKSSDaemonStatusTableTO getDataFromDaemonStatusTableByHttps() throws HttpResponseException {
        LOCKSSDaemonStatusTableXmlStreamParser ldstxp = null;
        LOCKSSDaemonStatusTableTO ldstTO = null;
        HttpEntity entity = null;
        HttpGet httpget = null;
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("HttpClientDAO", HttpClientDAO.class);
        try {
            httpget = new HttpGet(dataUrl);
            logger.log(Level.INFO, "executing request {0}", httpget.getURI());
            HttpResponse resp = httpClient.execute(httpget);
            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                logger.log(Level.WARNING, "response to the request is not OK: skip this IP: status code={0}", statusCode);
                httpget.abort();
                ldstTO = new LOCKSSDaemonStatusTableTO();
                ldstTO.setBoxHttpStatusOK(false);
                return ldstTO;
            }
            entity = resp.getEntity();
            InputStream is = entity.getContent();
            ldstxp = new LOCKSSDaemonStatusTableXmlStreamParser();
            ldstxp.read(new BufferedInputStream(is));
            ldstTO = ldstxp.getLOCKSSDaemonStatusTableTO();
            ldstTO.setIpAddress(this.ip);
            logger.log(Level.INFO, "After parsing [{0}] table", this.tableId);
            logger.log(Level.FINEST, "After parsing {0}: contents of ldstTO:\n{1}", new Object[] { this.tableId, ldstTO });
            if (ldstTO.hasIncompleteRows) {
                logger.log(Level.WARNING, "!!!!!!!!! incomplete rows are found for {0}", tableId);
                if (ldstTO.getTableData() != null && ldstTO.getTableData().size() > 0) {
                    logger.log(Level.FINE, "incomplete rows: table(map) data dump =[\n{0}\n]", xstream.toXML(ldstTO.getTableData()));
                }
            } else {
                logger.log(Level.INFO, "All rows are complete for {0}", tableId);
            }
        } catch (ConnectTimeoutException ce) {
            logger.log(Level.WARNING, "ConnectTimeoutException occurred", ce);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } catch (SocketTimeoutException se) {
            logger.log(Level.WARNING, "SocketTimeoutException occurred", se);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } catch (ClientProtocolException pe) {
            logger.log(Level.SEVERE, "The protocol was not http; https is suspected", pe);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            ldstTO.setHttpProtocol(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IO exception occurs", ex);
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
            if (httpget != null) {
                httpget.abort();
            }
            return ldstTO;
        } finally {
            if (entity != null) {
                try {
                    EntityUtils.consume(entity);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "io exception when entity was to be" + "consumed", ex);
                }
            }
        }
        return ldstTO;
    }
}
