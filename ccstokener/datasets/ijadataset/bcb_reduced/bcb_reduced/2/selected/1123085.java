package org.apache.jetspeed.webservices.finance.stockmarket;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.rmi.RemoteException;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.InputSource;
import org.apache.turbine.services.TurbineBaseService;

/**
    Implements StockQuoteService,
    providing a web service for getting stock quotes.
        
    @author <a href="mailto:taylor@apache.org">David Sean Taylor</a>
    @version $Id: JetspeedStockQuoteService.java,v 1.7 2004/02/23 03:15:29 jford Exp $
*/
public class JetspeedStockQuoteService extends TurbineBaseService implements StockQuoteService {

    boolean debugIO = false;

    PrintStream debugOutputStream = System.out;

    private static final String BASE_SOAP_ENVELOPE = "<?xml version=\"1.0\"?>\n" + "<SOAP-ENV:Envelope " + "\n     xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"" + "\n     xmlns:xsd1=\"urn:DataQuoteService\"" + "\n     xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"" + "\n     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" + "\n     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" + "  <SOAP-ENV:Body>\n";

    private static final String END_SOAP_ENVELOPE = "  </SOAP-ENV:Body>\n</SOAP-ENV:Envelope>\n";

    private static final String SOAP_ENCODING = " SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n";

    private static final String SERVICE_END_POINT = "http://www.bluesunrise.com/webservices/container/BlueSunriseFinance/BlueSunriseFinanceService/BlueSunriseFinancePort/";

    private static final String WSDL_SERVICE_NAMESPACE = "urn:QuoteService";

    private static final String SOAP_METHOD_QUOTE = WSDL_SERVICE_NAMESPACE + "/quote";

    private static final String SOAP_METHOD_FULLQUOTE = WSDL_SERVICE_NAMESPACE + "/fullQuote";

    private static final String SOAP_METHOD_FULLQUOTES = WSDL_SERVICE_NAMESPACE + "/fullQuotes";

    private String soapEndPoint = SERVICE_END_POINT;

    private static final String QUOTE_RESULT = "quoteResult";

    private static final String FULL_QUOTE_RESULT = "fullQuoteResult";

    private static final String FULL_QUOTES_RESULT = "fullQuotesResult";

    private static final String DEFAULT_RETURN = "return";

    /**
        Get a single stock quote, given a symbol return the current price.

        @param symbol The stock symbol.
        @return String The current price.
      */
    public String quote(String symbol) throws RemoteException {
        StringBuffer envelope = new StringBuffer(BASE_SOAP_ENVELOPE);
        envelope.append("    <m1:quote xmlns:m1=\"urn:QuoteService\"");
        envelope.append(SOAP_ENCODING);
        envelope.append("      <symbol xsi:type=\"xsd:string\">");
        envelope.append(symbol);
        envelope.append("</symbol>\n");
        envelope.append("    </m1:quote>\n");
        envelope.append(END_SOAP_ENVELOPE);
        SOAPResponseHandler handler = new SOAPResponseHandler(WSDL_SERVICE_NAMESPACE, QUOTE_RESULT, DEFAULT_RETURN);
        doSOAPRequest(SOAP_METHOD_QUOTE, envelope, handler);
        if (handler.isFault()) {
            throw new RemoteException(handler.getFaultContent());
        }
        try {
            String resultString = handler.getResult();
            if (resultString == null) throw new RemoteException("Could not find return in soap response");
            return resultString;
        } catch (Exception e) {
            throw new RemoteException("Error generating result.", e);
        }
    }

    /**
        Get a single stock quote record, given a symbol return a StockQuote object.

        @param symbol The stock symbol.
        @return StockQuote A full stock quote record.
      */
    public StockQuote fullQuote(String symbol) throws RemoteException {
        StringBuffer envelope = new StringBuffer(BASE_SOAP_ENVELOPE);
        envelope.append("    <m1:fullQuote xmlns:m1=\"urn:QuoteService\"");
        envelope.append(SOAP_ENCODING);
        envelope.append("      <symbol xsi:type=\"xsd:string\">");
        envelope.append(symbol);
        envelope.append("</symbol>\n");
        envelope.append("    </m1:fullQuote>\n");
        envelope.append(END_SOAP_ENVELOPE);
        SOAPResponseHandler handler = new SOAPResponseHandler(WSDL_SERVICE_NAMESPACE, FULL_QUOTE_RESULT, DEFAULT_RETURN);
        StockQuoteHandler quoteHandler = new StockQuoteHandler();
        handler.setResultHandler(quoteHandler);
        doSOAPRequest(SOAP_METHOD_FULLQUOTE, envelope, handler);
        if (handler.isFault()) {
            throw new RemoteException(handler.getFaultContent());
        }
        try {
            return quoteHandler.getResult();
        } catch (Exception e) {
            throw new RemoteException("Error generating result.", e);
        }
    }

    /**
        Get an array of quote records, given a array of stock symbols.

        @param symbols[] The array of stock symbols.
        @return StockQuote[] An array of full stock quotes for each stock symbol.
      */
    public StockQuote[] fullQuotes(String[] symbols) throws RemoteException {
        if (null == symbols || symbols.length < 1) throw new RemoteException("Invalid symbols[] parameter");
        StringBuffer envelope = new StringBuffer(BASE_SOAP_ENVELOPE);
        envelope.append("    <m1:fullQuotes xmlns:m1=\"urn:QuoteService\"");
        envelope.append(SOAP_ENCODING);
        envelope.append("      <symbols xsi:type=\"SOAP-ENC:Array\" SOAP-ENC:arrayType=\"xsd:string[" + symbols.length + "]\">\n");
        for (int ix = 0; ix < symbols.length; ix++) {
            envelope.append("        <item xsi:type=\"xsd:string\">");
            envelope.append(symbols[ix]);
            envelope.append("</item>\n");
        }
        envelope.append("      ");
        envelope.append("</symbols>\n");
        envelope.append("    </m1:fullQuotes>\n");
        envelope.append(END_SOAP_ENVELOPE);
        SOAPResponseHandler handler = new SOAPResponseHandler(WSDL_SERVICE_NAMESPACE, FULL_QUOTES_RESULT, DEFAULT_RETURN);
        StockQuoteArrayHandler quoteHandler = new StockQuoteArrayHandler();
        handler.setResultHandler(quoteHandler);
        doSOAPRequest(SOAP_METHOD_FULLQUOTES, envelope, handler);
        if (handler.isFault()) {
            throw new RemoteException(handler.getFaultContent());
        }
        try {
            return quoteHandler.getResult();
        } catch (Exception e) {
            throw new RemoteException("Error generating result.", e);
        }
    }

    /**
        Set the name of the web service used by this service to retrieve stock quotes.

        @param service The name of the web service.
      */
    public void setWebService(String service) {
        this.soapEndPoint = service;
    }

    /**
        Get the name of the web service used by this service to retrieve stock quotes.

        @return String The name of the web service.
      */
    public String getWebService() {
        return soapEndPoint;
    }

    /**
        make a SOAP Request to the web service.

     */
    private void doSOAPRequest(String soapAction, StringBuffer envelope, XMLFilterImpl handler) throws RemoteException {
        try {
            if (debugIO) {
                debugOutputStream.println("SOAPURL: " + soapEndPoint);
                debugOutputStream.println("SoapAction: " + soapAction);
                debugOutputStream.println("SoapEnvelope:");
                debugOutputStream.println(envelope.toString());
            }
            URL url = new URL(soapEndPoint);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.setDoOutput(true);
            byte bytes[] = envelope.toString().getBytes();
            connect.setRequestProperty("SOAPAction", "\"" + soapAction + "\"");
            connect.setRequestProperty("content-type", "text/xml");
            connect.setRequestProperty("content-length", "" + bytes.length);
            OutputStream out = connect.getOutputStream();
            out.write(bytes);
            out.flush();
            int rc = connect.getResponseCode();
            InputStream stream = null;
            if (rc == HttpURLConnection.HTTP_OK) {
                stream = connect.getInputStream();
            } else if (rc == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                stream = connect.getErrorStream();
            }
            if (stream != null) {
                if (debugIO) {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    int bt = stream.read();
                    while (bt != -1) {
                        bout.write(bt);
                        bt = stream.read();
                    }
                    debugOutputStream.println("Response:");
                    debugOutputStream.println(new String(bout.toByteArray()));
                    stream.close();
                    stream = new ByteArrayInputStream(bout.toByteArray());
                }
                String contentType = connect.getContentType();
                if (contentType.indexOf("text/xml") == -1) {
                    throw new RemoteException("Content-type not text/xml.  Instead, found " + contentType);
                }
                org.apache.xerces.parsers.SAXParser xmlreader = new org.apache.xerces.parsers.SAXParser();
                handler.setParent(xmlreader);
                xmlreader.setContentHandler(handler);
                xmlreader.parse(new InputSource(stream));
                stream.close();
            } else {
                throw new RemoteException("Communication error: " + rc + " " + connect.getResponseMessage());
            }
        } catch (RemoteException rex) {
            throw rex;
        } catch (Exception ex) {
            throw new RemoteException("Error doing soap stuff", ex);
        }
    }

    /**
      usage: 
         
          java JetspeedStockQuoteService [option] method [params]

          method:           parameter:       description:
          ----------------------------------------------------------------------
            quote             symbol       get the price for the given symbol  
            quotes            symbols..    get the prices for 1..n symbols 
            fullQuote         symbol       get a stock quote record for the given symbol
            fullQuotes        symbols...   1..n symbols to look up multiple stock quote records


          options:
          --------
            -debug            print to stdout the SOAP request and response packets

          Examples:
             java JetspeedStockQuoteService quote IBM
             java JetspeedStockQuoteService quotes IONA CSCO NOK ADSK
             java JetspeedStockQuoteService -debug fullQuote DST
             java JetspeedStockQuoteService fullQuotes SUNW MSFT ORCL



     **/
    public static void main(String args[]) {
        try {
            JetspeedStockQuoteService service = new JetspeedStockQuoteService();
            if (args.length == 0) {
                return;
            }
            int index = 0;
            if (args[index].startsWith("-")) {
                if ("-debug".equals(args[0])) {
                    service.debugIO = true;
                    index++;
                }
            }
            if (index >= args.length) return;
            if ("quote".equals(args[index])) {
                index++;
                if (index >= args.length) return;
                System.out.println(service.quote(args[index]));
            } else if ("quotes".equals(args[index])) {
            } else if ("fullQuote".equals(args[index])) {
                index++;
                if (index >= args.length) return;
                System.out.println(service.fullQuote(args[index]));
            } else if ("fullQuotes".equals(args[index])) {
                index++;
                String[] symbols = new String[args.length - index];
                for (int ix = 0; ix < symbols.length; ix++) {
                    symbols[ix] = args[index];
                    index++;
                }
                StockQuote[] stocks = service.fullQuotes(symbols);
                for (int ix = 0; ix < stocks.length; ix++) System.out.println(stocks[ix]);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
