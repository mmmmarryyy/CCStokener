package openfarm.interpreter.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class TalkToManager implements ManagerHttpXmlIO {

    private BufferedReader in;

    private OutputStreamWriter out;

    private URL url;

    private URLConnection connection;

    public TalkToManager(String location) throws IOException {
        url = new URL(location);
        in = null;
        out = null;
        connection = null;
    }

    public synchronized void connect() throws IOException {
        connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
    }

    public synchronized String readServerInput() throws IOException {
        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String input = null;
        String retStr = "";
        while ((input = in.readLine()) != null) {
            retStr += input;
        }
        in.close();
        return retStr;
    }

    public synchronized void writeToServer(String msg, String contentType) throws IOException {
        connection.setRequestProperty("Content-Type", contentType);
        this.writeToServer(msg);
    }

    public synchronized void writeToServer(String msg) throws IOException {
        out = new OutputStreamWriter(connection.getOutputStream());
        out.write(msg);
        out.close();
    }

    public synchronized void close() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        url = null;
        connection = null;
    }

    public URL getUrl() {
        return url;
    }

    public URLConnection getConnection() {
        return connection;
    }
}
