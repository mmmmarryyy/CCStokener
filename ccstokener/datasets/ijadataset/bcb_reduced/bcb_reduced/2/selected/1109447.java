package org.torrcast.torrents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.torrcast.utils.FileUtils;

/**
 * Used for downloading podcast files.
 * 
 * @author Reidar Øksnevad (reidar@oksnevad.org)
 */
public class FileDownloader {

    private static final Logger logger = Logger.getLogger(FileDownloader.class);

    private static int MAX_RETRIES = 3;

    private DefaultHttpClient httpClient = new DefaultHttpClient();

    private String directory;

    private String filename;

    private long fileSize;

    public FileDownloader(String directory) {
        this.directory = directory;
    }

    /**
     * Download a file.
     * 
     * @param url URL to the file that you want to download.
     * @param filename The filename given to the the downloaded file.
     * @return true if everything went well.
     * @throws ClientProtocolException
     */
    public boolean download(String url) {
        HttpGet httpGet = new HttpGet(url);
        String filename = FileUtils.replaceNonAlphanumericCharacters(url);
        String completePath = directory + File.separatorChar + filename;
        int retriesLeft = MAX_RETRIES;
        while (retriesLeft > 0) {
            try {
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    logger.info("Downloading file from " + url + " -> " + completePath);
                    IOUtils.copy(resEntity.getContent(), new FileOutputStream(completePath));
                    logger.info("File " + filename + " was downloaded successfully.");
                    setFileSize(new File(completePath).length());
                    setFilename(filename);
                    return true;
                } else {
                    logger.warn("Trouble downloading file from " + url + ". Status was: " + response.getStatusLine());
                }
            } catch (ClientProtocolException e) {
                logger.error("Protocol error. This is probably serious, and there's no need " + "to continue trying to download this file.", e);
                return false;
            } catch (IOException e) {
                logger.warn("IO trouble: " + e.getMessage() + ". Retries left: " + retriesLeft);
            }
            retriesLeft--;
        }
        return false;
    }

    /**
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize the fileSize to set
     */
    private void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    private void setFilename(String filename) {
        this.filename = filename;
    }
}
