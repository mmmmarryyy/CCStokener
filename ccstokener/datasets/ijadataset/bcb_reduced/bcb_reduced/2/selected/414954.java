package net.exclaimindustries.fotobilder;

import java.net.*;
import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import net.exclaimindustries.tools.DOMUtil;

/**
 * <p>
 * <code>FBCreateOneGalConnection</code> is a simplified method of uploading a
 * single gallery.  This uses the standard CreateGals mode but only with one
 * gallery at a time, saving you the uncertainties of uploading a chain of
 * galleries (only the end of the chain gets an ID returned to it).
 *</p>
 *
 *<p>
 * While this is intended to be used, for instance, on every "create new gallery"
 * operation from the GUI (thus it will always create a new gallery under an
 * already existing gallery), this WILL account for a gallery without a known
 * parent and upload the entire directory chain with it.
 *</p>
 *
 *<p>
 * Also, as FB can't handle multiple parenting on the client protocol yet, any
 * gallery uploaded through here will only have one parent.  This will be fixed
 * when the protocol changes.
 *</p>
 *
 * @author Nicholas Killewald
 */
public class FBCreateOneGalConnection extends FBConnection {

    /**
     * Stores the gallery ready for uploadering.
     */
    private Gallery gallery;

    /** 
     * Creates a new instance of FBCreateOneGalConnection with the given credentials.
     *
     * @param creds credentials to set
     */
    public FBCreateOneGalConnection(LoginCredentials creds) {
        this.creds = creds;
    }

    /**
     * Creates a new instance of FBCreateOneGalConnection using the specified
     * gallery to create.
     * 
     * @param creds credentials to set
     * @param g Gallery object to use
     */
    public FBCreateOneGalConnection(LoginCredentials creds, Gallery g) {
        this.creds = creds;
        gallery = g;
    }

    /**
     * Sets the gallery to be uploaded.
     * 
     * @param g Gallery to be uploaded
     */
    public void setGallery(Gallery g) {
        gallery = g;
    }

    /**
     * Returns the gallery currently slated to be uploaded.
     * 
     * @return the Gallery to be uploaded
     */
    public Gallery getGallery() {
        return gallery;
    }

    /**
     * Uploads one gallery to the FotoBilder server.  This assumes there is some
     * identified gallery parenting it.  If not, we have a problem.  Badly.
     * After uploading, all metadata is also set as need be.
     * 
     * @param g Gallery to upload
     * @return true on success, false on failure
     * @throws FBConnectionException an error with the connection happened
     * @throws FBErrorException the FB server returned an error
     * @throws MalformedURLException the URL to the FB interface was bogus
     * @throws IOException an exception got thrown from the connection itself
     */
    private boolean uploadOneGallery(Gallery g) throws FBConnectionException, FBErrorException, MalformedURLException, IOException {
        URL url = new URL(getHost() + getPath());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-FB-User", getUser());
        conn.setRequestProperty("X-FB-Auth", makeResponse());
        conn.setRequestProperty("X-FB-Mode", "CreateGals");
        conn.setRequestProperty("X-FB-CreateGals.Gallery._size", "1");
        if (!gallery.isTopLevel()) {
            conn.setRequestProperty("X-FB-CreateGals.Gallery.0.ParentID", Integer.toString(g.getIdentifiedParent()));
        }
        conn.setRequestProperty("X-FB-CreateGals.Gallery.0.GalName", g.getName());
        conn.setRequestProperty("X-FB-CreateGals.Gallery.0.GalSec", Integer.toString(g.getSecurity()));
        if (!gallery.isUndated()) {
            conn.setRequestProperty("X-FB-CreateGals.Gallery.0.GalDate", gallery.getDate());
        }
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
        NodeList gal = fbresponse.getElementsByTagName("CreateGalsResponse");
        for (int i = 0; i < gal.getLength(); i++) {
            Element curelement = (Element) gal.item(i);
            if (hasError(curelement)) {
                FBErrorException fbee = new FBErrorException();
                fbee.setErrorCode(errorcode);
                fbee.setErrorText(errortext);
                throw fbee;
            }
        }
        gal = fbresponse.getElementsByTagName("Gallery");
        for (int i = 0; i < gal.getLength(); i++) {
            Element curelement = (Element) gal.item(i);
            if (hasError(curelement)) {
                FBErrorException fbee = new FBErrorException();
                fbee.setErrorCode(errorcode);
                fbee.setErrorText(errortext);
                throw fbee;
            }
            try {
                g.setURL(DOMUtil.getSimpleElementText(curelement, "GalURL"));
                g.setName(DOMUtil.getSimpleElementText(curelement, "GalName"));
                g.setID(Integer.parseInt(DOMUtil.getSimpleElementText(curelement, "GalID")));
            } catch (Exception e) {
                System.out.println("HEY!  Metadata failed to parse on gallery " + g.getName() + "!");
            }
        }
        conn.disconnect();
        return true;
    }

    /**
     * Spin me the connection...
     *
     * @throws BrokenGalleryTreeException No Gallery was defined or a serious error occured within the Gallery
     * @throws FBConnectionException The FB server returned a non-OK HTTP response or XML parsing failed (check sub-exception)
     * @throws FBErrorException The FB server returned an error
     * @throws IOException A problem occured with the connection
     */
    public void go() throws BrokenGalleryTreeException, FBConnectionException, FBErrorException, IOException {
        clearError();
        if (gallery == null) {
            error = true;
            errorcode = -102;
            errortext = "No Gallery to upload was defined";
            throw new BrokenGalleryTreeException("No Gallery to upload was defined");
        }
        if (gallery.hasIdentifiedParent()) {
            uploadOneGallery(gallery);
        } else {
            Stack<Gallery> uploadstack = gallery.getWayUp();
            Queue<Gallery> toupload = new LinkedList<Gallery>();
            while (!uploadstack.empty()) {
                Gallery gnow = uploadstack.pop();
                if (gnow instanceof RootGallery) continue;
                if (gnow.isIdentified()) continue;
                toupload.offer(gnow);
            }
            if (toupload.isEmpty()) {
                throw new BrokenGalleryTreeException("A new gallery has no identified parents, but it couldn't find any way up to the root for uploading.");
            }
            while (!toupload.isEmpty()) {
                Gallery ggo = toupload.poll();
                uploadOneGallery(ggo);
            }
        }
    }
}
