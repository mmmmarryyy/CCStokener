package co.za.gvi.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import co.za.gvi.bind.GviSmsMessageType;
import co.za.gvi.handler.DispatchMessageHandler;

/**
 * This class is used to create a <code>Transport</code> to connect to the URL and send the request.
 * <p>
 * Example:
 * <pre>
 * Transport transport = Transport.createTransport(url, affiliateCode, authenticationCode);
 * transport.connect();
 * transport.send(message);
 * transport.disconnect();
 * </pre>
 * 
 * @author Enio Perpetuo
 */
public final class Transport {

    private static final String MESSAGE_TYPE = "text";

    private String url;

    private String affiliateCode;

    private String authenticationCode;

    private HttpURLConnection connection;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
	 * Constructor
	 * 
	 * @param url The URL to which to POST requests
	 * @param affiliateCode The Affiliate Code
	 * @param authenticationCode The Authentication Code
	 */
    private Transport(String url, String affiliateCode, String authenticationCode) {
        this.url = url;
        this.affiliateCode = affiliateCode;
        this.authenticationCode = authenticationCode;
    }

    /**
	 * Creates a Transport
	 * 
	 * @param url The URL to which to POST requests
	 * @param affiliateCode The Affiliate Code
	 * @param authenticationCode The Authentication Code
	 * @return a Transport
	 */
    public static Transport createTransport(String url, String affiliateCode, String authenticationCode) {
        return new Transport(url, affiliateCode, authenticationCode);
    }

    /**
	 * Connects to the URL
	 * 
	 * @throws IOException if an I/O exception occurs.
	 */
    public void connect() throws IOException {
        if (this.connection == null) {
            this.connection = (HttpURLConnection) (new URL(url)).openConnection();
            this.connection.setRequestMethod("POST");
            this.connection.setUseCaches(false);
            this.connection.setDoOutput(true);
        }
    }

    /**
	 * Disconnects to the URL
	 */
    public void disconnect() {
        if (this.connection != null) {
            this.connection.disconnect();
            this.connection = null;
        }
    }

    /**
	 * Posts the request to the URL
	 * 
	 * @param message The message to be sent
	 * 
	 * @throws IOException if an I/O error occurs while creating the output stream.
	 * @throws JAXBException if any unexpected problem occurs during the marshaling.
	 */
    public void send(MessageProxy message) throws IOException, JAXBException {
        JAXBElement<GviSmsMessageType> sms = new JAXBElement<GviSmsMessageType>(new QName("", "gviSmsMessage"), GviSmsMessageType.class, null, message.getMessage());
        this.setProperties(sms.getValue());
        DispatchMessageHandler handler = new DispatchMessageHandler();
        DataOutputStream out = new DataOutputStream(this.connection.getOutputStream());
        handler.marshal(sms, out);
        out.flush();
        out.close();
        this.connection.getInputStream();
    }

    /**
	 * This method is commonly used for testing. 
	 * Instead of connecting to a URL, it prints the message to the standard output stream.
	 * When creating a Transport to use this method it's not necessary to specify a URL.
	 * <p>
	 * Example:
	 * <pre>
	 * Transport transport = Transport.createTransport(null, affiliateCode, authenticationCode);
	 * transport.print(message);
	 * </pre>
	 * 
	 * @param message The message to print
	 * 
	 * @throws IOException if an I/O error occurs while creating the output stream.
	 * @throws JAXBException if any unexpected problem occurs during the marshalling.
	 */
    public void print(MessageProxy message) throws IOException, JAXBException {
        JAXBElement<GviSmsMessageType> sms = new JAXBElement<GviSmsMessageType>(new QName("", "gviSmsMessage"), GviSmsMessageType.class, null, message.getMessage());
        this.setProperties(sms.getValue());
        DispatchMessageHandler handler = new DispatchMessageHandler();
        handler.marshal(sms, System.out);
    }

    /**
	 * Sets the <code>affiliateCode</code>, <code>authenticationCode</code>, <code>messageType</code> and 
	 * <code>submitDateTime</code> properties.
	 * 
	 * @param sms The original message
	 */
    private void setProperties(GviSmsMessageType sms) {
        sms.setAffiliateCode(this.affiliateCode);
        sms.setAuthenticationCode(this.authenticationCode);
        sms.setMessageType(Transport.MESSAGE_TYPE);
        String dateTime = dateFormat.format(new Date()).replace(' ', 'T');
        sms.setSubmitDateTime(dateTime);
    }
}
