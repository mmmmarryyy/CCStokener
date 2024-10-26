package hu.sztaki.lpds.repository.service.veronica.server.exporter;

import hu.sztaki.lpds.wfs.com.RepositoryWorkflowBean;
import hu.sztaki.lpds.repository.service.veronica.commons.RepositoryFileUtils;
import hu.sztaki.lpds.repository.service.veronica.commons.RepositoryRequestUtils;
import hu.sztaki.lpds.storage.service.carmen.commons.FileUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Egy zip file-t (workflow-t) ker el, tolt le a storage-rol,
 * es menti le a repository teruletere.
 *
 * @author lpds
 */
public class RepositoryZipFileGetter {

    private String sep;

    private static final int bufferSize = 4 * 1024;

    public RepositoryZipFileGetter() {
        sep = RepositoryFileUtils.getInstance().getSeparator();
    }

    /**
     * A beallitott parameterek alltal meghatarozott
     * zip file-t (workflow-t) elkeri a storage-tol.
     *
     * @param RepositoryWorkflowBean bean - workflow parameterek
     * @return zip file merete byte-ban
     */
    public long getZipFiles(RepositoryWorkflowBean bean) throws Exception {
        return getRemoteZipFiles(bean);
    }

    /**
     * A beallitott parameterek alltal meghatarozott
     * zip file-t (workflow-t) elkeri a storage-tol.
     *
     * not use httpURLConnection.connect()
     *
     * @param RepositoryWorkflowBean bean - workflow parameterek
     *
     * @return zip file merete byte-ban
     * @throws Exception
     */
    private long getLocalZipFiles(RepositoryWorkflowBean bean) throws Exception {
        return 0;
    }

    /**
     * A beallitott parameterek alltal meghatarozott zip file-t
     * (workflow-t) POST keressel keri es kapja meg a storgae-tol.
     *
     * @param RepositoryWorkflowBean bean - workflow parameterek
     *
     * @return zip file merete byte-ban
     * @throws Exception
     */
    private long getRemoteZipFiles(RepositoryWorkflowBean bean) throws Exception {
        File zipFile = new File(bean.getZipFileFullPath());
        FileUtils.getInstance().createDirectory(zipFile.getParent());
        URL url = new URL(bean.getStorageID() + "/download");
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        RepositoryRequestUtils requestUtils = new RepositoryRequestUtils();
        requestUtils.preRequestAddParameter("senderObj", "RepositoryZipFileGetter");
        requestUtils.preRequestAddParameter("portalID", bean.getPortalID());
        requestUtils.preRequestAddParameter("wfsID", bean.getWfsID());
        requestUtils.preRequestAddParameter("userID", bean.getUserID());
        requestUtils.preRequestAddParameter("workflowID", bean.getWorkflowID());
        requestUtils.preRequestAddParameter("downloadType", bean.getDownloadType());
        requestUtils.preRequestAddParameter("instanceType", bean.getInstanceType());
        requestUtils.preRequestAddParameter("outputLogType", bean.getOutputLogType());
        requestUtils.preRequestAddParameter("exportType", bean.getExportType());
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        boolean resp = false;
        int respCode = 0;
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
                int writeLen;
                byte dataBuff[] = new byte[bufferSize];
                InputStream in = httpURLConnection.getInputStream();
                BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(zipFile), bufferSize);
                while ((writeLen = in.read(dataBuff)) > 0) {
                    fileOut.write(dataBuff, 0, writeLen);
                }
                fileOut.flush();
                in.close();
                fileOut.close();
            } else if (respCode == 500) {
                resp = false;
                throw new Exception(httpURLConnection.getResponseMessage());
            } else if (respCode == 560) {
                resp = false;
                throw new Exception("Server Side Remote Exeption !!! respCode = (" + respCode + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            zipFile.delete();
            throw e;
        }
        return zipFile.length();
    }
}
