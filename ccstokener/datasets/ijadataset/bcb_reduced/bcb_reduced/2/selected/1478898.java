package com.snipreel.mocks3;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3Connection {

    private static final String AMAZON_HEADER_PREFIX = "x-amz-";

    private static final String ALTERNATIVE_DATE_HEADER = "x-amz-date";

    private static final Pattern INTERESTING_HEADERS = Pattern.compile("^(?:content-type|content-md5|date)$|^x-amz-", Pattern.CASE_INSENSITIVE);

    private static final Pattern INTERESTING_QUERIES = Pattern.compile("^(acl)$");

    private final String method;

    private final String host;

    private final String bucket;

    private final String request;

    private final Map<String, List<String>> mapOfHeaders;

    public S3Connection(String method, String host, String bucket, String request, Map<String, List<String>> mapOfHeaders) {
        this.method = method;
        this.host = host;
        this.bucket = bucket;
        this.request = request;
        this.mapOfHeaders = mapOfHeaders;
    }

    public String getMethod() {
        return method;
    }

    public String getHost() {
        return host;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRequest() {
        return request;
    }

    public void addHeader(String name, String value) {
        List<String> listOfValues = mapOfHeaders.get(name);
        if (listOfValues == null) {
            listOfValues = new ArrayList<String>();
            mapOfHeaders.put(name, listOfValues);
        }
        listOfValues.add(value);
    }

    public void setHeader(String name, String value) {
        List<String> listOfValues = mapOfHeaders.get(name);
        if (listOfValues == null) {
            listOfValues = new ArrayList<String>();
            mapOfHeaders.put(name, listOfValues);
        }
        listOfValues.clear();
        listOfValues.add(value);
    }

    private static final String join(List<String> listOfValues) {
        StringBuilder builder = new StringBuilder();
        String separator = "";
        for (String s : listOfValues) {
            builder.append(separator).append(s);
            separator = ",";
        }
        return builder.toString();
    }

    public String getSignaturePlainText() {
        Map<String, List<String>> mapOfSignedHeaders = new TreeMap<String, List<String>>();
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : mapOfHeaders.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Matcher matcher = INTERESTING_HEADERS.matcher(entry.getKey());
            if (matcher.find()) {
                String key = entry.getKey().toLowerCase();
                List<String> listOfValues = mapOfSignedHeaders.get(key);
                if (listOfValues == null) {
                    listOfValues = new ArrayList<String>();
                    mapOfSignedHeaders.put(key, listOfValues);
                }
                for (String value : entry.getValue()) {
                    listOfValues.add(value);
                }
            }
        }
        if (mapOfSignedHeaders.containsKey(ALTERNATIVE_DATE_HEADER)) {
            mapOfSignedHeaders.put("date", new ArrayList<String>());
        }
        if (!mapOfSignedHeaders.containsKey("content-type")) {
            mapOfSignedHeaders.put("content-type", new ArrayList<String>());
        }
        if (!mapOfSignedHeaders.containsKey("content-md5")) {
            mapOfSignedHeaders.put("content-md5", new ArrayList<String>());
        }
        builder.append(method).append('\n');
        builder.append(join(mapOfSignedHeaders.get("content-type"))).append('\n');
        builder.append(join(mapOfSignedHeaders.get("content-md5"))).append('\n');
        builder.append(join(mapOfSignedHeaders.get("date"))).append('\n');
        for (Map.Entry<String, List<String>> entry : mapOfSignedHeaders.entrySet()) {
            if (entry.getKey().startsWith(AMAZON_HEADER_PREFIX)) {
                builder.append(entry.getKey()).append(':').append(join(entry.getValue())).append('\n');
            }
        }
        String path = request;
        if (bucket != null && !bucket.equals("")) {
            builder.append('/').append(bucket);
            if (path.startsWith('/' + bucket)) {
                path = path.substring(0, bucket.length() + 1);
            }
        }
        if (path == "") {
            path = "/";
        }
        if (request.indexOf('?') != -1) {
            String query = request.substring(request.indexOf('?') + 1);
            Matcher queries = INTERESTING_QUERIES.matcher(query);
            if (queries.find()) {
                path += '?' + queries.group(1);
            }
        }
        builder.append(path).append('\n');
        return builder.toString();
    }

    public String getAWSAuthentcation() {
        return null;
    }

    public HttpURLConnection openConnection() throws IOException {
        URL url = new URL("http", host, request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Host", host);
        for (Map.Entry<String, List<String>> entry : mapOfHeaders.entrySet()) {
            for (String value : entry.getValue()) {
                connection.addRequestProperty(entry.getKey(), value);
            }
        }
        return connection;
    }
}
