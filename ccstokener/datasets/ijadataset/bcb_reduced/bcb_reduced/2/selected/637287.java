package org.is.web;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URL url = new URL(getCodeBase(), "/servlet/ServletName");
 * HttpRequest req = new HttpRequest(url);
 * InputStream in = req.get(cgiParameters);
 *
 * @since   JDK1.0
 */
public class HttpRequest {

    /**
	 * CGI or servlet to ask for information about nodes (nodes tree)
	 */
    protected URL cgi;

    /**
	 * If true prints additional debugging information
	 */
    protected boolean debug;

    /**
	 * The constructor
	 */
    public HttpRequest(URL cgi) {
        this.cgi = cgi;
    }

    /**
	 * Sets debug mode
	 */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
	 * Makes GET request to servlet or CGI with CGIParameters
	 *
	 * @return an InputStream to read the response
	 * @exception IOException if an I/O error occurs
	 */
    public InputStream get(CGIParameters cgiparams) throws IOException {
        String argString = null;
        if (cgiparams != null && cgiparams.size() > 0) {
            argString = "?" + cgiparams.getEncoded();
        } else argString = "";
        URL url = new URL(cgi.toExternalForm() + argString);
        if (debug) System.out.println("GET:" + cgi.toExternalForm() + argString);
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        return con.getInputStream();
    }

    /**
	 * Performs a POST request to servlet or CGI, posting CGIParameters.
	 *
	 * @return an InputStream to read the response
	 * @exception IOException if an I/O error occurs
	 */
    public InputStream post(CGIParameters cgiparams) throws IOException {
        String argString = null;
        if (cgiparams != null && cgiparams.size() > 0) {
            argString = cgiparams.getEncoded();
        } else argString = "";
        URLConnection con = cgi.openConnection();
        if (debug) System.out.println("POST:" + cgi + " with parameters:" + cgiparams.toString());
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(argString);
        out.flush();
        out.close();
        return con.getInputStream();
    }

    public static void main(String[] args) {
        try {
            URL url = new URL("http://google.com");
            HttpRequest httpReq = new HttpRequest(url);
            CGIParameters params = new CGIParameters();
            InputStream is = httpReq.get(params);
            int read = 0;
            byte b[] = new byte[1024];
            while ((read = is.read(b, 0, b.length)) != -1) {
                System.out.write(b, 0, read);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
