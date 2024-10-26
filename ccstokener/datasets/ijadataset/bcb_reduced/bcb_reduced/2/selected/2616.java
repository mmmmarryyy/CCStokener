package hu.sztaki.lpds.storage.service.carmen.client;

import hu.sztaki.lpds.storage.service.carmen.commons.FileUtils;
import hu.sztaki.lpds.storage.service.carmen.commons.ZipUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Gets the zipped content of a directory from the storage
 * 
 * @author lpds
 */
public class FileGetter {

    private String sep;

    private String storageUrlString;

    private String repositoryDir;

    private String getFilesDir;

    private String portalURL;

    private String portalID;

    private String userID;

    private Hashtable fileRenameHash;

    /**
     * Parameter is the URL of the storage service(receiverServlet).
     * 
     * (http://localhost:8080/storage/receiver)
     *
     * @param storageUrlString
     */
    public FileGetter(String storageUrlString) {
        this.storageUrlString = storageUrlString;
        this.repositoryDir = FileUtils.getInstance().getRepositoryDir();
        sep = System.getProperty("file.separator");
        if (sep == null) {
            sep = "/";
        }
    }

    /**
     * Searches for parameter settings
     * 

     * @param getFilesDir
     *            where the files will be unpacked
     * @param portalURL
     * @param userID
     * @param fileRenameHash
     *            rename descriptor hash
     * 
     * (key: newName, value: oldPathName)
     * 
     * ("newFileName1.txt", "/testworkflow1/job1/inputs/1/file1.txt")
     * ("newFileName2.txt", "testworkflow2/job2/inputs/2/file2.txt")
     * 
     * @throws Exception
     */
    public void setParameters(String getFilesDir, String portalURL, String userID, Hashtable fileRenameHash) throws Exception {
        this.portalURL = portalURL;
        this.portalID = FileUtils.getInstance().convertPortalIDtoDirName(portalURL);
        this.userID = userID;
        this.fileRenameHash = fileRenameHash;
        if (sep == null) {
            sep = "/";
        }
        if (getFilesDir != null) {
            if (!getFilesDir.endsWith(sep)) {
                getFilesDir += sep;
            }
        }
        FileUtils.getInstance().createDirectory(getFilesDir);
        this.getFilesDir = getFilesDir;
        if (!validParameters()) {
            throw new Exception("FilesDir not exist ! or not valid parameters: portalURL, userID or hashTable !");
        }
    }

    /**
     * Checks whether the base parameter values are real or not
     * 
     * @return
     */
    private boolean validParameters() {
        if ((getFilesDir != null) && (portalID != null) && (userID != null) && (fileRenameHash != null)) {
            if ((!"".equals(getFilesDir)) && (!"".equals(portalID)) && (!"".equals(userID))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the files indicated by the parameters from the storage
     * 
     * @param localMode 
     * 
     * (if the submitter and the storage are on the same machine, than it is true, else false)
     * 
     * @return true/false
     * @throws Exception file access error
     */
    public boolean getFiles(boolean localMode) throws Exception {
        if (localMode) {
            return getLocalFiles();
        } else {
            return getRemoteFiles();
        }
    }

    /**
     * The files given as basic parameters will be copied 
     * to the given directory without network communication.
     * 
     * not use httpURLConnection.connect()
     * 
     * @return true / false
     * @throws Exception
     */
    private boolean getLocalFiles() throws Exception {
        try {
            String userBaseDirStr = repositoryDir + portalID + sep + userID;
            int reCnt = 0;
            while (reCnt < 3) {
                try {
                    FileUtils.getInstance().copyHashAllFilesToDirectory(userBaseDirStr, fileRenameHash, getFilesDir);
                    reCnt = 4;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    reCnt++;
                    System.out.println("reCnt : " + reCnt);
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Server Side Exeption !!!");
        }
        return true;
    }

    /**
     * Requests and gets the files given in the basic 
     * parameters with POST request from the given servlet.
     * 
     * @return true / false
     * @throws Exception
     */
    private boolean getRemoteFiles() throws Exception {
        boolean resp = false;
        int respCode = 0;
        URL url = new URL(storageUrlString);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        RequestUtils requestUtils = new RequestUtils();
        requestUtils.preRequestAddParameter("senderObj", "FileGetter");
        requestUtils.preRequestAddParameter("wfiType", "zen");
        requestUtils.preRequestAddParameter("portalID", this.portalID);
        requestUtils.preRequestAddParameter("userID", this.userID);
        addRenameFileParameters(requestUtils);
        requestUtils.createPostRequest();
        httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + requestUtils.getBoundary());
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        try {
            httpURLConnection.connect();
            OutputStream out = httpURLConnection.getOutputStream();
            byte[] preBytes = requestUtils.getPreRequestStringBytes();
            out.write(preBytes);
            out.flush();
            byte[] postBytes = requestUtils.getPostRequestStringBytes();
            out.write(postBytes);
            out.flush();
            out.close();
            respCode = httpURLConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = true;
                InputStream in = httpURLConnection.getInputStream();
                ZipUtils.getInstance().getFilesFromStream(in, getFilesDir);
                in.close();
            }
            if (respCode == 500) {
                resp = false;
            }
            if (respCode == 560) {
                resp = false;
                throw new Exception("Server Side Remote Exeption !!! respCode = (" + respCode + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Cannot connect to: " + storageUrlString, e);
        }
        return resp;
    }

    /**
     * Regarding to the basic parameter places the required file renaming parameters
     * 
     * @param requestUtils
     */
    private void addRenameFileParameters(RequestUtils requestUtils) {
        Enumeration enumKey = fileRenameHash.keys();
        while (enumKey.hasMoreElements()) {
            String newName = (String) enumKey.nextElement();
            String oldPathName = (String) fileRenameHash.get(newName);
            requestUtils.preRequestAddRenameFile(newName, oldPathName);
        }
    }
}
