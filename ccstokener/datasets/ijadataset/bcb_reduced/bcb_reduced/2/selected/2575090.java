package espider.libs.com.inzyme.jtrm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {

    protected URL url;

    protected HttpURLConnection server;

    /**
   * @param szUrl: String object for the URL
   */
    public HttpClient(String szUrl) {
        try {
            url = new URL(szUrl);
        } catch (Exception e) {
            System.out.println("Invalid URL");
        }
    }

    /**
   * @param method: String object for client method (POST, GET,...)
   */
    public void connect(String method) throws Exception {
        try {
            server = (HttpURLConnection) url.openConnection();
            server.setDoInput(true);
            server.setDoOutput(true);
            server.setRequestMethod(method);
            server.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            server.connect();
        } catch (Exception e) {
            throw new Exception("Connection failed");
        }
    }

    public void disconnect() {
        server.disconnect();
    }

    public void getResponse() {
        String line;
        try {
            BufferedReader s = new BufferedReader(new InputStreamReader(server.getInputStream()));
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter("mb.xml"));
            line = s.readLine();
            while (line != null) {
                bufWriter.write(line);
                line = s.readLine();
            }
            s.close();
            bufWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void post(String s) throws Exception {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
            bw.write(s, 0, s.length());
            bw.flush();
            bw.close();
        } catch (Exception e) {
            throw new Exception("Unable to write to output stream");
        }
    }
}
