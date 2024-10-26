package org.xactor.test.ws.coordination.endpoint.test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.logging.Logger;
import org.xactor.test.ws.AbstractEndpointTestCaseSupport;
import org.xactor.test.ws.EndpointUtil;
import org.xactor.ws.IOUtils;
import org.xactor.ws.XMLInputFactorySingleton;
import org.xactor.ws.addressing.element.AttributedURI;
import org.xactor.ws.addressing.element.EndpointReference;
import org.xactor.ws.atomictx.WSATMessageFactory;
import org.xactor.ws.coordination.WSCoorMessageFactory;
import org.xactor.ws.coordination.element.CoordinationContext;
import org.xactor.ws.coordination.element.CreateCoordinationContext;
import org.xactor.ws.coordination.element.CreateCoordinationContextResponse;

/**
 * Tests for checking the HTTP codes returned by the WS-Coordination and the WS-AtomicTransaction
 * servlets.
 * 
 * @author <a href="mailto:ivanneto@gmail.com">Ivan Neto</a>
 */
public class HTTPCodesUnitTestCase extends AbstractEndpointTestCaseSupport {

    /** Logging. */
    private static Logger msgLog = Logger.getLogger("jbossws.SOAPMessage");

    /** The StAX XML input factory. */
    private static XMLInputFactory factory = XMLInputFactorySingleton.getSingleton();

    /** The <code>EndpointUtil</code>. */
    private EndpointUtil endpointUtil = EndpointUtil.getHost0EndpointUtil();

    public void testSuccessfulResponseHTTPCode() throws Exception {
        EndpointReference targetEndpoint = endpointUtil.getActivationRpcEpr();
        HttpURLConnection connection = openConnection(targetEndpoint);
        URI coordinationType = WSAT.CoordinationTypes.WSAT_COORDINATION_TYPE;
        CreateCoordinationContext param = new CreateCoordinationContext(coordinationType);
        String reqMessage = WSCoorMessageFactory.newCreateCoordinationContextMessage(targetEndpoint, param);
        String resMessage = sendMessage(connection, reqMessage);
        XMLStreamReader parser = factory.createXMLStreamReader(new StringReader(resMessage));
        try {
            CreateCoordinationContextResponse response = new CreateCoordinationContextResponse(parser);
            CoordinationContext ctx = response.getCoordinationContext();
            assertNotNull("Coordination context is null", ctx);
            EndpointReference ref = ctx.getRegistrationService();
            assertNotNull("Registration service is null", ref);
            AttributedURI address = ref.getAddress();
            assertNotNull("Registration service address is null", address);
            endpointUtil.setReferencePropertiesInAllEndpointReferences(ref.getReferenceProperties());
            endpointUtil.commitTransaction();
        } finally {
            parser.close();
        }
        int expectedHttpCode = HttpServletResponse.SC_OK;
        int httpCode = connection.getResponseCode();
        assertEquals("Worng HTTP code: " + httpCode, expectedHttpCode, httpCode);
        connection.disconnect();
    }

    public void testFaultResponseHTTPCode() throws Exception {
        EndpointReference targetEndpoint = endpointUtil.getCompletionRpcEpr();
        HttpURLConnection connection = openConnection(targetEndpoint);
        String reqMessage = WSATMessageFactory.newCompletionCommitMessage(null, null);
        String resMessage = sendMessage(connection, reqMessage);
        XMLStreamReader parser = factory.createXMLStreamReader(new StringReader(resMessage));
        try {
            assertNoTransactionSoapFault(parser, null);
        } finally {
            parser.close();
        }
        int expectedHttpCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        int httpCode = connection.getResponseCode();
        assertEquals("Wrong HTTP code: " + httpCode, expectedHttpCode, httpCode);
        connection.disconnect();
    }

    public void testOneWayResponseHTTPCode() throws Exception {
        EndpointReference targetEndpoint = endpointUtil.getCoordinatorEpr();
        HttpURLConnection connection = openConnection(targetEndpoint);
        String reqMessage = WSATMessageFactory.newCommittedMessage(null);
        String resMessage = sendMessage(connection, reqMessage);
        XMLStreamReader parser = factory.createXMLStreamReader(new StringReader(resMessage));
        try {
            boolean isEmpty = parser.hasText() == false;
            assertTrue("The HTTP response for one way messages should be empty.", isEmpty);
        } finally {
            parser.close();
        }
        int expectedHttpCode = HttpServletResponse.SC_ACCEPTED;
        int httpCode = connection.getResponseCode();
        assertEquals("Worng HTTP code: " + httpCode, expectedHttpCode, httpCode);
        connection.disconnect();
    }

    public void testOneWayFaultHTTPCode() throws Exception {
        EndpointReference targetEndpoint = endpointUtil.getCoordinatorEpr();
        HttpURLConnection connection = openConnection(targetEndpoint);
        String reqMessage = WSATMessageFactory.newParticipantCommitMessage(null, null);
        String resMessage = sendMessage(connection, reqMessage);
        XMLStreamReader parser = factory.createXMLStreamReader(new StringReader(resMessage));
        try {
            boolean isEmpty = parser.hasText() == false;
            assertTrue("The HTTP response for one way messages should be empty.", isEmpty);
        } finally {
            parser.close();
        }
        int expectedHttpCode = HttpServletResponse.SC_ACCEPTED;
        int httpCode = connection.getResponseCode();
        assertEquals("Wrong HTTP code: " + httpCode, expectedHttpCode, httpCode);
        connection.disconnect();
    }

    private HttpURLConnection openConnection(EndpointReference targetEndpoint) throws MalformedURLException, IOException {
        URL url = new URL(targetEndpoint.getAddress().getValue());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        return connection;
    }

    private String sendMessage(HttpURLConnection connection, String reqMessage) throws IOException, XMLStreamException {
        if (msgLog.isTraceEnabled()) msgLog.trace("Outgoing SOAPMessage\n" + reqMessage);
        BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
        out.write(reqMessage.getBytes("UTF-8"));
        out.close();
        InputStream inputStream = null;
        if (connection.getResponseCode() < 400) inputStream = connection.getInputStream(); else inputStream = connection.getErrorStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        IOUtils.copyStream(baos, inputStream);
        inputStream.close();
        byte[] byteArray = baos.toByteArray();
        String resMessage = new String(byteArray, "UTF-8");
        if (msgLog.isTraceEnabled()) msgLog.trace("Incoming Response SOAPMessage\n" + resMessage);
        return resMessage;
    }
}
