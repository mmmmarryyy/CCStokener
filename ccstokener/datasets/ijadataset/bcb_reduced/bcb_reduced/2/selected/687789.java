package org.soda.dpws.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.soda.dpws.DPWSException;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.transport.AbstractMessageSender;
import org.soda.dpws.util.STAXUtils;
import org.soda.dpws.util.serialize.XMLSerializer;

/**
 * Sends a message via the JDK HTTP URLConnection. This is very buggy. Drop
 * commons-httpclient on your classpath and XFire will use
 * CommonsHttpMessageSender instead.
 * 
 */
public class SimpleMessageSender extends AbstractMessageSender {

    private HttpURLConnection urlConn;

    private InputStream is;

    /**
   * @param message
   * @param context
   */
    public SimpleMessageSender(OutMessage message, DPWSContextImpl context) {
        super(message, context);
    }

    public void open() throws IOException, DPWSFault {
        URL url = new URL(getUri());
        urlConn = createConnection(url);
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestMethod("POST");
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConn.setRequestProperty("User-Agent", "XFire Client +http://xfire.codehaus.org");
        urlConn.setRequestProperty("Accept", "text/xml; text/html");
        urlConn.setRequestProperty("Content-type", "text/xml; charset=" + getEncoding());
        urlConn.setRequestProperty("SOAPAction", getQuotedSoapAction());
    }

    /**
   * @return current {@link OutputStream}
   * @throws IOException
   * @throws DPWSFault
   */
    public OutputStream getOutputStream() throws IOException, DPWSFault {
        return urlConn.getOutputStream();
    }

    public InMessage getInMessage() throws DPWSException {
        try {
            is = urlConn.getInputStream();
        } catch (IOException ioe) {
            try {
                if (urlConn.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    is = urlConn.getErrorStream();
                }
            } catch (IOException e) {
                throw new DPWSException(e);
            }
        }
        return new InMessage(STAXUtils.createXMLStreamReader(is, getEncoding(), getMessageContext()), getUri());
    }

    public void close() throws DPWSException {
        try {
            if (is != null) is.close();
        } catch (IOException e) {
            throw new DPWSException("Couldn't close stream.", e);
        } finally {
            if (urlConn != null) urlConn.disconnect();
        }
    }

    private HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    public boolean hasResponse() {
        return true;
    }

    public void send() throws IOException, DPWSFault {
        OutputStream out = getOutputStream();
        OutMessage message = getMessage();
        XMLSerializer ser = new XMLSerializer(out, message.getEncoding());
        message.getSerializer().writeMessage(message, ser, getMessageContext());
        out.flush();
        out.close();
    }
}
