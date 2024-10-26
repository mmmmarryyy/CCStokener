package algutil.fichier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.log4j.Logger;
import algutil.fichier.exception.CopieException;
import algutil.fichier.exception.CreationDossierException;
import algutil.fichier.exception.SuppressionException;

public class ActionsFichiers {

    private static final Logger log = Logger.getLogger(ActionsFichiers.class);

    private ActionsFichiers() {
    }

    /**
	 * Supprime le r�cursivement le r�pertoire pass� en param�tre M�thode
	 * d'appel
	 * 
	 * @throws SuppressionException
	 */
    public static void supprimerRepertoire(String chemin) throws SuppressionException {
        supprimerRepertoire(new File(chemin));
    }

    /**
	 * Supprime le r�cursivement le r�pertoire pass� en param�tre M�thode
	 * d'appel
	 * 
	 * @throws SuppressionException
	 */
    public static void supprimerRepertoire(File f) throws SuppressionException {
        if (!f.exists()) {
            throw new SuppressionException("ERREUR : Suppression du repertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire n'existe pas.");
        }
        if (!f.isDirectory()) {
            throw new SuppressionException("ERREUR : Suppression du repertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Ce n'est pas un r�pertoire.");
        }
        boolean ok = supprimerRepertoireP(f);
        if (!ok) {
            throw new SuppressionException("ERREUR : Suppression du repertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Inconnu.");
        }
    }

    /**
	 * Supprime le r�cursivement le r�pertoire pass� en param�tre M�thode de
	 * parcours
	 */
    private static boolean supprimerRepertoireP(File f) {
        boolean ok = true;
        File[] liste = f.listFiles();
        for (int i = 0; i < liste.length; i++) {
            if (liste[i].isDirectory()) {
                ok &= supprimerRepertoireP(liste[i]);
            } else {
                ok &= liste[i].delete();
            }
        }
        return ok && f.delete();
    }

    /**
	 * Supprime r�cursivement le contenu du r�pertoire pass� en param�tre.
	 * M�thode d'appel
	 * 
	 * @throws SuppressionException
	 */
    public static void supprimerContenuRepertoire(String chemin) throws SuppressionException {
        supprimerContenuRepertoire(new File(chemin));
    }

    /**
	 * Supprime r�cursivement le contenu du r�pertoire pass� en param�tre.
	 * M�thode d'appel
	 * 
	 * @throws SuppressionException
	 */
    public static void supprimerContenuRepertoire(File f) throws SuppressionException {
        if (!f.exists()) {
            throw new SuppressionException("ERREUR : Suppression du repertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire n'existe pas.");
        }
        if (!f.isDirectory()) {
            throw new SuppressionException("ERREUR : Suppression du repertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Ce n'est pas un r�pertoire.");
        }
        boolean ok = supprimerContenuRepertoireP(f);
        if (!ok) {
            throw new SuppressionException("ERREUR : Suppression du repertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Inconnu.");
        }
    }

    /**
	 * Supprime r�cursivement le contenu du r�pertoire pass� en param�tre.
	 * M�thode de parcours
	 */
    private static boolean supprimerContenuRepertoireP(File f) {
        boolean ok = true;
        File[] liste = f.listFiles();
        for (int i = 0; i < liste.length; i++) {
            if (liste[i].isDirectory()) {
                ok &= supprimerRepertoireP(liste[i]);
            } else {
                ok &= liste[i].delete();
            }
        }
        return ok;
    }

    /**
	 * Supprime le fichier
	 * 
	 * @throws SuppressionException
	 */
    public static void supprimerFichier(String cheminFichier) throws SuppressionException {
        supprimerFichier(new File(cheminFichier));
    }

    /**
	 * Supprime le fichier
	 * 
	 * @throws SuppressionException
	 */
    public static void supprimerFichier(File f) throws SuppressionException {
        if (!f.delete()) {
            throw new SuppressionException("ERREUR : Suppression du fichier '" + f.getPath() + "' impossible!\n" + "CAUSE  : Inconnu.");
        }
        log.info("(SUPPRESSION) Suppression du fichier : " + f.getPath());
    }

    /**
	 * Copie le fichier source vers le fichier destination. Ecrase le fichier
	 * destination s'il existe d�j�.
	 * 
	 * @throws CopieException
	 */
    public static void copierFichier(String cheminSource, String CheminDestination) throws CopieException {
        copierFichier(new File(cheminSource), new File(CheminDestination));
    }

    /**
	 * Copie le fichier source vers le fichier destination. Ecrase le fichier
	 * destination s'il existe d�j�.
	 * 
	 * @throws CopieException
	 */
    public static void copierFichier(File source, File destination) throws CopieException {
        copierFichier(source, destination, true);
    }

    public static void copierFichier(URL url, File destination) throws CopieException, IOException {
        if (destination.exists()) {
            throw new CopieException("ERREUR : Copie du fichier '" + url.getPath() + "' vers '" + destination.getPath() + "' impossible!\n" + "CAUSE  : Le fichier destination existe d�j�.");
        }
        URLConnection urlConnection = url.openConnection();
        InputStream httpStream = urlConnection.getInputStream();
        FileOutputStream destinationFile = new FileOutputStream(destination);
        byte buffer[] = new byte[512 * 1024];
        int nbLecture;
        while ((nbLecture = httpStream.read(buffer)) != -1) {
            destinationFile.write(buffer, 0, nbLecture);
        }
        log.debug("(COPIE) Copie du fichier : " + url.getPath() + " --> " + destination.getPath());
        httpStream.close();
        destinationFile.close();
    }

    /**
	 * Copie le fichier source vers le fichier destination.
	 * 
	 * @throws CopieException
	 */
    public static void copierFichier(String cheminSource, String CheminDestination, boolean ecraser) throws CopieException {
        copierFichier(new File(cheminSource), new File(CheminDestination), ecraser);
    }

    /**
	 * Copie le fichier source vers le fichier destination.
	 * 
	 * @throws CopieException
	 */
    public static void copierFichierVersRepertoire(String cheminFichier, String cheminRep) throws CopieException {
        copierFichier(new File(cheminFichier), new File(cheminRep), true);
    }

    /**
	 * Copie le fichier source vers le fichier destination.
	 * 
	 * @throws CopieException
	 */
    public static void copierFichierVersRepertoire(File fichier, File repDest) throws CopieException {
        copierFichier(fichier, new File(repDest.getPath() + File.separator + fichier.getName()), true);
    }

    /**
	 * Copie le fichier source vers le fichier destination
	 * 
	 * @throws CopieException
	 */
    public static void copierFichier(File source, File destination, boolean ecraser) throws CopieException {
        if (!source.exists()) {
            throw new CopieException("ERREUR : Copie du fichier '" + source.getPath() + "' impossible!\n" + "CAUSE  : Le fichier n'existe pas.");
        }
        if (source.isDirectory()) {
            throw new CopieException("ERREUR : Copie du fichier '" + source.getPath() + "' impossible!\n" + "CAUSE  : Ce n'est pas un fichier.");
        }
        if (source.equals(destination)) {
            throw new CopieException("ERREUR : Copie du fichier '" + source.getPath() + "' vers '" + destination.getPath() + "' impossible!\n" + "CAUSE  : Le fichier source est le m�me que celui de destination.");
        }
        if (!ecraser && destination.exists()) {
            throw new CopieException("ERREUR : Copie du fichier '" + source.getPath() + "' vers '" + destination.getPath() + "' impossible!\n" + "CAUSE  : Le fichier destination existe d�j�.");
        }
        FileInputStream sourceFile = null;
        FileOutputStream destinationFile = null;
        try {
            destination.createNewFile();
            sourceFile = new FileInputStream(source);
            destinationFile = new FileOutputStream(destination);
            byte buffer[] = new byte[512 * 1024];
            int nbLecture;
            while ((nbLecture = sourceFile.read(buffer)) != -1) {
                destinationFile.write(buffer, 0, nbLecture);
            }
            log.debug("(COPIE) Copie du fichier : " + source.getPath() + " --> " + destination.getPath());
        } catch (FileNotFoundException f) {
            throw new CopieException("ERREUR : Copie du fichier '" + source.getPath() + "' vers '" + destination.getPath() + "' impossible!", f);
        } catch (IOException e) {
            throw new CopieException("ERREUR : Copie du fichier '" + source.getPath() + "' vers '" + destination.getPath() + "' impossible!", e);
        } finally {
            try {
                sourceFile.close();
            } catch (Exception e) {
            }
            try {
                destinationFile.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Copie le repertoire repSource dans le r�pertoire repDestination. Si un
	 * repertoire portant le m�me nom que repSource existe d�j�, une exception
	 * est lev�e.
	 * 
	 * @throws CopieException
	 */
    public static void copierRepertoireVers(String repSource, String repDestination) throws CopieException {
        copierRepertoireVers(new File(repSource), new File(repDestination));
    }

    /**
	 * Copie le repertoire repSource dans le r�pertoire repDestination. Si un
	 * repertoire portant le m�me nom que repSource existe d�j�, une exception
	 * est lev�e.
	 * 
	 * @throws CopieException
	 */
    public static void copierRepertoireVers(File repSource, File repDestination) throws CopieException {
        if (!repDestination.exists()) {
            throw new CopieException("ERREUR : Copie du r�pertoire '" + repSource.getPath() + "' dans '" + repDestination.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire '" + repDestination.getPath() + "' n'existe pas.");
        }
        if (!repDestination.isDirectory()) {
            throw new CopieException("ERREUR : Copie du r�pertoire '" + repSource.getPath() + "' dans '" + repDestination.getPath() + "' impossible!\n" + "CAUSE  : Le fichier '" + repDestination.getPath() + "' n'est pas un r�pertoire.");
        }
        File f = new File(repDestination.getPath() + File.separator + repSource.getName());
        if (f.exists()) {
            throw new CopieException("ERREUR : Copie du r�pertoire '" + repSource.getPath() + "' dans '" + repDestination.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire '" + f.getPath() + "' existe d�j�.");
        }
        if (!f.mkdir()) {
            throw new CopieException("ERREUR : Cr�ation du r�pertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : inconnu.");
        }
        copierContenuRepertoire(repSource, f);
    }

    /**
	 * Copie le contenu du r�pertoire s dans le r�pertoire d. M�thode d'appel.
	 * 
	 * @throws CopieException
	 */
    public static void copierContenuRepertoire(String s, String d) throws CopieException {
        copierContenuRepertoireP(new File(s), new File(d));
    }

    /**
	 * Copie le contenu du r�pertoire s dans le r�pertoire d M�thode d'appel.
	 * 
	 * @throws CopieException
	 */
    public static void copierContenuRepertoire(File s, File d) throws CopieException {
        copierContenuRepertoire(s, d, null, null);
    }

    public static void copierContenuRepertoire(File s, File d, String prefixe, String suffixe) throws CopieException {
        if (!s.exists()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + s.getPath() + "' dans '" + d.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire '" + s.getPath() + "' n'existe pas.");
        }
        if (!s.isDirectory()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + s.getPath() + "' dans '" + d.getPath() + "' impossible!\n" + "CAUSE  : Le fichier '" + s.getPath() + "' n'est pas un r�pertoire.");
        }
        if (!d.exists()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + s.getPath() + "' dans '" + d.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire '" + d.getPath() + "' n'existe pas.");
        }
        if (!d.isDirectory()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + s.getPath() + "' dans '" + d.getPath() + "' impossible!\n" + "CAUSE  : Le fichier '" + d.getPath() + "' n'est pas un r�pertoire.");
        }
        copierContenuRepertoireP(s, d, prefixe, suffixe);
    }

    /**
	 * Copie le contenu du r�pertoire s dans le r�pertoire d M�thode de
	 * parcours.
	 * 
	 * @throws CopieException
	 */
    private static void copierContenuRepertoireP(File s, File d) throws CopieException {
        copierContenuRepertoireP(s, d, null, null);
    }

    /**
	 * Copie le contenu du r�pertoire s dans le r�pertoire d M�thode de
	 * parcours.
	 * 
	 * @throws CopieException
	 */
    private static void copierContenuRepertoireP(File s, File d, String prefixe, String suffixe) throws CopieException {
        if (prefixe == null) {
            prefixe = "";
        }
        if (suffixe == null) {
            suffixe = "";
        }
        File[] fichiers = s.listFiles();
        for (int i = 0; i < fichiers.length; i++) {
            File fichierS = fichiers[i];
            if (fichierS.isDirectory()) {
                File fichierD = new File(d + File.separator + fichierS.getName());
                if (!fichierD.mkdir()) {
                    throw new CopieException("ERREUR : Cr�ation du r�pertoire '" + fichierD.getPath() + "' impossible!\n" + "CAUSE  : inconnu.");
                } else {
                    copierContenuRepertoireP(fichierS, fichierD);
                }
            } else {
                File fichierD = new File(d + File.separator + prefixe + fichierS.getName() + suffixe);
                copierFichier(fichierS, fichierD);
            }
        }
    }

    /**
	 * On deplace le fichier si le d�placements � l'int�rieur d'un disque
	 * physique. Sinon appelle la m�thode de copie. Si le fichier destination
	 * existe d�j� il sera ecras�.
	 * 
	 * @throws CopieException
	 * @throws SuppressionException
	 */
    public static void deplacerFichier(String CheminSource, String cheminDestination) throws CopieException, SuppressionException {
        deplacerFichier(new File(CheminSource), new File(cheminDestination));
    }

    /**
	 * On deplace le fichier si le d�placements � l'int�rieur d'un disque
	 * physique. Sinon appelle la m�thode de copie. Si le fichier destination
	 * existe d�j� il sera ecras�.
	 * 
	 * @throws CopieException
	 * @throws SuppressionException
	 */
    public static void deplacerFichier(File source, File destination) throws CopieException, SuppressionException {
        deplacerFichier(source, destination, true);
    }

    /**
	 * On deplace le fichier si le d�placements � l'int�rieur d'un disque
	 * physique. Sinon appelle la m�thode de copie. Si le fichier destination
	 * existe d�j� il sera ecras�.
	 * 
	 * @throws CopieException
	 * @throws SuppressionException
	 */
    public static void deplacerFichier(String CheminSource, String cheminDestination, boolean ecraser) throws CopieException, SuppressionException {
        deplacerFichier(new File(CheminSource), new File(cheminDestination), ecraser);
    }

    /**
	 * On deplace le fichier si le d�placements � l'int�rieur d'un disque
	 * physique. Sinon appelle la m�thode de copie.
	 * 
	 * @throws CopieException
	 * @throws SuppressionException
	 */
    public static void deplacerFichier(File source, File destination, boolean ecraser) throws CopieException, SuppressionException {
        log.info("(DEPLACEMENT) " + source.getPath() + " --> " + destination.getPath());
        boolean result = source.renameTo(destination);
        if (!result) {
            copierFichier(source, destination, ecraser);
            supprimerFichier(source);
        }
    }

    /**
	 * D�place le fichier CheminFichier dans le r�pertoire cheminRep.
	 * 
	 * @throws SuppressionException
	 * @throws CopieException
	 */
    public static void deplacerFichierVersRepertoire(String CheminFichier, String cheminRep) throws CopieException, SuppressionException {
        deplacerFichierVersRepertoire(new File(CheminFichier), new File(cheminRep));
    }

    /**
	 * D�place le fichier CheminFichier dans le r�pertoire cheminRep.
	 * 
	 * @throws SuppressionException
	 * @throws CopieException
	 */
    public static void deplacerFichierVersRepertoire(File fichier, File rep) throws CopieException, SuppressionException {
        deplacerFichier(fichier, new File(rep.getPath() + File.separator + fichier.getName()));
    }

    /**
	 * Deplacer le r�pertoire source dans le r�pertoire destination. Si dans le
	 * r�pertoire destination, il existe un repertoire avec le m�me nom que
	 * source, une exception est lev�e.
	 * 
	 * @throws SuppressionException
	 * @throws CopieException
	 */
    public static void deplacerRepertoireVers(String source, String dest) throws CopieException, SuppressionException {
        deplacerRepertoireVers(new File(source), new File(dest));
    }

    /**
	 * Deplacer le r�pertoire source dans le r�pertoire destination. Si dans le
	 * r�pertoire destination, il existe un repertoire avec le m�me nom que
	 * source, une exception est lev�e.
	 * 
	 * @throws CopieException
	 * @throws SuppressionException
	 */
    public static void deplacerRepertoireVers(File source, File dest) throws CopieException, SuppressionException {
        copierRepertoireVers(source, dest);
        supprimerRepertoire(source);
    }

    public static void creerDossier(File f) throws CreationDossierException {
        if (f.exists()) {
            throw new CreationDossierException("ERREUR : Creation du r�pertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Le dossier existe deja.");
        }
        if (!f.mkdir()) {
            throw new CreationDossierException("ERREUR : Creation du r�pertoire '" + f.getPath() + "' impossible!\n" + "CAUSE  : Inconnue.");
        } else {
            System.out.println("Creation du dossier : '" + f.getPath() + "' reussi.");
        }
    }

    public static void creerDossier(String path) throws CreationDossierException {
        creerDossier(new File(path));
    }

    public static void deplacerRepertoire(String pathRepS, String pathRrepD) throws CopieException, SuppressionException, CreationDossierException {
        deplacerRepertoire(new File(pathRepS), new File(pathRrepD));
    }

    public static void deplacerRepertoire(File repS, File repD) throws CopieException, SuppressionException, CreationDossierException {
        if (!repS.exists()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + repS.getPath() + "' dans '" + repD.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire '" + repS.getPath() + "' n'existe pas.");
        }
        if (!repS.isDirectory()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + repS.getPath() + "' dans '" + repD.getPath() + "' impossible!\n" + "CAUSE  : Le fichier '" + repS.getPath() + "' n'est pas un r�pertoire.");
        }
        if (repD.exists()) {
            throw new CopieException("ERREUR : Copie du contenu du r�pertoire '" + repS.getPath() + "' dans '" + repD.getPath() + "' impossible!\n" + "CAUSE  : Le r�pertoire destination '" + repD.getPath() + "' existe deja.");
        }
        creerDossier(repD);
        copierContenuRepertoireP(repS, repD);
        supprimerRepertoire(repS);
    }

    public static void creerFichierTexte(File f, String contenu) throws IOException {
        FileWriter fw = new FileWriter(f);
        PrintWriter writer = new PrintWriter(fw, true);
        writer.print(contenu);
        writer.close();
        fw.close();
    }

    public static void creerFichierTexte(File f, List<String> lines) throws IOException {
        FileWriter fw = new FileWriter(f);
        PrintWriter writer = new PrintWriter(fw, true);
        for (int i = 0; i < lines.size(); i++) {
            writer.print(lines.get(i) + "\n");
        }
        writer.close();
        fw.close();
    }
}
