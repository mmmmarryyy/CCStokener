package org.pointrel.pointrel20110330.archives;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

class InputStreamBodyWithKnownSize extends InputStreamBody {

    private long length;

    public InputStreamBodyWithKnownSize(final InputStream inputStream, final String filename, final long length) {
        super(inputStream, filename);
        this.length = length;
    }

    public long getContentLength() {
        return this.length;
    }
}

public class ArchiveUsingCGI extends ArchiveAbstract {

    public static String archiveType = "cgi";

    @Override
    public ChangesList basicGetChangesList(String suffix, String fromToken, String toToken, ProgressCallback progressCallback) {
        ChangesList resultList = new ChangesList(suffix, fromToken, toToken);
        try {
            byte[] bytes = this.getBytesForWebPageUsingHTTPClient(this.archiveAccessSpecification.getURL());
            if (bytes == null) return null;
            String fileListResult = new String(bytes);
            if (fileListResult.equals("")) {
                return resultList;
            }
            log("splitting");
            String[] remoteFileNames = fileListResult.split("\n");
            log("got remote file list OK");
            for (String name : remoteFileNames) {
                if (suffix == null) {
                    if (ResourceFileSupport.isValidResourceFileName(name)) {
                        resultList.add(name);
                    }
                } else {
                    if (ResourceFileSupport.isValidResourceFileNameWithSuffix(name, suffix)) {
                        resultList.add(name);
                    }
                }
            }
        } catch (ClientProtocolException e) {
            log(e.getMessage());
            return null;
        } catch (IOException e) {
            log(e.getMessage());
            return null;
        }
        return resultList;
    }

    @Override
    public boolean basicRetrieveResourceToStream(String resourceReference, OutputStream outputStream) {
        log("trying to read remote file:" + resourceReference);
        byte[] transactionBytes = null;
        try {
            transactionBytes = getBytesForWebPageUsingHTTPClient(this.archiveAccessSpecification.getURL() + "?fileName=" + resourceReference);
        } catch (ClientProtocolException e) {
            log(e.getMessage());
            return false;
        } catch (IOException e) {
            log(e.getMessage());
            return false;
        }
        log("File size: " + transactionBytes.length);
        try {
            outputStream.write(transactionBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    String basicAddResourceFromStream(InputStream inputStream, String extension, String prefix, String requestedResourceReference) {
        if (prefix != null) {
            if (!this.isSupportedPrefix(prefix)) {
                System.out.println("Unsupported resource prefix: " + prefix);
                return null;
            }
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ResourceFileSupport.copyInputStreamToOutputStream(inputStream, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            String resourceReference = Standards.getResourceReferenceWithSHA256HashAsHexEncodedString(bytes, extension);
            if ((requestedResourceReference != null) && (!resourceReference.equals(requestedResourceReference))) {
                System.out.println("Requested resource reference does not match generated one: " + requestedResourceReference + " != " + resourceReference);
                return null;
            }
            log("Trying to store file to server " + resourceReference);
            boolean stored = this.uploadFile(this.archiveAccessSpecification.getURL(), resourceReference, new ByteArrayInputStream(bytes), bytes.length);
            if (!stored) return null;
            log("Done storing ");
            return resourceReference;
        } catch (ClientProtocolException e) {
            log(e.getMessage());
            return null;
        } catch (IOException e) {
            log(e.getMessage());
            return null;
        }
    }

    public boolean uploadFile(String url, String fileName, InputStream inputStream, long inputLength) throws ClientProtocolException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        if (this.archiveAccessSpecification.getUserID() != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(this.archiveAccessSpecification.getUserID(), this.archiveAccessSpecification.getUserPassword()));
        }
        HttpPost httpPost = new HttpPost(url);
        log("File1 name = " + fileName);
        InputStreamBodyWithKnownSize bin = new InputStreamBodyWithKnownSize(inputStream, fileName, inputLength);
        StringBody comment = new StringBody("A Pointrel transaction file");
        MultipartEntity multipartEntityForPost = new MultipartEntity();
        multipartEntityForPost.addPart("uploaded", bin);
        multipartEntityForPost.addPart("comment", comment);
        log("about to set entity");
        httpPost.setEntity(multipartEntityForPost);
        log("about to do post");
        HttpResponse response = null;
        try {
            response = client.execute(httpPost);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log("got response to post");
        HttpEntity entity = response.getEntity();
        log("statusLine>>>" + response.getStatusLine());
        if (entity != null) {
            EntityUtils.consume(entity);
        }
        if (response.getStatusLine().getStatusCode() != 200) return false;
        return true;
    }

    protected byte[] getBytesForWebPageUsingHTTPClient(String urlString) throws ClientProtocolException, IOException {
        log("Retrieving url: " + urlString);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        if (this.archiveAccessSpecification.getUserID() != null) {
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(this.archiveAccessSpecification.getUserID(), this.archiveAccessSpecification.getUserPassword()));
        }
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
            EntityUtils.consume(entity);
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
}
