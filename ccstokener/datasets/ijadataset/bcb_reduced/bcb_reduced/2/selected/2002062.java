package cm.util;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Utility class to perform HTTP POST request to the specified URL.
 */
public class HttpPostRequest {

    /**
     * Utility class to perform HTTP POST request to the specified URL.
     */
    public static StringBuffer sendPostRequest(String data, String urlString, String paramName) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        String postData = paramName + "=" + data;
        writer.write(postData);
        writer.flush();
        StringBuffer answer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            answer.append(line);
        }
        writer.close();
        reader.close();
        return answer;
    }
}
