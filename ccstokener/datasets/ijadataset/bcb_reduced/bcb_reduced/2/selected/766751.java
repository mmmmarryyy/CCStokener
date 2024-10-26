package org.apache.wicket.examples.stockquote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides access to a SOAP service for getting stock quotes based on a symbol. Found on
 * http://www.devx.com/Java/Article/27559/0/page/2
 */
public class StockQuote {

    /**
	 * We used to use the www.xmethods.com demo webservice for stockquotes. We now use webservicex,
	 * as xmethods was really overloaded and unreliable.
	 */
    private static final String serviceUrl = "http://www.webservicex.net/stockquote.asmx";

    /** the symbol to get the quote for. */
    private String symbol;

    /**
	 * Default constructor.
	 */
    public StockQuote() {
    }

    /**
	 * Constructor setting the symbol to get the quote for.
	 * 
	 * @param symbol
	 *            the symbol to look up
	 */
    public StockQuote(String symbol) {
        this.symbol = symbol;
    }

    /**
	 * Gets the symbol.
	 * 
	 * @return the symbol
	 */
    public String getSymbol() {
        return symbol;
    }

    /**
	 * Sets the symbol for getting the quote.
	 * 
	 * @param symbol
	 */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
	 * Gets a stock quote for the given symbol
	 * 
	 * @return the stock quote
	 */
    public String getQuote() {
        final String response = getSOAPQuote(symbol);
        int start = response.indexOf("&lt;Last&gt;") + "&lt;Last&gt;".length();
        int end = response.indexOf("&lt;/Last&gt;");
        if (start < "&lt;Last&gt;".length()) {
            return "(unknown)";
        }
        String result = response.substring(start, end);
        return result.equals("0.00") ? "(unknown)" : result;
    }

    /**
	 * Calls the SOAP service to get the stock quote for the symbol.
	 * 
	 * @param symbol
	 *            the name to search for
	 * @return the SOAP response containing the stockquote
	 */
    private String getSOAPQuote(String symbol) {
        String response = "";
        try {
            final URL url = new URL(serviceUrl);
            final String message = createMessage(symbol);
            HttpURLConnection httpConn = setUpHttpConnection(url, message.length());
            writeRequest(message, httpConn);
            response = readResult(httpConn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
	 * Writes the message to the connection.
	 * 
	 * @param message
	 *            the message to write
	 * @param httpConn
	 *            the connection
	 * @throws IOException
	 */
    private void writeRequest(String message, HttpURLConnection httpConn) throws IOException {
        OutputStream out = httpConn.getOutputStream();
        out.write(message.getBytes());
        out.close();
    }

    /**
	 * Sets up the HTTP connection.
	 * 
	 * @param url
	 *            the url to connect to
	 * @param length
	 *            the length to the input message
	 * @return the HttpurLConnection
	 * @throws IOException
	 * @throws ProtocolException
	 */
    private HttpURLConnection setUpHttpConnection(URL url, int length) throws IOException, ProtocolException {
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        httpConn.setRequestProperty("Content-Length", String.valueOf(length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", "\"http://www.webserviceX.NET/GetQuote\"");
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        return httpConn;
    }

    /**
	 * Reads the response from the http connection.
	 * 
	 * @param connection
	 *            the connection to read the response from
	 * @return the response
	 * @throws IOException
	 */
    private String readResult(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader in = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        in.close();
        return sb.toString();
    }

    /**
	 * Creates the request message for retrieving a stock quote.
	 * 
	 * @param symbol
	 *            the symbol to query for
	 * @return the request message
	 */
    private String createMessage(String symbol) {
        StringBuffer message = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        message.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        message.append("  <soap:Body>");
        message.append("    <GetQuote xmlns=\"http://www.webserviceX.NET/\">");
        message.append("      <symbol>").append(symbol).append("</symbol>");
        message.append("    </GetQuote>");
        message.append("  </soap:Body>");
        message.append("</soap:Envelope>");
        return message.toString();
    }
}
