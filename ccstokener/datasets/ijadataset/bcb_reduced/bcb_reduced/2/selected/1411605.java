package downloadPackage;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;
import filePackage.MP3FileInformation;
import mainpackage.FileStorage;
import headFrame.SuperFrame;

public class ImageDownloadThread extends SwingWorker<Void, Void> {

    ImageDownloaderFrame idf;

    ArrayList result = new ArrayList();

    boolean pause = false;

    SuperFrame mframe;

    public ImageDownloadThread(ImageDownloaderFrame idf, SuperFrame mframe) {
        this.idf = idf;
        this.mframe = mframe;
    }

    public Void doInBackground() {
        println("started!");
        resetbar(idf.jList1.getModel().getSize());
        for (int i = 0; i < idf.mp3list.size(); i++) {
            setbar(i, idf.mp3list.size());
            MP3FileInformation mp3 = (MP3FileInformation) idf.mp3list.get(i);
            URL url = grabCover(mp3.getArtist(), mp3.getTitle());
            (new File("Covers/")).mkdirs();
            saveCover(url, new File("Covers/" + (mp3.getArtist() + " - " + mp3.getTitle() + ".jpg").replace("/", "").replace("\\", "")));
            FileStorage.getMP3FileFromPath(mp3.getPath()).setImage(new File("Covers/" + (mp3.getArtist() + " - " + mp3.getTitle() + ".jpg").replace("/", "").replace("\\", "")).getAbsolutePath());
        }
        return null;
    }

    public void saveCover(URL url, File file) {
        if (url == null) {
            return;
        }
        println("Saving image to " + file);
        try {
            BufferedImage bi = ImageIO.read(url);
            ImageIO.write(bi, "jpg", file);
        } catch (Exception e) {
            println("Error saving image from url: " + url);
        }
    }

    public URL grabCover(String artist, String title) {
        if (idf.jCheckBox3.isSelected()) {
            println("Searching cover for: " + artist);
            artist = artist.trim();
            URL url = null;
            int searchnumber = 0;
            try {
                URL yahoo = new URL("http://www.gracenote.com/search/?query=" + artist.toLowerCase().replaceAll(" ", "+") + "&search_type=artist");
                BufferedReader in = new BufferedReader(new InputStreamReader(yahoo.openStream()));
                println("" + yahoo);
                String inputLine;
                String line = "";
                while ((inputLine = in.readLine()) != null) line += inputLine;
                boolean notfound = true;
                String cut = line;
                while (notfound) {
                    String search = "<div class=\"album-name large\"><strong>Album:</strong> <a href=\"";
                    if (line.indexOf(search) <= 0) {
                        println("Artist was not found!");
                        in.close();
                        return null;
                    }
                    cut = cut.substring(cut.indexOf(search) + search.length());
                    String test = cut.substring(0, cut.indexOf("\""));
                    URL secondurl = new URL("http://www.gracenote.com" + test);
                    println("" + secondurl);
                    BufferedReader secin = new BufferedReader(new InputStreamReader(secondurl.openStream()));
                    String secinputLine;
                    String secline = "";
                    while ((secinputLine = secin.readLine()) != null) secline += secinputLine;
                    if (!(secline.toUpperCase().indexOf(title.toUpperCase()) < 0 && idf.jCheckBox2.isSelected())) {
                        String secsearch = "<div class=\"album-image\"><img src=\"";
                        String seccut = secline.substring(secline.indexOf(secsearch) + secsearch.length());
                        seccut = seccut.substring(0, seccut.indexOf("\""));
                        url = new URL("http://www.gracenote.com" + seccut);
                        if (url.toString().indexOf("covers/default") <= 0 && url.toString().indexOf("covers/") > 0) {
                            notfound = false;
                        }
                    }
                    secin.close();
                }
                in.close();
                println(url.toString());
            } catch (Exception e) {
                println("error " + e + "\n");
                e.printStackTrace();
            }
            return url;
        } else {
            return null;
        }
    }

    public void println(String s) {
        idf.jTextArea1.append("\n" + s);
    }

    public void resetbar(int max) {
        setProgress(0);
    }

    public void setbar(int val, int max) {
        setProgress(100 * val / (max + 1) == 0 ? 1 : 100 * val / (max + 1));
    }

    protected void done() {
        setbar(1, 0);
        idf.setVisible(false);
        ((DefaultListModel) idf.jList1.getModel()).clear();
        idf.mp3list.clear();
        idf = null;
    }
}
