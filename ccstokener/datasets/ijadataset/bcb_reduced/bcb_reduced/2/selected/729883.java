package net.interfax;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.interfax.impl.FaxStatus;
import net.interfax.impl.FaxStatusResponse;
import net.interfax.impl.SendCharFax;
import net.interfax.impl.SendCharFaxResponse;
import net.interfax.impl.SendFax;
import net.interfax.impl.SendFaxResponse;
import net.interfax.impl.XmlDeserializable;
import net.interfax.impl.XmlSerializable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 
 * @author DL
 */
public class InterFaxClient {

    private static final String INTERFAX_SOAP_API_URL = "https://ws.interfax.net/dfs.asmx";

    private final String username;

    private final String password;

    private final Integer connectTimeout;

    private final Integer readTimeout;

    private final DocumentBuilder docBuilder;

    public InterFaxClient(String username, String password, Integer connectTimeout, Integer readTimeout) {
        this.username = username;
        this.password = password;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Could not initialize XML parser", e);
        }
    }

    public InterFaxClient(String username, String password) {
        this(username, password, null, null);
    }

    public long sendPlainText(String faxNumber, String text) throws IOException, InterFaxException {
        return sendCharFax(faxNumber, text, FileType.TEXT);
    }

    public long sendHtml(String faxNumber, String html) throws IOException, InterFaxException {
        return sendFax(faxNumber, html.getBytes("UTF-8"), FileType.HTML);
    }

    private long sendFax(String faxNumber, byte[] data, FileType fileType) throws IOException, InterFaxException {
        final SendFax request = new SendFax(username, password, faxNumber, data, fileType);
        final SendFaxResponse response = new SendFaxResponse();
        callInterFaxSoap(request, response);
        verify(response.sendFaxResult);
        return response.sendFaxResult;
    }

    private long sendCharFax(String faxNumber, String data, FileType fileType) throws IOException, InterFaxException {
        final SendCharFax request = new SendCharFax(username, password, faxNumber, data, fileType);
        final SendCharFaxResponse response = new SendCharFaxResponse();
        callInterFaxSoap(request, response);
        verify(response.sendCharFaxResult);
        return response.sendCharFaxResult;
    }

    /** @see http://www.interfax.net/en/help/error_codes */
    public FaxItem getFaxItem(long transactionId) throws IOException, InterFaxException {
        final FaxStatus request = new FaxStatus(username, password, transactionId + 1, 1);
        final FaxStatusResponse response = new FaxStatusResponse();
        callInterFaxSoap(request, response);
        verify(response.resultCode);
        if ((response.listSize != 1) || (response.faxItems.size() != 1)) {
            throw new InterFaxException();
        }
        return response.faxItems.get(0);
    }

    private static final String SOAP_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" + "<soap12:Body>";

    private static final String SOAP_FOOTER = "</soap12:Body></soap12:Envelope>";

    private void callInterFaxSoap(XmlSerializable request, XmlDeserializable response) throws IOException {
        final URL url = new URL(INTERFAX_SOAP_API_URL);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (connectTimeout != null) {
            conn.setConnectTimeout(connectTimeout.intValue());
        }
        if (readTimeout != null) {
            conn.setReadTimeout(readTimeout.intValue());
        }
        conn.setRequestProperty("Accept", "application/soap+xml");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
        conn.connect();
        final OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        try {
            writer.write(SOAP_HEADER);
            writer.write(request.toXml());
            writer.write(SOAP_FOOTER);
        } finally {
            writer.close();
        }
        final InputStream is = conn.getInputStream();
        try {
            final Document doc = docBuilder.parse(is);
            response.fromXml(doc.getFirstChild().getFirstChild().getFirstChild());
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            is.close();
        }
    }

    private static void verify(long code) throws InterFaxException {
        if (code < 0) {
            throw new InterFaxException((int) code);
        }
    }
}
