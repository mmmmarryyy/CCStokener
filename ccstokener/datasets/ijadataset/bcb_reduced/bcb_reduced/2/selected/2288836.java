package WebCodeLayer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

/**
 * Classe contenant des methodes statiques pour le traitement des URL et
 * des pages web.
 * @author gregory
 *
 */
public class URLResolver {

    /**
	 * L instance du gestionnaire d'evenements 
	 * utilisee dans l'execution presente
	 */
    private static Logger logs = Logger.getLogger("logs");

    /**
	 * Methode statique permettant de recuperer le code d une page a partir
	 * de son Url passe en parametre
	 * @param anURL : une url
	 * @return String : code source de la page 
	 */
    public static String getStringFromAFileAtURL(String anURL) {
        String htmlCode = "<html><body></body></html>";
        try {
            URL url = new URL(anURL);
            URLConnection urlConnection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine = "";
            htmlCode = "";
            while ((inputLine = in.readLine()) != null) htmlCode += inputLine + "\n";
            in.close();
        } catch (Exception e) {
            logs.info("URLResolver : lien mort");
            JOptionPane.showMessageDialog(null, "lien mort");
            return "<html><body></body></html>";
        }
        return checkXMLTagsLowerCase(htmlCode);
    }

    /**
	 * Methode static permettant de recuperer le document DOM d'une page web
	 * dont l url est passe en parametre
	 * @param anURL : une url
	 * @return Document : document DOM de la page web
	 */
    public static Document getDomDocumentHTMLCodeFromURL(String anURL) {
        String res = getStringFromAFileAtURL(anURL);
        return getDomDocumentFromStringHTMLCode(res);
    }

    /**
	 * Methode statique permettant de recuperer le document DOM d'une page web
	 * dont le code est passe en parametre
	 * @param HTMLCode : code de la page web
	 * @return Document : document DOM de la page web
	 */
    public static Document getDomDocumentFromStringHTMLCode(String HTMLCode) {
        HTMLCode = checkXMLTagsLowerCase(HTMLCode);
        Tidy tidy = new Tidy();
        tidy.setXHTML(false);
        tidy.setShowWarnings(true);
        tidy.setXmlTags(true);
        InputStream inStream = new ByteArrayInputStream(HTMLCode.getBytes());
        tidy.setDocType("\"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\" ");
        Document dom = tidy.parseDOM(inStream, null);
        return dom;
    }

    /**
	 * Methode statique permettant de passer d'une adresse relative (path) 
	 * a une adresse absolue
	 * @param aUrl : url du site
	 * @param path : chemin relatif de la page
	 * @param isTheLastFile : 
	 * @return String : adresse path transformer en adresse relative
	 */
    public static String relativeToAbsolute(String aUrl, String path, boolean isTheLastFile) {
        if (path.startsWith("ftp:/") || path.startsWith("https:/") || path.startsWith("http:/") || path.startsWith("mailto:")) return path;
        return (new URLChecker(aUrl, path, isTheLastFile)).getAbsoluteURL();
    }

    /**
	 * Methode statique permettant de mettre les noms des balises et des parametres 
	 * en minuscule (utlile pour les getElementsByTagName de CodeParser)
	 * @param originalXML : code de la page avant la transformation
	 * @return String : code de la page apres la transformation
	 */
    public static String checkXMLTagsLowerCase(String originalXML) {
        String regExpOfTagName = "<[^<>' ']*";
        String regExpOfAttributeName = "[^' ']*=\"|[^' ']*=\'";
        Pattern p1 = Pattern.compile(regExpOfTagName);
        Pattern p2 = Pattern.compile(regExpOfAttributeName);
        Matcher m1 = p1.matcher(originalXML);
        Matcher m2 = p2.matcher(originalXML);
        while (m1.find()) {
            String partToReplace = originalXML.substring(m1.start(), m1.end());
            String firstPart = "", lastPart = "";
            if (m1.start() > 0) firstPart = originalXML.substring(0, m1.start());
            if (m1.end() < originalXML.length()) lastPart = originalXML.substring(m1.end(), originalXML.length());
            originalXML = firstPart + partToReplace.toLowerCase() + lastPart;
        }
        while (m2.find()) {
            String partToReplace = originalXML.substring(m2.start(), m2.end());
            String firstPart = "", lastPart = "";
            if (m2.start() > 0) firstPart = originalXML.substring(0, m2.start());
            if (m2.end() < originalXML.length()) lastPart = originalXML.substring(m2.end(), originalXML.length());
            originalXML = firstPart + partToReplace.toLowerCase() + lastPart;
        }
        return originalXML;
    }
}
