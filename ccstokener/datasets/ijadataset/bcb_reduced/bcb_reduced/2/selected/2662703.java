package com.google.api.gbase.examples.basic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.StringTokenizer;

/**
 * Display all items of a specific customer.
 */
public class QueryExample3 {

    /**
   * URL of the authenticated customer feed.
   */
    private static final String ITEMS_FEED = "http://base.google.com/base/feeds/items";

    /**
   * Insert here the developer key obtained for an "installed application" at
   * http://code.google.com/apis/base/signup.html
   */
    private static final String DEVELOPER_KEY = "";

    /**
   * URL used for authenticating and obtaining an authentication token. 
   * More details about how it works:
   * <code>http://code.google.com/apis/accounts/AuthForInstalledApps.html<code>
   */
    private static final String AUTHENTICATION_URL = "https://www.google.com/accounts/ClientLogin";

    /**
   * Fill in your Google Account email here.
   */
    private static final String EMAIL = "";

    /**
   * Fill in your Google Account password here.
   */
    private static final String PASSWORD = "";

    /**
   * Create a <code>QueryExample3</code> instance and call
   * <code>displayMyItems</code>, which displays all items that belong to the
   * currently authenticated user.
   */
    public static void main(String[] args) throws IOException {
        QueryExample3 queryExample = new QueryExample3();
        String token = queryExample.authenticate();
        new QueryExample3().displayMyItems(token);
    }

    /**
   * Retrieves the authentication token for the provided set of credentials.
   * @return the authorization token that can be used to access authenticated
   *         Google Base data API feeds
   */
    public String authenticate() {
        String postOutput = null;
        try {
            URL url = new URL(AUTHENTICATION_URL);
            postOutput = makeLoginRequest(url);
        } catch (IOException e) {
            System.out.println("Could not connect to authentication server: " + e.toString());
            System.exit(1);
        }
        StringTokenizer tokenizer = new StringTokenizer(postOutput, "=\n ");
        String token = null;
        while (tokenizer.hasMoreElements()) {
            if (tokenizer.nextToken().equals("Auth")) {
                if (tokenizer.hasMoreElements()) {
                    token = tokenizer.nextToken();
                }
                break;
            }
        }
        if (token == null) {
            System.out.println("Authentication error. Response from server:\n" + postOutput);
            System.exit(1);
        }
        return token;
    }

    /**
   * Displays the "items" feed, that is the feed that contains the items that
   * belong to the currently authenticated user.
   * 
   * @param token the authorization token, as returned by
   *        <code>authenticate<code>
   * @throws IOException if an IOException occurs while creating/reading the 
   *         request
   */
    public void displayMyItems(String token) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(ITEMS_FEED)).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "GoogleLogin auth=" + token);
        connection.setRequestProperty("X-Google-Key", "key=" + DEVELOPER_KEY);
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        System.out.println(toString(inputStream));
    }

    /**
   * Makes a HTTP POST request to the provided {@code url} given the provided
   * {@code parameters}. It returns the output from the POST handler as a
   * String object.
   * 
   * @param url the URL to post the request
   * @return the output from the Google Accounts server, as string
   * @throws IOException if an I/O exception occurs while
   *           creating/writing/reading the request
   */
    private String makeLoginRequest(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder content = new StringBuilder();
        content.append("Email=").append(URLEncoder.encode(EMAIL, "UTF-8"));
        content.append("&Passwd=").append(URLEncoder.encode(PASSWORD, "UTF-8"));
        content.append("&service=").append(URLEncoder.encode("gbase", "UTF-8"));
        content.append("&source=").append(URLEncoder.encode("Google Base data API example", "UTF-8"));
        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(content.toString().getBytes("UTF-8"));
        outputStream.close();
        int responseCode = urlConnection.getResponseCode();
        InputStream inputStream;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = urlConnection.getInputStream();
        } else {
            inputStream = urlConnection.getErrorStream();
        }
        return toString(inputStream);
    }

    /**
   * Writes the content of the input stream to a <code>String<code>.
   */
    private String toString(InputStream inputStream) throws IOException {
        String string;
        StringBuilder outputBuilder = new StringBuilder();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        }
        return outputBuilder.toString();
    }
}
