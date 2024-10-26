package fr.amille.animebrowser.control.websearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import fr.amille.animebrowser.model.exception.HtmlSiteRecuperatorException;
import fr.amille.animebrowser.model.language.Language;
import fr.amille.animebrowser.model.site.Site;

/**
 * Classe qui se connecte à un serveur, et récupère une page web sous la forme
 * d'une chaîne de caractère
 * 
 * @author amille
 * 
 */
public class HtmlSiteSearcher {

    /**
	 * Connect and fetch site page content.
	 * 
	 * @return String with site content
	 */
    public String getSiteContent(final Site site) throws HtmlSiteRecuperatorException {
        URL url;
        try {
            url = new URL(site.getAddress());
            final URLConnection connection = url.openConnection();
            final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String str;
            final StringBuffer strBuff = new StringBuffer();
            while ((str = in.readLine()) != null) {
                strBuff.append(str);
            }
            in.close();
            return strBuff.toString();
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            throw new HtmlSiteRecuperatorException(site, Language.getInstance().get("MalformedURL") + site.getName());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
