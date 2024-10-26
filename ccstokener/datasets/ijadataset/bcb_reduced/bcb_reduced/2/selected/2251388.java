package dpdesktop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.HttpURLConnection;
import java.net.URL;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

/**
 *
 * @author Heiner Reinhardt
 */
public class DPDesktopVersion {

    public static final String currentVersion = "" + DPDesktopApp.class.getPackage().getImplementationVersion();

    private static final String versionURL = "http://dpdesktop.sourceforge.net/version.xml";

    public static String[] check() throws Exception {
        if (currentVersion == null) throw new Exception();
        URL url = new URL(versionURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String str = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while (br.ready()) {
            str = str + br.readLine();
        }
        br.close();
        Document document = DocumentHelper.parseText(str);
        Node node = document.selectSingleNode("//root/version");
        String latestVersion = node.valueOf("@id");
        Double latest = Double.parseDouble(latestVersion);
        Double current = Double.parseDouble(currentVersion.substring(0, currentVersion.indexOf("-")));
        if (latest > current) {
            String[] a = { latestVersion, node.valueOf("@url"), node.valueOf("@description") };
            return a;
        }
        return null;
    }
}
