package megaupload;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import algutil.system.Systeme;

public class MegaFilmsWebSite {

    public static final String URL_PAGE_1 = "http://www.mega-films.net/";

    public static final String URL_PAGE_2 = "http://www.mega-films.net/page/2/";

    public static final String URL_PAGE_3 = "http://www.mega-films.net/page/3/";

    private String url = null;

    private static final Logger log = Logger.getLogger(MegaFilmsWebSite.class);

    public MegaFilmsWebSite() {
        url = URL_PAGE_1;
    }

    public MegaFilmsWebSite(String url) {
        this.url = url;
    }

    private void parse() throws Exception {
        BufferedReader br = null;
        InputStream httpStream = null;
        URL fileURL = new URL(url);
        URLConnection urlConnection = fileURL.openConnection();
        httpStream = urlConnection.getInputStream();
        br = new BufferedReader(new InputStreamReader(httpStream, "UTF-8"));
        String ligne;
        String post;
        String date;
        String titre;
        String resume;
        String url2DL;
        while ((ligne = br.readLine()) != null) {
            if (ligne.indexOf("div class=\"post\" id=\"post") != -1) {
                post = null;
                date = null;
                titre = null;
                try {
                    post = ligne.substring(ligne.indexOf("post-") + 5, ligne.indexOf("\"", ligne.indexOf("post-")));
                    ligne = br.readLine();
                    date = ligne.substring(ligne.indexOf("<div class=\"date\"><span>") + 24);
                    date = date.replaceAll("</span>", "").replaceAll("</div>", "").trim();
                    log.info("Post   : " + post + " du " + date);
                    ligne = br.readLine();
                    ligne = br.readLine();
                    titre = ligne.substring(ligne.indexOf(">", ligne.indexOf("title")) + 1, ligne.indexOf("</a>"));
                    titre = titre.replaceAll("&#8217;", "'").replaceAll("&#8220;", "\"").replaceAll("&#8221;", "\"");
                    url2DL = ligne.substring(ligne.indexOf("<a href=\"") + 9, ligne.indexOf("/\"")).trim();
                    url2DL = url2DL.replace("mega-films.net", "mega-protect.com") + ".php";
                    log.info("Titre  : " + titre);
                    log.info("To DL  : " + url2DL);
                    ligne = br.readLine();
                    ligne = br.readLine();
                    ligne = br.readLine();
                    ligne = br.readLine();
                    ligne = br.readLine();
                    ligne = br.readLine();
                    ligne = br.readLine();
                    resume = ligne.substring(ligne.indexOf("<em>") + 4, ligne.indexOf("</em>"));
                    resume = resume.replaceAll("&#8217;", "'").replaceAll("&#8220;", "\"").replaceAll("&#8221;", "\"");
                    log.info("Resume : " + resume);
                } catch (Exception e) {
                    log.error("ERREUR : Le film n'a pas pu etre parse...");
                }
                log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            DOMConfigurator.configure("conf/log4j.xml");
        } catch (Exception e) {
        }
        if (Systeme.getOS() == Systeme.WINDOWS_OS) {
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("http.proxyHost", "172.16.1.138");
            systemProperties.setProperty("http.proxyPort", "3128");
        }
        MegaFilmsWebSite mf = new MegaFilmsWebSite(MegaFilmsWebSite.URL_PAGE_1);
        mf.parse();
        mf = new MegaFilmsWebSite(MegaFilmsWebSite.URL_PAGE_2);
        mf.parse();
    }
}
