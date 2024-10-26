package uebung13.ml.aufgabe06;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author tbeeler
 * @edited msuess
 */
public class HTMLSite {

    private String content;

    private String[] outlinks;

    private URL url;

    public HTMLSite(String url) throws MalformedURLException, IOException {
        this.url = new URL(url);
        content = fetchContent();
        parseOutLinks();
    }

    public String[] getOutLinks() {
        return outlinks;
    }

    public String getContent() {
        return content;
    }

    private String fetchContent() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer buf = new StringBuffer();
        String str;
        while ((str = reader.readLine()) != null) {
            buf.append(str);
        }
        return buf.toString();
    }

    private void parseOutLinks() {
        ArrayList<String> temp = new ArrayList<String>();
        int indx = 0;
        int indx2 = 0;
        String t1 = "<a";
        String t2 = "href=\"";
        String t3 = "\"";
        while (indx >= 0) {
            indx = content.toLowerCase().indexOf(t1, indx);
            if (indx == -1) break;
            indx = content.toLowerCase().indexOf(t2, indx);
            if (indx == -1) break;
            indx += t2.length();
            indx2 = content.toLowerCase().indexOf(t3, indx);
            if (indx2 == -1) break;
            temp.add(content.substring(indx, indx2));
            indx = indx2;
        }
        indx = 0;
        indx2 = 0;
        t1 = "<frame";
        t2 = "src=\"";
        t3 = "\"";
        while (indx >= 0) {
            indx = content.toLowerCase().indexOf(t1, indx);
            if (indx == -1) break;
            indx = content.toLowerCase().indexOf(t2, indx);
            if (indx == -1) break;
            indx += t2.length();
            indx2 = content.toLowerCase().indexOf(t3, indx);
            if (indx2 == -1) break;
            temp.add(content.substring(indx, indx2));
            indx = indx2;
        }
        outlinks = new String[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            outlinks[i] = temp.get(i);
        }
    }
}
