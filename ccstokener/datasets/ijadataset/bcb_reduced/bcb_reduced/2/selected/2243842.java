package sg.com.fastwire.mediation.plugins.huaweiMTOSI.common.utils;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.apache.log4j.Logger;
import sg.com.fastwire.mediation.plugins.huaweiMTOSI.common.utils.CommonLogger;

public class HttpSoapClient {

    private URL url;

    private HttpURLConnection httpConn = null;

    private byte[] bData = null;

    public HttpSoapClient(byte[] bData) {
        try {
            url = new URL("http://172.254.254.6:9997/ConnectionRetrieval");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.bData = bData;
    }

    public String postXml() {
        connect(bData);
        writeXml();
        return response();
    }

    private void connect(byte[] bData) {
        System.out.println("Connecting to: " + url.toString());
        String SOAPAction = "";
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            httpConn = (HttpURLConnection) connection;
            httpConn.setRequestProperty("Content-Length", String.valueOf(bData.length));
            httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpConn.setRequestProperty("SOAPAction", SOAPAction);
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
        } catch (IOException ioExp) {
            CommonLogger.error(this, "Error while connecting to  SOAP server !", ioExp);
        }
    }

    private String response() {
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(httpConn.getInputStream());
        } catch (IOException ioExp) {
            CommonLogger.error(this, "Error while connecting to read from SOAP server!", ioExp);
        }
        BufferedReader in = new BufferedReader(isr);
        String output = "";
        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                output += inputLine;
                if (in.ready()) {
                    output += "\n";
                }
            }
        } catch (IOException ioExp) {
            CommonLogger.error(this, "Error while reading SOAP response!", ioExp);
        }
        try {
            in.close();
        } catch (IOException ioExp) {
            CommonLogger.error(this, "Error while closing SOAP server connection!", ioExp);
        }
        return output;
    }

    private void writeXml() {
        OutputStream out = null;
        ;
        try {
            out = httpConn.getOutputStream();
            out.write(bData);
            out.close();
        } catch (IOException ioExp) {
            CommonLogger.error(this, "Error while writing to SOAP server output !", ioExp);
        }
    }
}
