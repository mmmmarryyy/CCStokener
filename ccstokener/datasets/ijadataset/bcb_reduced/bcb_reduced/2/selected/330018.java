package aspiration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * classe mod�lisant le t�l�chargement de fichier
 *
 */
public class FileDownloader extends Downloader {

    /**
	 * constructeur
	 * @param _target destination du telechargement
	 * @param _src url du site web
	 */
    public FileDownloader(String _target, String _src) {
        super(_target, _src);
    }

    /**
	 * sauvegarde du fichier
	 */
    public void save() {
        String racine = src.substring(7, src.length());
        String urlRelative = toRelative(url.toString());
        if (urlRelative.contains("/")) {
            String reps = urlRelative.substring(0, urlRelative.lastIndexOf("/") + 1);
            File dossiers = new File(target + "/" + racine + "/" + reps + "/");
            dossiers.mkdirs();
        }
        try {
            File fichier = new File(target + "/" + racine + "/" + urlRelative);
            byte[] donnees = new byte[2048];
            InputStream in = url.openStream();
            FileOutputStream out = new FileOutputStream(fichier);
            int i;
            System.out.println("Downloading " + fichier.toString());
            while ((i = in.read(donnees, 0, donnees.length)) != -1) out.write(donnees, 0, i);
            out.close();
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found : " + this.url.toString());
        } catch (Exception e) {
            System.out.println("Connection failed to " + this.url.toString());
        }
    }

    /**
	 * acc�s au chemin relatif d'un fichier de mani�re statique
	 * @param address adresse du fichier
	 * @param src site web � t�l�charger
	 * @param target destination du t�l�chargement
	 * @param name nom du site
	 * @return adresse relative du fichier
	 */
    public static String getFilePath(String address, String src, String target, String name) {
        String racine = src.substring(7, src.length());
        String urlRelative = addressToRelative(address, src);
        return target + "/" + name + "/" + racine + "/" + urlRelative;
    }
}
