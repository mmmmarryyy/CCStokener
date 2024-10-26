package networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class MasterServerInterface {

    private static final String MASTER_SERVER = "http://wasserleiche.cwsurf.de";

    private static final String REST = "<script";

    private static final String GETGAMES = "getgames";

    private static final String REGISTERGAME = "register";

    private static final String DELETEGAME = "delete";

    private static URLConnection connect(String address) {
        try {
            URL url = new URL(address);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setDoOutput(true);
            return urlConnection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean masterServerReachable() {
        URLConnection connection = connect(MASTER_SERVER);
        try {
            connection.getOutputStream();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static String getServerResponse(String type) {
        OutputStreamWriter out = null;
        BufferedReader reader = null;
        URLConnection urlConnection = null;
        try {
            urlConnection = connect(MASTER_SERVER);
            if (urlConnection == null) return null;
            out = new OutputStreamWriter(urlConnection.getOutputStream());
            String data = "type=" + type;
            out.write(data);
            out.flush();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line = "";
            while (reader.ready()) {
                line += reader.readLine().split(REST)[0];
            }
            return line;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static String[] getActiveGames() {
        String response = getServerResponse(GETGAMES);
        if (response != null) return response.split(":");
        return null;
    }

    public static String registerNewGame() {
        return getServerResponse(REGISTERGAME);
    }

    public static void deleteGame() {
        getServerResponse(DELETEGAME);
    }
}
