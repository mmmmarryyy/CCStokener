package net.jxta.impl.endpoint.servlethttp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import net.jxta.logging.Logging;
import java.util.logging.Logger;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.impl.endpoint.BlockingMessenger;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;
import net.jxta.impl.util.TimeUtils;

/**
 *  Simple messenger that simply posts a message to a URL.
 *
 *  <p/>URL/HttpURLConnection is used, so (depending on your JDK) you will get
 *  reasonably good persistent connection management.
 */
final class HttpClientMessenger extends BlockingMessenger {

    /**
     *  Logger
     */
    private static final transient Logger LOG = Logger.getLogger(HttpClientMessenger.class.getName());

    /**
     *  Minimum amount of time between poll
     */
    private static final int MIMIMUM_POLL_INTERVAL = (int) (5 * TimeUtils.ASECOND);

    /**
     *  Amount of time to wait for connections to open.
     */
    private static final int CONNECT_TIMEOUT = (int) (15 * TimeUtils.ASECOND);

    /**
     *  Amount of time we are willing to wait for responses. This is the amount
     *  of time between our finishing sending a message or beginning a poll and 
     *  the beginning of receipt of a response.
     */
    private static final int RESPONSE_TIMEOUT = (int) (2 * TimeUtils.AMINUTE);

    /**
     *  Amount of time we are willing to accept for additional responses. This 
     *  is the total amount of time we are willing to wait after receiving an
     *  initial response message whether additional responses are sent or not.
     *  This setting governs the latency with which we switch back and forth 
     *  between sending and receiving messages. 
     */
    private static final int EXTRA_RESPONSE_TIMEOUT = (int) (2 * TimeUtils.AMINUTE);

    /**
     *  Messenger idle timeout.
     */
    private static final long MESSENGER_IDLE_TIMEOUT = 15 * TimeUtils.AMINUTE;

    /**
     *  Number of attempts we will attempt to make connections.
     */
    private static final int CONNECT_RETRIES = 2;

    /**
     *  Warn only once about obsolete proxies.
     */
    private static boolean neverWarned = true;

    /**
     *  The URL we send messages to.
     */
    private final URL senderURL;

    /**
     * The ServletHttpTransport that created this object.
     */
    private final ServletHttpTransport servletHttpTransport;

    /**
     *  The Return Address element we will add to all messages we send.
     */
    private final MessageElement srcAddressElement;

    /**
     *  The logical destination address of this messenger.
     */
    private final EndpointAddress logicalDest;

    private TransportBindingMeter transportBindingMeter;

    /**
     *  The last time at which we successfully received or sent a message.
     */
    private transient long lastUsed = TimeUtils.timeNow();

    /**
     *  Poller that we use to get our messages.
     */
    private MessagePoller poller = null;

    /**
     *  Constructs the messenger.
     *
     *  @param servletHttpTransport The transport this messenger will work for.
     *  @param srcAddr The source address.
     *  @param destAddr The destination address.
     */
    HttpClientMessenger(ServletHttpTransport servletHttpTransport, EndpointAddress srcAddr, EndpointAddress destAddr) throws IOException {
        super(servletHttpTransport.getEndpointService().getGroup().getPeerGroupID(), destAddr, true);
        this.servletHttpTransport = servletHttpTransport;
        EndpointAddress srcAddress = srcAddr;
        this.srcAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, srcAddr.toString(), null);
        String protoAddr = destAddr.getProtocolAddress();
        String host;
        int port;
        int lastColon = protoAddr.lastIndexOf(':');
        if ((-1 == lastColon) || (lastColon < protoAddr.lastIndexOf(']')) || ((lastColon + 1) == protoAddr.length())) {
            host = protoAddr;
            port = 80;
        } else {
            host = protoAddr.substring(0, lastColon);
            port = Integer.parseInt(protoAddr.substring(lastColon + 1));
        }
        senderURL = new URL("http", host, port, "/");
        logicalDest = retreiveLogicalDestinationAddress();
        poller = new MessagePoller(srcAddr.getProtocolAddress(), destAddr);
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("New messenger : " + this);
        }
    }

    /**
     *  {@inheritDoc}
     *  <p/>
     *  A simple implementation for debugging. <b>Do not parse the String
     *  returned. All of the information is available in other (simpler) ways.</b>
     */
    public String toString() {
        StringBuilder result = new StringBuilder(super.toString());
        result.append(" {");
        result.append(getDestinationAddress());
        result.append(" / ");
        result.append(getLogicalDestinationAddress());
        result.append("}");
        return result.toString();
    }

    /**
     *  {@inheritDoc}
     */
    void doShutdown() {
        super.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void closeImpl() {
        if (isClosed()) {
            return;
        }
        super.close();
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Close messenger to " + senderURL);
        }
        MessagePoller stopPoller = poller;
        poller = null;
        if (null != stopPoller) {
            stopPoller.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessageBImpl(Message message, String service, String serviceParam) throws IOException {
        if (isClosed()) {
            IOException failure = new IOException("Messenger was closed, it cannot be used to send messages.");
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Messenger was closed, it cannot be used to send messages.", failure);
            }
            throw failure;
        }
        message = message.clone();
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddressElement);
        EndpointAddress destAddressToUse = getDestAddressToUse(service, serviceParam);
        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddressToUse.toString(), null);
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);
        try {
            doSend(message);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EndpointAddress getLogicalDestinationImpl() {
        return logicalDest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIdleImpl() {
        return isClosed() || (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), lastUsed) > MESSENGER_IDLE_TIMEOUT);
    }

    /**
     *  Connects to the http server and retrieves the Logical Destination Address
     */
    private EndpointAddress retreiveLogicalDestinationAddress() throws IOException {
        long beginConnectTime = 0;
        long connectTime = 0;
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Ping (" + senderURL + ")");
        }
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            beginConnectTime = TimeUtils.timeNow();
        }
        HttpURLConnection urlConn = (HttpURLConnection) senderURL.openConnection();
        urlConn.setRequestMethod("GET");
        urlConn.setDoOutput(true);
        urlConn.setDoInput(true);
        urlConn.setAllowUserInteraction(false);
        urlConn.setUseCaches(false);
        urlConn.setConnectTimeout(CONNECT_TIMEOUT);
        urlConn.setReadTimeout(CONNECT_TIMEOUT);
        try {
            int code = urlConn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                    transportBindingMeter = servletHttpTransport.getTransportBindingMeter(null, getDestinationAddress());
                    if (transportBindingMeter != null) {
                        transportBindingMeter.connectionFailed(true, TimeUtils.timeNow() - beginConnectTime);
                    }
                }
                throw new IOException("Message not accepted: HTTP status " + "code=" + code + " reason=" + urlConn.getResponseMessage());
            }
            int msglength = urlConn.getContentLength();
            if (msglength <= 0) {
                throw new IOException("Ping response was empty.");
            }
            InputStream inputStream = urlConn.getInputStream();
            byte[] uniqueIdBytes = new byte[msglength];
            int bytesRead = 0;
            while (bytesRead < msglength) {
                int thisRead = inputStream.read(uniqueIdBytes, bytesRead, msglength - bytesRead);
                if (thisRead < 0) {
                    break;
                }
                bytesRead += thisRead;
            }
            if (bytesRead < msglength) {
                throw new IOException("Content ended before promised Content length");
            }
            String uniqueIdString;
            try {
                uniqueIdString = new String(uniqueIdBytes, "UTF-8");
            } catch (UnsupportedEncodingException never) {
                uniqueIdString = new String(uniqueIdBytes);
            }
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                connectTime = TimeUtils.timeNow();
                transportBindingMeter = servletHttpTransport.getTransportBindingMeter(uniqueIdString, getDestinationAddress());
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionEstablished(true, connectTime - beginConnectTime);
                    transportBindingMeter.ping(connectTime);
                    transportBindingMeter.connectionClosed(true, connectTime - beginConnectTime);
                }
            }
            EndpointAddress remoteAddress = new EndpointAddress("jxta", uniqueIdString.trim(), null, null);
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Ping (" + senderURL + ") -> " + remoteAddress);
            }
            return remoteAddress;
        } catch (IOException failure) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                connectTime = TimeUtils.timeNow();
                transportBindingMeter = servletHttpTransport.getTransportBindingMeter(null, getDestinationAddress());
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(true, connectTime - beginConnectTime);
                }
            }
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Ping (" + senderURL + ") -> failed");
            }
            throw failure;
        }
    }

    /**
     *  Connects to the http server and POSTs the message
     */
    private void doSend(Message msg) throws IOException {
        long beginConnectTime = 0;
        long connectTime = 0;
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            beginConnectTime = TimeUtils.timeNow();
        }
        WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, EndpointServiceImpl.DEFAULT_MESSAGE_TYPE, null);
        for (int connectAttempt = 1; connectAttempt <= CONNECT_RETRIES; connectAttempt++) {
            if (connectAttempt > 1) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Retrying connection to " + senderURL);
                }
            }
            HttpURLConnection urlConn = (HttpURLConnection) senderURL.openConnection();
            try {
                urlConn.setRequestMethod("POST");
                urlConn.setDoOutput(true);
                urlConn.setDoInput(true);
                urlConn.setAllowUserInteraction(false);
                urlConn.setUseCaches(false);
                urlConn.setConnectTimeout(CONNECT_TIMEOUT);
                urlConn.setReadTimeout(CONNECT_TIMEOUT);
                urlConn.setRequestProperty("content-length", Long.toString(serialed.getByteLength()));
                urlConn.setRequestProperty("content-type", serialed.getMimeType().toString());
                OutputStream out = urlConn.getOutputStream();
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    connectTime = TimeUtils.timeNow();
                    transportBindingMeter.connectionEstablished(true, connectTime - beginConnectTime);
                }
                serialed.sendToStream(out);
                out.flush();
                int responseCode;
                try {
                    responseCode = urlConn.getResponseCode();
                } catch (SocketTimeoutException expired) {
                    continue;
                } catch (IOException ioe) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("HTTP 1.0 proxy seems in use");
                    }
                    continue;
                }
                if (responseCode == -1) {
                    if (neverWarned && Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.warning("Obsolete HTTP proxy does not issue HTTP_OK response. Assuming OK");
                        neverWarned = false;
                    }
                    responseCode = HttpURLConnection.HTTP_OK;
                }
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                        transportBindingMeter.dataSent(true, serialed.getByteLength());
                        transportBindingMeter.connectionDropped(true, TimeUtils.timeNow() - beginConnectTime);
                    }
                    throw new IOException("Message not accepted: HTTP status " + "code=" + responseCode + " reason=" + urlConn.getResponseMessage());
                }
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                    long messageSentTime = TimeUtils.timeNow();
                    transportBindingMeter.messageSent(true, msg, messageSentTime - connectTime, serialed.getByteLength());
                    transportBindingMeter.connectionClosed(true, messageSentTime - beginConnectTime);
                }
                lastUsed = TimeUtils.timeNow();
                return;
            } finally {
                urlConn.disconnect();
            }
        }
        throw new IOException("Failed sending " + msg + " to " + senderURL);
    }

    /**
     *  Polls for messages sent to us.
     */
    private class MessagePoller implements Runnable {

        /**
         *  If <tt>true</tt> then this poller is stopped or stopping.
         */
        private volatile boolean stopped = false;

        /**
         *  The thread that does the work.
         */
        private Thread pollerThread;

        /**
         *  The URL we poll for messages.
         */
        private final URL pollingURL;

        MessagePoller(String pollAddress, EndpointAddress destAddr) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("new MessagePoller for " + senderURL);
            }
            try {
                pollingURL = new URL(senderURL, "/" + pollAddress + "?" + Integer.toString(RESPONSE_TIMEOUT) + "," + Integer.toString(EXTRA_RESPONSE_TIMEOUT) + "," + destAddr);
            } catch (MalformedURLException badAddr) {
                IllegalArgumentException failure = new IllegalArgumentException("Could not construct polling URL");
                failure.initCause(badAddr);
                throw failure;
            }
            pollerThread = new Thread(this, "HttpClientMessenger poller for " + senderURL);
            pollerThread.setDaemon(true);
            pollerThread.start();
        }

        protected void stop() {
            if (stopped) {
                return;
            }
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Stop polling for " + senderURL);
            }
            stopped = true;
            Thread stopPoller = pollerThread;
            if (null != stopPoller) {
                stopPoller.interrupt();
            }
        }

        /**
         *  Returns {@code true} if this messenger is stopped otherwise 
         *  {@code false}.
         *
         *  @return returns {@code true} if this messenger is stopped otherwise 
         *  {@code false}.
         */
        protected boolean isStopped() {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(this + " " + senderURL + " --> " + (stopped ? "stopped" : "running"));
            }
            return stopped;
        }

        /**
         *  {@inheritDoc}
         *
         *  <p/>Connects to the http server and waits for messages to be received and processes them.
         */
        public void run() {
            try {
                long beginConnectTime = 0;
                long connectTime = 0;
                long noReconnectBefore = 0;
                HttpURLConnection conn = null;
                if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                    LOG.info("Message polling beings for " + pollingURL);
                }
                int connectAttempt = 1;
                while (!isStopped()) {
                    if (conn == null) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Opening new connection to " + pollingURL);
                        }
                        conn = (HttpURLConnection) pollingURL.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setDoOutput(false);
                        conn.setDoInput(true);
                        conn.setAllowUserInteraction(false);
                        conn.setUseCaches(false);
                        conn.setConnectTimeout(CONNECT_TIMEOUT);
                        conn.setReadTimeout(RESPONSE_TIMEOUT);
                        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                            beginConnectTime = TimeUtils.timeNow();
                        }
                        continue;
                    }
                    long untilNextConnect = TimeUtils.toRelativeTimeMillis(noReconnectBefore);
                    try {
                        if (untilNextConnect > 0) {
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Delaying for " + untilNextConnect + "ms before reconnect to " + senderURL);
                            }
                            Thread.sleep(untilNextConnect);
                        }
                    } catch (InterruptedException woken) {
                        Thread.interrupted();
                        continue;
                    }
                    InputStream inputStream;
                    MimeMediaType messageType;
                    try {
                        if (connectAttempt > 1) {
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Reconnect attempt for " + senderURL);
                            }
                        }
                        conn.connect();
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Waiting for response code from " + senderURL);
                        }
                        int responseCode = conn.getResponseCode();
                        if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                            LOG.finer("Response " + responseCode + " for Connection : " + senderURL + "\n\tContent-Type : " + conn.getHeaderField("Content-Type") + "\tContent-Length : " + conn.getHeaderField("Content-Length") + "\tTransfer-Encoding : " + conn.getHeaderField("Transfer-Encoding"));
                        }
                        connectTime = TimeUtils.timeNow();
                        noReconnectBefore = TimeUtils.toAbsoluteTimeMillis(MIMIMUM_POLL_INTERVAL, connectTime);
                        if (0 == conn.getContentLength()) {
                            continue;
                        }
                        if (HttpURLConnection.HTTP_NO_CONTENT == responseCode) {
                            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                                transportBindingMeter.connectionClosed(true, TimeUtils.toRelativeTimeMillis(beginConnectTime, connectTime));
                            }
                            conn = null;
                            continue;
                        }
                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                                transportBindingMeter.connectionClosed(true, TimeUtils.timeNow() - beginConnectTime);
                            }
                            throw new IOException("HTTP Failure: " + conn.getResponseCode() + " : " + conn.getResponseMessage());
                        }
                        String contentType = conn.getHeaderField("Content-Type");
                        if (null == contentType) {
                            messageType = EndpointServiceImpl.DEFAULT_MESSAGE_TYPE;
                        } else {
                            messageType = MimeMediaType.valueOf(contentType);
                        }
                        inputStream = conn.getInputStream();
                        connectAttempt = 1;
                    } catch (InterruptedIOException broken) {
                        Thread.interrupted();
                        if (connectAttempt > CONNECT_RETRIES) {
                            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                                LOG.warning("Unable to connect to " + senderURL);
                            }
                            stop();
                            break;
                        } else {
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Failed connecting to " + senderURL);
                            }
                            if (null != conn) {
                                conn.disconnect();
                            }
                            conn = null;
                            connectAttempt++;
                            continue;
                        }
                    } catch (IOException ioe) {
                        if (connectAttempt > CONNECT_RETRIES) {
                            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                                LOG.log(Level.WARNING, "Unable to connect to " + senderURL, ioe);
                            }
                            stop();
                            break;
                        } else {
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Failed connecting to " + senderURL);
                            }
                            if (null != conn) {
                                conn.disconnect();
                            }
                            conn = null;
                            connectAttempt++;
                            continue;
                        }
                    }
                    try {
                        while (!isStopped() && (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), connectTime) < RESPONSE_TIMEOUT)) {
                            long messageReceiveStart = TimeUtils.timeNow();
                            Message incomingMsg;
                            incomingMsg = WireFormatMessageFactory.fromWire(inputStream, messageType, null);
                            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                                transportBindingMeter.messageReceived(true, incomingMsg, incomingMsg.getByteLength(), TimeUtils.timeNow() - messageReceiveStart);
                            }
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Received " + incomingMsg + " from " + senderURL);
                            }
                            servletHttpTransport.executor.execute(new MessageProcessor(incomingMsg));
                            lastUsed = TimeUtils.timeNow();
                        }
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.connectionClosed(true, TimeUtils.timeNow() - beginConnectTime);
                        }
                    } catch (EOFException e) {
                        conn = null;
                    } catch (InterruptedIOException broken) {
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.connectionDropped(true, TimeUtils.timeNow() - beginConnectTime);
                        }
                        Thread.interrupted();
                        if (null != conn) {
                            conn.disconnect();
                        }
                        conn = null;
                    } catch (IOException e) {
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.connectionDropped(true, TimeUtils.timeNow() - beginConnectTime);
                        }
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Failed to read message from " + senderURL, e);
                        }
                        stop();
                        break;
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            } catch (Throwable argh) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Poller exiting because of uncaught exception", argh);
                }
                stop();
            } finally {
                pollerThread = null;
            }
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Message polling stopped for " + senderURL);
            }
        }
    }

    /**
     * A small class for processing individual messages. 
     */
    private class MessageProcessor implements Runnable {

        private Message msg;

        MessageProcessor(Message msg) {
            this.msg = msg;
        }

        public void run() {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Demuxing " + msg + " from " + senderURL);
            }
            servletHttpTransport.getEndpointService().demux(msg);
        }
    }
}
