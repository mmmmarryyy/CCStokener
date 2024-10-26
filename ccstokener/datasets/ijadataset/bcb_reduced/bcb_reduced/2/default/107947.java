import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Resource {

    private static final String ONLINEJUDGE_URL = "http://uva.onlinejudge.org/index.php?option=com_onlinejudge&" + "Itemid=20&page=show_authorsrank&limit=" + Ranking.PAGE_SIZE + "&limitstart=";

    final int start;

    final Parser parser;

    Resource(int start, Parser parser) {
        this.start = start;
        this.parser = parser;
    }

    public void collectInto(List<String[]> results) {
        try {
            System.out.printf("Processando resource %d-%d \n", start, start + Ranking.PAGE_SIZE);
            HttpURLConnection urlConnection = openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(urlConnection)));
            results.addAll(parser.parse(reader));
            urlConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HttpURLConnection openConnection() throws IOException {
        URL url = new URL(ONLINEJUDGE_URL + start);
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        urlConnection.connect();
        return urlConnection;
    }

    private InputStream getInputStream(URLConnection connection) throws IOException {
        InputStream inputStream = null;
        String encoding = connection.getContentEncoding();
        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
            inputStream = new GZIPInputStream(connection.getInputStream());
        } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
            inputStream = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
        } else {
            inputStream = connection.getInputStream();
        }
        return inputStream;
    }
}
