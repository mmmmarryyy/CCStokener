package jeliot.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class NetworkUtils {

    public static String getContent(String urlName) throws Exception {
        URL url = new URL(urlName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String response = new String();
        response = readFromConnection(connection);
        connection.disconnect();
        return response;
    }

    public static String readFromConnection(HttpURLConnection con) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine = new String();
        String tmp = new String();
        while ((tmp = in.readLine()) != null) {
            inputLine = inputLine.concat(tmp);
            inputLine = inputLine.concat("\n");
        }
        in.close();
        return inputLine;
    }

    public static void postContent(String urlName) throws Exception {
        URLConnection dbpc = (new URL(urlName)).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(dbpc.getInputStream()));
    }
}
