package nat;

import gestionnaires.AfficheurLog;
import gestionnaires.GestionnaireErreur;
import nat.ConfigNat;
import nat.transcodeur.*;
import nat.convertisseur.*;
import nat.presentateur.*;
import outils.CharsetToolkit;
import outils.FileToolKit;
import outils.HyphenationToolkit;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Classe représentant une transcription dans nat
 */
public class Transcription {

    /** Une instance de Convertisseur */
    private Convertisseur conv;

    /** Une instance de Transcodeur */
    private Transcodeur trans;

    /** Une instance de Presentateur */
    private Presentateur pres;

    /** Type mime du fichier source */
    private String sourceMimeType = "";

    /** Instance de GestionnaireErreur */
    private GestionnaireErreur gest;

    /** adresse du fichier temporaire au format interne (après conversion)*/
    public static final String fTempXML = ConfigNat.getUserTempFolder() + "tmp.xml";

    /** adresse du fichier temporaire au format xhtml (après conversion en xhtml et avant conversion au format interne)*/
    public static final String fTempXHTML = ConfigNat.getUserTempFolder() + "tmp.xhtml";

    /** adresse du fichier temporaire au format de mise en page (après transcription et avant mise en page)*/
    public static final String fTempXML2 = ConfigNat.getUserTempFolder() + "tmp_mep.xml";

    /** adresse du fichier xml contenant les entêtes pour le changement de table braille */
    public static final String fTempEntetes = ConfigNat.getUserTempFolder() + "convTexteEntetes.tmp";

    /** adresse du fichier temporaire au format odt (après conversion par JODTConverter et avant conversion en XHTML)*/
    public static final String fTempODT = ConfigNat.getUserTempFolder() + "tmp.odt";

    /** adresse du fichier temporaire convertit dans la table UTF8 pour TAN */
    public static final String fTempTan = ConfigNat.getUserTempFolder() + "tmpUTF8.tan";

    /** adresse du fichier temporaire html téléchargé */
    public static final String fTempHtml = ConfigNat.getUserTempFolder() + "tmpHtml.html";

    /** Le fichier à utiliser pour l'hyphenation */
    public static final String xslHyphen = ConfigNat.getUserTempFolder() + "hyphens.xsl";

    /**
     * Constructeur privé paramétré
     * @param g Le GestionnaireErreur à utiliser
     * @param c une instance de Convertisseur
     * @param t une instance de Transcodeur
     * @param p une instance de Presentateur
     */
    private Transcription(GestionnaireErreur g, Convertisseur c, Transcodeur t, Presentateur p) {
        gest = g;
        pres = p;
        trans = t;
        conv = c;
        rotateLogs();
    }

    /**
	 * Fabrique d'instances de Transcription
	 * Fabrique une transcription pour le fichier <code>fs</code> et la sortie <code>fc</code>
	 * Utilise la valeur renvoyée par {@link ConfigNat#isReverseTrans()} pour déterminer le sens de la transcription
	 * Appelle ensuite this{@link #fabriqueTranscription(String, String, GestionnaireErreur, boolean)}
	 * @param fNoir Le fichier noir
	 * @param fBraille Le  fichier braille
	 * @param g une instance de GestionnaireErreur
	 * @return une instance de Transcription ou null si le fichier d'entrée n'existe pas
	 */
    public static Transcription fabriqueTranscription(String fNoir, String fBraille, GestionnaireErreur g) {
        boolean reverse = ConfigNat.getCurrentConfig().isReverseTrans();
        return fabriqueTranscription(fNoir, fBraille, g, reverse);
    }

    /**
	 * Fabrique d'instances de Transcription
	 * Fabrique une transcription pour le fichier <code>fs</code> et la sortie <code>fc</code>
	 * @param fNoir Le fichier noir
	 * @param fBraille Le  fichier braille
	 * @param g une instance de GestionnaireErreur
	 * @param reverse vrai si transcription inverse, false si transcription du noir vers le braille
	 * @return une instance de Transcription ou null si le fichier d'entrée n'existe pas
	 */
    public static Transcription fabriqueTranscription(String fNoir, String fBraille, GestionnaireErreur g, boolean reverse) {
        Transcription retour = null;
        Convertisseur c;
        Presentateur p;
        Transcodeur t;
        String noirEncoding = (reverse) ? "UTF-8" : ConfigNat.getCurrentConfig().getNoirEncoding();
        String brailleEncoding = ConfigNat.getCurrentConfig().getBrailleEncoding();
        String tableBraille = ConfigNat.getCurrentConfig().getTableBraille();
        String sourceMimeType = "";
        g.afficheMessage("\nAnalyse du fichier source " + fNoir, Nat.LOG_SILENCIEUX);
        if (fNoir.startsWith("http://") || fNoir.startsWith("www")) {
            URL url;
            try {
                url = new URL(fNoir);
                URLConnection urlCon = url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
                File ftmp = new File(fTempHtml);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ftmp)));
                String ligne = "";
                while ((ligne = br.readLine()) != null) {
                    bw.write(ligne);
                }
                br.close();
                bw.close();
                fNoir = fTempHtml;
            } catch (MalformedURLException e) {
                g.afficheMessage("\n** adresse internet non valide", Nat.LOG_SILENCIEUX);
            } catch (IOException e) {
                g.afficheMessage("\n** erreur d'entrée sortie lors de la création du fichier source temporaire sur le disque", Nat.LOG_SILENCIEUX);
            }
        }
        if (verifFichier(new File(fNoir), g) && !reverse || reverse && verifFichier(new File(fBraille), g)) {
            if (reverse) {
                if (!noirEncoding.equals("automatique") && !noirEncoding.equals("")) {
                    g.afficheMessage("\n** Utilisation de l'encodage " + noirEncoding + " spécifié dans les options pour le fichier braille\n", Nat.LOG_NORMAL);
                } else {
                    noirEncoding = trouveEncodingSource(fNoir, g);
                    if (noirEncoding == null || noirEncoding.equals("")) {
                        noirEncoding = Charset.defaultCharset().name();
                        g.afficheMessage("\n** Impossible de détecter l'encodage du fichier braille." + "\n** Utilisation de l'encodage par défaut: " + noirEncoding + "\n", Nat.LOG_NORMAL);
                    } else {
                        g.afficheMessage("\n** Détection automatique de l'encodage du fichier braille: " + noirEncoding + "\n", Nat.LOG_NORMAL);
                    }
                }
                FileToolKit.convertBrailleFile(fBraille, fTempTan, ConfigNat.getUserBrailleTableFolder() + "Brltab.ent", ConfigNat.getInstallFolder() + "xsl/tablesEmbosseuse/brailleUTF8.ent", ConfigNat.getCurrentConfig().getBrailleEncoding(), "UTF-8", g);
                c = new ConvertisseurTan(fTempTan, fTempXML, '⠀');
                if (noirEncoding.equals("automatique")) {
                    if (brailleEncoding.equals("automatique")) {
                        noirEncoding = Charset.defaultCharset().name();
                    } else {
                        noirEncoding = brailleEncoding;
                    }
                }
                t = new TranscodeurNormal(fTempXML, fTempXML2, "UTF-8", g);
                t.setSens(true);
                p = new PresentateurSans(g, noirEncoding, fTempXML2, fNoir, tableBraille);
                retour = new Transcription(g, c, t, p);
            } else {
                sourceMimeType = trouveMimeTypeSource(fNoir, g);
                g.afficheMessage("\n** Le fichier source est de type " + sourceMimeType, Nat.LOG_NORMAL);
                if (sourceMimeType.equals("text/plain")) {
                    if (!noirEncoding.equals("automatique") && !noirEncoding.equals("")) {
                        g.afficheMessage("\n** Utilisation de l'encodage " + noirEncoding + " spécifié dans les options pour le fichier source\n", Nat.LOG_NORMAL);
                    } else {
                        noirEncoding = trouveEncodingSource(fNoir, g);
                        if (noirEncoding == null || noirEncoding.equals("")) {
                            noirEncoding = Charset.defaultCharset().name();
                            g.afficheMessage("\n** Impossible de détecter l'encodage du fichier source.\n** Utilisation de l'encodage par défaut: " + noirEncoding + "\n", Nat.LOG_NORMAL);
                        } else {
                            g.afficheMessage("\n** Détection automatique de l'encodage du fichier source: " + noirEncoding + "\n", Nat.LOG_NORMAL);
                        }
                    }
                    if (ConfigNat.getCurrentConfig().getTraiterMaths() || ConfigNat.getCurrentConfig().getTraiterMusique()) {
                        c = new ConvertisseurTexteMixte(fNoir, fTempXML, noirEncoding);
                    } else {
                        c = new ConvertisseurTexte(fNoir, fTempXML, noirEncoding);
                    }
                } else if (sourceMimeType.equals("") || fNoir.endsWith("odt") || fNoir.endsWith("sxw")) {
                    g.afficheMessage("\n** Le fichier source est identifié comme document openoffice ", Nat.LOG_NORMAL);
                    c = new ConvertisseurOpenOffice(fNoir, fTempXML);
                } else if (sourceMimeType.equals("text/html")) {
                    g.afficheMessage("\n** Le fichier source est identifié comme document xml/html", Nat.LOG_NORMAL);
                    c = new ConvertisseurXML(fNoir, fTempXML);
                } else if (fNoir.endsWith("xhtml")) {
                    g.afficheMessage("\n** Le fichier source est identifié comme document xhtml", Nat.LOG_NORMAL);
                    c = new ConvertisseurXML(fNoir, fTempXML);
                } else if (sourceMimeType.equals("application/xml")) {
                    if (fNoir.endsWith("nat") || fNoir.endsWith("zob")) {
                        g.afficheMessage("\n** Le fichier source est identifié comme un format interne", Nat.LOG_NORMAL);
                        c = new ConvertisseurSans(fNoir, fTempXML);
                    } else {
                        g.afficheMessage("\n** Le fichier source est identifié comme document xml/html", Nat.LOG_NORMAL);
                        c = new ConvertisseurXML(fNoir, fTempXML);
                    }
                } else {
                    g.afficheMessage("\n** Utilisation de JODConverter", Nat.LOG_NORMAL);
                    c = new Convertisseur2ODT(fNoir, fTempXML);
                }
                if (brailleEncoding.compareTo("automatique") == 0) {
                    if (noirEncoding.equals("automatique")) {
                        brailleEncoding = Charset.defaultCharset().name();
                    } else {
                        brailleEncoding = noirEncoding;
                    }
                }
                p = new PresentateurMEP(g, brailleEncoding, fTempXML2, fBraille, tableBraille);
                if (!new File(ConfigNat.getCurrentConfig().getDicoCoup()).exists() || !new File(ConfigNat.getUserTempFolder() + "hyphens.xsl").exists()) {
                    g.afficheMessage("\n** Création du fichier de coupure à partir de " + ConfigNat.getCurrentConfig().getDicoCoup(), Nat.LOG_NORMAL);
                    HyphenationToolkit.fabriqueDicoNat(ConfigNat.getCurrentConfig().getDicoCoup(), xslHyphen, "UTF-8");
                } else {
                    g.afficheMessage("\n** Utilisation du dictionnaire de coupure existant", Nat.LOG_NORMAL);
                }
                if (fNoir.endsWith("nat")) {
                    g.afficheMessage("\n** Le fichier source est identifié comme format interne de présentation", Nat.LOG_NORMAL);
                    t = new TranscodeurSans(fTempXML, fTempXML2, "UTF-8", g);
                } else {
                    t = new TranscodeurNormal(fTempXML, fTempXML2, "UTF-8", g);
                }
                retour = new Transcription(g, c, t, p);
            }
        }
        return retour;
    }

    /**
     * Renvoie sourceMimeType
     * @return sourceMimeType
     * @see Transcription#sourceMimeType
     */
    public String getSourceMimeType() {
        return sourceMimeType;
    }

    /**
     * Lance le scénario complet de transcription de la transcription
     * @return true si le scénario s'est déroulé sans erreur
     */
    public boolean transcrire() {
        boolean ok = true;
        if (trans.getSens()) {
            gest.afficheMessage("\n     ********\n     * TAN \n     *******", Nat.LOG_SILENCIEUX);
        }
        gest.afficheMessage("\n*** Suppression des images temporaires...", Nat.LOG_VERBEUX);
        File repertoire = new File(ConfigNat.getUserTempFolder() + "tmp.xhtml-img");
        if (repertoire.isDirectory()) {
            File[] listImages = repertoire.listFiles();
            for (File f : listImages) {
                f.delete();
            }
        }
        gest.afficheMessage("\nDébut de la conversion du document ... \n", Nat.LOG_SILENCIEUX);
        ok = conv.convertir(gest);
        if (ok) {
            gest.afficheMessage("\n--Conversion terminée en " + conv.donneTempsExecution() + " msec.\n", Nat.LOG_SILENCIEUX);
            ok = trans.transcrire(gest);
        }
        if (ok) {
            gest.afficheMessage("\n--Transcodage terminé en " + trans.donneTempsExecution() + " msec.\n", Nat.LOG_SILENCIEUX);
            ok = pres.presenter();
        }
        if (ok) {
            gest.afficheMessage("\n--Mise en forme terminée en " + pres.donneTempsExecution() + " msec.\n", Nat.LOG_SILENCIEUX);
            long tempsExecution = conv.donneTempsExecution() + trans.donneTempsExecution() + pres.donneTempsExecution();
            gest.afficheMessage("\n----Transcription terminée en " + tempsExecution / 1000 + "," + tempsExecution % 1000 + " sec.\n", Nat.LOG_SILENCIEUX);
        } else {
            gest.afficheMessage("\n--ERREUR lors de la transcription ! --", Nat.LOG_SILENCIEUX);
        }
        return ok;
    }

    /**
     * essaie de trouver le type mime du fichier <code>source</code>
     * @param source le fichier à analyser
     * @param gest une instance de GestionnaireErreur
     * @return le type mime de <code>source</code> ou "" si type non reconnu
     */
    private static String trouveMimeTypeSource(String source, GestionnaireErreur gest) {
        String retour = "";
        File file = new File(source);
        if (verifFichier(file, gest)) {
            try {
                URL url = file.toURI().toURL();
                URLConnection connection = url.openConnection();
                retour = connection.getContentType();
            } catch (MalformedURLException mue) {
                gest.setException(mue);
                gest.gestionErreur();
            } catch (IOException ioe) {
                gest.setException(ioe);
                gest.gestionErreur();
            }
        }
        return retour;
    }

    /**
	 * Essaie de trouver l'encodage du fichier <code>source</code>
	 * @param source le fichier à analyser
	 * @param gest une instance de GestionnaireErreur
	 * @return l'encodage du fichier <code>source</code> ou "" ou null si l'encoding n'est pas reconnu
	 */
    public static String trouveEncodingSource(String source, GestionnaireErreur gest) {
        String sourceEncoding = "";
        File file = new File(source);
        if (verifFichier(file, gest)) {
            try {
                URL url = file.toURI().toURL();
                URLConnection connection = url.openConnection();
                sourceEncoding = connection.getContentEncoding();
            } catch (MalformedURLException mue) {
                gest.setException(mue);
                gest.gestionErreur();
            } catch (IOException ioe) {
                gest.setException(ioe);
                gest.gestionErreur();
            }
        }
        if (sourceEncoding == null) {
            try {
                File fsource = new File(source);
                Charset guessedCharset = CharsetToolkit.guessEncoding(fsource, 4096, Charset.defaultCharset());
                if (guessedCharset != null) {
                    sourceEncoding = guessedCharset.name();
                }
            } catch (FileNotFoundException fnfe) {
                gest.setException(fnfe);
                gest.gestionErreur();
            } catch (IOException ioe) {
                gest.setException(ioe);
                gest.gestionErreur();
            }
        }
        return sourceEncoding;
    }

    /**
	 * Vérifie si le Fichier <code>file</code> existe et n'est pas un répertoire
	 * @param file un objet File
	 * @param gest une instance de GestionnaireErreur
	 * @return true si <code>file</code> existe et n'est pas un répertoire
	 */
    private static boolean verifFichier(File file, GestionnaireErreur gest) {
        boolean retour = true;
        if (file.isDirectory()) {
            gest.afficheMessage("nok\n *Erreur: " + file.getAbsolutePath() + " est un répertoire et non un fichier", Nat.LOG_SILENCIEUX);
            gest.setException(new Exception("Le fichier est un répertoire"));
            retour = false;
        } else if (!file.exists()) {
            gest.afficheMessage("nok\n*Erreur: le fichier " + file.getAbsolutePath() + " n'existe pas", Nat.LOG_SILENCIEUX);
            gest.setException(new Exception("Le fichier n'existe pas"));
            retour = false;
        }
        return retour;
    }

    /**
	 * Choisi de lancer ou non la rotation des fichiers de logs
	 */
    private void rotateLogs() {
        ArrayList<AfficheurLog> afficheurs = gest.getAfficheursLog();
        if (afficheurs.size() > 0) {
            File log = new File(ConfigNat.getUserTempFolder() + "/nat_log.1");
            if (log.exists() && log.length() > ConfigNat.getCurrentConfig().getLogFileSize() * 1000) {
                for (int i = ConfigNat.getCurrentConfig().getNbLogFiles(); i > 0; i--) {
                    if (new File(ConfigNat.getUserTempFolder() + "/nat_log." + (i - 1)).exists()) {
                        FileToolKit.copyFile(ConfigNat.getUserTempFolder() + "/nat_log." + (i - 1), ConfigNat.getUserTempFolder() + "/nat_log." + i);
                    }
                }
                log.delete();
            }
        }
    }
}
