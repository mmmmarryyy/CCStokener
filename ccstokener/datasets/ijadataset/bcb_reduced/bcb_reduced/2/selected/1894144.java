package hu.sztaki.lpds.storage.service.carmen.client;

import hu.sztaki.lpds.storage.service.carmen.commons.FileUtils;
import hu.sztaki.lpds.storage.service.carmen.commons.ZipUploadUtils;
import hu.sztaki.lpds.storage.service.carmen.commons.ZipUtils;
import hu.sztaki.lpds.wfs.com.StorageWorkflowNamesBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Sends a zipped file (download workflow zip file) to the storage
 * 
 * @author lpds
 */
public class ZipFileSender {

    private String sep;

    private String storageURL;

    private String sendZipFilePath;

    private String portalURL;

    private String wfsID;

    private String userID;

    private String newGrafName;

    private String newAbstName;

    private String newRealName;

    /**
     * Parameter is the URL of the storage service
     *
     * (http://localhost:8080/storage)
     *
     * (the /receiver is not needed to the end!)
     *
     * @param storageURL
     */
    public ZipFileSender(String storageURL) {
        this.storageURL = storageURL;
        sep = FileUtils.getInstance().getSeparator();
    }

    /**
     * Setting the sending parameters
     *
     * @param sendZipFilePath -
     *            where the zip file to be sent is located (full path)
     * @param portalURL -
     *            portal ID (url)
     * @param wfsID
     * @param userID
     * @param newGrafName -
     *            the name of the uploaded graph workflow
     * @param newAbstName -
     *            the name of the abstract workflow
     * @param newRealName -
     *            the name of the real, concrete workflow
     * @throws Exception
     */
    public void setParameters(String sendZipFilePath, String portalURL, String wfsID, String userID, String newGrafName, String newAbstName, String newRealName) throws Exception {
        if (newGrafName == null) {
            newGrafName = new String("");
        }
        if (newAbstName == null) {
            newAbstName = new String("");
        }
        if (newRealName == null) {
            newRealName = new String("");
        }
        this.portalURL = portalURL;
        this.wfsID = wfsID;
        this.userID = userID;
        this.newGrafName = newGrafName;
        this.newAbstName = newAbstName;
        this.newRealName = newRealName;
        this.sendZipFilePath = sendZipFilePath;
        if (!validParameters()) {
            throw new Exception("Zip file not exist ! or not valid parameters: portalURL, wsfID, userID !");
        }
    }

    /**
     * Checks whether the base parameter values are real or not
     *
     * @return
     */
    private boolean validParameters() {
        if ((sendZipFilePath != null) && (portalURL != null) && (wfsID != null) && (userID != null)) {
            if ((!"".equals(sendZipFilePath)) && (!"".equals(portalURL)) && (!"".equals(wfsID)) && (!"".equals(userID))) {
                File sendZipFile = new File(sendZipFilePath);
                if ((sendZipFile.exists()) && (sendZipFile.isFile()) && (sendZipFile.length() > 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sends the zip file indicated by the parameters 
     * (download workflow zip file) to the storage
     *
     * @param localMode
     *
     * (if the submitter and the storage are on the same machine, than it is true, else false)
     * @throws Exception file sending error
     */
    public void sendZipFile(boolean localMode) throws Exception {
        if (localMode) {
            sendLocalZipFile();
        } else {
            sendRemoteZipFile();
        }
    }

    /**
     * The zip file given as basic parameter will be copied 
     * to the given directory without network communication.
     * 
     * not use httpURLConnection.connect()
     *
     * @throws Exception
     */
    private void sendLocalZipFile() throws Exception {
        if (validParameters()) {
            String repositoryDir = FileUtils.getInstance().getRepositoryDir();
            String portalID = FileUtils.getInstance().convertPortalIDtoDirName(portalURL);
            String userBaseDir = repositoryDir + portalID + sep + userID;
            FileUtils.getInstance().createDirectory(userBaseDir);
            String zipPathName = userBaseDir + sep + ZipUtils.getInstance().getUniqueZipFileName();
            FileUtils.getInstance().copyFileToFileWithPaths(sendZipFilePath, zipPathName);
            File uploadedZipFile = new File(zipPathName);
            StorageWorkflowNamesBean bean = new StorageWorkflowNamesBean();
            bean.setPortalID(portalID);
            bean.setPortalURL(portalURL);
            bean.setStorageURL(storageURL);
            bean.setWfsID(wfsID);
            bean.setUserID(userID);
            bean.setZipFilePathStr(zipPathName);
            bean.setNewMainGrafName(newGrafName);
            bean.setNewMainAbstName(newAbstName);
            bean.setNewMainRealName(newRealName);
            ZipUploadUtils.getInstance().uploadZipFileToStorage(bean);
        } else {
            throw new Exception("Zip file not exist ! or not valid parameters: portalURL, wsfID, userID !");
        }
    }

    /**
     * Sends the zip file given in the basic 
     * parameters with POST request to the given servlet.
     */
    private void sendRemoteZipFile() throws Exception {
        if (validParameters()) {
            URL url = new URL(storageURL + "/receiver");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            RequestUtils requestUtils = new RequestUtils();
            requestUtils.preRequestAddParameter("senderObj", "ZipFileSender");
            requestUtils.preRequestAddParameter("wfiType", "zen");
            requestUtils.preRequestAddParameter("portalURL", this.portalURL);
            requestUtils.preRequestAddParameter("wfsID", this.wfsID);
            requestUtils.preRequestAddParameter("userID", this.userID);
            requestUtils.preRequestAddParameter("newGrafName", this.newGrafName);
            requestUtils.preRequestAddParameter("newAbstName", this.newAbstName);
            requestUtils.preRequestAddParameter("newRealName", this.newRealName);
            String zipFileName = ZipUtils.getInstance().getUniqueZipFileName();
            requestUtils.preRequestAddFile("zipFileName", zipFileName);
            requestUtils.createPostRequest();
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + requestUtils.getBoundary());
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            try {
                httpURLConnection.connect();
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Cannot connect to: " + storageURL, e);
            }
            OutputStream out = httpURLConnection.getOutputStream();
            byte[] preBytes = requestUtils.getPreRequestStringBytes();
            out.write(preBytes);
            out.flush();
            FileUtils.getInstance().sendFileToStream(out, sendZipFilePath);
            byte[] postBytes = requestUtils.getPostRequestStringBytes();
            out.write(postBytes);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String retMess = in.readLine() + "\n";
            while (in.ready()) {
                retMess += in.readLine() + "\n";
            }
            in.close();
            if (HttpURLConnection.HTTP_OK != httpURLConnection.getResponseCode()) {
                throw new Exception("response not HTTP_OK !");
            }
            if (!"Workflow upload successfull".equals(retMess.trim())) {
                throw new Exception("-" + retMess + "-" + ("Workflow upload successfull".equals(retMess)));
            }
        } else {
            throw new Exception("Zip file not exist ! or not valid parameters: portalURL, wsfID, userID !");
        }
    }
}
