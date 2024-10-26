package net.sourceforge.juploader.upload.server;

import java.util.ResourceBundle;
import net.sourceforge.juploader.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import net.sourceforge.juploader.upload.ClientHttpRequest;

/**
 *
 * @author proktor
 */
public class ZippyShare extends BasicServer {

    private final int DIRECT = 0;

    private final int FORUM = 1;

    private final int THUMB = 2;

    private final int MAX_FILESIZE = 524288000;

    public ZippyShare() {
        final ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/sourceforge/juploader/upload/server/Bundle");
        fieldNames = new String[] { bundle.getString("ZZLink"), bundle.getString("ZSForum"), bundle.getString("ZSThumbnail") };
    }

    public String[] uploadFile() {
        links = new String[fieldNames.length];
        try {
            URL url1 = new URL("www.zippyshare.com/upload");
            HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
            ClientHttpRequest req = new ClientHttpRequest(conn1);
            req.setParameter("file_0", new File(fileName));
            req.setParameter("terms", "1");
            req.post();
            Map<String, List<String>> field = conn1.getHeaderFields();
            String cookie = field.get("Set-Cookie").get(0);
            URL url2 = new URL("http://www1.zippyshare.com/links.jsp?link=1");
            HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
            conn2.setRequestMethod("GET");
            conn2.setRequestProperty("Cookie", cookie);
            conn2.setDoOutput(false);
            InputStream inStream = conn2.getInputStream();
            createLinks(inStream);
            conn1.disconnect();
            conn2.disconnect();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            net.sourceforge.juploader.app.Error.showConnectionError();
        }
        return links;
    }

    private boolean createLinks(InputStream serverInput) {
        Scanner in = new Scanner(serverInput);
        boolean searching = true;
        while (in.hasNext() && searching) {
            String line = in.nextLine().trim();
            if (line.contains("Link:")) {
                line = in.nextLine();
                links[DIRECT] = line.substring(line.indexOf("http://"), line.indexOf(".html") + 5);
            } else if (line.contains("Forum:")) {
                line = in.nextLine();
                links[FORUM] = line.substring(line.indexOf("[url="), line.indexOf("[/url]") + 6);
            } else if (line.contains("Thumbnail:")) {
                line = in.nextLine();
                links[THUMB] = Utils.replaceHtmlCharacters(line.substring(line.indexOf("&lt;"), line.lastIndexOf("&gt;") + 4));
                searching = false;
            }
        }
        return true;
    }

    public Servers getServer() {
        return Servers.ZIPPYSHARE;
    }

    public boolean isAcceptable(String fileName) {
        return new File(fileName).length() < MAX_FILESIZE;
    }
}
