package org.arpenteur.common.misc.io;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.arpenteur.common.ATK_Config;
import org.arpenteur.common.misc.logging.Logging;

public final class NameBuilder {

    private static final String checkImageName(String namePict) {
        if (namePict == null) {
            return null;
        }
        String copie = namePict.toUpperCase();
        EndsWithFilter filter = new EndsWithFilter(ATK_Config.suffixeImage);
        if (filter.acceptName(copie)) {
            return namePict;
        } else {
            return forceSuffixe(namePict, ATK_Config.defaultSuffixeImage);
        }
    }

    public static String getLocaleDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_H_mm");
        df.getTimeInstance();
        String date = df.format(new Date());
        return date;
    }

    public static String forceSuffixe(String chaine, String suffixe) {
        int index = chaine.lastIndexOf(".");
        if (suffixe.indexOf(".") == -1) {
            suffixe = new String("." + suffixe);
        }
        if ((index == -1) || ((chaine.length() - index) > 4)) {
            chaine = new String(chaine + suffixe);
        } else {
            chaine = new String(chaine.substring(0, index) + suffixe);
        }
        return chaine;
    }

    public static String cutSuffixe(String chaine) {
        int index = chaine.lastIndexOf(".");
        if (index == -1) {
            chaine = new String(chaine);
        } else {
            chaine = new String(chaine.substring(0, index));
        }
        return chaine;
    }

    /**
	 * for example from D:\dev\arpenteurRoot\data\model\Scandola2006.xml return
	 * Scandola2006
	 * 
	 * @param chaine
	 * @return
	 */
    public static String getStrictFileName(String chaine) {
        String name = cutSuffixe(chaine);
        int index = name.lastIndexOf("/");
        if ((index > 0) && (index < chaine.length())) name = name.substring(index + 1);
        return name;
    }

    public static String changeAntiSlashBySlach(String chaine) {
        String name = chaine.replace('\\', '/');
        name = name.replace("//", "/");
        return name;
    }

    /**
	 * Return the url string of the camera file which the name is given in
	 * parameter. this method use the external form of the url returned by the
	 * method <code>getCameraURL</code>.
	 * 
	 * @param name
	 *            String the name of the camera
	 * @return String the url string of the camera
	 */
    public static final String getCameraURLString(String name, boolean verif) {
        return getCameraURL(name).toExternalForm();
    }

    /**
	 * Return the url of the camera which the name is given in parameter. It is
	 * searched:<br/>
	 * - For a local use: 1) In the user directory.<br/>
	 * 2) If not found, in the camera default directoy.<br/>
	 * <br/>
	 * - For a server use: 1) In the user account on the server.<br/>
	 * 2) In the default camera directory on the server.
	 * 
	 * @param name
	 *            String name of the camera.
	 * @return URL url of the camera file.
	 * @see getFileURL(String name, String dir)
	 */
    public static final URL getCameraURL(String cameraName) {
        URL url = null;
        URI uri = null;
        cameraName = forceSuffixe(cameraName, ".xml");
        String path = ATK_Config.getArpenteurCameraBank();
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += cameraName;
        uri = PathUtil.pathToURI(path);
        if (uri == null) {
            Logging.log.error("Cannot create URI from path: " + path);
            url = null;
        } else {
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    /**
	 * <pre>
	 * Return the url of the image given in parameter. The image is searched:&lt;br/&gt;
	 * -                   1) In the user directory.&lt;br/&gt;
	 *                     2) If not found, in the image default directoy.&lt;br/&gt;&lt;br/&gt;
	 * &#064;param namePict String the name of the image
	 * &#064;return URL the url of the image
	 * </pre>
	 */
    public static final URL getImageURL(String namePict, boolean verif) {
        URL url = null;
        URI uri = null;
        if (PathUtil.isAbsoluteURL(namePict)) {
            try {
                return new URL(namePict);
            } catch (MalformedURLException ex) {
                return null;
            }
        } else if (PathUtil.isAbsolutePath(namePict)) {
            try {
                return new File(namePict).toURL();
            } catch (MalformedURLException ex1) {
                return null;
            }
        }
        namePict = checkImageName(namePict);
        String path = ATK_Config.getArpenteurPhotograph();
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += namePict;
        uri = PathUtil.pathToURI(path);
        if (uri == null) {
            Logging.log.error("Cannot create URI from path: " + path);
            url = null;
        } else {
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        if (verif) {
            try {
                url.openConnection();
            } catch (IOException ex1) {
                Logging.log.error("Cannot create URI from path: " + path, ex1);
                path = "" + ATK_Config.getArpenteurHome() + ATK_Config.DATA + "/" + ATK_Config.PHOTOGRAPH + "/" + namePict;
                Logging.log.error("Trying to locate resource in default folder (" + path + ")");
                uri = PathUtil.pathToURI(path);
                if (uri == null) {
                    Logging.log.error("Cannot create URI from path: " + path);
                    url = null;
                } else {
                    try {
                        url = uri.toURL();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    url.openConnection();
                } catch (Exception e) {
                    Logging.log.error("Cannot Open URL " + url.toString());
                    url = null;
                }
            }
        }
        return url;
    }

    /**
	 * compression peut �tre 0, 2 ou 4 O correpond au repertoire thumbnail o� se
	 * trouve des vignettes de dim 200x200 2 les images sont divis�es par 2 4
	 * les images sont divis�es par 4 -1 image originale
	 * 
	 * Attention, on peut entrer avec un chaine contenant d�j� "/thumbnail" ou
	 * "/thumbnail2" ou "/thumbnail4" il faut donc supprimer la reference au
	 * r�pertoire vignette puis ajouter celui qui convient en fonction du
	 * parametre compression.
	 * 
	 * @param chaine
	 *            String
	 * @param compression
	 *            int
	 * @return String
	 */
    public static String addThumbnail(final String chaine) {
        return addThumbnail(chaine, 0);
    }

    public static String addThumbnail(final String chaine, int compression) {
        String localChaine = new String(chaine);
        String vignette = "/thumbnail";
        int addLength = 0;
        int idTh = localChaine.lastIndexOf(vignette);
        int idTh2 = localChaine.lastIndexOf(vignette + "2");
        int idTh4 = localChaine.lastIndexOf(vignette + "4");
        int index = localChaine.lastIndexOf("/");
        if (idTh4 > -1) {
            index = idTh4;
            addLength = 11;
        } else {
            if (idTh2 > -1) {
                index = idTh2;
                addLength = 11;
            } else {
                if (idTh > -1) {
                    index = idTh;
                    addLength = 10;
                }
            }
        }
        if (compression == 2) {
            vignette += "2";
        }
        if (compression == 4) {
            vignette += "4";
        }
        if (compression == -1) {
            vignette = "/";
        }
        String result = "";
        if (index == -1) {
            result = new String(localChaine);
        } else {
            String prem = localChaine.substring(0, index);
            result = new String(prem + vignette + localChaine.substring(index + addLength, localChaine.length()));
        }
        return result;
    }

    public static void main(String[] args) {
        Logging.init();
        Logging.log.println("NameBuilder Test");
    }
}
