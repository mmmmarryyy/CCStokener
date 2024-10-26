package com.chungco.rest.boxnet.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import com.chungco.rest.RestUtils;
import com.chungco.rest.boxnet.exception.BoxException;
import com.chungco.rest.exception.MalformedXmlException;

public class FileDownloadService extends AbstractBoxService<FileDownloadResult> {

    public static final String KEY_SAVE_FOLDER = "save_folder";

    public static final String KEY_FILE_NAME = "file_name";

    public static final String KEY_FILE_ID = "file_id";

    @Override
    public String getEndpointURL() {
        final String sid = getParam(KEY_SID);
        final String fileid = getParam(KEY_FILE_ID);
        return getBoxConfig().getBoxNetHost() + "/ping/download/" + fileid + "/" + sid;
    }

    @Override
    protected FileDownloadResult doRequest(final URL pUrl) throws IOException, BoxException {
        final FileDownloadResult r = new FileDownloadResult();
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            final String endUrl = getEndpointURL();
            final URL url = new URL(endUrl);
            final URLConnection conn = url.openConnection();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bis = new BufferedInputStream(conn.getInputStream());
            final byte[] buf = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = bis.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, bytesRead);
            }
            final String file = getParam(KEY_FILE_NAME);
            final String folder = getParam(KEY_SAVE_FOLDER);
            final File fileOut = new File(folder, file);
            bos = new BufferedOutputStream(new FileOutputStream(fileOut));
            bos.write(baos.toByteArray());
            r.setStatus(FileDownloadResult.SUCCESSFUL_DOWNLOAD);
            r.setXmlResponse(FileDownloadResult.toStatusXml(FileDownloadResult.SUCCESSFUL_DOWNLOAD, "File downloaded to " + fileOut.getAbsolutePath()));
        } finally {
            RestUtils.closeQuietly(bis);
            RestUtils.closeQuietly(bos);
        }
        return r;
    }

    @Override
    protected String loadXml() {
        throw new UnsupportedOperationException("what are you doing here?");
    }

    @Override
    protected FileDownloadResult doParseXml(String pXmlStr) throws MalformedXmlException {
        throw new UnsupportedOperationException("what are you doing here?");
    }

    public void setFileId(final String pFileId) {
        setParam(KEY_FILE_ID, pFileId);
    }

    public void setSaveFolder(final String pFolderPath) {
        setParam(KEY_SAVE_FOLDER, pFolderPath);
    }

    public void setFileName(String pFilename) {
        setParam(KEY_FILE_NAME, pFilename);
    }
}
