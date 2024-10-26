package org.hironico.dbtool2.sqleditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Cette classe modélise une liste de mots clés qui puisse étre chargée depuis un fichier.
 * @version $Rev: 1.1 $ 
 * @author $Author: hironico $
 * @since 0.0.2 
 **/
public class KeywordList extends LinkedList<String> {

    private static final long serialVersionUID = 5794126674771919751L;

    private static final Logger logger = Logger.getLogger("org.hironico.dbtool2.sqleditor");

    private File inputFile;

    /**
	 * Construite cette liste de mots clés sans charger le fichier en entrée.
	 * @param inputFile File représetant l'abstraction du fichiers de mots clés à charger.
	 * @since 0.0.2
	 */
    public KeywordList(File inputFile) {
        super();
        this.inputFile = inputFile;
    }

    /**
     * Constructeur par défaut qui ne charge aucun fichier.
     * @since 0.0.9
     */
    public KeywordList() {
        super();
    }

    /**
     * Permet de charger une resource du classpath dans la liste de mot clef.
     * @param resourcePath le chemin de la resource à charger.
     * @return true si le chargement est effectué et false sinon.
     * @since 0.0.9
     */
    public boolean loadResource(String resourcePath) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
            if (url == null) {
                logger.error("Cannot find the resource named: '" + resourcePath + "'. Failed to load the keyword list.");
                return false;
            }
            InputStreamReader isr = new InputStreamReader(url.openStream());
            BufferedReader br = new BufferedReader(isr);
            String ligne = br.readLine();
            while (ligne != null) {
                if (!contains(ligne.toUpperCase())) addLast(ligne.toUpperCase());
                ligne = br.readLine();
            }
            return true;
        } catch (IOException ioe) {
            logger.log(Level.ERROR, "Cannot load default SQL keywords file.", ioe);
        }
        return false;
    }

    /**
     * @return true si le fichier a été chargé correctement et false dans le cas contraire.
     * @since 0.0.2
     */
    public boolean loadFile() {
        if (inputFile.isDirectory()) return false;
        try {
            BufferedReader in = new BufferedReader(new FileReader(inputFile));
            String ligne = "";
            while ((ligne = in.readLine()) != null) {
                addLast(ligne.toUpperCase());
            }
            in.close();
            return true;
        } catch (IOException ioe) {
            logger.error("Cannot load kaywords file.", ioe);
            return false;
        }
    }
}
