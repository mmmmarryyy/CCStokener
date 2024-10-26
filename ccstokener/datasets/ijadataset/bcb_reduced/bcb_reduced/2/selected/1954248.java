package aspiration;

import gestionSites.Page;
import gestionSites.Site;
import gestionSites.SiteListHandler;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.ParserException;
import visiteurs.EmailVisitor;
import visiteurs.StatsVisitor;
import visiteurs.URLExtractingVisitor;
import visiteurs.URLModifyingVisitor;

/**
 * classe principale de capture d'un site
 *
 */
public class SiteCapturer extends Thread {

    /**
	 * liste des pages � capturer
	 */
    private LinkList pagesToBeCaptured;

    /**
	 * listes des pages d�j� captur�es
	 */
    private LinkList pagesCaptured;

    /**
	 * liste des fichiers � t�l�charger
	 */
    private LinkList filesToBeCaptured;

    /**
	 * liste des fichiers � t�l�charger
	 */
    private LinkList cssList;

    /**
	 * le parser
	 */
    private Parser parser;

    /**
	 * site en cours de capture
	 */
    private Site site;

    /**
	 * gestionnaire des sites
	 */
    private SiteListHandler handler;

    /**
	 * le t�l�chargeur
	 */
    private Downloader fd;

    /**
	 * taille maximum du site
	 */
    public static int maxSize;

    /**
	 * taille cumul�e des fichiers d�j� t�l�charg�s
	 */
    public static int accumulatedSize;

    /**
	 * gestionnaire de nombre de fichiers
	 */
    private NumbersHandler numberHandler;

    /**
	 * bool�en de fin de t�l�chargement
	 */
    private boolean finished;

    /**
	 * nombre de fichiers t�l�charg�s
	 */
    private int nbFichiersTelecharges;

    /**
	 * constructeur
	 * @param _site site � t�l�charger
	 * @param _fd downlader
	 * @param SLH gestionnaire de sites
	 * @param _maxSize taille max du site
	 */
    public SiteCapturer(Site _site, Downloader _fd, SiteListHandler SLH, int _maxSize) {
        finished = false;
        accumulatedSize = 0;
        nbFichiersTelecharges = 0;
        site = _site;
        pagesToBeCaptured = new LinkList();
        pagesCaptured = new LinkList();
        filesToBeCaptured = new LinkList();
        handler = SLH;
        maxSize = _maxSize;
        fd = _fd;
        pagesToBeCaptured.add(site.getSourceLink());
        numberHandler = new NumbersHandler();
        cssList = new LinkList();
    }

    /**
	 * lancement de la capture
	 */
    public void run() {
        System.out.println("DOWNLOAD FROM " + site.getSourceString());
        while (!pagesToBeCaptured.isEmpty() && accumulatedSize < maxSize) {
            pageCapture();
            numberHandler.notifier();
        }
        if (accumulatedSize < maxSize) {
            LinkListIterator it = filesToBeCaptured.createIterator();
            while (it.hasNext() && accumulatedSize < maxSize) {
                nbFichiersTelecharges++;
                numberHandler.notifier();
                fd.setUrl(it.next().getUrl());
                fd.save();
            }
            LinkListIterator it2 = cssList.createIterator();
            while (it2.hasNext()) {
                it2.next().parseCSS(filesToBeCaptured, site.getSourceString(), site.getTarget(), site.getName());
            }
            if (it.hasNext()) {
                System.out.println("Downloading image from CSS\n");
                while (it.hasNext() && accumulatedSize < maxSize) {
                    nbFichiersTelecharges++;
                    numberHandler.notifier();
                    fd.setUrl(it.next().getUrl());
                    fd.save();
                }
                if (accumulatedSize < maxSize) {
                    System.out.println("Reaching site size maximum, stopping the download");
                }
            }
        } else {
            System.out.println("Reaching site size maximum, stopping the download");
        }
        handler.add(site);
        site.setSiteSize(accumulatedSize);
        handler.writeList();
        site.drawGraph();
        System.out.println("\nDownload completed");
        finished = true;
        numberHandler.notifier();
    }

    /**
	 * capture d'une page
	 *
	 */
    public void pageCapture() {
        Link link = pagesToBeCaptured.get();
        try {
            System.out.println("Parsing " + link.getUrl());
            URL _url = new URL(link.getUrl());
            parser = new Parser(_url.openConnection());
            NodeIterator nodeIterator = parser.elements();
            ArrayList<String> listeBalises = new ArrayList<String>();
            Page page = new Page(link.getUrl());
            while (nodeIterator.hasMoreNodes()) {
                Node node = nodeIterator.nextNode();
                node.accept(new URLExtractingVisitor(link.getDepth() + 1, this, link.getUrl(), site.getStatsPages()));
                node.accept(new URLModifyingVisitor(site.getSourceString()));
                node.accept(new StatsVisitor(page, site.getStatsBalises()));
                node.accept(new EmailVisitor(site));
                listeBalises.add(node.toHtml());
            }
            PageDownloader fd = new PageDownloader(link.getUrl(), site.getTarget() + "/" + site.getName(), site.getSourceString(), listeBalises);
            fd.save(page);
            site.addPage(page);
        } catch (MalformedURLException e) {
            System.out.println("Malformed url " + link.getUrl());
            System.exit(-1);
        } catch (ParserException e) {
            System.out.println("Error while parsing " + link.getUrl());
        } catch (IOException e) {
            System.out.println("Connection failed to " + link.getUrl());
        }
        pagesToBeCaptured.remove(link);
        pagesCaptured.add(link);
    }

    /**
	 * acc�s � la liste des fichiers � capturer
	 * @return filesToBeCaptured fichiers � capturer
	 */
    public LinkList getFilesToBeCaptured() {
        return filesToBeCaptured;
    }

    /**
	 * acc�s � la profondeur maximale
	 * @return maxDepth profondeur
	 */
    public int getMaxDepth() {
        return site.getMaxDepth();
    }

    /**
	 * acc�s � la liste des pages captur�es
	 * @return pagesCaptured pages captur�es
	 */
    public LinkList getPagesCaptured() {
        return pagesCaptured;
    }

    /**
	 * acc�s � la liste des pages � capturer
	 * @return pagesToBeCaptured liste des pages � capturer
	 */
    public LinkList getPagesToBeCaptured() {
        return pagesToBeCaptured;
    }

    /**
	 * acc�s � l'url du site
	 * @return source url du site
	 */
    public String getSource() {
        return site.getSourceString();
    }

    /**
	 * teste si le t�l�chargement est fini
	 * @return renvoie true si le t�l�chargement est fini
	 */
    public boolean isFinished() {
        return finished;
    }

    /**
	 * mise � jour du nombre de fichiers
	 */
    public void majNbFichiers() {
        numberHandler.setNbFichiers(pagesCaptured.size() + pagesToBeCaptured.size() + filesToBeCaptured.size());
    }

    /**
	 * mise � jour du nombre de fichiers t�l�charg�s
	 */
    public void majNbFichiersTelecharges() {
        if (!finished) {
            numberHandler.setNbFichiersTelecharges(pagesCaptured.size() + nbFichiersTelecharges);
        } else {
            numberHandler.setNbFichiersTelecharges(pagesCaptured.size() + pagesToBeCaptured.size() + filesToBeCaptured.size());
        }
    }

    /**
	 * acc�s au gestionnaire de nombre de fichiers
	 * @return gestionnaire de nombre de fichiers
	 */
    public NumbersHandler getNumbersHandler() {
        return numberHandler;
    }

    /**
	 * acc�s � la liste de fichiers css � t�l�charger
	 * @return liste de fichiers css � t�l�chager
	 */
    public LinkList getCssToBeCaptured() {
        return cssList;
    }
}
