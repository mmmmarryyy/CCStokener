package com.persistent.appfabric.servicebus;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import com.persistent.appfabric.acs.ACSTokenProvider;
import com.persistent.appfabric.acs.Credentials;
import com.persistent.appfabric.acs.TokenConstants;
import com.persistent.appfabric.common.AppFabricException;
import com.persistent.appfabric.common.LoggerUtil;
import com.persistent.appfabric.common.logger.SDKLoggerHelper;

;

/**
 * This class creates a message buffer, sends a message to the message buffer, retrieves a message from the message buffer
 */
public class MessageBuffer {

    private String httpWebProxyServer_;

    private int httpWebProxyPort_;

    private Proxy httpWebProxy_;

    Credentials credentials;

    private String solutionName;

    private static String DefaultLockDuration = "120";

    private static String DefaultReceiveTimeout = "60";

    private static String DefaultSendTimeout = "60";

    /**
	 * Constructor: Used when no proxy is required
     * Initializes the MessageBuffer class
     * @param credentials
     * @param solutionName   
     * */
    public MessageBuffer(Credentials credentials, String solutionName) {
        super();
        this.credentials = credentials;
        this.solutionName = solutionName;
    }

    /**
	 * Constructor: Used when proxy is required
     * Initializes the MessageBuffer class
     * @param httpWebProxyServer Name of the proxy server
     * @param httpWebProxyPort Port for the proxy server
     * @param credentials
     * @param solutionName  
     * 
     * */
    public MessageBuffer(String httpWebProxyServer, int httpWebProxyPort, Credentials credentials, String solutionName) {
        super();
        httpWebProxyServer_ = httpWebProxyServer;
        httpWebProxyPort_ = httpWebProxyPort;
        this.credentials = credentials;
        this.solutionName = solutionName;
        setHttpWebProxy(this.httpWebProxyServer_, this.httpWebProxyPort_);
    }

    /**
     *Used to set the web proxy if not done using the constructor
     *@param httpWebProxyServer Name of the proxy server
     *@param httpWebProxyPort Port for the proxy server
     * */
    public void setHttpWebProxy(String httpWebProxyServer, int httpWebProxyPort) {
        if (httpWebProxyServer_ != null) {
            SocketAddress address = new InetSocketAddress(httpWebProxyServer_, httpWebProxyPort_);
            httpWebProxy_ = new Proxy(Proxy.Type.HTTP, address);
        } else {
            httpWebProxy_ = null;
        }
    }

    /**
     * Extract message body from the raw message received on the message buffer
     * @param rawMessage
     * @return
     */
    public static String extractMessageBody(String rawMessage) {
        String searchString = "<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">";
        int startIndex = rawMessage.indexOf(searchString);
        if (startIndex > 0) {
            int endIndex = rawMessage.indexOf("</string>");
            if (endIndex > 0) {
                return rawMessage.substring(startIndex + searchString.length(), endIndex);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     *Function to create a message buffer using by making a POST request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name
     *@param messageBufferName Name of the message buffer
     *@param messageBufferPolicyObj Message Buffer Policy
     * */
    public void createMessageBuffer(String messageBufferName, MessageBufferPolicy messageBufferPolicyObj) throws AppFabricException {
        String messageBufferPolicy = messageBufferPolicyObj.getMessageBufferPolicy();
        if (messageBufferPolicy == null) {
            throw new AppFabricException("MessageBufferPolicy can not be null");
        }
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String messageBufferUri = msgBufferUtilObj.getCreateMessageBufferUri(messageBufferName);
        if (messageBufferUri == null) {
            throw new AppFabricException("MessageBufferUri can not be null");
        }
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            authorizationToken = tp.getACSToken(requestUri, messageBufferUri);
        } catch (AppFabricException e) {
            throw e;
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
        try {
            messageBufferUri = messageBufferUri.replaceAll("http", "https");
            URL urlConn = new URL(messageBufferUri);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_ATOM_XML());
            connection.setRequestProperty("Content-Length", "" + messageBufferPolicy.length());
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            connection.setRequestProperty("Expect", "100-continue");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(messageBufferPolicy);
            wr.flush();
            wr.close();
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.CreateMessageBuffer_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if ((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_ACCEPTED) || (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_CREATED) || (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK)) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
                if (LoggerUtil.getIsLoggingOn()) {
                    StringBuilder responseXML = new StringBuilder();
                    responseXML.append(responseCode);
                    responseXML.append(response.toString());
                    SDKLoggerHelper.logMessage(URLEncoder.encode(responseXML.toString(), "UTF-8"), SDKLoggerHelper.RecordType.CreateMessageBuffer_RESPONSE);
                }
            } else {
                if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.CreateMessageBuffer_RESPONSE);
                throw new AppFabricException("MessageBuffer could not be created or updated. Error. Response code:  " + connection.getResponseCode());
            }
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to get message buffer policy from the message buffer by making a GET request https://solution_name.servicebus.windows-bvt.net/messagebuffer_name
     *@param messageBufferName Name of the message buffer
     * */
    public String getPolicy(String messageBufferName) throws AppFabricException {
        String responseString = null;
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String messageBufferUri = msgBufferUtilObj.getCreateMessageBufferUri(messageBufferName);
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            authorizationToken = tp.getACSToken(requestUri, messageBufferUri);
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
        try {
            messageBufferUri = messageBufferUri.replaceAll("http", "https");
            URL urlConn = new URL(messageBufferUri);
            HttpURLConnection connection;
            StringBuffer sBuf = new StringBuffer();
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_ATOM_XML());
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.GetPolicy_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if ((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK)) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = rd.readLine()) != null) {
                    sBuf.append(line);
                    sBuf.append('\r');
                }
                rd.close();
                if (sBuf.toString().indexOf("<entry xmlns=") != -1) {
                    responseString = sBuf.toString();
                    if (LoggerUtil.getIsLoggingOn()) {
                        StringBuilder responseXML = new StringBuilder();
                        responseXML.append(responseCode);
                        responseXML.append(responseString);
                        SDKLoggerHelper.logMessage(URLEncoder.encode(responseXML.toString(), "UTF-8"), SDKLoggerHelper.RecordType.GetPolicy_RESPONSE);
                    }
                    return responseString;
                } else {
                    throw new AppFabricException("Message buffer policy could not be retrieved");
                }
            } else {
                if (LoggerUtil.getIsLoggingOn()) {
                    SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.GetPolicy_RESPONSE);
                }
                throw new AppFabricException("Message buffer policy could not be retrieved. Error.Response code:  " + connection.getResponseCode());
            }
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to delete message buffer policy from the message buffer by making a DELETE request https://solution_name.servicebus.windows-bvt.net/messagebuffer_name
     *@param messageBufferName Name of the message buffer
     * */
    public void deleteMessageBuffer(String messageBufferName) throws AppFabricException {
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String messageBufferUri = msgBufferUtilObj.getCreateMessageBufferUri(messageBufferName);
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            authorizationToken = tp.getACSToken(requestUri, messageBufferUri);
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
        try {
            messageBufferUri = messageBufferUri.replaceAll("http", "https");
            URL urlConn = new URL(messageBufferUri);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_ATOM_XML());
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.DeleteMessageBuffer_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if ((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK)) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
            } else {
                throw new AppFabricException("MessageBuffer could not be deleted.Error...Response code:  " + connection.getResponseCode());
            }
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.DeleteMessageBuffer_RESPONSE);
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to send a message to the message buffer by making a POST request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages
     *@param messageBufferName Name of the message buffer
     *@param messageStr Message to be sent
     * */
    public void sendMessage(String messageBufferName, String messageStr) throws AppFabricException {
        this.sendMessage(messageBufferName, messageStr, DefaultSendTimeout);
    }

    /**
     *Function to send a message to the message buffer by making a POST request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages
     *@param messageBufferName Name of the message buffer
     *@param messageStr Message to be sent
     *@param timeout timeout duration to be sent
     * */
    public void sendMessage(String messageBufferName, String messageStr, String timeout) throws AppFabricException {
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String sendPath = MessageBufferConstants.getPATH_FOR_SEND_MESSAGE();
        String timeOutParameter = MessageBufferConstants.getTIMEOUTPARAMETER();
        String messageBufferUri = msgBufferUtilObj.getMessageUri(messageBufferName, sendPath);
        String message = msgBufferUtilObj.getFormattedMessage(messageStr);
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            authorizationToken = tp.getACSToken(requestUri, messageBufferUri);
            messageBufferUri = messageBufferUri.replaceAll("http", "https");
            String sendUri = messageBufferUri + "?" + timeOutParameter + "=" + timeout;
            URL urlConn = new URL(sendUri);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_TEXT());
            connection.setRequestProperty("Content-Length", "" + message.length());
            connection.setRequestProperty("Expect", "100-continue");
            connection.setRequestProperty("Accept", "*/*");
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(message);
            wr.flush();
            wr.close();
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.SendMessage_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if (!((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_ACCEPTED) || (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_CREATED) || (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK))) {
                throw new AppFabricException("Message could not be sent. Error.Response code: " + connection.getResponseCode());
            }
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.SendMessage_RESPONSE);
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to gets the first unlocked message and locks it. It returns message content, message URI, lock duration and lock URIby making a 
     *POST request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages/head
     *@param messageBufferName Name of the message buffer   
     * */
    public LockedMessageInfo peekLock(String messageBufferName) throws AppFabricException {
        return this.peekLock(messageBufferName, DefaultReceiveTimeout);
    }

    /**
     *Function to gets the first unlocked message and locks it. It returns message content, message URI, lock duration and lock URIby making a 
     *POST request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages/head
     *@param messageBufferName Name of the message buffer
     *@param timeout timeout value
     * */
    public LockedMessageInfo peekLock(String messageBufferName, String timeout) throws AppFabricException {
        return this.peekLock(messageBufferName, timeout, DefaultLockDuration);
    }

    /**
     *Function to gets the first unlocked message and locks it. It returns message content, message URI, lock duration and lock URIby making a 
     *POST request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages/head
     *@param messageBufferName Name of the message buffer
     *@param timeout timeout value
     *@param lockDuration lockDuration value
     * */
    public LockedMessageInfo peekLock(String messageBufferName, String timeout, String lockDuration) throws AppFabricException {
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String sendPath = MessageBufferConstants.getPATH_FOR_RETRIEVE_MESSAGE();
        String timeOutParameter = MessageBufferConstants.getTIMEOUTPARAMETER();
        String lockDurationParameter = MessageBufferConstants.getLOCKDURATIONPARAMETER();
        String messageBufferUri = msgBufferUtilObj.getMessageUri(messageBufferName, sendPath);
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            authorizationToken = tp.getACSToken(requestUri, messageBufferUri);
            String tempMessageBufferUri = messageBufferUri;
            tempMessageBufferUri = tempMessageBufferUri.replaceAll("http", "https");
            String retrieveUrl = tempMessageBufferUri + "?" + timeOutParameter + "=" + timeout + "&" + lockDurationParameter + "=" + lockDuration;
            URL urlConn = new URL(retrieveUrl);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_TEXT());
            connection.setRequestProperty("Expect", "100-continue");
            connection.setRequestProperty("Accept", "*/*");
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.flush();
            wr.close();
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.PeekLock_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if ((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_ACCEPTED) || (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_CREATED) || (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK)) {
                String uriString = connection.getHeaderField("X-MS-MESSAGE-LOCATION");
                if (uriString == null) {
                    throw new AppFabricException("Unable to retrieve 'X-MS-MESSAGE-LOCATION' header from message.");
                }
                URL messageUri = new URL(uriString);
                String lockId = connection.getHeaderField("X-MS-LOCK-ID");
                if (lockId == null) {
                    throw new AppFabricException("Unable to retrieve 'X-MS-LOCK-ID' header from message.");
                }
                String lockLocation = connection.getHeaderField("X-MS-LOCK-LOCATION");
                if (lockLocation == null) {
                    throw new AppFabricException("Unable to retrieve 'X-MS-LOCK-LOCATION' header from message.");
                }
                if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(responseCode, SDKLoggerHelper.RecordType.PeekLock_REQUEST);
                return new LockedMessageInfo(lockId, new URL(lockLocation), messageUri);
            } else if (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_NORESPONSE) {
                if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.PeekLock_REQUEST);
                throw new AppFabricException("No content");
            } else {
                if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.PeekLock_REQUEST);
                throw new AppFabricException("Message could not be PeekLocked. Error .... Response code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to delete locked message from the message buffer by making a DELETE request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messageid/lockId    
     *@param messageUri Value of message uri
     *@param lockId Value of lockId
     * */
    public void deleteLockedMessage(URL messageUri, String lockId) throws AppFabricException {
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String lockIdParameter = MessageBufferConstants.getLOCKIDPARAMETER();
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            String messageUriStr = messageUri.toString();
            authorizationToken = tp.getACSToken(requestUri, messageUriStr.replaceAll("https", "http"));
            String query = messageUriStr + "?" + lockIdParameter + "=" + lockId;
            URL urlConn = new URL(query);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_ATOM_XML());
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.DeleteLockedMessage_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if ((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK)) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
            } else {
                throw new AppFabricException("Messages could not be deleted.");
            }
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.DeleteLockedMessage_RESPONSE);
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to delete locked message from the message buffer by making a DELETE request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messageid/lockId    
     *@param lockUri Value of lock uri 
     * */
    public void releaseLock(URL lockUri) throws AppFabricException {
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            String lockUriStr = lockUri.toString().replaceAll("https", "http");
            authorizationToken = tp.getACSToken(requestUri, lockUriStr);
            lockUriStr = lockUriStr.replaceAll("http", "https");
            URL urlConn = new URL(lockUriStr);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_ATOM_XML());
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.ReleaseLock_REQUEST);
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if ((connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK)) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
            } else {
                throw new AppFabricException("Lock could not be released.Error.... Response code:  " + connection.getResponseCode());
            }
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.ReleaseLock_RESOPONSE);
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }

    /**
     *Function to retrieve a message from the message buffer by making a DELETE request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages/head
     *@param messageBufferName Name of the message buffer
     * */
    public String retrieveMessage(String messageBufferName) throws AppFabricException {
        return this.retrieveMessage(messageBufferName, DefaultReceiveTimeout);
    }

    /**
     *Function to retrieve a message from the message buffer by making a DELETE request to https://solution_name.servicebus.windows-bvt.net/messagebuffer_name/messages/head
     *@param messageBufferName Name of the message buffer
     *@param timeout timeout duration 
     * */
    public String retrieveMessage(String messageBufferName, String timeout) throws AppFabricException {
        MessageBufferUtil msgBufferUtilObj = new MessageBufferUtil(solutionName, TokenConstants.getSimpleAuthAuthenticationType());
        String requestUri = msgBufferUtilObj.getRequestUri();
        String sendPath = MessageBufferConstants.getPATH_FOR_RETRIEVE_MESSAGE();
        String timeOutParameter = MessageBufferConstants.getTIMEOUTPARAMETER();
        String messageBufferUri = msgBufferUtilObj.getMessageUri(messageBufferName, sendPath);
        String authorizationToken = "";
        try {
            ACSTokenProvider tp = new ACSTokenProvider(httpWebProxyServer_, httpWebProxyPort_, this.credentials);
            authorizationToken = tp.getACSToken(requestUri, messageBufferUri);
            messageBufferUri = messageBufferUri.replaceAll("http", "https");
            String retrieveUrl = messageBufferUri + "?" + timeOutParameter + "=" + timeout;
            URL urlConn = new URL(retrieveUrl);
            HttpURLConnection connection;
            if (httpWebProxy_ != null) connection = (HttpURLConnection) urlConn.openConnection(httpWebProxy_); else connection = (HttpURLConnection) urlConn.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-type", MessageBufferConstants.getCONTENT_TYPE_PROPERTY_FOR_TEXT());
            String authStr = TokenConstants.getWrapAuthenticationType() + " " + TokenConstants.getWrapAuthorizationHeaderKey() + "=\"" + authorizationToken + "\"";
            connection.setRequestProperty("Authorization", authStr);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logRequest(connection, SDKLoggerHelper.RecordType.GetMessage_REQUEST);
            connection.connect();
            String responseCode = "<responseCode>" + connection.getResponseCode() + "</responseCode>";
            if (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_OK) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();
                if (LoggerUtil.getIsLoggingOn()) {
                    StringBuilder responseXML = new StringBuilder();
                    responseXML.append(responseCode);
                    responseXML.append(response.toString());
                    SDKLoggerHelper.logMessage(URLEncoder.encode(responseXML.toString(), "UTF-8"), SDKLoggerHelper.RecordType.GetMessage_RESPONSE);
                }
                return response.toString();
            } else if (connection.getResponseCode() == MessageBufferConstants.HTTP_STATUS_CODE_NORESPONSE) {
                if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.GetMessage_RESPONSE);
                throw new AppFabricException("No content");
            } else {
                if (LoggerUtil.getIsLoggingOn()) SDKLoggerHelper.logMessage(URLEncoder.encode(responseCode, "UTF-8"), SDKLoggerHelper.RecordType.GetMessage_RESPONSE);
                throw new AppFabricException("Message could not be retrieved . Response code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            throw new AppFabricException(e.getMessage());
        }
    }
}
