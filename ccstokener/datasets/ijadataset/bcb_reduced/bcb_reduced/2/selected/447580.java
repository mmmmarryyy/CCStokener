package eu.digmap.thumbnails;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * This class provides methods for the handling of exceptional cases in the
 * construction of image thumbnails from web sites related to historical
 * cartography. For now, the provided methods are based on hand-coded rules,
 * although future versions of the software will look at automated methods for
 * doing this.
 * 
 * The idea is to generate thumbnails with basis on the map image that in
 * displayed on an HTML page, instead of doing a thumbnail as a web browser
 * would render the page. This way, we can better deal with the case of several
 * web known map libraries.
 * 
 * @author Bruno Martins (bgmartins@gmail.com)
 * @version 0.1
 */
public class HTMLSpecialCasesThumbnailMaker extends AbstractThumbnailMaker {

    private ImageThumbnailMaker imageThumbnailMaker = new ImageThumbnailMaker();

    protected BufferedImage getImage() throws Exception {
        BufferedImage img = null;
        if ((img = handleNLAException()) != null) return img;
        if ((img = handleRaremapsException()) != null) return img;
        if ((img = handleICCException()) != null) return img;
        if ((img = handleFirenzeException()) != null) return img;
        if ((img = handleGMUException()) != null) return img;
        if ((img = handleFCLAException()) != null) return img;
        if ((img = handleBNException()) != null) return img;
        if ((img = handleKBRException()) != null) return img;
        if ((img = handleGOVException()) != null) return img;
        if ((img = handleBibDigHispanica()) != null) return img;
        return img;
    }

    /** Handle the exception case of the National Library of Australia */
    protected BufferedImage handleNLAException() {
        if (params.uri.startsWith("http://nla.gov.au/nla.map")) try {
            params.uri = params.uri + "-v.jpg";
            URL url = new URL(params.uri);
            URLConnection connection = url.openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
        } else if (params.uri.startsWith("http://www.nla.gov.au/apps/cdview?pi=nla.map")) try {
            params.uri = "http://nla.gov.au/nla.map" + params.uri.substring(params.uri.indexOf('-'), params.uri.lastIndexOf('-')) + "-v.jpg";
            URL url = new URL(params.uri);
            URLConnection connection = url.openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
        }
        return null;
    }

    private BufferedImage processNewUri(URLConnection connection) throws IOException {
        ThumbnailParams newParams = new ThumbnailParams(this.params.uri, connection.getInputStream(), -1, -1, (byte) this.params.transparency);
        imageThumbnailMaker.setParams(newParams);
        return scaleImage(imageThumbnailMaker.getImage());
    }

    /** Handle the exception case of the Raremaps website */
    protected BufferedImage handleRaremapsException() {
        if (params.uri.startsWith("http://www.raremaps.com/cgi-bin/gallery.pl/detail/")) try {
            params.uri = params.uri.replace("cgi-bin/gallery.pl/detail", "maps/medium");
            URL url = new URL(params.uri);
            URLConnection connection = url.openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the Instituto Cartografico Catalunha */
    protected BufferedImage handleICCException() {
        if (params.uri.startsWith("http://vacani.icc.cat") || params.uri.startsWith("http://louisdl.louislibraries.org")) try {
            params.uri = params.uri.replace("cdm4/item_viewer.php", "cgi-bin/getimage.exe") + "&DMSCALE=3";
            params.uri = params.uri.replace("/u?", "cgi-bin/getimage.exe?CISOROOT=").replace(",", "&CISOPTR=") + "&DMSCALE=3";
            URL url = new URL(params.uri);
            URLConnection connection = url.openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the Biblioteca Nazionale Centrale di Firenze */
    protected BufferedImage handleFirenzeException() {
        if (params.uri.indexOf("bncf.firenze.sbn.it") != -1) try {
            params.uri = params.uri.replace("http://opac.bncf.firenze.sbn.it/mdigit/jsp/mdigit.jsp?idr", "http://teca.bncf.firenze.sbn.it/TecaViewer/index.jsp?RisIdr");
            URLConnection connection = new URL(params.uri).openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String url = null;
            params.uri = "addPage('http://opac.bncf.firenze.sbn.it/php/xlimage/XLImageRV.php";
            while ((url = reader.readLine()) != null) {
                int index = url.indexOf(params.uri);
                if (index != -1) {
                    url = url.substring(url.indexOf("'") + 1, url.lastIndexOf("'"));
                    break;
                }
            }
            connection = new URL(url).openConnection();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            params.uri = "<input type=\"image\" border=\"0\" name=\"tpos\" width=\"";
            while ((url = reader.readLine()) != null) {
                int index = url.indexOf(params.uri);
                if (index != -1) {
                    url = url.substring(url.indexOf(" src=\"") + 6, url.lastIndexOf("\" alt=\"")).replace("&z=2", "&z=32").replace("&z=4", "&z=64").replace("&z=8", "&z=128");
                    break;
                }
            }
            if (url != null) {
                connection = new URL(url).openConnection();
                return processNewUri(connection);
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the George Mason University */
    protected BufferedImage handleGMUException() {
        if (params.uri.startsWith("http://mars.gmu.edu:8080")) try {
            URLConnection connection = new URL(params.uri).openConnection();
            int index = params.uri.lastIndexOf("?");
            params.uri = "<img class=\"itemthumb\" src=\"";
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String url = null;
            while ((url = reader.readLine()) != null) {
                index = url.indexOf(params.uri);
                if (index != -1) {
                    url = "http://mars.gmu.edu:8080" + url.substring(index + 28);
                    url = url.substring(0, url.indexOf("\" alt=\""));
                    break;
                }
            }
            if (url != null) {
                connection = new URL(url).openConnection();
                return processNewUri(connection);
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the Florida Center for Library Automation */
    protected BufferedImage handleFCLAException() {
        if (params.uri.startsWith("http://image11.fcla.edu/cgi")) try {
            params.uri = params.uri.substring(params.uri.indexOf("q1=") + 3);
            params.uri = params.uri.substring(0, params.uri.indexOf("&"));
            params.uri = "http://image11.fcla.edu/m/map/thumb/" + params.uri.substring(params.uri.length() - 3, params.uri.length() - 2) + "/" + params.uri.substring(params.uri.length() - 2, params.uri.length() - 1) + "/" + params.uri.substring(params.uri.length() - 1, params.uri.length()) + "/" + params.uri + ".jpg";
            URL url = new URL(params.uri);
            URLConnection connection = url.openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the Portuguese National Library */
    protected BufferedImage handleBNException() {
        if (params.uri.startsWith("http://purl.pt/")) try {
            URLConnection connection = new URL(params.uri).openConnection();
            if (params.uri.endsWith("/")) params.uri = params.uri.substring(0, params.uri.length() - 1);
            int index = params.uri.lastIndexOf("/");
            params.uri = "http://purl.pt/homepage/" + params.uri.substring(index + 1) + "/" + params.uri.substring(index + 1);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String url = null;
            while ((url = reader.readLine()) != null) {
                index = url.indexOf(params.uri);
                if (index != -1) {
                    url = url.substring(index);
                    url = url.substring(0, url.indexOf("\""));
                    break;
                }
            }
            if (url != null) {
                connection = new URL(url).openConnection();
                return processNewUri(connection);
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the National Library of Estonia */
    protected BufferedImage handleNLIBException() {
        if (params.uri.startsWith("http://digar.nlib.ee/otsing/") || params.uri.startsWith("http://digar.nlib.ee/show")) try {
            String url = "http://digar.nlib.ee/gmap/nd" + params.uri.substring(params.uri.indexOf(":") + 1, params.uri.lastIndexOf("&")) + "-tiles/z0x0y0.jpeg";
            URLConnection connection = new URL(url).openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
            try {
                if (params.uri.startsWith("http://digar.nlib.ee/show")) params.uri = "http://digar.nlib.ee/otsing/?pid=" + params.uri.substring(params.uri.lastIndexOf("/") + 1) + "&show";
                URLConnection connection = new URL(params.uri).openConnection();
                String url = params.uri;
                if (url.endsWith("&show")) url = url.substring(0, url.length() - 5);
                int index = url.lastIndexOf("?");
                url = "stream" + url.substring(index);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String aux = null;
                while ((aux = reader.readLine()) != null) {
                    index = aux.indexOf(url);
                    if (index != -1) {
                        url = "http://digar.nlib.ee/otsing/" + aux.substring(index);
                        index = url.indexOf('>');
                        if (index == -1) index = url.indexOf("\"");
                        url = url.substring(0, index);
                        break;
                    }
                }
                connection = new URL(url).openConnection();
                return processNewUri(connection);
            } catch (Exception e2) {
            }
        }
        return null;
    }

    /** Handle the exception case of the Royal Library of Belgium */
    protected BufferedImage handleKBRException() {
        if (params.uri.startsWith("http://mara.kbr.be/kbrImage/CM/") || params.uri.startsWith("http://mara.kbr.be/kbrImage/maps/") || params.uri.startsWith("http://opteron2.kbr.be/kp/viewer/")) try {
            URLConnection connection = new URL(params.uri).openConnection();
            String url = "get_image.php?intId=";
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String aux = null;
            while ((aux = reader.readLine()) != null) {
                if (aux.indexOf(url) != -1) {
                    aux = aux.substring(aux.indexOf(url));
                    url = "http://mara.kbr.be/kbrImage/" + aux.substring(0, aux.indexOf("\""));
                    break;
                }
            }
            connection = new URL(url).openConnection();
            return processNewUri(connection);
        } catch (Exception e) {
            try {
                String url = "http://mara.kbr.be/xlimages/maps/thumbnails" + params.uri.substring(params.uri.lastIndexOf("/")).replace(".imgf", ".jpg");
                if (url != null) {
                    URLConnection connection = new URL(url).openConnection();
                    return processNewUri(connection);
                }
            } catch (Exception e2) {
            }
        }
        return null;
    }

    /** TODO: Handle the exception case of the Biblioteca Digital Hispanica */
    protected BufferedImage handleBibDigHispanica() {
        if (params.uri.startsWith("http://bibliotecadigitalhispanica.bne.es:80/view/action/singleViewer.do")) try {
        } catch (Exception e) {
        }
        return null;
    }

    /** Handle the exception case of the US Library of Congress */
    protected BufferedImage handleGOVException() {
        if (params.uri.startsWith("http://memory.loc.gov/cgi-bin/ampage")) try {
            URLConnection connection = new URL(params.uri).openConnection();
            params.uri = ".*/cgi-bin/map_item.pl.*<img src=.*";
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String url = null;
            while ((url = reader.readLine()) != null) {
                if (url.matches(params.uri)) {
                    url = "http://memory.loc.gov" + url.substring(url.indexOf("<img src=\"") + 10);
                    url = url.substring(0, url.indexOf("\""));
                    break;
                }
            }
            if (url != null) {
                connection = new URL(url).openConnection();
                return processNewUri(connection);
            }
        } catch (Exception e) {
        }
        return null;
    }
}
