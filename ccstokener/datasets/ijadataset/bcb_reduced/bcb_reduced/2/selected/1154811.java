package gr.academic.city.msc.industrial.mobileclickers.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

/**
 * 
 * @author Ivo Neskovic <ineskovic@6pmplc.com>
 */
public final class Communicator {

    private static final String USER_AGENT = "MobileClickersDroid";

    private static final String ENCODING = "UTF-8";

    private static final String PARAM_SEPARATOR = "/";

    /**
	 * The static instance of the {@link Communicator}, as per the typical
	 * Singleton design pattern. Note the usage of the volatile keyword for
	 * thread-safe programming.
	 */
    private static volatile Communicator instance;

    /**
	 * Static initialisation method. This method will always return the
	 * singleton instance of the {@link Communicator} object and it will
	 * initialise it the first time it is requested. It uses the double-checked
	 * locking mechanism and initialisation on demand.
	 * 
	 * @return the system-wide singleton instance of {@link Communicator}.
	 */
    public static Communicator getInstance() {
        if (instance == null) {
            synchronized (Communicator.class) {
                if (instance == null) {
                    instance = new Communicator();
                }
            }
        }
        return instance;
    }

    /**
	 * A private no-args default constructor, preventing the manual
	 * initialisation of this object as per the singleton pattern.
	 */
    private Communicator() {
        client = HttpClientFactory.getInstance().produceHttpClient(HttpClientFactory.HttpClientType.THREAD_SAFE);
    }

    private HttpClient client;

    public int fetchQuestion(String questionCode) throws URISyntaxException, UnsupportedEncodingException, IOException {
        HttpPost postRequest = new HttpPost();
        postRequest.setURI(new URI("http://localhost:8080/MobileClickersWebModule/resources/question/".concat(URLEncoder.encode(questionCode, ENCODING))));
        return Integer.parseInt(transportMessage(postRequest));
    }

    public boolean answerQuestion(String questionCode, String answer, String uniqueSubmissionCode) throws URISyntaxException, ClientProtocolException, IOException {
        HttpPost postRequest = new HttpPost();
        postRequest.setURI(new URI("http://localhost:8080/MobileClickersWebModule/resources/question/".concat(URLEncoder.encode(questionCode, ENCODING)).concat(PARAM_SEPARATOR).concat(URLEncoder.encode(answer, ENCODING)).concat(PARAM_SEPARATOR).concat(URLEncoder.encode(uniqueSubmissionCode, ENCODING))));
        return transportMessageGetStatus(postRequest);
    }

    private String transportMessage(HttpPost postRequest) throws UnsupportedEncodingException, IOException {
        postRequest.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(postRequest);
        return convertStreamToString(response.getEntity().getContent());
    }

    private boolean transportMessageGetStatus(HttpPost postRequest) throws ClientProtocolException, IOException {
        postRequest.setHeader("User-Agent", USER_AGENT);
        return client.execute(postRequest).getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append((line + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
