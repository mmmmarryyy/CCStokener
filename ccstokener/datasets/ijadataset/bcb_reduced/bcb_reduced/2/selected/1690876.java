package org.webcastellum;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public final class LargeFormPostRequestTester {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(Version.tagLine());
            System.err.println("This tool tests a web application by sending a large POST request");
            System.err.println("Please provide the following arguments: url size-of-form-post-in-bytes");
            System.err.println("NOTE: Please use the full qualified real target URL (i.e. index.jsp) of the form post to avoid redirect trouble which might render your POST actually as a GET request");
            System.exit(-1);
        }
        final int size = Integer.parseInt(args[1]);
        final LargeFormPostRequestTester tester = new LargeFormPostRequestTester(args[0]);
        tester.sendLargePostRequest(size);
    }

    private final URL url;

    public LargeFormPostRequestTester(final String webAddress) throws MalformedURLException {
        if (webAddress == null) {
            throw new NullPointerException("webAddress must not be null");
        }
        this.url = new URL(webAddress);
    }

    public void sendLargePostRequest(final int size) throws IOException {
        String encodedData = URLEncoder.encode("test", WebCastellumFilter.DEFAULT_CHARACTER_ENCODING) + "=" + URLEncoder.encode("this is just a mass test", WebCastellumFilter.DEFAULT_CHARACTER_ENCODING);
        encodedData += "&" + URLEncoder.encode("payload", WebCastellumFilter.DEFAULT_CHARACTER_ENCODING) + "=" + createTestdata(size);
        final long start = System.currentTimeMillis();
        final long end;
        HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setFollowRedirects(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", "" + encodedData.length());
        DataOutputStream output = null;
        BufferedReader reader = null;
        try {
            output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(encodedData);
            output.flush();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
            }
            end = System.currentTimeMillis();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println("Duration: " + (end - start) + " ms");
    }

    private static final char[] TEST_DATA = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', ' ', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', ' ', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', ' ', 'x', 'y', 'z', '.', '\n' };

    static String createTestdata(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        final StringBuilder result = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            result.append(TEST_DATA[i % TEST_DATA.length]);
        }
        return result.toString();
    }
}
