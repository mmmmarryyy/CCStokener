package fr.cpbrennestt.presentation.utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.swing.ImageIcon;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import fr.cpbrennestt.presentation.display.frontal.DbAnniversaire;

public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class);

    public static final String LICENCE_A_REMPLACER = "_LICENCE_";

    public static final String URL_FFTT_CLT_H = "http://www.fftt.com/sportif/pclassement/php3/FFTTfi.php3?session=precision%3D" + LICENCE_A_REMPLACER + "%26reqid%3D200&cler=";

    public static final String URL_FFTT_CLT_F = "http://www.fftt.com/sportif/pclassement/php3/FFTTfi.php3?session=precision%3D" + LICENCE_A_REMPLACER + "%26reqid%3D300&cler=";

    public static final String URL_FFTT_CLER = "http://www.fftt.com/sportif/pclassement/php3/FFTTfo.php3?Menu=J2";

    public static String formatDate(Date date, String DatePattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(DatePattern);
        return sdf.format(date);
    }

    public static Date stringToDate(String sDate, String sFormat) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(sFormat);
        return sdf.parse(sDate);
    }

    private Utils() {
    }

    /**
	 * estDateValide : Teste si une date est valide
	 * 
	 * @param jour
	 *            Jour de la date � valider
	 * @param mois
	 *            Mois de la date � valider
	 * @param annee
	 *            Ann�e de la date � valider
	 * @return vrai si la date est valide, faux sinon
	 */
    public static boolean estDateValide(int jour, int mois, int annee) {
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.set(annee, mois, jour);
        try {
            c.getTime();
        } catch (IllegalArgumentException iAE) {
            return false;
        }
        return true;
    }

    /**
	 * estDateValide : Teste si une date de mutation ne date pas de plus d'un an
	 * *
	 * 
	 * @param jour
	 *            Jour de la date � valider
	 * @param mois
	 *            Mois de la date � valider
	 * @param annee
	 *            Ann�e de la date � valider
	 * @return vrai si la date est valide, faux sinon
	 */
    @SuppressWarnings("deprecation")
    public static boolean estDateValideMutation(int jour, int mois, int annee) {
        Date aujourdHui = new Date();
        Date mutation = new Date(annee + 1, mois, jour);
        return aujourdHui.before(mutation);
    }

    /**
	 * Test si une date est bien avant la date d'aujourd'hui
	 * 
	 * @param jour
	 * @param mois
	 * @param annee
	 * @return
	 */
    @SuppressWarnings("deprecation")
    public static boolean estDateValideToday(int jour, int mois, int annee) {
        Date aujourdHui = new Date();
        Date date = new Date(annee, mois, jour);
        return aujourdHui.after(date);
    }

    public static boolean estEmailValide(String email) {
        Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
        Matcher m = p.matcher(email);
        boolean matchFound = m.matches();
        return matchFound;
    }

    public static String transformerPourZip(String nom) {
        if (StringUtils.isNotEmpty(nom)) {
            return removeAccent(nom.replace('/', '_').replace(".", ""));
        }
        return "";
    }

    /**
	 * enregistrerFichier : enregistre une image depuis un fichier de type
	 * 'image'
	 * 
	 * @param fileName
	 *            : nom du fichier
	 * @param file
	 *            : fichier correspondant
	 * @return le nom de la photo enregistr�e.
	 */
    public static File enregistrerFichier(String fileName, File file, String path, String fileMime) throws Exception {
        if (file != null) {
            try {
                HttpServletRequest request = ServletActionContext.getRequest();
                HttpSession session = request.getSession();
                String pathFile = session.getServletContext().getRealPath(path) + File.separator + fileName;
                File outfile = new File(pathFile);
                String[] nomPhotoTab = fileName.split("\\.");
                String extension = nomPhotoTab[nomPhotoTab.length - 1];
                StringBuffer pathResBuff = new StringBuffer(nomPhotoTab[0]);
                for (int i = 1; i < nomPhotoTab.length - 1; i++) {
                    pathResBuff.append(".").append(nomPhotoTab[i]);
                }
                String pathRes = pathResBuff.toString();
                String nomPhoto = fileName;
                for (int i = 0; !outfile.createNewFile(); i++) {
                    nomPhoto = pathRes + "_" + +i + "." + extension;
                    pathFile = session.getServletContext().getRealPath(path) + File.separator + nomPhoto;
                    outfile = new File(pathFile);
                }
                logger.debug(" enregistrerFichier - Enregistrement du fichier : " + pathFile);
                FileChannel in = null;
                FileChannel out = null;
                try {
                    in = new FileInputStream(file).getChannel();
                    out = new FileOutputStream(outfile).getChannel();
                    in.transferTo(0, in.size(), out);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                        }
                    }
                }
                return outfile;
            } catch (IOException e) {
                logger.error("Erreur lors de l'enregistrement de l'image ", e);
                throw new Exception("Erreur lors de l'enregistrement de l'image ");
            }
        }
        return null;
    }

    /**
	 * downloadFileUrl : t�l�charge une image gr�ce � son adresse URL
	 * 
	 * @param adresse
	 *            : URL de l'image � t�l�charger
	 * @param nomImage
	 *            : nom de l'image � enregistrer
	 */
    private void downloadFileUrl(String adresse, String nomImage) {
        BufferedReader reader = null;
        FileOutputStream fos = null;
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        try {
            File dest = new File(session.getServletContext().getRealPath("/images_articles/") + File.separator + nomImage);
            FileInputStream in = new FileInputStream(dest);
            reader = new BufferedReader(new InputStreamReader(in));
            fos = new FileOutputStream(dest);
            byte[] buff = new byte[1024];
            int l = in.read(buff);
            while (l > 0) {
                fos.write(buff, 0, l);
                l = in.read(buff);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * @param path
	 *            chemin du dossier � cr�er
	 * @throws Exception
	 */
    public static File creerDossier(String path) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        File dossier = new File(session.getServletContext().getRealPath(path));
        if (!dossier.exists()) {
            dossier.mkdir();
            dossier.setWritable(true);
        }
        return dossier;
    }

    /**
	 * enregistrerFichier : enregistre une image depuis un fichier de type
	 * 'image'
	 * 
	 * @param fileName
	 *            : nom du fichier
	 * @param file
	 *            : fichier correspondant
	 * @return le nom de la photo enregistr�e.
	 */
    public static File enregistrerMiniature(String fileName, File file, String pathOut, String fileMime, int height, int width) throws Exception {
        try {
            logger.debug("DEBUT - enregistrerMiniature " + fileName);
            logger.debug("FileMime " + fileMime);
            logger.debug("File " + file);
            if (file != null && fileMime.contains("image")) {
                logger.debug("image");
                HttpServletRequest request = ServletActionContext.getRequest();
                HttpSession session = request.getSession();
                logger.debug("cr�ation dossier");
                creerDossier(pathOut);
                String pathFileOut = session.getServletContext().getRealPath(pathOut) + File.separator + fileName;
                logger.debug("Path miniature " + pathFileOut);
                File outfile = new File(pathFileOut);
                logger.debug("La miniature existe : " + outfile.exists());
                if (!outfile.exists()) {
                    double ratio = redimImage(file.getAbsolutePath(), height, width);
                    logger.debug("ratio : " + ratio);
                    BufferedImage im = ImageIO.read(file);
                    logger.debug("im : " + im);
                    FileOutputStream fileContents = new FileOutputStream(file.getAbsolutePath());
                    logger.debug("fileContent" + fileContents);
                    BufferedImage imOut = scale(im, ratio);
                    String[] nomPhotoTab = fileName.split("\\.");
                    String extension = nomPhotoTab[nomPhotoTab.length - 1];
                    logger.debug(" enregistrerFichier - Enregistrement de la miniature : " + pathFileOut);
                    ImageIO.write(imOut, extension, outfile);
                    fileContents.close();
                    logger.debug("FIN - enregistrerMiniature");
                    return outfile;
                } else {
                    logger.debug("FIN - enregistrerMiniature");
                    return null;
                }
            } else {
                logger.debug("FIN - enregistrerMiniature");
                return null;
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'enregistrement de l'image ", e);
            throw new Exception("Erreur lors de l'enregistrement de l'image ");
        }
    }

    /**
	 * Fontion affichant les images suivant une taille max
	 */
    public static double redimImage(String path, int heightRef, int widthRef) {
        try {
            logger.debug("redimImage");
            logger.debug("heightRef : " + heightRef);
            logger.debug("widthRef : " + widthRef);
            ImageIcon ic = new ImageIcon(path);
            logger.debug("ic : " + ic);
            double height = ic.getIconHeight();
            logger.debug("height : " + height);
            double width = ic.getIconWidth();
            logger.debug("width : " + width);
            if (height < heightRef && width < widthRef) {
                return 1;
            } else {
                double ratio;
                double ratio1 = ((heightRef * 100) / height);
                double ratio2 = ((widthRef * 100) / width);
                if (ratio1 < ratio2) ratio = ratio1; else ratio = ratio2;
                return ratio / 100;
            }
        } catch (Exception e) {
            logger.debug("erreur : ", e);
            return 1;
        }
    }

    /**
	 * Effectue une homoth�tie de l'image.
	 * 
	 * @param bi
	 *            l'image.
	 * @param scaleValue
	 *            la valeur de l'homoth�tie.
	 * @return une image r�duite ou agrandie.
	 * 
	 */
    public static BufferedImage scale(BufferedImage bi, double scaleValue) {
        logger.debug("scale");
        AffineTransform tx = new AffineTransform();
        tx.scale(scaleValue, scaleValue);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage biNew = new BufferedImage((int) (bi.getWidth() * scaleValue), (int) (bi.getHeight() * scaleValue), bi.getType());
        return op.filter(bi, biNew);
    }

    /**
	 * supprimerFichier : t�l�charge une image depuis un fichier de type 'image'
	 * 
	 * @param nomPhoto
	 *            : nom de la photo
	 */
    public static void supprimerFichier(String nomPhoto, String path) throws Exception {
        try {
            HttpServletRequest request = ServletActionContext.getRequest();
            HttpSession session = request.getSession();
            String pathFile = session.getServletContext().getRealPath(path) + File.separator + nomPhoto;
            logger.debug("supprimerFichier - Suppression du fichier : " + pathFile);
            File outfile = new File(pathFile);
            outfile.setReadable(true);
            outfile.setWritable(true);
            FileDeleteStrategy fileDeleteStrategy = FileDeleteStrategy.FORCE;
            fileDeleteStrategy.delete(outfile);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'image ", e);
            throw new Exception("Erreur lors de la suppression de l'image ");
        }
    }

    public static boolean supprimerDossier(File path) {
        boolean resultat = true;
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    resultat &= supprimerDossier(files[i]);
                    logger.debug("Suppression du dossier " + files[i].getName() + " : " + resultat);
                } else {
                    resultat &= files[i].delete();
                    logger.debug("Suppression du fichier " + files[i].getName() + " : " + resultat);
                }
            }
        }
        resultat &= path.delete();
        logger.debug("Suppression du dossier " + path.getName() + " : " + resultat);
        return (resultat);
    }

    public static String lirePageWeb(String url) {
        BufferedInputStream bisTemp = null;
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) (u.openConnection());
            HttpURLConnection connTemp = (HttpURLConnection) (u.openConnection());
            InputStream fluxTemp = connTemp.getInputStream();
            InputStream flux = conn.getInputStream();
            bisTemp = new BufferedInputStream(fluxTemp);
            int taille = 0;
            while (bisTemp.read() != -1) {
                taille++;
            }
            char[] donnees = new char[taille];
            int octetsLus = 0;
            int deplacement = 0;
            float alreadyRead = 0;
            Reader reader = new InputStreamReader(flux, "windows-1252");
            while (deplacement < taille) {
                octetsLus = reader.read(donnees, deplacement, donnees.length - deplacement);
                alreadyRead = alreadyRead + octetsLus;
                if (octetsLus == -1) {
                    break;
                }
                deplacement += octetsLus;
            }
            String monString = new String(donnees);
            flux.close();
            reader.close();
            return monString;
        } catch (Exception e) {
            logger.error("ERREUR : " + e);
            return "";
        } finally {
            if (bisTemp != null) {
                try {
                    bisTemp.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void writeToFile(URL url, File file) throws IOException, FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            InputStream is = url.openStream();
            try {
                byte[] buf = new byte[2048];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
            } finally {
                is.close();
            }
        } finally {
            fos.close();
        }
    }

    public static String readFile(File file) {
        try {
            FileReader reader = new FileReader(file);
            try {
                StringBuffer buffer = new StringBuffer();
                char[] cbuf = new char[2048];
                int len;
                while ((len = reader.read(cbuf)) > 0) {
                    buffer.append(cbuf, 0, len);
                }
                return buffer.toString();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double calculPoints(int pt1, int pt2, boolean victoire) {
        boolean unSup2 = true;
        if (pt1 < pt2) {
            unSup2 = false;
        }
        int diff = Math.abs(pt2 - pt1);
        if (0 <= diff && diff <= 24) {
            if (victoire) return 6; else return -5;
        } else if (25 <= diff && diff <= 49) {
            if (victoire && unSup2) return 5.5; else if (!victoire && !unSup2) return -4.5; else if (victoire && !unSup2) return 7; else return -6;
        } else if (50 <= diff && diff <= 99) {
            if (victoire && unSup2) return 5; else if (!victoire && !unSup2) return -4; else if (victoire && !unSup2) return 8; else return -7;
        } else if (100 <= diff && diff <= 149) {
            if (victoire && unSup2) return 4; else if (!victoire && !unSup2) return -3; else if (victoire && !unSup2) return 10; else return -8;
        } else if (150 <= diff && diff <= 199) {
            if (victoire && unSup2) return 3; else if (!victoire && !unSup2) return -2; else if (victoire && !unSup2) return 13; else return -10;
        } else if (200 <= diff && diff <= 299) {
            if (victoire && unSup2) return 2; else if (!victoire && !unSup2) return -1; else if (victoire && !unSup2) return 17; else return -12.5;
        } else if (300 <= diff && diff <= 399) {
            if (victoire && unSup2) return 1; else if (!victoire && !unSup2) return -0.5; else if (victoire && !unSup2) return 22; else return -16;
        } else if (400 <= diff && diff <= 499) {
            if (victoire && unSup2) return 0.5; else if (!victoire && !unSup2) return 0; else if (victoire && !unSup2) return 28; else return -20;
        } else {
            if (victoire && unSup2) return 0; else if (!victoire && !unSup2) return 0; else if (victoire && !unSup2) return 40; else return -25;
        }
    }

    public static String obtenirPointMois(String page) throws Exception {
        Pattern pattern = Pattern.compile("<td width='99' align='center' bgcolor=#EBEBEB>([0-9]{3,4})</td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String obtenirPointDebutSaison(String page) {
        Pattern pattern = Pattern.compile("<td width='115' align='center' bgcolor=#EBEBEB>([0-9]{3,4})</td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String obtenirPointOfficiel(String page) {
        Pattern pattern = Pattern.compile("<td width='152' align='center' bgcolor=#EBEBEB>([0-9]{3,4})</td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String obtenirNom(String page) {
        Pattern pattern = Pattern.compile("<p align='center'><font size='3' color=#FFFFFF><B>(.*) (.*)</B></font></td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String obtenirCler(String page) {
        Pattern pattern = Pattern.compile("<input type='hidden' name='cler' value='(.*)'>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String obtenirPrenom(String page) {
        Pattern pattern = Pattern.compile("<p align='center'><font size='3' color=#FFFFFF><B>(.*) (.*)</B></font></td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "";
    }

    public static String obtenirClt(String page) {
        Pattern pattern = Pattern.compile("<td width='115' align='center' bgcolor=#D2E9FF>(.*)</td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String obtenirCategorie(String page) {
        Pattern pattern = Pattern.compile("<td width='115' align='center' bgcolor=#EBEBEB>(.*)</td>");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public static String affichePoints(double points) {
        DecimalFormat df = new DecimalFormat("0.#");
        String res = df.format(points);
        if (points > 0) {
            res = "+" + res;
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(lirePageWeb(args[0]));
    }

    public static String formatTelephone(String numero, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(numero.substring(0, 2));
        buffer.append(delimiter);
        buffer.append(numero.substring(2, 4));
        buffer.append(delimiter);
        buffer.append(numero.substring(4, 6));
        buffer.append(delimiter);
        buffer.append(numero.substring(6, 8));
        buffer.append(delimiter);
        buffer.append(numero.substring(8, 10));
        return buffer.toString();
    }

    public static String formatPrefixeTelephone(String numero) {
        if (numero.length() == 10) {
            return numero;
        } else {
            StringBuffer buffer = new StringBuffer();
            for (int i = numero.length(); i < 10; i++) {
                buffer.append("0");
            }
            buffer.append(numero);
            return buffer.toString();
        }
    }

    public static String removeAccent(String source) {
        return Normalizer.normalize(source, Normalizer.Form.NFD).replaceAll("[̀-ͯ]", "");
    }

    public static String saisonToString(int saison) {
        StringBuffer saisonString = new StringBuffer();
        saisonString.append(saison);
        saisonString.append("/");
        saisonString.append(saison + 1);
        return saisonString.toString();
    }

    public static List<DbAnniversaire> translateAnniversaire(DbAnniversaire[][] tabAnniversaires, int jourDebut, int moisDebut) {
        List<DbAnniversaire> anniversaires = new LinkedList<DbAnniversaire>();
        boolean init = true;
        for (int i = moisDebut - 1; ((i % 12) != (moisDebut - 1)) || (init); i++) {
            init = false;
            for (int j = 0; j < 31; j++) {
                DbAnniversaire anniv = tabAnniversaires[(i % 12)][j];
                if (anniv != null) {
                    anniversaires.add(anniv);
                }
            }
        }
        return anniversaires;
    }
}
