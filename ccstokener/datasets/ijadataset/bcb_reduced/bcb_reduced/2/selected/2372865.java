package com.ca.jndiproviders.dsml;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;

public class SoapClient {

    private static Logger log = Logger.getLogger(SoapClient.class.getName());

    /**
     * This takes a byte array and hoofs off the contents to the target URL, adding
     * a bunch of http headers, including an optional 'SOAPaction:' header.  It returns
     * the raw contents of the reply, sans any http headers.
     *
     * @param SOAPUrl
     * @param b
     * @param SOAPAction
     * @return the response data
     * @throws IOException
     */
    public static String sendSoapMsg(String SOAPUrl, byte[] b, String SOAPAction) throws IOException {
        log.finest("HTTP REQUEST SIZE " + b.length);
        if (SOAPAction.startsWith("\"") == false) SOAPAction = "\"" + SOAPAction + "\"";
        URL url = new URL(SOAPUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestProperty("SOAPAction", SOAPAction);
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Cache-Control", "no-cache");
        httpConn.setRequestProperty("Pragma", "no-cache");
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        out.write(b);
        out.close();
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
        BufferedReader in = new BufferedReader(isr);
        StringBuffer response = new StringBuffer(1024);
        String inputLine;
        while ((inputLine = in.readLine()) != null) response.append(inputLine);
        in.close();
        log.finest("HTTP RESPONSE SIZE: " + response.length());
        return response.toString();
    }
}
