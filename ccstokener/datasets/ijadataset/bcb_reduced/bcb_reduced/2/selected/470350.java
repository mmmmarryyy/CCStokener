package controler.http;

import java.util.Vector;
import java.lang.Byte;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.ConnectException;
import java.net.MalformedURLException;

/**
 * Perform a simple http request
 * under the control of a global http manager.
 * This class implements the Runnable interface
 * to ensure efficiency and flexibility.
 */
class HttpUnit implements Runnable {

    private URL url;

    private ParallelHttp manager;

    private HttpURLConnection conn;

    /**
     * Constructor and initializer
     * 
     * @param url    the URL of the resource to retrieve
     * @param manager the http manager that control the requests
     * @throws MalformedURLException if supplied URL doesn't even look like a valid one
     * the method throws an exception
     */
    public HttpUnit(String url, ParallelHttp manager) throws MalformedURLException {
        this.url = new URL(url);
        this.conn = null;
        this.manager = manager;
    }

    /**
     * the class implements the Runnable interface and
     * has to have its logic in the run() method.
     * Here is performed the http request.
     */
    public void run() {
        byte[] bytesArray = null;
        try {
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.connect();
            } catch (ConnectException e) {
                System.err.println("[!] Can't connect to " + url.toString());
                return;
            } catch (IOException e) {
                System.err.println("[!] HttpUnit : " + e.getMessage());
                return;
            }
            Vector<Byte> datas = new Vector<Byte>();
            DataInputStream input;
            try {
                input = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
            } catch (IOException e) {
                System.err.println("[!] HttpUnit " + url.toString() + ") not found");
                return;
            }
            try {
                while (input.available() != 0) {
                    datas.add(new Byte((byte) input.readByte()));
                }
                bytesArray = new byte[datas.size()];
                int i;
                for (i = 0; i < datas.size(); i++) {
                    bytesArray[i] = datas.elementAt(i).byteValue();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                manager.addRequestResults(url.toString(), bytesArray);
            } catch (NullPointerException e) {
            }
            manager.requestFinalize();
        }
    }

    /**
     * get the targeted URL
     * 
     * @return the URL, as an URL object
     */
    public URL getUrl() {
        return url;
    }

    /**
     * set the targeted URL
     * 
     * @param url    the new url
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * get the associated HttpURLConnection
     * 
     * @return the connection
     */
    public HttpURLConnection getConn() {
        return conn;
    }

    /**
     * set the HttpURLCOnnection
     * 
     * @param conn the new connection
     */
    public void setConn(HttpURLConnection conn) {
        this.conn = conn;
    }
}
