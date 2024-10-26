package com.parexel.toolkit.main;

import java.io.*;
import java.net.*;
import com.parexel.toolkit.getdata.files.TmpFile;

/**
 * The connection connects the URL and gets the request result of execution.
 *
 * Need:
 * - java.net.URL.
 * - java.net.URLConnection.
 * - java.net.MalformedURLException.
 *
 * Functions:
 * - Constructors.
 * - Show method.
 * - Get and set methods.
 * - Connect the URL.
 *
 */
public class Connection {

    /**
     * The URL to connect.
     *
     * It was used for the version 3.0 of the Toolkit.
     * It contains:
     * - protocol ; 
     *      Example: http. 
     * - host ; 
     *      Example: us-bos-wb901.
     * - port ;
     *      Example: 8000.
     * - filepath ;
     *      Example: WEBSERVICE/WebSrvUrl.asp.
     * - query ; 
     *      Example: CMD=<GET_CMD_PERFORM OUTPUT='TAB' ENCAPSULATE='TRUE' IDENTIFIER='000-000-000' />
     * 
     * Example http://us-bos-wb901:8000/WEBSERVICE/WebSrvUrl.asp?CMD=<GET_CMD_PERFORM OUTPUT='TAB' ENCAPSULATE='TRUE' IDENTIFIER='000-000-000' />
     * 
     * @see java.net.URL for further informations.
     *
     * In the new version 4.0 it contains only a file URL.
     **/
    private URL urlHome;

    /**
     * Variable of global error.
     * - This attribute is used to check the validity of a Connection instance.
     * - It is initialized at "no" (noError).
     * - If an error occurs, the error description is set.
     **/
    private String error;

    /**
     * Attribute value to say that no errors occurred during the execution of a function.
     * - It initializes the error and staticError attribute, if needed.
     **/
    private String noError = "no";

    /**
     * Constructor by default.
     * @param p_URL Boston server? The url+request to create a new URL. Else url of a file.
     **/
    public Connection(String p_URL) {
        error = noError;
        setUrlHome(p_URL);
    }

    /**
     * Get the value of the error attribute of the class.
     * @return The value of the error attribute.
     **/
    public String getError() {
        return error;
    }

    /**
     * Set the value of the error attribute of the class.
     * @param p_Error The value to put in the error attribute.
     **/
    public void setError(String p_Error) {
        error = p_Error;
    }

    /**
     * Set the URL urlHome.
     * @param p_URL a string which is the url.
     **/
    public void setUrlHome(String p_URL) {
        try {
            urlHome = new URL(p_URL);
        } catch (MalformedURLException mu) {
            error = "MalformedURLException in creating url was " + mu.getMessage();
            urlHome = null;
        } catch (Exception e) {
            error = "Exception in creating url was " + e.getMessage();
            urlHome = null;
        }
    }

    /**
     * Get the URL urlHome.
     * @return A String which is the url file. It allowed to create URL(String).
     **/
    public String getUrlHome() {
        return urlHome.getFile();
    }

    /**
     * Show all the attributes values and parse the URL. Attributes : protocol, authority, host, port, path, query, filename, ref, error.
     * @return A text that describes this attributes.
     **/
    public String parseURL() {
        StringBuffer l_Show = new StringBuffer();
        l_Show.append("protocol = " + urlHome.getProtocol() + "\n");
        l_Show.append("authority = " + urlHome.getAuthority() + "\n");
        l_Show.append("host = " + urlHome.getHost() + "\n");
        l_Show.append("port = " + urlHome.getPort() + "\n");
        l_Show.append("path = " + urlHome.getPath() + "\n");
        l_Show.append("query = " + urlHome.getQuery() + "\n");
        l_Show.append("filename = " + urlHome.getFile() + "\n");
        l_Show.append("ref = " + urlHome.getRef() + "\n");
        l_Show.append("error = " + error + "\n");
        return l_Show.toString();
    }

    /**
     * Connect the URL and retrieve the result in a temporary file.
     * @param p_TmpFile A TMPFile where the text retrieved with the url execution is written.
     **/
    public void copyURLToFile(TmpFile p_TmpFile) {
        byte[] l_Buffer;
        URLConnection l_Connection = null;
        DataInputStream l_IN = null;
        DataOutputStream l_Out = null;
        FileOutputStream l_FileOutStream = null;
        try {
            System.gc();
            if (error.compareTo(noError) == 0) {
                l_Connection = urlHome.openConnection();
                l_FileOutStream = new FileOutputStream(p_TmpFile.getAbsolutePath());
                l_Out = new DataOutputStream(l_FileOutStream);
                l_IN = new DataInputStream(l_Connection.getInputStream());
                l_Buffer = new byte[8192];
                int bytes = 0;
                while ((bytes = l_IN.read(l_Buffer)) > 0) {
                    l_Out.write(l_Buffer, 0, bytes);
                }
            }
        } catch (MalformedURLException mue) {
            error = "MalformedURLException in connecting url was " + mue.getMessage();
        } catch (IOException io) {
            error = "IOException in connecting url was " + io.getMessage();
        } catch (Exception e) {
            error = "Exception in connecting url was " + e.getMessage();
        } finally {
            try {
                l_IN.close();
                l_Out.flush();
                l_FileOutStream.flush();
                l_FileOutStream.close();
                l_Out.close();
            } catch (Exception e) {
                error = "Exception in connecting url was " + e.getMessage();
            }
        }
    }
}
