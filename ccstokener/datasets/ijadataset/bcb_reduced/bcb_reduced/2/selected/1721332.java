package com.simpledata.bc.webcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import com.simpledata.bc.BC;
import com.simpledata.bc.Params;
import com.simpledata.bc.SoftInfos;
import com.simpledata.filetools.TextFileUtils;

/**
 * This class retrive infos from the TariffEye.com website. The serverside
 * implementation can answer to differents queries (actions). 
 * @author Simpledata SARL, 2004, all rights reserved.
 * @version $Id: TariffEyeQuery.java,v 1.2 2007/04/02 17:04:27 perki Exp $
 */
public class TariffEyeQuery {

    /** any header used by TariffEye should must start with this string **/
    private static final String HEADER = "TariffEye-";

    /** 
	 * Header key of a set parameter command.<br> 
	 * When this header is found we set a parameter to a new value<br>
	 * Content: <parameter key>=<parameter value>
	 */
    private static final String HEADER_SET_PARAMS = HEADER + "SetParameter";

    private static final String KEY_VALUE_SEPARATOR = "=";

    /** 
	 * should be present in any query .. used to be sure this 
	 * request was set by TariffEye web site
	 */
    private static final String HEADER_TAG_OK = HEADER + "OK";

    /** Subscription Expire TimeStamp **/
    private static final String HEADER_SUBSCR_EXPIRE = HEADER + "SubscrExpires";

    /** LastUpdate TimeStamp (from the server time) **/
    private static final String HEADER_LAST_UPDATE = HEADER + "LastUpdate";

    /** Logger */
    private static final Logger m_log = Logger.getLogger(TariffEyeQuery.class);

    /** Custom arguments for the http query */
    private final Map m_args;

    /** Action name */
    private final String m_action;

    /** The connection */
    private HttpURLConnection m_connection;

    /** the inputStream relative to this connection, may be null **/
    private InputStream m_inputStream;

    /**
	 * Creates a new TariffEyeQuery instance for a specified action, with
	 * a map of custom arguments.
	 * @param action Action name. The server answers with the corresponding 
	 * php page.
	 * @param args Custom arguments for the Url args part. Will be added to the
	 * built-in ones.
	 */
    public TariffEyeQuery(String action, Map args) {
        m_args = args;
        m_action = action;
        m_connection = null;
    }

    /**
	 * Creates a new TariffEyeQuery instance for a specified action.
	 * @param action Action name. The server answers with the corresponding 
	 * php page.
	 */
    public TariffEyeQuery(String action) {
        this(action, new HashMap());
    }

    /**
	 * add a parameter 
	 */
    public void addParam(String key, String value) {
        assert connectStatus < 0 : "cannot modifiy paramters on a commited Query";
        m_args.put(key, value);
    }

    /**
	 * Do the query.<BR>
	 * Will attempt to connect if not already done;
	 * @return an input stream, getting the result for this query. Return null
	 * if it fails.
	 */
    public InputStream queryInputStream() {
        if (!connect()) return null;
        return m_inputStream;
    }

    /**
	 * Do the query.<BR>
	 * Will attempt to connect if not already done;
	 * @return String containing the whole HTML document resulting from the
	 * the query. Return null if the query fails.
	 */
    public String document() {
        String document = null;
        try {
            document = TextFileUtils.getString(queryInputStream());
        } catch (IOException e) {
            m_log.error("getDocument failed", e);
        }
        return document;
    }

    /** get the URL representation of this Query **/
    public String getURL() {
        String versionId = SoftInfos.softVersion();
        String licenseId = SoftInfos.id();
        String langId = BC.langManager.getLang();
        Date lastUpdate = (Date) BC.getParameter(Params.KEY_LAST_UPDATE_REMOTE_TIMESTAMP);
        String lastUpdateStr = "";
        if (lastUpdate != null) {
            lastUpdateStr = String.valueOf(lastUpdate.getTime());
        }
        StringBuffer args = new StringBuffer("?ACTION=" + m_action + "&id_license=" + licenseId + "&version=" + versionId + "&lang=" + langId + "&last_update=" + lastUpdateStr);
        Iterator it = m_args.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = (String) m_args.get(key);
            args.append("&" + key + "=" + value);
        }
        String serverName = BC.getParameterStr(Params.KEY_UPDATE_URL);
        return serverName + args;
    }

    /** 
	 * Initialize http connection .. and process the headers<BR>
	 * connect(); can be called sevral times.. it will not connect again, but
	 * return true or false in case of success / failed on the first attempt
	 * @return true if Connection has been established.. false if failed.
	 */
    public boolean connect() {
        if (connectStatus > -1) return (connectStatus == 1);
        connectStatus = 0;
        try {
            URL url = new URL(getURL());
            m_connection = (HttpURLConnection) url.openConnection();
            m_connection.connect();
            processHeaders();
            m_inputStream = m_connection.getInputStream();
        } catch (MalformedURLException e) {
            newError("connect failed", e, true);
        } catch (IOException e) {
            newError("connect failed", e, true);
        }
        return (connectStatus == 1);
    }

    /** 
	 * -1 = never tried, 0 = failed ; 1 = success 
	 * connectStatus is modified by connect() et processHeaders();
	 * **/
    private int connectStatus = -1;

    /** When we connect, we look for commands in the http header */
    private void processHeaders() {
        assert m_connection != null : "Must be connected before processing headers";
        String v = null;
        if ((v = getHeaderValue(HEADER_TAG_OK)) != null) {
            connectStatus = 1;
        }
        if ((v = getHeaderValue(HEADER_SET_PARAMS)) != null) {
            handleParams(v);
        }
        if ((v = getHeaderValue(HEADER_SUBSCR_EXPIRE)) != null) {
            handleSubscr(v);
        }
        if ((v = getHeaderValue(HEADER_LAST_UPDATE)) != null) {
            handleLastUpdate(v);
        }
    }

    /** get an header value **/
    private String getHeaderValue(String key) {
        assert m_connection != null : "Must be connected before processing headers";
        return m_connection.getHeaderField(key);
    }

    /** Called for each te_parameter param */
    private void handleParams(String value) {
        int delimPos = value.indexOf(KEY_VALUE_SEPARATOR);
        if (delimPos != -1) {
            int size = value.length();
            String pKey = value.substring(0, delimPos - 1);
            String pValue = value.substring(delimPos + 1, size - 1);
            m_log.debug("Set the param: " + pKey + " -> " + pValue);
        } else {
            m_log.warn("Invalid parameter set command: " + value);
        }
    }

    /** Subscription expiration header. Set the ad hoc parameter */
    private void handleSubscr(String value) {
        m_log.info("Subscribtion expiration date: " + value);
        Date expiresOn = new Date(new Long(value).longValue() * 1000);
        BC.setParameter(Params.KEY_SUBSCRIBTION_EXPIRES, expiresOn);
    }

    private void handleLastUpdate(String value) {
        m_log.info("LastUpdate TS: " + value);
        Date lastUpdate = new Date(new Long(value).longValue() * 1000);
        Date now = new Date();
        BC.setParameter(Params.KEY_LAST_UPDATE_REMOTE_TIMESTAMP, lastUpdate);
        BC.setParameter(Params.KEY_LAST_UPDATE_LOCAL_TIMESTAMP, now);
    }

    /**
	 * Contains all the Exceptions that might have happend
	 */
    private ArrayList errors = new ArrayList();

    private void newError(String msg, Throwable t, boolean dispStackTrace) {
        errors.add(t);
        if (dispStackTrace) {
            m_log.warn(msg);
        } else {
            m_log.error(msg, t);
        }
    }
}
