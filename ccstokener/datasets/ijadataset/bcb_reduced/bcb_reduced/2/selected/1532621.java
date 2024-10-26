package org.pointrel.pointrel20110330.archives;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

public class ArchiveUsingCouchDB extends ArchiveAbstract {

    public static String archiveType = "couchdb";

    public String getCurrentStateToken() {
        byte[] bytes;
        try {
            bytes = this.getBytesForWebPageUsingHTTPClient(this.archiveAccessSpecification.getURL());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (bytes == null) return null;
        String[] segments = new String(bytes).split(",");
        for (String segment : segments) {
            String[] nameValue = segment.split(":");
            if (nameValue.length != 2) continue;
            if (nameValue[0].indexOf("update_seq") != -1) {
                String result = nameValue[1];
                if (result.indexOf('}') != -1) result = result.substring(0, result.length() - 1);
                return result;
            }
        }
        return null;
    }

    public boolean hasResource(String resourceReference) {
        try {
            int statusCode = this.getHeadForWebPageUsingHTTPClient(this.archiveAccessSpecification.getURL() + "/" + resourceReference);
            return (statusCode == 200);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public ChangesList basicGetChangesList(String suffix, String fromToken, String toToken, ProgressCallback progressCallback) {
        ChangesList resultList = new ChangesList(suffix, fromToken, toToken);
        try {
            String since = "";
            if (fromToken != null) since = "?since=" + fromToken;
            byte[] bytes = this.getBytesForWebPageUsingHTTPClient(this.archiveAccessSpecification.getURL() + "/_changes" + since);
            if (bytes == null) {
                System.out.println("No bytes");
                return resultList;
            }
            String fileListResult = new String(bytes);
            if (fileListResult.equals("")) {
                System.out.println("Empty bytes");
                return resultList;
            }
            log("splitting");
            String[] remoteFileLines = fileListResult.split("\n");
            log("got remote file list OK");
            for (String line : remoteFileLines) {
                String[] sections = line.split(",");
                if (sections.length < 3) continue;
                String nameSection = sections[1];
                int startOfName = nameSection.indexOf(ResourceFileSupport.ResourceFilePrefix);
                if (startOfName == -1) continue;
                String restOfNameString = nameSection.substring(startOfName);
                int endOfName = restOfNameString.indexOf("\"");
                if (endOfName == -1) continue;
                String name = restOfNameString.substring(0, endOfName);
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
            return resultList;
        } catch (IOException e) {
            log(e.getMessage());
            return resultList;
        }
        return resultList;
    }

    @Override
    public boolean basicRetrieveResourceToStream(String resourceReference, OutputStream outputStream) {
        log("trying to read remote file:" + resourceReference);
        byte[] transactionBytes = null;
        try {
            transactionBytes = getBytesForWebPageUsingHTTPClient(this.archiveAccessSpecification.getURL() + "/" + resourceReference);
        } catch (ClientProtocolException e) {
            log(e.getMessage());
            return false;
        } catch (IOException e) {
            log(e.getMessage());
            return false;
        }
        if (transactionBytes == null) {
            log("No file");
            return false;
        }
        log("File size: " + transactionBytes.length);
        ObjectMapper m = new ObjectMapper();
        try {
            ObjectNode node = (ObjectNode) m.readValue(transactionBytes, 0, transactionBytes.length, JsonNode.class);
            byte[] binaryContent = node.get("content").getBinaryValue();
            outputStream.write(binaryContent);
        } catch (JsonParseException e1) {
            e1.printStackTrace();
            return false;
        } catch (JsonMappingException e1) {
            e1.printStackTrace();
            return false;
        } catch (IOException e1) {
            e1.printStackTrace();
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
        ObjectMapper m = new ObjectMapper();
        ObjectNode node = m.createObjectNode();
        node.put("_id", fileName);
        node.put("content", IOUtils.toByteArray(inputStream));
        byte[] bytes = m.writeValueAsBytes(node);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(new ByteArrayEntity(bytes));
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
        if (response.getStatusLine().getStatusCode() != 201) return false;
        return true;
    }

    protected int getHeadForWebPageUsingHTTPClient(String urlString) throws ClientProtocolException, IOException {
        log("Retrieving url HEAD: " + urlString);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        if (this.archiveAccessSpecification.getUserID() != null) {
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), new UsernamePasswordCredentials(this.archiveAccessSpecification.getUserID(), this.archiveAccessSpecification.getUserPassword()));
        }
        HttpHead httpget = new HttpHead(urlString);
        log("about to do request: " + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        log("-------------- Request results --------------");
        log("Status line: " + response.getStatusLine());
        if (entity != null) {
            log("Response content length: " + entity.getContentLength());
        }
        if (entity != null) {
            EntityUtils.consume(entity);
        }
        int statusCode = response.getStatusLine().getStatusCode();
        log("Status code: " + statusCode);
        log(response.getStatusLine().getReasonPhrase());
        return statusCode;
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
        log("Status code: " + response.getStatusLine().getStatusCode());
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
