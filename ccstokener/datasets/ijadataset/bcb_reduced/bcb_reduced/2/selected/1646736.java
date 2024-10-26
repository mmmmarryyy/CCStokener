package com.amarphadke.stocks.accessor.webservicex;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.amarphadke.stocks.accessor.InvalidBackendResponse;
import com.amarphadke.stocks.accessor.StockQuoteAccessor;
import com.amarphadke.stocks.model.StockQuote;

public class StockQuoteAccessorImpl implements StockQuoteAccessor {

    public StockQuote[] getStockQuote(String[] symbols) throws InvalidBackendResponse {
        List stockQuotes = new ArrayList();
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
        httpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
        for (int i = 0; i < symbols.length; i++) {
            stockQuotes.add(getStockQuote(symbols[i], httpClient));
        }
        return (StockQuote[]) stockQuotes.toArray(new StockQuote[0]);
    }

    private StockQuote getStockQuote(String symbol, HttpClient httpClient) throws InvalidBackendResponse {
        try {
            HttpUriRequest httpUriRequest = new HttpGet("http://www.webservicex.net/stockquote.asmx/GetQuote?symbol=" + symbol);
            HttpResponse httpResponse = httpClient.execute(httpUriRequest);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(httpResponse.getEntity().getContent());
            int contentLength = (int) httpResponse.getEntity().getContentLength();
            if (contentLength <= 0) {
                InvalidBackendResponse invalidBackendResponse = new InvalidBackendResponse(httpResponse.getStatusLine().getStatusCode(), httpResponse.getStatusLine().getReasonPhrase());
                System.out.println("Error while receiving response from backend, " + invalidBackendResponse);
                throw invalidBackendResponse;
            }
            byte[] bytes = new byte[contentLength];
            bufferedInputStream.read(bytes);
            bufferedInputStream.close();
            String responseText = new String(bytes, "utf-8");
            responseText = responseText.replaceAll("&lt;", "<");
            responseText = responseText.replaceAll("&gt;", ">");
            InputStream inputStream = new ByteArrayInputStream(responseText.getBytes());
            return parseStockQuoteFromWebServiceResponse(inputStream);
        } catch (InvalidBackendResponse e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InvalidBackendResponse(-1, e.getMessage());
        }
    }

    private StockQuote parseStockQuoteFromWebServiceResponse(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException, InvalidBackendResponse {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(inputStream);
        StockQuote stockQuote = new StockQuote();
        String stockSymbol = getElementValue(doc, "Symbol");
        if (stockSymbol == null || stockSymbol.trim().length() == 0) {
            throw new InvalidBackendResponse(-1, "Unable to retrieve Stock Symbol information");
        }
        stockQuote.setSymbol(stockSymbol);
        stockQuote.setCompanyName(getElementValue(doc, "Name"));
        String date = getElementValue(doc, "Date");
        String time = getElementValue(doc, "Time");
        time = time.toUpperCase();
        if (time.indexOf(":") == 1) {
            time = "0" + time;
        }
        String dateTime = date + " " + time;
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mmaa");
        Date updatedTime;
        try {
            updatedTime = dateFormat.parse(dateTime);
            stockQuote.setUpdatedTime(updatedTime);
        } catch (ParseException e) {
            System.out.println("Error in parsing update date/time:" + e);
        }
        try {
            stockQuote.setLastTradedPrice(Double.parseDouble(getElementValue(doc, "Last")));
        } catch (Throwable e) {
            System.out.println("Error in parsing Last Traded Price:" + e);
        }
        try {
            stockQuote.setChange(Double.parseDouble(getElementValue(doc, "Change")));
        } catch (Throwable e) {
            System.out.println("Error in parsing Change:" + e);
        }
        try {
            stockQuote.setOpeningPrice(Double.parseDouble(getElementValue(doc, "Open")));
        } catch (Throwable e) {
            System.out.println("Error in parsing Opening Price:" + e);
        }
        try {
            stockQuote.setHighPrice(Double.parseDouble(getElementValue(doc, "High")));
        } catch (Throwable e) {
            System.out.println("Error in parsing High Price:" + e);
        }
        try {
            stockQuote.setLowPrice(Double.parseDouble(getElementValue(doc, "Low")));
        } catch (Throwable e) {
            System.out.println("Error in parsing Low Price:" + e);
        }
        try {
            stockQuote.setPreviousClose(Double.parseDouble(getElementValue(doc, "PreviousClose")));
        } catch (Throwable e) {
            System.out.println("Error in parsing Previous Close:" + e);
        }
        try {
            String annualRange = getElementValue(doc, "AnnRange");
            String[] annualLimits = annualRange.split("-");
            if (annualLimits.length == 2) {
                stockQuote.setAnnualLowPrice(Double.parseDouble(annualLimits[0]));
                stockQuote.setAnnualHighPrice(Double.parseDouble(annualLimits[1]));
            }
        } catch (Throwable e) {
            System.out.println("Exception setting Annual high-Low:" + e);
        }
        try {
            stockQuote.setVolume(Long.parseLong(getElementValue(doc, "Volume")));
        } catch (Throwable e) {
            System.out.println("Exception setting Volume:" + e);
        }
        try {
            stockQuote.setEarnings(Double.parseDouble(getElementValue(doc, "Earns")));
        } catch (Throwable e) {
            System.out.println("Exception setting Earnings:" + e);
        }
        try {
            stockQuote.setPriceToEarningRatio(Double.parseDouble(getElementValue(doc, "P-E")));
        } catch (Throwable e) {
            System.out.println("Exception setting P-E:" + e);
        }
        try {
            stockQuote.setMarketCapitalization(Double.parseDouble(getElementValue(doc, "MktCap")));
        } catch (Throwable e) {
            System.out.println("Exception setting MktCap:" + e);
        }
        return stockQuote;
    }

    private String getElementValue(Document doc, String elementName) {
        Element element = (Element) doc.getElementsByTagName(elementName).item(0);
        StringBuffer elementValue = new StringBuffer("");
        if (element != null && element.hasChildNodes()) {
            NodeList childrenNodes = element.getChildNodes();
            for (int i = 0; i < childrenNodes.getLength(); i++) {
                elementValue.append(element.getChildNodes().item(i).getNodeValue());
            }
        } else {
            System.out.println("Did not find " + elementName + "/Value");
        }
        return elementValue.toString();
    }
}
