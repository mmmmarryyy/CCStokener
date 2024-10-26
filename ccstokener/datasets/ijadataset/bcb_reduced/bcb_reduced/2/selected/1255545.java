package org.mcisb.util.net;

import java.io.*;
import java.net.*;
import java.util.*;
import org.mcisb.util.io.*;

/**
 *
 * @author Neil Swainston
 */
public class NetUtils {

    /**
	 * 
	 */
    private static final Random random = new Random();

    /**
	 *
	 * @param url
	 * @param parameters
	 * @return InputStream
	 * @throws IOException
	 */
    public static String getUrl(final String url, final Map<String, Object> parameters) throws IOException {
        final String SEPARATOR = "?";
        return url + SEPARATOR + getQueryString(parameters, false);
    }

    /**
	 *
	 * @param url
	 * @param parameters
	 * @return InputStream
	 * @throws IOException
	 */
    public static InputStream doPost(final URL url, final Map<String, Object> parameters) throws IOException {
        return doPost(url, parameters, true);
    }

    /**
	 *
	 * @param url
	 * @param parameters
	 * @param encode
	 * @return InputStream
	 * @throws IOException
	 */
    public static InputStream doPost(final URL url, final Map<String, Object> parameters, final boolean encode) throws IOException {
        final URLConnection urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = null;
        try {
            os = new DataOutputStream(urlConn.getOutputStream());
            os.write(getQueryString(parameters, encode).getBytes());
            os.flush();
        } finally {
            if (os != null) {
                os.close();
            }
        }
        return urlConn.getInputStream();
    }

    /**
	 * 
	 * @param parameters
	 * @param encode 
	 * @return String
	 * @throws UnsupportedEncodingException
	 */
    public static String getQueryString(final Map<String, Object> parameters, final boolean encode) throws UnsupportedEncodingException {
        final String ENCODING = "UTF-8";
        final String EQUALS = "=";
        final String AMPERSAND = "&";
        final String EQUALS_ENCODED = encode ? URLEncoder.encode(EQUALS, ENCODING) : EQUALS;
        final String AMPERSAND_ENCODED = encode ? URLEncoder.encode(AMPERSAND, ENCODING) : AMPERSAND;
        final StringBuffer queryString = new StringBuffer();
        for (Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<String, Object> entry = iterator.next();
            queryString.append(encode ? URLEncoder.encode(entry.getKey(), ENCODING) : entry.getKey());
            queryString.append(EQUALS_ENCODED);
            queryString.append(encode ? URLEncoder.encode(entry.getValue().toString(), ENCODING) : entry.getValue().toString());
            queryString.append(AMPERSAND_ENCODED);
        }
        if (queryString.length() > 0) {
            queryString.setLength(queryString.length() - AMPERSAND_ENCODED.length());
        }
        return queryString.toString();
    }

    /**
	 * 
	 * @param url
	 * @param parameters
	 * @return HttpURLConnection
	 * @throws IOException
	 */
    public static HttpURLConnection doPostMultipart(final URL url, final Map<String, Object> parameters) throws IOException {
        final int RADIX = 36;
        final String SEPARATOR = "--";
        final String BOUNDARY = Long.toString(random.nextLong(), RADIX);
        final String NEW_LINE = "\r\n";
        BufferedWriter writer = null;
        try {
            final HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Host", url.getHost());
            http.setRequestProperty("Content-Type", "multipart/form-data, boundary=" + BOUNDARY);
            http.setRequestProperty("Cache-Control", "no-cache");
            http.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            http.setDoOutput(true);
            writer = new BufferedWriter(new OutputStreamWriter(http.getOutputStream()));
            for (Iterator<Map.Entry<String, Object>> i = parameters.entrySet().iterator(); i.hasNext(); ) {
                final Map.Entry<String, Object> entry = i.next();
                Object value = null;
                writer.write(SEPARATOR);
                writer.write(BOUNDARY);
                writer.write(NEW_LINE);
                writer.write("Content-Disposition: form-data; name=\"");
                writer.write(entry.getKey());
                writer.write("\"");
                if (entry.getValue() instanceof File) {
                    final File file = (File) entry.getValue();
                    writer.write("; filename=\"");
                    writer.write(file.getName());
                    writer.write("\"");
                    writer.write(NEW_LINE);
                    writer.write("Content-Type:");
                    String contentType = URLConnection.guessContentTypeFromName(file.getPath());
                    if (contentType == null) {
                        contentType = "text/plain";
                    }
                    writer.write(contentType);
                    value = new String(new FileUtils().read(file.toURI().toURL()));
                } else {
                    value = entry.getValue();
                }
                writer.write(NEW_LINE);
                writer.write(NEW_LINE);
                writer.write(value.toString());
                writer.write(NEW_LINE);
            }
            writer.write(SEPARATOR);
            writer.write(BOUNDARY);
            writer.write(SEPARATOR);
            writer.write(NEW_LINE);
            writer.close();
            return http;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
