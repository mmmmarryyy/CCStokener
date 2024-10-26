package org.exist.examples.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.exist.Namespaces;
import org.exist.storage.DBBroker;

/**
 * PostExample
 * Execute: bin\run.bat org.exist.examples.http.PostExample
 * Make sure you have the server started with bin\startup.bat beforehand.
 *
 * @author wolf
 */
public class PostExample {

    public static final String REQUEST_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<query xmlns=\"" + Namespaces.EXIST_NS + "\" ";

    public static final String REQUEST_FOOTER = "</query>";

    public static final String PROPERTIES = "<properties>" + "<property name=\"indent\" value=\"yes\"/>" + "<property name=\"encoding\" value=\"UTF-8\"/>" + "</properties>";

    public void query(String query) throws IOException {
        String request = REQUEST_HEADER + " howmany=\"-1\">" + "<text>" + query + "</text>" + PROPERTIES + REQUEST_FOOTER;
        doPost(request);
    }

    private void doPost(String request) throws IOException {
        URL url = new URL("http://localhost:8080/exist/rest" + DBBroker.ROOT_COLLECTION);
        HttpURLConnection connect = (HttpURLConnection) url.openConnection();
        connect.setRequestMethod("POST");
        connect.setDoOutput(true);
        OutputStream os = connect.getOutputStream();
        os.write(request.getBytes("UTF-8"));
        connect.connect();
        BufferedReader is = new BufferedReader(new InputStreamReader(connect.getInputStream()));
        String line;
        while ((line = is.readLine()) != null) System.out.println(line);
    }

    public static void main(String[] args) {
        PostExample client = new PostExample();
        try {
            client.query("declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\";\ndeclare namespace dc=\"http://purl.org/dc/elements/1.1/\";\n//rdf:Description[dc:subject &amp;= 'umw*']");
        } catch (IOException e) {
            System.err.println("An exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
