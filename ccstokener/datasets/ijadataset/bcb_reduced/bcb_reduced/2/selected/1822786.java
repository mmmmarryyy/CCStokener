package com.amazon.s3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides an interface into the Amazon Simple Storage System (S3) by providing
 * services to access and manipulate data via REST over an authenticated
 * connection.
 *
 * This code was originally provided by Amazon Web Services at:
 * http://developer.amazonwebservices.com/connect/entry.jspa?categoryID=47&externalID=132
 *
 * @author Eric Wagner - EricW@AWS
 * @author Bill Branan
 */
public class AWSAuthConnection {

    public static String LOCATION_DEFAULT = null;

    public static String LOCATION_EU = "EU";

    private String awsAccessKeyId;

    private String awsSecretAccessKey;

    private boolean isSecure;

    private String server;

    private int port;

    private CallingFormat callingFormat;

    public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey) {
        this(awsAccessKeyId, awsSecretAccessKey, true);
    }

    public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure) {
        this(awsAccessKeyId, awsSecretAccessKey, isSecure, Utils.DEFAULT_HOST);
    }

    public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure, String server) {
        this(awsAccessKeyId, awsSecretAccessKey, isSecure, server, isSecure ? Utils.SECURE_PORT : Utils.INSECURE_PORT);
    }

    public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure, String server, int port) {
        this(awsAccessKeyId, awsSecretAccessKey, isSecure, server, port, CallingFormat.getSubdomainCallingFormat());
    }

    public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure, String server, CallingFormat format) {
        this(awsAccessKeyId, awsSecretAccessKey, isSecure, server, isSecure ? Utils.SECURE_PORT : Utils.INSECURE_PORT, CallingFormat.getSubdomainCallingFormat());
    }

    /**
     * Create a new interface to interact with S3 with the given credential and
     * connection parameters
     *
     * @param awsAccessKeyId
     *        Your user key into AWS
     * @param awsSecretAccessKey
     *        The secret string used to generate signatures for authentication.
     * @param isSecure
     *        use SSL encryption
     * @param server
     *        Which host to connect to. Usually, this will be s3.amazonaws.com
     * @param port
     *        Which port to use.
     * @param callingFormat
     *        Type of request Regular/Vanity or Pure Vanity domain
     */
    public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure, String server, int port, CallingFormat format) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.isSecure = isSecure;
        this.server = server;
        this.port = port;
        this.callingFormat = format;
    }

    /**
     * Creates a new bucket.
     *
     * @param bucket
     *        The name of the bucket to create.
     * @param location
     *        Desired location ("EU") (or null for default).
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     * @param metadata
     *        A Map of String to List of Strings representing the s3 metadata
     *        for this bucket (can be null).
     * @throws IllegalArgumentException
     *         on invalid location
     */
    public Response createBucket(String bucket, String location, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        String body;
        if (location == null) {
            body = null;
        } else if (LOCATION_EU.equals(location)) {
            if (!callingFormat.supportsLocatedBuckets()) throw new IllegalArgumentException("Creating location-constrained bucket with unsupported calling-format");
            body = "<CreateBucketConstraint><LocationConstraint>" + location + "</LocationConstraint></CreateBucketConstraint>";
        } else {
            throw new IllegalArgumentException("Invalid Location: " + location);
        }
        if (!Utils.validateBucketName(bucket, callingFormat, location != null)) {
            throw new IllegalArgumentException("Invalid Bucket Name: " + bucket);
        }
        HttpURLConnection request = makeRequest("PUT", bucket, "", null, headers);
        if (body != null) {
            request.setDoOutput(true);
            request.getOutputStream().write(body.getBytes("UTF-8"));
        }
        return new Response(request);
    }

    /**
     * Check if the specified bucket exists (via a HEAD request)
     *
     * @param bucket
     *        The name of the bucket to check
     * @return true if HEAD access returned success
     */
    public boolean checkBucketExists(String bucket) throws MalformedURLException, IOException {
        HttpURLConnection response = makeRequest("HEAD", bucket, "", null, null);
        int httpCode = response.getResponseCode();
        return httpCode >= 200 && httpCode < 300;
    }

    /**
     * Lists the contents of a bucket.
     *
     * @param bucket
     *        The name of the bucket to create.
     * @param prefix
     *        All returned keys will start with this string (can be null).
     * @param marker
     *        All returned keys will be lexographically greater than this string
     *        (can be null).
     * @param maxKeys
     *        The maximum number of keys to return (can be null).
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public ListBucketResponse listBucket(String bucket, String prefix, String marker, Integer maxKeys, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return listBucket(bucket, prefix, marker, maxKeys, null, headers);
    }

    /**
     * Lists the contents of a bucket.
     *
     * @param bucket
     *        The name of the bucket to list.
     * @param prefix
     *        All returned keys will start with this string (can be null).
     * @param marker
     *        All returned keys will be lexographically greater than this string
     *        (can be null).
     * @param maxKeys
     *        The maximum number of keys to return (can be null).
     * @param delimiter
     *        Keys that contain a string between the prefix and the first
     *        occurrence of the delimiter will be rolled up into a single
     *        element.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public ListBucketResponse listBucket(String bucket, String prefix, String marker, Integer maxKeys, String delimiter, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        Map<String, String> pathArgs = Utils.paramsForListOptions(prefix, marker, maxKeys, delimiter);
        return new ListBucketResponse(makeRequest("GET", bucket, "", pathArgs, headers));
    }

    /**
     * Deletes a bucket.
     *
     * @param bucket
     *        The name of the bucket to delete.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public Response deleteBucket(String bucket, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return new Response(makeRequest("DELETE", bucket, "", null, headers));
    }

    /**
     * Writes an object to S3.
     *
     * @param bucket
     *        The name of the bucket to which the object will be added.
     * @param key
     *        The name of the key to use.
     * @param object
     *        An S3Object containing the data to write.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public Response put(String bucket, String key, Map<String, List<String>> metadata, byte[] content, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        HttpURLConnection request = makeRequest("PUT", bucket, Utils.urlencode(key), null, headers, metadata);
        request.setDoOutput(true);
        request.getOutputStream().write(content == null ? new byte[] {} : content);
        return new Response(request);
    }

    /**
     * Writes a content stream to S3.
     *
     * @param bucket
     *        The name of the bucket to which the object will be added.
     * @param key
     *        The name of the key to use.
     * @param content
     *        The stream of content to write to the object
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public void putContent(String bucket, String key, InputStream content, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        HttpURLConnection request = makeRequest("PUT", bucket, Utils.urlencode(key), null, headers);
        BufferedInputStream in = new BufferedInputStream(content);
        request.setDoOutput(true);
        BufferedOutputStream out = new BufferedOutputStream(request.getOutputStream());
        int b = -1;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        out.flush();
        int responseCode = request.getResponseCode();
        if (responseCode >= 400) {
            throw new IOException("PUT request for content with key " + key + " was unsuccessful. " + "Response was: " + responseCode + " " + request.getResponseMessage());
        }
    }

    /**
     * Reads an object from S3.
     *
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param key
     *        The name of the key to use.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public GetResponse get(String bucket, String key, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return new GetResponse(makeRequest("GET", bucket, Utils.urlencode(key), null, headers));
    }

    /**
     * Gets content from S3.
     *
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param key
     *        The name of the key to use.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public InputStream getContent(String bucket, String key, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        HttpURLConnection request = makeRequest("GET", bucket, Utils.urlencode(key), null, headers);
        int responseCode = request.getResponseCode();
        if (responseCode < 400) {
            return request.getInputStream();
        } else {
            throw new IOException("GET request for content with key " + key + " was unsuccessful. " + "Response was: " + responseCode + " " + request.getResponseMessage());
        }
    }

    /**
     * Deletes an object from S3.
     *
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param key
     *        The name of the key to use.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public Response delete(String bucket, String key, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return new Response(makeRequest("DELETE", bucket, Utils.urlencode(key), null, headers));
    }

    /**
     * Deletes content from S3.
     *
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param key
     *        The name of the key to use.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public void deleteContent(String bucket, String key, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        HttpURLConnection request = makeRequest("DELETE", bucket, Utils.urlencode(key), null, headers);
        int responseCode = request.getResponseCode();
        if (responseCode >= 400) {
            throw new IOException("DELETE request for content with key " + key + " was unsuccessful. " + "Response was: " + responseCode + " " + request.getResponseMessage());
        }
    }

    /**
     * Get the logging xml document for a given bucket
     *
     * @param bucket
     *        The name of the bucket
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public GetResponse getBucketLogging(String bucket, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        Map<String, String> pathArgs = new HashMap<String, String>();
        pathArgs.put("logging", null);
        return new GetResponse(makeRequest("GET", bucket, "", pathArgs, headers));
    }

    /**
     * Write a new logging xml document for a given bucket
     *
     * @param loggingXMLDoc
     *        The xml representation of the logging configuration as a String
     * @param bucket
     *        The name of the bucket
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public Response putBucketLogging(String bucket, String loggingXMLDoc, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        byte[] loggingXMLBytes = new byte[0];
        if (loggingXMLBytes != null && !loggingXMLBytes.equals("")) {
            loggingXMLBytes = loggingXMLDoc.getBytes();
        }
        Map<String, String> pathArgs = new HashMap<String, String>();
        pathArgs.put("logging", null);
        HttpURLConnection request = makeRequest("PUT", bucket, "", pathArgs, headers);
        request.setDoOutput(true);
        request.getOutputStream().write(loggingXMLBytes);
        return new Response(request);
    }

    /**
     * Get the ACL for a given bucket
     *
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public GetResponse getBucketACL(String bucket, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return getACL(bucket, "", headers);
    }

    /**
     * Get the ACL for a given object (or bucket, if key is null).
     *
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param key
     *        The name of the key to use.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public GetResponse getACL(String bucket, String key, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        if (key == null) key = "";
        Map<String, String> pathArgs = new HashMap<String, String>();
        pathArgs.put("acl", null);
        return new GetResponse(makeRequest("GET", bucket, Utils.urlencode(key), pathArgs, headers));
    }

    /**
     * Write a new ACL for a given bucket
     *
     * @param aclXMLDoc
     *        The xml representation of the ACL as a String
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public Response putBucketACL(String bucket, String aclXMLDoc, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return putACL(bucket, "", aclXMLDoc, headers);
    }

    /**
     * Write a new ACL for a given object
     *
     * @param aclXMLDoc
     *        The xml representation of the ACL as a String
     * @param bucket
     *        The name of the bucket where the object lives.
     * @param key
     *        The name of the key to use.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public Response putACL(String bucket, String key, String aclXMLDoc, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        byte[] aclXMLBytes = new byte[0];
        if (aclXMLDoc != null && !aclXMLDoc.equals("")) {
            aclXMLBytes = aclXMLDoc.getBytes();
        }
        Map<String, String> pathArgs = new HashMap<String, String>();
        pathArgs.put("acl", null);
        HttpURLConnection request = makeRequest("PUT", bucket, Utils.urlencode(key), pathArgs, headers);
        request.setDoOutput(true);
        request.getOutputStream().write(aclXMLBytes);
        return new Response(request);
    }

    public LocationResponse getBucketLocation(String bucket) throws MalformedURLException, IOException {
        Map<String, String> pathArgs = new HashMap<String, String>();
        pathArgs.put("location", null);
        return new LocationResponse(makeRequest("GET", bucket, "", pathArgs, null));
    }

    /**
     * List all the buckets created by this account.
     *
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    public ListAllMyBucketsResponse listAllMyBuckets(Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return new ListAllMyBucketsResponse(makeRequest("GET", "", "", null, headers));
    }

    /**
     * Make a new HttpURLConnection without passing an S3Object parameter. Use
     * this method for key operations that do require arguments
     *
     * @param method
     *        The method to invoke
     * @param bucketName
     *        the bucket this request is for
     * @param key
     *        the key this request is for
     * @param pathArgs
     *        the
     * @param headers
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private HttpURLConnection makeRequest(String method, String bucketName, String key, Map<String, String> pathArgs, Map<String, List<String>> headers) throws MalformedURLException, IOException {
        return makeRequest(method, bucketName, key, pathArgs, headers, null);
    }

    /**
     * Make a new HttpURLConnection.
     *
     * @param method
     *        The HTTP method to use (GET, PUT, DELETE)
     * @param bucketName
     *        The bucket name this request affects
     * @param key
     *        The key this request is for
     * @param pathArgs
     *        parameters if any to be sent along this request
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     * @param object
     *        The S3Object that is to be written (can be null).
     */
    private HttpURLConnection makeRequest(String method, String bucket, String key, Map<String, String> pathArgs, Map<String, List<String>> headers, Map<String, List<String>> metadata) throws MalformedURLException, IOException {
        URL url = this.callingFormat.getURL(this.isSecure, server, this.port, bucket, key, pathArgs);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        if (!connection.getInstanceFollowRedirects() && callingFormat.supportsLocatedBuckets()) throw new RuntimeException("HTTP redirect support required.");
        addHeaders(connection, headers);
        if (metadata != null) {
            addMetadataHeaders(connection, metadata);
        }
        addAuthHeader(connection, method, bucket, key, pathArgs);
        return connection;
    }

    /**
     * Add the given headers to the HttpURLConnection.
     *
     * @param connection
     *        The HttpURLConnection to which the headers will be added.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     */
    private void addHeaders(HttpURLConnection connection, Map<String, List<String>> headers) {
        addHeaders(connection, headers, "");
    }

    /**
     * Add the given metadata fields to the HttpURLConnection.
     *
     * @param connection
     *        The HttpURLConnection to which the headers will be added.
     * @param metadata
     *        A Map of String to List of Strings representing the s3 metadata
     *        for this resource.
     */
    private void addMetadataHeaders(HttpURLConnection connection, Map<String, List<String>> metadata) {
        addHeaders(connection, metadata, Utils.METADATA_PREFIX);
    }

    /**
     * Add the given headers to the HttpURLConnection with a prefix before the
     * keys.
     *
     * @param connection
     *        The HttpURLConnection to which the headers will be added.
     * @param headers
     *        A Map of String to List of Strings representing the http headers
     *        to pass (can be null).
     * @param prefix
     *        The string to prepend to each key before adding it to the
     *        connection.
     */
    private void addHeaders(HttpURLConnection connection, Map<String, List<String>> headers, String prefix) {
        if (headers != null) {
            for (Iterator<String> i = headers.keySet().iterator(); i.hasNext(); ) {
                String key = i.next();
                for (Iterator<String> j = ((List<String>) headers.get(key)).iterator(); j.hasNext(); ) {
                    String value = j.next();
                    connection.addRequestProperty(prefix + key, value);
                }
            }
        }
    }

    /**
     * Add the appropriate Authorization header to the HttpURLConnection.
     *
     * @param connection
     *        The HttpURLConnection to which the header will be added.
     * @param method
     *        The HTTP method to use (GET, PUT, DELETE)
     * @param bucket
     *        the bucket name this request is for
     * @param key
     *        the key this request is for
     * @param pathArgs
     *        path arguments which are part of this request
     */
    private void addAuthHeader(HttpURLConnection connection, String method, String bucket, String key, Map<String, String> pathArgs) {
        if (connection.getRequestProperty("Date") == null) {
            connection.setRequestProperty("Date", httpDate());
        }
        if (connection.getRequestProperty("Content-Type") == null) {
            connection.setRequestProperty("Content-Type", "");
        }
        String canonicalString = Utils.makeCanonicalString(method, bucket, key, pathArgs, connection.getRequestProperties());
        String encodedCanonical = Utils.encode(this.awsSecretAccessKey, canonicalString, false);
        connection.setRequestProperty("Authorization", "AWS " + this.awsAccessKeyId + ":" + encodedCanonical);
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
}
