import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.StringTokenizer;

public class ClientLogin {

    /**
	   * URL used for authenticating and obtaining an authentication token. 
	   * More details about how it works:
	   * <code>http://code.google.com/apis/accounts/AuthForInstalledApps.html<code>
	   */
    private static final String AUTHENTICATION_URL = "https://www.google.com/accounts/ClientLogin";

    private String EMAIL;

    private String PASSWORD;

    private String SERVICE;

    public static void main(String[] args) {
        ClientLogin cl = new ClientLogin(args[0], args[1], args[2]);
        System.out.println(cl.authenticate());
    }

    public ClientLogin(String email, String password, String service) {
        this.EMAIL = email;
        this.PASSWORD = password;
        this.SERVICE = service;
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
        content.append("&service=").append(URLEncoder.encode(SERVICE, "UTF-8"));
        content.append("&source=").append(URLEncoder.encode("Google Base data API", "UTF-8"));
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
