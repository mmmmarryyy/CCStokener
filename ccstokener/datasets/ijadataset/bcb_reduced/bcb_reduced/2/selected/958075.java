package hu.sztaki.lpds.storage.service.carmen.client;

import hu.sztaki.lpds.information.local.PropertyLoader;
import hu.sztaki.lpds.storage.service.carmen.commons.FileSenderUtils;
import hu.sztaki.lpds.storage.service.carmen.commons.FileUtils;
import hu.sztaki.lpds.storage.service.carmen.commons.ZipUtils;
import hu.sztaki.lpds.storage.service.carmen.server.quota.QuotaBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Gets the zipped content of a directory to the storage
 *
 * @author lpds
 */
public class FileSender {

    private String sep;

    private String storageUrlString;

    private String sendFilesDir;

    private String portalURL;

    private String portalID;

    private String userID;

    private String workflowID;

    private String jobID;

    private String runtimeID;

    private Hashtable copyHash;

    private String pidID;

    /**
     * Parameter is the URL of the storage service(receiverServlet).
     *
     * (http://localhost:8080/storage/receiver)
     *
     * @param storageUrlString
     */
    public FileSender(String storageUrlString) {
        this.storageUrlString = storageUrlString;
        sep = FileUtils.getInstance().getSeparator();
    }

    /**
     * Sender parameter settings
     *
     * @param sendFilesDir -
     *            where the files to be sent are
     * @param portalURL
     * @param userID
     * @param workflowID
     * @param jobID
     * @param pidID
     * @param runtimeID
     * @param copyHash
     * @throws Exception
     */
    public void setParameters(String sendFilesDir, String portalURL, String userID, String workflowID, String jobID, String pidID, String runtimeID, Hashtable copyHash) throws Exception {
        this.portalURL = portalURL;
        this.portalID = FileUtils.getInstance().convertPortalIDtoDirName(portalURL);
        this.userID = userID;
        this.workflowID = workflowID;
        this.jobID = jobID;
        this.pidID = pidID;
        this.runtimeID = runtimeID;
        this.copyHash = copyHash;
        if (this.copyHash == null) {
            this.copyHash = new Hashtable();
        }
        if (!sendFilesDir.endsWith(sep)) {
            sendFilesDir += sep;
        }
        this.sendFilesDir = sendFilesDir;
        if (!validParameters()) {
            throw new Exception("FilesDir not exist ! or not valid parameters: portalURL, userID, workflowID, jobID, pidID, runtimeID !");
        }
    }

    /**
     * Checks whether the base parameter values are real or not
     *
     * @return
     */
    private boolean validParameters() {
        if ((sendFilesDir != null) && (portalID != null) && (userID != null) && (workflowID != null) && (jobID != null) && (pidID != null) && (runtimeID != null) && (copyHash != null)) {
            if ((!"".equals(sendFilesDir)) && (!"".equals(portalID)) && (!"".equals(userID)) && (!"".equals(workflowID)) && (!"".equals(jobID)) && (!"".equals(pidID)) && (!"".equals(runtimeID))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends the files indicated by the parameters to the storage
     *
     * @param localMode
     *
     * (if the submitter and the storage are on the same machine, than it is true, else false)
     * @throws Exception file sending error
     */
    public void sendFiles(boolean localMode) throws Exception {
        if (localMode) {
            sendLocalFiles(localMode);
        } else {
            sendRemoteFiles();
        }
    }

    /**
     * The files given as basic parameters will be copied (moved) 
     * to the given directory without network communication.
     *
     * not use httpURLConnection.connect()
     *
     * @throws Exception
     */
    private void sendLocalFiles(boolean localMode) throws Exception {
        FileSenderUtils fileSenderUtils = new FileSenderUtils();
        if (validParameters()) {
            String repositoryDir = FileUtils.getInstance().getRepositoryDir();
            String jobbase = repositoryDir + portalID + sep + userID + sep + workflowID + sep + jobID;
            File basedirin = new File(jobbase + sep + "inputs");
            basedirin.mkdirs();
            File basedirout = new File(jobbase + sep + "outputs");
            basedirout.mkdirs();
            File basedirrun = new File(jobbase + sep + "outputs" + sep + runtimeID + sep + pidID);
            basedirrun.mkdirs();
            String runtimeBaseDir = new String(jobbase + sep + "outputs" + sep + runtimeID + sep + pidID);
            long plussQuotaSize = FileUtils.getInstance().moveDirAllFilesToDirectory(sendFilesDir, runtimeBaseDir);
            fileSenderUtils.parseCopyHash(localMode, portalID, userID, workflowID, runtimeID, runtimeBaseDir, copyHash);
            fileSenderUtils.addPlussRtIDQuotaSizeBean(localMode, new QuotaBean(portalID, userID, workflowID, runtimeID, new Long(plussQuotaSize)));
        } else {
            try {
                fileSenderUtils.parseCopyHash(localMode, portalID, userID, workflowID, runtimeID, sendFilesDir, copyHash);
                throw new Exception("FilesDir not exist ! or not valid parameters: portalID, userID, workflowID, jobID, pidID, runtimeID !");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if ("true".equals(PropertyLoader.getInstance().getProperty("guse.storageclient.localmode.sendquota"))) {
            fileSenderUtils.sendQuotaInformationsToStorage(storageUrlString);
        }
    }

    /**
     * Sends the files given in the basic parameters
     * with POST request to the given servlet.
     *
     */
    private void sendRemoteFiles() throws Exception {
        if (validParameters()) {
            URL url = new URL(storageUrlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            RequestUtils requestUtils = new RequestUtils();
            requestUtils.preRequestAddParameter("senderObj", "FileSender");
            requestUtils.preRequestAddParameter("wfiType", "zen");
            requestUtils.preRequestAddParameter("portalID", this.portalID);
            requestUtils.preRequestAddParameter("userID", this.userID);
            requestUtils.preRequestAddParameter("workflowID", this.workflowID);
            requestUtils.preRequestAddParameter("jobID", this.jobID);
            requestUtils.preRequestAddParameter("pidID", this.pidID);
            requestUtils.preRequestAddParameter("runtimeID", this.runtimeID);
            requestUtils.preRequestAddParameter("copyhash", getCopyHashStr());
            String zipFileName = ZipUtils.getInstance().getUniqueZipFileName();
            requestUtils.preRequestAddFile("zipFileName", zipFileName);
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
                ZipUtils.getInstance().sendDirAllFilesToStream(sendFilesDir, out);
                byte[] postBytes = requestUtils.getPostRequestStringBytes();
                out.write(postBytes);
                out.flush();
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                in.readLine();
                in.close();
                if (HttpURLConnection.HTTP_OK != httpURLConnection.getResponseCode()) {
                    throw new Exception("response not HTTP_OK !");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Cannot connect to: " + storageUrlString, e);
            }
        } else {
            throw new Exception("FilesDir not exist ! or not valid parameters: portalID, userID, workflowID, jobID, pidID, runtimeID !");
        }
    }

    /**
     * Returns the copyhash descriptor hash value:
     * first row is key, second row is value and so on..
     *
     * @return copyhash string
     */
    private String getCopyHashStr() {
        String copyHashStr = "";
        try {
            Enumeration enumKey = copyHash.keys();
            while (enumKey.hasMoreElements()) {
                String filePath = (String) enumKey.nextElement();
                String fileName = (String) copyHash.get(filePath);
                copyHashStr += filePath + "\r\n";
                copyHashStr += fileName + "\r\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyHashStr += "end" + "\r\n";
        return copyHashStr;
    }
}
