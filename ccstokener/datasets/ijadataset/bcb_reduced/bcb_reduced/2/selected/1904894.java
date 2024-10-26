package uk.icat3.io.ids;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.icat3.io.entity.FileId;

public class IDSUploader {

    private static final Logger logger = LoggerFactory.getLogger(IDSUploader.class);

    private URL idsURL;

    private static final String idsBaseUrl = "http://localhost:12345/";

    private final String sessionId;

    public IDSUploader(String sessionId) throws MalformedURLException {
        this.sessionId = sessionId;
        idsURL = new URL(idsBaseUrl);
    }

    public void uploadFile(FileId fileId, byte[] content) throws Exception {
        HttpURLConnection urlConnection = getURLConnection(fileId, false);
        OutputStream out = urlConnection.getOutputStream();
        out.write(content);
        out.close();
        if (!checkResponse(urlConnection)) {
            System.err.println("kuku");
        }
        urlConnection.disconnect();
    }

    private HttpURLConnection getURLConnection(FileId fileId, boolean update) throws Exception {
        HttpURLConnection urlc = null;
        urlc = (HttpURLConnection) idsURL.openConnection();
        urlc.setRequestMethod("POST");
        urlc.setDoOutput(true);
        urlc.setDoInput(true);
        urlc.setUseCaches(false);
        urlc.setAllowUserInteraction(false);
        urlc.addRequestProperty("sessionid", sessionId);
        urlc.addRequestProperty("filename", fileId.getName());
        urlc.addRequestProperty("filedirectory", fileId.getLocation());
        urlc.addRequestProperty("datastreamname", fileId.getName());
        urlc.addRequestProperty("datafileformat", getFileExtension(fileId));
        urlc.addRequestProperty("datasetid", Long.toString(fileId.getDatasetId()));
        urlc.addRequestProperty("update", update ? "True" : "False");
        urlc.addRequestProperty("content-type", "application/octet-stream");
        return urlc;
    }

    private boolean checkResponse(HttpURLConnection urlc) throws Exception {
        try {
            int responseCode = urlc.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        logger.debug("Http receiver says: " + line);
                        System.err.println(line);
                    }
                    return true;
                } catch (IOException e) {
                    throw new Exception("IOException while reading response " + e.getMessage());
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                        }
                    }
                }
            } else if (responseCode == 401) {
                System.err.println("Unauthorized!");
                return false;
            } else {
                String msg = "Http receiver has code: " + responseCode + " " + urlc.getResponseMessage();
                logger.error(msg);
                System.err.println(msg);
                throw new Exception(msg);
            }
        } catch (IOException e) {
            throw new Exception("IOException while checking response " + e.getMessage());
        }
    }

    private String getFileExtension(FileId fileId) {
        String name = fileId.getName();
        return name.contains("\\.") ? name.substring(name.lastIndexOf('.') + 1) : "";
    }
}
