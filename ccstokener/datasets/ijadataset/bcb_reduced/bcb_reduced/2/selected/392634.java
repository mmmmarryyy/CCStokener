package aspirateur;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class Aspirateur {

    private Parser parser;

    private ListeDePages pagesATraiter;

    private ListeDePages pagesTraitees;

    private ListeDePages imagesATelecharger;

    private ListeDePages cssATelecharger;

    private ListeDePages jsATelecharger;

    private String source;

    private Filtre filtre;

    private ChercheurDeBalise chercheur;

    private String destination;

    public Aspirateur(String url, String d, Filtre f) {
        filtre = f;
        chercheur = new ChercheurDeBalise();
        if (url.endsWith("/")) url = url.substring(0, url.lastIndexOf("/"));
        if (url.contains("index.")) url = url.substring(0, url.indexOf("index."));
        source = url;
        destination = d;
        pagesATraiter = new ListeDePages();
        pagesTraitees = new ListeDePages();
        imagesATelecharger = new ListeDePages();
        cssATelecharger = new ListeDePages();
        jsATelecharger = new ListeDePages();
        pagesATraiter.ajouterPage(new Lien(source, 0));
        try {
            URL _url = new URL(source);
            URLConnection urlConnection = _url.openConnection();
            parser = new Parser(urlConnection);
            System.out.println("---Aspiration des pages HTML");
            while (!pagesATraiter.estVide()) {
                aspirer();
            }
            System.out.println("Fin de l'aspiration des pages");
            System.out.println("\n\n---Aspiration des feuilles de style");
            while (!cssATelecharger.estVide()) {
                telechargerCSS();
            }
            System.out.println("Fin de l'aspiration des feuilles de style");
            System.out.println("\n\n---Aspiration des Javascripts");
            while (!jsATelecharger.estVide()) {
                telechargerJS();
            }
            System.out.println("Fin de l'aspiration des feuilles de style");
            System.out.println("\n\n---Aspiration des images");
            while (!imagesATelecharger.estVide()) {
                telechargerImage();
            }
            System.out.println("Fin de l'aspiration des images");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }
    }

    private void telechargerJS() {
        String url = jsATelecharger.getFirst().getUrl();
        System.out.println("Telechargement du script " + url + " en cours...");
        CSSandJSDownloader id = new CSSandJSDownloader(url, source, destination);
        jsATelecharger.supprimerPage(url);
        id.save();
        System.out.println("Telechargement du script " + url + " termin");
    }

    /**
	 * tlchargement d'une feuille CSS
	 *
	 */
    private void telechargerCSS() {
        String url = cssATelecharger.getFirst().getUrl();
        System.out.println("Telechargement du css " + url + " en cours...");
        CSSandJSDownloader id = new CSSandJSDownloader(url, source, destination);
        cssATelecharger.supprimerPage(url);
        id.save();
        System.out.println("Telechargement du css " + url + " termin");
    }

    /**
	 * tlchargement d'une image
	 *
	 */
    private void telechargerImage() {
        String url = imagesATelecharger.getFirst().getUrl();
        System.out.println("Telechargement de l'image " + url + " en cours...");
        ImageDownloader id = new ImageDownloader(url, source, destination);
        imagesATelecharger.supprimerPage(url);
        id.save();
        System.out.println("Telechargement de l'image " + url + " fini.");
    }

    /**
	 * mthode d'aspiration d'une page
	 *
	 */
    public void aspirer() {
        String url = pagesATraiter.getFirst().getUrl();
        int profondeur = pagesATraiter.getFirst().getProfondeur();
        if (profondeur > filtre.getProfondeur()) {
            pagesTraitees.ajouterPage(new Lien(url, profondeur));
            pagesATraiter.supprimerPage(url);
            return;
        }
        System.out.println("Telechargement de " + url + " en cours...");
        try {
            parser.setURL(url);
            NodeList listeNoeuds = new NodeList();
            NodeIterator ni = parser.elements();
            while (ni.hasMoreNodes()) listeNoeuds.add(ni.nextNode());
            ArrayList<String> liste = new ArrayList<String>();
            for (int i = 0; i < listeNoeuds.size(); i++) {
                Node n = listeNoeuds.elementAt(i);
                String html = n.toHtml();
                html = chercheur.chercherHREF(html, profondeur, source, pagesATraiter, pagesTraitees);
                chercheur.chercherBalise(html, "IMG", "SRC", source, imagesATelecharger);
                chercheur.chercherBalise(html, "LINK", "HREF", source, cssATelecharger);
                chercheur.chercherBalise(html, "SCRIPT", "SRC", source, jsATelecharger);
                liste.add(html);
            }
            PageDownloader edp = new PageDownloader(url, liste, source, destination);
            edp.save();
            System.out.println("Telechargement de " + url + " fini");
        } catch (ParserException e) {
            System.out.println("(ParserException)");
        }
        pagesTraitees.ajouterPage(new Lien(url, profondeur));
        pagesATraiter.supprimerPage(url);
    }

    /**
	 * main
	 * @param args
	 */
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.print("Site  tlcharger [triloc.hd.free.fr/~sasha] : ");
        String site_a_dl = scan.nextLine();
        System.out.println();
        if (site_a_dl.compareTo("") == 0) site_a_dl = "http://triloc.hd.free.fr/~sasha";
        System.out.print("Destination [dossier captures] : ");
        String destination = scan.nextLine();
        if (destination.compareTo("") == 0) destination = "captures";
        System.out.println();
        new Aspirateur(site_a_dl, destination, new Filtre(2));
    }

    public static String formaterImage(String src) {
        return src;
    }
}
