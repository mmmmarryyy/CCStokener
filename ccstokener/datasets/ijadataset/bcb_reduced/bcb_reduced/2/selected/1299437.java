package net.exclaimindustries.fotobilder;

import java.net.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import net.exclaimindustries.tools.DOMUtil;

/**
 *<p>
 * <code>FBLoginConnection</code> handles the eerily extensible Login mode of
 * FotoBilder.  The Login mode retrieves information about the account in question,
 * including, but not limited to, server messages, quota space, and last login time.
 *</p>
 *
 *<p>
 * The "but not limited to" part of that last sentence is the tricky part.  There
 * is no defined behavior for Login across multiple FB implementations, so this
 * class must be ready for anything.  While it will automatically read in the
 * data given by LiveJournal's implementation of FB and provide convenience
 * methods to deal with it, any additional data will have to be dealt with
 * manually by reading the raw XML output.  Just as well, the data LJ gives might
 * not be given by other FB servers, so prepare accordingly.
 *</p>
 *
 * @author Nicholas Killewald
 */
public class FBLoginConnection extends FBConnection {

    /** Vanity check! */
    private String agent = "FotoFoo/0.3.0";

    private LoginResults results;

    private long quotatotal = -1;

    private long quotaused = -1;

    private long quotaremaining = -1;

    private String message;

    private String servertime;

    /** 
     * Creates a new instance of FBLoginConnection with the given credentials.
     *
     * @param creds credentials to set
     */
    public FBLoginConnection(LoginCredentials creds) {
        this.creds = creds;
    }

    /**
     *<p>
     * Sets the agent string.  Vanity check!
     *</p>
     *
     *<p>
     * The format of an agent string should be <code>ClientName/MajorVersion.MinorVersion.Revision</code>.
     * This makes no check to make sure it IS like that, though.
     *</p>
     *
     * @param agent the agent string
     */
    public void setAgent(String agent) {
        this.agent = agent;
    }

    /**
     * Returns the current agent string.
     *
     * @return the current agent string
     */
    public String getAgent() {
        return agent;
    }

    /**
     * Returns the login results in a handy-dandy <code>LoginResults</code> object.
     * Refer to THAT Javadoc for more info.
     *
     * @return the results of this login
     * @see LoginResults
     */
    public LoginResults getLoginResults() {
        return results;
    }

    /**
     * Sets the connection in motion once everything's set up and makes a new
     * thread out of it.
     *
     * @throws FBConnectionException The FB server returned a non-OK HTTP response or XML parsing failed (check sub-exception)
     * @throws FBErrorException The FB server returned an error
     * @throws IOException A problem occured with the connection
     */
    public void go() throws FBConnectionException, FBErrorException, IOException {
        clearError();
        results = new LoginResults();
        URL url = new URL(getHost() + getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-FB-User", getUser());
        conn.setRequestProperty("X-FB-Auth", makeResponse());
        conn.setRequestProperty("X-FB-Mode", "Login");
        conn.setRequestProperty("X-FB-Login.ClientVersion", agent);
        conn.connect();
        Element fbresponse;
        try {
            fbresponse = readXML(conn);
        } catch (FBConnectionException fbce) {
            throw fbce;
        } catch (FBErrorException fbee) {
            throw fbee;
        } catch (Exception e) {
            FBConnectionException fbce = new FBConnectionException("XML parsing failed");
            fbce.attachSubException(e);
            throw fbce;
        }
        NodeList nl = fbresponse.getElementsByTagName("LoginResponse");
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element && hasError((Element) nl.item(i))) {
                error = true;
                FBErrorException e = new FBErrorException();
                e.setErrorCode(errorcode);
                e.setErrorText(errortext);
                throw e;
            }
        }
        results.setMessage(DOMUtil.getAllElementText(fbresponse, "Message"));
        results.setServerTime(DOMUtil.getAllElementText(fbresponse, "ServerTime"));
        NodeList quotas = fbresponse.getElementsByTagName("Quota");
        for (int i = 0; i < quotas.getLength(); i++) {
            if (quotas.item(i) instanceof Node) {
                NodeList children = quotas.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j) instanceof Element) {
                        Element working = (Element) children.item(j);
                        if (working.getNodeName().equals("Remaining")) {
                            try {
                                results.setQuotaRemaining(Long.parseLong(DOMUtil.getSimpleElementText(working)));
                            } catch (Exception e) {
                            }
                        }
                        if (working.getNodeName().equals("Used")) {
                            try {
                                results.setQuotaUsed(Long.parseLong(DOMUtil.getSimpleElementText(working)));
                            } catch (Exception e) {
                            }
                        }
                        if (working.getNodeName().equals("Total")) {
                            try {
                                results.setQuotaTotal(Long.parseLong(DOMUtil.getSimpleElementText(working)));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
        results.setRawXML(getLastRawXML());
        return;
    }
}
