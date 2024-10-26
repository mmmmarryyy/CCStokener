package net.sf.entDownloader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import net.sf.entDownloader.core.events.AuthenticationSucceededEvent;
import net.sf.entDownloader.core.events.Broadcaster;
import net.sf.entDownloader.core.events.DirectoryChangedEvent;
import net.sf.entDownloader.core.events.DirectoryChangingEvent;
import net.sf.entDownloader.core.events.DownloadAbortEvent;
import net.sf.entDownloader.core.events.EndDownloadEvent;
import net.sf.entDownloader.core.events.FileAlreadyExistsEvent;
import net.sf.entDownloader.core.events.StartDownloadEvent;
import net.sf.entDownloader.core.exceptions.ENTDirectoryNotFoundException;
import net.sf.entDownloader.core.exceptions.ENTFileNotFoundException;
import net.sf.entDownloader.core.exceptions.ENTInvalidFS_ElementTypeException;
import net.sf.entDownloader.core.exceptions.ENTUnauthenticatedUserException;
import com.ziesemer.utils.pacProxySelector.PacProxySelector;

/**
 * Classe principale de l'application, interface entre les classes externes et
 * l'ENT.<br>
 * <b>Classe singleton</b> : utilisez {@link ENTDownloader#getInstance()
 * getInstance()} pour
 * obtenir l'instance de la classe.
 */
public class ENTDownloader {

    /** L'instance statique */
    private static ENTDownloader instance;

    /** Flag indiquant si l'utilisateur est connecté ou non */
    private boolean isLogin = false;

    /** Liste des dossiers et des fichiers du dossier courant */
    private List<FS_Element> directoryContent = null;

    /** Chemin vers le dossier courant */
    private ENTPath path = null;

    /** Paramètre de l'URL de la page de stockage **/
    private String uP_root;

    /** Paramètre de l'URL de la page de stockage **/
    private String tag;

    /** Identifiant de connexion */
    private String sessionid;

    /** Nom de l'utilisateur */
    private String username = null;

    /** Login */
    private String login = null;

    /** Espace disque utilisé */
    private int usedSpace = -1;

    /** Espace disque total */
    private int capacity = -1;

    /** Instance d'un navigateur utilisé pour la communication avec le serveur */
    private Browser browser;

    /**
	 * Enregistre le fichier PAC utilisé pour la configuration du proxy le
	 * cas échéant. Si aucun proxy n'est utilisé ou si la configuration ne
	 * provient pas d'un fichier PAC, cette variable vaut null.
	 */
    private String proxyFile = null;

    /**
	 * Récupère l'instance unique de la classe ENTDownloader.<br>
	 * Remarque : le constructeur est rendu inaccessible
	 */
    public static ENTDownloader getInstance() {
        if (null == instance) {
            instance = new ENTDownloader();
        }
        return instance;
    }

    private ENTDownloader() {
        browser = new Browser();
    }

    /**
	 * Établit la connexion au serveur de l'ENT.
	 * 
	 * @param login Le nom d'utilisatateur pour la connexion.
	 * @param password Mot de passe de connexion.
	 * @return True en cas de réussite, ou false si l'authentification a échoué.
	 * @throws ParseException Impossible d'obtenir les informations de session.
	 */
    public boolean login(String login, char[] password) throws java.io.IOException, ParseException {
        if (isLogin == true) return true;
        browser.setUrl(CoreConfig.loginURL);
        browser.setFollowRedirects(false);
        String loginPage = browser.getPage();
        if (browser.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            browser.setUrl(browser.getHeaderField("Location"));
            loginPage = browser.getPage();
        }
        List<String> ticket = new ArrayList<String>();
        Misc.preg_match("input type=\"hidden\" name=\"lt\" value=\"([0-9a-zA-Z\\-]+)\" />", loginPage, ticket);
        List<String> execution = new ArrayList<String>();
        Misc.preg_match("input type=\"hidden\" name=\"execution\" value=\"([0-9a-zA-Z]+)\" />", loginPage, execution);
        browser.setMethod(Browser.Method.POST);
        browser.setParam("_eventId", "submit");
        browser.setParam("username", login);
        browser.setParam("password", new String(password));
        browser.setParam("lt", ticket.get(1).toString());
        if (execution.size() > 1) {
            browser.setParam("execution", execution.get(1).toString());
        }
        browser.setFollowRedirects(false);
        loginPage = browser.getPage();
        browser.setMethod(Browser.Method.GET);
        if (Misc.preg_match("<div id=\"erreur\">", loginPage)) return false;
        Broadcaster.fireAuthenticationSucceeded(new AuthenticationSucceededEvent(login));
        browser.setUrl(browser.getHeaderField("Location"));
        browser.clearParam();
        browser.getPage();
        sessionid = browser.getCookieValueByName("JSESSIONID");
        isLogin = true;
        browser.setFollowRedirects(true);
        browser.setUrl(CoreConfig.rootURL);
        browser.clearParam();
        String rootPage = null;
        rootPage = browser.getPage();
        setStockageUrlParams(rootPage);
        this.login = login;
        setUserName(rootPage);
        directoryContent = null;
        path = null;
        return true;
    }

    private void setStockageUrlParams(String pageContent) throws ParseException {
        List<String> matches = new ArrayList<String>(5);
        if (!Misc.preg_match("<a href=\"[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]*?/tag\\.([0-9A-Fa-f]{13,17})\\.[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]*?uP_root=([\\w\\|]+?)&[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]*?\" title=\"Espace de stockage WebDAV\" id=\"chanLink\"><span>Mes Documents</span>", pageContent, matches)) throw new ParseException("Unable to find the URL parameters of the storage's service in this page.", -1);
        tag = matches.get(1);
        uP_root = matches.get(2);
    }

    /**
	 * Obtient et définit les propriétés de l'espace de stockage (espace total
	 * et utilisé)
	 * 
	 * @param pageContent Code HTML d'une page de stockage.
	 * @return true si les propriétés ont été trouvés, false sinon
	 */
    private boolean setStorageProperties(String pageContent) {
        List<String> matches = new ArrayList<String>(4);
        if (!Misc.preg_match("([\\d]+) ?% utilis&eacute;s sur ([\\d]+).0 Mo", pageContent, matches)) return false;
        try {
            capacity = Integer.parseInt(matches.get(2));
            usedSpace = Integer.parseInt(matches.get(1)) * capacity / 100;
        } catch (NumberFormatException e) {
            capacity = usedSpace = -1;
            return false;
        }
        return true;
    }

    /**
	 * Détermine le nom complet de l'utilisateur à partir du code HTML de la
	 * page.
	 * 
	 * @param pageContent Code HTML d'une page de stockage.
	 * @return true si le nom a été trouvé, false sinon
	 */
    private boolean setUserName(String pageContent) {
        List<String> matches = new ArrayList<String>(4);
        if (!Misc.preg_match("&gt;</span> Bienvenue (.*?)</div><div", pageContent, matches)) return false;
        username = matches.get(1);
        return true;
    }

    /**
	 * Change le répertoire courant.
	 * 
	 * @param path Nom du dossier ou directive de parcours. Le dossier . est le
	 *            dossier courant : appeler cette méthode avec ce paramètre ne
	 *            change donc pas le dossier courant
	 *            mais permet de rafraîchir son contenu. Le dossier .. est le
	 *            dossier parent, il permet donc de remonter dans
	 *            l'arborescence. Enfin, le dossier / est la racine
	 *            du service de stockage de l'utilisateur.
	 * @throws ENTDirectoryNotFoundException
	 *             Si le répertoire demandé n'existe pas dans le dossier courant
	 * @throws ENTInvalidFS_ElementTypeException
	 *             Si le nom d'un fichier a été passé en paramètre.
	 * @throws ParseException
	 *             En cas d'erreur d'analyse du contenu du dossier cible.
	 * @throws ENTUnauthenticatedUserException
	 *             Si l'utilisateur n'est pas authentifier lors de l'appel de la
	 *             méthode
	 * @throws IOException
	 *             Si le service est indisponible.
	 */
    public void changeDirectory(String path) throws ENTUnauthenticatedUserException, ENTDirectoryNotFoundException, ENTInvalidFS_ElementTypeException, ParseException, IOException {
        if (path == null) throw new NullPointerException();
        DirectoryChangingEvent changingevent = new DirectoryChangingEvent();
        changingevent.setSource(this);
        DirectoryChangedEvent changedevent = new DirectoryChangedEvent();
        changedevent.setSource(this);
        if (path.isEmpty()) {
            changingevent.setDirectory(path);
            Broadcaster.fireDirectoryChanging(changingevent);
            submitDirectory("");
            changedevent.setDirectory(path);
            Broadcaster.fireDirectoryChanged(changedevent);
            return;
        }
        String splitPath[] = path.split("/");
        if (splitPath.length > 1) {
            ENTPath destination = new ENTPath(this.path);
            destination.goTo(path);
            String absDest = destination.toString(), relDest = this.path.getRelative(destination);
            int absc = destination.getNbRequests(), relc = ENTPath.getNbRequests(relDest);
            if (relc < absc) {
                path = relDest;
            } else {
                path = absDest;
            }
            splitPath = path.split("/");
        }
        changingevent.setDirectory(path);
        Broadcaster.fireDirectoryChanging(changingevent);
        boolean isAbsolute = ENTPath.isAbsolute(path);
        if (isAbsolute) {
            submitDirectory("/");
        }
        for (int i = isAbsolute ? 1 : 0; i < splitPath.length; ++i) {
            if (!splitPath[i].isEmpty() && (i == 0 || !splitPath[i].equals("."))) {
                submitDirectory(splitPath[i]);
            }
        }
        changedevent.setDirectory(path);
        Broadcaster.fireDirectoryChanged(changedevent);
    }

    /**
	 * Descend ou remonte d'un pas dans l'aborescence à partir du dossier
	 * courant.
	 * 
	 * @param name
	 *            Nom du dossier ou directive de parcours. Le dossier . est le
	 *            dossier courant, appeler cette methode avec ce paramètre ne
	 *            change donc pas le dossier courant
	 *            mais permet de rafraîchir son contenu. Le dossier .. est le
	 *            dossier parent, il permet donc de remonter dans
	 *            l'arborescence. Enfin, le dossier / ou ~ est la racine
	 *            du service de stockage de l'utilisateur. Si <code>name</code>
	 *            est vide, le dossier / est chargé.
	 * @throws ENTDirectoryNotFoundException
	 *             Si le répertoire demandé n'existe pas dans le dossier courant
	 * @throws ENTInvalidFS_ElementTypeException
	 *             Si le nom d'un fichier a été passé en paramètre.
	 * @throws ParseException
	 *             {@link ENTDownloader#parsePage(String) Voir la méthode
	 *             parsePage}
	 * @throws ENTUnauthenticatedUserException
	 *             Si l'utilisateur n'est pas authentifier lors de l'appel de la
	 *             méthode
	 * @throws IOException
	 *             Si le service est indisponible.
	 */
    private void submitDirectory(String name) throws ENTDirectoryNotFoundException, ENTInvalidFS_ElementTypeException, ParseException, ENTUnauthenticatedUserException, IOException {
        if (isLogin == false) throw new ENTUnauthenticatedUserException("Non-authenticated user.", ENTUnauthenticatedUserException.UNAUTHENTICATED);
        browser.clearParam();
        if (name.equals("/") || name.equals("~") || name.isEmpty()) {
            if (path == null) {
                path = new ENTPath();
                browser.setUrl(urlBuilder(CoreConfig.stockageURL));
            } else {
                path.clear();
                browser.setUrl(urlBuilder("http://ent.u-clermont1.fr/tag.{tag}.render.userLayoutRootNode.target.{uP_root}.uP?link=0#{uP_root}"));
            }
            name = "/";
            browser.setMethod(Browser.Method.GET);
        } else if (name.equals(".")) {
            browser.setMethod(Browser.Method.GET);
            browser.setUrl(urlBuilder(CoreConfig.refreshDirURL));
        } else if (name.equals("..")) {
            if (path.isRoot()) return;
            browser.setMethod(Browser.Method.GET);
            browser.setUrl(urlBuilder(CoreConfig.directoryBackURL));
        } else {
            int pos = indexOf(name);
            if (pos == -1) throw new ENTDirectoryNotFoundException(name); else if (!directoryContent.get(pos).isDirectory()) throw new ENTInvalidFS_ElementTypeException(name);
            browser.setUrl(urlBuilder(CoreConfig.goIntoDirectoryURL));
            browser.setMethod(Browser.Method.POST);
            browser.setParam("targetDirectory", name);
        }
        browser.setFollowRedirects(false);
        browser.setCookieField("JSESSIONID", sessionid);
        String pageContent = null;
        pageContent = browser.getPage();
        if (browser.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP && browser.getHeaderField("Location").equals(CoreConfig.loginRequestURL)) {
            isLogin = false;
            throw new ENTUnauthenticatedUserException("Session expired, please login again.", ENTUnauthenticatedUserException.SESSION_EXPIRED);
        }
        setStockageUrlParams(pageContent);
        if (capacity < 0) {
            setStorageProperties(pageContent);
        }
        if (pageContent.isEmpty() || Misc.preg_match("<font class=\"uportal-channel-strong\">La ressource sp&eacute;cifi&eacute;e n'existe pas.<br /></font>", pageContent)) throw new ENTDirectoryNotFoundException(name);
        parsePage(pageContent);
        path.goTo(name);
    }

    /**
	 * Télécharge le fichier <i>name</i>.
	 * Le fichier sera enregistré sous le dossier et le nom spécifié dans le
	 * paramètre <i>destination</i>, ou sous le même nom que celui sous lequel
	 * il est stocké sur l'ENT si le nouveau nom n'est pas indiqué dans
	 * <i>destination</i>.<br>
	 * <br>
	 * Exemples pour un fichier "tp13.pdf":<br>
	 * <ul>
	 * <li>Si le chemin de destination est "/home/sasa/tps/tpNumero13.pdf", le
	 * fichier sera stocké sous "/home/sasa/tps/tpNumero13.pdf";</li>
	 * <li>Si le chemin de destination est "/home/sasa/bonjour/", le fichier
	 * sera stocké sous "/home/sasa/bonjour/tp13.pdf";</li>
	 * <li>Si le chemin de destination est "tp13.pdf", le fichier sera stocké
	 * sous System.getProperty("user.dir") + "tp13.pdf";</li>
	 * <li>Si le chemin de destination est vide ou null, le fichier sera stocké
	 * sous System.getProperty("user.dir") + nom utilisé sous l'ENT;</li>
	 * <li>Si le chemin de destination est "~/bonjour.pdf", le fichier sera
	 * stocké sous System.getProperty("user.home") + "bonjour.pdf;</li>
	 * </ul>
	 * 
	 * @param name
	 *            Nom du fichier à télécharger
	 * @param destination
	 *            Chemin de destination du fichier
	 * @throws IOException
	 * @see Misc#tildeToHome(String)
	 * @see Browser#downloadFile(String)
	 * @return <code>True</code> si le téléchargement du fichier s'est terminé
	 *         normalement, <code>false</code> sinon.
	 */
    public boolean getFile(String name, String destination) throws IOException {
        if (isLogin == false) throw new ENTUnauthenticatedUserException("Non-authenticated user.", ENTUnauthenticatedUserException.UNAUTHENTICATED);
        final int pos = indexOf(name);
        if (pos == -1) throw new ENTFileNotFoundException("File not found"); else if (!directoryContent.get(pos).isFile()) throw new ENTInvalidFS_ElementTypeException(name + " isn't a file");
        FS_File file = (FS_File) directoryContent.get(pos);
        Broadcaster.fireStartDownload(new StartDownloadEvent(file));
        if (destination == null || destination.isEmpty()) {
            destination = name;
        } else if (!destination.equals(name)) {
            destination = Misc.tildeToHome(destination);
            if (destination.substring(destination.length() - 1).equals(System.getProperty("file.separator"))) {
                destination += name;
            }
        }
        File fpath = new File(destination).getCanonicalFile();
        if (fpath.exists()) {
            FileAlreadyExistsEvent fileAlreadyExistsEvent = new FileAlreadyExistsEvent(file);
            Broadcaster.fireFileAlreadyExists(fileAlreadyExistsEvent);
            if (fileAlreadyExistsEvent.abortDownload) {
                Broadcaster.fireDownloadAbort(new DownloadAbortEvent(file));
                return false;
            }
        }
        browser.clearParam();
        browser.setUrl(urlBuilder(CoreConfig.downloadFileURL));
        browser.setMethod(Browser.Method.POST);
        browser.setParam("downloadFile", name);
        browser.setFollowRedirects(false);
        browser.setCookieField("JSESSIONID", sessionid);
        browser.downloadFile(destination);
        Broadcaster.fireEndDownload(new EndDownloadEvent(file));
        return true;
    }

    /**
	 * Télécharge le fichier <i>name</i>.
	 * Le fichier sera enregistré dans le dossier local courant (généralement le
	 * dossier de l'application), sous le même nom que celui sous lequel il est
	 * stocké sur l'ENT.
	 * 
	 * @param name
	 *            Le nom du fichier à télécharger.
	 * @throws IOException
	 * @return <code>True</code> si le téléchargement du fichier s'est terminé
	 *         normalement, <code>false</code> sinon.
	 */
    public boolean getFile(String name) throws IOException {
        return getFile(name, name);
    }

    /**
	 * Télécharge tous les fichiers contenus dans le dossier courant et ses sous
	 * dossiers.
	 * Les fichiers et dossiers seront enregistrés sous le dossier
	 * <i>destination</i>, sous le même nom que celui sous lequel ils sont
	 * stockés sur l'ENT.
	 * 
	 * @param destination
	 *            Dossier de destination des fichiers et dossiers téléchargés.
	 *            Si null ou vide, ils seront enregistrés dans le répertoire
	 *            courant.
	 * @throws ENTInvalidFS_ElementTypeException
	 *             Lancée lorsque le paramètre <i>destination</i> désigne un
	 *             fichier existant.
	 * @throws IOException
	 * @see ENTDownloader#getFile(String, String)
	 * @return Le nombre de fichiers téléchargés
	 * @deprecated Remplacé par getAllFiles(String destination, int maxdepth)
	 */
    @Deprecated
    public int getAllFiles(String destination) throws IOException {
        return getAllFiles(destination, -1);
    }

    /**
	 * Télécharge tous les fichiers contenus dans le dossier courant et ses sous
	 * dossiers.
	 * Les fichiers et dossiers seront enregistrés sous le dossier
	 * <i>destination</i>, sous le même nom que celui sous lequel ils sont
	 * stockés sur l'ENT.
	 * 
	 * @param destination
	 *            Dossier de destination des fichiers et dossiers téléchargés.
	 *            Si null ou vide, ils seront enregistrés dans le répertoire
	 *            courant.
	 * @param maxdepth
	 *            Profondeur maximale de téléchargement. 0 (zéro) signifie que
	 *            la méthode ne va télécharger que les fichiers du dossier
	 *            courant, sans descendre dans les sous-dossiers. Une valeur
	 *            négative signifie aucune limite.
	 * @throws ENTInvalidFS_ElementTypeException
	 *             Lancée lorsque le paramètre <i>destination</i> désigne un
	 *             fichier existant.
	 * @throws IOException
	 * @see ENTDownloader#getFile(String, String)
	 * @return Le nombre de fichiers téléchargés
	 */
    public int getAllFiles(String destination, int maxdepth) throws IOException {
        int i = 0;
        if (directoryContent == null) throw new IllegalStateException("Directory content hasn't been initialized");
        if (destination == null) {
            destination = "";
        } else if (!destination.isEmpty()) {
            if (!destination.substring(destination.length() - 1).equals(System.getProperty("file.separator"))) {
                destination += System.getProperty("file.separator");
            }
            destination = Misc.tildeToHome(destination);
            File dest = new File(destination);
            if (dest.isFile()) throw new ENTInvalidFS_ElementTypeException("Unable to create the required directory : a file with that name exists.");
        }
        List<FS_Element> directoryContentcp = new ArrayList<FS_Element>(directoryContent);
        for (FS_Element e : directoryContentcp) if (e.isFile()) {
            if (getFile(e.getName(), destination)) {
                ++i;
            }
        } else if (maxdepth != 0) {
            try {
                submitDirectory(e.getName());
            } catch (ParseException e1) {
                try {
                    submitDirectory(e.getName());
                } catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
            i += getAllFiles(destination + e.getName(), maxdepth - 1);
            try {
                submitDirectory("..");
            } catch (ParseException e1) {
                try {
                    submitDirectory("..");
                } catch (ParseException e2) {
                    e2.printStackTrace();
                }
            }
        }
        return i;
    }

    /**
	 * Analyse le contenu de la page passé en paramètre afin de déterminer les
	 * dossiers et fichiers contenus dans le dossier courant.
	 * 
	 * @param pageContent
	 *            Le contenu de la page à analyser
	 * @throws ParseException
	 *             Si l'analyse échoue
	 */
    private void parsePage(String pageContent) throws ParseException {
        List<List<String>> matches = new ArrayList<List<String>>();
        if (directoryContent == null) {
            directoryContent = new ArrayList<FS_Element>(50);
        } else {
            directoryContent.clear();
        }
        Misc.preg_match_all("&nbsp;<a href=\"javascript:submit(File|Directory)\\('.+?'\\);\"\\s+class.*?nnel\">(.*?)</a></td><td class=\"uportal-crumbtrail\" align=\"right\">\\s+?&nbsp;([0-9][0-9]*\\.[0-9][0-9]? [MKGo]{1,2})?\\s*?</td><td class=\"uportal-crumbtrail\" align=\"right\">\\s+?&nbsp;([0-9]{2})-([0-9]{2})-([0-9]{4})&nbsp;([0-9]{2}):([0-9]{2})", pageContent, matches, Misc.PREG_ORDER.SET_ORDER);
        for (List<String> fileInfos : matches) {
            FS_Element file = null;
            if (fileInfos.get(1).equals("Directory")) {
                file = new FS_Directory(HTMLEntities.unhtmlentities(fileInfos.get(2)), new GregorianCalendar(Integer.parseInt(fileInfos.get(6)), Integer.parseInt(fileInfos.get(5)) - 1, Integer.parseInt(fileInfos.get(4)), Integer.parseInt(fileInfos.get(7)), Integer.parseInt(fileInfos.get(8))));
            } else if (fileInfos.get(1).equals("File")) {
                file = new FS_File(HTMLEntities.unhtmlentities(fileInfos.get(2)), new GregorianCalendar(Integer.parseInt(fileInfos.get(6)), Integer.parseInt(fileInfos.get(5)) - 1, Integer.parseInt(fileInfos.get(4)), Integer.parseInt(fileInfos.get(7)), Integer.parseInt(fileInfos.get(8))), fileInfos.get(3));
            }
            if (file == null) throw new ParseException("Error while parsing page content : unable to determine if \"" + fileInfos.get(2) + "\" is a file or a directory.", -1);
            directoryContent.add(file);
        }
    }

    /**
	 * Construit l'url demandé en remplaçant les champs {...} par les valeurs
	 * correspondantes
	 * 
	 * @param url
	 *            URL à construire
	 * @return L'url passé en paramètre, après avoir remplacer les champs {...}.
	 */
    private String urlBuilder(String url) {
        return url.replaceAll("\\{tag\\}", tag).replaceAll("\\{uP_root\\}", uP_root);
    }

    /**
	 * Retourne le contenu du répertoire courant.
	 */
    public List<FS_Element> getDirectoryContent() {
        if (directoryContent == null) throw new IllegalStateException("Directory content hasn't been initialized");
        return directoryContent;
    }

    /**
	 * Obtient le nom du répertoire courant.
	 */
    public String getDirectoryName() {
        return path.getDirectoryName();
    }

    /**
	 * Retourne le nombre de dossiers dans le dossier courant.
	 */
    public int getNbDossiers() {
        int i = 0;
        for (FS_Element e : directoryContent) {
            if (e.isDirectory()) {
                ++i;
            }
        }
        return i;
    }

    /**
	 * Retourne le nombre de fichiers dans le dossier courant.
	 */
    public int getNbFiles() {
        int i = 0;
        for (FS_Element e : directoryContent) {
            if (e.isFile()) {
                ++i;
            }
        }
        return i;
    }

    /**
	 * Retourne la taille totale des fichiers dans le dossier courant en octets.
	 */
    public long getFilesSize() {
        long s = 0;
        for (FS_Element e : directoryContent) {
            if (e.isFile()) {
                s += ((FS_File) e).getSize();
            }
        }
        return s;
    }

    /**
	 * Obtient le chemin absolu permettant d'atteindre le répertoire courant.
	 */
    public String getDirectoryPath() {
        if (path == null) return null;
        return path.toString();
    }

    /**
	 * Retourne le nom complet de l'utilisateur, ou null si ce dernier est
	 * inconnu.
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * Retourne l'espace disque utilisé sur le service de stockage en Mo, ou -1
	 * si ce dernier est inconnu
	 */
    public int getUsedSpace() {
        return usedSpace;
    }

    /**
	 * Retourne l'espace disque total sur le service de stockage en Mo, ou -1 si
	 * ce dernier est inconnu.
	 */
    public int getCapacity() {
        return capacity;
    }

    /**
	 * Retourne le login utilisé pour la connexion, ou null si ce dernier est
	 * inconnu.
	 */
    public String getLogin() {
        return login;
    }

    /**
	 * Retourne l'index de la première occurrence de l'élément spécifié dans la
	 * liste {@link net.sf.entDownloader.core.ENTDownloader#directoryContent
	 * directoryContent}, ou -1 si la liste ne contient pas cet élément. Plus
	 * formellement, retourne le plus petit index i tel que (o==null ?
	 * get(i)==null : get(i).equals(o)), ou -1 si cet index n'existe pas.
	 * 
	 * @param o L'élément à rechercher
	 * @return L'index de la première occurrence de l'élément spécifié dans la
	 *         liste
	 *         {@link net.sf.entDownloader.core.ENTDownloader#directoryContent
	 *         directoryContent}, ou -1 si la liste ne contient pas cet élément.
	 * @throws IllegalStateException Si le répertoire courant n'a pas été
	 *             chargé.
	 */
    private int indexOf(Object o) throws IllegalStateException {
        if (directoryContent == null) throw new IllegalStateException("Directory content hasn't been initialized");
        int i = 0;
        for (Object e : directoryContent) {
            if (e.equals(o)) return i;
            ++i;
        }
        return -1;
    }

    /**
	 * Installe un proxy HTTP à utiliser pour la connexion à l'ENT.
	 * 
	 * @param host Le nom d'hôte ou l'adresse du proxy.
	 * @param port Le port du proxy.
	 */
    public void setProxy(String host, int port) {
        browser.setHttpProxy(host, port);
        proxyFile = null;
    }

    /**
	 * Installe un proxy HTTP à utiliser pour la connexion à l'ENT.
	 * 
	 * @param proxy L'instance de java.net.Proxy à utiliser.
	 * @see java.net.Proxy
	 */
    public void setProxy(Proxy proxy) {
        browser.setHttpProxy(proxy);
        proxyFile = null;
    }

    /**
	 * Retourne le proxy HTTP utilisé pour la connexion à l'ENT.
	 * 
	 * @return Le proxy HTTP utilisé pour la connexion à l'ENT.
	 */
    public Proxy getProxy() {
        return browser.getProxy();
    }

    /**
	 * Retourne le fichier PAC utilisé pour la configuration du proxy le
	 * cas échéant.
	 * 
	 * Si aucun proxy n'est utilisé ou si la configuration ne
	 * provient pas d'un fichier PAC, cette méthode retourne <code>null</code>.
	 * 
	 * @return Le fichier PAC utilisé pour la configuration du proxy.
	 */
    public String getProxyFile() {
        return proxyFile;
    }

    /**
	 * Supprime la configuration de proxy précédemment installé.
	 */
    public void removeProxy() {
        browser.removeHttpProxy();
        proxyFile = null;
    }

    /**
	 * Installe un proxy HTTP à utiliser pour la connexion à l'ENT en utilisant
	 * un fichier PAC (Proxy auto-configuration).
	 * 
	 * @param pacFile Emplacement du fichier PAC
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @see <a href="http://en.wikipedia.org/wiki/Proxy_auto-config"> PAC File
	 *      on Wikipedia</a>
	 */
    public void setProxy(String pacFile) throws Exception {
        setProxy((Proxy) null);
        File localFile = new File(pacFile);
        URL url;
        if (localFile.canRead()) {
            url = localFile.toURI().toURL();
        } else {
            url = new URI(pacFile).toURL();
        }
        URLConnection conn = url.openConnection();
        PacProxySelector a = new PacProxySelector(new BufferedReader(new InputStreamReader(conn.getInputStream())));
        Proxy proxy = a.select(new URI(CoreConfig.rootURL)).get(0);
        setProxy(proxy);
        proxyFile = pacFile;
    }
}
