package com.idna.wsconsumer.http;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Implementation of the interface NetIDClient
 * 
 * @author kan.sun
 * 
 */
public class NetIDClientImpl implements NetIDClient {

    private String urlStr;

    private String rawXML;

    private String soapWrapper;

    private HttpURLConnection urlConnection;

    private String request;

    private String response;

    public synchronized void setUrlStr(String urlString) {
        this.urlStr = urlString;
    }

    public synchronized void setRawXML(String filePath) {
        this.rawXML = filePath;
    }

    public synchronized void setSoapWrapper(String soapProp) {
        this.soapWrapper = soapProp;
    }

    /**
	 * Read contents of a specified file and return them as a string
	 * 
	 * @param filePath
	 * @return fileContent
	 */
    private String readFile(String filePath) {
        String fileContent = "";
        String line = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            while ((line = reader.readLine()) != null) {
                fileContent += " " + (line.trim());
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return fileContent;
    }

    /**
	 * Set up the URL connection channel via which messages can be transfered
	 * 
	 * @param urlString
	 * @throws IOException
	 */
    private synchronized void setUrlConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        this.urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("SOAPAction", urlString);
    }

    private synchronized void setRequest() throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(soapWrapper));
        String header = prop.getProperty("header");
        String footer = prop.getProperty("footer");
        request = readFile(rawXML);
        request = header.trim() + request + footer.trim();
    }

    public synchronized void sendRequest() throws IOException {
        setRequest();
        setUrlConnection(urlStr);
        OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
        out.write(request);
        out.flush();
        out.close();
    }

    public synchronized void printResponse(String threadID) throws IOException {
        if (urlConnection.getResponseCode() == 200) {
            InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
            String responseLine;
            BufferedReader reader = new BufferedReader(in);
            while ((responseLine = reader.readLine()) != null) {
                if (responseLine != null) {
                    responseLine = responseLine.replaceAll("&lt;", "<");
                    responseLine = responseLine.replaceAll("&gt;", ">");
                    response += responseLine;
                }
            }
            in.close();
            System.out.println(threadID + "-> Response is: " + response);
        } else {
            System.err.println(threadID + "-> HTTP error: " + urlConnection.getResponseCode());
        }
    }
}
