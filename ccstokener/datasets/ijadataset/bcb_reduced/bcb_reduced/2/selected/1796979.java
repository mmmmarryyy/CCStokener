package biz.sfsservices.ebiz;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Simple SOAP-Client for J2SE (Standard Edition)
 *
 * @author pire, bens, aben
 * This class make a connection to the PI System and create the XML request
 */
public class StandardSOAP {

    private String url;

    private String auth;

    private String errorString;

    /**
     *
     * @param url give the url to connect the server
     * @param auth give the password for autenthification
     */
    public StandardSOAP(String url, String auth) {
        this.url = url;
        this.auth = auth;
    }

    /**
     *
     * @param url give the url to connect the server
     */
    public StandardSOAP(String url) {
        this.url = url;
        this.auth = null;
    }

    /**
     *
     * @param url give the url to connect the server
     * @param user give the username to connect with the server
     * @param pass give the password for autenthification
     */
    public StandardSOAP(String url, String user, String pass) {
        this.url = url;
        setAuth(user, pass);
    }

    /**
     *
     * @return gives the url back
     */
    public String getURL() {
        return url;
    }

    /**
     *
     * @param newUrl change the url and give the changed url back
     */
    public void setURL(String newUrl) {
        url = newUrl;
    }

    /**
     *
     * @param user set the username for the connection
     * @param pass set the password for the connection
     */
    public void setAuth(String user, String pass) {
    }

    /**
     *
     * @return gives the error code back if an error is occured
     */
    public String getLastError() {
        return errorString;
    }

    /**
     *
     * @param xml handover of the xml String
     * @return give the new string back, if an error is occured it gives null back
     */
    public String callSOAPRequest(String xml) {
        OutputStream os;
        InputStream is;
        errorString = "";
        long length;
        try {
            URL l_url = new URL(this.url);
            HttpURLConnection conn = (HttpURLConnection) l_url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("CallingType", "SJ");
            if (auth != null) conn.setRequestProperty("Authorization", auth);
            conn.setRequestProperty("SOAPAction", "http://sap.com/xi/WebService/soap1.1");
            conn.setDoOutput(true);
            os = conn.getOutputStream();
            OutputStreamWriter osr = new OutputStreamWriter(os);
            osr.write(xml);
            osr.flush();
            osr.close();
            is = conn.getInputStream();
            length = conn.getContentLength();
            String str = "";
            if (length != -1) {
                byte incomingData[] = new byte[(int) length];
                is.read(incomingData);
                str = new String(incomingData);
            }
            is.close();
            return str;
        } catch (Exception e) {
            errorString = e.getMessage();
            return null;
        }
    }

    /**
     * 
     * @param xml handover of the xml String
     * @return gives the complete connection String back
     */
    public String makePISOAPEnvelope(String xml) {
        return "<?xml version='1.0' encoding='UTF-8'?>" + "<SOAP-ENV:Envelope xmlns:xs='http://www.w3.org/2001/XMLSchema' xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + "<SOAP-ENV:Body>" + xml + "</SOAP-ENV:Body>" + "</SOAP-ENV:Envelope>\n";
    }
}
