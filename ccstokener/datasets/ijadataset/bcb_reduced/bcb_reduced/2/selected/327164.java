package espider.webservices.amazon;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import espider.libs.helliker.id3.CorruptHeaderException;
import espider.libs.helliker.id3.ID3v2FormatException;
import espider.libs.helliker.id3.MP3File;
import espider.libs.helliker.id3.NoMPEGFramesException;

public class Cover {

    public static void getCoverFromUrl(URL url, String directory) {
        try {
            url.openConnection();
            InputStream is = url.openStream();
            System.out.flush();
            FileOutputStream fos = null;
            fos = new FileOutputStream(directory);
            int oneChar, count = 0;
            while ((oneChar = is.read()) != -1) {
                fos.write(oneChar);
                count++;
            }
            is.close();
            fos.close();
        } catch (MalformedURLException e) {
            System.err.println(" getCoverFromUrl " + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(" getCoverFromUrl " + e.toString());
            e.printStackTrace();
        }
    }

    /**
   *  Scan a directory recusivly and download the cover
   *
   *@param repertoire                 The directory to scan
   *@param deep						  how deep it will be
   *
   */
    public static void MassCovering(File repertoire, int deep) {
        File coverjpg = new File(repertoire.getParent() + "//folder.jpg");
        if ((repertoire.isFile()) & (!coverjpg.exists())) {
            if (repertoire.getName().toLowerCase().endsWith(".mp3")) {
                try {
                    MP3File mp3file = new MP3File(repertoire);
                    if ((!mp3file.getArtist().equals("")) || (!mp3file.getAlbum().equals(""))) {
                        ArrayList<AmazonItem> cover = Amazon.searchAmazonItem(mp3file.getArtist().toLowerCase(), mp3file.getAlbum().toLowerCase());
                        if (cover.size() > 0) {
                            URL url = new URL("" + cover.get(0).getMediumImageURL());
                            getCoverFromUrl(url, coverjpg.getAbsolutePath());
                            KDEIcon(coverjpg.getAbsolutePath());
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (NoMPEGFramesException nmfe) {
                    nmfe.printStackTrace();
                } catch (ID3v2FormatException ID3v2) {
                    ID3v2.printStackTrace();
                } catch (CorruptHeaderException che) {
                    che.printStackTrace();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        if (repertoire.isDirectory()) {
            File[] list = repertoire.listFiles();
            for (int i = 0; i < list.length; i++) {
                MassCovering(list[i], 1);
            }
        }
    }

    /**
   *  Create .directory file for kde OS
   *
   *@param repertoire                 The directory
   *
   */
    private static void KDEIcon(String CoverDirectory) {
        try {
            File jpgdirectory = new File(CoverDirectory);
            File folderDirectory = new File(jpgdirectory.getParent());
            File inputFile = new File(folderDirectory + "//folder.jpg");
            BufferedImage input = ImageIO.read(inputFile);
            File outputFile = new File(folderDirectory + "//folder.png");
            ImageIO.write(input, "PNG", outputFile);
            File kdeFile = new File(folderDirectory + "//.directory");
            PrintWriter file = new PrintWriter(new BufferedWriter(new FileWriter(kdeFile)));
            file.println("[Desktop Entry]	");
            file.println("Icon=" + outputFile.getAbsolutePath());
            file.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
