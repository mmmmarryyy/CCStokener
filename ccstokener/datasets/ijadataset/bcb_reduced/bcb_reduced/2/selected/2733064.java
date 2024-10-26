package net.exclaimindustries.fotobilder;

import java.net.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import net.exclaimindustries.tools.DOMUtil;

/**
 * <p>
 * One of two methods of grabbing the gallery tree (graph) from the server (and
 * fortunately the one this API uses), <code>FBGetGalsConnection</code> will
 * retrieve the gallery tree to a RootGallery object.  From there, the rest
 * of the program can act upon it as it sees fit.
 * </p>
 * 
 * <p>
 * <code>FBGetGalsConnection</code> uses the GetGals mode to retrieve the tree.
 * </p>
 * 
 * @author Nicholas Killewald
 */
public class FBGetGalsConnection extends FBConnection {

    private RootGallery rg;

    /**
     * Creates a new instance of FBGetGalsConnection, complete with a new
     * RootGallery to act on.  Remember to grab it later.  Don't use this, use
     * the one that involves credentials.
     */
    public FBGetGalsConnection() {
        rg = new RootGallery();
    }

    /** 
     * Creates a new instance of FBGetGalsConnection with the given credentials
     * and a brand new RootGallery to act on.
     *
     * @param creds credentials to set
     */
    public FBGetGalsConnection(LoginCredentials creds) {
        this.creds = creds;
        rg = new RootGallery();
    }

    /**
     * Creates a new instace of FBGetGalsConnection, attaching the specified
     * RootGallery as what will be acted upon.  Note that this will not
     * make a copy of the object, it will just take the reference.
     * 
     * @param creds credentials to set
     * @param rg the RootGallery all these galleries will be added to
     */
    public FBGetGalsConnection(LoginCredentials creds, RootGallery rg) {
        this.creds = creds;
        this.rg = rg;
    }

    /**
     * Attaches the specified RootGallery to this connection.  Note that there
     * MUST be one attached, else the connection will fail.  Also note that this
     * will not make a copy of the object, it will just take the reference.
     * 
     * @param rg the RootGallery all these galleries will be added to
     */
    public void attachRootGallery(RootGallery rg) {
        this.rg = rg;
    }

    /**
     * Retrieves the RootGallery attached to this connection.  This can be
     * useful if the base constructor was counted on to make the root, not an
     * external object.  Once the connection completes successfully, this
     * RootGallery will contain all the Galleries in the tree.
     * 
     * @return the RootGallery attached to this connection
     */
    public RootGallery getRootGallery() {
        return rg;
    }

    /**
     * Go!  Go!  Go!
     *
     * @throws FBConnectionException The FB server returned a non-OK HTTP response or XML parsing failed (check sub-exception)
     * @throws FBErrorException The FB server returned an error
     * @throws IOException A problem occured with the connection
     */
    public void go() throws FBConnectionException, FBErrorException, IOException {
        clearError();
        if (rg == null) {
            error = true;
            errorcode = -102;
            errortext = "No RootGalleryTree was defined";
            return;
        }
        URL url = new URL(getHost() + getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-FB-User", getUser());
        conn.setRequestProperty("X-FB-Auth", makeResponse());
        conn.setRequestProperty("X-FB-Mode", "GetGals");
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
        NodeList gals = fbresponse.getElementsByTagName("Gal");
        for (int i = 0; i < gals.getLength(); i++) {
            Gallery g;
            Element curelement = (Element) gals.item(i);
            try {
                if (DOMUtil.getSimpleElementText(curelement, "Name").startsWith("Tag: ")) {
                    g = new Tag(rg, DOMUtil.getSimpleElementText(curelement, "Name").substring(5), Integer.parseInt(DOMUtil.getSimpleAttributeText(curelement, "id")));
                } else {
                    g = rg.createGallery(Integer.parseInt(DOMUtil.getSimpleAttributeText(curelement, "id")), DOMUtil.getSimpleElementText(curelement, "Name"));
                }
            } catch (Exception e) {
                complain("HEY!  Gallery " + DOMUtil.getSimpleAttributeText(curelement, "id") + " failed to parse!");
                continue;
            }
            try {
                g.setURL(DOMUtil.getSimpleElementText(curelement, "URL"));
                g.setSecurity(Integer.parseInt(DOMUtil.getSimpleElementText(curelement, "Sec")));
            } catch (Exception e) {
                complain("HEY!  Metadata failed on " + (g instanceof Tag ? "tag" : "gallery") + " " + DOMUtil.getSimpleAttributeText(curelement, "id") + "!");
                complain(e.toString());
            }
            try {
                g.setDate(DOMUtil.getSimpleElementText(curelement, "Date"));
            } catch (Exception e) {
            }
        }
        for (int i = 0; i < gals.getLength(); i++) {
            int current;
            Element curelement = (Element) gals.item(i);
            try {
                current = Integer.parseInt(DOMUtil.getSimpleAttributeText(curelement, "id"));
            } catch (Exception e) {
                complain("HEY!  Gallery " + DOMUtil.getSimpleAttributeText(curelement, "id") + " failed to parse!");
                continue;
            }
            Gallery g = rg.getNode(current);
            NodeList parents;
            try {
                parents = DOMUtil.getFirstElement(curelement, "ParentGals").getElementsByTagName("ParentGal");
            } catch (Exception e) {
                complain("HEY!  Parsing failed on gallery " + current + ", so I'm assuming it's unparented!");
                continue;
            }
            for (int j = 0; j < parents.getLength(); j++) {
                try {
                    g.addParent(rg.getNode(Integer.parseInt(DOMUtil.getSimpleAttributeText((Element) parents.item(j), "id"))));
                } catch (Exception e) {
                    complain("HEY!  Adding parent to gallery " + current + " failed!");
                    continue;
                }
            }
        }
        return;
    }

    private void complain(String s) {
        System.err.println("FBGetGalsConnection: " + s);
    }

    private void scream(String s) {
        System.out.println("FBGetGalsConnection: " + s);
    }
}
