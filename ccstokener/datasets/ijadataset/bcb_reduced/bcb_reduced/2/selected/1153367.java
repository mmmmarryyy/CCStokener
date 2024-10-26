package fi.hip.gb.disk.transport.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fi.hip.gb.disk.transport.Transport;

/**
 * 
 * 
 * @author mjpitka2
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class HttpTransport implements Transport {

    private Log log = LogFactory.getLog(HttpTransport.class);

    private String endpointURL = "";

    /**
     * Sets Http connection parameters to the endpoint
     * 
     * @param endpointURL
     *            Endopoint for client connection
     */
    public HttpTransport(String endpointURL) {
        if (endpointURL.charAt(endpointURL.length() - 1) != '/') {
            endpointURL += '/';
        }
        this.endpointURL = endpointURL;
    }

    /**
     * Primitive for uploading a file via existing Transport
     * 
     * @param fileToPut
     *            Absolut path to the file to upload
     * @throws IOException
     *             if failed to upload the file
     */
    public void put(File fileToPut) throws IOException {
        try {
            HttpClient client = new HttpClient();
            MultipartPostMethod mPost = new MultipartPostMethod(this.endpointURL);
            client.setConnectionTimeout(8000);
            log.debug("putting file Length = " + fileToPut.length());
            mPost.addParameter(fileToPut.getName(), fileToPut);
            client.executeMethod(mPost);
            if (mPost.getStatusCode() != 200) {
                throw new IOException("HTTP response not 200 " + mPost.getStatusLine());
            }
            log.debug("put results: statusLine >>" + mPost.getStatusLine());
            mPost.releaseConnection();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Primitive for downloading a file via existing Transport. This method
     * blocks until the file is downloaded.
     * 
     * @param fileToGet
     *            Name of the file to be fetched (160-bit hash)
     * @throws IOException
     *             if failed to download the file
     */
    public void get(File fileToGet) throws IOException {
        String fileName = fileToGet.getName();
        URL url = new URL(this.endpointURL + fileName);
        URLConnection connection = url.openConnection();
        InputStream input = connection.getInputStream();
        log.debug("get: " + fileName);
        try {
            FileOutputStream fileStream = new FileOutputStream(fileToGet);
            byte[] bt = new byte[10000];
            int cnt = input.read(bt);
            log.debug("Read bytes: " + cnt);
            while (cnt != -1) {
                fileStream.write(bt, 0, cnt);
                cnt = input.read(bt);
            }
            input.close();
            fileStream.close();
        } catch (IOException e) {
            new File(fileName).delete();
            throw e;
        }
    }

    /**
     * Primitive for removing a file via existing Transport
     * 
     * @param fileName
     *            Name of the file to be removed (160-bit hash)
     * @throws IOException
     *             if failed to delete the file
     */
    public void delete(String fileName) throws IOException {
        log.debug("deleting: " + fileName);
        URL url = new URL(this.endpointURL + "?operation=delete&filename=" + fileName);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.getInputStream();
    }

    /**
     * Tries to locate the file.
     * 
     * @param fileName
     *            name of the file to be checked
     * @return true if file exists, and is downloadable
     * @throws IOException
     */
    public boolean exists(String fileName) throws IOException {
        log.debug("does exist: " + fileName);
        URL url = new URL(this.endpointURL + "?operation=exists&filename=" + fileName);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        int found = 0;
        InputStream in = null;
        try {
            in = connection.getInputStream();
            found = in.read();
        } finally {
            if (in != null) in.close();
        }
        return (1 == found);
    }
}
