package edu.harvard.iq.safe.saasystem.etl.http;

import edu.harvard.iq.safe.saasystem.etl.util.lockss.DaemonStatusDataUtil;
import edu.harvard.iq.safe.saasystem.etl.util.lockss.LOCKSSDaemonStatusTableTO;
import edu.harvard.iq.safe.saasystem.etl.util.lockss.LOCKSSPlatformStatusHtmlParser;
import edu.harvard.iq.safe.saasystem.etl.util.lockss.LOCKSSPlatformStatusReader;
import edu.harvard.iq.safe.saasystem.util.SAASConfigurationRegistryBean;
import edu.harvard.iq.safe.saasystem.util.SAASEJBConstants;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Akio Sone
 */
public final class HttpClientPlatformStatusDAO extends HttpClientDAO {

    private static final Logger logger = Logger.getLogger(HttpClientPlatformStatusDAO.class.getName());

    SAASConfigurationRegistryBean saasConfigRegistry = null;

    InitialContext ic = null;

    static String SAAS_EJB_JNDI_MODULE_NAME = SAASEJBConstants.SAAS_EJB_JNDI_MODULE_NAME_DEFAULT;

    static {
        if (StringUtils.isNotBlank(System.getProperty(SAASEJBConstants.SAAS_EJB_JNDI_MODULE_NAME_KEY))) {
            SAAS_EJB_JNDI_MODULE_NAME = System.getProperty(SAASEJBConstants.SAAS_EJB_JNDI_MODULE_NAME_KEY);
            logger.log(Level.INFO, "user-defined saasEjbJndiModuleName is available:{0}", SAAS_EJB_JNDI_MODULE_NAME);
        } else {
            logger.log(Level.INFO, "user-defined saasEjbJndiModuleName is not available; use the default one:{0}", SAAS_EJB_JNDI_MODULE_NAME);
        }
    }

    {
        try {
            ic = new InitialContext();
            logger.log(Level.INFO, "JNDI MODULE NAME={0}", SAAS_EJB_JNDI_MODULE_NAME);
            logger.log(Level.INFO, "SAASConfigurationRegistryBean.class.getSimpleName()={0}", SAASConfigurationRegistryBean.class.getSimpleName());
            saasConfigRegistry = (SAASConfigurationRegistryBean) ic.lookup(SAAS_EJB_JNDI_MODULE_NAME + SAASConfigurationRegistryBean.class.getSimpleName());
        } catch (NamingException ex) {
            logger.log(Level.SEVERE, "Class {0} - JNDI lookup failed: SAASConfigurationRegistryBean", HttpClientPlatformStatusDAO.class);
        }
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
    public HttpClientPlatformStatusDAO(DefaultHttpClient httpClient, String ip, String portNumber, String queryStringRoot, String tableId, String outputFormat) {
        super(httpClient, ip, portNumber, queryStringRoot, tableId, "html");
    }

    public HttpClientPlatformStatusDAO(DefaultHttpClient httpClient, String ip, String portNumber, String queryStringRoot, String tableId, String outputFormat, String protocol) {
        super(httpClient, ip, portNumber, queryStringRoot, tableId, "html", protocol);
    }

    /**
     *
     * @param httpClient
     * @param ip
     * @param tableId
     */
    public HttpClientPlatformStatusDAO(DefaultHttpClient httpClient, String ip, String tableId) {
        super(httpClient, ip, "8081", "/DaemonStatus?table=", tableId, "html");
    }

    public HttpClientPlatformStatusDAO(DefaultHttpClient httpClient, String ip, String tableId, String protocol) {
        super(httpClient, ip, "8081", "/DaemonStatus?table=", tableId, "html", protocol);
    }

    /**
     *
     * @return @throws HttpResponseException
     */
    @Override
    public LOCKSSDaemonStatusTableTO getDataFromDaemonStatusTable() throws HttpResponseException {
        LOCKSSPlatformStatusReader ldstxp = null;
        LOCKSSPlatformStatusHtmlParser ldsthp = null;
        LOCKSSDaemonStatusTableTO ldstTO = null;
        HttpEntity entity = null;
        HttpGet httpget = null;
        String headerTimeString = null;
        try {
            httpClient.getParams().setParameter("http.connection.timeout", 100000);
            httpClient.getParams().setParameter("http.socket.timeout", 100000);
            httpget = new HttpGet(dataUrl);
            logger.log(Level.INFO, "executing request {0}", httpget.getURI());
            HttpResponse resp = httpClient.execute(httpget);
            HeaderElementIterator it = new BasicHeaderElementIterator(resp.headerIterator());
            while (it.hasNext()) {
                HeaderElement elem = it.nextElement();
                logger.log(Level.INFO, "name({0})=value({1})", new Object[] { elem.getName(), elem.getValue() });
                if (elem.getName().endsWith("GMT")) {
                    headerTimeString = elem.getName();
                }
                NameValuePair[] params = elem.getParameters();
                for (int i = 0; i < params.length; i++) {
                    logger.log(Level.FINE, "parampair:name = {0}", params[i].getName());
                }
            }
            logger.log(Level.INFO, "headerTimeString={0}", headerTimeString);
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
            ldsthp = new LOCKSSPlatformStatusHtmlParser();
            ldsthp.getPlatformStatusData(is);
            ldstTO = ldsthp.getLOCKSSDaemonStatusTableTO();
            ldstTO.setIpAddress(this.ip);
            logger.log(Level.FINE, "After parsing {0}: contents of ldstTO:\n{1}", new Object[] { this.tableId, ldstTO });
            String currenttimeTimestamp = ldstTO.getBoxInfoMap().get("time");
            logger.log(Level.INFO, "headerTimeString={0} : currenttimeTimestamp={1}", new Object[] { headerTimeString, currenttimeTimestamp });
            String timezoneOffset = DaemonStatusDataUtil.calculateTimezoneOffset(headerTimeString, currenttimeTimestamp);
            ldstTO.setTimezoneOffset(timezoneOffset);
            logger.log(Level.INFO, "timezone offset={0}", ldstTO.getTimezoneOffset());
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
        } catch (ConnectException ce) {
            logger.log(Level.SEVERE, "connection to this box is refused:{0}", this.ip);
            if (httpget != null) {
                httpget.abort();
            }
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
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

    @Override
    public LOCKSSDaemonStatusTableTO getDataFromDaemonStatusTableByHttps() throws HttpResponseException {
        LOCKSSPlatformStatusReader ldstxp = null;
        LOCKSSPlatformStatusHtmlParser ldsthp = null;
        LOCKSSDaemonStatusTableTO ldstTO = null;
        HttpEntity entity = null;
        HttpGet httpget = null;
        String headerTimeString = null;
        try {
            httpClient.getParams().setParameter("http.connection.timeout", 100000);
            httpClient.getParams().setParameter("http.socket.timeout", 100000);
            httpget = new HttpGet(dataUrl);
            logger.log(Level.INFO, "executing request {0}", httpget.getURI());
            HttpResponse resp = httpClient.execute(httpget);
            HeaderElementIterator it = new BasicHeaderElementIterator(resp.headerIterator());
            while (it.hasNext()) {
                HeaderElement elem = it.nextElement();
                logger.log(Level.INFO, "name({0})=value({1})", new Object[] { elem.getName(), elem.getValue() });
                if (elem.getName().endsWith("GMT")) {
                    headerTimeString = elem.getName();
                }
                NameValuePair[] params = elem.getParameters();
                for (int i = 0; i < params.length; i++) {
                    logger.log(Level.FINE, "parampair:name = {0}", params[i].getName());
                }
            }
            logger.log(Level.INFO, "headerTimeString={0}", headerTimeString);
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
            ldsthp = new LOCKSSPlatformStatusHtmlParser();
            ldsthp.getPlatformStatusData(is);
            ldstTO = ldsthp.getLOCKSSDaemonStatusTableTO();
            ldstTO.setHostname(this.ip);
            logger.log(Level.FINE, "After parsing {0}: contents of ldstTO:\n{1}", new Object[] { this.tableId, ldstTO });
            String currenttimeTimestamp = ldstTO.getBoxInfoMap().get("time");
            logger.log(Level.INFO, "headerTimeString={0} : currenttimeTimestamp={1}", new Object[] { headerTimeString, currenttimeTimestamp });
            String timezoneOffset = DaemonStatusDataUtil.calculateTimezoneOffset(headerTimeString, currenttimeTimestamp);
            ldstTO.setTimezoneOffset(timezoneOffset);
            logger.log(Level.INFO, "timezone offset={0}", ldstTO.getTimezoneOffset());
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
        } catch (ConnectException ce) {
            logger.log(Level.SEVERE, "connection to this box is refused:{0}", this.ip);
            if (httpget != null) {
                httpget.abort();
            }
            ldstTO = new LOCKSSDaemonStatusTableTO();
            ldstTO.setBoxHttpStatusOK(false);
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
