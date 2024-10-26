package com.alexmcchesney.versionchecker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import com.alexmcchesney.versionchecker.jaxb.*;

/**
 * Class that checks a given url for a release xml file
 * describing the latest app version.
 * 
 * @author amcchesney
 *
 */
public class VersionChecker {

    /** URL to fetch */
    private String m_sURL;

    /** Current version of app */
    private String m_sVersion;

    /** ID of the product to get version from */
    private String m_sProductID;

    /** Package path to the jaxb context */
    private static final String JAXB_CONTEXT = "com.alexmcchesney.versionchecker.jaxb";

    /** Singleton JAXB context */
    private static JAXBContext m_context;

    /**
	 * Constructor
	 * @param sURL		URL from which we expect to get release details
	 * @param sVersion	Current version of the application
	 */
    public VersionChecker(String sURL, String sVersion, String sProductID) {
        m_sURL = sURL;
        m_sVersion = sVersion;
        m_sProductID = sProductID;
    }

    /**
	 * Contacts the update site and checks for the latest version.
	 * @return VersionDetails object returned if a new version
	 * is available.  Otherwise, null.
	 * @throws RedirectException	Thrown if the server has indicated that
	 * the old update url is obsolete and has been replaced.  Clients
	 * should get the new url from this exception, try again, and, if successful,
	 * update themselves to use that url from now on.
	 * @throws HttpException	Thrown if we are unable to connect to the server
	 * @throws InvalidXMLException	Thrown if we were able to connect to the server, but
	 * the response was not valid xml
	 * @throws UnknownMessageException	Thrown if we were able to connect to the server
	 * and got xml back, but could not understand it.
	 */
    public VersionDetails checkVersion() throws RedirectException, HttpException, InvalidXMLException, UnknownMessageException {
        VersionDetails newVersion = null;
        ReleaseInfo releaseInfo = getReleaseInfoFromServer();
        String sRedirect = releaseInfo.getRedirect();
        if (sRedirect != null && sRedirect.length() > 0) {
            throw new RedirectException(sRedirect);
        }
        Release releaseXML = releaseInfo.getRelease();
        List<Product> products = (List<Product>) releaseXML.getProduct();
        for (Product product : products) {
            if (product.getName().equals(m_sProductID)) {
                Stable stableRelease = product.getStable();
                ReleaseDetails stableReleaseDetails = stableRelease.getReleaseDetails();
                String sReleaseVersionNumber = stableReleaseDetails.getVersion();
                if (!sReleaseVersionNumber.equals(m_sVersion)) {
                    if (versionNumberIsGreater(m_sVersion, sReleaseVersionNumber)) {
                        newVersion = new VersionDetails(sReleaseVersionNumber, stableReleaseDetails.getDownloadURL(), stableReleaseDetails.getReleaseNotesURL());
                    }
                }
                break;
            }
        }
        return newVersion;
    }

    /**
	 * Calls the server and attempts to get a release info jaxb object from
	 * the resulting xml.
	 * 
	 * @return	ReleaseInfo object representing the returned xml
	 * 
	 * @throws HttpException	Thrown if we are unable to connect to the server
	 * @throws InvalidXMLException	Thrown if we were able to connect to the server, but
	 * the response was not valid xml
	 * @throws UnknownMessageException	Thrown if we were able to connect to the server
	 * and got xml back, but could not understand it.
	 */
    private ReleaseInfo getReleaseInfoFromServer() throws RedirectException, HttpException, InvalidXMLException, UnknownMessageException {
        URL url = null;
        try {
            url = new URL(m_sURL);
        } catch (MalformedURLException e) {
            throw new HttpException(e);
        }
        URLConnection con = null;
        InputStream stream = null;
        ReleaseInfo release = null;
        try {
            con = url.openConnection();
            con.connect();
            if (con instanceof HttpURLConnection) {
                int iResponseCode = ((HttpURLConnection) con).getResponseCode();
                if (iResponseCode != 200) {
                    throw new HttpException(m_sURL, iResponseCode);
                }
            }
            stream = con.getInputStream();
            Object resultObject = null;
            try {
                if (m_context == null) {
                    m_context = JAXBContext.newInstance(JAXB_CONTEXT, this.getClass().getClassLoader());
                }
                Unmarshaller u = m_context.createUnmarshaller();
                resultObject = u.unmarshal(stream);
            } catch (JAXBException jaxbEx) {
                throw new InvalidXMLException(jaxbEx);
            }
            if (!(resultObject instanceof ReleaseInfo)) {
                throw new UnknownMessageException();
            }
            release = (ReleaseInfo) resultObject;
        } catch (IOException e) {
            throw new HttpException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            if (con != null && con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).disconnect();
            }
        }
        return release;
    }

    /**
	 * Analyses two version numbers and works out if the new one
	 * is genuinely higher than the old
	 * 
	 * @param sOldVersion	The "old" version number.
	 * @param sNewVersion	The "new" version number.
	 * @return	True if sNewVersion appears to be higher than sOldVersion
	 */
    private boolean versionNumberIsGreater(String sOldVersion, String sNewVersion) {
        boolean bIsGreater = false;
        String[] oldSections = sOldVersion.split("\\.");
        String[] newSections = sNewVersion.split("\\.");
        int iTotalSections = oldSections.length;
        if (newSections.length > iTotalSections) {
            iTotalSections = newSections.length;
        }
        for (int i = 0; i < iTotalSections; i++) {
            int iOldValue = 0;
            int iNewValue = 0;
            if (i < oldSections.length) {
                iOldValue = Integer.parseInt(oldSections[i]);
            }
            if (i < newSections.length) {
                iNewValue = Integer.parseInt(newSections[i]);
            }
            if (iNewValue > iOldValue) {
                bIsGreater = true;
                break;
            } else if (iNewValue < iOldValue) {
                break;
            }
        }
        return bIsGreater;
    }
}
