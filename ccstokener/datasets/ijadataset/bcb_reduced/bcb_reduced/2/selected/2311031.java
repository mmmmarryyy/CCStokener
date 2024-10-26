package org.pointrel.pointrel20090201.synchronizing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.pointrel.pointrel20090201.ArchiveFileSupport;

public class SimpleHTTPArchiveSynchronizer extends SimpleRemoteDirectoryArchiveSynchronizer {

    String remoteUser;

    String remotePassword;

    String[] remoteFileNames;

    public SimpleHTTPArchiveSynchronizer(String localPath, String remotePath, String remoteUser, String remotePassword, OutputStream logStream) {
        super(localPath, remotePath, logStream);
        this.remoteUser = remoteUser;
        if (remotePassword == null) remotePassword = JOptionPane.showInputDialog("Enter password for remote server:\n" + remotePath + "\nuser: " + remoteUser);
        this.remotePassword = remotePassword;
    }

    public boolean loginToRemote() throws SocketException, IOException {
        try {
            byte[] bytes = this.getBytesForWebPageUsingHTTPClient(this.remotePath);
            if (bytes == null) return false;
            String fileListResult = new String(bytes);
            if (fileListResult.equals("")) {
                remoteFileNames = new String[0];
                return true;
            }
            log("splitting");
            remoteFileNames = fileListResult.split("\n");
            log("got remote file list OK");
            for (String fileName : remoteFileNames) {
                if (!ArchiveFileSupport.isValidTransactionFileName(fileName)) {
                    log("Bad file name: " + fileName);
                    return false;
                }
            }
        } catch (ClientProtocolException e) {
            log(e.getMessage());
            return false;
        }
        return true;
    }

    public void addRemoteFilesToMap(TreeMap<String, String> fileLocations) throws IOException {
        for (String fileName : remoteFileNames) {
            log("Remote File: " + fileName);
            fileLocations.put(fileName, "remote");
        }
    }

    public void storeFileToRemote(String fileName) throws IOException {
        log("Trying to store file to server " + fileName);
        try {
            File file = new File(this.localPath, fileName);
            this.uploadFile(this.remotePath, file);
        } catch (ClientProtocolException e) {
            log(e.getMessage());
        } catch (IOException e) {
            log(e.getMessage());
        }
        log("Done storing ");
    }

    public void retrieveFileFromRemote(String fileName) throws IOException {
        log("trying to read remote file:" + fileName);
        byte[] transactionBytes = getBytesForWebPageUsingHTTPClient(this.remotePath + "?fileName=" + fileName);
        log("File size: " + transactionBytes.length);
        FileOutputStream outputStream = new FileOutputStream(new File(localPath, fileName));
        outputStream.write(transactionBytes);
        outputStream.close();
        log("done with reading file");
    }

    public void uploadFile(String url, File file) throws ClientProtocolException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(this.remoteUser, this.remotePassword));
        HttpPost httpPost = new HttpPost(url);
        log("File1 Length = " + file.length());
        FileBody bin = new FileBody(file);
        StringBody comment = new StringBody("A Pointrel transaction file");
        MultipartEntity multipartEntityForPost = new MultipartEntity();
        multipartEntityForPost.addPart("uploaded", bin);
        multipartEntityForPost.addPart("comment", comment);
        httpPost.setEntity(multipartEntityForPost);
        HttpResponse response = client.execute(httpPost);
        HttpEntity entity = response.getEntity();
        log("statusLine>>>" + response.getStatusLine());
        if (entity != null) {
            entity.consumeContent();
        }
    }

    private byte[] getBytesForWebPageUsingHTTPClient(String urlString) throws ClientProtocolException, IOException {
        log("Retrieving url: " + urlString);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(this.remoteUser, this.remotePassword));
        HttpGet httpget = new HttpGet(urlString);
        log("about to do request: " + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        log("-------------- Request results --------------");
        log("Status line: " + response.getStatusLine());
        if (entity != null) {
            log("Response content length: " + entity.getContentLength());
        }
        log("contents");
        byte[] bytes = null;
        if (entity != null) {
            bytes = getBytesFromInputStream(entity.getContent());
            entity.consumeContent();
        }
        log("Status code :" + response.getStatusLine().getStatusCode());
        log(response.getStatusLine().getReasonPhrase());
        if (response.getStatusLine().getStatusCode() != 200) return null;
        return bytes;
    }

    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int b = inputStream.read();
        while (b != -1) {
            outputStream.write(b);
            b = inputStream.read();
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    public static void main(String[] args) {
        SimpleHTTPArchiveSynchronizer synchronizer = new SimpleHTTPArchiveSynchronizer("trunk/Pointrel20090201/TestArchive1/", "http://www.oscomak.net/temporary_pointrel_testing/pointrel.php", "test", null, null);
        synchronizer.synchronize();
    }
}
