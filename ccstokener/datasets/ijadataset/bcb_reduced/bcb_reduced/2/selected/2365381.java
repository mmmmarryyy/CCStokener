package algutil.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;

public class HTMLTableParser {

    private String url;

    private String oneLine;

    private String allDocInOneLine;

    private TableTag tableTab;

    private static final Logger log = Logger.getLogger(HTMLTableParser.class);

    public HTMLTableParser(String url) throws IOException {
        this.url = url;
    }

    public String parseInOneLine() throws Exception {
        BufferedReader br = null;
        InputStream httpStream = null;
        if (url.startsWith("http")) {
            URL fileURL = new URL(url);
            URLConnection urlConnection = fileURL.openConnection();
            httpStream = urlConnection.getInputStream();
            br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        } else {
            br = new BufferedReader(new FileReader(url));
        }
        StringBuffer sb = new StringBuffer();
        StringBuffer sbAllDoc = new StringBuffer();
        String ligne = null;
        boolean get = false;
        while ((ligne = br.readLine()) != null) {
            log.debug(ligne);
            sbAllDoc.append(ligne + " ");
            if (ligne.indexOf("<table") != -1) {
                get = true;
            }
            if (get) {
                sb.append(ligne + " ");
            }
            if (ligne.indexOf("</table") != -1 || ligne.indexOf("</tr></font><center><a href='affichaire.php") != -1 || ligne.indexOf("</font><center><a href='afficheregion.php") != -1) {
                get = false;
                break;
            }
        }
        oneLine = sb.toString();
        allDocInOneLine = sbAllDoc.toString();
        if (oneLine.indexOf("</table") != -1) {
            tableTab = new TableTag(oneLine.substring(oneLine.indexOf(">") + 1, oneLine.indexOf("</table")));
        } else if (oneLine.indexOf("</font><center><a href='affichaire") != -1) {
            tableTab = new TableTag(oneLine.substring(oneLine.indexOf(">") + 1, oneLine.indexOf("</font><center><a href='affichaire")));
        } else if (oneLine.indexOf("</font><center><a href='afficheregion.php") != -1) {
            tableTab = new TableTag(oneLine.substring(oneLine.indexOf(">") + 1, oneLine.indexOf("</font><center><a href='afficheregion.php")));
        } else {
            log.error("La fin du fichier HTML n'a pas ete trouvee, ca va merder...");
        }
        br.close();
        if (httpStream != null) {
            httpStream.close();
        }
        return allDocInOneLine;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOneLine() {
        return oneLine;
    }

    public void setOneLine(String oneLine) {
        this.oneLine = oneLine;
    }

    public TableTag getTableTab() {
        return tableTab;
    }

    public void setTableTab(TableTag tableTab) {
        this.tableTab = tableTab;
    }

    public String getAllDocInOneLine() {
        return allDocInOneLine;
    }

    public void setAllDocInOneLine(String allDocInOneLine) {
        this.allDocInOneLine = allDocInOneLine;
    }
}
