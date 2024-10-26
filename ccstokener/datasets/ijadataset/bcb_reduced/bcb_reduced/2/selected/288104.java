package com.sun.ebxml.registry.interfaces.rest;

import java.net.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * This is a test program for the REST interface to the registry server
 *
 * @see
 * @author Uday Subbarayan(uday.s@sun.com)
 */
public class RESTSender {

    String restURL = "http://localhost:8080/ebxmlrr/registry/rest";

    public RESTSender() {
    }

    public void testQMAdhocQueryRequest(String xmlFileName) throws FileNotFoundException, IOException, MalformedURLException {
        String xmlAdhocQueryRequest = "";
        String input;
        File xmlInputFile = new File(xmlFileName);
        BufferedReader in = new BufferedReader(new FileReader(xmlInputFile));
        while ((input = in.readLine()) != null) {
            xmlAdhocQueryRequest = xmlAdhocQueryRequest + input;
        }
        in.close();
        URL url = new URL(restURL);
        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true);
        PrintWriter out = new PrintWriter(httpConnection.getOutputStream());
        out.println("xmldoc=" + URLEncoder.encode(xmlAdhocQueryRequest, "UTF-8"));
        out.close();
        BufferedReader in2 = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        String inputLine;
        while ((inputLine = in2.readLine()) != null) System.out.println(inputLine);
        in2.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java RESTSender <input-AdhocQueryRequest-XML>");
            System.exit(0);
        }
        String xmlFileName = args[0];
        RESTSender rs = new RESTSender();
        rs.testQMAdhocQueryRequest(xmlFileName);
    }
}
