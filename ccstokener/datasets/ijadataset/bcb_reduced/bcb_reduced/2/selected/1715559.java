package org.xactor.test.ws.coordination.endpoint.test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.xactor.test.ws.EndpointUtil;
import org.xactor.ws.Constants;
import org.xactor.ws.IOUtils;
import org.xactor.ws.XMLInputFactorySingleton;
import org.xactor.ws.addressing.element.EndpointReference;

/**
 * WS-Addressing header tests for one way WS-Coordination and WS-AtomicTransaction messages.
 * 
 * @author <a href="mailto:ivanneto@gmail.com">Ivan Neto</a>
 */
public class WSAddressingHeadersUnitTestCase extends TestCase implements Constants {

    /** The port number where the response will arrive. */
    private static final int PORT = 2020;

    /** The size of the buffer that will receive the asynchronous response. */
    private static final int BUFFER_SIZE = 4096;

    /** Logging. */
    private static Logger log = Logger.getLogger(WSAddressingHeadersUnitTestCase.class);

    /** Logging. */
    private static Logger msgLog = Logger.getLogger("jbossws.SOAPMessage");

    /** The StAX XML input factory. */
    private static XMLInputFactory factory = XMLInputFactorySingleton.getSingleton();

    /** The response returned by the asynchronous Web service. */
    private static String asyncResponse;

    /** The <code>EndpointUtil</code>. */
    private static EndpointUtil endpointUtil = EndpointUtil.getHost0EndpointUtil();

    public void testSuccessfulResponseHeaders() throws Exception {
        EndpointReference targetEndpoint = endpointUtil.getParticipantEpr();
        HttpURLConnection connection = openConnection(targetEndpoint);
        String targetAddress = targetEndpoint.getAddress().getValue();
        String hostName = InetAddress.getLocalHost().getHostName();
        String replyToAddress = "http://" + hostName + ":" + PORT;
        String reqMessage = "<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/' " + "xmlns:jbosswscoor='http://www.jboss.org/wscoor/extension' " + "xmlns:wsa='http://schemas.xmlsoap.org/ws/2004/08/addressing' " + "xmlns:wsat='http://schemas.xmlsoap.org/ws/2004/10/wsat'><env:Header>" + "<jbosswscoor:CoordinatedActivityID>1</jbosswscoor:CoordinatedActivityID>" + "<wsa:MessageID>uuid:a94fc653-3b22-4902-bac4-4fcc14a700c4</wsa:MessageID><wsa:To>" + targetAddress + "</wsa:To><wsa:Action>" + WSAT.Actions.COMMIT + "</wsa:Action>" + "<wsa:ReplyTo><wsa:Address>" + replyToAddress + "</wsa:Address>" + "</wsa:ReplyTo></env:Header><env:Body><wsat:Commit/></env:Body></env:Envelope>";
        openServerSocketInANewThreadAndWaitForTheHTTPResponse();
        String httpResponse = sendMessage(connection, reqMessage);
        assertEquals("The HTTP response for one way messages should be empty.", "", httpResponse);
        assertNotNull("The asynchronous response should not be null", asyncResponse);
        int firstElementIndex = asyncResponse.indexOf("<");
        String resMessage = asyncResponse.substring(firstElementIndex);
        String expectedAction = WSAT.Actions.COMMITTED;
        assertWSAddressingHeaders(resMessage, replyToAddress, expectedAction, false);
    }

    public void testFaultTo() throws Exception {
        EndpointReference targetEndpoint = endpointUtil.getCompletionEpr();
        HttpURLConnection connection = openConnection(targetEndpoint);
        String targetAddress = targetEndpoint.getAddress().getValue();
        String hostName = InetAddress.getLocalHost().getHostName();
        String replyToAddress = "http://someNonExistentAddress";
        String faultToAddress = "http://" + hostName + ":" + PORT;
        String reqMessage = "<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/' " + "xmlns:jbosswscoor='http://www.jboss.org/wscoor/extension' " + "xmlns:wsa='http://schemas.xmlsoap.org/ws/2004/08/addressing' " + "xmlns:wsat='http://schemas.xmlsoap.org/ws/2004/10/wsat'><env:Header>" + "<jbosswscoor:CoordinatedActivityID>1</jbosswscoor:CoordinatedActivityID>" + "<wsa:MessageID>uuid:a94fc653-3b22-4902-bac4-4fcc14a700c4</wsa:MessageID><wsa:To>" + targetAddress + "</wsa:To><wsa:Action>" + WSAT.Actions.COMMIT + "</wsa:Action>" + "<wsa:ReplyTo><wsa:Address>" + replyToAddress + "</wsa:Address></wsa:ReplyTo>" + "<wsa:FaultTo><wsa:Address>" + faultToAddress + "</wsa:Address></wsa:FaultTo>" + "</env:Header><env:Body><wsat:Commit/></env:Body></env:Envelope>";
        openServerSocketInANewThreadAndWaitForTheHTTPResponse();
        String httpResponse = sendMessage(connection, reqMessage);
        assertEquals("The HTTP response for one way messages should be empty.", "", httpResponse);
        assertNotNull("The asynchronous response should not be null", asyncResponse);
        int firstElementIndex = asyncResponse.indexOf("<");
        String resMessage = asyncResponse.substring(firstElementIndex);
        String expectedAction = WSAT.Actions.FAULT;
        assertWSAddressingHeaders(resMessage, faultToAddress, expectedAction, true);
    }

    private HttpURLConnection openConnection(EndpointReference targetEndpoint) throws MalformedURLException, IOException {
        URL url = new URL(targetEndpoint.getAddress().getValue());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        return connection;
    }

    private String sendMessage(HttpURLConnection connection, String reqMessage) throws IOException {
        if (msgLog.isTraceEnabled()) msgLog.trace("Outgoing SOAPMessage\n" + reqMessage);
        BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
        out.write(reqMessage.getBytes("UTF-8"));
        out.close();
        InputStream inputStream = null;
        if (connection.getResponseCode() < 400) inputStream = connection.getInputStream(); else inputStream = connection.getErrorStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        IOUtils.copyStream(baos, inputStream);
        inputStream.close();
        String response = new String(baos.toByteArray(), "UTF-8");
        if (msgLog.isTraceEnabled()) msgLog.trace("Incoming Response SOAPMessage\n" + response);
        return response;
    }

    private Thread openServerSocketInANewThreadAndWaitForTheHTTPResponse() throws IOException, InterruptedException {
        final ServerSocket serverSocket = new ServerSocket(PORT);
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    byte[] b = new byte[BUFFER_SIZE];
                    StringBuffer request = new StringBuffer(BUFFER_SIZE);
                    do {
                        int i = in.read(b);
                        for (int j = 0; j < i; j++) request.append((char) b[j]);
                    } while (!request.toString().endsWith("</env:Envelope>"));
                    asyncResponse = request.toString();
                    log.debug("Incoming HTTP response:\n" + asyncResponse);
                    serverSocket.close();
                    OutputStream out = socket.getOutputStream();
                    out.write("HTTP/1.1 \n\n".getBytes());
                    in.close();
                    out.close();
                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
        Thread.sleep(1000);
        return t;
    }

    private void assertWSAddressingHeaders(String resMessage, String expectedTo, String expectedAction, boolean isFault) throws XMLStreamException, UnsupportedEncodingException {
        XMLStreamReader parser = null;
        boolean inHeader = false;
        boolean foundRelatesTo = false;
        boolean foundTo = false;
        boolean foundAction = false;
        try {
            byte[] byteArray = resMessage.getBytes("UTF-8");
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
            parser = factory.createXMLStreamReader(bais, "UTF-8");
            for (int ev = parser.next(); ev != XMLStreamConstants.END_DOCUMENT; ev = parser.next()) {
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    QName qname = parser.getName();
                    if (SOAP11.HEADER.equals(qname)) {
                        inHeader = true;
                    } else if (inHeader && WSAddr.Elements.RELATES_TO.equals(qname)) {
                        foundRelatesTo = true;
                    } else if (inHeader && WSAddr.Elements.TO.equals(qname)) {
                        foundTo = true;
                        String to = parser.getElementText();
                        assertEquals("Wrong wsa:To value: " + to, expectedTo, to);
                    } else if (inHeader && WSAddr.Elements.ACTION.equals(qname)) {
                        foundAction = true;
                        String action = parser.getElementText();
                        assertEquals("Wrong wsa:Action value: " + action, expectedAction, action);
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    QName qname = parser.getName();
                    if (SOAP11.HEADER.equals(qname)) {
                        break;
                    }
                }
            }
        } finally {
            if (parser != null) parser.close();
        }
        assertTrue("Could not find wsa:To element", foundTo);
        assertTrue("Could not find wsa:Action element", foundAction);
        if (isFault) assertTrue("Could not find wsa:RelatesTo element", foundRelatesTo);
    }
}
