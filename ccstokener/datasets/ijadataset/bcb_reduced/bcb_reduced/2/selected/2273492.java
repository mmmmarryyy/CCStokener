package fit.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class ServletInvoker {

    private final String servletUrl;

    private final String testDirPath;

    private final String resultDirPath;

    static final int ERROR = -1;

    static final String EXIT_CODE_PREFIX = "Exit Code = ";

    static final String USAGE = "usage: java fit.ServletInvoker testDirPath resultDirPath servletUrl";

    public static void main(String[] args) {
        try {
            new ServletInvoker(args).run();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public ServletInvoker(String[] args) {
        validateInput(args);
        this.testDirPath = args[0];
        this.resultDirPath = args[1];
        this.servletUrl = args[2];
    }

    private void validateInput(String[] args) {
        if (args == null || args.length != 3) {
            usage();
        }
    }

    public void run() throws Exception {
        int returnCode = postUrlRequest();
        System.exit(returnCode);
    }

    private void usage() {
        System.out.println(USAGE);
        throw new IllegalArgumentException(USAGE);
    }

    String urlWithRequestParams() {
        return servletUrl + "?testDirPath=" + testDirPath + "&resultsDirPath=" + resultDirPath;
    }

    private int postUrlRequest() throws Exception {
        HttpURLConnection conn = createHttpConnection();
        conn.connect();
        int errorCode = readResults(conn);
        conn.disconnect();
        return errorCode;
    }

    int readResults(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        int errorCode = returnCode(reader);
        reader.close();
        return errorCode;
    }

    HttpURLConnection createHttpConnection() throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(urlWithRequestParams());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return conn;
    }

    int returnCode(BufferedReader reader) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            int index = line.indexOf(EXIT_CODE_PREFIX);
            if (index != -1) {
                return Integer.parseInt(line.substring(index + EXIT_CODE_PREFIX.length()));
            }
            System.out.println(line);
        }
        return ERROR;
    }
}
