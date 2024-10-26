package org.t2framework.t2.format.amf.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.t2framework.t2.format.amf.client.exception.AmfFaultException;
import org.t2framework.t2.format.amf.message.Message;
import org.t2framework.t2.format.amf.message.MessageBody;
import org.t2framework.t2.format.amf.message.flex.AcknowledgeMessage;
import org.t2framework.t2.format.amf.message.flex.ErrorMessage;
import org.t2framework.t2.format.amf.message.flex.RemotingMessage;
import org.t2framework.t2.format.amf.spi.impl.AmfMessageProcessorImpl;
import org.t2framework.t2.mock.NullWebConfiguration;

/**
 * 
 * <#if locale="en">
 * <p>
 * AmfConnection for Java AMF client.
 * </p>
 * <#else>
 * <p>
 * Java AMF クライアント用のAmfConnectionです.
 * </p>
 * </#if>
 * 
 * @author yone098
 * 
 */
public class AmfConnection {

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String CONTENT_TYPE_AMF = "application/x-amf";

    private static final Logger logger = Logger.getLogger(AmfConnection.class.getName());

    private static final int AMF_VERSION_3 = new Integer(3);

    private static final String UTF8 = "UTF-8";

    private static final String CHARSET = "charset";

    public static final String COOKIE = "Cookie";

    public static final String COOKIE2 = "Cookie2";

    public static final String COOKIE_SEPERATOR = ";";

    public static final String COOKIE_NAMEVALUE_SEPERATOR = "=";

    public static final String SET_COOKIE = "Set-Cookie";

    public static final String SET_COOKIE2 = "Set-Cookie2";

    private RemotingMessage remoteMessage;

    private URL urlObject;

    private String url;

    private String destination;

    private String operation;

    protected Map<String, String> cookies;

    /**
	 * constructor
	 */
    public AmfConnection() {
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Connection destination URL is set.
	 * </p>
	 * <#else>
	 * <p>
	 * 指定されたURLを設定します.
	 * </p>
	 * </#if>
	 * 
	 * @param url
	 */
    public void connect(final String url) {
        try {
            this.urlObject = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        this.url = url;
    }

    public void close() {
        if (cookies != null) {
            cookies.clear();
        }
        this.url = null;
        this.urlObject = null;
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * The connection destination is called by using the AMF format.
	 * </p>
	 * <#else>
	 * <p>
	 * AMFフォーマットを利用してパラメータ無しで接続先を呼び出します。
	 * </p>
	 * </#if>
	 * 
	 * @param <T>
	 * @return
	 * @throws AmfFaultException
	 */
    public <T> T call() throws AmfFaultException {
        return this.<T>call(new Object[] {});
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * The connection destination is called by using the AMF format.
	 * </p>
	 * <#else>
	 * <p>
	 * AMFフォーマットを利用して接続先を呼び出します。
	 * </p>
	 * </#if>
	 * 
	 * @param param
	 * @return
	 * @throws Exception
	 */
    public <T> T call(Object param) throws AmfFaultException {
        return this.<T>call(new Object[] { param });
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * The connection destination is called by using the AMF format.
	 * </p>
	 * <#else>
	 * <p>
	 * AMFフォーマットを利用して接続先を呼び出します。
	 * </p>
	 * </#if>
	 * 
	 * @param params
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public <T> T call(Object[] params) throws AmfFaultException {
        Message message = new Message();
        MessageBody body = new MessageBody();
        RemotingMessage remotingMessage = null;
        if (this.remoteMessage != null) {
            remotingMessage = this.remoteMessage;
        } else {
            remotingMessage = new RemotingMessage();
            if (this.destination == null) {
                throw new RuntimeException("not setting destination.");
            }
            if (this.operation == null) {
                throw new RuntimeException("not setting operation.");
            }
            remotingMessage.setHeader("version", AMF_VERSION_3);
            remotingMessage.setDestination(this.destination);
            remotingMessage.setOperation(this.operation);
            String messageId = UUID.randomUUID().toString();
            remotingMessage.setMessageId(messageId);
            List<Object> paramList = new ArrayList<Object>();
            for (Object param : params) {
                paramList.add(param);
            }
            remotingMessage.setBody(paramList);
        }
        body.setData(remotingMessage);
        body.setTarget("null");
        body.setResponse("/1");
        message.addBody(body);
        message.setVersion(3);
        Message responseMessage;
        try {
            URLConnection conn = this.urlObject.openConnection();
            logger.log(Level.INFO, "connect to [" + url + "] destination[" + this.destination + "] operation[" + this.operation + "]");
            conn.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_AMF);
            conn.setRequestProperty(CHARSET, UTF8);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            this.setHttpRequestCookieHeader(conn);
            OutputStream outputStream = conn.getOutputStream();
            AmfMessageProcessorImpl amfMessageProcessorImpl = createAmfMessageProcessorImpl();
            amfMessageProcessorImpl.writeRequestMessage(outputStream, message);
            InputStream inputStream = conn.getInputStream();
            this.processHttpResponseHeaders(conn.getHeaderFields());
            responseMessage = amfMessageProcessorImpl.readMessage(inputStream);
        } catch (Exception e) {
            throw new AmfFaultException(e);
        }
        MessageBody res = responseMessage.getBody(0);
        Object obj = res.getData();
        if (obj instanceof ErrorMessage) {
            ErrorMessage errMessage = (ErrorMessage) obj;
            final String code = errMessage.getFaultCode();
            final String msg = errMessage.getFaultDetail();
            throw new AmfFaultException(msg, code);
        } else if (obj instanceof AcknowledgeMessage) {
            AcknowledgeMessage ack = (AcknowledgeMessage) res.getData();
            Object response = ack.getBody();
            logger.log(Level.INFO, "response:" + response);
            return (T) response;
        } else {
            throw new RuntimeException("unknown MessageBody");
        }
    }

    protected void processHttpResponseHeaders(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> element : headers.entrySet()) {
            String headerName = element.getKey();
            List<String> headerValues = element.getValue();
            for (String headerValue : headerValues) {
                if (SET_COOKIE.equals(headerName) || COOKIE.equals(headerName) || SET_COOKIE2.equals(headerName) || COOKIE2.equals(headerName)) {
                    processSetCookieHeader(headerValue);
                }
            }
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Processes the incoming set-cookie headers.
	 * </p>
	 * <#else>
	 * <p>
	 * set-cookieヘッダを処理します。
	 * </p>
	 * </#if>
	 * 
	 * @param headerValue
	 *            The value of the set-cookie header.
	 */
    protected void processSetCookieHeader(String headerValue) {
        String cookie = headerValue;
        if (cookie.indexOf(COOKIE_SEPERATOR) > 0) {
            cookie = headerValue.substring(0, cookie.indexOf(COOKIE_SEPERATOR));
        }
        String name = cookie.substring(0, cookie.indexOf(COOKIE_NAMEVALUE_SEPERATOR));
        String value = cookie.substring(cookie.indexOf(COOKIE_NAMEVALUE_SEPERATOR) + 1, cookie.length());
        if (cookies == null) {
            cookies = new HashMap<String, String>();
        }
        logger.log(Level.INFO, "setCookie key[" + name + "] value[" + value + "]");
        cookies.put(name, value);
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Processes the incoming set-cookie headers.
	 * </p>
	 * <#else>
	 * <p>
	 * set-cookieヘッダを処理します。
	 * </p>
	 * </#if> Sets the Http request cookie headers.
	 */
    protected void setHttpRequestCookieHeader(URLConnection conn) {
        if (cookies == null) return;
        StringBuffer cookieHeaderValue = null;
        for (Map.Entry<String, String> element : cookies.entrySet()) {
            String name = element.getKey();
            String value = element.getValue();
            if (cookieHeaderValue == null) {
                cookieHeaderValue = new StringBuffer(name + COOKIE_NAMEVALUE_SEPERATOR + value);
            } else {
                cookieHeaderValue.append(COOKIE_SEPERATOR + " " + name + COOKIE_NAMEVALUE_SEPERATOR + value);
            }
        }
        if (cookieHeaderValue != null) {
            final String cookieValue = cookieHeaderValue.toString();
            logger.log(Level.INFO, "setRequestCookie value[" + cookieValue + "]");
            conn.setRequestProperty(COOKIE, cookieValue);
        }
    }

    /**
	 * @return the destination
	 */
    public String getDestination() {
        return destination;
    }

    /**
	 * @param destination
	 *            the destination to set
	 */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
	 * @param remoteMessage
	 *            the remoteMessage to set
	 */
    public void setRemoteMessage(RemotingMessage remoteMessage) {
        this.remoteMessage = remoteMessage;
    }

    /**
	 * @param operation
	 *            the operation to set
	 */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    /**
	 * create AmfMessageProcessor
	 * 
	 * @return {@link AmfMessageProcessorImpl} object
	 */
    protected AmfMessageProcessorImpl createAmfMessageProcessorImpl() {
        AmfMessageProcessorImpl amfMessageProcessorImpl = new AmfMessageProcessorImpl();
        amfMessageProcessorImpl.initialize(new NullWebConfiguration());
        return amfMessageProcessorImpl;
    }
}
