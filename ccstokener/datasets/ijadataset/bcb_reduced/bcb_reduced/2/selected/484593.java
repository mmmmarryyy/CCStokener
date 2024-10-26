package com.io_software.utils.web;

import com.abb.util.Request;
import java.io.Serializable;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** Given an <tt>InetAddress</tt>, an instance of this class can probe
    that address on port 80 using the HTTP protocol and check if the
    address hosts a server.<p>
    
    The timeout can be set until which the server has to respond to be
    acknowledged as a valid server.<p>
    
    Being subclass of {@link Request} an instance of this class can be
    passed to a {@link com.abb.util.RequestManager} object for pooled
    execution.

    @author Axel Uhl
    @version $Id: URLProbeRequest.java,v 1.2 2001/02/03 15:24:12 aul Exp $
  */
public class URLProbeRequest implements Request {

    /** constructs an instance of this class with the specified server
	timeout and server address for the specified port.
	
	@param timeout the time to wait for a response in milliseconds.
		    If this timeout passes after sending the request without
		    a reply by the server, the address is considered not to
		    operate a server currently.
	@param url the url to probe. Specifies protocol, host and port
	@param resultTable defines where to put the results of the probe.
		As key into this table the {@link InetAddress} of the
		URL is to be used, as value the {@line ProbeResult}
		determined for the specified URL.
      */
    public URLProbeRequest(long timeout, URL url, Hashtable resultTable) {
        this.timeout = timeout;
        this.url = url;
        this.resultTable = resultTable;
    }

    /** Performing a request of this class means to attempt to open the
	URL specified at the constructor, wait for the timeout period and
	check whether the server has responded within this time interval.<p>
	
      */
    public void execute() {
        Date startedRequestAt = new Date();
        ProbeResult result;
        InetAddress ia = null;
        HttpURLConnection connection = null;
        try {
            ia = InetAddress.getByName(url.getHost());
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            result = new ProbeResult(url, connection.getResponseCode(), -1, null);
            resultTable.put(ia, result);
        } catch (IOException ioe) {
            Date requestEndedAt = new Date();
            result = new ProbeResult(url, -1, requestEndedAt.getTime() - startedRequestAt.getTime(), ioe);
            resultTable.put(ia, result);
        }
        if (connection != null) connection.disconnect();
    }

    /** timeout for the probe */
    private long timeout;

    /** URL to probe */
    private URL url;

    /** table where to put the probe result. Keys are the URL's
	{@link InetAddress}, values are the corresponding {@link ProbeResult}
	objects.
      */
    private Hashtable resultTable;

    /** Inner class to hold and manage results of a probe. */
    public static class ProbeResult implements Serializable {

        /** constructs an instance of this class
	
	    @param url the URL that was probed
	    @param responseCode the response code retrieved from the response header
	    @param timeoutedAfter a value of <tt>-1</tt> indicated that the URL did not
		    timeout and a server responded in the previously specified time.
		    Otherwise this parameter indicates the number of milliseconds
		    after which the server still did not respond.
	    @param ioe the exception with which establishing the connection failed
	  */
        public ProbeResult(URL url, int responseCode, long timeoutedAfter, IOException ioe) {
            this.url = url;
            this.responseCode = responseCode;
            this.timeoutedAfter = timeoutedAfter;
            this.exception = ioe;
        }

        /** Tells whether this result is considered a successful probe.
	    This is the case if the server responded within the specified
	    timeout period.
	  */
        public boolean succeeded() {
            return (timeoutedAfter == -1);
        }

        /** Computes a string representation of the probe result.
	
	    @retutn the string representation of this probe result. Contains
		    the URL and an indication about the timeout state
	  */
        public String toString() {
            String result = url.toString();
            if (timeoutedAfter == -1) result += " successful with code " + responseCode; else result += " timed out after " + timeoutedAfter + "ms with code " + responseCode + " with exception " + exception.getMessage();
            return result;
        }

        /** the url that this probe result refers to */
        private URL url;

        /** the response code that was returned by the server if not timeouted */
        private int responseCode;

        /** -1 if the server responded; otherwise the milliseconds that the prober
	    waited for a result before giving up.
	  */
        private long timeoutedAfter;

        /** the exception that indicates the failure to establish the connection */
        private IOException exception;
    }
}
