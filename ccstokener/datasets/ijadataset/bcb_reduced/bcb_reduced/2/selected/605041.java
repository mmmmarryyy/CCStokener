package avoware.zimbra.tray;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 *
 * @author Andrew Orlov
 */
public class ZimbraSOAP {

    public static SOAPMessage newInstance() throws SOAPException {
        MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return mf.createMessage();
    }

    public static SOAPMessage call(SOAPMessage request, URL url) throws IOException, SOAPException {
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.connect();
        request.writeTo(conn.getOutputStream());
        MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return mf.createMessage(null, conn.getInputStream());
    }
}
