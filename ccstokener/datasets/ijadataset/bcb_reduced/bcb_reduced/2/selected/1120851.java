package com.bonkey.filesystem.S3.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import com.bonkey.config.BonkeyConstants;
import com.bonkey.config.ConfigManager;
import com.bonkey.filesystem.S3.S3FileSystem;

/**
 * An interface into the S3 system.  It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate S3 data.
 */
@SuppressWarnings(value = { "unchecked" })
public class AWSAuthConnection {

    public static String LOCATION_DEFAULT = null;

    public static String LOCATION_EU = "EU";

    private S3FileSystem target;

    private boolean isSecure;

    private String server;

    private int port;

    private CallingFormat callingFormat;

    private Map initHeaders;

    private static final String XAMZ_SECURITY_TOKEN = "x-amz-security-token";

    public AWSAuthConnection(S3FileSystem target) {
        this(target, new ArrayList());
    }

    public AWSAuthConnection(S3FileSystem target, List tokens) {
        this(target, tokens, true);
    }

    public AWSAuthConnection(S3FileSystem target, List tokens, boolean isSecure) {
        this(target, tokens, isSecure, Utils.DEFAULT_HOST);
    }

    public AWSAuthConnection(S3FileSystem target, List tokens, boolean isSecure, String server) {
        this(target, tokens, isSecure, server, isSecure ? Utils.SECURE_PORT : Utils.INSECURE_PORT);
    }

    public AWSAuthConnection(S3FileSystem target, List tokens, boolean isSecure, String server, int port) {
        this(target, tokens, isSecure, server, port, CallingFormat.getPathCallingFormat());
    }

    public AWSAuthConnection(S3FileSystem target, List tokens, boolean isSecure, String server, CallingFormat format) {
        this(target, tokens, isSecure, server, isSecure ? Utils.SECURE_PORT : Utils.INSECURE_PORT, format);
    }

    /**
     * Create a new interface to interact with S3 with the given credential and connection
     * parameters
     *
     * @param target the S3filesystem to access (incl auth details)
     * @param tokens AWS Security Tokens.
     * @param isSecure use SSL encryption
     * @param server Which host to connect to.  Usually, this will be s3.amazonaws.com
     * @param port Which port to use.
     */
    public AWSAuthConnection(S3FileSystem target, List tokens, boolean isSecure, String server, int port, CallingFormat format) {
        this.target = target;
        this.isSecure = isSecure;
        this.server = server;
        this.port = port;
        this.callingFormat = format;
        this.initHeaders = new HashMap();
        if (tokens != null && tokens.size() > 0) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < tokens.size(); ++i) {
                if (buffer.length() > 0) buffer.append(",");
                buffer.append(tokens.get(i));
            }
            List tokensHeaderValue = new ArrayList();
            tokensHeaderValue.add(buffer.toString());
            initHeaders.put(XAMZ_SECURITY_TOKEN, tokensHeaderValue);
        }
    }

    /**
     * Creates a new bucket.
     * @param bucket The name of the bucket to create.
     * @param location Desired location ("EU") (or null for default).
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     * @throws IllegalArgumentException on invalid location
     */
    public Response createBucket(String bucket, String location, Map headers) throws MalformedURLException, IOException {
        String body;
        if (location == null) {
            body = null;
        } else if (LOCATION_EU.equals(location)) {
            if (!callingFormat.supportsLocatedBuckets()) throw new IllegalArgumentException(Messages.getString("AWSAuthConnection.ErrorLocationConstraint"));
            body = "<CreateBucketConstraint><LocationConstraint>" + location + "</LocationConstraint></CreateBucketConstraint>";
        } else throw new IllegalArgumentException(Messages.getString("AWSAuthConnection.ErrorInvalidLocation") + location);
        if (!Utils.validateBucketName(bucket, callingFormat, location != null)) throw new IllegalArgumentException(Messages.getString("AWSAuthConnection.ErrorInvalidBucketName") + bucket);
        HttpURLConnection request = makeRequest("PUT", bucket, "", null, headers);
        if (body != null) {
            request.setDoOutput(true);
            request.getOutputStream().write(body.getBytes("UTF-8"));
        }
        return new Response(request);
    }

    /**
     * Check if the specified bucket exists (via a HEAD request)
     * @param bucket The name of the bucket to check
     * @return true if HEAD access returned success
     */
    public boolean checkBucketExists(String bucket) throws MalformedURLException, IOException {
        HttpURLConnection response = makeRequest("HEAD", bucket, "", null, null);
        int httpCode = response.getResponseCode();
        return httpCode >= 200 && httpCode < 300;
    }

    /**
     * Lists the contents of a bucket.
     * @param bucket The name of the bucket to create.
     * @param prefix All returned keys will start with this string (can be null).
     * @param marker All returned keys will be lexographically greater than
     * this string (can be null).
     * @param maxKeys The maximum number of keys to return (can be null).
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public ListBucketResponse listBucket(String bucket, String prefix, String marker, Integer maxKeys, Map headers) throws MalformedURLException, IOException {
        return listBucket(bucket, prefix, marker, maxKeys, null, headers);
    }

    /**
     * Lists the contents of a bucket.
     * @param bucket The name of the bucket to list.
     * @param prefix All returned keys will start with this string (can be null).
     * @param marker All returned keys will be lexographically greater than
     * this string (can be null).
     * @param maxKeys The maximum number of keys to return (can be null).
     * @param delimiter Keys that contain a string between the prefix and the first 
     * occurrence of the delimiter will be rolled up into a single element.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public ListBucketResponse listBucket(String bucket, String prefix, String marker, Integer maxKeys, String delimiter, Map headers) throws MalformedURLException, IOException {
        Map pathArgs = Utils.paramsForListOptions(prefix, marker, maxKeys, delimiter);
        return new ListBucketResponse(makeRequest("GET", bucket, "", pathArgs, headers));
    }

    /**
     * Deletes a bucket.
     * @param bucket The name of the bucket to delete.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response deleteBucket(String bucket, Map headers) throws MalformedURLException, IOException {
        return new Response(makeRequest("DELETE", bucket, "", null, headers));
    }

    /**
     * Writes an object to S3.
     * @param bucket The name of the bucket to which the object will be added.
     * @param key The name of the key to use.
     * @param object An S3Object containing the data to write.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response put(String bucket, String key, S3Object object, Map headers) throws MalformedURLException, IOException {
        HttpURLConnection request = makeRequest("PUT", bucket, Utils.urlencode(key), null, headers, object);
        request.setDoOutput(true);
        object.put(request.getOutputStream());
        return new Response(request);
    }

    /**
     * Reads an object from S3.
     * @param bucket The name of the bucket where the object lives.
     * @param key The name of the key to use.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public GetResponse get(String bucket, String key, Map headers) throws MalformedURLException, IOException {
        return new GetResponse(makeRequest("GET", bucket, Utils.urlencode(key), null, headers));
    }

    /**
     * Deletes an object from S3.
     * @param bucket The name of the bucket where the object lives.
     * @param key The name of the key to use.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response delete(String bucket, String key, Map headers) throws MalformedURLException, IOException {
        return new Response(makeRequest("DELETE", bucket, Utils.urlencode(key), null, headers));
    }

    /**
     * Get the logging xml document for a given bucket
     * @param bucket The name of the bucket
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public GetResponse getBucketLogging(String bucket, Map headers) throws MalformedURLException, IOException {
        Map pathArgs = new HashMap();
        pathArgs.put("logging", null);
        return new GetResponse(makeRequest("GET", bucket, "", pathArgs, headers));
    }

    /**
     * Write a new logging xml document for a given bucket
     * @param loggingXMLDoc The xml representation of the logging configuration as a String
     * @param bucket The name of the bucket
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response putBucketLogging(String bucket, String loggingXMLDoc, Map headers) throws MalformedURLException, IOException {
        Map pathArgs = new HashMap();
        pathArgs.put("logging", null);
        S3Object object = new S3ByteObject(loggingXMLDoc.getBytes(), null);
        HttpURLConnection request = makeRequest("PUT", bucket, "", pathArgs, headers, object);
        request.setDoOutput(true);
        object.put(request.getOutputStream());
        return new Response(request);
    }

    /**
     * Get the ACL for a given bucket
     * @param bucket The name of the bucket where the object lives.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public GetResponse getBucketACL(String bucket, Map headers) throws MalformedURLException, IOException {
        return getACL(bucket, "", headers);
    }

    /**
     * Get the ACL for a given object (or bucket, if key is null).
     * @param bucket The name of the bucket where the object lives.
     * @param key The name of the key to use.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public GetResponse getACL(String bucket, String key, Map headers) throws MalformedURLException, IOException {
        if (key == null) key = "";
        Map pathArgs = new HashMap();
        pathArgs.put("acl", null);
        return new GetResponse(makeRequest("GET", bucket, Utils.urlencode(key), pathArgs, headers));
    }

    /**
     * Write a new ACL for a given bucket
     * @param aclXMLDoc The xml representation of the ACL as a String
     * @param bucket The name of the bucket where the object lives.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response putBucketACL(String bucket, String aclXMLDoc, Map headers) throws MalformedURLException, IOException {
        return putACL(bucket, "", aclXMLDoc, headers);
    }

    /**
     * Write a new ACL for a given object
     * @param aclXMLDoc The xml representation of the ACL as a String
     * @param bucket The name of the bucket where the object lives.
     * @param key The name of the key to use.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response putACL(String bucket, String key, String aclXMLDoc, Map headers) throws MalformedURLException, IOException {
        S3Object object = new S3ByteObject(aclXMLDoc.getBytes(), null);
        Map pathArgs = new HashMap();
        pathArgs.put("acl", null);
        HttpURLConnection request = makeRequest("PUT", bucket, Utils.urlencode(key), pathArgs, headers, object);
        request.setDoOutput(true);
        object.put(request.getOutputStream());
        return new Response(request);
    }

    public LocationResponse getBucketLocation(String bucket) throws MalformedURLException, IOException {
        Map pathArgs = new HashMap();
        pathArgs.put("location", null);
        return new LocationResponse(makeRequest("GET", bucket, "", pathArgs, null));
    }

    /**
     * List all the buckets created by this account.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public ListAllMyBucketsResponse listAllMyBuckets(Map headers) throws MalformedURLException, IOException {
        return new ListAllMyBucketsResponse(makeRequest("GET", "", "", null, headers));
    }

    /**
     * Make a new HttpURLConnection without passing an S3Object parameter. 
     * Use this method for key operations that do require arguments
     * @param method The method to invoke
     * @param bucketName the bucket this request is for
     * @param key the key this request is for
     * @param pathArgs the 
     * @param headers
     * @return the connection to S3
     * @throws MalformedURLException
     * @throws IOException
     */
    private HttpURLConnection makeRequest(String method, String bucketName, String key, Map pathArgs, Map headers) throws MalformedURLException, IOException {
        return makeRequest(method, bucketName, key, pathArgs, headers, null);
    }

    /**
     * Make a new HttpURLConnection.
     * @param method The HTTP method to use (GET, PUT, DELETE)
     * @param bucketName The bucket name this request affects
     * @param key The key this request is for
     * @param pathArgs parameters if any to be sent along this request
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     * @param object The S3Object that is to be written (can be null).
     */
    private HttpURLConnection makeRequest(String method, String bucket, String key, Map pathArgs, Map headers, S3Object object) throws MalformedURLException, IOException {
        URL url = this.callingFormat.getURL(this.isSecure, server, this.port, bucket, key, pathArgs);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (object != null && object.getSize() > 0) {
            int size = (int) object.getSize();
            if (target.isEncrypted()) {
                size += 16 - (size % 16);
            }
            connection.setFixedLengthStreamingMode(size);
        }
        int timeout = ConfigManager.getConfigManager().getPropertyInt(BonkeyConstants.S3_TIMEOUT);
        connection.setConnectTimeout(timeout * 1000);
        connection.setRequestMethod(method);
        if (!connection.getInstanceFollowRedirects() && callingFormat.supportsLocatedBuckets()) throw new RuntimeException(Messages.getString("AWSAuthConnection.ErrorHTTPRedirect"));
        addHeaders(connection, initHeaders);
        addHeaders(connection, headers);
        if (object != null) addMetadataHeaders(connection, object.metadata);
        addAuthHeader(connection, method, bucket, key, pathArgs);
        return connection;
    }

    /**
     * Add the given headers to the HttpURLConnection.
     * @param connection The HttpURLConnection to which the headers will be added.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    private void addHeaders(HttpURLConnection connection, Map headers) {
        addHeaders(connection, headers, "");
    }

    /**
     * Add the given metadata fields to the HttpURLConnection.
     * @param connection The HttpURLConnection to which the headers will be added.
     * @param metadata A Map of String to List of Strings representing the s3
     * metadata for this resource.
     */
    private void addMetadataHeaders(HttpURLConnection connection, Map metadata) {
        addHeaders(connection, metadata, Utils.METADATA_PREFIX);
    }

    /**
     * Add the given headers to the HttpURLConnection with a prefix before the keys.
     * @param connection The HttpURLConnection to which the headers will be added.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     * @param prefix The string to prepend to each key before adding it to the connection.
     */
    private void addHeaders(HttpURLConnection connection, Map headers, String prefix) {
        if (headers != null) {
            for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                for (Iterator j = ((List) headers.get(key)).iterator(); j.hasNext(); ) {
                    String value = (String) j.next();
                    connection.addRequestProperty(prefix + key, value);
                }
            }
        }
    }

    /**
     * Add the appropriate Authorization header to the HttpURLConnection.
     * @param connection The HttpURLConnection to which the header will be added.
     * @param method The HTTP method to use (GET, PUT, DELETE)
     * @param bucket the bucket name this request is for
     * @param key the key this request is for
     * @param pathArgs path arguments which are part of this request
     */
    private void addAuthHeader(HttpURLConnection connection, String method, String bucket, String key, Map pathArgs) throws IOException {
        if (connection.getRequestProperty("Date") == null) {
            connection.setRequestProperty("Date", httpDate());
        }
        if (connection.getRequestProperty("Content-Type") == null) {
            connection.setRequestProperty("Content-Type", "");
        }
        String secretKey = target.getSecretKey();
        String accessKey = target.getAccessKey();
        String canonicalString = Utils.makeCanonicalString(method, bucket, key, pathArgs, connection.getRequestProperties());
        String encodedCanonical = Utils.encode(secretKey, canonicalString, false);
        connection.setRequestProperty("Authorization", "AWS " + accessKey + ":" + encodedCanonical);
    }

    /**
     * Generate an rfc822 date for use in the Date HTTP header.
     */
    public static String httpDate() {
        final String DateFormat = "EEE, dd MMM yyyy HH:mm:ss ";
        SimpleDateFormat format = new SimpleDateFormat(DateFormat, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date()) + "GMT";
    }

    public int getPort() {
        return this.port;
    }
}
