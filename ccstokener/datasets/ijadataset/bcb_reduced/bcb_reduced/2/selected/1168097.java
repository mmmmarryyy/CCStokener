package gmusic.ma.services.impl;

import gmusic.bo.ArtistBean;
import gmusic.bo.ReleaseBean;
import gmusic.bo.TrackBean;
import gmusic.dao.impl.GMusicDAOImpl;
import gmusic.ma.bo.ArtistMABean;
import gmusic.ma.bo.DemandeChargement;
import gmusic.ma.bo.ReleaseMABean;
import gmusic.ma.bo.TrackMABean;
import gmusic.ma.dao.impl.MetalArchivesDAOImpl;
import gmusic.ma.vo.ReleaseMAVO;
import gmusic.services.impl.GMusicServicesImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import algutil.fichier.ActionsFichiers;
import algutil.fichier.exception.CopieException;
import algutil.internet.InternetUtil;
import algutil.internet.Tag;
import algutil.internet.exception.NoValueException;
import algutil.parser.TDTag;
import algutil.parser.TRTag;
import algutil.parser.TableTag;
import dao.MultiColumnSort;
import dao.QueryFilter;
import dao.SingleColumnSort;
import dao.Sort;

public class MetalArchivesServicesImpl {

    private static final Logger log = Logger.getLogger(MetalArchivesServicesImpl.class);

    private static final String PREFFIXE_URL_MA_RELEASES = "http://www.metal-archives.com/band/discography/id/";

    private static MetalArchivesServicesImpl gs = null;

    private static boolean chargementInfoActif = true;

    private MetalArchivesServicesImpl() {
    }

    public static MetalArchivesServicesImpl getInstance() {
        if (gs == null) {
            gs = new MetalArchivesServicesImpl();
        }
        return gs;
    }

    public static boolean isChargementInfoActif() {
        return chargementInfoActif;
    }

    public static void desactiverChargementInfo() {
        MetalArchivesServicesImpl.chargementInfoActif = false;
    }

    public static void activerChargementInfo() {
        MetalArchivesServicesImpl.chargementInfoActif = true;
    }

    /** @deprecated */
    public List<ArtistMABean> rechercherArtistFromWebSite(String groupe) throws Exception {
        groupe = groupe.toLowerCase();
        log.info("Recherche : " + groupe);
        List<String> lines = InternetUtil.getHTMLSourcePage("http://www.metal-archives.com/search.php?type=band&string=" + groupe);
        long maid = -1;
        boolean unSeulArtistTrouve = false;
        for (int i = 0; i < lines.size(); i++) {
            String ligne = lines.get(i);
            if (ligne.indexOf("<script language='JavaScript'> location.href = 'band.php?id=") != -1) {
                unSeulArtistTrouve = true;
                maid = Long.parseLong(ligne.substring(ligne.indexOf("band.php?id=") + 12, ligne.indexOf("';</script>")));
                log.debug("Un seul artiste trouve maid = " + maid);
                break;
            }
        }
        List<ArtistMABean> artists = null;
        if (unSeulArtistTrouve) {
            artists = new ArrayList<ArtistMABean>();
            artists.add(getArtistFromMAWebSite(maid));
        } else {
            artists = parseHTMLForArtistsSearchPage(lines);
        }
        return artists;
    }

    /** @deprecated */
    private List<ArtistMABean> parseHTMLForArtistsSearchPage(List<String> lines) {
        List<ArtistMABean> artists = new ArrayList<ArtistMABean>();
        String ligne = null;
        for (int i = 0; i < lines.size(); i++) {
            ligne = lines.get(i);
            if (ligne.indexOf("<a href='band.php?id=") != -1) {
                log.debug(ligne);
                log.debug(ligne.indexOf("<table"));
                log.debug(ligne.indexOf("</table>"));
                log.debug(ligne.indexOf("</table>", ligne.indexOf("<table")));
                ligne = ligne.substring(ligne.indexOf("<table"), ligne.indexOf("</table>", ligne.indexOf("<table")) + 8);
                TableTag tt = new TableTag(ligne);
                List<TRTag> trs = tt.getTrs();
                for (int j = 0; j < trs.size(); j++) {
                    ArtistMABean artist = new ArtistMABean();
                    log.debug(trs.get(j).toString());
                    trs.get(j).extractTD();
                    List<TDTag> tds = trs.get(j).getTds();
                    log.debug(tds.get(0).toString());
                    log.debug(tds.get(1).toString());
                    String maidS = tds.get(1).getText().substring(tds.get(1).getText().indexOf("id=") + 3, tds.get(1).getText().indexOf("'", tds.get(1).getText().indexOf("id=")));
                    log.debug("MAID=" + maidS);
                    artist.setMaid(Long.parseLong(maidS));
                    String artistName = tds.get(1).getText().substring(tds.get(1).getText().indexOf("'>") + 2, tds.get(1).getText().indexOf("</a>")).trim();
                    log.debug("NAME=" + artistName);
                    artist.setNom(artistName);
                    log.debug(tds.get(2).toString());
                    if (tds.get(2).getText().trim().length() > 0) {
                        String alternativeName = tds.get(2).getText().substring(tds.get(2).getText().indexOf("</i>") + 4).replaceAll("<strong>", "").replaceAll("</strong>", "").trim();
                        log.debug("ALT NAME=" + alternativeName);
                        artist.setAlternativeName(alternativeName);
                    }
                    artists.add(artist);
                }
                break;
            }
        }
        return artists;
    }

    /** @deprecated */
    public ArtistMABean getArtistFromMAWebSite(long maid) throws Exception {
        List<String> lines = InternetUtil.getHTMLSourcePage("http://www.metal-archives.com/band.php?id=" + maid);
        return parseHTMLForOneArtistPage(lines, maid);
    }

    /** @deprecated */
    private ArtistMABean parseHTMLForOneArtistPage(List<String> lines, long maid) throws SQLException {
        ArtistMABean ab = new ArtistMABean(maid);
        boolean nomArtisteTrouve = false;
        boolean logoArtisteTrouve = false;
        boolean paysArtisteTrouve = false;
        boolean genreTrouve = false;
        boolean lyricalThemeTrouve = false;
        boolean maidTrouve = maid == -1 ? false : true;
        String ligne;
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < lines.size(); i++) {
            ligne = lines.get(i);
            log.debug("==> " + ligne);
            if (!maidTrouve && ligne.indexOf(">Add new data</a>") != -1) {
                maid = Long.parseLong(ligne.substring(ligne.indexOf("band_id=") + 8, ligne.indexOf("\">Add new data")));
                log.debug("Maid : " + maid);
                ab.setMaid(maid);
                maidTrouve = true;
            } else if (!nomArtisteTrouve && ligne.indexOf("Encyclopaedia Metallum - ") != -1) {
                String nomArtiste = ligne.substring(ligne.indexOf("-") + 2, ligne.indexOf("<", 3));
                log.debug("Nom artiste : " + nomArtiste);
                ab.setNom(nomArtiste);
                nomArtisteTrouve = true;
            } else if (!logoArtisteTrouve && ligne.indexOf("<img") != -1) {
                String url = ligne.substring(ligne.indexOf("src=") + 5, ligne.indexOf("\"", 10));
                if (!url.startsWith("http")) {
                    url = "http://www.metal-archives.com" + url;
                }
                log.debug("URL logo : " + url);
                ab.setURLLogo(url);
                logoArtisteTrouve = true;
            } else if (!paysArtisteTrouve && ligne.indexOf("browseC.php") != -1) {
                String pays = ligne.substring(ligne.indexOf("=", 13) + 1, ligne.indexOf("\"", 15));
                log.debug("Pays id : " + pays);
                ab.setPaysMaid(Integer.parseInt(pays));
                paysArtisteTrouve = true;
            } else if (ligne.indexOf("<td>Active</td>") != -1) {
                ab.setStatut(ArtistBean.ACTIVE_STATUS);
            } else if (ligne.indexOf("<td>On hold</td>") != -1) {
                ab.setStatut(ArtistBean.ON_HOLD_STATUS);
            } else if (ligne.indexOf("<td>Split-up</td>") != -1) {
                ab.setStatut(ArtistBean.SPLIT_UP_STATUS);
            } else if (ligne.indexOf("<td>Split-up</td>") != -1) {
                ab.setStatut(ArtistBean.SPLIT_UP_STATUS);
            } else if (ligne.indexOf("<td>Changed name</td>") != -1) {
                ab.setStatut(ArtistBean.CHANGED_NAME_STATUS);
            } else if (ligne.indexOf(">Discography<") != -1) {
                while (ligne.indexOf(">Links<") == -1 && ligne.indexOf("Back to the Encyclopaedia Metallum") == -1) {
                    log.debug("==> " + ligne);
                    sb.append(ligne.trim());
                    ligne = lines.get(++i);
                }
                break;
            } else if (!genreTrouve && ligne.indexOf(">Genre(s)</td>") != -1) {
                i = i + 3;
                ligne = lines.get(i);
                if (ligne.indexOf("<td colspan=\"4\">") != -1) {
                    String genre = ligne.substring(ligne.indexOf("<td colspan") + 16, ligne.indexOf("<", 10));
                    log.debug("GENRE : " + genre);
                    ab.setGenre(genre);
                }
                genreTrouve = true;
            } else if (!lyricalThemeTrouve && ligne.indexOf(">Lyrical theme(s)</td>") != -1) {
                i = i + 3;
                ligne = lines.get(i);
                if (ligne.indexOf("<td colspan=\"4\">") != -1) {
                    String lyricalTheme = ligne.substring(ligne.indexOf("<td colspan") + 16, ligne.indexOf("<", 10));
                    log.debug("LYRICAL THEME : " + lyricalTheme);
                    ab.setLyricalTheme(lyricalTheme);
                }
                lyricalThemeTrouve = true;
            }
        }
        ligne = sb.toString();
        ligne = ligne.replaceAll("\t", "");
        String[] lignes = ligne.split(">");
        for (int i = 0; i < lignes.length; i++) {
            if (lignes[i].startsWith("<a")) {
                log.debug("Ligne 1 : " + lignes[i]);
                String rmaidS = lignes[i].substring(lignes[i].indexOf("=", lignes[i].indexOf("href") + 7) + 1, lignes[i].length() - 1);
                log.debug("rmaid : " + rmaidS);
                int rmaid = Integer.parseInt(rmaidS);
                log.debug("Ligne 2 : " + lignes[++i]);
                String nom = lignes[i].substring(0, lignes[i].indexOf("<"));
                log.debug("Nom : " + nom);
                i = i + 3;
                log.debug("Ligne 3 : " + lignes[i]);
                String type = lignes[i].substring(0, lignes[i].indexOf(","));
                log.debug("Type : " + type);
                String anneeS = lignes[i].substring(lignes[i].indexOf(",") + 2, lignes[i].indexOf("<"));
                log.debug("Annee : " + anneeS);
                int annee = Integer.parseInt(anneeS);
                ab.addRelease(new ReleaseMABean(rmaid, ab.getMaid(), nom, MetalArchivesDAOImpl.getInstance().getMAReleaseTypeId(type), annee));
            }
        }
        if (ab.getMaid() == -1) {
            ab = null;
        }
        return ab;
    }

    /** @deprecated */
    public void getTracksFromMAWebSite(ReleaseMABean rb) throws Exception {
        URL fileURL = new URL("http://www.metal-archives.com/release.php?id=" + rb.getMaid());
        URLConnection urlConnection = fileURL.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        String ligne;
        int discNum = 1;
        boolean tracksTrouve = false;
        while ((ligne = br.readLine()) != null) {
            log.debug("==> " + ligne);
            if (!tracksTrouve && ligne.indexOf("<tr><td>1.<") != -1) {
                String[] lignes = ligne.split(">");
                for (int i = 0; i < lignes.length; i++) {
                    log.debug("--> " + lignes[i]);
                    if (lignes[i].indexOf("Disc ") != -1) {
                        String discNumS = lignes[i].substring(lignes[i].indexOf("Disc ") + 5, lignes[i].indexOf("<"));
                        log.debug("Disque Numero : " + discNumS);
                        discNum = Integer.parseInt(discNumS);
                    } else if (lignes[i].indexOf(".</td") != -1) {
                        String numS = lignes[i].substring(0, lignes[i].indexOf("."));
                        log.debug("Num : " + numS);
                        int num = Integer.parseInt(numS);
                        log.debug("--> " + lignes[++i]);
                        log.debug("--> " + lignes[++i]);
                        String titre = lignes[i].substring(0, lignes[i].indexOf("<"));
                        log.debug("Titre : " + titre);
                        log.debug("--> " + lignes[++i]);
                        log.debug("--> " + lignes[++i]);
                        int duree = -1;
                        if (lignes[i].indexOf(":") != -1) {
                            String dureeS = lignes[i].substring(0, lignes[i].indexOf("<"));
                            log.debug("Duree : " + dureeS);
                            duree = Integer.parseInt(dureeS.substring(0, dureeS.indexOf(":"))) * 60;
                            duree += Integer.parseInt(dureeS.substring(dureeS.indexOf(":") + 1));
                        }
                        log.debug("--> " + lignes[++i]);
                        log.debug("--> " + lignes[++i]);
                        int maid = -1;
                        if (lignes[i].indexOf("openLyrics") != -1) {
                            String maidS = lignes[i].substring(lignes[i].indexOf("(") + 1, lignes[i].indexOf(")"));
                            log.debug("Maid : " + maidS);
                            maid = Integer.parseInt(maidS);
                        } else {
                            log.debug("--> " + lignes[++i]);
                            log.debug("--> " + lignes[++i]);
                        }
                        rb.addTrack(new TrackMABean(maid, rb.getMaid(), discNum, titre, duree, num));
                    }
                }
                tracksTrouve = true;
            } else if (ligne.indexOf("<a href=\"/images") != -1) {
                String URLCover = "http://www.metal-archives.com" + ligne.substring(ligne.indexOf("/images"), ligne.indexOf("\"", 15));
                log.debug("Cover = " + URLCover);
                rb.setURLCover(URLCover);
                break;
            }
        }
        if (rb.getURLCover() != null) {
            saveCoverToLocalhost(rb);
        }
        br.close();
        httpStream.close();
    }

    /** @deprecated */
    public void getLyricsFromMAWebSite(TrackMABean tb) throws Exception {
        URL fileURL = new URL("http://www.metal-archives.com/viewlyrics.php?id=" + tb.getMaid());
        URLConnection urlConnection = fileURL.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        String ligne;
        boolean chargerLyrics = false;
        StringBuffer sb = new StringBuffer("");
        String lyrics = null;
        while ((ligne = br.readLine()) != null) {
            log.debug("==> " + ligne);
            if (chargerLyrics && ligne.indexOf("<center>") != -1) {
                break;
            }
            if (chargerLyrics) {
                sb.append(ligne.trim());
            }
            if (!chargerLyrics && ligne.indexOf("<center>") != -1) {
                chargerLyrics = true;
            }
        }
        lyrics = sb.toString();
        lyrics = lyrics.replaceAll("<br>", "\n").trim();
        log.debug("Parole : " + lyrics);
        tb.setLyrics(lyrics);
        br.close();
        httpStream.close();
    }

    public ReleaseBean transformMABean2GMBean(ReleaseMABean marb) throws SQLException {
        ReleaseBean ab = null;
        if (MetalArchivesDAOImpl.getInstance().releaseMAExists(marb.getMaid())) {
            ab = new ReleaseBean();
            GMusicDAOImpl gc = GMusicDAOImpl.getInstance();
            ArtistBean gb = gc.getGroupeSearchWithMAId(marb.getArtistMaId());
            if (gb != null) {
                ab.setArtistId(gb.getGmid());
            } else {
                log.error("Le groupe n'existe pas dans GM maid : " + marb.getArtistMaId());
                return null;
            }
        } else {
            log.info("La release n'existe pas dans MA : " + marb);
        }
        return ab;
    }

    /** @deprecated */
    public TrackBean transformMABean2GMBean(TrackMABean tb) throws SQLException {
        TrackBean cb = new TrackBean();
        cb.setDisc(tb.getDisc());
        cb.setNumero(tb.getNumero());
        cb.setNom(tb.getNom());
        cb.setLyrics(tb.getLyrics());
        cb.setDuree(tb.getDuree());
        return cb;
    }

    public boolean saveCoverToLocalhost(ReleaseMABean rb) throws SQLException, MalformedURLException, CopieException, IOException {
        if (rb.getURLCoverRelative() == null) {
            return false;
        }
        File path = GMusicServicesImpl.getInstance().getCoverPath();
        if (path == null) {
            return false;
        }
        File coverFile = new File(path.getPath() + File.separator + "rmaid" + rb.getMaid() + ".jpg");
        if (!coverFile.exists()) {
            ActionsFichiers.copierFichier(new URL(rb.getURLCoverAbsolue()), coverFile);
        }
        return true;
    }

    public boolean persistArtist(ArtistMABean ab) throws SQLException {
        boolean retour = MetalArchivesDAOImpl.getInstance().ajouter(ab);
        if (retour) {
            MetalArchivesDAOImpl.getInstance().ajoutHistorique(ab);
        }
        return retour;
    }

    public void persist(List<TrackMABean> l) throws Exception {
        for (int i = 0; i < l.size(); i++) {
            persist(l.get(i));
        }
    }

    public boolean persist(TrackMABean tb) throws Exception {
        boolean isUpserted = false;
        TrackMABean dbTrack = getTrack(tb.getReleaseMaid(), tb.getDisc(), tb.getNumero());
        if (dbTrack == null) {
            isUpserted = true;
            log.info("SER add track " + tb);
            MetalArchivesDAOImpl.getInstance().add(tb);
        } else {
            if (tb.getMaid() != dbTrack.getMaid()) {
                isUpserted = true;
            } else if (tb.getDuree() != dbTrack.getDuree()) {
                isUpserted = true;
            }
            if (isUpserted) {
                log.info("SER update track " + tb);
                MetalArchivesDAOImpl.getInstance().update(tb);
            }
        }
        return isUpserted;
    }

    public boolean persist(ReleaseMABean rb) throws Exception {
        boolean isUpserted = false;
        ReleaseMABean dbRelease = getRelease(rb.getMaid());
        if (dbRelease == null) {
            isUpserted = true;
            log.info("SER add release " + rb);
            MetalArchivesDAOImpl.getInstance().add(rb);
        } else {
            StringBuffer sb = new StringBuffer("");
            if (rb.getAnnee() != dbRelease.getAnnee()) {
                isUpserted = true;
                sb.append("annee;");
            } else if (rb.getTypeId() != dbRelease.getTypeId()) {
                isUpserted = true;
                sb.append("type;");
            } else if (!(rb.getReview() == null ? "" : rb.getReview()).equalsIgnoreCase((dbRelease.getReview() == null ? "" : dbRelease.getReview()))) {
                isUpserted = true;
                sb.append("review;");
            } else if (!rb.getUrlRelative().equalsIgnoreCase(dbRelease.getUrlRelative())) {
                isUpserted = true;
                sb.append("url;");
            }
            if (isUpserted) {
                log.info("SER update release " + rb + " [modif=" + sb.toString() + "]");
                MetalArchivesDAOImpl.getInstance().update(rb);
                saveCoverToLocalhost(rb);
            }
        }
        return isUpserted;
    }

    /** @deprecated */
    public boolean persistRelease(ReleaseMABean rb) throws SQLException {
        return MetalArchivesDAOImpl.getInstance().ajouter(rb);
    }

    /** @deprecated */
    public boolean persistTrack(TrackMABean tb) throws SQLException {
        return MetalArchivesDAOImpl.getInstance().ajouter(tb);
    }

    public boolean persistArtistAndAllReleasesAndTracks(ArtistMABean ab) throws SQLException {
        persistArtist(ab);
        for (int i = 0; i < ab.getReleasesMA().size(); i++) {
            ReleaseMABean rb = ab.getReleasesMA().get(i);
            boolean releaseAjoutee = MetalArchivesDAOImpl.getInstance().add(rb);
            for (int j = 0; j < rb.getTracksMA().size(); j++) {
                TrackMABean tb = rb.getTracksMA().get(j);
                MetalArchivesDAOImpl.getInstance().ajouter(tb);
            }
            if (releaseAjoutee) {
                MetalArchivesDAOImpl.getInstance().ajoutHistorique(rb);
            }
        }
        return true;
    }

    public boolean persistReleaseAndAllTracks(ReleaseMABean rb) throws SQLException {
        boolean releaseAjoutee = MetalArchivesDAOImpl.getInstance().add(rb);
        for (int j = 0; j < rb.getTracksMA().size(); j++) {
            MetalArchivesDAOImpl.getInstance().ajouter(rb.getTracksMA().get(j));
        }
        if (releaseAjoutee) {
            MetalArchivesDAOImpl.getInstance().ajoutHistorique(rb);
        }
        MetalArchivesDAOImpl.getInstance().majStatDemandeChargement(rb);
        return true;
    }

    public ArtistMABean getAllArtistDataFromMAWebSite(long maid) throws Exception {
        return getAllArtistDataFromMAWebSite(maid, false);
    }

    public ArtistMABean getAllArtistDataFromMAWebSiteAndSave(long maid) throws Exception {
        return getAllArtistDataFromMAWebSite(maid, true);
    }

    public void preChargement(DemandeChargement dem) throws Exception {
        if (chargementInfoActif) {
            ArtistMABean ab = null;
            ab = getArtistFromMAWebSite(dem.getAmaid());
            persistArtist(ab);
            dem.setArtist(ab);
            MetalArchivesDAOImpl.getInstance().alimenterNbReleasesToLoad(dem);
        }
    }

    @SuppressWarnings("static-access")
    private ArtistMABean getAllArtistDataFromMAWebSite(long maid, boolean save) throws Exception {
        ArtistMABean ab = null;
        if (chargementInfoActif) {
            ab = getArtistFromMAWebSite(maid);
            if (save) {
                persistArtist(ab);
            }
        }
        if (chargementInfoActif) {
            Thread.currentThread().sleep(5000);
        }
        if (chargementInfoActif) {
            getAllReleasesDataFromMAWebSite(ab, save);
        }
        return ab;
    }

    @SuppressWarnings("static-access")
    private void getAllReleasesDataFromMAWebSite(ArtistMABean ab, boolean save) throws Exception {
        boolean loadLyrics = GMusicServicesImpl.getInstance().getOption("LOAD_LYRICS_FROM_MA_WS").getBooleanValue();
        for (int i = 0; i < ab.getReleasesMA().size() && chargementInfoActif; i++) {
            ReleaseMABean rb = ab.getReleasesMA().get(i);
            if (!MetalArchivesDAOImpl.getInstance().releaseMAExists(rb.getMaid())) {
                getTracksFromMAWebSite(rb);
                for (int j = 0; j < rb.getTracksMA().size() && chargementInfoActif; j++) {
                    TrackMABean tr = rb.getTracksMA().get(j);
                    if (tr.getMaid() != -1 && chargementInfoActif && loadLyrics) {
                        Thread.currentThread().sleep(5000);
                        getLyricsFromMAWebSite(tr);
                    }
                }
                if (save && chargementInfoActif) {
                    persistReleaseAndAllTracks(rb);
                }
                if (chargementInfoActif) {
                    Thread.currentThread().sleep(5000);
                }
            }
        }
    }

    public List<TrackMABean> getTracksFromMAWebSite(long rmaid, String url) throws Exception {
        List<String> lines = null;
        lines = InternetUtil.getHTMLSourcePage(url);
        String line;
        Tag t;
        TrackMABean track = null;
        String urlCover = null;
        int discNumber = 1;
        List<TrackMABean> tracks = new ArrayList<TrackMABean>();
        for (int i = 0; i < lines.size(); i++) {
            line = lines.get(i).trim();
            if (line.indexOf("id=\"cover\"") != -1) {
                log.debug(line);
                t = new Tag(line);
                urlCover = t.getAttribute("href");
                if (urlCover.indexOf("?") != -1) {
                    urlCover = urlCover.substring(0, urlCover.indexOf("?"));
                }
                log.debug("Release URL cover : " + urlCover);
            }
            if (line.indexOf("colspan=\"4\">Disc ") != -1) {
                t = new Tag(line);
                log.debug("Disc Number : " + t.getValues());
                discNumber = Integer.parseInt(t.getValues().replace("Disc", "").trim());
            }
            if (line.indexOf("<td width=\"20\"><a name=") != -1 && line.indexOf(".</td>") != -1) {
                track = new TrackMABean();
                track.setReleaseMaid(rmaid);
                track.setDisc(discNumber);
                track.setURLCover(urlCover);
                t = new Tag(new Tag(line).getValues());
                int num = Integer.parseInt(t.getSource().substring(t.getSource().lastIndexOf(">") + 1).replace(".", "").trim());
                log.debug("Track Number : " + num);
                track.setNumero(num);
                log.debug("tmaid : " + t.getAttribute("name"));
                track.setMaid(Integer.parseInt(t.getAttribute("name")));
                i++;
                line = lines.get(++i).replace("</td>", "").trim();
                log.debug("Name : " + line);
                track.setNom(line);
                line = lines.get(++i).trim();
                t = new Tag(line);
                int duree = -1;
                try {
                    String dureeS = t.getValues();
                    duree = Integer.parseInt(dureeS.substring(0, dureeS.indexOf(":"))) * 60;
                    duree += Integer.parseInt(dureeS.substring(dureeS.indexOf(":") + 1));
                    log.debug("Duree : " + dureeS + " (" + duree + "s)");
                    track.setDuree(duree);
                } catch (Exception e) {
                }
                tracks.add(track);
            }
        }
        return tracks;
    }

    public List<ReleaseMABean> getReleasesFromMAWebSite(long amaid) throws Exception {
        List<String> lines = null;
        lines = InternetUtil.getHTMLSourcePage(PREFFIXE_URL_MA_RELEASES + amaid + "/tab/all");
        String line;
        Tag t;
        ReleaseMABean r = null;
        List<ReleaseMABean> releases = new ArrayList<ReleaseMABean>();
        for (int i = 0; i < lines.size(); i++) {
            line = lines.get(i).trim();
            if (line.indexOf("www.metal-archives.com/albums") != -1) {
                r = new ReleaseMABean();
                r.setArtistMaId(amaid);
                line = line.replaceAll("<td>", "").replaceAll("</td>", "");
                t = new Tag(line);
                log.debug("Release Name : " + t.getValues());
                r.setNom(t.getValues());
                log.debug("Release URL  : " + t.getAttribute("href"));
                r.setUrl(t.getAttribute("href"));
                log.debug("rmaid        : " + r.getUrlRelative().substring(r.getUrlRelative().lastIndexOf("/") + 1));
                r.setMaid(Long.parseLong(r.getUrlRelative().substring(r.getUrlRelative().lastIndexOf("/") + 1)));
                t = new Tag(lines.get(++i).trim());
                log.debug("Type         : " + t.getValues());
                r.setTypeId(MetalArchivesDAOImpl.getInstance().getMAReleaseTypeId(t.getValues()));
                t = new Tag(lines.get(++i).trim());
                log.debug("Year         : " + t.getValues());
                r.setAnnee(Integer.parseInt(t.getValues()));
                ++i;
                t = new Tag(lines.get(++i).trim());
                try {
                    log.debug("Note         : " + t.getValues());
                    r.setReview(t.getValues());
                } catch (NoValueException e) {
                }
                releases.add(r);
            }
        }
        return releases;
    }

    public void majArtistDataFromMAWebSite(long amaid) throws Exception {
        log.info("MAJ artist amaid=" + amaid);
        List<ReleaseMABean> releases = getReleasesFromMAWebSite(amaid);
        for (int i = 0; i < releases.size(); i++) {
            if (MetalArchivesDAOImpl.getInstance().getRelease(releases.get(i).getMaid()) == null) {
                Thread.sleep(1000);
                List<TrackMABean> tracks = getTracksFromMAWebSite(releases.get(i).getMaid(), releases.get(i).getUrlAbsolue());
                if (tracks.size() >= 1) {
                    releases.get(i).setURLCover(tracks.get(0).getURLCover());
                }
                persist(tracks);
                Thread.sleep(1000);
            }
            persist(releases.get(i));
        }
        MetalArchivesDAOImpl.getInstance().updateLastMajDate(amaid);
    }

    public List<ArtistMABean> rechercherArtistes(String nom) throws Exception {
        return MetalArchivesDAOImpl.getInstance().rechercherArtistes(nom);
    }

    public List<ReleaseMABean> getReleasesAndTracks(ArtistMABean ab) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getMAReleasesAndTracks(ab.getMaid());
    }

    public List<ArtistMABean> getAllArtists() throws Exception {
        return MetalArchivesDAOImpl.getInstance().getAllArtists();
    }

    public List<ReleaseMABean> getReleases(long artist_maid) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getReleases(artist_maid);
    }

    public List<ReleaseMABean> getReleasesByName(String name) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getReleasesByName(name);
    }

    public List<TrackMABean> getTracks(long release_maid) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getTracks(release_maid);
    }

    public TrackMABean getTrack(long rmaid, int discNumber, int trackNumber) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getTrack(rmaid, discNumber, trackNumber);
    }

    public ArtistMABean getArtist(long maid) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getArtist(maid);
    }

    public List<ReleaseMABean> rechercherReleases(String nom) throws Exception {
        return MetalArchivesDAOImpl.getInstance().rechercherReleases(nom);
    }

    public List<TrackMABean> rechercherTracks(String nom) throws Exception {
        return MetalArchivesDAOImpl.getInstance().rechercherTracks(nom);
    }

    public List<ReleaseMABean> getReleasesAndTracks(long maid) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getMAReleasesAndTracks(maid, new SingleColumnSort(ReleaseMABean.anneeColumnName, Sort.ASC_SORT));
    }

    public ReleaseMABean getReleaseAndTracks(long maid) throws Exception {
        ReleaseMABean release = MetalArchivesDAOImpl.getInstance().getRelease(maid);
        release.setTracksMA(MetalArchivesDAOImpl.getInstance().getTracks(maid));
        return release;
    }

    public void supprimerArtiste(long maid) throws Exception {
        List<ReleaseMABean> releases = MetalArchivesDAOImpl.getInstance().getReleases(maid);
        for (int i = 0; i < releases.size(); i++) {
            MetalArchivesDAOImpl.getInstance().supprimerTracks(releases.get(i).getMaid());
            MetalArchivesDAOImpl.getInstance().supprimerRelease(releases.get(i).getMaid());
        }
        MetalArchivesDAOImpl.getInstance().supprimerArtiste(maid);
    }

    public List<String> getPremieresLettreArtistes() throws SQLException {
        return MetalArchivesDAOImpl.getInstance().getPremieresLettreArtistes();
    }

    public int getNbArtists() throws Exception {
        return MetalArchivesDAOImpl.getInstance().getNbArtists();
    }

    public int getNbReleases() throws Exception {
        return MetalArchivesDAOImpl.getInstance().getNbReleases();
    }

    public int getNbTracks() throws Exception {
        return MetalArchivesDAOImpl.getInstance().getNbTracks();
    }

    public ReleaseMABean getRelease(long maid) throws Exception {
        return MetalArchivesDAOImpl.getInstance().getRelease(maid);
    }

    public List<ArtistMABean> searchArtistsByName(String name) throws SQLException {
        return MetalArchivesDAOImpl.getInstance().getArtistsByName(name);
    }

    /** @deprecated */
    public List<ArtistMABean> searchArtistsFromMAWebSite(String name) throws Exception {
        URL fileURL = new URL("http://www.metal-archives.com/search.php?string=" + name + "&type=band");
        URLConnection urlConnection = fileURL.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        List<ArtistMABean> artists = new ArrayList<ArtistMABean>();
        ArtistMABean ab = new ArtistMABean();
        boolean typePageTrouve = false;
        boolean plusieursReponses = false;
        boolean nomArtisteTrouve = false;
        boolean logoArtisteTrouve = false;
        boolean paysArtisteTrouve = false;
        boolean idArtisteTrouve = false;
        String ligne;
        String nomArtiste;
        StringBuffer sb = new StringBuffer("");
        while ((ligne = br.readLine()) != null) {
            log.debug("==> " + ligne);
            if (!typePageTrouve) {
                if (ligne.indexOf("Encyclopaedia Metallum - ") != -1) {
                    plusieursReponses = false;
                    typePageTrouve = true;
                    artists.add(ab);
                } else if (ligne.indexOf("Search results") != -1) {
                    plusieursReponses = true;
                    typePageTrouve = true;
                    ligne = br.readLine();
                }
            }
            if (typePageTrouve && !plusieursReponses) {
                if (!nomArtisteTrouve && ligne.indexOf("Encyclopaedia Metallum - ") != -1) {
                    nomArtiste = ligne.substring(ligne.indexOf("-") + 2, ligne.indexOf("<", 3));
                    log.debug("Nom artiste : " + nomArtiste);
                    ab.setNom(nomArtiste);
                    nomArtisteTrouve = true;
                } else if (!logoArtisteTrouve && ligne.indexOf("<img") != -1) {
                    String url = ligne.substring(ligne.indexOf("http"), ligne.indexOf("\"", 10));
                    log.debug("URL logo : " + url);
                    ab.setURLLogo(url);
                    logoArtisteTrouve = true;
                } else if (!paysArtisteTrouve && ligne.indexOf("browseC.php") != -1) {
                    String pays = ligne.substring(ligne.indexOf("=", 13) + 1, ligne.indexOf("\"", 15));
                    log.debug("Pays id : " + pays);
                    ab.setPaysMaid(Integer.parseInt(pays));
                    paysArtisteTrouve = true;
                } else if (!idArtisteTrouve && ligne.indexOf("editdata.php") != -1) {
                    String maid = ligne.substring(ligne.indexOf("=", 20) + 1, ligne.indexOf("\"", 20));
                    log.debug("MAID : " + maid);
                    ab.setMaid(Long.parseLong(maid));
                    idArtisteTrouve = true;
                } else if (ligne.indexOf(">Discography<") != -1) {
                    while (ligne.indexOf(">Links<") == -1 && ligne.indexOf("Back to the Encyclopaedia Metallum") == -1) {
                        log.debug("==> " + ligne);
                        sb.append(ligne.trim());
                        ligne = br.readLine();
                    }
                    break;
                }
            } else if (typePageTrouve && plusieursReponses) {
                int posBandPhp = 0;
                while (ligne.indexOf("band.php", posBandPhp + 1) != -1) {
                    posBandPhp = ligne.indexOf("band.php", posBandPhp + 1);
                    long maid = Long.parseLong(ligne.substring(posBandPhp + 12, ligne.indexOf("'", posBandPhp)));
                    String nom = ligne.substring(ligne.indexOf("'", posBandPhp) + 2, ligne.indexOf("<", posBandPhp));
                    log.debug("Maid : " + maid);
                    log.debug("Nom : " + nom);
                    ab = new ArtistMABean();
                    ab.setMaid(maid);
                    ab.setNom(nom);
                    artists.add(ab);
                }
                break;
            }
        }
        if (typePageTrouve && !plusieursReponses) {
            ligne = sb.toString();
            ligne = ligne.replaceAll("\t", "");
            String[] lignes = ligne.split(">");
            for (int i = 0; i < lignes.length; i++) {
                if (lignes[i].startsWith("<a")) {
                    log.debug("Ligne 1 : " + lignes[i]);
                    String rmaidS = lignes[i].substring(lignes[i].indexOf("=", lignes[i].indexOf("href") + 7) + 1, lignes[i].length() - 1);
                    log.debug("rmaid : " + rmaidS);
                    int rmaid = Integer.parseInt(rmaidS);
                    log.debug("Ligne 2 : " + lignes[++i]);
                    String nom = lignes[i].substring(0, lignes[i].indexOf("<"));
                    log.debug("Nom : " + nom);
                    i = i + 3;
                    log.debug("Ligne 3 : " + lignes[i]);
                    String type = lignes[i].substring(0, lignes[i].indexOf(","));
                    log.debug("Type : " + type);
                    String anneeS = lignes[i].substring(lignes[i].indexOf(",") + 2, lignes[i].indexOf("<"));
                    log.debug("Annee : " + anneeS);
                    int annee = Integer.parseInt(anneeS);
                    ab.addRelease(new ReleaseMABean(rmaid, ab.getMaid(), nom, MetalArchivesDAOImpl.getInstance().getMAReleaseTypeId(type), annee));
                }
            }
            getAllReleasesDataFromMAWebSite(ab, false);
        }
        br.close();
        httpStream.close();
        return artists;
    }

    public List<DemandeChargement> getDemandesChargementLoaded() throws Exception {
        QueryFilter qf = new QueryFilter(new SingleColumnSort(DemandeChargement.lastUpdateDateColumnName, Sort.DESC_SORT), 50);
        List<DemandeChargement> dem = MetalArchivesDAOImpl.getInstance().getDemandesChargement(DemandeChargement.LOADED, qf);
        chargerArtistToDemandeChargement(dem);
        return dem;
    }

    public List<DemandeChargement> getDemandesChargementLoading() throws Exception {
        List<DemandeChargement> dem = MetalArchivesDAOImpl.getInstance().getDemandesChargement(DemandeChargement.LOADING);
        chargerArtistToDemandeChargement(dem);
        return dem;
    }

    public List<DemandeChargement> getDemandesChargementToLoad() throws Exception {
        MultiColumnSort mcs = new MultiColumnSort();
        mcs.addColumnSort(new SingleColumnSort(DemandeChargement.prioriteColumnName, Sort.DESC_SORT));
        mcs.addColumnSort(new SingleColumnSort(DemandeChargement.creationDateColumnName, Sort.ASC_SORT));
        List<DemandeChargement> dem = MetalArchivesDAOImpl.getInstance().getDemandesChargement(DemandeChargement.TO_LOAD, new QueryFilter(mcs));
        chargerArtistToDemandeChargement(dem);
        return dem;
    }

    public List<DemandeChargement> getDemandesChargementToPreLoad() throws Exception {
        List<DemandeChargement> dem = MetalArchivesDAOImpl.getInstance().getDemandesChargementToPreLoad();
        chargerArtistToDemandeChargement(dem);
        return dem;
    }

    private void chargerArtistToDemandeChargement(List<DemandeChargement> dem) throws SQLException {
        for (int i = 0; i < dem.size(); i++) {
            dem.get(i).setArtist(MetalArchivesDAOImpl.getInstance().getArtist(dem.get(i).getAmaid()));
        }
    }

    public boolean persist(DemandeChargement dem) throws SQLException {
        boolean retour = false;
        if (MetalArchivesDAOImpl.getInstance().getDemandeChargement(dem.getAmaid(), DemandeChargement.TO_LOAD) == null) {
            MetalArchivesDAOImpl.getInstance().ajouter(dem);
            retour = true;
        }
        return retour;
    }

    public void supprimerDemandeChargement(int demId) throws SQLException {
        MetalArchivesDAOImpl.getInstance().supprimerDemandeChargement(demId);
    }

    public DemandeChargement getNextDemandeChargement() throws Exception {
        DemandeChargement next = null;
        List<DemandeChargement> loading = getDemandesChargementLoading();
        if (loading.size() > 0) {
            next = loading.get(0);
        } else {
            List<DemandeChargement> toLoad = getDemandesChargementToLoad();
            if (toLoad.size() > 0) {
                next = toLoad.get(0);
                MetalArchivesDAOImpl.getInstance().updateDemandeChargementStatut(next, DemandeChargement.LOADING);
            }
        }
        return next;
    }

    public void augmenterPrioriteDemandeChargement(int demId) throws SQLException {
        DemandeChargement demande = MetalArchivesDAOImpl.getInstance().getDemandeChargement(demId);
        MetalArchivesDAOImpl.getInstance().modifierPrioriteDemandeChargement(demId, demande.getPriorite() + 1);
    }

    public void diminuerPrioriteDemandeChargement(int demId) throws SQLException {
        DemandeChargement demande = MetalArchivesDAOImpl.getInstance().getDemandeChargement(demId);
        MetalArchivesDAOImpl.getInstance().modifierPrioriteDemandeChargement(demId, demande.getPriorite() - 1);
    }

    public void updateDemandeChargementStatut(DemandeChargement dem, String statut) throws SQLException {
        MetalArchivesDAOImpl.getInstance().updateDemandeChargementStatut(dem, statut);
    }

    public List<DemandeChargement> getArtistToLoadFromWiki() throws Exception {
        URL fileURL = new URL("http://beastchild.free.fr/wiki/doku.php?id=music");
        URLConnection urlConnection = fileURL.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        String ligne;
        List<DemandeChargement> dem = new ArrayList<DemandeChargement>();
        while ((ligne = br.readLine()) != null) {
            if (ligne.indexOf("&lt;@@@&gt;") != -1) {
                String maidS = ligne.substring(ligne.indexOf("&lt;@@@&gt;") + 11, ligne.indexOf("&lt;/@@@&gt;")).trim();
                try {
                    long maid = Long.parseLong(maidS);
                    log.info("MAID to load : " + maid);
                    dem.add(new DemandeChargement(maid));
                } catch (Exception e) {
                    log.error("Impossible de recuperer le MAID : " + maidS);
                }
            }
        }
        br.close();
        httpStream.close();
        return dem;
    }

    public void exportMAArtist(long amaid, File exportFile) throws Exception {
        ActionsFichiers.creerFichierTexte(exportFile, exportMAArtist(amaid));
    }

    public String exportMAArtist(long amaid) throws Exception {
        ArtistMABean a = getArtist(amaid);
        List<ReleaseMABean> r = getReleasesAndTracks(amaid);
        StringBuffer sb = new StringBuffer();
        sb.append("A;");
        sb.append(a.getMaid() + ";");
        sb.append(a.getNom() + ";");
        sb.append(a.getPaysMaid() + ";");
        sb.append(a.getURLLogo() + "\n");
        for (int i = 0; i < r.size(); i++) {
            sb.append("R;");
            sb.append(r.get(i).getMaid() + ";");
            sb.append(r.get(i).getNom() + ";");
            sb.append(r.get(i).getArtistMaId() + ";");
            sb.append(r.get(i).getAnnee() + ";");
            sb.append(r.get(i).getTypeId() + ";");
            sb.append(r.get(i).getURLCoverRelative() + "\n");
            for (int j = 0; j < r.get(i).getTracksMA().size(); j++) {
                sb.append("T;");
                sb.append(r.get(i).getTracksMA().get(j).getMaid() + ";");
                sb.append(r.get(i).getTracksMA().get(j).getReleaseMaid() + ";");
                sb.append(r.get(i).getTracksMA().get(j).getDisc() + ";");
                sb.append(r.get(i).getTracksMA().get(j).getNumero() + ";");
                sb.append(r.get(i).getTracksMA().get(j).getNom() + ";");
                sb.append(r.get(i).getTracksMA().get(j).getDuree() + "\n");
                if (r.get(i).getTracksMA().get(j).getLyrics() != null) {
                    sb.append("L;\n" + r.get(i).getTracksMA().get(j).getLyrics() + "\n/L;\n");
                }
            }
        }
        return sb.toString();
    }

    public void importMAArtist(File importFile) throws Exception {
        log.info("Importation du fichier : " + importFile.getPath());
        ArtistMABean a = null;
        ReleaseMABean r = null;
        TrackMABean t = null;
        InputStream ips = new FileInputStream(importFile);
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String ligne;
        while ((ligne = br.readLine()) != null) {
            if (ligne.startsWith("A;")) {
                try {
                    log.debug(ligne);
                    String[] col = ligne.split(";");
                    a = new ArtistMABean();
                    a.setMaid(Long.parseLong(col[1]));
                    a.setNom(col[2]);
                    a.setPaysMaid(Integer.parseInt(col[3]));
                    a.setURLLogo(col[4]);
                    if (MetalArchivesDAOImpl.getInstance().artisteMAExists(a.getMaid())) {
                        log.info("[A] L'artiste existe deja : " + a);
                    } else {
                        log.info("[A] Ajout de l'artiste : " + a);
                        persistArtist(a);
                    }
                } catch (Exception e) {
                    log.error("Erreur a l'importation de l'artiste : " + a, e);
                }
            } else if (ligne.startsWith("R;")) {
                try {
                    if (r != null) {
                        if (MetalArchivesDAOImpl.getInstance().releaseMAExists(r.getMaid())) {
                            log.info(" ->[R] La release existe deja : " + r);
                        } else {
                            log.info(" ->[R] Ajout de la release : " + r);
                            persistReleaseAndAllTracks(r);
                        }
                    }
                    log.debug(ligne);
                    String[] col = ligne.split(";");
                    r = new ReleaseMABean();
                    r.setMaid(Long.parseLong(col[1]));
                    r.setNom(col[2]);
                    r.setArtistMaId(Long.parseLong(col[3]));
                    r.setAnnee(Integer.parseInt(col[4]));
                    r.setTypeId(Integer.parseInt(col[5]));
                    r.setURLCover(col[6]);
                } catch (Exception e) {
                    log.error("Erreur a l'importation de la release : " + r, e);
                }
            } else if (ligne.startsWith("T;")) {
                try {
                    log.debug(ligne);
                    String[] col = ligne.split(";");
                    t = new TrackMABean();
                    t.setMaid(Long.parseLong(col[1]));
                    t.setReleaseMaid(Long.parseLong(col[2]));
                    t.setDisc(Integer.parseInt(col[3]));
                    t.setNumero(Integer.parseInt(col[4]));
                    t.setNom(col[5]);
                    t.setDuree(Integer.parseInt(col[6]));
                    r.addTrack(t);
                } catch (Exception e) {
                    log.error("Erreur a l'importation de la track : " + t, e);
                }
            } else if (ligne.startsWith("L;")) {
                try {
                    StringBuffer sb = new StringBuffer();
                    while (!ligne.startsWith("/L;")) {
                        ligne = br.readLine();
                        if (ligne == null) break;
                        if (!ligne.startsWith("/L;")) {
                            sb.append(ligne + "\n");
                        }
                    }
                    t.setLyrics(sb.toString());
                } catch (Exception e) {
                    log.error("Erreur a l'importation de la lyric : " + t, e);
                }
            }
        }
        if (r != null) {
            if (MetalArchivesDAOImpl.getInstance().releaseMAExists(r.getMaid())) {
                log.info(" ->[R] La release existe deja : " + r);
            } else {
                log.info(" ->[R] Ajout de la release : " + r);
                persistReleaseAndAllTracks(r);
            }
        }
        br.close();
    }

    public List<ArtistMABean> getArtistsToMAJ() throws Exception {
        int nbJours;
        try {
            nbJours = GMusicServicesImpl.getInstance().getOption("NB_DAYS_BEFORE_RELOADING").getIntValue();
        } catch (Exception e) {
            nbJours = 120;
        }
        return MetalArchivesDAOImpl.getInstance().getArtistsToMAJ(nbJours);
    }

    public void ajoutDemandeChargementMAJAuto() throws Exception {
        List<ArtistMABean> artistToLoad = getArtistsToMAJ();
        if (artistToLoad.size() > 0) {
            DemandeChargement toMAJ = new DemandeChargement(artistToLoad.get(0).getMaid(), DemandeChargement.SOURCE_MAJ_AUTO);
            log.info("Creation AUTO pour la demande de MAJ du amaid=" + toMAJ.getAmaid());
            persist(toMAJ);
        }
    }

    public List<ReleaseMAVO> getLastReleases(int nb) throws Exception {
        List<ReleaseMAVO> releases = new ArrayList<ReleaseMAVO>();
        return releases;
    }

    public List<ReleaseMABean> getReleasesByName(long maid, String chaine) throws SQLException {
        return MetalArchivesDAOImpl.getInstance().getReleasesByName(maid, chaine);
    }
}
