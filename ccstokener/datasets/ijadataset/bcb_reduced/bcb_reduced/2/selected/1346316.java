package sisi.imp;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import javax.activation.MimetypesFileTypeMap;
import org.zkoss.image.AImage;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.util.Clients;
import sisi.General;
import sisi.articoli.Artcateg;
import sisi.articoli.ArtcategController;
import sisi.articoli.Articoli;
import sisi.articoli.ArticoliController;
import sisi.artimg.Artimg;
import sisi.artimg.ArtimgController;
import sisi.listini.Listini;
import sisi.listini.ListiniController;

public class SistemaDoppi {

    public SistemaDoppi(int valore) {
        if (valore == 1) {
            sistemaDoppi1();
        } else if (valore == 2) {
            scaricaImmagini();
        }
    }

    private void sistemaDoppi1() {
        System.out.println("INIZIO SISTEMAZIONE DOPPI");
        Articoli[] articoli = new ArticoliController().getArticoli();
        String codArt = "";
        System.out.println("Cancellazione articoli doppi");
        for (Articoli articolo : articoli) {
            if (articolo.getCodart().equals(codArt)) {
                Artcateg catArt = new Artcateg();
                catArt.setCodart(articolo.getCodart().trim());
                catArt.setCodcat(articolo.getCodcat());
                new ArtcategController().addArtcateg(catArt);
                new ArticoliController().removeArticoloSoloArt(articolo);
            } else {
                codArt = articolo.getCodart();
                Artcateg catArt = new Artcateg();
                catArt.setCodart(articolo.getCodart().trim());
                catArt.setCodcat(articolo.getCodcat());
                new ArtcategController().addArtcateg(catArt);
                System.out.println("Controllando articolo: " + codArt);
            }
        }
        articoli = new ArticoliController().getArticoli();
        System.out.println("Cancellazione listini doppi");
        for (Articoli articolo : articoli) {
            Collection<Listini> listini = articolo.getListiniart();
            int i = 0;
            System.out.println("Cancellazione listini doppi del articolo:" + articolo.getCodart());
            for (Listini listino : listini) {
                if (i > 0) {
                    new ListiniController().removeListini(listino);
                }
                i++;
            }
        }
        System.out.println("FINE");
    }

    private void scaricaImmagini() {
        boolean debug = true;
        System.out.println("INIZIO SCARICO IMMAGINI");
        Articoli[] articoli = new ArticoliController().getArticoli();
        String sitoWeb = "http://www.toner24.it/";
        int secondAttesa = 2;
        int i = 0;
        int x = 5732;
        int n = 200;
        for (Articoli articolo : articoli) {
            if (i > (n + x)) {
                break;
            }
            i++;
            if (i >= x && !articolo.getIpertesto().isEmpty()) {
                Artimg[] artimg = new ArtimgController().getArtimg(articolo.getCodart());
                byte[] bytes = null;
                String nomeImg = articolo.getIpertesto().trim();
                nomeImg = nomeImg.substring(nomeImg.lastIndexOf("nav=") + 4);
                nomeImg = "img/" + nomeImg + "_6.jpg";
                String img = sitoWeb + nomeImg;
                String imageUrl = img;
                String nomeFileSuDB = "ar" + articolo.getArid() + ".jpg";
                String destinationFile = "c://tmp//img//ar" + articolo.getArid() + ".jpg";
                try {
                    saveImage(imageUrl, destinationFile);
                    File fileImage = new File(destinationFile);
                    String contenttype = "";
                    if (debug) {
                        System.out.println("Data: " + new Date());
                        System.out.println("URL: " + nomeImg);
                        System.out.println("Articolo: " + articolo.getCodart().trim() + " immagine: " + img);
                        System.out.println("Valore I: " + i + " - Valore X:" + x);
                        System.out.println("------------------------------");
                    }
                    if (fileImage.exists()) {
                        contenttype = new MimetypesFileTypeMap().getContentType(fileImage);
                        try {
                            int size = (int) fileImage.length();
                            bytes = new byte[size];
                            DataInputStream dis = new DataInputStream(new FileInputStream(fileImage));
                            int read = 0;
                            int numRead = 0;
                            while (read < bytes.length && (numRead = dis.read(bytes, read, bytes.length - read)) >= 0) {
                                read = read + numRead;
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (artimg != null && artimg.length >= 1) {
                        artimg[0].setPercorso(nomeFileSuDB);
                        artimg[0].setImmagine(bytes);
                        artimg[0].setContenttype(contenttype);
                        new ArtimgController().updateArtimg(artimg[0]);
                    } else {
                        Artimg immagine = new Artimg();
                        immagine.setCodart(articolo.getCodart());
                        immagine.setPercorso(nomeFileSuDB);
                        immagine.setImmagine(bytes);
                        immagine.setContenttype(contenttype);
                        new ArtimgController().addArtimg(immagine);
                    }
                    General.waitSeconds(secondAttesa);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        System.out.println("FINE SCARICO IMMAGINI");
    }

    public static void saveImage(String imageUrl, String destinationFile) throws IOException {
        URL url = new URL(imageUrl);
        InputStream is = url.openStream();
        OutputStream os = new FileOutputStream(destinationFile);
        byte[] b = new byte[2048];
        int length;
        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }
        is.close();
        os.close();
    }
}
