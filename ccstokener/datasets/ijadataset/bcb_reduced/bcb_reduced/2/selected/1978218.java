package org.marre.mms.transport.mm1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import org.marre.mime.MimeBodyPart;
import org.marre.mms.MmsException;
import org.marre.mms.MmsHeaders;
import org.marre.mms.transport.MmsTransport;
import org.marre.util.IOUtil;
import org.marre.util.StringUtil;

/**
 * Sends mms using the mm1 protocol. 
 * 
 * @author Markus Eriksson
 * @version $Id: Mm1Transport.java 410 2006-03-13 19:48:31Z c95men $
 */
public class Mm1Transport implements MmsTransport {

    private static Logger log_ = LoggerFactory.getLogger(Mm1Transport.class);

    /** 
     * Content type for a mms message. 
     */
    public static final String CONTENT_TYPE_WAP_MMS_MESSAGE = "application/vnd.wap.mms-message";

    /**
     * URL for the proxy gateway
     */
    private String mmsProxyGatewayAddress_;

    /**
     * @see org.marre.mms.transport.MmsTransport#init(java.util.Properties)
     */
    public void init(Properties properties) throws MmsException {
        mmsProxyGatewayAddress_ = properties.getProperty("smsj.mm1.proxygateway");
        if (mmsProxyGatewayAddress_ == null) {
            throw new MmsException("smsj.mm1.proxygateway not set");
        }
    }

    /**
     * The mm1 protocol is connection less so this method is not used.
     * @see org.marre.mms.transport.MmsTransport#connect()
     */
    public void connect() {
    }

    /**
     * Sends MMS.
     * 
     * @see org.marre.mms.transport.MmsTransport#send(org.marre.mime.MimeBodyPart, org.marre.mms.MmsHeaders)
     */
    public void send(MimeBodyPart message, MmsHeaders headers) throws MmsException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Mm1Encoder.writeMessageToStream(baos, message, headers);
        baos.close();
        if (log_.isDebugEnabled()) {
            String str = StringUtil.bytesToHexString(baos.toByteArray());
            log_.debug("request [" + str + "]");
        }
        URL url = new URL(mmsProxyGatewayAddress_);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.addRequestProperty("Content-Length", "" + baos.size());
        urlConn.addRequestProperty("Content-Type", CONTENT_TYPE_WAP_MMS_MESSAGE);
        urlConn.setDoOutput(true);
        urlConn.setDoInput(true);
        urlConn.setAllowUserInteraction(false);
        OutputStream out = urlConn.getOutputStream();
        baos.writeTo(out);
        out.flush();
        out.close();
        baos.reset();
        baos = new ByteArrayOutputStream();
        InputStream response = urlConn.getInputStream();
        int responsecode = urlConn.getResponseCode();
        log_.debug("HTTP response code : " + responsecode);
        IOUtil.copy(response, baos);
        baos.close();
        if (log_.isDebugEnabled()) {
            String str = StringUtil.bytesToHexString(baos.toByteArray());
            log_.debug("response [" + str + "]");
        }
    }

    /**
     * The mm1 protocol is connection less so this method is not used.
     * @see org.marre.mms.transport.MmsTransport#disconnect()
     */
    public void disconnect() {
    }
}
