package net.exclaimindustries.fotobilder.network;

import net.exclaimindustries.fotobilder.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import net.exclaimindustries.tools.*;
import org.apache.http.*;

/**
 * <p><code>FBConnection</code> is the class from which other FotoBilder connection
 * classes derive.  Alone, this defines the basics of FotoBilder communication,
 * such as setting up the connection, getting a challenge, grabbing XML, and
 * other common goodies.  The child classes are what do the specific work.</p>
 *
 * <p>Note that all credentials come from a LoginCredentials class.  For the sake
 * of convenience, the accessors in this class just call the accessors in that
 * class.</p>
 *
 *<p>TODO: Proxy support isn't in yet.</p>
 *
 * @author Nicholas Killewald
 */
public abstract class FBConnection implements Runnable {

    private static Queue<String> challenges = new LinkedList<String>();

    /** Credentials to be used in this connection. */
    protected LoginCredentials creds;

    protected int errorcode = 0;

    protected String errortext = "";

    protected boolean error = false;

    private Exception lastException;

    private String lastRawXML;

    /**
     * Returns the last XML recieved by the server.  Or, if the XML failed to
     * parse, returns whatever the last thing the server said to us was.
     *
     * @return the last server response this FBConnection fielded
     */
    public String getLastRawXML() {
        return lastRawXML;
    }

    public String getUser() {
        return creds.getUser();
    }

    public String getPasswordHash() {
        return creds.getPasswordHash();
    }

    public String getHost() {
        return creds.getHost();
    }

    public String getPath() {
        return creds.getPath();
    }

    public Proxy getProxy() {
        return creds.getProxy();
    }

    public void setLoginCredentials(LoginCredentials creds) {
        this.creds = creds;
    }

    /**
     * Takes the given stream and makes a String out of whatever data it has.
     * Be really careful with this, as it will just attempt to read whatever's
     * in the stream until it stops, meaning it'll spin endlessly if this isn't
     * the sort of stream that ends.
     *
     * @param stream InputStream to read from
     * @return a String consisting of the data from the stream
     */
    public static String getStringFromStream(InputStream stream) throws IOException {
        BufferedReader buff = new BufferedReader(new InputStreamReader(stream));
        StringBuffer tempstring = new StringBuffer();
        char bean[] = new char[1024];
        int read = 0;
        while ((read = buff.read(bean)) != -1) {
            tempstring.append(bean, 0, read);
        }
        return tempstring.toString();
    }

    /**
     * Determines whether or not an error or exception is waiting to be taken care of.
     * Please don't confuse this with the element-level hasError(Element).
     *
     * @return true if an error is waiting, false otherwise
     */
    public boolean hasError() {
        return error;
    }

    /**
     * Returns the last error code encountered by this connection.  If nothing
     * went wrong, this will be zero (there is no equivilant to a 200 OK HTTP
     * response in FB).
     *
     * @return the last error code
     */
    public int getErrorCode() {
        return errorcode;
    }

    /**
     * Returns the text of the last error encountered by this connection.  If
     * nothing went wrong, this will be null (there is no equivilant to a 200 OK
     * HTTP response in FB).
     *
     * @return the text of the last error
     */
    public String getErrorText() {
        return errortext;
    }

    /**
     * Clears the error, if any needs clearing.
     */
    public void clearError() {
        errorcode = 0;
        errortext = "";
        error = false;
    }

    /**
     * Reads in XML from an HttpURLConnection, parses it to an Element (top-level
     * FBResponse element), and puts the raw XML into the lastRawXML field.  If
     * anything goes wrong, this also catches that and throws exceptions accordingly.
     *
     * @param conn HttpURLConnection to read XML from
     * @return an Element object, ready to be searched over
     * @exception SAXException XML parsing error or other DOM problem
     * @exception ParserConfigurationException the parser was incorrectly configured, which would be very odd indeed in this case
     * @exception IOException an I/O exception occured
     * @exception FBConnectionException the FotoBilder server returned a non-OK HTTP response
     * @exception FBErrorException the FotoBilder server returned a top-level error
     */
    protected Element readXML(HttpURLConnection conn) throws org.xml.sax.SAXException, ParserConfigurationException, IOException, FBConnectionException, FBErrorException {
        Document document = readXMLDocument(conn);
        Element fbresponse = document.getDocumentElement();
        if (!fbresponse.getNodeName().equals("FBResponse")) {
            error = true;
            throw new FBConnectionException("FotoBilder server returned unexpected XML");
        }
        if (hasError(fbresponse)) {
            FBErrorException e = new FBErrorException();
            e.setErrorCode(errorcode);
            e.setErrorText(errortext);
            error = true;
            throw e;
        }
        return fbresponse;
    }

    /**
     *<p>
     * Reads in XML from an HttpURLConnection, parses it to a Document, and puts
     * the raw XML into the lastRawXML field.  If anything goes wrong, this also
     * catches that and throws exceptions accordingly.
     *</p>
     *
     *<p>
     * Note that this is a lot like readXML, but it stops before turning it into
     * an Element in case you want the entire Document object.
     *</p>
     *
     * @param conn HttpURLConnection to read XML from
     * @return a Document object, ready to be searched over
     * @exception SAXException XML parsing error or other DOM problem
     * @exception ParserConfigurationException the parser was incorrectly configured, which would be very odd indeed in this case
     * @exception IOException an I/O exception occured
     * @exception FBConnectionException the FotoBilder server returned a non-OK HTTP response
     */
    protected Document readXMLDocument(HttpURLConnection conn) throws org.xml.sax.SAXException, ParserConfigurationException, IOException, FBConnectionException {
        int responsecode = conn.getResponseCode();
        String responsemessage = conn.getResponseMessage();
        if (responsecode != HttpURLConnection.HTTP_OK) {
            error = true;
            throw new FBConnectionException("FotoBilder server returned a non-OK HTTP response: " + responsecode + " " + responsemessage);
        }
        lastRawXML = getStringFromStream(conn.getInputStream());
        InputStream inagain = new ByteArrayInputStream(lastRawXML.getBytes());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inagain);
        return document;
    }

    /**
     * Reads in XML from a Jakarta Commons HttpMethod, parses it to an Element
     * (top-level FBResponse element), and puts the raw XML into the lastRawXML
     * field.  If anything goes wrong, this also catches that and throws
     * exceptions accordingly.
     *
     * @param method HttpMethod to read XML from
     * @return an Element object, ready to be searched over
     * @exception SAXException XML parsing error or other DOM problem
     * @exception ParserConfigurationException the parser was incorrectly configured, which would be very odd indeed in this case
     * @exception IOException an I/O exception occured
     * @exception FBConnectionException the FotoBilder server returned a non-OK HTTP response
     * @exception FBErrorException the FotoBilder server returned a top-level error
     */
    protected Element readXML(HttpResponse response) throws org.xml.sax.SAXException, ParserConfigurationException, IOException, FBConnectionException, FBErrorException {
        Document document = readXMLDocument(response);
        Element fbresponse = document.getDocumentElement();
        if (!fbresponse.getNodeName().equals("FBResponse")) {
            error = true;
            throw new FBConnectionException("FotoBilder server returned unexpected XML");
        }
        if (hasError(fbresponse)) {
            FBErrorException e = new FBErrorException();
            e.setErrorCode(errorcode);
            e.setErrorText(errortext);
            error = true;
            throw e;
        }
        return fbresponse;
    }

    /**
     *<p>
     * Reads in XML from an HttpResponse, parses it to a Document,
     * and puts the raw XML into the lastRawXML field.  If anything goes wrong,
     * this also catches that and throws exceptions accordingly.
     *</p>
     *
     *<p>
     * Note that this is a lot like readXML, but it stops before turning it into
     * an Element in case you want the entire Document object.
     *</p>
     *
     * @param response HttpResponse to read XML from
     * @return a Document object, ready to be searched over
     * @exception SAXException XML parsing error or other DOM problem
     * @exception ParserConfigurationException the parser was incorrectly configured, which would be very odd indeed in this case
     * @exception IOException an I/O exception occured
     * @exception FBConnectionException the FotoBilder server returned a non-OK HTTP response
     */
    protected Document readXMLDocument(HttpResponse response) throws IOException, FBConnectionException, org.xml.sax.SAXException, ParserConfigurationException {
        int responsecode = response.getStatusLine().getStatusCode();
        String responsemessage = response.getStatusLine().getReasonPhrase();
        if (responsecode != HttpURLConnection.HTTP_OK) {
            error = true;
            throw new FBConnectionException("FotoBilder server returned a non-OK HTTP response: " + responsecode + " " + responsemessage);
        }
        lastRawXML = getStringFromStream(response.getEntity().getContent());
        InputStream inagain = new ByteArrayInputStream(lastRawXML.getBytes());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inagain);
        return document;
    }

    /**
     *<p>
     * Check an element to see if FotoBilder dropped an error into it.  Usually,
     * a block-level or document-level node gets errors, but in the case of
     * CreateGals and UploadPrepare, deeper nodes can, too.  But, owing to how
     * this method currently sets errors on a connection-level basis, this
     * shouldn't be used in that case.
     *</p>
     *
     *<p>
     * In FotoBilder, any mode can return an error if, well, there's an error.
     * This could come from any of a vast variety of reasons, stemming from the
     * client feeding the server garbage, the server choking on valid input,
     * a bogus username or authorization check, any amount of things.  Each error
     * has a code associated with it.  While the organization of the codes was
     * inspired by HTTP status responses, the actual numbers have little to nothing
     * to do with them.
     *</p>
     *
     *<p>
     * If an error is found, hasError will return true and set errorcode and
     * errortext.  Those can later be retrieved with getErrorCode and
     * getErrorText outside of the class, but which will probably be accessed
     * via a thrown FBErrorException outside.
     *</p>
     *
     * @param e Element object to check for errors
     * @return true on error (will also set errorcode and errortext), false if no error
     */
    protected boolean hasError(Element e) {
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                if (n.getNodeName().equals("Error")) {
                    NamedNodeMap nnm = n.getAttributes();
                    try {
                        errorcode = Integer.parseInt(nnm.getNamedItem("code").getNodeValue());
                    } catch (NumberFormatException nfe) {
                        errorcode = -100;
                    }
                    NodeList nl = n.getChildNodes();
                    StringBuffer sb = new StringBuffer();
                    for (int j = 0; j < children.getLength(); j++) {
                        if (nl.item(j) instanceof Text) {
                            sb.append(nl.item(j).getNodeValue());
                        }
                    }
                    errortext = sb.toString();
                    error = true;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines whether an item-level element has an error.  This is not to
     * be used with the connection-level or document-level elements.  Just, say,
     * responses from UploadPrepare or CreateGals.
     *
     * @param e Element to check for errors
     * @return true if an error exists, false if not
     * @see #hasError(Element)
     * @see #addError(Element,SingleResponse)
     */
    protected boolean hasErrorSingle(Element e) {
        try {
            DOMUtil.getFirstElement(e, "Error");
            return true;
        } catch (RuntimeException re) {
            return false;
        }
    }

    /**
     *<p>
     * Adds an error contained in the given element to the given SingleResponse-implementing
     * thingy that it applies to.  This is used when parsing multiple Picture or
     * Gallery responses.  Note that this is not to be used on a connection-
     * level basis.  Use haserror(Element) for that.
     *</p>
     *
     *<p>
     * If the Element in question doesn't have an error in it, this does nothing.
     *</p>
     *
     * @param e Element to retrieve an error from
     * @param s SingleResponse-implementing object to add the error code and response to
     * @see #hasErrorSingle(Element)
     * @see #hasError(Element)
     */
    protected void addError(Element e, SingleResponse s) {
        Element er;
        try {
            er = DOMUtil.getFirstElement(e, "Error");
            s.setResponseCode(Integer.parseInt(DOMUtil.getSimpleAttributeText(er, "code")));
            s.setResponseText(DOMUtil.getSimpleElementText(er));
        } catch (RuntimeException re) {
            return;
        }
    }

    /**
     * Sets the last exception encountered by a running thread of this connection.
     * This should only be used in a threaded context.
     *
     * @param e Exception to be set as the last one that run() threw
     */
    public void setLastException(Exception e) {
        lastException = e;
    }

    /**
     * Retrieves the last exception encountered by a running thread of this
     * connection.  Note that this will NOT be set if this was not run using
     * run() (that is, it was run via go()).  If go() was used, the exception
     * will be thrown as normal and will bypass this.
     *
     * @return the last exception run() ran across
     * @see #run()
     * @see #go()
     */
    public Exception getLastException() {
        return lastException;
    }

    public void clearLastException() {
        lastException = null;
    }

    /**
     * Determines if this object has a run()-induced exception waiting to be
     * dealt with.
     *
     * @return true if there is an exception, false otherwise
     */
    public boolean hasException() {
        return !(lastException == null);
    }

    /**
     * Retrieves a single challenge from the server and adds it to the list of
     * useable challenges.
     *
     * @exception MalformedURLException the URL could not be properly formed from the host value
     * @exception IOException an I/O exception occured
     * @exception FBConnectionException a problem occured with the FB server's response, either in XML parsing, XML validity, or HTTP response.  In the case of XML parsing, the exception that caused that will be attached to the FBConnectionException.
     * @exception FBErrorException the FB server returned an error in the challenge request
     */
    public void retrieveChallenge() throws MalformedURLException, IOException, FBConnectionException, FBErrorException {
        URL url = new URL(getHost() + getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-FB-User", getUser());
        conn.setRequestProperty("X-FB-Mode", "GetChallenge");
        conn.connect();
        Element fbresponse;
        try {
            fbresponse = readXML(conn);
        } catch (FBConnectionException fbce) {
            error = true;
            throw fbce;
        } catch (FBErrorException fbee) {
            error = true;
            throw fbee;
        } catch (Exception e) {
            error = true;
            FBConnectionException fbce = new FBConnectionException("XML parsing failed");
            fbce.attachSubException(e);
            throw fbce;
        }
        NodeList nl = fbresponse.getElementsByTagName("GetChallengeResponse");
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element && hasError((Element) nl.item(i))) {
                error = true;
                FBErrorException e = new FBErrorException();
                e.setErrorCode(errorcode);
                e.setErrorText(errortext);
                throw e;
            }
        }
        NodeList challenge = fbresponse.getElementsByTagName("Challenge");
        for (int i = 0; i < challenge.getLength(); i++) {
            NodeList children = challenge.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if (children.item(j) instanceof Text) {
                    challenges.offer(children.item(j).getNodeValue());
                }
            }
        }
    }

    /**
     *<p>
     * Retrieves numerous challenges and adds them to the list of useable
     * challenges.  Note that the challenges are shared among all FBConnection
     * objects (the connection queue is static).
     *</p>
     *
     * @param num number of challenges to retrieve.  <code>1 &lt;= num &lt;= 100</code>
     * @exception MalformedURLException the URL could not be properly formed from the host value
     * @exception IOException an I/O exception occured
     * @exception FBConnectionException a problem occured with the FB server's response, either in XML parsing, XML validity, or HTTP response.  In the case of XML parsing, the exception that caused that will be attached to the FBConnectionException.
     * @exception FBErrorException the FB server returned an error in the challenge request
     */
    public void retrieveChallenges(int num) throws MalformedURLException, IOException, FBErrorException, FBConnectionException {
        if (num < 1 || num > 100) {
            error = true;
            FBErrorException fbee = new FBErrorException();
            fbee.setErrorCode(-100);
            fbee.setErrorText("Invalid GetChallenges range");
            throw fbee;
        }
        URL url = new URL(getHost() + getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-FB-User", getUser());
        conn.setRequestProperty("X-FB-Mode", "GetChallenges");
        conn.setRequestProperty("X-FB-GetChallenges.Qty", new Integer(num).toString());
        conn.connect();
        Element fbresponse;
        try {
            fbresponse = readXML(conn);
        } catch (FBConnectionException fbce) {
            error = true;
            throw fbce;
        } catch (FBErrorException fbee) {
            error = true;
            throw fbee;
        } catch (Exception e) {
            error = true;
            FBConnectionException fbce = new FBConnectionException("XML parsing failed");
            fbce.attachSubException(e);
            throw fbce;
        }
        NodeList nl = fbresponse.getElementsByTagName("GetChallengesResponse");
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element && hasError((Element) nl.item(i))) {
                error = true;
                FBErrorException e = new FBErrorException();
                e.setErrorCode(errorcode);
                e.setErrorText(errortext);
                throw e;
            }
        }
        NodeList challenge = fbresponse.getElementsByTagName("Challenge");
        for (int i = 0; i < challenge.getLength(); i++) {
            NodeList children = challenge.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if (children.item(j) instanceof Text) {
                    challenges.offer(children.item(j).getNodeValue());
                }
            }
        }
    }

    /**
     * Gets a challenge from the current list of useable challenges and removes
     * it from the queue.  If there are no challenges, this calls retrieveChallenge
     * first to get one.
     *
     * @return a String containing a challenge to use
     * @exception FBConnectionException something went wrong with retrieveChallenge (check the attached exception)
     * @exception FBErrorException the FB server returned an error in the challenge request
     * @see #retrieveChallenge()
     */
    public String getChallenge() throws FBConnectionException, FBErrorException {
        if (challenges.isEmpty()) {
            try {
                retrieveChallenge();
            } catch (FBConnectionException fbce) {
                throw fbce;
            } catch (FBErrorException fbee) {
                throw fbee;
            } catch (Exception e) {
                FBConnectionException fbce = new FBConnectionException("Error in challenge retrieving");
                fbce.attachSubException(e);
                throw fbce;
            }
        }
        return challenges.poll();
    }

    /**
     * Creates a response to a challenge and returns it.  This calls getChallenge,
     * so if there are no challenges to apply, it will attempt to retrieve one.
     *
     * @return a String containing a response to a challenge
     * @exception FBConnectionException something went wrong with retrieveChallenge (check the attached exception)
     * @see #retrieveChallenge()
     * @see #getChallenge()
     */
    public String makeResponse() throws FBConnectionException, FBErrorException {
        String challenge = getChallenge();
        return "crp:" + challenge + ":" + MD5Tools.MD5hash(challenge + getPasswordHash());
    }

    /**
     * Returns the number of challenges left in the challenge queue.
     *
     * @return the number of challenges left in the challenge queue
     */
    public static int getChallengesLeft() {
        return challenges.size();
    }

    /**
     * Wipes out all the challenges in the queue.
     */
    public static void clearChallenges() {
        challenges.clear();
    }

    /** 
     * Does just what it says.  go() makes the logic go live and starts the chain
     * of events that leads to the FB mode taking effect.  This is the abstracty
     * part of FBConnection.  Also, go() can be used if you do not want this
     * connection acting in a threaded context for some reason.  If you want to
     * turn this object into a thread, there's a nice handy run() just itching
     * to be used.
     *
     * @throws Exception Something went wrong and wasn't caught.  This is very vague, yes, but a considerable amount can go wrong with a connection.  This is why you most likely want to catch exceptions from specific FBConnection-derived objects, which define specific exceptions to catch.
     */
    public abstract void go() throws Exception;

    /**
     * Wraps around go() to catch exceptions.  Under this, any exception thrown
     * by go() will be caught for later retrieval by getLastException().  This
     * will also clear the last exception caught by run().  This is not only
     * useful for quickly making an <code>FBConnection</code> into a thread,
     * but it's also great for catching exceptions for later in any case.
     *
     * @see #getLastException()
     * @see #setLastException(Exception)
     */
    public void run() {
        clearLastException();
        try {
            go();
        } catch (Exception e) {
            setLastException(e);
        }
    }
}
