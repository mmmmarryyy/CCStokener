package nat;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import outils.ConfConv;
import nat.ConfigNat;
import nat.GetOptNat;
import nat.convertisseur.Convertisseur;
import nat.convertisseur.ConvertisseurChaine;
import nat.presentateur.PresentateurMEP;
import nat.transcodeur.Transcodeur;
import nat.transcodeur.TranscodeurNormal;
import nat.presentateur.Presentateur;
import gestionnaires.AfficheurLog;
import gestionnaires.GestionnaireErreur;
import ui.AfficheurConsole;
import ui.FenetrePrinc;

/**
 * Classe principale de l'application
 * @author bruno
 *
 */
public class Nat {

    /** Représente un niveau de verbosité des logs muet */
    public static final int LOG_AUCUN = 0;

    /** Représente un niveau de verbosité des logs très faible */
    public static final int LOG_SILENCIEUX = 1;

    /** Représente un niveau de verbosité des logs normal */
    public static final int LOG_NORMAL = 2;

    /** Représente un niveau de verbosité des logs verbeux */
    public static final int LOG_VERBEUX = 3;

    /** Représente un niveau de verbosité des logs verbeux avec les informations de débuggage */
    public static final int LOG_DEBUG = 4;

    /** Représente la génération de version de configuration */
    public static final String CONFS_VERSION = "3";

    /** adresse web du fichier contenant le n° de la dernière version en ligne */
    private static final String CURRENT_VERSION_ADDRESS = "http://natbraille.free.fr/current-version.txt";

    /** Represents the use of an unknown or not implemented screen reader */
    public static final int SR_DEFAULT = 1;

    /** Represents the use of JAWS */
    public static final int SR_JAWS = 2;

    /** Represents the use of NVDA */
    public static final int SR_NVDA = 3;

    /** Represents the use of NVDA */
    public static final int SR_WEYES = 4;

    /** String contenant la licence de NAT (GPL) */
    private static String licence;

    /** Une instance de gestionnaire d'erreur */
    private GestionnaireErreur gest;

    /** true si pas de transcriptions en cours */
    private boolean ready = true;

    /** true si nouvelle version disponible */
    private boolean updateAvailable = false;

    /** Liste d'instances de transcription représentant les transcription à réaliser */
    private ArrayList<Transcription> transcriptions = new ArrayList<Transcription>();

    /**
     * Constructeur
     * @param g Une instance de GestionnaireErreur
     */
    public Nat(GestionnaireErreur g) {
        licence = getLicence("", "");
        gest = g;
    }

    /**
     * renvoie le nom du fichier de configuration
     * @return le nom du fichier de configuration
     */
    public String getFichierConf() {
        return ConfigNat.getCurrentConfig().getFichierConf();
    }

    /**
     * Renvoie une chaine contenant le numéro long de la version de NAT
     * @return une chaine contenant le numéro long de version
     */
    public String getVersionLong() {
        return ConfigNat.getVersionLong();
    }

    /**
     * Renvoie une chaine contenant le nom de version de NAT
     * @return une chaine contenant le nom de version
     */
    public String getVersion() {
        return ConfigNat.getVersion();
    }

    /**
     * @param ua the updateAvailable to set
     * @see #updateAvailable
     */
    public void setUpdateAvailable(boolean ua) {
        updateAvailable = ua;
    }

    /**
     * @return the updateAvailable value
     * @see #updateAvailable
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Renvoie l'instance de GestionnaireErreur
     * @return l'instance de GestionnaireErreur
     * @see Nat#gest
     */
    public GestionnaireErreur getGestionnaireErreur() {
        return gest;
    }

    /**
     * Renvoie la licence de nat préfixée par prefixe et terminée par suffixe
     * @param prefixe préfixe à insérer avant la licence (/* ou <!-- par exemple)
     * @param suffixe suffixe à insérer après la licence (* / ou --> par exemple)
     * @return la licence de NAT
     */
    public static String getLicence(String prefixe, String suffixe) {
        licence = prefixe + " * NAT - An universal Translator\n" + "* Copyright (C) 2009 Bruno Mascret\n" + "* Contact: bmascret@free.fr\n" + "* \n" + "* This program is free software; you can redistribute it and/or\n" + "* modify it under the terms of the GNU General Public License\n" + "* as published by the Free Software Foundation; either version 2\n" + "* of the License.\n" + "* \n" + "* This program is distributed in the hope that it will be useful,\n" + "* but WITHOUT ANY WARRANTY; without even the implied warranty of\n" + "* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" + "* GNU General Public License for more details.\n" + "* \n" + "* You should have received a copy of the GNU General Public License\n" + "* along with this program; if not, write to the Free Software\n" + "* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.\n" + suffixe;
        return licence;
    }

    /**
     * Fait appel à la fabrique Transcription pour obtenir les instances de transcription à réaliser
     * Utilise le booléen <code>reverse</code> pour contraindre le sens de transcription 
     * @param noirs les adresses des fichiers noir
     * @param brailles les adresses des fichiers braille
     * @param reverse indique le sens de transcription: true si inverse, false sinon
     * @return <code>true</code> si la fabrication a réussi
     * @see Transcription#fabriqueTranscription(String, String, GestionnaireErreur, boolean)
     */
    public boolean fabriqueTranscriptions(ArrayList<String> noirs, ArrayList<String> brailles, boolean reverse) {
        boolean retour = true;
        transcriptions.removeAll(transcriptions);
        for (int i = 0; i < noirs.size(); i++) {
            String noir = noirs.get(i);
            String braille = brailles.get(i);
            Transcription t = Transcription.fabriqueTranscription(noir, braille, gest, reverse);
            if (t != null) {
                transcriptions.add(t);
            } else {
                retour = false;
            }
        }
        return retour;
    }

    /**
     * Fait appel à la fabrique Transcription pour obtenir les instances de transcription à réaliser
     * Ne détermine pas le sens de la transcription, qui sera établit dans {@link Transcription#fabriqueTranscription(String, String, GestionnaireErreur)}
     * @param noirs les adresses des fichiers noirs
     * @param brailles les adresses des fichiers braille
     * @return <code>true</code> si la fabrication a réussi
     * @see Transcription#fabriqueTranscription(String, String, GestionnaireErreur)
     */
    public boolean fabriqueTranscriptions(ArrayList<String> noirs, ArrayList<String> brailles) {
        boolean retour = true;
        transcriptions.removeAll(transcriptions);
        for (int i = 0; i < noirs.size(); i++) {
            String noir = noirs.get(i);
            String braille = brailles.get(i);
            Transcription t = Transcription.fabriqueTranscription(noir, braille, gest);
            if (t != null) {
                transcriptions.add(t);
            } else {
                retour = false;
            }
        }
        return retour;
    }

    /**
     * Lance le processus complet de transcription des instances de <code>transcription</code>
     * Attends éventuellement si une transcription est en cours
     * @return true si le scénario s'est déroulé normallement
     * @see Nat#transcriptions
     */
    public boolean lanceScenario() {
        if (!ready) {
            gest.afficheMessage("\nLa transcription commencera dès la fin de la transcription en cours\n", Nat.LOG_NORMAL);
            while (!ready) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ready = false;
        gest.setException(null);
        boolean retour = true;
        for (Transcription t : transcriptions) {
            try {
                retour = retour & t.transcrire();
            } catch (OutOfMemoryError oome) {
                gest.setException(new Exception("mémoire", oome));
                gest.gestionErreur();
            }
        }
        ready = true;
        return retour;
    }

    /**
     * Vérifie si une nouvelle version est disponible en ligne
     * Met à jour {@link #updateAvailable}
     * @return true si vérification effectuée, false si vérification impossible
     */
    public boolean checkUpdate() {
        boolean retour = true;
        gest.afficheMessage("Recherche d'une mise à jour de NAT...", LOG_VERBEUX);
        URL url;
        try {
            url = new URL(CURRENT_VERSION_ADDRESS);
            URLConnection urlCon = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
            String ligne = br.readLine();
            br.close();
            if (Integer.parseInt(ligne) > ConfigNat.getSvnVersion()) {
                updateAvailable = true;
            }
        } catch (NumberFormatException nfe) {
            gest.afficheMessage("\n** pas de connexion web pour vérifier la présence de mise à jour", Nat.LOG_SILENCIEUX);
            retour = false;
        } catch (MalformedURLException e) {
            gest.afficheMessage("\n** adresse internet " + CURRENT_VERSION_ADDRESS + " non valide", Nat.LOG_SILENCIEUX);
            retour = false;
        } catch (IOException e) {
            gest.afficheMessage("\n** erreur d'entrée sortie lors de la vérification de l'existence d'une mise à jour", Nat.LOG_SILENCIEUX);
            retour = false;
        }
        return retour;
    }

    /**
     * Appel à la méthode touveEncodingSource de Transcription
     * @param source le fichier source
     * @return une chaîne correspondant à l'encodage du fichier source
     * @see Transcription#trouveEncodingSource(String, GestionnaireErreur)
     */
    public String trouveEncodingSource(String source) {
        return Transcription.trouveEncodingSource(source, gest);
    }

    /**
     * Lecture de l'entrée standard
     * @return chaine lue dans l'entrée standard, terminée par un '\n'
     */
    private static String getOneStdinLine() {
        char c = 0;
        String buf = "";
        try {
            while (c != '\n') {
                c = (char) (System.in.read());
                buf += c;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf;
    }

    /**
     * Méthode main
     * Analyse la chaine de paramètres, lance ou non l'interface graphique, la transcription, etc
     * @param argv les paramètres de la méthode main
     */
    public static void main(String argv[]) {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        ConfigNat.charger(null);
        GestionnaireErreur gestErreur = new GestionnaireErreur(null, ConfigNat.getCurrentConfig().getNiveauLog());
        Nat nat = new Nat(gestErreur);
        AfficheurConsole ac = new AfficheurConsole();
        gestErreur.addAfficheur(ac);
        gestErreur.addAfficheur(new AfficheurLog());
        ConfConv.convert(gestErreur);
        try {
            GetOptNat cl = new GetOptNat(argv);
            if (cl.useTheGui) {
                ConfigNat.setGUI(true);
                FenetrePrinc fenetre = new FenetrePrinc(nat);
                fenetre.pack();
                if (ConfigNat.getCurrentConfig().getMaximizedPrincipal()) {
                    fenetre.setExtendedState(Frame.MAXIMIZED_BOTH);
                }
                if (cl.fromFiles.size() == 1) {
                    fenetre.setEntree(cl.fromFiles.get(0));
                    fenetre.setSortie(cl.fromFiles.get(0));
                    fenetre.setSortieAuto(false);
                }
                fenetre.setVisible(true);
            } else {
                ConfigNat.charger(cl.getConfigFile());
                if (cl.quiet) {
                    gestErreur.removeAfficheur(ac);
                }
                if (cl.fromFiles.get(0).equals("-")) {
                    while (true) {
                        String line = getOneStdinLine();
                        String txml = ConfigNat.getUserTempFolder() + "/tmp.xml";
                        String ttxt = ConfigNat.getUserTempFolder() + "/tmp.txt";
                        String tout = ConfigNat.getUserTempFolder() + "/out.txt";
                        Convertisseur c = new ConvertisseurChaine(line, txml, "UTF-8");
                        Transcodeur t = new TranscodeurNormal(txml, ttxt, "UTF-8", gestErreur);
                        Presentateur p = new PresentateurMEP(gestErreur, "UTF-8", ttxt, tout, "brailleUTF8");
                        c.convertir(gestErreur);
                        t.transcrire(gestErreur);
                        p.presenter();
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tout)));
                            System.out.println(br.readLine());
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                for (int i = 0; i < cl.fromFiles.size(); i++) {
                    gestErreur.afficheMessage("conversion de " + cl.fromFiles.get(i) + " vers " + cl.toFiles.get(i), Nat.LOG_NORMAL);
                }
                if (nat.fabriqueTranscriptions(cl.fromFiles, cl.toFiles)) {
                    nat.lanceScenario();
                } else {
                    gestErreur.afficheMessage("\n**ERREUR: certains fichiers n'existent pas " + "et ne pourront être transcrits", Nat.LOG_SILENCIEUX);
                    nat.lanceScenario();
                }
            }
        } catch (GetOptNatException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
