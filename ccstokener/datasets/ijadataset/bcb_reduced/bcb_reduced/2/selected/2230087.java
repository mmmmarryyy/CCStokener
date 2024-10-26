package org.mooym;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.mooym.incident.FeedbackManager;
import org.mooym.incident.FeedbackManagerFactory;

/**
 * Manages Internet connections.
 * 
 * In Mooym, the user can specify as much different connections (with Proxy
 * etc.) as he wants. It is up to Mooym to test which one is currently valid.
 * This is the responsibility of this class together with some common
 * functionality.
 * 
 * @author roesslerj
 * 
 */
public class InternetManager {

    private static final int END_OF_STREAM = -1;

    private static InternetManager instance;

    private static final FeedbackManager feedback = FeedbackManagerFactory.getInstance(InternetManager.class);

    /**
   * Singleton retrieval method.
   * 
   * @return The single instance of the {@link InternetManager}.
   */
    public static InternetManager getInstance() {
        if (instance == null) {
            instance = new InternetManager();
        }
        return instance;
    }

    /**
   * Downloads the specified jar file into the local plugIns folder.
   * 
   * @param fileName
   *          The name of the file to download.
   * 
   * @throws IOException
   *           If the connection to the server encounters problems.
   */
    public void downloadJar(String sourceURL, String fileName, String md5Server) throws IOException {
        feedback.informPassiv("Downloading file '{}'.", sourceURL);
        HttpURLConnection sourceConnection = null;
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        String md5Client;
        try {
            sourceConnection = getHttpConnection(sourceURL);
            sourceConnection.setRequestProperty("Accept-Encoding", "zip, jar");
            sourceConnection.connect();
            inputStream = new BufferedInputStream(sourceConnection.getInputStream());
            outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
            MD5Checker md5Checker = new MD5Checker();
            byte[] buffer = new byte[1024];
            for (int bytesRead = inputStream.read(buffer); bytesRead != END_OF_STREAM; bytesRead = inputStream.read(buffer)) {
                outputStream.write(buffer);
                md5Checker.update(buffer);
            }
            md5Client = md5Checker.getValue();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
            if (sourceConnection != null) {
                sourceConnection.disconnect();
            }
        }
        if (!md5Client.equals(md5Server)) {
            File localFile = new File(fileName);
            localFile.delete();
            throw new IOException("MD5 checksum did not match for file '" + fileName + "': " + md5Client + "!=" + md5Server);
        }
    }

    public HttpURLConnection getHttpConnection(String urlString) throws IOException {
        try {
            URL url = new URL(urlString);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException exc) {
            throw new RuntimeException("Configured URL caused a MalformedURLException: ", exc);
        }
    }

    /**
   * Retrieves the document represented by the given URL and returns it as a
   * simple text String.
   * 
   * @param targetURL
   *          The URL to retrieve the text content from.
   * @throws IOException
   *           If the connection encounters problems.
   */
    public String getTextContent(final String targetURL) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = getHttpConnection(targetURL);
            conn.connect();
            InputStreamReader in = new InputStreamReader((InputStream) conn.getContent());
            BufferedReader buff = new BufferedReader(in);
            String line = buff.readLine();
            StringBuffer text = new StringBuffer();
            while (line != null) {
                text.append(line + "\n");
                line = buff.readLine();
            }
            return text.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
