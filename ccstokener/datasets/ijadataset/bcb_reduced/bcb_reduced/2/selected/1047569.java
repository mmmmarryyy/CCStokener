package algutil.internet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class InternetUtil {

    public static List<String> getHTMLSourcePage(String url) throws Exception {
        List<String> lines = new ArrayList<String>();
        URL fileURL = new URL(url);
        URLConnection urlConnection = fileURL.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        String ligne;
        while ((ligne = br.readLine()) != null) {
            lines.add(ligne);
        }
        br.close();
        httpStream.close();
        return lines;
    }

    public static List<String> getLinesFromFile(File f) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String ligne;
        while ((ligne = br.readLine()) != null) {
            lines.add(ligne);
        }
        br.close();
        return lines;
    }
}
