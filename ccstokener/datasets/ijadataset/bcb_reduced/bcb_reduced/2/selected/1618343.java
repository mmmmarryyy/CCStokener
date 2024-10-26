package net.exclaimindustries.fotobilder.network;

import net.exclaimindustries.fotobilder.*;
import java.net.*;
import org.w3c.dom.*;
import net.exclaimindustries.tools.DOMUtil;
import java.io.IOException;

/**
 * FotoBilder allows for security groups to determine who can view what picutres,
 * which the <code>FBGetSecGroupsConnection</code> object retrieves.  Of couse,
 * what this retrieves only lists what's valid as a group and what each group's
 * name is.  Only the IDs are dealt with when sending to the server, but with
 * this information in hand, the UI can display the names of the groups instead
 * of just numbers.
 *
 * @author Nicholas Killewald
 * @see FBSecurity
 */
public class FBGetSecGroupsConnection extends FBConnection {

    private FBSecurity security;

    /** 
     * Creates a new instance of FBGetSecGroupsConnection.  This will create a
     * new FBSecurity object attached to it. 
     */
    public FBGetSecGroupsConnection() {
        security = new FBSecurity();
    }

    /** 
     * Creates a new instance of FBGetSecGroupsConnection with the given
     * credentials.
     *
     * @param creds credentials to set
     */
    public FBGetSecGroupsConnection(LoginCredentials creds) {
        this.creds = creds;
        security = new FBSecurity();
    }

    /**
     * Creates a new instance of FBGetSecGroupsConnection with the given FBSecurity
     * object attached.  And the given credentials.
     *
     * @param creds credentials to set
     * @param security FBSecurity object to attach
     */
    public FBGetSecGroupsConnection(LoginCredentials creds, FBSecurity security) {
        this.creds = creds;
        this.security = security;
    }

    /**
     * Gets the FBSecurity object attached to this connection.  After a successful
     * connection, it will be properly situated with all the groups the user
     * has.
     *
     * @return the FBSecurity object attached to this connection
     */
    public FBSecurity getSecurity() {
        return security;
    }

    /**
     * Sets the connection in motion once everything's set up and makes something
     * out of it.
     *
     * @throws FBConnectionException The FB server returned a non-OK HTTP response or XML parsing failed (check sub-exception)
     * @throws FBErrorException The FB server returned an error
     * @throws IOException A problem occured with the connection
     */
    public void go() throws FBConnectionException, FBErrorException, IOException {
        clearError();
        URL url = new URL(getHost() + getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-FB-User", getUser());
        conn.setRequestProperty("X-FB-Auth", makeResponse());
        conn.setRequestProperty("X-FB-Mode", "GetSecGroups");
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
        NodeList nl = fbresponse.getElementsByTagName("GetSecGroupsResponse");
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element && hasError((Element) nl.item(i))) {
                error = true;
                FBErrorException e = new FBErrorException();
                e.setErrorCode(errorcode);
                e.setErrorText(errortext);
                throw e;
            }
        }
        nl = fbresponse.getElementsByTagName("SecGroup");
        for (int i = 0; i < nl.getLength(); i++) {
            NamedNodeMap nnm = nl.item(i).getAttributes();
            int tempid = 0;
            try {
                tempid = Integer.parseInt(nnm.getNamedItem("id").getNodeValue());
            } catch (Exception e) {
                continue;
            }
            String tempname = DOMUtil.getSimpleElementText((Element) nl.item(i), "Name");
            security.addGroup(tempid, tempname);
        }
        return;
    }
}
