package musicbox.backend;

import java.text.ParseException;
import musicbox.backend.filefilters.ImageFilter;
import musicbox.backend.comparators.AlbumComparator;
import musicbox.backend.comparators.ArtistComparator;
import musicbox.database.Database;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle.Control;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javazoom.jl.decoder.Bitstream;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.datatype.Artwork;
import javax.sound.sampled.AudioPermission;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import musicbox.gui.CustomTableModel;
import musicbox.gui.MiniPlayer;
import musicbox.gui.MusicBoxView;
import noTalent.MusicOutputDesign;
import noTalent.SocketObject;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.io.BitInputStream;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.kc7bfi.jflac.sound.spi.FlacAudioFileReader;
import org.kc7bfi.jflac.sound.spi.FlacAudioFormat;

/**
 * The application's backbone.
 *
 * @author Isaac Hammon
 */
public class Backend extends Thread {

    /**
     *
     */
    public static String fFilename = null;

    /**
     *
     */
    public static MP3AudioHeader audioHeader;

    /**
     *
     */
    public static AudioHeader aH;

    /**
     *
     */
    public static org.kc7bfi.jflac.apps.Player flacPlayer;

    /**
     *
     */
    public static String filePath;

    /**
     *
     */
    public static Vector<Integer> playList;

    /**
     *
     */
    public static int currentLocation = 0;

    private static String artist;

    private static String previousArtist;

    /**
     *
     */
    public static ScrollText sT;

    /**
     *
     */
    public static SourceDataLine line;

    /**
     *
     */
    public static Control c;

    /**
     *
     */
    public static AudioInputStream din;

    /**
     *
     */
    public int nBytesRead;

    /**
     *
     */
    public static long doSeek = -1;

    /**
     *
     */
    public static long toSkip = 0;

    /**
     *
     */
    public AudioFormat decodedFormat;

    /**
     *
     */
    public AudioInputStream in;

    /**
     *
     */
    public AudioFileFormat aFF;

    /**
     *
     */
    public static long fileLength;

    /**
     *
     */
    public static long numFrames;

    /**
     *
     */
    public static long currentFrame;

    /**
     *
     */
    public static AudioPermission aP = new AudioPermission("play");

    /**
     *
     */
    public static Database db = new Database();

    /**
     *
     */
    public static MusicOutputDesign currentTrack;

    /**
     *
     */
    public static Vector<String> googleImages = new Vector<String>();

    public static boolean cont = true;

    public MusicBoxView mbv;

    /**
     *Shuffles the current playlist when called.
     */
    public void shuffle() {
        Collections.shuffle(playList);
        CustomTableModel ctm = (CustomTableModel) MusicBoxView.currentlyPlayingTable.getModel();
        ctm.removeAllRows();
        MusicBoxView.populateTableModel(ctm, playList);
        currentLocation = 0;
    }

    /**
     *Returns the name of the current file being played.
     *
     * @return fFilename
     */
    public String getFileName() {
        return fFilename;
    }

    /**
     * Returns the file path of the current file being played.
     *
     * @return filePath
     */
    public String getFilePath() {
        return filePath;
    }

    public static void setImagePath(String imagePath, String value) {
        int loc = 0;
        if (MusicBoxView.jTabbedPane2.getSelectedIndex() == 0) {
            Collections.sort(MusicBoxView.allMusic, new ArtistComparator());
            loc = binarySearchArtist(MusicBoxView.allMusic, value, 0, MusicBoxView.allMusic.size());
        } else {
            Collections.sort(MusicBoxView.allMusic, new AlbumComparator());
            loc = binarySearchAlbum(MusicBoxView.allMusic, value, 0, MusicBoxView.allMusic.size());
        }
        if (loc != -1) {
            MusicBoxView.allMusic.elementAt(loc).setImagePath(imagePath);
            try {
                java.sql.Statement stmt = db.getConnection().createStatement();
                Database.updateImagePath(stmt, MusicBoxView.allMusic.elementAt(loc));
                stmt.close();
            } catch (SQLException ex) {
                MusicBoxView.showErrorDialog(ex);
            }
        }
    }

    private static int binarySearchArtist(Vector<MusicOutputDesign> v, String value, int low, int high) {
        if (high < low) {
            return -1;
        }
        int mid = (high + low) / 2;
        if (mid > v.size()) {
            return -1;
        }
        String s = v.elementAt(mid).getArtist().replace("\\", "/");
        if (s.compareToIgnoreCase(value) > 0) {
            return binarySearchArtist(v, value, low, mid - 1);
        } else if (s.compareToIgnoreCase(value) < 0) {
            return binarySearchArtist(v, value, mid + 1, high);
        } else {
            return mid;
        }
    }

    private static int binarySearchAlbum(Vector<MusicOutputDesign> v, String value, int low, int high) {
        if (high < low) {
            return -1;
        }
        int mid = (high + low) / 2;
        String s = v.elementAt(mid).getAlbum().replace("\\", "/");
        if (s.compareToIgnoreCase(value) > 0) {
            return binarySearchArtist(v, value, low, mid - 1);
        } else if (s.compareToIgnoreCase(value) < 0) {
            return binarySearchArtist(v, value, mid + 1, high);
        } else {
            return mid;
        }
    }

    /**
     *
     * @param m
     * @param r
     */
    @SuppressWarnings("static-access")
    public void updateRating(MusicOutputDesign m, int r) {
        try {
            m.setRating("" + r);
            java.sql.Statement stmt = db.getConnection().createStatement();
            db.updateRating(stmt, m);
            stmt.close();
            if (!MusicBoxView.playList.isEmpty()) {
                MusicBoxView.updateVector(MusicBoxView.allMusic, m, MusicBoxView.playlistTable);
            }
            MusicBoxView.updateVector(MusicBoxView.allMusic, m, MusicBoxView.currentlyShowingTable);
        } catch (SQLException ex) {
            MusicBoxView.showErrorDialog(ex);
        }
    }

    public static void updateRating(MusicOutputDesign m, int r, String flag) {
        m.setRating("" + r);
        try {
            java.sql.Statement stmt = db.getConnection().createStatement();
            db.updateRating(stmt, m);
            stmt.close();
        } catch (Exception e) {
            MusicBoxView.showErrorDialog(e);
        }
        if (!MusicBoxView.playList.isEmpty()) {
            MusicBoxView.updateVector(MusicBoxView.allMusic, m, MusicBoxView.playlistTable);
        }
        MusicBoxView.updateVector(MusicBoxView.allMusic, m, MusicBoxView.currentlyShowingTable);
    }

    /**
     * Gets the current play list selected by the user.
     * @return currentlyPlaying
     */
    public static Vector<Integer> getPlaylist() {
        return MusicBoxView.currentlyPlaying;
    }

    /**
     * TODO:
     * @param filePath
     * @param file
     * @param playCount
     */
    @SuppressWarnings("static-access")
    private static void updatePlayCount(String filePath, String file, String playCount) {
        try {
            int count = Integer.parseInt(playCount);
            count += 1;
            String c1 = "" + count;
            currentTrack.setPlayCount(c1);
            java.sql.Statement stmt = db.getConnection().createStatement();
            db.updatePlayCount(stmt, currentTrack);
            stmt.execute("COMMIT");
            stmt.close();
        } catch (Exception e) {
            MusicBoxView.showErrorDialog(e);
        }
        MusicBoxView.updateVectorAtLocation(playList, currentLocation, currentTrack, MusicBoxView.currentlyPlayingTable);
    }

    /**
     * Gets the next item in the play list.
     * @return MusicPutputDesign Returns the next item.
     */
    public int getNext() {
        return playList.elementAt(currentLocation);
    }

    /**
     * 
     * @param txt
     * @param v
     * @return
     */
    public Vector<MusicOutputDesign> search(String txt, Vector<MusicOutputDesign> v) {
        Vector<MusicOutputDesign> tmp = new Vector<MusicOutputDesign>();
        for (int i = 0; i < v.size(); i++) {
            if (v.elementAt(i).getAlbum().toLowerCase().contains(txt.toLowerCase())) {
                tmp.add(v.elementAt(i));
            } else if (v.elementAt(i).getArtist().toLowerCase().contains(txt.toLowerCase())) {
                tmp.add(v.elementAt(i));
            } else if (v.elementAt(i).getTrackTitle().toLowerCase().contains(txt.toLowerCase())) {
                tmp.add(v.elementAt(i));
            }
        }
        return tmp;
    }

    @Override
    @SuppressWarnings("static-access")
    public void run() {
        try {
            playList = getPlaylist();
            SocketObject objectToPlay = null;
            while (currentLocation < playList.size()) {
                if (currentLocation == 0) {
                    MusicBoxView.previousTrackButton.setEnabled(false);
                    if (MusicBoxView.mini != null && MusicBoxView.mini.isVisible()) {
                        MiniPlayer.jButton1.setEnabled(false);
                    }
                } else {
                    MusicBoxView.previousTrackButton.setEnabled(true);
                    if (MusicBoxView.mini != null && MusicBoxView.mini.isVisible()) {
                        MiniPlayer.jButton1.setEnabled(true);
                    }
                }
                googleImages.clear();
                if (!playList.isEmpty()) {
                    int next = getNext();
                    if (next >= 0) {
                        currentTrack = MusicBoxView.allMusic.elementAt(next);
                    } else {
                        currentTrack = MusicBoxView.sharedLibrary.elementAt((next + 1) * -1);
                        try {
                            mbv.getSocketListener().getAudioStream(((String) mbv.addressList.getModel().getElementAt(mbv.getJList4PreviousSelection())), currentTrack.getFilePath() + "/" + currentTrack.getFile());
                            for (int wait = 0; wait < 120; wait++) {
                                objectToPlay = mbv.getSocketListener().getObjectToPlay();
                                if (objectToPlay != null) {
                                    break;
                                } else {
                                    Thread.sleep(1000);
                                }
                            }
                            if (objectToPlay == null) {
                                System.out.println("ObjectToPlay is null");
                                continue;
                            } else if (objectToPlay.getMessageType().equals("FileNotFound")) {
                                System.out.println("FileNotFoundExpection thrown");
                                objectToPlay = null;
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    MusicBoxView.showRating();
                    MusicBoxView.showSongSplash(currentTrack);
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            MusicBoxView.currentlyPlayingTable.changeSelection(currentLocation, 0, false, false);
                        }
                    });
                    fFilename = currentTrack.getFilePath() + "/" + currentTrack.getFile();
                    filePath = currentTrack.getFilePath();
                    String trackTitle = currentTrack.getTrackTitle();
                    if (artist == null) {
                        previousArtist = "";
                    } else {
                        previousArtist = artist;
                    }
                    artist = currentTrack.getArtist();
                    String album = currentTrack.getAlbum();
                    String show1 = trackTitle;
                    String show2 = "By: " + artist;
                    String show3 = "Album: " + album;
                    sT = new ScrollText(show1, show2, show3);
                    sT.start();
                    if (objectToPlay == null) {
                        play();
                    } else {
                        play(objectToPlay);
                    }
                    sT.stop();
                    if (objectToPlay == null) {
                        updatePlayCount(currentTrack.getFilePath(), currentTrack.getFile(), currentTrack.getPlayCount());
                    } else {
                        mbv.getSocketListener().setObjectToPlay(null);
                        objectToPlay = null;
                    }
                    currentLocation += 1;
                    MusicBoxView.clicked = false;
                    java.awt.EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            MusicBoxView.jProgressBar1.setValue(0);
                        }
                    });
                }
            }
            MusicBoxView.playButton.setEnabled(true);
            MusicBoxView.pauseButton.setEnabled(false);
            MusicBoxView.stopButton.setEnabled(false);
            MusicBoxView.playing = false;
            currentLocation = 0;
            playList.removeAllElements();
            MusicBoxView.fin();
        } catch (Exception ex) {
            System.err.println(ex);
            MusicBoxView.showErrorDialog(ex);
        }
    }

    /**
     * Empty constructor.
     */
    public Backend() {
        currentTrack = new MusicOutputDesign();
    }

    class GetArt extends TimerTask {

        @Override
        public void run() {
            showAlbumArt();
        }
    }

    /**
     * Gets and displays album art of the file currently playing.
     */
    public void showAlbumArt() {
        MusicBoxView.albumArtLabel.setVisible(true);
        MusicBoxView.jButton7.setEnabled(false);
        MusicBoxView.jButton8.setEnabled(false);
        boolean foundArt = false;
        if (currentTrack.getImagePath().compareTo("NoImage") != 0 && currentTrack.getImagePath().compareTo("embedded") != 0) {
            ImageIcon iI = new ImageIcon();
            if (currentTrack.getImagePath().contains("http://")) {
                try {
                    if (MusicBoxView.internetConnection) {
                        iI = new ImageIcon(new URL(currentTrack.getImagePath()));
                    }
                } catch (MalformedURLException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
            } else {
                iI = new ImageIcon(currentTrack.getImagePath());
            }
            Image k = iI.getImage();
            Image l = k.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            ImageIcon iL = new ImageIcon(l);
            MusicBoxView.albumArtLabel.setSize(100, 100);
            MusicBoxView.albumArtLabel.setIcon(iL);
            foundArt = true;
        } else {
            try {
                AudioFile file = AudioFileIO.read(new File(fFilename));
                Artwork albumArt = file.getTag().getFirstArtwork();
                byte[] b1 = albumArt.getBinaryData();
                ImageIcon iI = new ImageIcon(b1);
                int h = iI.getIconHeight();
                int w = iI.getIconWidth();
                Image i = iI.getImage();
                Image j = i.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                ImageIcon iJ = new ImageIcon(j);
                MusicBoxView.albumArtLabel.setSize(100, 100);
                MusicBoxView.albumArtLabel.setIcon(iJ);
                foundArt = true;
                if (h < 0 && w < 0) {
                    foundArt = false;
                }
            } catch (Exception ex) {
                if (filePath != null) {
                    File myDir = new File(filePath);
                    String[] dirNames = myDir.list(new ImageFilter());
                    if (dirNames != null && dirNames.length > 0) {
                        ImageIcon max = new ImageIcon(filePath + "/" + dirNames[0]);
                        for (int p1 = 0; p1 < dirNames.length; p1++) {
                            ImageIcon iK = new ImageIcon(filePath + "/" + dirNames[p1]);
                            if (iK.getIconHeight() + iK.getIconWidth() > max.getIconHeight() + iK.getIconWidth()) {
                                max = iK;
                            }
                        }
                        Image k = max.getImage();
                        Image l = k.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        ImageIcon iL = new ImageIcon(l);
                        MusicBoxView.albumArtLabel.setSize(100, 100);
                        MusicBoxView.albumArtLabel.setIcon(iL);
                        foundArt = true;
                    }
                }
            }
            if (foundArt == false) {
                if (MusicBoxView.internetConnection) {
                    MusicBoxView.jButton7.setEnabled(true);
                    googleImageSearch();
                    ImageIcon icon;
                    try {
                        if (!googleImages.isEmpty()) {
                            icon = new ImageIcon(new URL(googleImages.elementAt(MusicBoxView.googleImageLocation)));
                            ImageIcon ico = new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                            MusicBoxView.albumArtLabel.setIcon(ico);
                        }
                    } catch (MalformedURLException ex1) {
                        MusicBoxView.showErrorDialog(ex1);
                    }
                }
            }
        }
    }

    /**
     *
     */
    public void googleImageSearch() {
        if (artist.compareToIgnoreCase(previousArtist) != 0) {
            MusicBoxView.googleImageLocation = 0;
            try {
                String u = "http://images.google.com/images?q=" + currentTrack.getArtist() + " - " + currentTrack.getAlbum() + "&sa=N&start=0&ndsp=21";
                if (u.contains(" ")) {
                    u = u.replace(" ", "+");
                }
                URL url = new URL(u);
                HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
                httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
                BufferedReader readIn = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                String text = "";
                String lin = "";
                while ((lin = readIn.readLine()) != null) {
                    text += lin;
                }
                readIn.close();
                if (text.contains("\n")) {
                    text = text.replace("\n", "");
                }
                String[] array = text.split("\\Qhref=\"/imgres?imgurl=\\E");
                for (String s : array) {
                    if (s.startsWith("http://") || s.startsWith("https://") && s.contains("&amp;")) {
                        String s1 = s.substring(0, s.indexOf("&amp;"));
                        googleImages.add(s1);
                    }
                }
            } catch (Exception ex4) {
                MusicBoxView.showErrorDialog(ex4);
            }
        }
    }

    /**
     *
     * @param search
     * @param start
     */
    public void googleImageSearch(String search, String start) {
        try {
            String u = "http://images.google.com/images?q=" + search + start;
            if (u.contains(" ")) {
                u = u.replace(" ", "+");
            }
            URL url = new URL(u);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
            BufferedReader readIn = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            googleImages.clear();
            String text = "";
            String lin = "";
            while ((lin = readIn.readLine()) != null) {
                text += lin;
            }
            readIn.close();
            if (text.contains("\n")) {
                text = text.replace("\n", "");
            }
            String[] array = text.split("\\Qhref=\"/imgres?imgurl=\\E");
            for (String s : array) {
                if (s.startsWith("http://") || s.startsWith("https://") && s.contains("&amp;")) {
                    String s1 = s.substring(0, s.indexOf("&amp;"));
                    googleImages.add(s1);
                }
            }
        } catch (Exception ex4) {
            MusicBoxView.showErrorDialog(ex4);
        }
        MusicBoxView.jButton7.setEnabled(true);
        ImageIcon icon;
        try {
            icon = new ImageIcon(new URL(googleImages.elementAt(MusicBoxView.googleImageLocation)));
            ImageIcon ico = new ImageIcon(icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
            MusicBoxView.albumArtLabel.setIcon(ico);
        } catch (MalformedURLException ex1) {
            MusicBoxView.showErrorDialog(ex1);
        }
    }

    class GetWiki extends TimerTask {

        @Override
        public void run() {
            if (MusicBoxView.internetConnection) {
                getBandWikiPage();
            }
        }
    }

    /**
     * Gets a bands wikipedia page and displays it.
     */
    public void getBandWikiPage() {
        if (artist.compareToIgnoreCase(previousArtist) != 0) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        String s = "http://en.wikipedia.org/wiki/" + replaceSpace(artist);
                        URL url = new URL(s);
                        MusicBoxView.jEditorPane1.setPage(url);
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("BODY { font-size: 12pt; text-align: left; width:50px;}");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("P { font-size: 12pt; }");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("A { font-size: 12pt; text-decoration:underline;  color: blue;}");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("LI { font-size: 12pt; }");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("DIV { font-size: 12pt; }");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("SPAN { font-size: 12pt; }");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("TD { font-size: 12pt; }");
                        ((HTMLDocument) MusicBoxView.jEditorPane1.getDocument()).getStyleSheet().addRule("TH { font-size: 12pt; }");
                    } catch (Exception ex) {
                        MusicBoxView.showErrorDialog(ex);
                    }
                }
            });
        }
    }

    /**
     * Plays audio file.
     */
    public void play() {
        try {
            new Timer().schedule(new GetArt(), 100);
            new Timer().schedule(new GetWiki(), 200);
            MusicBoxView.playing = true;
            MusicBoxView.timeSkipped = 0;
            MusicBoxView.jLabel2.setText(currentTrack.getTrackLength());
            if (fFilename.toLowerCase().endsWith(".mp3")) {
                MP3File f = (MP3File) AudioFileIO.read(new File(fFilename));
                audioHeader = (MP3AudioHeader) f.getAudioHeader();
                numFrames = audioHeader.getNumberOfFrames();
                MusicBoxView.trackLength = audioHeader.getPreciseTrackLength();
            } else {
                if (!fFilename.toLowerCase().endsWith(".ape") && !fFilename.toLowerCase().endsWith(".au") && !fFilename.toLowerCase().endsWith(".aif")) {
                    AudioFile aF = AudioFileIO.read(new File(fFilename));
                    aH = aF.getAudioHeader();
                    MusicBoxView.trackLength = aH.getTrackLength();
                } else {
                    AudioFileFormat aff = AudioSystem.getAudioFileFormat(new File(fFilename));
                    Map map = aff.properties();
                    if (map.containsKey("duration")) {
                        MusicBoxView.trackLength = ((Long) map.get("duration")).doubleValue();
                    }
                }
            }
            if (fFilename.toLowerCase().endsWith(".flac")) {
                FlacAudioFileReader fafr = new FlacAudioFileReader();
                AudioInputStream flacAudioInputStream = fafr.getAudioInputStream(new File(fFilename));
                AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 2, 4, 44100.0F, false);
                Flac2PcmAudioInputStream f2pcmStream = new Flac2PcmAudioInputStream(flacAudioInputStream, format, 100);
                din = AudioSystem.getAudioInputStream(format, f2pcmStream);
                rawplay(format, din);
            } else if (fFilename.toLowerCase().endsWith(".ogg") || fFilename.toLowerCase().endsWith(".mp3") || fFilename.toLowerCase().endsWith(".wav") || fFilename.toLowerCase().endsWith(".aifc") || fFilename.toLowerCase().endsWith(".aiff") || fFilename.toLowerCase().endsWith(".aif") || fFilename.toLowerCase().endsWith(".au") || fFilename.toLowerCase().endsWith(".ape")) {
                try {
                    File file1 = new File(fFilename);
                    fileLength = file1.length();
                    in = null;
                    try {
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file1));
                        while (bis.available() < file1.length()) System.out.print("");
                        byte[] buf = new byte[bis.available()];
                        bis.read(buf, 0, buf.length);
                        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                        in = AudioSystem.getAudioInputStream(bais);
                    } catch (Exception ex1) {
                        MusicBoxView.showErrorDialog(ex1);
                        FileInputStream f_in = new FileInputStream(fFilename);
                        Bitstream m = new Bitstream(f_in);
                        long start = m.header_pos();
                        fileLength = fileLength - start;
                        try {
                            m.close();
                        } catch (Exception ex) {
                            MusicBoxView.showErrorDialog(ex);
                        }
                        f_in = new FileInputStream(fFilename);
                        f_in.skip(start);
                        in = AudioSystem.getAudioInputStream(f_in);
                    }
                    din = null;
                    if (in != null) {
                        AudioFormat baseFormat = in.getFormat();
                        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                        din = AudioSystem.getAudioInputStream(decodedFormat, in);
                        aFF = AudioSystem.getAudioFileFormat(file1);
                        rawplay(decodedFormat, din);
                        in.close();
                    }
                } catch (Exception ex1) {
                    MusicBoxView.showErrorDialog(ex1);
                    System.err.println("Cannot play file: " + fFilename);
                }
            }
            MusicBoxView.albumArtLabel.setText("Nothing is playing");
        } catch (Exception ex) {
            MusicBoxView.showFileUnavailable(currentTrack);
            mbv.stop();
            MusicBoxView.showErrorDialog(ex);
        }
        MusicBoxView.changed = 0;
        MusicBoxView.timeSkipped = 0;
    }

    public void play(SocketObject objectToPlay) {
        try {
            new Timer().schedule(new GetArt(), 100);
            new Timer().schedule(new GetWiki(), 200);
            MusicBoxView.playing = true;
            MusicBoxView.timeSkipped = 0;
            MusicBoxView.jLabel2.setText(currentTrack.getTrackLength());
            String time = currentTrack.getTrackLength();
            SimpleDateFormat format = new SimpleDateFormat("mm:ss");
            Date d = format.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            int t = ((cal.get(Calendar.MINUTE) * 60) + cal.get(Calendar.SECOND));
            MusicBoxView.trackLength = (((double) t));
        } catch (ParseException ex) {
            mbv.stop();
            MusicBoxView.showErrorDialog(ex);
        }
        if (currentTrack.getFile().toLowerCase().endsWith(".flac")) {
            FlacAudioFileReader fafr = new FlacAudioFileReader();
            try {
                AudioInputStream flacAudioInputStream = fafr.getAudioInputStream(new ByteArrayInputStream(objectToPlay.getAudioBytes()));
                AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 2, 4, 44100.0F, false);
                Flac2PcmAudioInputStream f2pcmStream = new Flac2PcmAudioInputStream(flacAudioInputStream, format, 100);
                din = AudioSystem.getAudioInputStream(format, f2pcmStream);
                rawplay(format, din);
            } catch (UnsupportedAudioFileException ex) {
                MusicBoxView.showErrorDialog(ex);
            } catch (IOException ex) {
                MusicBoxView.showErrorDialog(ex);
            } catch (LineUnavailableException ex) {
                MusicBoxView.showErrorDialog(ex);
            }
        } else {
            byte[] buf = objectToPlay.getAudioBytes();
            fileLength = buf.length;
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            try {
                in = AudioSystem.getAudioInputStream(bais);
            } catch (Exception ex1) {
                MusicBoxView.showErrorDialog(ex1);
                Bitstream m = new Bitstream(bais);
                long start = m.header_pos();
                fileLength = fileLength - start;
                try {
                    m.close();
                } catch (Exception ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
                bais = new ByteArrayInputStream(buf);
                bais.skip(start);
                try {
                    in = AudioSystem.getAudioInputStream(bais);
                } catch (UnsupportedAudioFileException ex) {
                    MusicBoxView.showErrorDialog(ex);
                } catch (IOException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
            }
            din = null;
            if (in != null) {
                try {
                    AudioFormat baseFormat = in.getFormat();
                    decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                    din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    rawplay(decodedFormat, din);
                    in.close();
                } catch (IOException ex) {
                    MusicBoxView.showErrorDialog(ex);
                } catch (LineUnavailableException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
            }
        }
        MusicBoxView.changed = 0;
        MusicBoxView.timeSkipped = 0;
    }

    private String replaceSpace(String s) {
        if (s.contains(" ")) {
            s = s.replace(' ', '_');
        }
        return s;
    }

    @SuppressWarnings("static-access")
    public void rawplay(AudioFormat targetFormat, AudioInputStream d) throws IOException, LineUnavailableException {
        aP = new AudioPermission("*");
        line = getLine(targetFormat);
        byte[] data = new byte[4096];
        MusicBoxView.executeTask();
        if (line != null) {
            line.start();
            nBytesRead = 0;
            int nBytesWritten = 0;
            while (nBytesRead != -1) {
                if (doSeek == -1) {
                    nBytesRead = d.read(data, 0, data.length);
                    if (nBytesRead != -1) {
                        nBytesWritten = line.write(data, 0, nBytesRead);
                    }
                } else {
                    line.flush();
                    line.close();
                    line.stop();
                    line = getLine(targetFormat);
                    d.reset();
                    int skip = 0;
                    long skipped = 0;
                    long skippy = 0;
                    long previousSkipped = -1;
                    long previousS = -1;
                    while (skippy < toSkip) {
                        skipped = d.skip(doSeek);
                        skippy += skipped;
                        doSeek = doSeek - skipped;
                        if (skipped == previousSkipped) {
                            if (previousSkipped == previousS) {
                                break;
                            } else {
                                previousS = previousSkipped;
                            }
                        }
                        previousSkipped = skipped;
                    }
                    double ratio = (double) skippy / (double) fileLength;
                    MusicBoxView.timeSkipped = ratio * MusicBoxView.trackLength;
                    if ((int) (ratio * 100) >= (int) (MusicBoxView.finalPercent * 100 - 1)) {
                        doSeek = -1;
                    } else {
                        doSeek = toSkip;
                    }
                    line.start();
                }
            }
            line.flush();
            line.drain();
            line.stop();
            line.close();
            d.close();
        }
    }

    /**
     * 
     * @param audioFormat
     * @return
     * @throws LineUnavailableException 
     */
    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    /**
     *
     */
    public void calculatePlaylistTime() {
        if (MusicBoxView.currentlyPlaying.size() > 0) {
            String leng;
            if (MusicBoxView.currentlyPlaying.elementAt(0) >= 0) {
                leng = MusicBoxView.allMusic.elementAt(MusicBoxView.currentlyPlaying.elementAt(0)).getTrackLength();
            } else {
                leng = MusicBoxView.sharedLibrary.elementAt((MusicBoxView.currentlyPlaying.elementAt(0) + 1) * -1).getTrackLength();
            }
            int a = 0;
            int a1 = 0;
            if (leng.contains(":")) {
                a = Integer.parseInt(leng.substring(0, leng.indexOf(":")));
                a1 = Integer.parseInt(leng.substring(leng.lastIndexOf(":") + 1, leng.length()));
            }
            Long m = new Long(minutesToMillis(a) + secondsToMillis(a1));
            try {
                for (int i = 1; i < MusicBoxView.currentlyPlaying.size(); i++) {
                    String l2 = "";
                    if (MusicBoxView.currentlyPlaying.elementAt(i) >= 0) {
                        l2 = MusicBoxView.allMusic.elementAt(MusicBoxView.currentlyPlaying.elementAt(i)).getTrackLength();
                    } else {
                        l2 = MusicBoxView.sharedLibrary.elementAt((MusicBoxView.currentlyPlaying.elementAt(i) + 1) * -1).getTrackLength();
                    }
                    if (l2.contains(":")) {
                        int b1 = Integer.parseInt(l2.substring(0, l2.indexOf(":")));
                        int b2 = Integer.parseInt(l2.substring(l2.lastIndexOf(":") + 1, l2.length()));
                        m = m + new Long(minutesToMillis(b1) + secondsToMillis(b2));
                    }
                }
                DatatypeFactory df = DatatypeFactory.newInstance();
                Duration d = df.newDuration(m);
                String totalTime = new String();
                if (d.getDays() > 0) {
                    totalTime = totalTime + d.getDays() + " days, ";
                }
                if (d.getHours() > 0) {
                    totalTime = totalTime + d.getHours() % 24 + " hours, ";
                }
                if (d.getMinutes() > 0) {
                    totalTime = totalTime + d.getMinutes() % 60 + " minutes, ";
                }
                if (d.getSeconds() > 0) {
                    totalTime = totalTime + d.getSeconds() % 60 + " seconds";
                }
                MusicBoxView.jLabel5.setText(totalTime);
            } catch (DatatypeConfigurationException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     *
     * @param i
     * @return i Converts parameter i to milliseconds
     */
    public static int minutesToMillis(int i) {
        return i * 60000;
    }

    /**
     *
     * @param i
     * @return i Converts parameter i to milliseconds
     */
    public static int secondsToMillis(int i) {
        return i * 1000;
    }

    public void setMusicBoxView(MusicBoxView mbv) {
        this.mbv = mbv;
    }

    public MusicBoxView getMusicBoxView() {
        return mbv;
    }
}
