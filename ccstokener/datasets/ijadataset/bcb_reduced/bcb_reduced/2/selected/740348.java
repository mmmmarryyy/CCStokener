package edu.sdsc.grid.gui.applet;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import org.json.JSONObject;
import edu.sdsc.grid.io.RemoteFileSystem;
import edu.sdsc.grid.io.irods.IRODSFileSystem;
import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.GeneralFileSystem;
import edu.sdsc.grid.io.GeneralMetaData;
import edu.sdsc.grid.io.MetaDataSet;
import edu.sdsc.grid.io.MetaDataCondition;
import edu.sdsc.grid.io.MetaDataRecordList;
import edu.sdsc.grid.io.MetaDataSelect;
import edu.sdsc.grid.io.ResourceMetaData;
import edu.sdsc.grid.io.UserMetaData;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The account information of a remote filesystem.
 */
class Account {

    static AppletLogger logger = AppletLogger.getInstance();

    private URI uri;

    private GeneralFileSystem fileSystem;

    private GeneralFile destination, destinationFolder;

    private String zone = "tempZone";

    static String defaultResource = "demoResc";

    private int port;

    private String sessionId;

    /**
   * Remote file system resource list
   */
    private List resourceList = new ArrayList();

    /**
   * Used for parsing uri
   */
    private static String SCHEME_DELIMITER = "://";

    /**
   * Web url for requesting a password for the given uri
   */
    static String TEMP_PASSWORD_SERVICE_URL = "http://www.irods.org/web";

    /**
   * Used by the applet at initialization
   */
    Account(String tempPasswordServiceUrl, URI uri, String sessionId) throws IOException {
        if (uri == null) {
            throw new NullPointerException("iRODS URI cannot be null");
        } else {
            this.uri = uri;
        }
        TEMP_PASSWORD_SERVICE_URL = tempPasswordServiceUrl;
        parseURI(uri, true);
        setSessionId(sessionId);
        setResourceList();
    }

    /**
   * Queries the irods filesystem for available resources
   * saves resources to a List
   */
    private void setResourceList() {
        try {
            MetaDataRecordList[] rl = fileSystem.query(null, new MetaDataSelect[] { MetaDataSet.newSelection(ResourceMetaData.RESOURCE_NAME) });
            for (int i = 0; i < rl.length; i++) {
                String rsc = rl[i].getStringValue(0);
                resourceList.add(rsc);
                DBUtil.getInstance().addResource(((RemoteFileSystem) fileSystem).getHost(), ((RemoteFileSystem) fileSystem).getPort(), rsc);
            }
        } catch (Exception e) {
            logger.log("Account.class. Problem querying for resource list. " + e);
        }
    }

    List getResourceList() {
        return resourceList;
    }

    public void parseURI(URI uri, boolean isInit) throws IOException {
        if (uri == null) return;
        if (isInit) {
            destinationFolder = FileFactory.newFile(uri, getPassword(uri));
            fileSystem = destinationFolder.getFileSystem();
        } else {
            fileSystem = FileFactory.newFile(uri, getPassword(uri)).getFileSystem();
        }
    }

    /**
   * In case of need to change upload destination after applet start
   */
    void setURI(URI uri) throws IOException {
        parseURI(uri, true);
    }

    URI getURI() {
        return uri;
    }

    void setSessionId(String sessionId) {
        if (sessionId != null && !sessionId.trim().equals("")) this.sessionId = sessionId;
    }

    String getSessionId() {
        return sessionId;
    }

    GeneralFile getDestinationFolder() {
        return destinationFolder;
    }

    void setDefaultResource() {
        defaultResource = ((IRODSFileSystem) fileSystem).getDefaultStorageResource();
    }

    String getDefaultResource() {
        return defaultResource;
    }

    public String _getPassword(URI uri) {
        String result = uri.getUserInfo();
        int index = result.indexOf(":");
        if (index >= 0) {
            return result.substring(index + 1);
        } else {
            return null;
        }
    }

    public String getPassword(URI uri) {
        if (_getPassword(uri) != null) return _getPassword(uri);
        String result = null;
        try {
            String sUri = scrubURI(uri);
            URL url = new URL(TEMP_PASSWORD_SERVICE_URL + "?SID=" + sessionId + "&ruri=" + URLEncoder.encode(sUri, "UTF-8"));
            JSONObject jsonObject = null;
            URLConnection conn = url.openConnection();
            InputStream istream = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(istream));
            if ((result = in.readLine()) != null) {
                jsonObject = new JSONObject(result);
            }
            if (jsonObject.has("success")) {
                if (jsonObject.get("success").toString().equals("false")) {
                    if (jsonObject.has("error")) {
                        logger.log("Returned error message from temporary password service is: " + jsonObject.get("error"));
                    }
                    return null;
                }
            }
            if (jsonObject.has("temppass")) {
                result = (String) jsonObject.get("temppass");
            }
        } catch (java.io.FileNotFoundException fe) {
            logger.log("Could not find temporary password service. " + fe);
            fe.printStackTrace();
        } catch (Exception e) {
            logger.log("Exception getting temporary password. " + e);
            e.printStackTrace();
        }
        if (result == null) return null;
        return result;
    }

    public String scrubURI(URI realURI) {
        String result = realURI.toString();
        int i = result.indexOf(SCHEME_DELIMITER);
        try {
            if (i >= 0) {
                return result.substring(i + SCHEME_DELIMITER.length());
            }
        } catch (StringIndexOutOfBoundsException e) {
            logger.log("Problem scrubbing uri. " + e);
        }
        return result;
    }
}
