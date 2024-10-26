import java.net.*;
import java.io.*;
import java.util.*;

public class CCDBProxyClient {

    String httpURL = "http://meower-db.ucsd.edu/CCDBProxy/getCCDBProxy";

    public String getProxy(String userName, String password) throws Exception {
        URL url = new URL(httpURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        ObjectOutputStream outputToServlet = new ObjectOutputStream(conn.getOutputStream());
        outputToServlet.writeObject(userName);
        outputToServlet.writeObject(password);
        outputToServlet.flush();
        outputToServlet.close();
        ObjectInputStream inputFromServlet = new ObjectInputStream(conn.getInputStream());
        return inputFromServlet.readObject() + "";
    }

    public static void main(String[] args) {
        try {
            String userName = "CCDB_DATA_USER";
            String password = "vis16le_CCdb";
            CCDBProxyClient client = new CCDBProxyClient();
            System.out.println(client.getProxy(userName, password));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
