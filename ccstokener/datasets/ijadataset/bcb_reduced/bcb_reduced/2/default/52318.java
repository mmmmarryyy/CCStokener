import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public final class OnlineClientData {

    private static final String ONLINE_CLIENT_DATA = "https://coopnet.svn.sourceforge.net/svnroot/coopnet/trunk/Client/misc/OnlineClientData/";

    private OnlineClientData() {
    }

    private static String extractFirstLine(String urlToFile) {
        try {
            URL url = new URL(urlToFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getLatestClientUrl() {
        return extractFirstLine(ONLINE_CLIENT_DATA + "LatestClient.txt");
    }
}
